package autocompchem.run;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import autocompchem.datacollections.NamedData;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.ActionApplier;
import autocompchem.utils.TimeUtils;


/**
 * Class for running a list of jobs in parallel.
 *
 * @author Marco Foscato
 */

public class ParallelJobsRunner extends JobsRunner
{
    /**
     * Storage of references to the submitted jobs with their future returned
     * value.
     */
    private Map<Job,Future<Object>> submittedJobs;
    
    /**
     * Storage of references to the submitted {@link MonitoringJob} with their 
     * future returned value.
     */
    private Map<MonitoringJob,Future<Object>> submittedMonitorJobs;
    
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
     * Number of submitted jobs including first submission and re-submissions.
     */
    private int numSubmittedJobs = 0;
	
//------------------------------------------------------------------------------

    /**
     * Constructor. The sizes of pool of threads and queue control the 
     * efficiency in the usage of resources.
     * @param poolSize number of parallel threads. We assume the number is 
     * sensible. No validity checking! If less jobs are available, then this 
     * number is ignored and we'll run as many threads as jobs. In case the
     * pool of jobs includes a monitoring jobs, we will reserve as many threads
     * as the number of monitoring jobs, and these threads will only be used 
     * for monitoring.
     * @param queueSize the size of the queue. When the queue is full, the 
     * executor gets blocked until any thread becomes available and take is a 
     * job from the queue.
     * @param master the job that creates this {@link ParallelJobsRunner}.
     */

    public ParallelJobsRunner(int poolSize, int queueSize, Job master)
    {
    	super(master);
        this.nThreads = Math.min(poolSize, todoJobs.size());
        
        initializeExecutor(true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a shutdown mechanism to kill the master thread and its sub jobs
     * including planned ones.
     */
    protected void addShutDownHook()
    {
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
    }
    
//------------------------------------------------------------------------------
    
    private void initializeExecutor(boolean reserveThreadsForMonitors)
    {
    	notificationId.set(0);
    	submittedJobs = new HashMap<Job,Future<Object>>();
    	submittedMonitorJobs = new HashMap<MonitoringJob,Future<Object>>();

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
    	for (Job submittedJob : submittedJobs.keySet())
    	{
    		Future<?> future = submittedJobs.get(submittedJob);
    		if (!future.isDone())
    		{
    			submittedJob.setInterrupted(true);
    		}
    		future.cancel(true);
    		submittedJob.stopJob();
    	}
    	
    	for (Job submittedJob : submittedMonitorJobs.keySet())
    	{
    		Future<?> future = submittedMonitorJobs.get(submittedJob);
    		if (!future.isDone())
    		{
    			submittedJob.setInterrupted(true);
    		}
    		future.cancel(true);
    		submittedJob.stopJob();
    	}
    	submittedJobs.clear();
    	submittedMonitorJobs.clear();
    	shutDownExecutionService();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @param jobtoKill
     */
    private void cancelOneRunningThread(Job jobtoKill)
    {
    	if (!(submittedJobs.keySet().contains(jobtoKill) || 
    			submittedMonitorJobs.keySet().contains(jobtoKill)))
    		return;
    	
    	if (jobtoKill instanceof MonitoringJob)
    	{
        	Future<?> monJobToKillFuture = submittedMonitorJobs.get(jobtoKill);
    		if (!monJobToKillFuture.isDone())
    		{
    			jobtoKill.setInterrupted(true);
    		}
    		monJobToKillFuture.cancel(true);
            jobtoKill.stopJob();
            submittedMonitorJobs.remove(jobtoKill);
    	} else {
	    	Future<?> jobToKillFuture = submittedJobs.get(jobtoKill);
			if (!jobToKillFuture.isDone())
			{
				jobtoKill.setInterrupted(true);
			}
			jobToKillFuture.cancel(true);
	        jobtoKill.stopJob();
	        submittedJobs.remove(jobtoKill);
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check for exceptions in sub-jobs
     * @return <code>true</code> if any sub-job returned an exception
     */

	private boolean exceptionInSubJobs()
    {
        boolean found = false;
        for (Job submittedJob : submittedJobs.keySet())
        {
            if (submittedJob.foundException())
            {
            	found = true;
                break;
            }
        }
        
        for (Job submittedJob : submittedMonitorJobs.keySet())
        {
            if (submittedJob.foundException())
            {
            	found = true;
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
        for (Job submittedJob : submittedJobs.keySet())
        {
            if (!submittedJob.isCompleted())
            {
                allDone = false;
                break;
            }
        }
        for (Job submittedJob : submittedMonitorJobs.keySet())
        {
            if (!submittedJob.isCompleted())
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

    		// This takes care of processing the requested action only in case  
    		// of a restart of the parallel batch. However, note that any part 
        	// of the action that affects the execution service 
    		// (e.g., the killing of a job) is done already by the 
    		// ParallelJobListener
    		if (requestedAction!=null)
    		{
    			todoJobs = ActionApplier.performActionOnParallelBatch(
    					requestedAction, 
    					master,
    					focusJob,
    					jobRequestingAction,
    					restartCounter.get());
    			
    			// Consume requested action
    			requestedAction = null;
    			jobRequestingAction = null;
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
            Job job = it.next();
			job.setJobNotificationListener(new ParallelJobListener());
			
			// Monitoring jobs are run on their own resources
			if (job instanceof MonitoringJob)
			{   
	            submittedMonitorJobs.put((MonitoringJob) job, 
	            		job.submitThread(stpeMonitoring));
			} else {
				submittedJobs.put(job, job.submitThread(tpExecutor));
			}
			numSubmittedJobs++;
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
	        	
	        	logger.trace("Waiting for parallel jobs - Step " 
		        			+ ii + " - " + TimeUtils.getTimestamp());
	        	
	            // Check for errors
	            if (exceptionInSubJobs())
	            {
	            	logger.error("Exception found in one of the "
	            			+ "parallel jobs. Shuting down all parallel jobs");
	            	cancellAllRunningThreadsAndShutDown();
	            	break;
	            }
	            
	            //Completion clause
	            if (requestedToStart)
	            {
	            	// Premature completion caused by any request to restart the 
	            	// batch. The details of the restart must be dealt with 
	            	// outside this method, i.e., by the ActionApplier.
	                break;
	            } else if (allSubJobsCompleted())
	            {
	            	logger.info("All " + numSubmittedJobs
	                    		+ " sub-jobs are completed. Parallelized "
	                    		+ "jobs done.");
	            	if (!requestedToStart)
	            		shutDownExecutionService();
	                break;
	            } else {
	            	String msg = "Checking completion of parallel "
            				+ "jobs:" + NL;
	            	for (Job j : submittedJobs.keySet())
	            		msg = msg + j + " " +j.isCompleted() + NL;
	            	for (Job j : submittedMonitorJobs.keySet())
	            		msg = msg + j + " " +j.isCompleted() + NL;
	            	logger.debug(msg);
	            }
	
	            // Check wall time
	            if(weRunOutOfTime())
	            {
	            	logger.warn("WARNING! Wall time reached: some "
	            				+ "jobs are being interrupted");
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
     * {@link ThreadPoolExecutor} has been initialised, 
     * so it is safe to assume that
     * the tpExecutor exists and is capable of accepting new jobs.
     */
    
    private class ParallelJobListener implements JobNotificationListener
    {
		@Override
		public void reactToRequestOfAction(Action action, EvaluationJob sender) 
		{
			if (notificationId.getAndIncrement() == 0)
			{
				// This is the very first notification: we take it into account
				// Note that notificationId is re-initialized to 0 every time
				// we re-initialize the execution service.
				
				requestedAction = action;
				jobRequestingAction = sender;
				master.exposedOutput.putNamedData(new NamedData(
						Job.ACTIONREQUESTBYSUBJOB, action));
				master.exposedOutput.putNamedData(new NamedData(
						Job.SUBJOBREQUESTINGACTION, sender));
				focusJob = jobRequestingAction.getFocusJob();
				
				if (action.getObject().equals(ActionObject.PARALLELJOB))
				{
					switch (action.getType())
    				{
					case REDO:
						logger.warn("KILLING ALL sub-jobs upon "
									+ "job's request to re-run parallel batch.");
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
						logger.warn("KILLING ALL sub-jobs upon "
									+ "job's request.");
						synchronized (lock)
		            	{
							cancellAllRunningThreadsAndShutDown();
		            		lock.notify();
		            	}
						break;
					}
				} else if (action.getObject().equals(ActionObject.FOCUSJOB))
				{
					switch (action.getType())
    				{
    					case REDO:
						case STOP:
						case SKIP:
						{
							logger.warn("KILLING job " 
										+ focusJob.getId()
										+ " upon request from " 
										+ jobRequestingAction.getId());
							synchronized (lock)
			            	{
								cancelOneRunningThread(focusJob);
								cancelOneRunningThread(jobRequestingAction);
								requestedToStart = true;
			            		lock.notify();
			            	}
							break;
						}
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

}
