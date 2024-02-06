package autocompchem.chemsoftware;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;

/**
 * Software-agnostic wrapper for classes able to write input files for comp.
 * chem. software packages (i.e., implementations of {@link ChemSoftInputWriter}.
 * This class allows to perform software agnostic operations directly.
 * For example, getting the help message for any implementation of 
 * {@link ChemSoftInputWriter}.  It also allows to make a concrete 
 * implementation of {@link ChemSoftInputWriter} based on any hint on the 
 * identity of the comp. chem. software package for which the input has to be 
 * prepared.
 * 
 * @author Marco Foscato
 */

//TODO-gg refactor to agnostic
public class AspecificInputWriter extends Worker
{
	
//------------------------------------------------------------------------------

  	@Override
	public Set<Task> getCapabilities() {
		return Collections.unmodifiableSet(new HashSet<Task>(
                        Arrays.asList(Task.make("prepareInput"))));
	}

//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job) 
	{
		String taskStr = job.getParameter(WorkerConstants.PARTASK)
				.getValueAsString();
    	Task taskID = Task.make(taskStr);
    	if (taskID != Task.make("PREPAREINPUT"))
    	{
			//TODO-gg log
			System.err.println("WARNING: attempt to make a " 
					+ ChemSoftInputWriter.class.getSimpleName() 
					+ " for task '" + taskStr + "', but this is not allowed.");
			return new AspecificInputWriter();
    	}
    	if (!job.hasParameter(ChemSoftConstants.SOFTWAREID))
		{
			//TODO-gg log
			System.err.println("WARNING: cannot detect the type of "
					+ "software for which to prepare an input. "
					+ "Make sure the parameter '" 
					+ ChemSoftConstants.SOFTWAREID + "' is given.");
			return new AspecificInputWriter();
		}
    	
    	String softwareID = job.getParameter(ChemSoftConstants.SOFTWAREID)
    			.getValueAsString();
    	
		ChemSoftReaderWriterFactory builder = 
				ChemSoftReaderWriterFactory.getInstance();
		return builder.makeInstanceInputWriter(softwareID);
	}

//------------------------------------------------------------------------------

	@Override
	public void initialize() {}

//------------------------------------------------------------------------------

	@Override
	public void performTask() {}
	
//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "inputdefinition/ChemSoftInputWriter.json";
	}

//-----------------------------------------------------------------------------
    
}
