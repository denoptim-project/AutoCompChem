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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.text.TextBlockIndexed;


/**
 * A job is a piece of computational work to be done
 *
 * @author Marco Foscato
 */

public class Job implements Runnable
{
	/**
	 * Reference to the parent job. This is null for the outermost, master job
	 */
	private Job parentJob = null;
	
	/**
	 * A job identifier meant to be unique only within sibling jobs, i.e., jobs
	 * that belong to the same master job.
	 */
	protected int jobId = 0;
	
	/**
	 * Counter for subjobs
	 */
	private AtomicInteger idSubJob = new AtomicInteger(1);
	
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
     * A listener that can hear notifications from this job (i.e., observer). 
     * Typically, the listener is a master job or the manager of parallel jobs.
     */
    private JobNotificationListener observer;

    /**
     * Flag signalling that this job has been interrupted
     */
    protected boolean isInterrupted = false;
    
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
    private static final String SEP = System.getProperty("file.separator");
    
    /**
     * Container for any kind of output that is made available to the outside
     * world /w.r.t. this job) via the {@link #getOutput} method. 
     * We say these data is "exposed".
     */
    protected NamedDataCollector exposedOutput = new NamedDataCollector();
    
    /**
     * Verbosity level: amount of logging from this jobs
     */
    private int verbosity = 0;
    
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
	public static final String JSONJOVTYPE = "jobType"; 
	
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
        params.setParameter(ref, NamedDataType.UNDEFINED, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a parameters.
     * @param ref the reference name of the parameter to add/set.
     * @param value the value of the parameter.
     */

    public void setParameter(String ref, String value)
    {
        params.setParameter(ref, NamedDataType.STRING, value);
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
        params.setParameter(ref, type, value);
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
    	step.setParent(this);
    	step.setId(idSubJob);
        steps.add(step);
    }

//------------------------------------------------------------------------------

    /**
     * Sets the reference to the parent job, which is the job that contains
     * this one.
     */
    
    public void setParent(Job parentJob)
    {
    	this.parentJob = parentJob;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the reference to the parent job, which is the job that contains
     * this one.
     * @return the parent job.
     */
    
    public Job getParent()
    {
    	return parentJob;
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
     * Returns the identifier of this job. This identifier is unique only within
     * the context of a single master job, i.e., all sub jobs have unique ID.
     * @return the string identifying this jobs
     */
    
    public String getId()
    {
    	String idStr = "";
    	if (parentJob == null)
    	{
    		idStr = "#" + jobId;
    	} else {
    		idStr = parentJob.getId() + "." + jobId;
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
     * Also, this method is only implemented in the super-class, and 
     * Sub-classed cannot overwrite it. Instead, sub-classes can overwrite 
     * {@link runThisJobSubClassSpecific()}, which is called by {@link run()}.
     */

    public final void run()
    {
    	//TODO use logger
    	if (verbosity > 0)
    	{
    		System.out.println(System.getProperty("line.separator") 
    				+ "Initiating " + appID + " Job " + getId());
    	}
    	
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
        
        finalizeStatusAndNotifications(!jobIsBeingKilled);
    }

//------------------------------------------------------------------------------

    /**
     * Tries to do the work of this very Job. Does not consider the sub jobs
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
     */

    private void runSubJobsSequentially()
    {
        for (int iJob=0; iJob<steps.size(); iJob++)
        {
        	Job j = steps.get(iJob);
            j.run();
            
            if (j.requestsAction())
            {
            	//TODO reactToEvent(j,act,i);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Runs all the sub-jobs in an embarrassingly parallel fashion.
     */

    private void runSubJobsPararelly()
    {
        ParallelRunner parallRun = 
        		new ParallelRunner(steps,nThreads,nThreads,this);
        if (hasParameter(ParallelRunner.WALLTIMEPARAM))
        {
        	parallRun.setWallTime(Long.parseLong(
        			params.getParameterValue(ParallelRunner.WALLTIMEPARAM)));
        }
        if (hasParameter(ParallelRunner.WAITTIMEPARAM))
        {
        	parallRun.setWaitingStep(Long.parseLong(
        			params.getParameterValue(ParallelRunner.WAITTIMEPARAM)));
        }
        parallRun.setVerbosity(verbosity);
        parallRun.start();
    }
    
//------------------------------------------------------------------------------

    /**
     * Sends this job to an executing thread managed by an existing, and
     * pre-started thread manager. This method is overwritten by subclasses 
     * that need special kinds of execution. For example, see 
     * {@link MonitoringJob}.
     * @param tpExecutor the manager of the job executing threads.
     * @return a Future representing pending completion of the task.
     */
    
  	protected Future<?> submitThread(ScheduledThreadPoolExecutor tpExecutor) {
  		return tpExecutor.submit(this);
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
     * Set the flag signalling that the execution of this job was interrupted
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
     * Checks if this job is requesting any action.
     * @return <code>true</code> if this job is requesting any action
     */
    
    public boolean requestsAction()
    {
    	return exposedOutput.contains(JobEvaluator.REACTIONTOSITUATION);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the requested action or null, if no action is requested
     */
    
    public Action getRequestedAction()
    {
    	if (requestsAction())
    	{
    		return (Action) exposedOutput.getNamedData(
    				JobEvaluator.REACTIONTOSITUATION).getValue();
    	} else {
    		return null;
    	}
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
     * listening to this job
     */
    private void finalizeStatusAndNotifications(boolean notify) 
    {
    	completed = true;
    	if (observer!=null && notify)
    	{
	    	if (requestsAction())
	        {
	        	observer.reactToRequestOfAction(getRequestedAction(), this);
	        } else {
	        	if (!(this instanceof MonitoringJob))
	        	{
	        		observer.notifyTermination(this);
	        	}
	        }
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this job following the format of
     * autocompchem's JobDetail text file, but collecting the text in
     * a {@link TextBlockIndexed}
     * @return the list of lines ready to print a jobDetails file.
     */

    public TextBlockIndexed toTextBlockJobDetails()
    {
    	return new TextBlockIndexed(toLinesJobDetails(), 0, 0, 0);
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
    
   @Override
   public boolean equals(Object o) 
   {
	   if (o == this)
		   return true;
	   
	   if (!(o instanceof Job))
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
			   && this.verbosity == other.verbosity
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

            jsonObject.addProperty(JSONJOVTYPE, job.getClass().getSimpleName());
            
            if (!job.params.isEmpty())
            	jsonObject.add(JSONPARAMS, context.serialize(job.params));
            if (!job.steps.isEmpty())
            	jsonObject.add(JSONSUBJOBS, context.serialize(job.steps));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class JobDeSerializer 
    implements JsonDeserializer<Job>
    {
        @Override
        public Job deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();
            
            if (!jsonObject.has(JSONJOVTYPE))
            {
                String msg = "Missing '" + JSONJOVTYPE + "': found a "
                        + "JSON string that cannot be converted into a Job "
                        + "subclass.";
                throw new JsonParseException(msg);
            }       

            String typ = context.deserialize(jsonObject.get(JSONJOVTYPE),
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
            	step.setParent(job);
            	step.setId(job.idSubJob);
            }
        	
        	return job;
        }
    }
    
//------------------------------------------------------------------------------

}
