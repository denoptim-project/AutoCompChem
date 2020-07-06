package autocompchem.run;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;


/**
 * A job is a piece of computational work to be done
 *
 * @author Marco Foscato
 */

public class Job implements Runnable
{
    /**
     * Container for parameters fed to this job. 
     * Typically contains initial settings, pathnames 
     * and configurations that are not default for a job.
     */
    protected ParameterStorage params;

    /**
     * List of steps. Steps are jobs nested in this very job. Each step can,
     * therefore, have further nesting levels.
     */
    protected ArrayList<Job> steps;

    /**
     * Application meant to do the job
     */
    protected RunnableAppID appID;

    /**
     * Known apps for performing jobs
     */
    public enum RunnableAppID {
        UNDEFINED,
        SHELL,
        ACC;
    
    	public String toString() {
    		switch (this) 
    		{
				case UNDEFINED: {
					return "UNDEFINED";
				}
				case SHELL: {
					return "SHELL";
				}	
				case ACC: {
					return "ACC";
				}
				default: {
					return "UNDEFINED";
				}
			}
    	}
    };

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
     * Flag signalling that this job has thrown an exception
     */
    protected boolean hasException = false;

    /**
     * Exception thrown by this job.
     */
    protected Throwable thrownExc;

    /**
     * Flag signalling the completion of this job
     */
    private boolean completed = false;

    /**
     * Flag signalling an action intended to kill this job
     */
    protected boolean jobIsBeingKilled = false;
    
    /**
     * Custom working directory
     */
    protected File customUserDir;
    
    /**
     * Flag controlling redirect of STDOUT and STDERR
     */
    protected Boolean redirectOutErr = false;
    
    /**
     * File where STDOUT is redirected
     */
    protected File stdout;
    
    /**
     * File where STDERR is redirected
     */
    protected File stderr;
    
    /**
     * File separator on this OS
     */
    private final String SEP = System.getProperty("file.separator");
    
    /**
     * Container for any kind of output that is made available to the outside
     * world /w.r.t. this job) via the {@Link #getOutput} method. 
     * We say these data is "exposed".
     */
    protected NamedDataCollector exposedOutput = new NamedDataCollector();
    
    /**
     * Verbosity level: amount of logging from this jobs
     */
    private int verbosity = 0;
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an undefined job
     */

    public Job()
    {
        this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = RunnableAppID.UNDEFINED;
    }

//------------------------------------------------------------------------------

    /**
     * Get the kind of app meant to perform this job
     * @return the enum representing the application.
     */

    public RunnableAppID getAppID()
    {
        return appID;
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
     * Sets the directory from which the job should be executed.
     * @param customUserDir the new directory
     */
    public void setUserDir(File customUserDir)
    {
    	this.customUserDir = customUserDir;
    	updateStdoutStdErr();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set redirection of STDOUT and STDERR to job specific files. The pathnames
     * are collected among the exposed output of the job.
     * @param redirectOutErr if <code>True</code> the nSTDOUT and STDERR will
     * be redirected to job specific files.
     */
    
    public void setRedirectOutErr(Boolean redirectOutErr)
    {
    	this.redirectOutErr = redirectOutErr;
    	if (redirectOutErr)
    	{
    		updateStdoutStdErr();
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Updates the pathnames of the files where this job redirects the stdout
     * and stderr.
     */
    
    private void updateStdoutStdErr()
    {
        //TODO: replace with unique ID: atomInteger for all jobs
        
        int hc = this.hashCode();
        
        String dir;
        if (customUserDir != null)
        {
        	dir = customUserDir.getAbsolutePath();
        }
        else
        {
        	dir = System.getProperty("user.dir");
        }
        
        stdout = new File(dir + SEP + "Job" +hc+".log");
        stderr = new File(dir + SEP + "Job" +hc+".err");
        exposedOutput.putNamedData( 
        		new NamedData("LOG", NamedDataType.FILE,stdout));
        exposedOutput.putNamedData( 
        		new NamedData("ERR", NamedDataType.FILE,stderr));
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
     * Get the level of detail for logging
     */
    
    public int getVerbosity()
    {
    	return verbosity;
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

    public void run()
    {
        // First do the work of this very Job
        runThisJobSubClassSpecific();
        
        // Then, run the sub-jobs
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
     * Tries to do the work of this very Job. Does not consider the subjobs
     * This method is overwritten by subclasses.
     */

    public void runThisJobSubClassSpecific()
    {
        // Subclasses overwrites this method, so if we are here
        // it is because we tried to run a job of an app for which there is
        // no implementation of app-specific Job yet.
        Terminator.withMsgAndStatus("ERROR! Cannot (yet) run Jobs for App '" 
                    + this.appID + "'. No subclass implementation of method "
                    + "running this job.", -1);
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
     * Runs all the sub-jobs in an embarrassingly parallel fashion.
     * This method is overwritten by subclasses.
     */

    public void runSubJobsPararelly()
    {
        ParallelRunner parallRun = new ParallelRunner(steps,nThreads,nThreads);
        parallRun.setVerbosity(verbosity);
        parallRun.start();
    }

    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collector of output data
     * @return the collector of output data
     */
    public NamedDataCollector getOutputCollector()
    {
    	return exposedOutput;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of all exposed output data.
     * @return the collection of all exposed output data.
     */
    public Collection<NamedData> getOutputDataSet()
    {
    	return exposedOutput.getAllNamedData().values();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the set of reference names used to identify any exposed output 
     * data.
     * @return the the set of reference names used to identify any exposed 
     * data.
     */
    
    public Set<String> getOutputRefSet()
    {
    	return exposedOutput.getAllNamedData().keySet();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the exposed output data identified by the given reference name.
     * @param refName the reference name identifying the data to fetch.
     * @return the exposed output data structure or null if no such data is
     * available.
     */
    
    public NamedData getOutput(String refName)
    {
    	return exposedOutput.getNamedData(refName);
    }

//------------------------------------------------------------------------------

    /**
     * Reports is an exception was thrown by the run methods.
     * This is part of the mechanism to catch exceptions.
     * @return <code>true</code> if running this job has returned an exception
     */

    public boolean foundException()
    {
        return hasException;
    }

//------------------------------------------------------------------------------

    /**
     * Get the exception that was thrown by the run methods.
     * This is part of the mechanism to catch exceptions.
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
     * that a specific application can read and use to run the job. 
     * This method is overwritten by subclasses, or, if this native method
     * is called, then the jobDetails format is used.
     * @return the list of lines ready to print a text input file.
     */

    public ArrayList<String> toLinesInput()
    {
        return toLinesJobDetails();
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this job following the format of
     * autocompchem's JobDetail text file.
     * @return the list of lines ready to print a jobDetails file.
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines= new ArrayList<String>();
        lines.add(ParameterConstants.STARTJOB);
        lines.addAll(params.toLinesJobDetails());
        for (int step = 0; step<steps.size(); step++)
        {
            lines.addAll(getStep(step).toLinesJobDetails());
        }
        lines.add(ParameterConstants.ENDJOB);
        return lines;
    }

//------------------------------------------------------------------------------

}
