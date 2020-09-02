package autocompchem.run;

import java.util.ArrayList;
import java.util.Iterator;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import autocompchem.datacollections.Parameter;
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;


/**
 * Class for running a list of independent jobs in parallel.
 *
 * @author Marco Foscato
 */

public class ParallelRunner 
{
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
     * List of threads used for monitoring 
     */
    private ArrayList<Thread> listenerThreads;
    
    /**
     * List of monitoring objects
     */
    private ArrayList<ParallelJobListener> listeners;
    
    /**
     * Asynchronous tasks manager
     */
    final ThreadPoolExecutor tpExecutor;

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
     * Placeholder for exception throws by a subjob
     */
    @SuppressWarnings("unused")
	private Throwable thrownBySubJob;
    
    /**
     * Verbosity level: amount of logging from this jobs
     */
    private int verbosity = 0;
    
    /**
     * Flag signalling that we are still in time, i.e., runtime is lower than 
     * limit
     */
	private boolean withinTime;

	/**
	 * The time when we started running
	 */
	private long startTime;
	
	/**
	 * Flag signalling that the parallel jobs have to be restarted
	 */
	private boolean restart = false;
	
//------------------------------------------------------------------------------

    /**
     * Constructor. The sizes of pool of threads and queue control the 
     * efficiency in the usage of resources.
     * @param todoJob the list of jobs to be done. We assume these can be run
     * in parallel. No validity checking!
     * @param poolSize number of parallel threads. We assume the number is 
     * sensible. No validity checking! If less jobs are available, then this 
     * number is ignored and we'll run as many threads as jobs.
     * @param queueSize the size of the queue. When the queue is full, the 
     * executor gets blocked until any thread becomes available and take is a 
     * job from the queue.
     */

    public ParallelRunner(ArrayList<Job> todoJobs, int poolSize, int queueSize)
    {
        this.todoJobs = todoJobs;
        this.nThreads = Math.min(poolSize,todoJobs.size());
        
        futureJobs = new ArrayList<>();
        submittedJobs = new ArrayList<Job>();

        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        tpExecutor = new ThreadPoolExecutor(nThreads,
                                        nThreads,
                                        Long.MAX_VALUE,
                                        TimeUnit.NANOSECONDS,
                                        new ArrayBlockingQueue<Runnable>(1),
                                        threadFactory,
                                        new RejectedExecHandlerImpl());

        // Add a shutdown mechanism to kill the master thread and its subjobs
        // including planned ones.
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
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
            try
            {
                // Wait a while for existing tasks to terminate
                if (!tpExecutor.awaitTermination(30, TimeUnit.SECONDS))
                {
                    tpExecutor.shutdownNow(); // Cancel running asks
                }
            }
            catch (InterruptedException ie)
            {
                // remove traces and cleanup
                cleanupPresentBatch();
                // (Re-)Cancel if current thread also interrupted
                tpExecutor.shutdownNow();
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
     * @param walltimeMillis the walltime in milliseconds
     */

    public void setWallTime(long walltimeMillis)
    {
        this.walltimeMillis = walltimeMillis;
    }

//------------------------------------------------------------------------------

    /**
     * Set the idle time between evaluations of sub-jobs completion status.
     * @param waitingStep the step in milliseconds
     */

    public void setWaitingStep(long waitingStep)
    {
        this.waitingStep = waitingStep;
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
     * Remove all reference to submitted and future jobs
     */

    private void cleanupPresentBatch()
    {
        for (Future<?> f : futureJobs)
        {
            f.cancel(true);
        }

        for (Job r : submittedJobs)
        {
            r.stopJob();
        }

        submittedJobs.clear();
        
        tpExecutor.purge();
        tpExecutor.getQueue().clear();
    }

//------------------------------------------------------------------------------

    /**
     * Stops all subtasks and shutdown executor
     */

    public void stopAndTerminateRun()
    {
        cleanupPresentBatch();
        tpExecutor.shutdown();
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
        for (int il=0; il<listeners.size(); il++)
        {
        	ParallelJobListener pjl = listeners.get(il);
        	Thread t = listenerThreads.get(il);
        	if (!pjl.done && t.isAlive())
        	{
        		//TODO del
        		System.out.println("Waiting for PJL: "+pjl.notificationFlagId +"("+pjl.done+") on thread "+t.getName()+"("+t.isAlive()+")");
        		allDone = false;
        		//break;
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
        withinTime = true;

        // Initialise empty threads that will be used for the sub-jobs
        tpExecutor.prestartAllCoreThreads();
        startTime = System.currentTimeMillis();

    	listeners = new ArrayList<ParallelJobListener>();
        listenerThreads = new ArrayList<Thread>();

        // Submit all sub-jobs in once. Those that do not fit because of all
        // thread pool is filled-up are dealt with by RejectedExecHandlerImp
        Iterator<Job> it = todoJobs.iterator();
        while (it.hasNext())
        {
            if(checkAgainstWalltime(startTime))
            {
                withinTime = false;
                break;
            }
            
            //TODO: set dedicated logger with dedicated log file
            
            Job job = it.next();
           
            // Start a listening task on another thread. This task handles any
            // request of further action from the job it monitors.
            // This thread is not part of the pool managed by tpExecutor.
            Object notificationFlagId = job.hashCode();
            job.setRequestActionFlagId(notificationFlagId);
            ParallelJobListener pjl = new ParallelJobListener(job,notificationFlagId);
            listeners.add(pjl);
            Thread pjlThread = new Thread(pjl, "PJL-thread");
            pjlThread.start();
            listenerThreads.add(pjlThread);

            submittedJobs.add(job);
            Future<?> fut = tpExecutor.submit(job);
            futureJobs.add(fut);
        }
       
        int ii = 0;
        //Wait for completion
        while (true && withinTime)
        {
        	//TODO del
        	ii++;
        	System.out.println("WAITING LOOP ITERATION ========  "+ii);
            //Completion clause
            if (allSubJobsCompleted())
            {
            	//TODO del
            	System.out.println("STOPPIND DUE TO ALL COMPLETED");
            	if (verbosity > 0)
                {
                    System.out.println("All "+submittedJobs.size()+" sub-jobs "
                             + "have been completed. Parallelized jobs done.");
                }
                break;
            }
            //TODO del
            else {
            	System.out.println("PPLL: NOT all completed");
            	for (Job j : submittedJobs)
            		System.out.println(j + " " +j.isCompleted()+" "+j.requestsAction());
            }

            // Check walltime
            if(checkAgainstWalltime(startTime))
            {
                withinTime = false;
                break;
            }

            //wait
            try
            {
                Thread.sleep(waitingStep);
            }
            catch (IllegalArgumentException iae)
            {
                Terminator.withMsgAndStatus("ERROR! Negative waiting time in "
                		+ "ParallelRunner.",-1);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
                Terminator.withMsgAndStatus("ERROR! Interrupted thread in "
                		+ "ParallelRunner.",-1);
            }
        }
        
        listeners.clear();
        listenerThreads.clear();
        
        cleanupPresentBatch();
        stopAndTerminateRun();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is a listener that is associated with a specific job in the pool of
     * jobs run in parallel. Instances of this class are created only after the 
     * ThreadPoolExecutor has been initialised, so it is safe to assume that
     * the tpExecutor exists and is capable of accepting new jobs.
     */
  /*  private class ParallelJobListener implements JobNotificationListener
    {
    	Action requestedAction;

		@Override
		public void reactToRequestOfAction(Action action, Job sender) 
		{
			this.requestedAction = action;
		}
    }
*/
    
    private class ParallelJobListener implements Runnable
    {
    	private final Job object;
    	private final Object notificationFlagId;
    	protected boolean done = false;
    	
    	public ParallelJobListener(Job object, Object notificationFlagId)
    	{
    		this.object = object;
    		this.notificationFlagId = notificationFlagId;
    		
    		//TODO set counters based on 'object'
    	}
    	
    	public void run()
    	{
			synchronized (notificationFlagId)
    		{
				if (Thread.currentThread().isInterrupted())
				{
					//TODO del
					System.out.println("PJL -> INTERRUPTED");
					return;
				}
					
				//TODO del
				System.out.println("PJL -> waiting on noNotificationFlag:"+notificationFlagId+" for "+object);
				try {
					notificationFlagId.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					terminate();
					return;
				}
				
				if (!object.requestsAction())
				{
					//TODO del
					System.out.println("Nothing to do for PJL "+notificationFlagId+ " ("+done+")");
					terminate();
					return;
				}
				
				//TODO del: handling of the action HAS to be done outside the ParallelRunner
				// the only thing we do here is to STOP the parallel Threads
				
				Action a = object.getRequestedAction();
				
				if (a.getObject().equals(ActionObject.PARALLELJOB))
				{
					switch (a.getType())
    				{
					case STOP:
					case REDO:
					case REDOAFTER:
						System.out.println("KILLING ALL upon action's request");
						cleanupPresentBatch();
						break;
						
					case REDO:
						//TODO del
						System.out.println("KILLING ALL upon action's request-REDO");
						cleanupPresentBatch();
						
						int numRestarts = 0;
						if (object.getParameter("RESTART") != null)
						{
							numRestarts = Integer.parseInt(
									object.getParameter("RESTART")
									.getValueAsString());
						}
						numRestarts++;
						object.setParameter(new Parameter("RESTART",
								numRestarts+""));
						
						//TODO get maximum from parameters
						if (numRestarts>5)
						{
							//TODO logging
							break;
						}
						
						//TODO: alter jobs according to action details
						
						// Make the new batch of modified jobs
						ArrayList<Job> newJobs = new ArrayList<Job>();
						for (Job oldJob : todoJobs)
						{
							Job newJob = JobFactory.createJob(
									oldJob.toTextBlockJobDetails());
							newJobs.add(newJob);
						}
						//todoJobs = newJobs; //NOT POSSIBLE
						//restart = true;
						break;
					}
				}

    			//TODO del
    			System.out.println("PJL -> AFTER "+notificationFlagId);
    			//noNotificationFlag.notify();
    		}
			terminate();
			return;
    	}
    	
    	private void terminate()
    	{
    		//TODO del
			System.out.println("Terminating PJL "+notificationFlagId);
    		done = true;
    		Thread.currentThread().interrupt();
    	}
    }
//------------------------------------------------------------------------------

    /*
     * In handling notification from a Job, note that that job is completed.
     * So, remove it from the list of submitted (and future?) and, if the
     * job is a monitoring one, resubmit to tpExcecutor:
     * 
     *      Job job = (the same , for monitoring jobs)
     *      submittedJobs.add(job);
            Future<?> fut = tpExecutor.submit(job);
            futureJobs.add(fut);
            
     * this should restart the job in the same thread, which has become 
     * available in the meantime, because the old job has terminated.
     */
    
//------------------------------------------------------------------------------

   /**
    * Stop all if walltime is reached
    * @param startTime the initial time in milliseconds single EPOCH
    * @return <code>true</code> if the walltime has been reached and we are 
    * killing sub-jobs
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

            Terminator.withMsgAndStatus("ERROR! Walltime reached for "
                                             + "parallel run. Killing all.",-1);
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

}
