package autocompchem.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;


/**
 * Unit Test for the factory of workers.
 * 
 * @author Marco Foscato
 */

public class WorkerFactoryTest 
{
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetNewWorkerInstance() throws Exception
    {
    	Worker w = WorkerFactory.getNewWorkerInstance(WorkerID.DummyWorker);
    	
    	assertTrue(w instanceof DummyWorker, "Type of worker created from "
    			+ "dummy task.");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testCreateDummyWorkerFromJob() throws Exception
    {
    	Job job = JobFactory.createJob(RunnableAppID.ACC);
    	ParameterStorage params = new ParameterStorage();
    	params.setParameter(new Parameter(WorkerConstants.PARTASK,
    			NamedDataType.STRING, 
    			TaskID.DummyTask.toString()));
    	job.setParameters(params);
    	job.run();
    	NamedData output = job.getOutput(DummyWorker.DATAREF);
    	
    	assertTrue(output != null, "Output from dummy atom is found.");
    	assertEquals(output.getValue(),DummyWorker.DATAVALUE,
    			"Stored output data.");
    }

//------------------------------------------------------------------------------

}
