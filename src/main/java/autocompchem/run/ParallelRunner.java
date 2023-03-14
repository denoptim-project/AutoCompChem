package autocompchem.run;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import autocompchem.datacollections.NamedData;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.ActionApplier;


/**
 * Class for running a list of independent jobs in parallel.
 *
 * @author Marco Foscato
 */

public class ParallelRunner
{
	/**
	 * The job that required the services of this class.
	 */
	private final Job master;
	
	//TODO check permissions on these fields
	
    /**
     * List of jobs to run
     */
    private List<Job> todoJobs;

    /**
     * List of references to the submitted subtasks.
     */
    private List<Future<?>> futureJobs;
    
    /**
     * List of references to the submitted subjobs.
     */
    private List<Job> submittedJobs;
    
    /**
     * List of references to the submitted monitoting subtasks
     */
    private List<Future<?>> futureMonitoringJobs;
    
    /**
     * List of references to the submitted monitoting subjobs.
     */
    private List<Job> submittedMonitoringJobs;
    
    /**
     * Index of notifications. Used to avoid concurrent notifications by
     * serving notification on the basis of first come, first served.
     */
    private final AtomicInteger notificationId = new AtomicInteger();
    
    /**
     * Asynchronous execution service with a queue
     */
    private ScheduledThreadPoolExecutor tpExecutor;
    
    /**
     * Asynchronous execution service with a queue. Dedicated to monitoring.
     */
    private ScheduledThreadPoolExecutor stpeMonitoring;

    /**
     * Number of threads
     */
    private int nThreads;

    /**
     * Walltime for waiting for completion (milliseconds)
     */
    private long walltimeMillis = 600000L; //Default 10 min
    
    /**
     * Time step for waiting for completion (milliseconds)
     */
    private long waitingStep = 1000L; //Default 1 sec
    
    /**
     * Placeholder for exception throws by a sub job
     */
    @SuppressWarnings("unused")
	private Throwable thrownBySubJob;
    
    /**
     * Verbosity level: amount of logging from this jobs
     */
    private int verbosity = 0;

	/**
	 * The time when we started running
	 */
	private long startTime;
	
	/**
	 * Flag reporting the presence of any non-handled request to run the
	 * parallel batch.
	 */
	private boolean requestedToStart = true;
	
    /**
     * Restart counter. counts how many times the parallel batch was restarted.
     */
    private final AtomicInteger restartCounter = new AtomicInteger();
	
	/**
	 * The action requested by any of the jobs we are asked to run.
	 */
	private Action reaction;
	
	/**
	 * The job that triggered the request for action
	 */
	private Job trigger;
	
	/**
	 * Lock for synchronisation of main thread with notifications from jobs
	 */
	private Object lock = new Object();
	
	/**
	 * The reference name of a Job parameter that can be used to control the
	 * walltime. Value in seconds.
	 */
	public static final String WALLTIMEPARAM = "WALLTIME";
	
	/**
	 * The reference name of a Job parameter that can be used to control the
	 * time step between each check for completion. Value in seconds.
	 */
	public static final String WAITTIMEPARAM = "WAITSTEP";
	
	//TODO: delete?
	protected Date date = new Date();
	protected SimpleDateFormat formatter = 
			new SimpleDateFormat("HH:mm:ss.SSS ");
	
//------------------------------------------------------------------------------

    /**
     * Constructor. The sizes of pool of threads and queue control the 
     * efficiency in the usage of resources.
     * @param todoJob the list of jobs to be done. We assume these can be run
     * in parallel. No validity checking!
     * @param poolSize number of parallel threads. We assume the number is 
     * sensible. No validity checking! If less jobs are available, then this 
     * number is ignored and we'll run as many threads as jobs. In case the
     * pool of jobs includes a monitoring jobs, we will reserve as many threads
     * as the number of monitoring jobs, and these threads will only be used 
     * for monitoring.
     * @param queueSize the size of the queue. When the queue is full, the 
     * executor gets blocked until any thread becomes available and take is a 
     * job from the queue.
     * @param master the job that creates this {@link ParallelRunner}.
     */

    public ParallelRunner(List<Job> todoJobs, int poolSize, int queueSize, 
    		Job master)
    {
    	this.master = master;
        this.todoJobs = todoJobs;
        this.nThreads = Math.min(poolSize, todoJobs.size());
        
        initializeExecutor(true);

        // Add a shutdown mechanism to kill the master thread and its subjobs
        // including planned ones.
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * WARNING!
     * This initialization has to be done after defining the list of jobs to do,
     * i.e., after assigning a value to <code>todoJobs</code>.
     */
    private void initializeExecutor(boolean reserveThreadsForMonitors)
    {
    	notificationId.set(0);
        futureJobs = new ArrayList<>();
        submittedJobs = new ArrayList<Job>();
        futureMonitoringJobs = new ArrayList<>();
        submittedMonitoringJobs = new ArrayList<Job>();

        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        
        int nMonitors = countMonitoringJobs(todoJobs);
        if (nMonitors > 0)
        {
        	if (reserveThreadsForMonitors)
        	{
        		nThreads = nThreads - nMonitors;
        	}
        	stpeMonitoring =  new ScheduledThreadPoolExecutor(nMonitors, 
        			threadFactory, 
                 	new RejectedExecHandlerImpl());
        } else {
        	stpeMonitoring = null;
        }
        
        tpExecutor = new ScheduledThreadPoolExecutor(nThreads, threadFactory, 
         	new RejectedExecHandlerImpl());
    }
    
//------------------------------------------------------------------------------

    /**
     * Just counts the instances of {@link MonitoringJob}
     * @param todoJobs the list to all jobs
     * @return the number of instances of {@link MonitoringJob}
     */
    
    private int countMonitoringJobs(List<Job> todoJobs)
    {
    	int num = 0;
    	for (Job j : todoJobs)
    	{
    		if (j instanceof MonitoringJob)
    		{
    			num++;
    		}
    	}
    	return num;
    }
    
//------------------------------------------------------------------------------

    /**
     * JavaVM shutdown hook that stops all sub processes including processes
     * outside the JavaVM (e.g., bash processes).
     */

    private class ShutDownHook extends Thread
    {
        @Override
        public void run()
        {
            tpExecutor.shutdown();
            if (stpeMonitoring != null)
            {
            	stpeMonitoring.shutdown();
        	}
            try
            {
                // Wait a while for existing tasks to terminate
                if (!tpExecutor.awaitTermination(30, TimeUnit.SECONDS))
                {
                    tpExecutor.shutdownNow(); // Cancel running asks
                    if (stpeMonitoring != null 
                    		&& !stpeMonitoring.awaitTermination(10, 
                    				TimeUnit.SECONDS))
                    {
                    	stpeMonitoring.shutdownNow();
                    }
                }
            }
            catch (InterruptedException ie)
            {
                // remove traces and cleanup
                cancellAllRunningThreadsAndShutDown();
                // (Re-)Cancel if current thread also interrupted
                tpExecutor.shutdownNow();
                stpeMonitoring.shutdownNow();
                // and stop possibly alive thread
                Thread.currentThread().interrupt();
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Implementation of handler of rejected/blocked jobs.
     * Rejected/blocked jobs are those that cannot fit into any thread of 
     * the pool because all threads are busy.
     */

    private class RejectedExecHandlerImpl implements RejectedExecutionHandler
    {
        @Override
        public void rejectedExecution(Runnable job, ThreadPoolExecutor tpe)
        {
            try
            {
                // Re-send rejected job to the queue until they fit in
                tpe.getQueue().put(job);
            }
            catch (InterruptedException ie)
            {
                //If we are here, then execution is broken beyond recovery
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Set the maximum time we'll wait for completion of subjobs
     * @param walltime the walltime in seconds
     */

    public void setWallTime(long walltime)
    {
        this.walltimeMillis = walltime*1000;
    }

//------------------------------------------------------------------------------

    /**
     * Set the idle time between evaluations of sub-jobs completion status.
     * @param waitingStep the step in seconds
     */

    public void setWaitingStep(long waitingStep)
    {
        this.waitingStep = 1000*waitingStep;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set the level of detail for logging
     */
    
    public void setVerbosity(int level)
    {
    	this.verbosity = level;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Shuts down the execution services
     */
    private void shutDownExecutionService()
    {
        tpExecutor.purge();
        tpExecutor.getQueue().clear();
        tpExecutor.shutdownNow();
        if (stpeMonitoring!=null)
        {
	        stpeMonitoring.purge();
	        stpeMonitoring.getQueue().clear();
	        stpeMonitoring.shutdownNow();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Remove all reference to submitted and future jobs
     */

    private void cancellAllRunningThreadsAndShutDown()
    {
    	// NB: assumption futureJobs.size() == submittedJobs.size()
    	
    	for (int i=0; i< submittedJobs.size(); i++)
    	{
    		Future<?> f  = futureJobs.get(i);
    		Job j = submittedJobs.get(i);
    		if (!f.isDone())
    		{
    			j.setInterrupted(true);
    		}
    		f.cancel(true);
            j.stopJob();
        }
    	
    	for (int i=0; i< submittedMonitoringJobs.size(); i++)
    	{
    		Future<?> f  = futureMonitoringJobs.get(i);
    		Job j = submittedMonitoringJobs.get(i);
    		if (!f.isDone())
    		{
    			j.setInterrupted(true);
    		}
    		f.cancel(true);
            j.stopJob();
        }
        submittedJobs.clear();
        submittedMonitoringJobs.clear();
    	shutDownExecutionService();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @param jobtoKill
     */
    private void cancellOneRunningThread(Job jobtoKill)
    {
    	//TODO: do the same on submittedMonitoringJobs
    	
    	//TODO-gg change submittedJobs to map so that we get rid of assumption on
    	// consistent index between submitted and future lists.
    	
    	// must be there
    	int idx = submittedJobs.indexOf(jobtoKill);
    	
    	Future<?> jobToKillFuture = futureJobs.get(idx);
		if (!jobToKillFuture.isDone())
		{
			jobtoKill.setInterrupted(true);
		}
		jobToKillFuture.cancel(true);
        jobtoKill.stopJob();
        submittedJobs.remove(jobtoKill);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check for exceptions in sub-jobs
     * @return <code>true</code> if any sub-job returned an exception
     */

    private boolean exceptionInSubJobs()
    {
        boolean found = false;
        for (Job j : submittedJobs)
        {
            if (j.foundException())
            {
            	found = false;
                break;
            }
        }
        
        for (Job j : submittedMonitoringJobs)
        {
            if (j.foundException())
            {
            	found = false;
                break;
            }
        }

        return found;
    }

//------------------------------------------------------------------------------

    /**
     * Check for completion of all sub-jobs
     * @return <code>true</code> if all sub-jobs are completed
     */

    private boolean allSubJobsCompleted()
    {
        boolean allDone = true;
        for (Job j : submittedJobs)
        {
            if (!j.isCompleted())
            {
                allDone = false;
                break;
            }
        }
        for (Job j : submittedMonitoringJobs)
        {
            if (!j.isCompleted())
            {
                allDone = false;
                break;
            }
        }
        return allDone;
    }

//------------------------------------------------------------------------------

    /**
     * Runs all in parallel. 
     */

    public void start()
    {
        startTime = System.currentTimeMillis();
    	requestedToStart = true;
    	while (requestedToStart && !weRunOutOfTime())
    	{
    		mainIteration();
    		
    		// This takes case of processing the reaction only in case of 
    		// restart of the batch of parallel jobs.
    		if (reaction!=null)
    		{
    			List<Job> reactionObjectJobs = new ArrayList<Job>();
    			switch (reaction.getObject()) 
    			{
					case FOCUSJOB:
					{
						reactionObjectJobs.add(
								(Job) trigger.exposedOutput.getNamedData(
										JobEvaluator.EVALUATEDJOB).getValue());
						break;
					}
					case PARALLELJOB:
					{
						reactionObjectJobs.addAll(todoJobs);
						break;
					}
					//TODO-gg remove these objects
					case FOCUSJOBPARENT:
						break;
					case PREVIOUSJOB:
						break;
					case SUBSEQUENTJOB:
						break;
					default:
						break;
				}
    			
    			ActionApplier.performAction(reaction, trigger, reactionObjectJobs, 
    					restartCounter.get());
    			reaction = null;
    			trigger = null;
    		}
    	}
    }
  
//------------------------------------------------------------------------------

    /**
     * Runs a parallel run iteration, i.e., an attempt to complete all 
     * the jobs to run in parallel.
     * This assumes that the executor has been initialized and is ready to go.
     */

    private void mainIteration()
    {
    	restartCounter.getAndIncrement();
    	
    	// Mark request to start as taken care of
    	requestedToStart = false;
    	
    	// Initialise empty threads that will be used for the sub-jobs
        tpExecutor.prestartAllCoreThreads();
        if (stpeMonitoring!=null)
        {
        	stpeMonitoring.prestartAllCoreThreads();
        }
        
        // Submit all sub-jobs in once. Those that do not fit because of all
        // thread pool is filled-up are dealt with by RejectedExecHandlerImp
        Iterator<Job> it = todoJobs.iterator();
        while (it.hasNext())
        {
            
            //TODO: set dedicated logger with dedicated log file
            
            Job job = it.next();
			job.setJobNotificationListener(new ParallelJobListener());
			
			// Monitoring jobs are run on their own resources
			if (job instanceof MonitoringJob)
			{
	            submittedMonitoringJobs.add(job);
	            Future<?> fut = job.submitThread(stpeMonitoring);
	            futureMonitoringJobs.add(fut);
			} else {
	            submittedJobs.add(job);
	            Future<?> fut = job.submitThread(tpExecutor);
	            futureJobs.add(fut);
			}
        }
        
        // NB: tpExecutor.shutdown() stops the execution from accepting tasks
        // but is needed to initiate an ordinary termination of the execution 
        // service. However, it cannot be used because it cancels also the 
        // periodic tasks (i.e., MonitoringJob).
        // For this reason shutdown() is done in the 
        // cancellAllRunningThreadsAndShutDown method.
        
        //Wait for completion
        int ii = 0;
        boolean withinTime = true;
        while (withinTime)
        {
        	synchronized (lock) 
        	{
	        	ii++;
	        	
	        	if (verbosity > 2)
		        {
		        	date = new Date();
		        	System.out.println("Waiting for parallel jobs - Step " 
		        			+ ii + " - " + formatter.format(date));
	        	}
	        	
	            //Completion clause
	            if (allSubJobsCompleted())
	            {
	            	if (verbosity > 0)
	                {
	                    System.out.println("All "+submittedJobs.size()
	                    		+ " sub-jobs are completed. Parallelized "
	                    		+ "jobs done.");
	                }
	            	if (!requestedToStart)
	            		shutDownExecutionService();
	                break;
	            } else {
	            	if (verbosity > 0)
	            	{
	            		System.out.println("Checking completion of parallel "
	            				+ "jobs:");
		            	for (Job j : submittedJobs)
		            		System.out.println(j + " " +j.isCompleted());
		            	for (Job j : submittedMonitoringJobs)
		            		System.out.println(j + " " +j.isCompleted());
	            	}
	            }
	
	            // Check wall time
	            if(weRunOutOfTime())
	            {
	            	if (verbosity > 0)
	            	{
	            		System.out.println("WARNING! Wall time reached: some "
	            				+ "jobs are being interrupted");
	            	}
	            	cancellAllRunningThreadsAndShutDown();
	                withinTime = false;
	                break;
	            }
	
	            // wait some time before checking again, 
	            // or weak up upon notification
	            try
	            {
	        		lock.wait(waitingStep);
	            }
	            catch (InterruptedException ie)
	            {
	                ie.printStackTrace();
	            }
        	}
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is a listener that is associated with a specific job in the pool of
     * jobs run in parallel. Instances of this class are created only after the 
     * ThreadPoolExecutor has been initialised, so it is safe to assume that
     * the tpExecutor exists and is capable of accepting new jobs.
     */
    
    private class ParallelJobListener implements JobNotificationListener
    {
		@Override
		public void reactToRequestOfAction(Action action, Job sender) 
		{
			if (notificationId.getAndIncrement() == 0)
			{
				//This is the very first notification: we take it into account
				
				reaction = action;
				trigger = sender;
				master.exposedOutput.putNamedData(new NamedData(
						Job.ACTIONREQUESTBYSUBJOB, action));
				master.exposedOutput.putNamedData(new NamedData(
						Job.SUBJOBREQUESTINGACTION, sender));
				
				if (action.getObject().equals(ActionObject.PARALLELJOB))
				{
					switch (action.getType())
    				{
					case REDO:
						if (verbosity > 0)
						{
							System.out.println("KILLING ALL sub-jobs upon "
									+ "job's request to re-run parallel batch.");
						}
						synchronized (lock)
		            	{
							cancellAllRunningThreadsAndShutDown();
							requestedToStart = true;
		            		lock.notify();
		            	}
						// Refresh status of runner to prepare for new start
						initializeExecutor(false);
						break;
						
					case STOP:
					case SKIP:
						if (verbosity > 0)
						{
							System.out.println("KILLING ALL sub-jobs upon "
									+ "job's request.");
						}
						synchronized (lock)
		            	{
							cancellAllRunningThreadsAndShutDown();
		            		lock.notify();
		            	}
						break;
					}
				} else if (action.getObject().equals(ActionObject.FOCUSJOB))
				{
					Job focusJob = (Job) trigger.exposedOutput
							.getNamedData(JobEvaluator.EVALUATEDJOB)
							.getValue();
					switch (action.getType())
    				{
						case REDO:
						{	
							throw new IllegalArgumentException("ERROR! Case of "
									+ action.getType() + " action on " 
									+ action.getObject() + " not implemented "
									+ "in " + this.getClass().getSimpleName()
									+ ". Please, contact the developers.");
						}
						
						case STOP:
						case SKIP:
							if (verbosity > 0)
							{
								System.out.println("KILLING job " 
										+ focusJob.getId()
										+ " upon request from " 
										+ trigger.getId());
							}
							synchronized (lock)
			            	{
								cancellOneRunningThread(focusJob);

				    			ActionApplier.performAction(reaction, trigger, 
				    					Arrays.asList(focusJob), 
				    					focusJob.getRestartCounter()
				    						.getAndIncrement());
				    			reaction = null;
				    			trigger = null;
			            		lock.notify();
			            	}
							break;
						}
				} else {
					throw new IllegalArgumentException("ERROR! Case of "
							+ action.getType() + " action on " 
							+ action.getObject() + " not implemented "
							+ "in " + this.getClass().getSimpleName()
							+ ". Please, contact the developers.");
				}
			} else {
				//Ignore late-coming notifications
			}
		}
		
		@Override
		public void notifyTermination(Job sender)
		{
			synchronized (lock)
			{
        		lock.notify();
        	}
		}
    }

//------------------------------------------------------------------------------

   /**
    * Stop all if the maximum run time has been reached.
    * @return <code>true</code> if the wall time has been reached and we are 
    * killing sub-jobs.
    */

    private boolean weRunOutOfTime()
    {
        boolean res = false;
        long endTime = System.currentTimeMillis();
        long millis = (endTime - startTime);

        if (millis > walltimeMillis)
        {
        	if (verbosity > 0)
            {
	            System.out.println("Walltime reached for parallel job execution.");
	            System.out.println("Killing remaining sub-jobs.");
            }
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * The job editing tasks defined in the action are applied to the job which
     * was evaluated.
     */
    /*
    private void applyReaction()
    {
    	// No need to archive or edit if the reaction is just to stop.
    	if (reaction.getType()==ActionType.STOP)
    		return;
    	
    	// Create copy of previous data from jobs
    	for (Job j : todoJobs)
    	{
    		if (!j.isStarted() || j instanceof MonitoringJob)
    			continue;
    		
    		String path = ".";
    		if (j.customUserDir!=null)
    			path = j.customUserDir.getAbsolutePath();
        	File archiveFolder = new File(path + File.separator 
        			+ "Job_" + j.getId() + "_" + restartCounter.get());
            if (!archiveFolder.mkdirs())
            {
                Terminator.withMsgAndStatus("ERROR! Unable to create folder '"
                		+ archiveFolder+ "' for archiving partial results of "
                		+ "job.", -1);
            }
            String pathToArchive = archiveFolder.getAbsolutePath() 
            		+ File.separator;
            
            Set<File> filesToArchive = new HashSet<File>();
            if (j.stdout!=null)
            	filesToArchive.add(j.stdout);
            if (j.stderr!=null)
            	filesToArchive.add(j.stderr);
            //TODO-gg add some from rules defined in Action
            
            Set<File> filesToKeep = new HashSet<File>();
            //TODO-gg add some from rules defined in Action
            
            for (File file : filesToArchive)
            {
            	File newFile = new File(pathToArchive + file.getName());
            	try {
					Files.copy(file, newFile);
				} catch (IOException e) {
					System.out.println("WARNING: cannot copy file '" + file 
							+ "' to '" + newFile + "'. " + e.getMessage());
				}
            	if (!filesToKeep.contains(file))
            		file.delete();
            }
    	}
    	
    	Job focusJob = (Job) trigger.exposedOutput.getNamedData(
    			JobEvaluator.EVALUATEDJOB).getValue();
    	
    	for (JobEditTask jet : reaction.jobEditTasks)
    	{
    		jet.apply(focusJob);
    	}
    	
    	//NB: any data that may be needed to restart of fix things should be taken
    	// before resetting the jobs.
    	
    	// Reset status of all jobs to re-run them.
    	for (Job job : todoJobs)
    	{
    		job.resetRunStatus();
    	}
    	reaction = null;
    	trigger = null;
    }
    */
//------------------------------------------------------------------------------

}
