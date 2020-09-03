package autocompchem.run;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.SituationBase;

/**
 * A class of {@link ACCJob}s that in meant to evaluate other jobs periodically.
 * This is basically an {@link EvaluationJob} that is scheduled to repeat 
 * itself.
 *
 * @author Marco Foscato
 */


//TODO: implement choice of this class in JobFactory


public class MonitoringJob extends EvaluationJob
{
	/**
	 * The initial delay before first run of job
	 */
	protected long delay = 2000;
	/**
	 * The period between each run of this job (in milliseconds)
	 */
	protected long period = 2000; 
	
	//TODO butta
	protected Date date = new Date();
	protected SimpleDateFormat formatter = 
			new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss.SSS ");
 
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public MonitoringJob(Job jobToEvaluate, SituationBase sitsDB,
    		InfoChannelBase icDB, long delay, long period)
    {
        super(jobToEvaluate,sitsDB,icDB);
        this.delay = delay;
        this.period = period;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sends this job to an executing thread managed by an existing, and
     * pre-started thread manager. The job will be re-scheduled according to the
     * period defined upon construction of this job.
     * @param tpExecutor the manager of the job executing threads.
     * @return a Future representing pending completion of the task.
     */
    
    @Override
  	protected Future<?> submitThread(ScheduledThreadPoolExecutor tpExecutor) 
  	{
  		return tpExecutor.scheduleAtFixedRate(this, delay, period, 
  				TimeUnit.MILLISECONDS);
  	}

//------------------------------------------------------------------------------

}
