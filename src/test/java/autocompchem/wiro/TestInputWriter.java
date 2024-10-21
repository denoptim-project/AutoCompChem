package autocompchem.wiro;

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
import java.util.HashSet;
import java.util.Set;

import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * A dummy implementation that is meant only to for testing the 
 * {@link ReaderWriterFactory}. This class does not do any real
 * preparation of input files, but offers utilities to ensure this 
 * implementation is the one built by the builder.
 * 
 * @author Marco Foscato
 *
 */
class TestInputWriter extends InputWriter
{
	public static final String IDVAL = "12@4%oi598";
	
//------------------------------------------------------------------------------
	
	public TestInputWriter()
	{
		outFile = new File(IDVAL);
	}
//------------------------------------------------------------------------------
	
	@Override
	public StringBuilder getTextForInput(Job job) {
		return null;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public Set<Task> getCapabilities() {
        return new HashSet<Task>();
	}

//------------------------------------------------------------------------------
	
	@Override
	public Worker makeInstance(Job job) {
		return new TestInputWriter();
	}

//------------------------------------------------------------------------------
}