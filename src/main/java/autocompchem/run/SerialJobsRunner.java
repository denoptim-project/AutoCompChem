package autocompchem.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import autocompchem.datacollections.NamedData;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.ActionApplier;
import autocompchem.utils.TimeUtils;


/**
 * Class for running a list of jobs sequentially.
 *
 * @author Marco Foscato
 */

public class SerialJobsRunner extends JobsRunner
{
    /**
     * Storage of references to the submitted jobs with their future returned
     * value.
     */
    private Map<Job,Future<Object>> submittedJobs;
    
    /**
     * Serial execution service with a queue
     */
    private ExecutorService executor;

    /**
     * Index of notifications. Used to avoid concurrent notifications by
     * serving notification on the basis of first come, first served.
     */
    private final AtomicInteger notificationId = new AtomicInteger();
  
//------------------------------------------------------------------------------

    /**
     * Constructor for a sequential jobs runner. 
     * @param master the job that creates this {@link SerialJobsRunner}.
     */

    public SerialJobsRunner(Job master)
    {
    	super(master);
        initializeExecutor();
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

    /**
     * Initializes a single thread executor with a queue that can hold at most
     * Integer.MAX_VALUE tasks.
     */
    private void initializeExecutor()
    {
    	notificationId.set(0);
    	submittedJobs = new HashMap<Job,Future<Object>>();
        
    	executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, 
    			new LinkedBlockingQueue<Runnable>());
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
        	executor.shutdown();
            try
            {
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(30, TimeUnit.SECONDS))
                {
                	executor.shutdownNow(); // Cancel running asks
                }
            }
            catch (InterruptedException ie)
            {
                // remove traces and cleanup
                cancellAllRunningThreadsAndShutDown();
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // and stop possibly alive thread
                Thread.currentThread().interrupt();
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Shuts down the execution services
     */
    private void shutDownExecutionService()
    {
    	executor.shutdownNow();
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
    	submittedJobs.clear();
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
        for (Job submittedJob : submittedJobs.keySet())
        {
            if (submittedJob.foundException())
            {
            	logger.warn(submittedJob.thrownExc.getClass().getSimpleName()
            				+ " thrown by job " + submittedJob.getId());
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
    	for (Job job : todoJobs)
    	{
    		if (!job.isCompleted())
    		{
    			allDone = false;
    			break;
    		}
    	}
        return allDone;
    }

//------------------------------------------------------------------------------

    /**
     * Runs the workflow in a dynamic fashion. Hence, the workflow gets modified
     * according to any requests of action coming from the workflow itself.
     */

    public void start()
    {
        startTime = System.currentTimeMillis();
    	requestedToStart = true;
    	while (requestedToStart && !weRunOutOfTime())
    	{
    		mainIteration();
    		
    		// This takes care of processing the requested action only in case  
    		// of a restart of the workflow. However, note that any part of the 
        	// action that affects the execution service (e.g., the killing of
        	// a job) is done already by the SerialJobListener
    		if (requestedAction!=null)
    		{
				Job focusJob = jobRequestingAction.getJobBeingEvaluated();
				int idFocusJob = todoJobs.indexOf(focusJob);
		    	
		    	ActionApplier.performActionOnSerialWorkflow(requestedAction, 
		    			master, idFocusJob, restartCounter.get());
    			todoJobs = new ArrayList<Job>(master.steps);
    			
    			// Consume requested action
    			requestedAction = null;
    			jobRequestingAction = null;
    		}
    	}
    }
  
//------------------------------------------------------------------------------

    /**
     * Runs a sequential run iteration, i.e., an attempt to complete all 
     * the jobs in a workflow.
     * This assumes that the executor has been initialized and is ready to go.
     */

    private void mainIteration()
    {
    	restartCounter.getAndIncrement();
    	
    	// Mark request to start as taken care of
    	requestedToStart = false;
    	
    	// Submit the jobs via the executor
        Iterator<Job> it = todoJobs.iterator();
        int numSubmittedJobs = 0;
        while (it.hasNext())
        {
            //We could use a dedicated log file for each job
            Job job = it.next();
			job.setJobNotificationListener(new SerialJobListener());
		    submittedJobs.put(job, job.submitThread(executor));
		    numSubmittedJobs++;
        }
        
        //Wait for completion
        int ii = 0;
        boolean withinTime = true;
        while (withinTime)
        {
        	synchronized (lock) 
        	{
	        	ii++;
	        	
	        	logger.trace("Waiting for serialized workflow - Step " 
		        			+ ii + " - " + TimeUtils.getTimestamp());
	        	
	            // Check for errors
	            if (exceptionInSubJobs())
	            {
	            	cancellAllRunningThreadsAndShutDown();
	            	break;
	            }
	        	
	            //Completion clauses
	            if (requestedToStart)
	            {
	            	// Premature completion caused by any request to restart the 
	            	// workflow. The details of the restart must be dealt with 
	            	// outside this method, i.e., by the ActionApplier.
	                break;
	            } else if (allSubJobsCompleted())
	            {
	            	logger.info("All " + numSubmittedJobs
	                    		+ " steps are completed. Serialized workflow "
	                    		+ "completed.");
	            	shutDownExecutionService();
	                break;
	            }
	
	            // Check wall time
	            if (weRunOutOfTime())
	            {
	            	logger.warn("WARNING! Wall time reached: "
	            				+ "interrupting serial workflow.");
	            	cancellAllRunningThreadsAndShutDown();
	                withinTime = false;
	                break;
	            }

	            // If configured, wait some time before checking again, 
	            // or weak up upon notification.
	            try
	            {
	        		if (waitingStep>0)
	        		{
	        			lock.wait(waitingStep);
	        		} else {
	        			lock.wait();
	        		}
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
     * jobs. Instances of this class are created only after the 
     * executor has been initialised, so it is safe to assume that
     * the executor exists and is capable of accepting new jobs.
     * This listener is charged with any part of the reaction that involves the
     * execution service, but should not be doing any alteration of the jobs,
     * including evaluation and monitoring jobs, or the job sequence 
     * (i.e., workflow of todo jobs). 
     * The latter should be done only outside of the 
     * {@link SerialJobsRunner#mainIteration()}, hence in the
     * while loop of the {@link SerialJobsRunner#start()} method by the 
     * {@link ActionApplier} that processes any action. This because
     * we want to retail all information of the jobs (e.g., the exposed
     * output) in the status it is when they were evaluated 
     * (or they did evaluate other jobs).
     */
    
    private class SerialJobListener implements JobNotificationListener
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
				Job focusJob = sender.getJobBeingEvaluated();
				
				if (action.getObject().equals(ActionObject.PARALLELJOB))
				{
					// Nothing to do, but we do notify the user as this may
					// be a wrong setting
					logger.warn("WARNING: ignoring action " 
							+ requestedAction.getType() + " of " 
							+ requestedAction.getObject() //it's 'PARALLELJOB'
							+ " because it was triggered by job " 
							+ jobRequestingAction.getId() + " while "
							+ "evaluating job " + focusJob.getId() 
							+ " and both belong to a sequential workflow.");
				} else if (action.getObject().equals(ActionObject.FOCUSJOB)
						|| action.getObject().equals(
								ActionObject.FOCUSANDFOLLOWINGJOBS))
				{
					switch (action.getType())
    				{
						case REDO:
						{
							synchronized (lock)
			            	{
								cancellAllRunningThreadsAndShutDown();
			            	
								logger.warn("Action " 
									+ requestedAction.getType() + " of " 
									+ requestedAction.getObject() 
									+ " requested by job " 
									+ jobRequestingAction.getId() + " upon "
									+ "evaluating job " + focusJob.getId() 
									+ ".");
							
								// Refresh status of runner to prepare for new start
								initializeExecutor();
								requestedToStart = true;
			            		lock.notify();
			            	};
							break;
						}
						
						case STOP:
						case SKIP:
						{
							// Notification occurs at completion of the job 
							// notifying the request of action, which may or
							// may not be the focus job. If it is not the focus 
							// job, the latter has been completed already, since
							// we are in a serial workflow. If it is the focus
							// job, then notification occurs
							// at the very end, i.e., after that job is flagged
							// "completed". Therefore, killing or skipping the 
							// focus job does not do anything, effectively.
							break;
						}
    				}
				} else {
					throw new IllegalArgumentException("Case of "
							+ action.getType() + " action on " 
							+ action.getObject() + " not implemented "
							+ "in " + this.getClass().getSimpleName()
							+ ". Please, contact the developers.");
				}
			} else {
				//Ignore late-coming notifications, which should not exist in 
				// a serial workflow scenario.
			}
		}
		
		@Override
		public void notifyTermination(Job sender)
		{
			// This wakes up the the thread waiting in the mainIteration() 
			synchronized (lock)
			{
        		lock.notify();
        	}
		}
    }
    
//------------------------------------------------------------------------------

}
