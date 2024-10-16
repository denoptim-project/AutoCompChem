package autocompchem.wiro.chem;

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


import autocompchem.run.Job;
import autocompchem.wiro.OutputReader;
import autocompchem.worker.Worker;

/**
 * This is a dummy class that is only used to provide a concrete implementation
 * of {@link ChemSoftOutputReader} that is agnostic to the software. This is
 * needed to print software-agnostic help messages from within this package,
 * hence knowing already that the unknown software is a computational chemistry 
 * software and that its reader is an implementation of 
 * {@link ChemSoftOutputReader}. 
 * Effectively, it is a complex way to satisfy the machinery generating help
 * messages. Such machinery, in fact, required concrete implementations of
 * {@link Worker}, so the abstract class {@link ChemSoftOutputReader} cannot 
 * be used directly.
 * 
 * @author Marco Foscato
 */

public class AgnosticCompChemOutputReader extends OutputReader
{

//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job) 
	{
		return new AgnosticCompChemOutputReader();
	}

//------------------------------------------------------------------------------

	@Override
	public void performTask() {}
	
//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "inputdefinition/ChemSoftOutputReader.json";
	}

//-----------------------------------------------------------------------------
    
}
