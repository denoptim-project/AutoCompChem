package autocompchem.chemsoftware;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Aspecific reader for log/output data files. This is a wrapper that includes
 * both detection of the type of data to read and creation of a suitable 
 * {@link Worker} to read and analyze that data.
 * 
 * @author Marco Foscato
 */
public class AspecificOutputAnalyzer extends Worker
{
	
//------------------------------------------------------------------------------

  	@Override
	public Set<TaskID> getCapabilities() {
		return Collections.unmodifiableSet(new HashSet<TaskID>(
                        Arrays.asList(TaskID.ANALYSEOUTPUT)));
	}

//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job) 
	{
		if (!job.hasParameter(ChemSoftConstants.PARJOBOUTPUTFILE))
		{
			Terminator.withMsgAndStatus("ERROR: cannot detect the type of "
					+ "ouput to analyze. Make sure the parameter '" 
					+ ChemSoftConstants.PARJOBOUTPUTFILE + "' is given.", -1);
		}
		String fileName = job.getParameter(
        		ChemSoftConstants.PARJOBOUTPUTFILE).getValueAsString();
		ChemSoftOutputAnalyzerBuilder builder = 
				new ChemSoftOutputAnalyzerBuilder();
		
		try {
			return builder.makeInstance(new File(fileName));
		} catch (FileNotFoundException e) {
			Terminator.withMsgAndStatus("ERROR: log/output file '"
					+ fileName + "' is defined by '" 
					+ ChemSoftConstants.PARJOBOUTPUTFILE 
					+ "' but does not exist.", -1);
		}
		return null;
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
		return "inputdefinition/ChemSoftOutputHandler.json";
	}

//-----------------------------------------------------------------------------
    
}
