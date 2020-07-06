package autocompchem.worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.run.IOutputExposer;
import autocompchem.run.Terminator;
import autocompchem.run.Job.RunnableAppID;


/**
 * A dummy worker that is meant only for testing purposes.
 * 
 * @author Marco Foscato
 */

public class DummyWorker extends Worker
{
	public static final String DATAREF = "duDataB";
	public static final String DATAVALUE = "Output data B";
	
//-----------------------------------------------------------------------------
	
	public static final Set<TaskID> capabilities = 
			Collections.unmodifiableSet(new HashSet<TaskID>(
					Arrays.asList(TaskID.DummyTask)));
	
//-----------------------------------------------------------------------------

	public DummyWorker(){}
	
//-----------------------------------------------------------------------------

	public DummyWorker(TaskID task) {
		super(task);
	}

//-----------------------------------------------------------------------------
	
	@Override
	public void initialize() 
	{
		inputCollector = new NamedDataCollector();
		
		// These dummy data are used only for testing
		// Do not modify them unless you change also the 
		// WorkerFactoryTest class
		inputCollector.putNamedData(new NamedData(DATAREF, 
				NamedDataType.STRING, DATAVALUE));
	}
	
//-----------------------------------------------------------------------------

	@Override
	public void performTask() 
	{
		exposeOutputData(inputCollector.getNamedData("duDataB"));
	}
	
//-----------------------------------------------------------------------------	

}
