package autocompchem.run;

import java.util.Date;

import autocompchem.datacollections.Parameter;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.SituationBase;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * A class of {@link ACCJob}s that in meant to evaluate other jobs.
 *
 * @author Marco Foscato
 */

public class EvaluationJob extends ACCJob 
{
	private Job jobToEvaluate;
	private SituationBase sitsDB;
	private InfoChannelBase icDB;
	
 
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public EvaluationJob(Job jobToEvaluate, SituationBase sitsDB,
    		InfoChannelBase icDB)
    {
        super();
        setParallelizable(true);
        setNumberOfThreads(1);
        params.setParameter(new Parameter(WorkerConstants.PARTASK, 
        		TaskID.EVALUATEJOB.toString()));
        this.jobToEvaluate = jobToEvaluate;
        this.sitsDB = sitsDB;
        this.icDB = icDB;
    }

//------------------------------------------------------------------------------

    /**
     * Runs the given task
     */

    @Override
    public void runThisJobSubClassSpecific()
    {   
        Date date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println(" AutoCompChem is initiating the ACC task '" 
                            + TaskID.EVALUATEJOB + "'. ");
        }
        
        JobEvaluator worker = new JobEvaluator(sitsDB, icDB, jobToEvaluate);
        worker.setDataCollector(this.exposedOutput);
        worker.initialize();
        worker.performTask();

        date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Done with ACC job (" + TaskID.EVALUATEJOB+")");
        }
    }

//------------------------------------------------------------------------------

}
