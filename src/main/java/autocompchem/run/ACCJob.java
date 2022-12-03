package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Date;

import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * A job class that represents work to be done by AutoCompChem itself
 *
 * @author Marco Foscato
 */

public class ACCJob extends Job 
{
 
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ACCJob()
    {
        super();
        this.appID = RunnableAppID.ACC;
    }

//------------------------------------------------------------------------------

    /**
     * Runs the given task
     */

    @Override
    public void runThisJobSubClassSpecific()
    {   
    	// Check for any ACC task...
    	if (!hasParameter(WorkerConstants.PARTASK))
    	{
    		// ...if none, then this job is just a container for other jobs
    		if (getVerbosity() > 0)
            {
                System.out.println("Running job container " + this.toString());
            }
    		return;
    	}

        // Here we are sure the params include this keyword
        String task = this.params.getParameter(WorkerConstants.PARTASK)
        		.getValue().toString();
        
        Date date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" AutoCompChem is initiating the ACC task '" 
                            + task + "' - "+date.toString());
        }
        
        Worker worker = getWorker();
        worker.initialize();
        worker.performTask();
       

        date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println("Done with ACC job (" + task	+ ") - "+date.toString());
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @return the {@link Worker} that can perform the task required in this job
     * or null.
     */
    public Worker getWorker()
    {
    	// Check for any ACC task...
    	if (!hasParameter(WorkerConstants.PARTASK))
    	{
    		return null;
    	}
       return WorkerFactory.createWorker(params,this,false);
    }
    
//------------------------------------------------------------------------------

}
