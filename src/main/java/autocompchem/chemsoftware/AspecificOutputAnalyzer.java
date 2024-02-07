package autocompchem.chemsoftware;

/*   
 *   Copyright (C) 2023  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Aspecific reader for log/output data files. This is a wrapper that includes
 * both detection of the type of data to read and creation of a suitable 
 * {@link Worker} to read and analyze that data.
 * 
 * @author Marco Foscato
 */

//TODO-gg remane to reader

public class AspecificOutputAnalyzer extends Worker
{
    /**
     * String defining the task for analyzing any job output
     */
    public static final String ANALYSEOUTPUTTASKNAME = "analyseOutput";

    /**
     * Task about analyzing any job output
     */
    public static final Task ANALYSEOUTPUTTASK;
    static {
    	ANALYSEOUTPUTTASK = Task.make(ANALYSEOUTPUTTASKNAME);
    }
	
//------------------------------------------------------------------------------

  	@Override
	public Set<Task> getCapabilities() {
		return Collections.unmodifiableSet(new HashSet<Task>(
                        Arrays.asList(ANALYSEOUTPUTTASK)));
	}

//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job) 
	{
		if (!job.hasParameter(ChemSoftConstants.PARJOBOUTPUTFILE))
		{
			//TODO-gg log
			System.err.println("WARNING: cannot detect the type of "
					+ "output to analyze. Make sure the parameter '" 
					+ ChemSoftConstants.PARJOBOUTPUTFILE + "' is given.");
			return new AspecificOutputAnalyzer();
		}
		
		String fileName = job.getParameter(
        		ChemSoftConstants.PARJOBOUTPUTFILE).getValueAsString();
		ChemSoftReaderWriterFactory builder = 
				ChemSoftReaderWriterFactory.getInstance();
		
		try {
			Worker w = builder.makeOutputReaderInstance(new File(fileName));
			if (w==null)
			{
				Terminator.withMsgAndStatus("ERROR: log/output file '"
						+ fileName + "' could not be understood as any "
						+ "log/output "
						+ "that can be read by AutoCompChem.", -1);	
			}
			return w;
		} catch (FileNotFoundException e) {
			Terminator.withMsgAndStatus("ERROR: log/output file '"
					+ fileName + "' is defined by '" 
					+ ChemSoftConstants.PARJOBOUTPUTFILE 
					+ "' but does not exist.", -1);
		}
		return new AspecificOutputAnalyzer();
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
