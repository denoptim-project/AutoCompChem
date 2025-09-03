package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.io.IOtools;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.wiro.WIROConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Unit Test 
 * 
 * @author Marco Foscato
 */

public class JobEvaluatorTest 
{
    private final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    protected File tempDir;
    
//-----------------------------------------------------------------------------

    @Test
    public void testJobEvaluator_standalone_on_serialWorkflow() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File existingFile = new File(
        		tempDir.getAbsolutePath() + SEP + "file.out");
        IOtools.writeTXTAppend(existingFile, "Text TRIGGERS REACTION", false);

        // NB: this situation is not related to any specific step of the job
        // being evaluated. Also, without an interpreted of the ICfeed that
        // can split the ICfeed into steps, the perceptron can only assume that
        // the situation is triggered by the first subjob of a batch/workflow.
        String situationName = "TestSituation-1.23";
        Situation situationWithAction = new Situation("SituationType",
        		situationName, new ArrayList<ICircumstance>(Arrays.asList(
        				new MatchText(".*TRIGGERS REACTION.*", 
        						InfoChannelType.LOGFEED))));
        situationWithAction.setReaction(new Action(ActionType.REDO, 
        		ActionObject.FOCUSJOB));
        
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(situationWithAction);
        
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(existingFile.getAbsolutePath(), 
        		InfoChannelType.LOGFEED));
        
    	Job workflowToEvaluate = new Job();
    	Job step1 = new ShellJob("command","-opt 1","arg");
    	step1.setParameter("ENV", "name of envaronment");
    	workflowToEvaluate.addStep(step1);
    	final String PARAMNAME = "PPAARRAAMM";
    	final String PARAMVALUE = "12345 acb ABC";
    	Job step2 = new ACCJob();
    	step2.setParameter(PARAMNAME, PARAMVALUE);
    	workflowToEvaluate.addStep(step2);
    	
    	Job evalJob = new EvaluationJob(workflowToEvaluate.getStep(0), 
    			workflowToEvaluate, sitsDB, icDB);

    	JobEvaluator w = (JobEvaluator) WorkerFactory.createWorker(evalJob);
    	w.performTask();
    	
    	NamedData situationData = evalJob.exposedOutput.getNamedData(
    			JobEvaluator.SITUATIONOUTKEY, true); 
    	Situation situationPerceived = (Situation) situationData.getValue();
    	assertNotNull(situationData);
    	assertEquals(situationName, situationPerceived.getRefName());
    	assertNotNull(situationPerceived.getReaction());
    	assertEquals(ActionType.REDO, situationPerceived.getReaction().getType());
    }
      
//-----------------------------------------------------------------------------

    @Test
    public void testJobEvaluator_standalone_on_parallelBatch() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File existingFile = new File(
        		tempDir.getAbsolutePath() + SEP + "file.out");
        IOtools.writeTXTAppend(existingFile, "Text TRIGGERS REACTION", false);

        // NB: this situation is not related to any specific step of the job
        // being evaluated. Also, without an interpreted of the ICfeed that
        // can split the ICfeed into steps, the perceptron can only assume that
        // the situation is triggered by the first subjob of a batch/workflow.
        String situationName = "TestSituation-1.23";
        Situation situationWithAction = new Situation("SituationType",
        		situationName, new ArrayList<ICircumstance>(Arrays.asList(
        				new MatchText(".*TRIGGERS REACTION.*", 
        						InfoChannelType.LOGFEED))));
        situationWithAction.setReaction(new Action(ActionType.REDO, 
        		ActionObject.FOCUSJOB));
        
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(situationWithAction);
        
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(existingFile.getAbsolutePath(), 
        		InfoChannelType.LOGFEED));
        
    	Job batchToEvaluate = new Job();
    	batchToEvaluate.setNumberOfThreads(2);
    	Job step1 = new ShellJob("command","-opt 1","arg");
    	step1.setParameter("ENV", "name of envaronment");
    	batchToEvaluate.addStep(step1);
    	final String PARAMNAME = "PPAARRAAMM";
    	final String PARAMVALUE = "12345 acb ABC";
    	Job step2 = new ACCJob();
    	step2.setParameter(PARAMNAME, PARAMVALUE);
    	batchToEvaluate.addStep(step2);
    	
    	Job evalJob = new EvaluationJob(batchToEvaluate.getStep(1), 
    			batchToEvaluate, sitsDB, icDB);

        File newJobDef = new File(
        		tempDir.getAbsolutePath() + SEP + "newJobDef.json");
    	evalJob.setParameter(WIROConstants.PAROUTFILE.toString(), 
    			newJobDef.getAbsolutePath());
    	JobEvaluator w = (JobEvaluator) WorkerFactory.createWorker(evalJob);
    	w.performTask();
    	
    	NamedData situationData = evalJob.exposedOutput.getNamedData(
    			JobEvaluator.SITUATIONOUTKEY, true); 
    	Situation situationPerceived = (Situation) situationData.getValue();
    	assertNotNull(situationData);
    	assertEquals(situationName, situationPerceived.getRefName());
    	assertNotNull(situationPerceived.getReaction());
    	assertEquals(ActionType.REDO, situationPerceived.getReaction().getType());
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testJobEvaluator_tolerant() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");
        File existingFile = new File(
        		tempDir.getAbsolutePath() + SEP + "file.out");
        IOtools.writeTXTAppend(existingFile, "Text to match is XYZ", false);

        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation( new Situation("SituationType","TestSituation", 
        		new ArrayList<ICircumstance>(Arrays.asList(
        				new MatchText(".*XYZ.*", InfoChannelType.OUTPUTFILE)))));
        
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(existingFile.getAbsolutePath(), 
        		InfoChannelType.OUTPUTFILE));
        
    	Job jobToEvaluate = new Job();
    	Job evalJob = new EvaluationJob(jobToEvaluate, jobToEvaluate, sitsDB, icDB);
    	evalJob.setParameter(ParameterConstants.TOLERATEMISSINGIC, "true");
    	
    	JobEvaluator w = (JobEvaluator) WorkerFactory.createWorker(evalJob);
    	NamedDataCollector output = new NamedDataCollector();
    	w.setDataCollector(output);
    	w.performTask();
    	assertNotNull(output.getNamedData(JobEvaluator.SITUATIONOUTKEY, true));
    	
    	icDB.addChannel(new FileAsSource("non_existing_pathname", 
    			InfoChannelType.OUTPUTFILE));
    	output = new NamedDataCollector();
    	w.setDataCollector(output);
    	w.performTask();
    	assertNotNull(output.getNamedData(JobEvaluator.SITUATIONOUTKEY, true));
    	
    	evalJob.setParameter(ParameterConstants.TOLERATEMISSINGIC, "false");
    	
    	//To avoid seeing the exception in stdout
    	evalJob.setParameter(ParameterConstants.VERBOSITY, "0");
    	
    	w = (JobEvaluator) WorkerFactory.createWorker(evalJob);
    	output = new NamedDataCollector();
    	w.setDataCollector(output);
    	w.performTask();
    	assertNotNull(output.getNamedData(JobEvaluator.EXCEPTION, true));
    }
    
//------------------------------------------------------------------------------

}
