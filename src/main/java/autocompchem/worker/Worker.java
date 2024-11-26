package autocompchem.worker;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.log.LogUtils;
import autocompchem.run.IOutputExposer;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.chem.ChemSoftConstants;


/**
 * A worker is anything that can perform any task or job. 
 * This abstract class serves 
 * as starting point for all other workers. 
 * The subclass must be registered in the {@link WorkerFactory} for the latter
 * to be able to construct a worker based on a job requiring its capabilities.
 *
 * @author Marco Foscato
 */

public abstract class Worker implements IOutputExposer
{
	
    /**
     * Container for parameters fed to this worker. 
     * Typically contains initial settings, pathnames 
     * and configurations.
     */
    protected ParameterStorage params;
    
    /**
     * The specific task for this worker
     */
    protected Task task;
    
    /**
     * The job that this worker is charged with.
     */
    protected Job myJob;
    
    /**
     * Flag notifying that worker had completed its work.
     */
    protected boolean workIsDone = false;

    /**
     * Collector for data taken as input.
     */
    protected NamedDataCollector inputCollector;
    
    /**
     * Collector for output data that is exposed to the outside world (i.e., is 
     * the Job this worker is part of).
     */
    protected NamedDataCollector exposedOutputCollector;
    
    /**
     * Class specific logger.
     */
    protected Logger logger = LogManager.getLogger(this.getClass());
    
    /**
     * System-specific newline characters
     */
    protected final static String NL = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty worker..
     */
    protected Worker()
    {}
    
//------------------------------------------------------------------------------
    
    /**
     * Setup the parameters used to initialize this worker. If the parameters
     * include the {@link WorkerConstants#PARTASK} parameter, then the task 
     * assigned to this worker upon instantiation with 
     * {@link WorkerFactory} is overwritten.
     * @param params the collection of parameters.
     */
    public void setParameters(ParameterStorage params)
    {
    	this.params = params;
    	String taskStr = this.params.getParameter(WorkerConstants.PARTASK)
    			.getValueAsString();
    	this.task = Task.make(taskStr);
    }

//------------------------------------------------------------------------------

    /**
     * Checks if a parameter has been set.
     * @param refName the reference name of the parameter.
     * @return <code>true</code> if the parameter exists, of <code>false</code>
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
     * Gets the job this worker is assigned to.
     * @return the job this worker is assigned to.
     */
    public Job getMyJob()
    {
    	return myJob;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads the list on known parameters that a worker can take as 
     * input settings.
     * @return the list of input settings.
     */
    public List<ConfigItem> getKnownParameters()
    {
    	String pathnameForWorker = getKnownInputDefinition();
    	if (task!=null)
    	{
	    	String pathnameForTask = pathnameForWorker.replace(".json", 
	    			"_" + task + ".json");
	    	List<ConfigItem> taskSpecificList = getKnownParameters(
	    			pathnameForTask, true);
	    	if (taskSpecificList.size()>0)
	    	{
	    		return taskSpecificList;
	    	}
    	}
        return getKnownParameters(pathnameForWorker, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads the list on known parameters from JSON file.
     * @param the JSON file containing the information to read in.
     * @return the list of input settings.
     */
    protected static List<ConfigItem> getKnownParameters(String pathName)
    {
    	return getKnownParameters(pathName, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads the list on known parameters from JSON file.
     * @param the JSON file containing the information to read in.
     * @param tolerant use <code>true</code> to ignore the error triggered by
     * file not found.
     * @return the list of input settings.
     */
    protected static List<ConfigItem> getKnownParameters(String pathName,
    		boolean tolerant)
    {
    	Gson reader = ACCJson.getReader();
    	List<ConfigItem> knownParams = new ArrayList<ConfigItem>();
        InputStream ins = Worker.class.getClassLoader()
        	 .getResourceAsStream(pathName);
        if (ins==null)
        {
        	if (tolerant)
        	{
        		return knownParams;
        	} else {
        		throw new Error("Resource file '" + pathName + "' not found!");
        	}
        }
        BufferedReader br = null;
        try
        {
        	br = new BufferedReader(new InputStreamReader(ins));
            knownParams = reader.fromJson(br, 
                    new TypeToken<List<ConfigItem>>(){}.getType());
        }
        catch (JsonSyntaxException jse)
        {
        	throw new Error("Format of '" + pathName + "' is "
           		+ "corrupted. Please report this to the authors.", jse);
        }
        finally 
        {
            try {
                if (br != null)
                {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new Error(ioe);
            }
        }
        return knownParams;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads the list on known parameters that a worker can take as 
     * input settings, and removes any that matches the given identifiers.
     * @param ignorableItems list of item keys (case insensitive)
     * @param ignorableItems list of worker class canonical name (case 
     * insensitive)
     * @return the sub list of input settings that excludes the ones to be
     * ignored.
     */
    public List<ConfigItem> getKnownParameters(List<String> ignorableItems, 
    		List<String> ignorableWorkers)
    {
    	List<ConfigItem> retained = new ArrayList<ConfigItem>();
		for (ConfigItem ci : getKnownParameters())
    	{					
			if (ci.key!=null && !ci.key.isBlank()
					&& !ignorableItems.stream().anyMatch(
							ci.key::equalsIgnoreCase))
			{
				retained.add(ci);
				continue;
			}
			
			if (ci.embeddedWorker!=null && !ci.embeddedWorker.isBlank()
					&& !ignorableWorkers.stream().anyMatch(
							ci.embeddedWorker::equalsIgnoreCase))
			{
				retained.add(ci);
			}
		}
    	return retained;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates a string defining all configuration items related to the task
     * this worker is expected to perform. The string is formatted to print
     * CLI's help messages.
     */
    public String getTaskSpecificHelp()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Settings available for task '" + task + "':");
    	sb.append(System.getProperty("line.separator"));
    	return getFormattedHelpString(sb);
    }

//------------------------------------------------------------------------------
    
    /**
     * Creates a string defining all configuration items related to the task
     * this worker is expected to perform when run from within another worker.
     * The string is formatted to print CLI's help messages.
     * This will produce a result that depends on the task configured for this
     * worker.
     */
    private String getFormattedHelpString(StringBuilder sb)
    {
    	for (ConfigItem ci : getKnownParameters())
    	{
    		// TODO make items link to source's constant names, so we can check that 
    		// the declared ID is indeed what the code expects
    		sb.append(System.getProperty("line.separator"));
    		sb.append(ci.getStringForHelpMsg());
    	}
    	return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Initializes this worker based on the parameters that were given upon 
     * construction.
     */
    public void initialize()
    {	
        logger.debug("Initializing in " 
        		+ this.getClass().getSimpleName() + ".");
        
        logger.debug("Adding parameters to " + this.getClass());
        
        if (params.contains(ParameterConstants.VERBOSITY))
        {
            String str = params.getParameter(
            		ParameterConstants.VERBOSITY).getValueAsString();
            if (!NumberUtils.isNumber(str))
			{
				Terminator.withMsgAndStatus("ERROR! Value '" + str + "' "
						+ "cannot be converted to an integer. Check parameter "
						+ ParameterConstants.VERBOSITY, -1);
			}
            Configurator.setLevel(logger.getName(), 
            		LogUtils.verbosityToLevel(Integer.parseInt(str)));
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Performs a specific task. 
     * This method is overwritten by subclasses.
     */
    public abstract void performTask();
    
//------------------------------------------------------------------------------
      
    /**
     * Declaration of the capabilities of an implementation of {@link Worker}.
     */
    public abstract Set<Task> getCapabilities();

//------------------------------------------------------------------------------
    
    /**
     * Returns pathname of the resource collecting the documentation of any
     * setting that this worker can react to.
     */
    public abstract String getKnownInputDefinition();
  
//------------------------------------------------------------------------------
    
    /**
     * Makes an instance of the specific implementation possibly adapting
     * the instance to the given argument.
     * @param job the job meant to be done by the instance of worker to make.
     */
    public abstract Worker makeInstance(Job job);
      
//------------------------------------------------------------------------------
	
    /**
	 * Sets the reference to the data structure where the exposed data
	 * is to be collected.
	 * @param collector the collector data structure.
	 */
    public void setDataCollector(NamedDataCollector collector)
    {
    	this.exposedOutputCollector = collector;
    }
    
//------------------------------------------------------------------------------

	/**
	 * Adds some data the the collection of exposed data.
	 * @param data the data to expose
	 */
	public void exposeOutputData(NamedData data)
	{
		if (exposedOutputCollector != null)
		{
		    exposedOutputCollector.putNamedData(data);
		}
		else
		{
			logger.warn("WARNING! Worker trying to put data on a null "
					+ "output collector");
		}
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Returns any {@link NamedData} from the {@link Job} that precedes this one
	 * in the job that contains this one, i.e., the container job. 
	 * This method assumes the container job defined a sequential workflow, so
	 * it refers to the job with index immediately preceding the index of the 
	 * job this worked is charged with.
	 * @param refName the name of the data to fetch.
	 * @param tolerant use <code>true</code> to allow returning 
	 * <code>null</code>, or <code>false</code> to trigger errors explaining
	 * why no data can be returned.
	 * @return the data to fetch or <code>null</code> is none was found or if 
	 * there is no preceding job.
	 */
	public NamedData getOutputOfPrevStep(String refName, boolean tolerant)
	{
		Job container = myJob.getContainer();
		if (container == null)
		{
			if (!tolerant)
			{
				throw new Error("No previous data to process. Job " 
						+ myJob.getId() + " is not contained in any workflow.");
			} else {
				return null;
			}
		}
		
		Job previous = null;
		int myJobIdx = container.getSteps().indexOf(myJob);
		if (myJobIdx < 1)
		{
			if (!tolerant)
			{
				throw new Error("No previous data to process. Job " 
						+ myJob.getId() 
						+ " is the first step in the workflow defined in job " 
						+ container.getId() + ".");
			} else {
				return null;
			}
		}
		
		previous = container.getStep(myJobIdx - 1);
		if (!previous.exposedOutput.contains(refName))
		{
			if (!tolerant)
			{
				throw new Error("No previous data to process. Job " 
						+ container.getId() 
						+ " does not contained data labelled '" + 
						refName + "'.");
			} else {
				return null;
			}
		}
		
		return previous.exposedOutput.getNamedData(refName);
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Triggers the reaction to a request to perform a {@link Task} that is not
	 * among those declared by this implementation.
	 */
	protected void dealWithTaskMismatch()
	{
    	Terminator.withMsgAndStatus("ERROR! Task '" + task + "' is not "
    			+ "linked to any method in "
    			+ this.getClass().getSimpleName() + ".", -1);
	}
	
//------------------------------------------------------------------------------
	
}
