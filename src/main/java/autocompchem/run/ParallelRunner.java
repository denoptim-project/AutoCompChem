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
import autocompchem.run.Action.ActionObject;


/**
 * Class for running a list of independent jobs in parallel.
 *
 * @author Marco Foscato
 */

public class ParallelRunner
{
	/**
	 * The job that requred the serives of this class
	 */
	private final Job master;
	
	//TODO check permissions on these fields
	
    /**
     * List of jobs to run
     */
    final ArrayList<Job> todoJobs;

    /**
     * List of references to the submitted subtasks
     */
    final List<Future<?>> futureJobs;
    
    /**
     * List of references to the submitted subjobs.
     */
    final ArrayList<Job> submittedJobs;
    
    /**
     * List of references to the submitted monitoting subtasks
     */
    final List<Future<?>> futureMonitoringJobs;
    
    /**
     * List of references to the submitted monitoting subjobs.
     */
    final ArrayList<Job> submittedMonitoringJobs;
    
    /**
     * Index of notifications. Used to avoid concurrent notifications by
     * serving notification on the basis of first come, first served.
     */
    private final AtomicInteger notificationId = new AtomicInteger();
    
    /**
     * Asynchronous execution service with a queue
     */
    final ScheduledThreadPoolExecutor tpExecutor;
    
    /**
     * Asynchronous execution service with a queue. Dedicated to monitoring.
     */
    final ScheduledThreadPoolExecutor stpeMonitoring;

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

    public ParallelRunner(ArrayList<Job> todoJobs, int poolSize, int queueSize, 
    		Job master)
    {
    	this.master = master;
        this.todoJobs = todoJobs;
        this.nThreads = Math.min(poolSize,todoJobs.size());
        
        futureJobs = new ArrayList<>();
        submittedJobs = new ArrayList<Job>();
        futureMonitoringJobs = new ArrayList<>();
        submittedMonitoringJobs = new ArrayList<Job>();

        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        
        int nMonitors = countMonitoringJobs(todoJobs);
        if (nMonitors > 0)
        {
        	nThreads = nThreads - nMonitors;
        	stpeMonitoring =  new ScheduledThreadPoolExecutor(nMonitors, 
        			threadFactory, 
                 	new RejectedExecHandlerImpl());
        } else {
        	stpeMonitoring = null;
        }
        
        tpExecutor = new ScheduledThreadPoolExecutor(nThreads, threadFactory, 
         	new RejectedExecHandlerImpl());

        // Add a shutdown mechanism to kill the master thread and its subjobs
        // including planned ones.
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
    }
    
//------------------------------------------------------------------------------

    /**
     * Just counts the instances of {@link MonitoringJob}
     * @param todoJobs the list to all jobs
     * @return the number of instances of {@link MonitoringJob}
     */
    
    private int countMonitoringJobs(ArrayList<Job> todoJobs)
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
    	// Initialise empty threads that will be used for the sub-jobs
        tpExecutor.prestartAllCoreThreads();
        if (stpeMonitoring!=null)
        {
        	stpeMonitoring.prestartAllCoreThreads();
        }
        
        startTime = System.currentTimeMillis();
        boolean withinTime = true;

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
	            	shutDownExecutionService();
	                break;
	            } else {
	            	if (verbosity > 0)
	            	{
		            	for (Job j : submittedJobs)
		            		System.out.println(j + " " +j.isCompleted());
		            	for (Job j : submittedMonitoringJobs)
		            		System.out.println(j + " " +j.isCompleted());
	            	}
	            }
	
	            // Check wall time
	            if(checkAgainstWalltime(startTime))
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
				master.exposedOutput.putNamedData(new NamedData(
						Job.ACTIONREQUESTBYSUBJOB, action));
				master.exposedOutput.putNamedData(new NamedData(
						Job.SUBJOBREQUESTINGACTION, sender));
				
				//This is the very first notification: we take it into account
				if (action.getObject().equals(ActionObject.PARALLELJOB))
				{
					switch (action.getType())
    				{
						case STOP:
						case REDO:
						case REDOAFTER:
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
							
						default:
							break;
    				}
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
    * @param startTime the initial time in milliseconds single EPOCH.
    * @return <code>true</code> if the wall time has been reached and we are 
    * killing sub-jobs.
    */

    private boolean checkAgainstWalltime(long startTime)
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

}