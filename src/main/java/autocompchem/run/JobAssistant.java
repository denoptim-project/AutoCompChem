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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;


import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.ActionApplier;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.InputWriter;
import autocompchem.wiro.WIROConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;


/**
 * A worker that runs a job, with monitoring and troubleshooting capabilities.
 * 
 * @author Marco Foscato
 */

public class JobAssistant extends Worker
{
    /**
     * String defining the task of evaluating any job output
     */
    public static final String ASSISTJOBTASKNAME = "assistJob";

    /**
     * Task about evaluating any job output
     */
    public static final Task ASSISTJOBTASK;
    static {
    	ASSISTJOBTASK = Task.make(ASSISTJOBTASKNAME);
    }
    
    /**
     * String defining the task of healing/fixing any job
     */
    public static final String CUREJOBTASKNAME = "cureJob";

    /**
     * Task about healing/fixing any job output
     */
    public static final Task CUREJOBTASK;
    static {
    	CUREJOBTASK = Task.make(CUREJOBTASKNAME);
    }
    
    /**
     * Keyword defining the job the be assisted
     */
    public static final String PARASSISTEDJOB = "ASSISTEDJOB";
    
    /**
     * The job the be assisted
     */
    private Job assistedJob;

    /**
     * Keyword defining the job needed to perform the assisted job, if any.
     */
    public static final String PARRUNJOB = "RUNJOB";
    
    /**
     * The job needed to run the assisted job, if needed.
     */
    private Job runJob;

    /**
     * Keyword defining the parameter controlling the maximum number of restarts
     * of the assisted worflow, i.e., the assisted job possibly coupled with the 
     * run job, if needed.
     */
    public static final String PARMAXRESTART = "MAXRESTART";
    
    /**
     * The maximum number of restarts of the assisted workflow.
     */
    private int maxRestart = 10;
    
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobAssistant()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
		Set<Task> tmpSet = new HashSet<Task>();
		tmpSet.add(ASSISTJOBTASK);
		tmpSet.add(CUREJOBTASK);
		return Collections.unmodifiableSet(tmpSet);
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/JobAssistant.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new JobAssistant();
    }
    
//------------------------------------------------------------------------------
	
	public void initialize() 
	{   	
    	super.initialize();
    	
		if (hasParameter(PARASSISTEDJOB)) 
		{
			assistedJob = (Job) params.getParameter(PARASSISTEDJOB).getValue();
		}
    	
		if (hasParameter(PARRUNJOB)) 
		{
			runJob = (Job) params.getParameter(PARRUNJOB).getValue();
		} else if (hasParameter(PARASSISTEDJOB)) {
			Terminator.withMsgAndStatus("Missing definition of assisted job. "
					+ "Please, use 'ASSISTEDJOB' in yout input.", -1);
		}
		
		if (hasParameter(PARMAXRESTART)) 
		{
			String value = params.getParameter(PARMAXRESTART).getValueAsString();
			if (NumberUtils.isParsableToInt(value))
			{
				maxRestart = Integer.parseInt(value);
			} else {
				Terminator.withMsgAndStatus("Could not parse string '" + value 
						+ "' to an integer. Please, check your input.", -1);
			}
		}
	}

//------------------------------------------------------------------------------
	
	@Override
	public void performTask() 
	{
    	if (task.equals(ASSISTJOBTASK))
    	{
    		runAssistedJob();
    	} else if (task.equals(CUREJOBTASK)) {	
    		cureAssistedJob();
    	} else {
    		dealWithTaskMismatch();
        }
	}

//------------------------------------------------------------------------------
	
	/**
	 * Running an assisted job means:<ol>
	 * <li>preparing some input</li>
	 * <li>running the actual job 
	 * (possibly monitoring its behavior while it runs)</li>
	 * <li>evaluating the outcome according to the knowledge about possible
	 * situations that may require some actions to fix whatever problem the job
	 * has encountered</li>
	 * <li>if needed, perform any action in response to the situation perceived</li>
	 * <li>if needed, start again from point 1, or terminate the assisted run</li>
	 * </ol>
	 */
	private void runAssistedJob() 
	{   
		// Define the assisted workflow
        ACCJob assistedWorkflow = new ACCJob();
		
		// First step, is the preparation of the input
		ParameterStorage parsToMakeInput = params.copy();
		parsToMakeInput.setParameter(WorkerConstants.PARTASK, 
				InputWriter.PREPAREINPUTTASK.casedID);
		parsToMakeInput.setParameter(WIROConstants.PARJOBDETAILSOBJ, 
				assistedJob);
		
		Job inputPreparationJob = new ACCJob();
		inputPreparationJob.setParameters(parsToMakeInput);
		assistedWorkflow.addStep(inputPreparationJob);
		
		// Next, the actual run of the assisted job
		Job monitoredRun = new ACCJob();
		//monitoredRun.addStep(monitoringJob);
		monitoredRun.addStep(runJob);
		assistedWorkflow.addStep(monitoredRun);
		
		// Finally, the evaluation job
		ParameterStorage parsToEvaluationJob = params.copy();
		parsToEvaluationJob.setParameter(WorkerConstants.PARTASK,
				JobEvaluator.EVALUATEJOBTASK.casedID);
		//TODO: the parameters defined on the constructor are overwritten by setParameters
		EvaluationJob evalJob = new EvaluationJob(assistedJob);
		evalJob.setParameters(parsToEvaluationJob);
		assistedWorkflow.addStep(evalJob);
		
		// Run the assisted workflow, possibly including restarts
		for (int i=0; i<maxRestart; i++)
		{
        	logger.info(System.getProperty("line.separator") 
        				+ "Assisted execution of a nested workflow ("
        				+ "Attempt: " + i + ")");
        	
        	runJob.setParameter("Version", i);
        	
        	assistedWorkflow.resetRunStatus();
        	
            try {
            	assistedWorkflow.run();
            } catch (Throwable t) {
            	t.printStackTrace();
            }
            
           if (evalJob.requestsAction())
           {
        	   Job reactiontriggeringJob = evalJob.getReactionTriggeringJob();
        	   Job editedAssistedJob = healJob(reactiontriggeringJob, 
        			   evalJob.getRequestedAction(), i, myJob, logger);
        	   inputPreparationJob.setParameter(WIROConstants.PARJOBDETAILSOBJ, 
        			   editedAssistedJob);
           } else {
        	   break;
           }
		}
	}

//------------------------------------------------------------------------------
	
	/**
	 * Cure an assisted job means:<ol>
	 * <li>evaluating the outcome according to the knowledge about possible
	 * situations that may require some actions to fix whatever problem the job
	 * has encountered</li>
	 * <li>if needed, perform any action in response to the situation perceived</li>
	 * <li>if needed, prepare a new input for to heal the assisted job</li>
	 * </ol>
	 */
	private void cureAssistedJob() 
	{	
		// First, the evaluation job
		ParameterStorage parsToEvalJob = params.copy();
		parsToEvalJob.setParameter(WorkerConstants.PARTASK, 
					JobEvaluator.EVALUATEJOBTASK.casedID);
		EvaluationJob evalJob = (EvaluationJob) JobFactory.createJob(
				parsToEvalJob);
		
		evalJob.run();
		
        if (evalJob.requestsAction())
        {
        	// Then, if needed, apply any error-handling response
    	    Job reactiontriggeringJob = evalJob.getReactionTriggeringJob();
    	    Job editedAssistedJob = healJob(reactiontriggeringJob, 
    	 		   evalJob.getRequestedAction(), 0, myJob, logger);
    	    
    	    // and, finally, make a new input
    	    ParameterStorage parsToMakeInput = params.copy();
    	    parsToMakeInput.setParameter(WorkerConstants.PARTASK, 
    		 	   InputWriter.PREPAREINPUTTASK.casedID);
    	    parsToMakeInput.setParameter(WIROConstants.PARJOBDETAILSOBJ, 
    		 	   editedAssistedJob);
    	    Job inputPreparationJob = JobFactory.createJob(parsToMakeInput);
    	   
    	    inputPreparationJob.run();
        }
        
        // Project output
        for (NamedData dataToExpose : evalJob.exposedOutput.getAllNamedData().values())
        {
        	exposeOutputData(dataToExpose);
        }
	}

//------------------------------------------------------------------------------
	
	/**
	 * Edits a job trying to heal it, i.e., change parts of it according to a 
	 * given recipe, i.e., the cure.
	 * @param jobToHeal the job the be healed.
	 * @param cure the action that is to be performed to cure the job.
	 * @param restartCounter integer used to identify the sequence of healing 
	 * attempts.
	 * @param requestingJob the job that requests the healing of another job.
	 * @param logger logging tool, but can be <code>null</code> so no logging
	 * will be done.
	 * @return
	 */
	public static Job healJob(Job jobToHeal, Action cure, int restartCounter,
			Job requestingJob, Logger logger) 
	{
		if (logger!=null)
		{
			logger.info("Attempting to heal job. Reaction: " 
				+ cure.getType() + " " 
				+ cure.getObject());
		}
			
		Job jobResultingFromAction = null;
		if (jobToHeal.hasContainer())
		{
			if (jobToHeal.getContainer().runsParallelSubjobs())
			{
				// We have evaluated a job that is a part of a batch
				// so, any action must be compatible with the lack
				// of a linear workflow.
				List<Job> newJobSteps = 
						ActionApplier.performActionOnParallelBatch(
								cure,   //action to perform
								jobToHeal.getContainer(), //parallel batch
								jobToHeal, //job causing the reaction
								(EvaluationJob) requestingJob, 
								restartCounter);
				jobToHeal.getContainer().steps = newJobSteps;
			} else {
				int idxStepEvaluated = jobToHeal
						.getContainer().getSteps().indexOf(
								jobToHeal);
				ActionApplier.performActionOnSerialWorkflow(
						cure,   //action to perform
						jobToHeal.getContainer(), //serial workflow
						idxStepEvaluated, //id of step triggering reaction
						restartCounter); 
			}
			jobResultingFromAction = jobToHeal.getContainer();
		} else {
			if (jobToHeal.getNumberOfSteps() > 0)
			{
				throw new IllegalArgumentException("Unexpected workflow"
						+ " marked as job to heal!");
			} else {
				// jobToHeal is a single, self-contained job.
				// We can add any preliminary step only by embedding
				// it into a workflow.
				Job embeddingWorkflow = JobFactory.createTypedJob(jobToHeal);
				embeddingWorkflow.addStep(jobToHeal);
	
				ActionApplier.performActionOnSerialWorkflow(
						cure,   //action to perform
						embeddingWorkflow, //serial workflow
						0, //id of step triggering reaction
						restartCounter);
				jobResultingFromAction = embeddingWorkflow;
			}
		}
		return jobResultingFromAction;
	}
	
//------------------------------------------------------------------------------

}
