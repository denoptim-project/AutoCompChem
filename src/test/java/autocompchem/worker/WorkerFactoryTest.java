package autocompchem.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.AppID;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;


/**
 * Unit Test for the factory of workers.
 * 
 * @author Marco Foscato
 */

//TODO-gg rename
public class WorkerFactoryTest 
{
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetNewWorkerInstance() throws Exception
    {
    	// Trying to create a worker that is not registered from its task
    	boolean triggered = false;
		DummyWorker2 dw2 = new DummyWorker2();
    	for (Task task : dw2.getCapabilities())
    	{
	    	try {
	    		WorkerFactory.createWorker(task);
	    	} catch (ClassNotFoundException e)
	    	{
	    		if (e.getMessage().contains("has not been registered"))
	    			triggered = true;
	    	}
	    	assertTrue(triggered);
    	}
    	
    	// Trying to create a worker for a task that is not registered
    	triggered = false;
    	try {
    		WorkerFactory.createWorker(DummyWorker2.class);
    	} catch (ClassNotFoundException e)
    	{
    		if (e.getMessage().contains("No registered worker with type"))
    			triggered = true;
    	}
    	assertTrue(triggered);
    	
    	// Now we register that type and try again
    	WorkerFactory.getInstance().registerType(new DummyWorker2());
    	
    	// Try again with task
    	Worker w = null;
    	triggered = false;
    	for (Task task : dw2.getCapabilities())
    	{
	    	try {
	    		w = WorkerFactory.createWorker(task);
	    	} catch (Throwable e)
	    	{
	    		triggered = true;
	    	}
	    	assertFalse(triggered);
	    	assertTrue(w instanceof DummyWorker2);
    	}
    	
    	// and with task from scratch
    	w = WorkerFactory.createWorker(DummyWorker2.DUMMYTASK2TASK);
    	assertTrue(w instanceof DummyWorker2);
    	
    	// Try again with class
    	w = WorkerFactory.createWorker(DummyWorker2.class);
    	assertTrue(w instanceof DummyWorker2);
    	
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testCreateDummyWorkerFromJob() throws Exception
    {
    	Job job = JobFactory.createJob(AppID.ACC);
    	ParameterStorage params = new ParameterStorage();
    	params.setParameter(WorkerConstants.PARTASK,
    			DummyWorker2.DUMMYTASK2TASK.casedID);
    	job.setParameters(params);
    	
    	WorkerFactory.getInstance().registerType(new DummyWorker2());
    	
    	Worker w = WorkerFactory.createWorker(job);
    	assertTrue(w instanceof DummyWorker2);
    }

//------------------------------------------------------------------------------

}
