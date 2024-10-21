package autocompchem.run;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.jobediting.Action;
import autocompchem.worker.WorkerConstants;

/**
 * A class of {@link ACCJob}s that in meant to evaluate other jobs.
 *
 * @author Marco Foscato
 */

public class EvaluationJob extends ACCJob 
{
	/**
	 * The job that is evaluated by this job.
	 */
	private Job focusJob;
	
	/**
	 * The reaction to the results of the evaluation
	 */
	private Action reaction;
 
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	
    public EvaluationJob() {
    	super();
        setParallelizable(true);
        setNumberOfThreads(1);
        params.setParameter(WorkerConstants.PARTASK, 
        		JobEvaluator.EVALUATEJOBTASK.casedID);
	}
    
//------------------------------------------------------------------------------

    /**
     * Constructor.
     * @param jobToEvaluate the job to be evaluated (single step isolated job, 
     * or a single step in a workflow, of a job belonging to a batch of jobs).
     * @param containerOfJobToEvaluate the workflow or batch that contains the
     * step to be evaluate. This can be the same of the 
     * <code>jobToEvaluate</code>, meaning that such job is not a part of a 
     * workflow or batch.
     * @param sitsDB the collection of {@link Situation}s that this 
     * {@link EvaluationJob} is made aware of.
     * @param icDB the collection of means give to this {@link EvaluationJob}
     * to perceive the {@link Situation}s.
     */

    public EvaluationJob(Job jobToEvaluate, Job containerOfJobToEvaluate, 
    		SituationBase sitsDB, InfoChannelBase icDB)
    {
        this();
        focusJob = jobToEvaluate;
        params.setParameter(ParameterConstants.JOBTOEVALPARENT,
        		containerOfJobToEvaluate);
        params.setParameter(ParameterConstants.JOBTOEVALUATE, jobToEvaluate);
        params.setParameter(ParameterConstants.SITUATIONSDB, sitsDB);
        params.setParameter(ParameterConstants.INFOCHANNELSDB, icDB);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if this job is requesting any action.
     * @return <code>true</code> if this job is requesting any action
     */
    
    public boolean requestsAction()
    {
    	return reaction!=null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the reaction triggered by the evaluation performed by this job.
     * @param reaction
     */
    public void setRequestedAction(Action reaction)
    {
    	this.reaction = reaction;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the requested action or null, if no action is requested
     */
    
    public Action getRequestedAction()
    {
    	return reaction;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the job that is evaluated by this one.
     * @return the job that is evaluated by this one.
     */
    public Job getFocusJob()
    {
    	return focusJob;
    }  
    
//------------------------------------------------------------------------------

    /**
     * Adjust notification to trigger reaction to the action that this job is 
     * requesting.
     */
    @Override
	protected void notifyObserver() 
    {
    	if (observer!=null)
    	{
	    	if (requestsAction())
	        {
	        	observer.reactToRequestOfAction(getRequestedAction(), this);
	        } else {
	        	observer.notifyTermination(this);
	        }
    	}			
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This method resets any information about the running of this job so that
     * it looks as if it had never run.
     */
    @Override
    public void resetRunStatus()
    {
    	reaction = null;
    	super.resetRunStatus();
    }
    
//------------------------------------------------------------------------------

}
