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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.perception.Perceptron;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.ActionApplier;
import autocompchem.run.jobediting.DataArchivingRule;
import autocompchem.run.jobediting.SetJobParameter;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.WIROConstants;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


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
		} else {
			Terminator.withMsgAndStatus("Missing definition of assisted job. "
					+ "Please, use 'ASSISTEDJOB' in yout input.", -1);
		}
    	
		if (hasParameter(PARRUNJOB)) 
		{
			runJob = (Job) params.getParameter(PARRUNJOB).getValue();
		} else {
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
		
		/*
		if (hasParameter(ParameterConstants. )) 
		{
			String pathNames = params.getParameter(
					ParameterConstants. ).getValueAsString();
			
		}
		*/
		
	}

//------------------------------------------------------------------------------
	
	@Override
	public void performTask() 
	{
		//TODO replace with sensible content: this is only for testing
        ICircumstance c = new MatchText("Geometry optimization did not converge", 
        		InfoChannelType.LOGFEED);
        Action act = new Action(ActionType.REDO, 
        		ActionObject.FOCUSANDFOLLOWINGJOBS);
        act.addJobEditingTask(new SetJobParameter(
        		new NamedData("___DUMMY____", "_______xtblast.sdf")));
        act.addJobArchivingDetails(DataArchivingRule.makeMoveRule("*"));
        act.addJobArchivingDetails(DataArchivingRule.makeCopyRule("*.sdf"));
        Situation sit1 = new Situation("ERROR", "Not_converged", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)),
        		act);
        
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(sit1);
        
        InfoChannel ic = new FileAsSource("cli24.out", 
        		InfoChannelType.LOGFEED);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(ic);

        
        Gson writer = ACCJson.getWriter();
        IOtools.writeTXTAppend(new File("/tmp/ic.json"), writer.toJson(ic), false);
        IOtools.writeTXTAppend(new File("/tmp/act.json"), writer.toJson(act), false);
        IOtools.writeTXTAppend(new File("/tmp/c.json"), writer.toJson(c), false);
        IOtools.writeTXTAppend(new File("/tmp/s.json"), writer.toJson(sit1), false);
        IOtools.writeTXTAppend(new File("/tmp/icDB.json"), writer.toJson(icDB), false);
        
		
        
		// Define the assisted workflow
        ACCJob assistedWorkflow = new ACCJob();
        //TODO set this as parent of assistedWorkflow
		
		// First step, is the preparation of the input
		ParameterStorage parsToMakeInput = params.clone();
		parsToMakeInput.setParameter(WorkerConstants.PARTASK, 
				Task.make("prepareInput").casedID);
		parsToMakeInput.setParameter(WIROConstants.PARJOBDETAILSOBJ, 
				assistedJob);
		
		Job inputPreparationJob = new ACCJob();
		inputPreparationJob.setParameters(parsToMakeInput);
		assistedWorkflow.addStep(inputPreparationJob);
		
		// Next, the actual run of the assisted job
		Job monitoredRun = new ACCJob();
		//TODO: set parent-child relation
		//monitoredRun.addStep(monitoringJob);
		monitoredRun.addStep(runJob);
		assistedWorkflow.addStep(monitoredRun);
		
		// Finally, the evaluation job
		EvaluationJob evalJob = new EvaluationJob(assistedJob, null, sitsDB, icDB);
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
        			   evalJob.getRequestedAction(), i);
        	   inputPreparationJob.setParameter(WIROConstants.PARJOBDETAILSOBJ, 
        			   editedAssistedJob);
           } else {
        	   break;
           }
		}
	}


//------------------------------------------------------------------------------
	
	private Job healJob(Job jobToHeal, Action cure, int restartCounter) 
	{
		logger.info("Attempting to heal job. Reaction: " 
				+ cure.getType() + " " 
				+ cure.getObject());
	
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
								(EvaluationJob) myJob, //job doing the evaluation 
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
	
	
//------------------------------------------------------------------------------

}
