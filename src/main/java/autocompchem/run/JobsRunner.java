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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * Any class able to run a list of {@link Job}s.
 *
 * @author Marco Foscato
 */

public abstract class JobsRunner
{
	/**
	 * The job that required the services of this class.
	 */
	protected final Job master;
	
    /**
     * List of jobs to run
     */
	protected List<Job> todoJobs;

    /**
     * Max time for waiting for completion (milliseconds)
     */
	protected long walltimeMillis = 600000L; //Default 10 min
    
    /**
     * Time step for waiting for completion (milliseconds)
     */
	protected long waitingStep = 1000L; //Default 1 sec
    
    /**
     * Placeholder for exception throws by a job
     */
    protected Throwable thrownBySubJob;
    
    /**
     * Verbosity level: amount of logging from this class.
     */
    protected int verbosity = 0;

	/**
	 * The time when we started running
	 */
    protected long startTime;
	
	/**
	 * Flag reporting the presence of any non-handled request to re start the
	 * workflow.
	 */
    protected boolean requestedToStart = true;
	
    /**
     * Restart counter. Counts how many times the workflow was restarted.
     */
    protected final AtomicInteger restartCounter = new AtomicInteger();
	
	/**
	 * The action requested by the the job that requested an 
	 * action (i.e., the trigger job)
	 */
    protected Action requestedAction;
	
	/**
	 * The job that requested an action
	 */
    protected Job jobRequestingAction;
	
	/**
	 * Lock for synchronisation of main thread with notifications from jobs
	 */
    protected Object lock = new Object();
	
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
	
	/**
	 * Utility to bake time stamps
	 */
	protected Date date = new Date();
	
	/**
	 * Utility to format timestamps
	 */
	protected SimpleDateFormat formatter = 
			new SimpleDateFormat("HH:mm:ss.SSS ");
	
//------------------------------------------------------------------------------

    /**
     * Constructor for a instance that wants to run some jobs.
     * @param todoJob the list of jobs to be run.
     * @param master the job that creates this {@link JobsRunner}.
     */

    public JobsRunner(List<Job> todoJobs, Job master)
    {
    	this.master = master;
        this.todoJobs = todoJobs;
        
        addShutDownHook();
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a shutdown mechanism to kill the master thread and its sub jobs
     * including planned ones.
     */
    protected abstract void addShutDownHook();

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
     * Runs the workflow
     */

    public abstract void start();
  
//------------------------------------------------------------------------------

   /**
    * Stop all if the maximum run time has been reached.
    * @return <code>true</code> if the wall time has been reached and we want to 
    * kill sub jobs.
    */

    protected boolean weRunOutOfTime()
    {
        boolean res = false;
        long endTime = System.currentTimeMillis();
        long millis = (endTime - startTime);

        if (millis > walltimeMillis)
        {
        	if (verbosity > 0)
            {
	            System.out.println("Walltime reached for " 
	            		+ this.getClass().getSimpleName() + ".");
	            System.out.println("Killing remaining workflow.");
            }
            res = true;
        }
        return res;
    }
    
//------------------------------------------------------------------------------

}
