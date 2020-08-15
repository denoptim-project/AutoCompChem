package autocompchem.worker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;


/**
 * A dummy worker that is meant only for testing purposes.
 * 
 * @author Marco Foscato
 */

public class DummyWorker extends Worker
{
	public static final String DATAREF = "duDataB";
	public static final String DATAVALUE = "Output data B";
	
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
		// The only task here is reporting something in the output collector
		exposeOutputData(inputCollector.getNamedData(DATAREF));
		
		if (params.contains("VERBOSITY")) 
		{
			if (Integer.parseInt(params.getParameter("VERBOSITY").getValue().toString()) > 2) {
				// In some tests we might want to check that the input has been read
				// properly, so we echo the content of the param. storage
				System.out.println(params.toLinesJobDetails());
			} 
		}
	}
	
//-----------------------------------------------------------------------------	

}
