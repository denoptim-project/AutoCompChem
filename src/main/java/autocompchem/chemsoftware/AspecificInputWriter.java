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
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

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
