package autocompchem.worker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.IOutputExposer;
import autocompchem.run.Job;


/**
 * A worker is anything that can perform any task. This abstract class serves 
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
     * Flag notifying that worker had completed its work.
     */
    protected boolean workIsDone = false;

    /**
     * Collector for data taken as input.
     */
    protected NamedDataCollector inputCollector;
    
    /**
     * Collector for output data exposed to the outside world (i.e., is the Job
     * this worker is part of).
     */
    protected NamedDataCollector exposedOutputCollector;

    /**
     * Verbosity level
     */
    protected int verbosity = 1;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty worker.
     */

    public Worker()
    {
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a worker with a specific task, but no further 
     * settings/parameters.
     * @param task the task for this worker
     */

    public Worker(TaskID task)
    {
    	this.task = task;
    	initialize();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an worker with specific settings. Settings must include
     * a 'TASK' parameter.
     * @param params the given collection of settings/parameters.
     */

    public Worker(ParameterStorage params)
    {
    	setParameters(params);
    	initialize();
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
			System.out.println("WARNING! Attempt to put data on a null output collector");
		}
	}
	
//------------------------------------------------------------------------------
	
}
