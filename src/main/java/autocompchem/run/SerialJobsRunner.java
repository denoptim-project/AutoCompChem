package autocompchem.run;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import autocompchem.datacollections.NamedData;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.utils.TimeUtils;
import autocompchem.run.jobediting.ActionApplier;


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
     * @param todoJob the list of jobs to be done. 
     * @param master the job that creates this {@link SerialJobsRunner}.
     */

    public SerialJobsRunner(List<Job> todoJobs, Job master)
    {
    	super(todoJobs, master);
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

    private void initializeExecutor()
    {
    	notificationId.set(0);
    	submittedJobs = new HashMap<Job,Future<Object>>();
        
        executor = Executors.newSingleThreadExecutor();
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
     * Runs the workflow 
     */

    public void start()
    {
        startTime = System.currentTimeMillis();
    	requestedToStart = true;
    	while (requestedToStart && !weRunOutOfTime())
    	{
    		mainIteration();
    		
    		// This takes care of processing the reaction only in case of 
    		// restart of the workflow.
    		if (requestedAction!=null)
    		{
    			List<Job> reactionObjectJobs = new ArrayList<Job>();
				Job evaluatedJob = (Job) jobRequestingAction.exposedOutput
						.getNamedData(JobEvaluator.EVALUATEDJOB).getValue();
    			switch (requestedAction.getObject()) 
    			{
					case FOCUSJOB:
					{
						reactionObjectJobs.add(evaluatedJob);
						break;
					}
					
					case PARALLELJOB:
					{
						// Nothing to do, but we do notify the user as this may
						// be a wrong setting
						logger.warn("WARNING: ignoring action " 
								+ requestedAction.getType() + " of " 
								+ requestedAction.getObject() 
								+ " because it was triggered by job " 
								+ jobRequestingAction.getId() + " while "
								+ "evaluating job " + evaluatedJob.getId() 
								+ " and both belong to a sequential workflow.");
						continue;
					}
				}
    			
    			ActionApplier.performAction(requestedAction, 
    					jobRequestingAction, 
    					reactionObjectJobs, 
    					restartCounter.get());
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
            //TODO: set dedicated logger with dedicated log file
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
	        	
	        	logger.trace("Waiting for serialized workflow- Step " 
		        			+ ii + " - " + TimeUtils.getTimestamp());
	        	
	            //TODO-gg do this in parallel runner?
	            // Check for errors
	            if (exceptionInSubJobs())
	            {
	            	cancellAllRunningThreadsAndShutDown();
	            	break;
	            }
	        	
	            //Completion clause
	            if (allSubJobsCompleted())
	            {
	            	logger.info("All " + numSubmittedJobs
	                    		+ " steps are completed. Serialized workflow "
	                    		+ "completed.");
	            	if (!requestedToStart)
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
    
    private class SerialJobListener implements JobNotificationListener
    {
		@Override
		public void reactToRequestOfAction(Action action, Job sender) 
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
				Job focusJob = (Job) jobRequestingAction.exposedOutput
						.getNamedData(JobEvaluator.EVALUATEDJOB).getValue();
				
				if (action.getObject().equals(ActionObject.PARALLELJOB))
				{
					// Nothing to do, but we do notify the user as this may
					// be a wrong setting
					logger.warn("WARNING: ignoring action " 
							+ requestedAction.getType() + " of " 
							+ requestedAction.getObject() 
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
							//TODO-gg: generate a new workflow and start a new
							// main iteration
							
							throw new IllegalArgumentException("ERROR! Case of "
									+ action.getType() + " action on " 
									+ action.getObject() + " not implemented "
									+ "in " + this.getClass().getSimpleName()
									+ ". Please, contact the developers.");
						}
						
						case STOP:
						case SKIP:
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
				} else {
					throw new IllegalArgumentException("ERROR! Case of "
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
