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

import autocompchem.datacollections.ParameterStorage;
import autocompchem.utils.TimeUtils;
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
        this.appID = SoftwareId.ACC;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a job given its parameters.
     * @param params the parameters to append to this job.
     */

    public ACCJob(ParameterStorage params)
    {
        this();
        setParameters(params);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that may return a subclass
     */
    @Override
    public Job makeInstance()
    {
    	return new ACCJob();
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
    		logger.info("Running job container " + this.toString());
    		return;
    	}

        // Here we are sure the params include this keyword
        String task = this.params.getParameter(WorkerConstants.PARTASK)
        		.getValue().toString();
        
        logger.debug("AutoCompChem is initiating the ACC task '" 
                            + task + "' - " + TimeUtils.getTimestamp());

        Worker worker = null;
		try {
			worker = WorkerFactory.createWorker(this);
		} catch (Throwable t) {
			hasException = true;
			thrownExc = new Error("Unable to make worker for " 
					+ params.getParameterValue(WorkerConstants.PARTASK), t);
			stopJob();
		}
		try {
			worker.performTask();
		} catch (Throwable t) {
			hasException = true;
			thrownExc = t;
		}
		
        logger.debug("Done with ACC job (" + task	+ ") - " 
        		+ TimeUtils.getTimestamp());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the {@link Worker} that can perform the task required in this job
     * or null.
     */
    public Worker getUninitializedWorker()
    {
    	// Check for any ACC task...
    	if (!hasParameter(WorkerConstants.PARTASK))
    	{
    		return null;
    	}
    	Worker worker = null;
    	try {
			worker = WorkerFactory.createWorker(this, false);
		} catch (ClassNotFoundException e) {
			throw new Error("Unable to make worker for " 
					+ params.getParameterValue(WorkerConstants.PARTASK));
		}
       return worker;
    }
    
//------------------------------------------------------------------------------

}
