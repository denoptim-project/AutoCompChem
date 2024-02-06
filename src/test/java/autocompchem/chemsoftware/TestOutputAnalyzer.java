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
import java.util.Set;

import autocompchem.files.FileFingerprint;
import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * A dummy implementation that is meant only to for testing the 
 * {@link ChemSoftReaderWriterFactory}. This class does not do any real
 * analysis, but offers utilities to ensure this implementation is the one built
 * by the builder. It is also tunable with respect to the definition of the
 * {@link FileFingerprint} that identifies a readable file structure.
 * 
 * @author Marco Foscato
 *
 */
class TestOutputAnalyzer extends ChemSoftOutputReader
{
	public static final String IDVAL = "12@4%oi598";
	
	public Set<FileFingerprint> outputFingerprints = 
			new HashSet<FileFingerprint>();
	
//------------------------------------------------------------------------------

	/**
	 * Constructor that sets the name of the superclass field to a hard-coded
	 * value. this way, we can test the content of that field to see if we have
	 * indeed created an instance of this very class using this constructor.
	 */
	public TestOutputAnalyzer() 
	{
		// We do not really read anything, we just put an identifier in
		// the data structure of this analyzer.
		inFile = new File(IDVAL);
	}
		
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(Task.make("analyzeOutput"))));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new TestOutputAnalyzer();
    }
    
//------------------------------------------------------------------------------

	@Override
	protected void readLogFile(LogReader reader) throws Exception {
	}

//------------------------------------------------------------------------------

	@Override
	protected Set<FileFingerprint> getOutputFingerprint() {
		return outputFingerprints;
	}

//------------------------------------------------------------------------------

	@Override
	protected String getSoftwareID() {
		return "Tester";
	}

//------------------------------------------------------------------------------

	@Override
	protected ChemSoftInputWriter getChemSoftInputWriter() {
		return new TestInputWriter();
	}
	
//------------------------------------------------------------------------------
}