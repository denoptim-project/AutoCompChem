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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.run.IOutputExposer;
import autocompchem.run.Job;


/**
 * A worker is anything that can perform any task or job. 
 * This abstract class serves 
 * as starting point for all other workers. 
 * Subclasses of this class need to respect these constraints:
 * <ul>
 * <li>The subclass name must be registered among the {@link WorkerID},</li>
 * <li>The subclass needs to include the following field;
 * <pre>
 * 	public static final Set&lt;TaskID&gt; capabilities = ...
 * </pre>
 * which is to be populated with any registered {@link TaskID}. If new task IDs
 * are needed they should be added to the {@link TaskID} registry.</li>
 * <li>Take care of exposing any output that should be made accessible from
 * outside the worker, i.e., typically from the {@link Job} that needed to
 * perform the task with the worker can deal with.</li>
 * </ul>
 *
 * @author Marco Foscato
 */

public abstract class Worker implements IOutputExposer
{
    /**
     * Declaration of the capabilities of this class of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList()));
	
    /**
     * Container for parameters fed to this worker. 
     * Typically contains initial settings, pathnames 
     * and configurations.
     */
    protected ParameterStorage params;
    
    /**
     * The specific task for this worker
     */
    protected TaskID task = TaskID.UNSET;
    
    /**
     * The job that this worker is charged with.
     */
    protected Job myJob;
    
    /**
     * The pathname of the resource collecting the documentation of any
     * setting that this worker can take as input.
     */
    final String knownInputDefinition;
    
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
     * Verbosity level
     */
    protected int verbosity = 0;
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty worker.
     * @param knownInputDefinition the pathname to the JSON file in the 
     * package resources that contains the definition of the input settings
     * as {@link ConfigItem}s.
     */

    protected Worker(String knownInputDefinition)
    {
    	this.knownInputDefinition = knownInputDefinition;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Setup the parameters used to initialise this worker.
     * @param params the collection of parameters.
     */
    
    public void setParameters(ParameterStorage params)
    {
    	this.params = params;
    	String taskStr = this.params.getParameter(WorkerConstants.PARTASK)
    			.getValueAsString();
    	this.task = TaskID.getFromString(taskStr);
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
    	Gson reader = ACCJson.getReader();
    	List<ConfigItem> knownParams = new ArrayList<ConfigItem>();
        InputStream ins = Worker.class.getClassLoader()
        	 .getResourceAsStream(knownInputDefinition);
        BufferedReader br = null;
        try
        {
        	br = new BufferedReader(new InputStreamReader(ins));
            knownParams = reader.fromJson(br, 
                    new TypeToken<List<ConfigItem>>(){}.getType());
        }
        catch (JsonSyntaxException jse)
        {
        	throw new Error("Format of '" + knownInputDefinition + "' is "
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
     * Creates a string defining all configuration items related to the task
     * this worker is expected to perform. The string is formatted to print
     * CLI's help messages.
     */
    public String getTaskSpecificHelp()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Settings available for task '" + task + "':");
    	sb.append(System.getProperty("line.separator"));
    	return getFormattedHelpString(sb, false);
    }

//------------------------------------------------------------------------------

    /**
     * Creates a string defining the configuration items that are compatible to 
     * running this worker from within another worker (i.e., non stand alone).
     * The string is formatted to print
     * CLI's help messages.
     */
    public String getEmbeddedTaskSpecificHelp()
    {
    	StringBuilder sb = new StringBuilder();
    	return getFormattedHelpString(sb, true);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates a string defining all configuration items related to the task
     * this worker is expected to perform when run from within another worker.
     * The string is formatted to print CLI's help messages.
     */
    private String getFormattedHelpString(StringBuilder sb, 
    		boolean ignoreNonStandalone)
    {
    	for (ConfigItem ci : getKnownParameters())
    	{
    		if (ci.isForStandalone() && ignoreNonStandalone)
    		{
	    		continue;
    		}
    		sb.append(System.getProperty("line.separator"));
    		sb.append(ci.getStringForHelpMsg());
    	}
    	return sb.toString();
    }
    
//------------------------------------------------------------------------------

    /**
     * Initialise this worker according to the given parameters. 
     * This method is overwritten by subclasses.
     */
    
    public abstract void initialize();
    
//------------------------------------------------------------------------------

    /**
     * Performs a specific task. 
     * This method is overwritten by subclasses.
     */
    
    public abstract void performTask();
    
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
			//TODO better
			System.out.println("WARNING! Worker trying to put data on a null "
					+ "output collector");
		}
	}
	
//------------------------------------------------------------------------------
	
}
