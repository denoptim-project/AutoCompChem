package autocompchem.run;

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
            System.out.println(" " + date.toString());
            System.out.println(" AutoCompChem is initiating the ACC task '" 
                            + task + "'. ");
        }
        
        Worker worker = WorkerFactory.createWorker(params,this);
        worker.performTask();
       

        date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Done with ACC job (" + task	+ ")");
        }
    }

//------------------------------------------------------------------------------

}
