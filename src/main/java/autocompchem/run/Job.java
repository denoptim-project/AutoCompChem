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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.web.HttpSC.Code;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.google.gson.Gson;
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
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.log.LogUtils;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
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
    protected SoftwareId appID;

    /**
     * Number of parallel threads for sub-jobs. This controls whether we'll try
     * to run sub-jobs in parallel.
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
	
	/**
	 * String used to request accessing data exposed by jobs in the family tree
	 * of this job.
	 */
	public static final String GETACCJOBSDATA = "GETACCJOBSDATA";
	
	
//------------------------------------------------------------------------------

    /**
     * Constructor for an undefined job.
     */
    protected Job()
    {
    	logger = LogManager.getLogger(this.getClass());
        this.params = new ParameterStorage();
        this.steps = new ArrayList<Job>();
        this.appID = SoftwareId.UNDEFINED;
        this.jobHashCode = super.hashCode();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that may return a subclass
     */
    public Job makeInstance()
    {
    	return new Job();
    }

//------------------------------------------------------------------------------

    /**
     * Get the kind of app meant to perform this job
     * @return the enum representing the application.
     */

    public SoftwareId getAppID()
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
        	processVerbosity(params.getParameter(ParameterConstants.VERBOSITY));
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
        setParameter(ref, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameters.
     * @param ref the reference name of the parameter to add/set.
     * @param type the type of the parameter
     * @param value the value of the parameter.
     */
    public void setParameter(String ref, Object value)
    {
        setParameter(ref, value, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameter.
     * @param ref the reference name of the parameter to add/set.
     * @param value the value of the parameter.
     * @param recursive use <code>true</code> to set the parameter in this job
     * and in any of its steps (i.e., first layer or embedded jobs) or any
     * further embedding level recursively.
     */

    public void setParameter(String ref, Object value, boolean recursive)
    {
        NamedData param = new NamedData(ref.toUpperCase(), value);
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
        exposedOutput.putNamedData(new NamedData("LOG", stdout));
        exposedOutput.putNamedData(new NamedData("ERR", stderr));
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
     * @return the containing job or <code>null</code> if this job is not
     * contained.
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
     * Method that runs this job and its sub-jobs. This is the only method
     * that can label this job as 'completed'.
     * Also, this method is only implemented in the super-class, and 
     * Sub-classed cannot overwrite it. Instead, sub-classes can overwrite 
     * {@link runThisJobSubClassSpecific()}, which is called by {@link run()}.
     */

    public final void run()
    {
    	started = true;
    	
    	// NB: this line in the log is used to detect the beginning of a job's
    	// step in the log of ACC jobs.
    	logger.info(System.getProperty("line.separator") 
    				+ "Initiating " + appID + " Job " + getId());
    	
    	// Convert any request to fetch data from other jobs into the actual data
    	try {
    		fetchValuesFromJobsTree();
    	} catch (Throwable t) {
    		hasException = true;
    		thrownExc = t;
    		stopJob();
    	}
    	
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
		return nThreads > 1;
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
        Terminator.withMsgAndStatus("ERROR! Cannot run '" 
                    + appID + "' Jobs directy within AutocompChem. "
                    + "Provide an extension of '" + Job.class.getName() 
                    + "' that enables running this job from within "
                    + "AutoCompChem.", -1);
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
     * @return the exposed output data structure or <code>null</code> 
     * if no such data is available.
     */
    
    public NamedData getOutput(String refName)
    {
    	return exposedOutput.getNamedData(refName);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Extracts the value of any data stored in the exposed output collectors
     * reachable from the job tree relationships.
     * @param pathToJob this is pointer to a job relative to "this" job. 
     * The expected syntax is <code>#S.L.M.N....Z</code> where S is 0 or a 
     * negative integer, while L, ..., Z are strictly positive integers. 
     * The first integer, i.e., <code>S</code>,
     * indicates how many steps to move back in the chain of container jobs. 
     * The other integers indicate which step (0-based) to take among the steps 
     * of the job identified by the previous index. Negative values are, therefore,
     * permitted only for the first index and indicate how many levels relative 
     * to the present jobs to move to identify the outermost job from which
     * we start looking at the contained steps. A value of 0 indicated that 
     * 'this' very job is the one where to start looking into the steps.
     * @param pathIntoExposedData
     * @return the content of the requested data or <code>null</code> if the
     * given path cannot be satisfied by any content in the exposed data 
     * collection.
     */
    public Object getExposedData(String pathToJob, String[] pathIntoExposedData)
    {
    	// Take away the pound sign
    	if (pathToJob.startsWith("#"))
    		pathToJob = pathToJob.substring(1);
    	String[] parts = pathToJob.split("\\.");
    	int[] pathAsInts = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
        	pathAsInts[i] = Integer.parseInt(parts[i].stripLeading().stripTrailing());
        }
        return getExposedData(pathAsInts, pathIntoExposedData);
    }
    
//------------------------------------------------------------------------------
    /**
     * Extracts the value of any data stored in the exposed output collectors
     * reachable from the job tree relationships.
     * @param pathToJob list of pointers to jobs. The first one indicates how
     * many steps to take up along the chain of containing jobs. 
     * Zero indicated that
     * we look at a job that is contained (at any level) in this very job. 
     * Positive values take no effect as they are interpreted as a 0.
     * @param pathIntoExposedData
     * @return the content of the requested data or <code>null</code> if the
     * given path cannot be satisfied by any content in the exposed data 
     * collection.
     */
    public Object getExposedData(int[] pathToJob, String[] pathIntoExposedData)
    {
    	if (pathToJob.length<1)
    		return null;
    	
        if (pathToJob[0]<0)
        {
        	int[] newPath = new int[pathToJob.length];
        	newPath[0] = pathToJob[0]+1;
        	for (int i = 1; i < newPath.length; i++) {
        		newPath[i] = pathToJob[i];
            }
        	if (!this.hasContainer())
        	{
        		return null;
        	}
        	return this.containerJob.getExposedData(newPath, pathIntoExposedData);
        }
        
        // So, the job we look after is contained in this very job
        if (pathToJob.length>1)
        {
        	// We move into the first level of steps
        	// Note the indexes in pathToJob: 
        	// * 0 is this job, and we know it is not negative (0 or positive 
        	//   is the same) and we now get rid of this index in newPath.
        	// * 1 is index where we find the index of the step to look at, and
        	//   we get rid (change it to 0) of it because we just take the step
        	//   object. By setting to 0 we tell the step that he is the right 
        	//   job and not any of his containers.
        	// * anything above 1 is further nested and is kept in the newPath
        	int[] newPath = new int[pathToJob.length-1];
        	
        	// this indicated to the step that the target data is contained in it.
        	newPath[0] = 0;
        	
        	// deeper level are dealt with in future recursions 
        	for (int i = 2; i < newPath.length; i++) {
        		newPath[i-1] = pathToJob[i];
            }
        	
        	if (pathToJob[1]+1 > getNumberOfSteps())
        		return null;
        	
        	Job step = getStep(pathToJob[1]);
        	
        	return step.getExposedData(newPath, pathIntoExposedData);
        } else {
        	// The target job this job
        	return getExposedData(pathIntoExposedData);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Extracts the value of any data stored in the exposed output collector.
     * @param pathIntoExposedData the array of names/integers that allow navigating 
     * the data structure. Names (or integers reported as strings)
     * are expected for data containers that are
     * {@link NamedData}, {@link NamedDataCollector}, or {@link Map}s with 
     * strings as key. Strings representations of integers are needed for 
     * {@link List}s and {@link Map}s with integers as keys.
     * @return the content of the requested data or <code>null</code> if the
     * given path cannot be satisfied by any content in the exposed data 
     * collection.
     */
    public Object getExposedData(String[] pathIntoExposedData)
    {
    	Set<String> availableKeys = getOutputRefSet();
    	String contentName = pathIntoExposedData[0].stripLeading().stripTrailing();
    	if (!availableKeys.contains(contentName))
    		return null;
    	
    	NamedData data = getOutput(contentName);
    	Object value = data.getValue();
    	
    	for (int i=1; i<pathIntoExposedData.length; i++)
    	{
        	String nestedContentID = pathIntoExposedData[i].stripLeading().stripTrailing();
        	Object nestedValue = null;
        	if (value instanceof NamedData)
        	{
        		NamedData container = (NamedData) value;
        		if (!container.getReference().equals(nestedContentID))
        			return null;
        		nestedValue = container.getValue();
        	} else if (value instanceof NamedDataCollector)
	    	{
	    		NamedDataCollector container = (NamedDataCollector) value;
	    		if (!container.contains(nestedContentID))
	    			return null;
	    		nestedValue = container.getNamedData(nestedContentID).getValue();
	    	} else if (value instanceof Map) 
	    	{
	    		Map<?,?> map = (Map<?, ?>) value;
	    		nestedValue = null;
	    		for (Object key : map.keySet()) 
	    		{
	    		    if (key instanceof String) 
	    		    {
	    		        String strKey = (String) key;
	    		        if (strKey.equals(nestedContentID)) 
	    		        {
	    		        	nestedValue = map.get(key);
	    		        	break;
	    		        }
	    		    } else if (key instanceof Integer 
	    		    		&& NumberUtils.isParsableToInt(nestedContentID)) 
	    		    {
	    		        Integer intKey = (Integer) key;
	    		        if (intKey == Integer.parseInt(nestedContentID)) 
	    		        {
	    		        	nestedValue = map.get(key);
	    		        	break;
	    		        }
	    		    }
	    		}
	    	} else if (value instanceof List 
	    			&& NumberUtils.isParsableToInt(nestedContentID)) 
	    	{
	    		List<?> list = (List<?>) value;
	    		nestedValue = list.get(Integer.parseInt(nestedContentID));
	    	} else {
	    		// NB: there might be other types of containers to consider!
	    	}
        	value = nestedValue;
    	}
    	
    	return value;
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
        	//This avoid looping with finalizeStatusAndNotifications sending
        	// a notification, and the notification triggering calling stopJob
            return;
        }
        this.jobIsBeingKilled = true;
        finalizeStatusAndNotifications(true);
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
        lines.addAll(params.toLinesParametersFileFormat());
        for (int step = 0; step<steps.size(); step++)
        {
            lines.addAll(getStep(step).toLinesJobParameters());
        }
        lines.add(ParameterConstants.ENDJOB);
        return lines;
    }
    
//------------------------------------------------------------------------------
    
    protected void fetchValuesFromJobsTree()
    {    	Gson jsonWriter = ACCJson.getWriter();
    	Gson jsonReader = ACCJson.getReader();
    	for (String paramKey : params.getAllNamedData().keySet())
    	{
    		NamedData data = params.getAllNamedData().get(paramKey);
    		String jsonStr = jsonWriter.toJson(data);
    		List<Integer> indexes = new ArrayList<Integer>();
    		boolean edited = false;
    		int fromIdx=0;
    		while (fromIdx>-1)
    		{
    			fromIdx = jsonStr.toUpperCase().indexOf(GETACCJOBSDATA, fromIdx);
    			if (fromIdx>-1)
    			{
        			indexes.add(fromIdx);
    				edited = true;
    				fromIdx++;
    			}
    		}
    		StringBuilder newJson = new StringBuilder();
    		int maxEnd = jsonStr.length();
    		int startCopying = 0;
    		for (int i=0; i<indexes.size(); i++)
    		{
    			int beginMatch = indexes.get(i);
    			int end = maxEnd;
    			if (indexes.size()>i+1)
    				end = indexes.get(i+1);
    			String argStr = StringUtils.getParenthesesContent(
    					jsonStr.substring(beginMatch, end));
    			String[] args = argStr.split(",");
    			String pathToJob = "#0";
    			String[] pathIntoExposedData = args;
    			if (args[0].stripLeading().startsWith("#"))
    			{
    				pathToJob = args[0].stripLeading().stripTrailing();
    				pathIntoExposedData = Arrays.copyOfRange(args, 1, args.length);
    			}
    			
    			Object value = getExposedData(pathToJob, pathIntoExposedData);
    			
    			String init = jsonStr.substring(startCopying, beginMatch);
    			int beginningLeftOver = beginMatch + GETACCJOBSDATA.length() 
    				+ 2 + argStr.length();
    			String leftover = jsonStr.substring(beginningLeftOver, end);
    			newJson.append(init);
    			newJson.append(value);
    			newJson.append(leftover);
    			
    			startCopying = end;
    		}
    		
    		if (edited)
    		{
    			params.getAllNamedData().put(paramKey, jsonReader.fromJson(
    					newJson.toString(), NamedData.class));
    		}
    	}
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

	    // NB: flags like 'isInterrupted', 'hasException', 'completed',
	    // and 'redirectOutErr' and also 'exposedOutput'
	    // change during the execution of the job so should not be used to 
	    // compare jobs otherwise the associated change of hashcode breaks
	    // consistency required to use Job instances as keys in Hash-based maps
	    // and similar.
	   
	    return this.jobId == other.jobId 
			   && this.appID == other.appID
			   && this.nThreads == other.nThreads
			   && Objects.equals(this.customUserDir, other.customUserDir)
			   && Objects.equals(this.stdout, other.stdout)
			   && Objects.equals(this.stderr, other.stderr)
			   && Objects.equals(this.params, other.params)
			   && Objects.equals(this.steps, other.steps);
    }
   
//-----------------------------------------------------------------------------

	@Override
	public int hashCode() 
	{
	    // NB: flags like 'isInterrupted', 'hasException', 'completed', 
		// and 'redirectOutErr' and also 'exposedOutput'
		// change during the execution of the job so should not be used to 
	    // compare jobs otherwise the associated change of hachcode breaks
	    // consistency required to use Job instances as keys in Hash-based maps
	    // and similar.
		
		return Objects.hash(jobId, appID, nThreads,
				customUserDir, stdout, stderr, params, steps);
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
