package autocompchem.run;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
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

    public EvaluationJob(Job jobToEvaluate)
    {
        this(jobToEvaluate, null, null, null);
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
        params.setParameter(ParameterConstants.JOBTOEVALUATE, jobToEvaluate);
        if (sitsDB!=null)
            params.setParameter(ParameterConstants.SITUATIONSDB, sitsDB);
        if (icDB!=null)
            params.setParameter(ParameterConstants.INFOCHANNELSDB, icDB);
    }
    
//------------------------------------------------------------------------------

    /**
     * Set this job parameters
     * @param params the new set of parameters
     */

    @Override
    public void setParameters(ParameterStorage params)
    {
    	super.setParameters(params);
    	if (params.contains(ParameterConstants.JOBTOEVALUATE))
    	{
    		focusJob = (Job) params.getParameter(
    				ParameterConstants.JOBTOEVALUATE).getValue();
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * The job triggering a reaction may be the job being evaluated or a step 
     * contained in the job being evaluated. Hence, we talk about the innermost
     * job.
     * @return the innermost job considered responsible for the situation that 
     * this job perceived and that triggered the request for a reaction. 
     */
  	public Job getReactionTriggeringJob()
  	{
  		if (exposedOutput.contains(JobEvaluator.NUMSTEPSKEY))
  		{
  			int idx = ((int) exposedOutput.getNamedData(
  					JobEvaluator.NUMSTEPSKEY).getValue()) - 1;
  			if (idx > 1)
  			{
  				if (focusJob.getNumberOfSteps() > 0)
  				{
  					return focusJob.steps.get(idx);
  				}
  			}
  		}
  		return focusJob;
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
     * requesting. Note that the reaction applies to the workflow this job
     * belongs to, so we notify the observer only if the evaluated job is part 
     * of the same batch/workflow of this very job.
     */
    @Override
	protected void notifyObserver() 
    {
    	if (observer!=null)
    	{
	    	if (requestsAction() && hasContainer() 
	    			&& focusJob.getContainers().contains(getContainer()))
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
