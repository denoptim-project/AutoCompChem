package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
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

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.log.LogUtils;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.jobediting.Action;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.Directive;


/**
 * A job is a piece of computational work to be done
 *
 * @author Marco Foscato
 */

public class Job implements Runnable
{
	/**
	 * Reference to the parent job, i.e., a job that had to create this one to
	 * take case of some specific task.
	 */
	private Job parentJob = null;
	
	/**
	 * Reference to any child job. Child jobs are NOT steps of this job, but are
	 * jobs started by this job to perform specific tasks.
	 * Reference to a child job is volatile: it is removed once the child job
	 * is completed.
	 */
	private Set<Job> childJobs = new HashSet<Job>();

	/**
	 * Reference to the job that contains this one as a step. 
	 * This is null for the outermost, master job.
	 */
	private Job containerJob = null;

    /**
     * List of steps. Steps are jobs nested in this very job. Each step can,
     * therefore, have further nesting levels. Steps are long lived, i.e., they
     * remain part of this workflow (is this job runs them in serial fashion) 
     * or batch (if this jog runs them in parallel fashion) even when they are 
     * completed and not running anymore. They can, however, be removed if 
     * the workflow or batch is edited.
     */
    protected List<Job> steps;
	
	/**
	 * A job identifier meant to be unique only within sibling jobs, i.e., jobs
	 * that belong to the same job container.
	 */
	protected int jobId = 0;
	
	/**
	 * An integer derived from the hashcode of this instance, but that is only a
	 * snapshot of the hashcode at construction time. This is used to 
	 * distinguish jobs that are nor related, i.e., are 
	 * not contained by the same container, nor 
	 * do they belong to the same family tree (not connected by parent-child 
	 * relations).
	 */
	private int jobHashCode;
	
	/**
	 * Counter for subjobs
	 */
	private AtomicInteger idSubJob = new AtomicInteger(1);
	
    /**
     * Restart counter. counts how many times this job is restarted
     */
    private final AtomicInteger restartCounter = new AtomicInteger();
	
    /**
     * Container for parameters fed to this job. 
     * Typically contains initial settings, pathnames 
     * and configurations that are not default for a job.
     */
    protected ParameterStorage params;

    /**
     * Application meant to do the job
     */
    protected AppID appID;

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
     * A listener that can hear notifications from this job (i.e., observer). 
     * Typically, the listener is a master job or the manager of parallel jobs.
     */
    protected JobNotificationListener observer;

    /**
     * Flag signaling that this job has been interrupted
     */
    protected boolean isInterrupted = false;
    
    /**
     * Flag signaling that this job has thrown an exception
     */
    protected boolean hasException = false;

    /**
     * Exception thrown by this job.
     */
    protected Throwable thrownExc;
    
    /**
     * Flag signaling that this job had been started.
     */
    private boolean started = false;

    /**
     * Flag signaling the completion of this job
     */
    private boolean completed = false;

    /**
     * Flag signaling an action intended to kill this job
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
    private static final String SEP = System.getProperty("file.separator");
    
    /**
     * Container for any kind of output that is made available to the outside
     * world /w.r.t. this job) via the {@link #getOutput} method. 
     * We say these data is "exposed".
     */
    public NamedDataCollector exposedOutput = new NamedDataCollector();
    
    /**
     * Logger
     */
    protected Logger logger;
    
	/**
	 * The string used to identify the data holding a action requested by a
	 * sub-job.
	 */
	public static final String ACTIONREQUESTBYSUBJOB = 
			"ActionRequestedByChild";
	
	/**
	 * The string used to identify the data holding the sub-job that requested 
	 * an action.
	 */
	public static final String SUBJOBREQUESTINGACTION = 
			"SubJobRequestingAction";
	
	/**
	 * Name of JSON element used to discriminate among implementations of 
	 * {@link Job}.
	 */
	public static final String JSONJOBTYPE = "jobType"; 
	
	/**
	 * Name of JSON element collecting the parameters given to this job.
	 */
	public static final String JSONPARAMS = "params";
	
	/**
	 * Name of JSON property collecting the jobs embedded in this one, i.e.,
	 * the steps or child jobs.
	 */
	public static final String JSONSUBJOBS = "steps";
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an undefined job.
     */
    protected Job()
    {
    	logger = LogManager.getLogger(this.getClass());
        this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = AppID.UNDEFINED;
        this.jobHashCode = hashCode();
    }

//------------------------------------------------------------------------------

    /**
     * Get the kind of app meant to perform this job
     * @return the enum representing the application.
     */

    public AppID getAppID()
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

        if (params.contains(ParameterConstants.VERBOSITY))
        {
        	processVerbosity(params.getParameter(
                    ChemSoftConstants.PARVERBOSITY));
        }
    }
    
//------------------------------------------------------------------------------
    
    private void processVerbosity(NamedData param)
    {
        String str = param.getValueAsString();
        if (!NumberUtils.isNumber(str))
		{
			Terminator.withMsgAndStatus("ERROR! Value '" + str + "' "
					+ "cannot be converted to an integer. Check parameter "
					+ ParameterConstants.VERBOSITY, -1);
		}
        Configurator.setLevel(logger.getName(), 
        		LogUtils.verbosityToLevel(Integer.parseInt(str)));
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

    public NamedData getParameter(String paramId)
    {
        return params.getParameterOrNull(paramId);
    }
    
//------------------------------------------------------------------------------

    /**
     * Get the value of a specific parameters
     * @param paramId the parameter identified
     * @return the parameter's value
     */

    public Object getParameterValue(String paramId)
    {
        return params.getParameterOrNull(paramId);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a value-less parameters (i.e., a keyword)
     * @param ref the reference name of the parameter to add/set.
     */

    public void setParameter(String ref)
    {
        setParameter(ref, NamedDataType.UNDEFINED, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameters.
     * @param ref the reference name of the parameter to add/set.
     * @param value the value of the parameter.
     */

    public void setParameter(String ref, String value)
    {
        setParameter(ref, NamedDataType.STRING, value);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameters.
     * @param ref the reference name of the parameter to add/set.
     * @param value the value of the parameter.
     * @param recursive use <code>true</code> to set the parameter in this job
     * and in any of its steps (i.e., first layer or embedded jobs) or any
     * further embedding level recursively.
     */

    public void setParameter(String ref, String value, boolean recursive)
    {
        setParameter(ref, NamedDataType.STRING, value, recursive);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameters.
     * @param ref the reference name of the parameter to add/set.
     * @param type the type of the parameter
     * @param value the value of the parameter.
     */
    public void setParameter(String ref, NamedDataType type, Object value)
    {
        setParameter(ref, type, value, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameter.
     * @param ref the reference name of the parameter to add/set.
     * @param type the type of the parameter
     * @param value the value of the parameter.
     * @param recursive use <code>true</code> to set the parameter in this job
     * and in any of its steps (i.e., first layer or embedded jobs) or any
     * further embedding level recursively.
     */

    public void setParameter(String ref, NamedDataType type, Object value, 
    		boolean recursive)
    {
        NamedData param = new NamedData(ref.toUpperCase(), type, value);
    	setParameter(param, recursive);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameter. It a parameter with the same reference name already
     * exists it will be overwritten.
     * @param param the parameter to add or overwrite
     */
    public void setParameter(NamedData param)
    {
    	setParameter(param, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameter. It a parameter with the same reference name already
     * exists it will be overwritten.
     * @param param the parameter to add or overwrite.
     * @param recursive use <code>true</code> to set the parameter in this job
     * and in any of its steps (i.e., first layer or embedded jobs) or any
     * further embedding level recursively.
     */
    public void setParameter(NamedData param, boolean recursive)
    {
    	params.setParameter(param);
        if (param.getReference().equals(ParameterConstants.VERBOSITY))
        {
        	processVerbosity(param);
        }
        if (recursive)
        {
	        for (Job step : steps)
	        {
	        	step.setParameter(param);
	        }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if a parameter has been set.
     * @param refName the reference name of the parameter.
     * @return <code>true</code if the parameter exists, of <code>false</code>
     * if it is not set or if the parameter storage is null.
     */

    public boolean hasParameter(String refName)
    {
    	if (params != null)
    	{
    		return params.contains(refName);
    	}
        return false;
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
     * Sets a listener/observer, i.e., a class that can listen to notifications 
     * sent out by this job.
     * @param observer the observer
     */

    public void setJobNotificationListener(JobNotificationListener listener)
    {
    	this.observer = listener;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the observer that watches for notifications from this job.
     * @return the listener to notifications from this job.
     */
    public JobNotificationListener getObserver() 
    {
		return observer;
	}

//------------------------------------------------------------------------------
    
	/**
	 * @return the STDOUT of this job
     */
    public File getStdOut()
    {
    	return stdout;
    }
    
//------------------------------------------------------------------------------
      
  	/**
  	 * @return the STDERR of this job
     */
    public File getStdErr()
    {
      	return stderr;
    }
    
//------------------------------------------------------------------------------
    
  	/**
  	 * Gets the main folder (user dir) of this job.
  	 * This is not guaranteed to be the same as the UserDir for the JAVA virtual 
  	 * machine. Instead, it is a path that may have been set for this job.
  	 * @return the the pathname to the main folder (user dir) of this job.
     */
    public File getUserDir()
    {
      	return customUserDir;
    }
      
//------------------------------------------------------------------------------
    
	/**
     * Sets the directory from which the job should be executed.
     * @param customUserDir the new directory
     */
    public void setUserDir(File customUserDir)
    {
    	this.customUserDir = customUserDir;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the directory from which the job should be executed and assigns
     * values to STDERR and STDOUT accordingly.
     * @param customUserDir the new directory
     */
    public void setUserDirAndStdFiles(File customUserDir)
    {
    	setUserDir(customUserDir);
    	updateStdoutStdErr();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set redirection of STDOUT and STDERR to job specific files. The pathnames
     * are collected among the exposed output of the job.
     * @param redirectOutErr if <code>True</code> the STDOUT and STDERR will
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
        String dir;
        if (customUserDir != null)
        {
        	dir = customUserDir.getAbsolutePath();
        }
        else
        {
        	dir = System.getProperty("user.dir");
        }
        
        if (containerJob!=null)
        {
        	if (stdout==null)
        		stdout = new File(dir + SEP + "Job" + getId() +".log");
	        if (stderr==null)
	        	stderr = new File(dir + SEP + "Job" + getId() +".err");
        } else {
        	if (stdout==null)
        		stdout = new File(dir + SEP + "Job" + jobHashCode +".log");
        	if (stderr==null)
        		stderr = new File(dir + SEP + "Job" + jobHashCode +".err");
        }
        exposedOutput.putNamedData(
        		new NamedData("LOG", NamedDataType.FILE, stdout));
        exposedOutput.putNamedData(
        		new NamedData("ERR", NamedDataType.FILE, stderr));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the counter of restarts of this job.
     */
    public AtomicInteger getRestartCounter()
    {
    	return restartCounter;
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
    	step.setContainer(this);
    	step.setId(idSubJob);
        steps.add(step);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the reference to the job that contains this one.
     */
    
    private void setContainer(Job container)
    {
    	this.containerJob = container;
    	updateStdoutStdErr();
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the reference to the job that contains this one among its steps.
     * @return the containing job.
     */
    
    public Job getContainer()
    {
    	return containerJob;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if there is a container job. 
     * @return <code>true</code> if this job is contained in a workflow or batch
     * of jobs.
     */
    public boolean hasContainer()
    {
    	return containerJob != null;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the reference to the parent job, which is the job that contains
     * this one.
     */
    
    private void setParent(Job parentJob)
    {
    	this.parentJob = parentJob;
    	updateStdoutStdErr();
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the reference to a child job, which is a job that has been created
     * by this one to perform a task. Child jobs are NOT steps in this job.
     */
    
    public void addChild(Job childJob)
    {
    	this.childJobs.add(childJob);
    	childJob.setParent(this);
    }
    
//------------------------------------------------------------------------------

    /**
     * Remove the reference to a child job, which is a job that has been created
     * by this one to perform a task. Child jobs are NOT steps in this job.
     */
    
    public void removeChild(Job childJob)
    {
    	childJob.parentJob = null;
    	this.childJobs.remove(childJob);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the reference to the parent job, which is the job that launched 
     * this one for taking case of a task not defined among its job steps.
     * @return the parent job.
     */
    
    public Job getParent()
    {
    	return parentJob;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if there is a parent job. 
     * @return <code>true</code> if this job has a parent.
     */
    public boolean hasParent()
    {
    	return parentJob != null;
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
            		+ " in a job that has only " + steps.size() + " steps.",-1);
        }
        return steps.get(i);
    }
    
//------------------------------------------------------------------------------

    /**
     * Recursively searches for the innermost first step. At any level of 
     * recursion it does not consider steps other than the first.
     * @return the job that is innermost and that is the first step of a first
     * N*(of a first step) with N from 0 to +infinite.
     */
  	public Job getInnermostFirstStep() 
  	{
  		if (this.getNumberOfSteps()>0)
  			return steps.get(0).getInnermostFirstStep();
  		return this;
  	}
   
//------------------------------------------------------------------------------
    
    /**
     * Returns the hash code of this job. This is a sort of identifier but it is 
     * not guaranteed to be unique. It most often is, but there is no guarantee.
     * See {@link Object#hashCode()}.
     * @return the hash code at construction time.
     */
    
    public int getHashCodeSnapshot()
    {
    	return jobHashCode;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the identifier of this job. This identifier is unique only within
     * the context of a single job container, i.e., all sub jobs have unique ID.
     * The ID differs is this job is 
     * a child (a job made by a parent job to take care of a task), or
     * a step in the workflow or batch defined by a job container.
     * @return the string identifying this jobs
     */
    
    public String getId()
    {
    	String idStr = "";
    	// NB: a job should have either a parent or a container, but not both
    	if (containerJob != null)
    	{
    		if (parentJob != null)
        	{
    			Terminator.withMsgAndStatus("A Job with both parent and "
    					+ "container should not exist. Check job " + this, -1);
        	}
    		idStr = containerJob.getId() + "." + jobId;
    	} 
    	else if (parentJob != null)
    	{
    		idStr = parentJob.getId() + "." + jobId;
    	} 
    	else 
    	{
    		idStr = "#" + jobId;
    	}
    	return idStr;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the identifier of this job.
     */
    private void setId(AtomicInteger idSrc)
    {

    	jobId = idSrc.getAndIncrement();
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

    public List<Job> getSteps()
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
     * Method that runs this job and its sub-jobs. This is the only method
     * that can label this job as 'completed'.
     * Also, this method is only implemented in the super-class, and 
     * Sub-classed cannot overwrite it. Instead, sub-classes can overwrite 
     * {@link runThisJobSubClassSpecific()}, which is called by {@link run()}.
     */

    public final void run()
    {
    	started = true;
    	if (!appID.isRunnableByACC())
    	{
    		throw new Error("Cannot run " + appID + " Job (" + getId() + ") "
    				+ "from ACC. You must define a " + ShellJob.class.getName()
    				+ " or extend " + Job.class.getName() + ".");
    	}
    	
    	logger.info(System.getProperty("line.separator") 
    				+ "Initiating " + appID + " Job " + getId());
    	
        // First do the work of this very Job
        runThisJobSubClassSpecific();
        
        // Then, run the sub-jobs (steps)
        if (steps.size()>0)
        {
	        if (runsParallelSubjobs())
	        {
	            //Parallel execution of sub-jobs
	            runSubJobsParallely();
	        }
	        else
	        {
	            //Serial execution
	            runSubJobsSequentially();
	        }
        }
        
        finalizeStatusAndNotifications(!jobIsBeingKilled);
    }

//------------------------------------------------------------------------------

    /**
     * Defines the conditions that makes this jobs use a parallel or serial
     * job execution service for the subjobs (i.e., the steps) defined within
     * this jobs.
     * @return <code>true</code> is this job would use a parallelized job 
     * runner.
     */
    public boolean runsParallelSubjobs() {
		return nThreads > 1 && parallelizableSubJobs();
	}

//------------------------------------------------------------------------------


	/**
     * Tries to do the work of this very Job. Does not consider the sub jobs
     * This method is overwritten by subclasses.
     */

    protected void runThisJobSubClassSpecific()
    {
        // Subclasses overwrites this method, so if we are here
        // it is because we tried to run a job of an app for which there is
        // no implementation of app-specific Job yet.
        Terminator.withMsgAndStatus("ERROR! Cannot (yet) run Jobs for App '" 
                    + appID + "'. No subclass implementation of method "
                    + "running this job.", -1);
    }

//------------------------------------------------------------------------------

    /**
     * Runs all the sub-jobs sequentially.
     */

    private void runSubJobsSequentially()
    {
        SerialJobsRunner serialRun = new SerialJobsRunner(this);
        if (hasParameter(JobsRunner.WALLTIMEPARAM))
        {
        	serialRun.setWallTime(Long.parseLong(
        			params.getParameterValue(JobsRunner.WALLTIMEPARAM)));
        }
        if (hasParameter(JobsRunner.WAITTIMEPARAM))
        {
        	serialRun.setWaitingStep(Long.parseLong(
        			params.getParameterValue(JobsRunner.WAITTIMEPARAM)));
        }
        serialRun.start();
    }

//------------------------------------------------------------------------------

    /**
     * Runs all the sub-jobs in an embarrassingly parallel fashion.
     */

    private void runSubJobsParallely()
    {
        ParallelJobsRunner parallRun = 
        		new ParallelJobsRunner(nThreads, nThreads, this);
        if (hasParameter(JobsRunner.WALLTIMEPARAM))
        {
        	parallRun.setWallTime(Long.parseLong(
        			params.getParameterValue(JobsRunner.WALLTIMEPARAM)));
        }
        if (hasParameter(JobsRunner.WAITTIMEPARAM))
        {
        	parallRun.setWaitingStep(Long.parseLong(
        			params.getParameterValue(JobsRunner.WAITTIMEPARAM)));
        }
        parallRun.start();
    }
    
//------------------------------------------------------------------------------

    /**
     * Sends this job to an executing thread managed by an existing, and
     * pre-started execution service. This method is overwritten by subclasses 
     * that need special kinds of execution. For example, see 
     * {@link MonitoringJob}.
     * @param executor the execution service.
     * @return a Future representing pending completion of the task.
     */
    
  	@SuppressWarnings("unchecked")
	protected Future<Object> submitThread(ExecutorService executor) {
  		return (Future<Object>) executor.submit(this);
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
     * Set the flag signaling that the execution of this job was interrupted
     * @param flag set to <code>true</code> to flag this job as interrupted.
     */
    
    public void setInterrupted(boolean flag)
    {
    	this.isInterrupted = flag;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns <code>true</code> if the execution of this job was interrupted.
     * @return <code>true</code> if the execution of this job was interrupted.
     */
    
    public boolean isInterrupted()
    {
    	return isInterrupted;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if the job has been completed.
     */

    public boolean isCompleted()
    {
        return completed;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if the job has been started.
     */

    public boolean isStarted()
    {
        return started;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This method resets any information about the running of this job so that
     * it looks as if it had never run.
     */
    public void resetRunStatus()
    {
    	started = false;
    	completed = false;
    	jobIsBeingKilled = false;
    	isInterrupted = false;
    	hasException = false;
    	thrownExc = null;
    	exposedOutput.clear();
    }

//------------------------------------------------------------------------------

    /**
     * Stop the Job if not already completed
     */

    public void stopJob()
    {
        if (completed)
        {
        	//This avoid looking with finalizeStatusAndNotifications sending
        	// a notification, and the notification triggering calling stopJob
            return;
        }
        this.jobIsBeingKilled = true;
        finalizeStatusAndNotifications(false);
        Thread.currentThread().interrupt();
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the status to 'complete' and notifies any listener that might be 
     * listening to this job.
     */
    private void finalizeStatusAndNotifications(boolean notify) 
    {
    	completed = true;
    	
    	// Handling of exceptions
    	for (Job step : steps)
    	{
            if (step.foundException())
            {
            	throw new Error(step.thrownExc);
            }
    	}
    	
    	// Notify observer of any request from this jobs
    	if (notify)
    	{
    		notifyObserver();
    	}
    	
    	// We do this to liberate memory
    	if (hasParent())
    	{
    		parentJob.removeChild(this);
    	}
    	
    	// final check for 
    	if (hasException)
    	{
    		throw new Error("ERROR! " + thrownExc.getClass().getSimpleName()
    				+ " thrown by job " + getId() + ".", thrownExc);
    	}			
    }

//------------------------------------------------------------------------------

    /**
     * Manager the notification to the {@link JobNotificationListener}
     * that might be associated with this job. Subclasses overwrite this method
     * to adjust the behavior to their needs.
     */
    protected void notifyObserver()
    {
    	if (observer==null)
    		return;
    	observer.notifyTermination(this);
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this job following the format of
     * autocompchem's Parameters text file.
     * @return the list of lines ready to print a parameters' file.
     */

    public List<String> toLinesJobParameters()
    {
        List<String> lines= new ArrayList<String>();
        lines.add(ParameterConstants.STARTJOB);
        lines.addAll(params.toLinesJobDetails());
        for (int step = 0; step<steps.size(); step++)
        {
            lines.addAll(getStep(step).toLinesJobParameters());
        }
        lines.add(ParameterConstants.ENDJOB);
        return lines;
    }
    
//------------------------------------------------------------------------------
    
   @Override
   public boolean equals(Object o) 
   {
	   if (o == this)
		   return true;
       
       if (o.getClass() != getClass())
           return false;
	   
	   Job other = (Job) o;
	   
	   return this.jobId == other.jobId 
			   && this.appID == other.appID
			   && this.parallelizable == other.parallelizable
			   && this.nThreads == other.nThreads
			   && this.isInterrupted == other.isInterrupted 
			   && this.hasException == other.hasException 
			   && this.completed == other.completed 
			   && Objects.equals(this.customUserDir, other.customUserDir)
			   && this.redirectOutErr == other.redirectOutErr
			   && Objects.equals(this.stdout, other.stdout)
			   && Objects.equals(this.stderr, other.stderr)
			   && Objects.equals(this.params, other.params)
			   && Objects.equals(this.steps, other.steps)
			   && Objects.equals(this.exposedOutput, other.exposedOutput);
   }
    
//------------------------------------------------------------------------------

    public static class JobSerializer 
    implements JsonSerializer<Job>
    {
        @Override
        public JsonElement serialize(Job job, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONJOBTYPE, job.getClass().getSimpleName());
            
            if (!job.params.isEmpty())
            	jsonObject.add(JSONPARAMS, context.serialize(job.params));
            if (!job.steps.isEmpty())
            	jsonObject.add(JSONSUBJOBS, context.serialize(job.steps));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class JobDeserializer 
    implements JsonDeserializer<Job>
    {
        @Override
        public Job deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();
            
            if (!jsonObject.has(JSONJOBTYPE))
            {
                String msg = "Missing '" + JSONJOBTYPE + "': found a "
                        + "JSON string that cannot be converted into a Job "
                        + "subclass.";
                throw new JsonParseException(msg);
            }       

            String typ = context.deserialize(jsonObject.get(JSONJOBTYPE),
                    String.class);
            
            Job job = null;
            switch (typ)
            {
                case "Job":
                {
                	job = new Job();
                	break;
                }
                
                case "ACCJob":
                {
                	job = new ACCJob();
                	break;
                }
                
                case "EvaluationJob":
                {
                	job = new EvaluationJob();
                	break;
                }
                
                case "MonitoringJob":
                {
                	job = new MonitoringJob();
                	break;
                }
                
                case "ShellJob":
                {
                	if (jsonObject.has("command"))
                	{
                		List<String> cmd = context.deserialize(
                				jsonObject.get("command"), 
                				new TypeToken<ArrayList<String>>(){}.getType());
                		job = new ShellJob(cmd.toArray(new String[0]));
                	} else {
                		job = new ShellJob();
                	}
                	break;
                }
                
                case "CompChemJob":
                {
                	job = new CompChemJob();
                	if (jsonObject.has("directives"))
                	{
                		((CompChemJob)job).setDirectives(context.deserialize(
                    			jsonObject.get("directives"),
                    			new TypeToken<ArrayList<Directive>>(){}.getType()));
                	}
                	break;
                }
            }
            
        	if (jsonObject.has(JSONSUBJOBS))
        		job.steps = context.deserialize(jsonObject.get(JSONSUBJOBS),
                    new TypeToken<ArrayList<Job>>(){}.getType());
        	if (jsonObject.has(JSONPARAMS))
        		job.params = context.deserialize(jsonObject.get(JSONPARAMS),
        			ParameterStorage.class);
        	
        	// Reconstruct references to parent/child job
            for (Job step : job.steps)
            {
            	step.setContainer(job);
            	step.setId(job.idSubJob);
            }
        	
        	return job;
        }
    }
    
//------------------------------------------------------------------------------

}
