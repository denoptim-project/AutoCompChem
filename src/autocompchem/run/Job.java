package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.ArrayList;

import java.lang.ProcessBuilder;

import autocompchem.parameters.Parameter;
import autocompchem.parameters.ParameterStorage;


/**
 * A job is a piece of computational work to be done
 *
 * @author Marco Foscato
 */

public class Job implements Runnable
{
    /**
     * Parameters for this job
     */
    protected ParameterStorage params;

    /**
     * List of steps. Steps are nested jobs
     */
    protected ArrayList<Job> steps;

    /**
     * Application meant to do the job
     */
    private RunnableAppID appID;

    /**
     * Known apps for performing jobs
     */
    public enum RunnableAppID {
        UNDEFINED,
        SHELL,
        ACC,
        NWCHEM,
        GAUSSIAN};

    /**
     * Flag defining this job as a parallelizable job, i.e., independent from
     * any of its parent or sibling jobs.
     */
    private boolean parallelizable = false;

    /**
     * Number of parallel threads for sub-jobs. This controls whether we'll try
     * to run sub-jobs in parallel. To this end, each sub-job must also be 
     * parallelizable.
     */
    private int nThreads = 1; 

    /**
     * Flag signaling that this job has thrown an exception
     */
    private boolean hasException = false;

    /**
     * Exception thrown by this job.
     */
    private Throwable thrownExc;

    /**
     * Flag signaling the completion of this job
     */
    private boolean completed = false;

    /**
     * Flag signaling the an intended action to kill this job
     */
    protected boolean jobIsBeingKilled = false;

    /**
     * Separaton of steps in input text file. TODO conseder removing
     */
    protected String stepSeparatorInp = System.getProperty("line.separator");

    /**
     * Separaton of steps in job details text file. TODO conseder removing
     */
    protected String stepSeparatorJd = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public Job()
    {
	this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = RunnableAppID.UNDEFINED;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param appID the application to be used to do the job
     */

    public Job(RunnableAppID appID)
    {
	this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = appID;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a potentially parallelizable job. 
     * @param appID the application to be used to do the job
     * @param parallelizable set <code>true</code> if this job if independent 
     * from its parent and sibling jobs, so that it can run on a separate thread
     */

    public Job(RunnableAppID appID, boolean parallelizable)
    {
	this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = appID;
        this.parallelizable = parallelizable;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a master job. A master job has the possibility of 
     * distributiong its subjobs on multiple threads, if the subjobs are
     * parallelizable.
     * @param appID the application to be used to do the job
     * @param nThreads max parallel threads for independent sub-jobs
     */

    public Job(RunnableAppID appID, int nThreads)
    {
	this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = appID;
        this.nThreads = nThreads;
    }

//------------------------------------------------------------------------------

    /**
     * Set this job parameters
     * @param params the new set of parameters
     */

    public void setParameters(ParameterStorage params)
    {
        this.params = params;
    }

//------------------------------------------------------------------------------

    /**
     * Get this job parameters
     * @return the parameters
     */

    public ParameterStorage getParameters()
    {
        return params;
    }

//------------------------------------------------------------------------------

    /**
     * Get a specific parameters
     * @param paramId the parameter identified
     * @return the parameter
     */

    public Parameter getParameter(String paramId)
    {
        return params.getParameterOrNull(paramId);
    }

//------------------------------------------------------------------------------

    /**
     * Set this job as parallelizable
     * @param flag set to <code>true</code> to allow parallelization
     */
    
    public void setParallelizable(boolean flag)
    {
        this.parallelizable = flag;
    }

//------------------------------------------------------------------------------

    /**
     * Set the number of threads to be used to parallelize sub-jobs
     * @param n number of threads
     */

    public void setNumberOfThreads(int n)
    {
        this.nThreads = n;
    }

//------------------------------------------------------------------------------

    /**
     * Add a single step or sub-Job to this Job. 
     * The new step is appended after
     * all previously existing steps.
     * @param step the new step to be added
     */

    public void addStep(Job step)
    {
        steps.add(step);
    }

//------------------------------------------------------------------------------

    /**
     * Get a specific step in this Job
     * @param i the index of the step (0 to n-1)
     * @return the given step
     */

    public Job getStep(int i)
    {
        if (i > steps.size())
        {
            Terminator.withMsgAndStatus("ERROR! Trying to get step number " + i
                                    + " in a job that has only " + steps.size()
                                                                + " steps.",-1);
        }
        return steps.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of steps (i.e., tasks) of this Job
     * @return the number of steps
     */

    public int getNumberOfSteps()
    {
        return steps.size();
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of steps steps (i.e., tasks) of this Job
     * @return the steps
     */

    public ArrayList<Job> getSteps()
    {
        return steps;
    }

//------------------------------------------------------------------------------

    /**
     * Returns parallelizable flag
     * @return <code>true</code> if this job can be parallelized
     */
    public boolean isParallelizable()
    {
        return parallelizable;
    }

//-----------------------------------------------------------------------------

    /**
     * Check if all the subjobs are parallelizable
     * @return true is subjobs are all parallelizable
     */

    public boolean parallelizableSubJobs()
    {
        boolean res = true;
        for (Job j : steps)
        {
            res = res && j.isParallelizable();
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Return the enum identifier of the application used to do this job.
     * @return the enum identifier
     */

    public RunnableAppID getAppName()
    {
        return appID;
    }

//------------------------------------------------------------------------------

    /**
     * Method that runs this job and its sub-jobs. This is the only method
     * that can label this job as 'completed'.
     * Also, this method is only implemented in the super-class. 
     * Sub-classes might overwrite 
     * {@Link #runSubJobsPararelly} and {@Link runSubJobsSequentially}.
     */

    @Override
    public void run()
    {
        // First do the work of this very Job
        runThisJobSubClassSpecific();
        // Then, finally run the sub-jobs
        if (nThreads > 1 && parallelizableSubJobs())
        {
            //Parallel execution of sub-jobs
            runSubJobsPararelly();
        }
        else
        {
            //Serial execution
            runSubJobsSequentially();
        }
        completed = true;
    }

//------------------------------------------------------------------------------

    /**
     * Tries to do the wor of this job. Does not consider the subjobs
     * This method is overwritten by subclasses.
     */

    public void runThisJobSubClassSpecific()
    {
        if (appID.equals(Job.RunnableAppID.ACC))
        {
            //TODO del: only for tesing
            System.out.println("Running ACCJob " + this.toString());
        }
        else
        {
            // External app's jobs overwrites this method, so if we are here
            // it is because we tried to run a job of an app for which there is
            // no implementation of app-specific Job yet.
            Terminator.withMsgAndStatus("ERROR! Cannot (yet) run Jobs for App " 
                                                              + this.appID, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Runs all the sub-jobs sequentially.
     * This method is overwritten by subclasses.
     */

    public void runSubJobsSequentially()
    {
        for (Job j : steps)
        {
            j.run();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Runs all the sub-jobs in an embarassingly parallel fashon.
     * This method is overwritten by subclasses.
     */

    public void runSubJobsPararelly()
    {
        ParallelRunner parallRun = new ParallelRunner(steps,nThreads,nThreads);
        parallRun.start();
    }

//------------------------------------------------------------------------------

    /**
     * Reports is an exception was thrown by the run methods.
     * This is part of the mechanism to catch exceptions from tun.
     * @return <code>true</code> if running this job has returned an exception
     */

    public boolean foundException()
    {
        return hasException;
    }

//------------------------------------------------------------------------------

    /**
     * Get the exception that was thrown by the run methods.
     * This is part of the mechanism to catch exceptions from tun.
     * @return the exception thrown within the run method.
     */

    public Throwable getException()
    {
        return thrownExc;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if the job has been completed
     */

    public boolean isCompleted()
    {
        return completed;
    }

//------------------------------------------------------------------------------

    /**
     * Stop the Job if not already completed
     */

    public void stopJob()
    {
        if (completed)
        {
            return;
        }
	this.jobIsBeingKilled = true;
        Thread.currentThread().interrupt();
    }

//------------------------------------------------------------------------------

    /**
     * Produced the text input. The text input is meant for a text file
     * that a specific application can read and use to run the job. If the 
     * application is the autocompchem, then the jobDetails format is used
     * @return the list of lines ready to print a text input file
     */

    public ArrayList<String> toLinesInput()
    {
        return toLinesJobDetails();
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this job following the format of
     * autocompchem's JobDetail text file.
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines= new ArrayList<String>();
        for (int step = 0; step<steps.size(); step++)
        {
            //Write job-separator
            if (step != 0)
            {
                lines.add(stepSeparatorJd);
            }

            lines.addAll(getStep(step).toLinesJobDetails());
        }
        return lines;
    }

//------------------------------------------------------------------------------

}