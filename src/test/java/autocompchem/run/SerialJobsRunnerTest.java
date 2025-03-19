package autocompchem.run;


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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
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
import autocompchem.run.jobediting.SetJobParameter;


/**
 * Unit Test for the runner of serial sets of jobs. 
 * 
 * @author Marco Foscato
 */

public class SerialJobsRunnerTest 
{
    private final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    protected File tempDir;
    
    // The ingredients to bake a perceptron
    private ICircumstance c = new MatchText("Iteration 3",
    		InfoChannelType.LOGFEED);
    private Action a = new Action(ActionType.STOP, ActionObject.PARALLELJOB);
    private Situation s = new Situation("SituationType","TestSituation", 
    		new ArrayList<ICircumstance>(Arrays.asList(c)),a);


//-----------------------------------------------------------------------------

    /*
     * Case tested:
     * Run of a plain sequence of steps without any action being triggered,
     * and all finishes within the walltime.
     */
    @Test
    public void testSerialWorkflow() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "10");
        for (int i=0; i<3; i++)
        {
        	main.addStep(new TestJob(roothName+i, 1, 0, 200));
        }

        // Comment out this to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "2", true);
        
        main.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, TestJob.ITERATIONKEY+"*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
        	assertFalse(main.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
    }
    
//-----------------------------------------------------------------------------

    /*
     * Case tested:
     * Run of a plain sequence of step, but the second step hits the walltime.
     */
    @Test
    public void testSerialWorkflow_hitWalltime() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 1);
        main.setParameter("WALLTIME", "3"); 
        for (int i=0; i<3; i++)
        {
        	main.addStep(new TestJob(roothName+i, 2, 0, 950));
        }

        // Comment out this to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "1", true);
        
        main.run();
        
        
        // First step runs to completion
        int n = FileAnalyzer.count(roothName+'0', TestJob.ITERATIONKEY+"*");
    	assertTrue(n>2,"Lines in log 0");
    	assertTrue(n<5,"Lines in log 0");
    	assertFalse(main.getStep(0).isInterrupted, 
    			"Interruption flag on job-0");
    	
    	// Second step is interrupted
        n = FileAnalyzer.count(roothName+'1', TestJob.ITERATIONKEY+"*");
    	assertTrue(n>0,"Lines in log 1");
    	assertTrue(n<4,"Lines in log 1");
    	assertTrue(main.getStep(1).isInterrupted,
    			"Interruption flag on job-"+1);
    	
    	// Third never run
    	File thirdLog = new File(roothName+'2');
    	assertFalse(thirdLog.exists(), "No 3rd log");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testRedoUponNotification() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        // Conditional rerun 1
        ICircumstance c = new MatchText("Iteration 3", 
        		InfoChannelType.LOGFEED);
        Action act = new Action(ActionType.REDO, 
        		ActionObject.FOCUSANDFOLLOWINGJOBS);
        String newPrefix = "RESTART-";
        act.addJobEditingTask(new SetJobParameter(
        		new NamedData(TestJob.PREFIX, newPrefix)));
        Situation sit1 = new Situation("SitTyp", "Sit-ONE", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)),
        		act);
        
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(sit1);
        
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(roothName+"_production_2", 
        		InfoChannelType.LOGFEED));
        
        // The main job
        Job main = JobFactory.createJob(SoftwareId.ACC);
        main.setParameter("WALLTIME", "1000");
        
        // Production subjob 1
        TestJob productionJob1 = new TestJob(roothName+"_production_1",
        		1,0,90);
        productionJob1.setUserDir(tempDir);
        main.addStep(productionJob1);
        
        // Production subjob 2
        TestJob productionJob2 = new TestJob(roothName+"_production_2",
        		1,0,90);
        productionJob2.setUserDir(tempDir);
        main.addStep(productionJob2);
        
        // Make the job that will evaluate the 2nd subjob job and trigger an action
        Job evalJob = new EvaluationJob(productionJob2, main, sitsDB, icDB);
        main.addStep(evalJob);

        // Production subjob 3
        TestJob productionJob3 = new TestJob(roothName+"_production_3",
        		1,0,90);
        productionJob3.setUserDir(tempDir);
        main.addStep(productionJob3);
        
        // Comment out this to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "2", true);
     
        // Run main job
        try {
        main.run();
        } catch (Throwable t) {
        	t.printStackTrace();
        }
        
        /*
         * we expect that Job_#0.1 and Job_#0.2 run just fine and write their 
         * log files to disk. 
         * Then Job_#0.3, which is the evaluation job, 
         * triggers the re-run from Job_#0.2. This
         * causes the archiving of the results from Job_#0.2 into Job_#0.2_1.
         * Note that Job_#0.1 is not archived because it will not be rerun.
         * Then, Job_#0.2 re-runs, creating its log, and Job_#0.3 runs silently
         * because it does not detect any situation that triggers a reaction.
         * Finally, Job_#0.4 runs normally.
         */

        assertTrue((new File(roothName+"_production_1")).exists());
        assertFalse((new File(tempDir + SEP + "Job_#0.1_1")).exists());
        assertEquals(1, 
        		FileUtils.findByREGEX(tempDir, ".*_production_1", true).size());
        
        assertTrue((new File(roothName+"_production_2")).exists());
        assertTrue((new File(tempDir + SEP + "Job_#0.2_1")).exists());
        assertTrue((new File(tempDir + SEP + "Job_#0.2_1" 
        		+ SEP + baseName + "_production_2")).exists());
        assertEquals(2, 
        		FileUtils.findByREGEX(tempDir, ".*_production_2", true).size());
        
        assertTrue((new File(roothName+"_production_3")).exists());
        assertFalse((new File(tempDir + SEP + "Job_#0.3_1")).exists());
        assertEquals(1, 
        		FileUtils.findByREGEX(tempDir, ".*_production_1", true).size());
    }
    
//------------------------------------------------------------------------------

}
