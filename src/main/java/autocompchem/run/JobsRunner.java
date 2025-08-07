package autocompchem.run;

import java.util.ArrayList;

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


import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import autocompchem.run.jobediting.Action;


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
     * Max time for waiting for completion (milliseconds). Negative value means
     * no walltime.
     */
	protected long walltimeMillis = -1L; //Default: no walltime
    
    /**
     * Time step for waiting for completion (milliseconds)
     */
	protected long waitingStep = -1L; //Default: no waiting step
    /**
     * Placeholder for exception throws by a job
     */
    protected Throwable thrownBySubJob;

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
    protected EvaluationJob jobRequestingAction;
    
    /**
     * The job that produced the situation triggering an action, i.e., a 
     * step in the job to be evaluated, or such job itself, if self-contained,
     */
    protected Job focusJob;
	
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
     *  System-specific line separator
     */
    protected static final String NL = System.getProperty("line.separator");
	
    /**
     * Logger
     */
    protected Logger logger;
	
//------------------------------------------------------------------------------

    /**
     * Constructor for a instance that wants to run some jobs.
     * @param master the job that defines the list of {@link Job}s to run 
     * (i.e., the steps) and creates this {@link JobsRunner} to run them.
     */

    public JobsRunner(Job master)
    {
    	logger = LogManager.getLogger(this.getClass());
        Configurator.setLevel(logger.getName(), master.logger.getLevel());
        
    	this.master = master;
        this.todoJobs = new ArrayList<Job>(master.steps);

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
     * Set the maximum time we'll wait for completion of subjobs. Negative value
     * means we wait forever, i.e., no wall time. By default, this also sets a 
     * waiting time step that is 1/1000 of the given wall time unless a waiting 
     * time step has already been set.
     * @param walltime the wall time in seconds
     */

    public void setWallTime(long walltime)
    {
        this.walltimeMillis = walltime*1000;
        if (waitingStep<0)
        {
        	waitingStep=walltime;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Set the idle time between evaluations of sub-jobs completion status.
     * @param waitingStep the step in seconds
     */

    public void setWaitingStep(long waitingStep)
    {
    	if (waitingStep<0)
    	{
    		logger.warn("Ignoring negative waiting step. "
    				+ "To avoid any checking for wall time, "
    				+ "set a negative wall time, instead.");
    		return;
    	}
        this.waitingStep = 1000*waitingStep;
    }

//------------------------------------------------------------------------------

    /**
     * Runs the workflow
     */

    public abstract void start();
  
//------------------------------------------------------------------------------

   /**
    * Stop all if the maximum run time has been reached. By default the wall
    * time is negative, meaning that it is ignored, so, by default, this method
    * does nothing. It operates only if a wall time has been set.
    * @return <code>true</code> if the wall time has been reached and we want to 
    * kill sub jobs.
    */

    protected boolean weRunOutOfTime()
    {
    	if (walltimeMillis<0)
    	{
    		return false;
    	}
    	
        boolean res = false;
        long endTime = System.currentTimeMillis();
        long millis = (endTime - startTime);

        if (millis>walltimeMillis)
        {
        	logger.error("Walltime reached for " 
        			+ this.getClass().getSimpleName() 
        			+ ". Killing remaining workflow.");
            res = true;
        }
        return res;
    }
    
//------------------------------------------------------------------------------

}
