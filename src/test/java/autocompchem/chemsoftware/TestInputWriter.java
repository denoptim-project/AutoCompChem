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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.files.FileFingerprint;
import autocompchem.run.Job;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * A dummy implementation that is meant only to for testing the 
 * {@link ChemSoftReaderWriterFactory}. This class does not do any real
 * preparation of input files, but offers utilities to ensure this 
 * implementation is the one built by the builder.
 * 
 * @author Marco Foscato
 *
 */
class TestInputWriter extends ChemSoftInputWriter
{
	public static final String IDVAL = "12@4%oi598";
	
//------------------------------------------------------------------------------
	
	public TestInputWriter()
	{
		outFile = new File(IDVAL);
	}

//------------------------------------------------------------------------------

	@Override
	protected File manageOutputFileStructure(List<IAtomContainer> mols, 
			File output) {
		return null;
	}

//------------------------------------------------------------------------------
	
	@Override
	protected void setSystemSpecificNames(CompChemJob ccj) {
	}

//------------------------------------------------------------------------------
	
	@Override
	protected void setChargeIfUnset(CompChemJob ccj, String charge, 
			boolean omitIfPossible) {
	}

//------------------------------------------------------------------------------
	
	@Override
	protected void setSpinMultiplicityIfUnset(CompChemJob ccj, String sm, 
			boolean omitIfPossible) {
	}

//------------------------------------------------------------------------------
	
	@Override
	protected void setChemicalSystem(CompChemJob ccj, 
			List<IAtomContainer> iacs) {
	}

//------------------------------------------------------------------------------
	
	@Override
	protected List<String> getTextForInput(CompChemJob job) {
		return null;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public Set<TaskID> getCapabilities() {
        return new HashSet<TaskID>();
	}

//------------------------------------------------------------------------------
	
	@Override
	public Worker makeInstance(Job job) {
		return new TestInputWriter();
	}

//------------------------------------------------------------------------------
}