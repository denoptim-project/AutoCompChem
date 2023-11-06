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

import org.junit.jupiter.api.Test;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.AppID;


/**
 * Unit Test for the factory of workers.
 * 
 * @author Marco Foscato
 */

//TODO-gg rename
public class WorkerFactory2Test 
{
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetNewWorkerInstance() throws Exception
    {
    	// Trying to create a worker for a task that is not registered
    	boolean triggered = false;
    	try {
    		WorkerFactory2.createWorker(TaskID.DUMMYTASK2);
    	} catch (ClassNotFoundException e)
    	{
    		if (e.getMessage().contains("has not been registered"))
    			triggered = true;
    	}
    	assertTrue(triggered);
    	
    	// Now we register that type and try again
    	WorkerFactory2.getInstance().registerType(TaskID.DUMMYTASK2, 
    			new DummyWorker2());
    	Worker w = WorkerFactory2.createWorker(TaskID.DUMMYTASK2);
    	assertTrue(w instanceof DummyWorker2);
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testCreateDummyWorkerFromJob() throws Exception
    {
    	Job job = JobFactory.createJob(AppID.ACC);
    	ParameterStorage params = new ParameterStorage();
    	params.setParameter(WorkerConstants.PARTASK,TaskID.DUMMYTASK2.toString());
    	job.setParameters(params);
    	
    	WorkerFactory2.getInstance().registerType(TaskID.DUMMYTASK2, 
    			new DummyWorker2());
    	
    	Worker w = WorkerFactory2.createWorker(job);
    	assertTrue(w instanceof DummyWorker2);
    }

//------------------------------------------------------------------------------

}
