package autocompchem.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;


/**
 * Unit Test for the {@link Worker}
 * 
 * @author Marco Foscato
 */

public class WorkerTest 
{
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetNewWorkerInstance() throws Exception
    {
    	DummyWorker w = new DummyWorker();
    	
    	List<ConfigItem> knownInput = w.getKnownParameters();
    	
    	assertEquals(3, knownInput.size());
    	
    	assertEquals(1, knownInput.stream()
    			.filter(i -> i.key.equals("INFILE"))
    			.count());
    	
    	ConfigItem ci = knownInput.get(0);
    	assertEquals("The pathname to the file to read as input.", ci.doc);
    	ci = knownInput.get(2);
    	assertEquals(3, ci.doc.split("\\n").length);
    	
    }

//------------------------------------------------------------------------------

}
