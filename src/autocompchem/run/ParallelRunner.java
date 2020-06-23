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


/**
 * Class for running a list of independent jobs in parallel.
 *
 * @author Marco Foscato
 */

public class ParallelRunner 
{
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
    private Throwable thrownBySubJob;

//------------------------------------------------------------------------------

    /**
     * Constructor. The sizes of pool of threads and queue controll the 
     * efficiency in the usage of resources.
     * @param todoJob the list of jobs to be done. We assume these can be run
     * in parallel. No validity checking!
     * @param poolSize number of parallel threads. We assume the number is 
     * sensible. No validity checking! If less jobs are available, then this 
     * number is ignored and we'll run as many threads as ajobs.
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
        // incuding planned ones.
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
    }

//------------------------------------------------------------------------------

    /**
     * JavaVM shutdown hook that stopps all sub processes including processes
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
                cleanup(tpExecutor, futureJobs, submittedJobs);
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
     * the pool becasue all threads are busy.
     */

    private class RejectedExecHandlerImpl implements RejectedExecutionHandler
    {
        @Override
        public void rejectedExecution(Runnable job, ThreadPoolExecutor tpe)
        {
            try
            {
                // Resend rejected job to the queue until they fit in
                tpe.getQueue().put(job);
            }
            catch (InterruptedException ie)
            {
                //If we are here, then execution is brocken beyond recovery
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
     * Remove all reference to submitted and future jobs
     */

    private void cleanup(ThreadPoolExecutor tpe, List<Future<?>> futureJobs,
                            ArrayList<Job> submittedJobs)
    {
        for (Future f : futureJobs)
        {
            f.cancel(true);
        }

        for (Job r : submittedJobs)
        {
            r.stopJob();
        }

        submittedJobs.clear();
        
        tpe.purge();
        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------

    /**
     * Stops all subtasks and shutdown executor
     */

    public void stopRun()
    {
        cleanup(tpExecutor, futureJobs, submittedJobs);
        tpExecutor.shutdown();
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

        return allDone;
    }

//------------------------------------------------------------------------------

    /**
     * Runs all in parallel. 
     */

    public void start()
    {
        boolean withinTime = true;

        // Initialize empty threads that will be used for the sb-jobs
        tpExecutor.prestartAllCoreThreads();
        long startTime = System.currentTimeMillis();

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
            Job job = it.next();

            submittedJobs.add(job);
            Future fut = tpExecutor.submit(job);
            futureJobs.add(fut);
//TODO del
//System.out.println("HERE: submitting "+job+" future "+fut+ " " + (System.currentTimeMillis()-startTime)+" queue"+tpExecutor.getQueue());

        }

        //Wait for completion
        while (true && withinTime)
        {
            //Completion clause
            if (allSubJobsCompleted())
            {
                //TODO log
                System.out.println("All "+submittedJobs.size()+" sub-jobs "
                             + "have been completed. Parallelized jobs done.");
                break;
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
                Terminator.withMsgAndStatus("ERROR! Negative waiting time.",-1);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
                Terminator.withMsgAndStatus("ERROR! Thread is interrupted.",-1);
            }
        }

        //All done stop and clean up
        stopRun();
    }

//------------------------------------------------------------------------------

   /**
    * Stop all if walltime is reached
    * @param startTime the initial time in milliseconds singe EPOCH
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
            //TODO log
            System.out.println("Walltime reached for parallel job execution.");
            System.out.println("Killing remaining sub-jobs.");

            //Terminator.withMsgAndStatus("ERROR! Walltime reached for "
            //                                 + "parallel run. Killing all.",-1);
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

}
