package autocompchem.run;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.utils.NumberUtils;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;

/**
 * A class of {@link ACCJob}s that in meant to evaluate other jobs periodically.
 * This is basically an {@link EvaluationJob} that is scheduled to repeat 
 * itself.
 *
 * @author Marco Foscato
 */


public class MonitoringJob extends EvaluationJob
{
	/**
	 * The initial delay before first run of job (in milliseconds)
	 */
	protected long delay = 2000;
	
	/**
	 * The period between each run of this job (in milliseconds)
	 */
	protected long period = 2000; 
	
	/**
	 * Key of parameter defining the initial delay. 
	 */
	public static final String DELAYPAR = "INITIALDELAY";
	
	/**
	 * Key of parameter defining the units of the initial delay. 
	 * Default: SECONDS.
	 */
	public static final String DELAYUNITS = "INITIALDELAY-TIMEUNITS";
	
	/**
	 * Key of parameter defining the period between each monitoring event.
	 */
	public static final String PERIODPAR = "PERIOD";
	
	/**
	 * Key of parameter defining the units of the period. 
	 * Default: SECONDS.
	 */
	public static final String PERIODUNITS = "PERIOD-TIMEUNITS";
	
	
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
    
    public MonitoringJob() 
    {
    	super();
	}
	
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies only what to evaluate. All the rest is expected
	 * to be set afterwards.
     * @param jobToEvaluate the job to be evaluated (single step isolated job, 
     * or a single step in a workflow, of a job belonging to a batch of jobs).
     */

    public MonitoringJob(Job jobToEvaluate)
    {
        super(jobToEvaluate);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor.
     * @param jobToEvaluate the job to be evaluated (single step isolated job, 
     * or a single step in a workflow, of a job belonging to a batch of jobs).
     * @param sitsDB the collection of {@link Situation}s that this 
     * {@link EvaluationJob} is made aware of.
     * @param icDB the collection of means give to this {@link EvaluationJob}
     * to perceive the {@link Situation}s.
     * @param delay the delay (milliseconds) after which this monitor should  
     * start monitoring once it is started.
     * @param period the time (millisecods) between each monitoring event 
     * and the next one.
     */

    public MonitoringJob(Job jobToEvaluate, 
    		SituationBase sitsDB, InfoChannelBase icDB, 
    		long delay, long period)
    {
        super(jobToEvaluate, sitsDB, icDB);
        this.delay = delay;
        this.period = period;
    }
    
//------------------------------------------------------------------------------

    /**
     * @returns the initial delay in milliseconds
     */
    
    public long getDelay()
    {
    	return delay;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @returns the period in milliseconds
     */
    
    public long getPeriod() 
    {
		return period;
	}
    
//------------------------------------------------------------------------------
    
    /**
     * Set this job parameters.
     * @param params the new set of parameters
     */
    
    @Override
    public void setParameters(ParameterStorage params)
    {
    	TimeUnit time = TimeUnit.MILLISECONDS;
    	String acceptableUnits = "";
    	for (TimeUnit u : TimeUnit.values())
    	{
    		acceptableUnits = acceptableUnits + " " + u.toString();
    	}
    	
    	if (params.contains(DELAYPAR))
    	{
			delay = NumberUtils.parseValueOrExpressionToInt(
					params.getParameterValue(DELAYPAR));
    	}
    	
    	if (params.contains(DELAYUNITS))
    	{
    		String units = params.getParameter(DELAYUNITS).getValueAsString();
			if (units.equals(TimeUnit.DAYS.toString())
					|| units.equals(TimeUnit.HOURS.toString()) 
					|| units.equals(TimeUnit.MINUTES.toString())
					|| units.equals(TimeUnit.SECONDS.toString())
					|| units.equals(TimeUnit.MILLISECONDS.toString()))	
			{
				
				delay = time.convert(delay, TimeUnit.valueOf(units));
			} else {
				throw new IllegalArgumentException("String '" + units 
						+ "' is not a known time unit. Check parameter '" 
						+ DELAYUNITS + "'. Use one of "
						+ acceptableUnits);
			}
    	} else {
    		if (params.contains(DELAYPAR))
    		{
				// default units fro input is seconds, but is milliseconds in the
				// code using the value.
    			delay = delay*1000;
    		}
    	}
    	
    	if (params.contains(PERIODPAR))
    	{
			period = NumberUtils.parseValueOrExpressionToInt(
					params.getParameterValue(PERIODPAR));
    	}
    	
    	if (params.contains(PERIODUNITS))
    	{
    		String units = params.getParameter(PERIODUNITS).getValueAsString();
			if (units.equals(TimeUnit.DAYS.toString())
					|| units.equals(TimeUnit.HOURS.toString()) 
					|| units.equals(TimeUnit.MINUTES.toString())
					|| units.equals(TimeUnit.SECONDS.toString())
					|| units.equals(TimeUnit.MILLISECONDS.toString()))	
			{
				
				period = time.convert(period, TimeUnit.valueOf(units));
			} else {
				throw new IllegalArgumentException("String '" + units 
						+ "' is not a known time unit. Check parameter '" 
						+ PERIODUNITS + "'. Use one of " 
						+ acceptableUnits);
			}
    	} else {
    		if (params.contains(PERIODPAR))
    		{
				// default units for input is seconds, but is milliseconds in the
				// code using the value.
    			period = period*1000; 
    		}
    	}

    	super.setParameters(params);
    }
    
//------------------------------------------------------------------------------

	/**
     * Sends this job to an executing thread managed by an existing, and
     * pre-started execution service. The job will be re-scheduled according to 
     * the period defined upon construction of this job.
     * @param executor the executing service.
     * @return a Future representing pending completion of the task.
     */
    
    @SuppressWarnings("unchecked")
	@Override
  	protected Future<Object> submitThread(ExecutorService executor) 
  	{
    	if (!(executor instanceof ScheduledThreadPoolExecutor))
    	{
    		throw new IllegalArgumentException("The execution service of a "
    				+ this.getClass().getName() + " must be ");
    	}
    	ScheduledThreadPoolExecutor tpExecutor = 
    			(ScheduledThreadPoolExecutor) executor;
  		return (Future<Object>) tpExecutor.scheduleAtFixedRate(this, delay, 
  				period, TimeUnit.MILLISECONDS);
  	}
    
//------------------------------------------------------------------------------

    /**
     * Adjust notification to trigger reaction to the action that this job is 
     * requesting.
     */
    @Override
	protected void notifyObserver()
    {
    	if (observer!=null && requestsAction())
        {
        	observer.reactToRequestOfAction(getRequestedAction(), this);
        }			
    }

//------------------------------------------------------------------------------

}
