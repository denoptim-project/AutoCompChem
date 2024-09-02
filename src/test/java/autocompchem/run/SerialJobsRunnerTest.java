package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

import autocompchem.files.FileAnalyzer;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;


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
        
        Job master = JobFactory.createJob(AppID.ACC, 3, true);
        master.setParameter("WALLTIME", "10");
        for (int i=0; i<3; i++)
        {
        	master.addStep(new TestJob(roothName+i, 1, 0, 200, false));
        }
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, TestJob.ITERATIONKEY+"*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
        	assertFalse(master.getStep(i).isInterrupted,
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
        
        Job master = JobFactory.createJob(AppID.ACC, 3, true);
        master.setParameter("WALLTIME", "3");
        for (int i=0; i<3; i++)
        {
        	master.addStep(new TestJob(roothName+i, 2, 0, 950, false));
        }
        master.run();
        
        
        // First step runs to completion
        int n = FileAnalyzer.count(roothName+'0', TestJob.ITERATIONKEY+"*");
    	assertTrue(n>2,"Lines in log 0");
    	assertTrue(n<5,"Lines in log 0");
    	assertFalse(master.getStep(0).isInterrupted, 
    			"Interruption flag on job-0");
    	
    	// Second step is interrupted
        n = FileAnalyzer.count(roothName+'1', TestJob.ITERATIONKEY+"*");
    	assertTrue(n>0,"Lines in log 1");
    	assertTrue(n<4,"Lines in log 1");
    	assertTrue(master.getStep(1).isInterrupted,
    			"Interruption flag on job-"+1);
    	
    	// Third never run
    	File thirdLog = new File(roothName+'2');
    	assertFalse(thirdLog.exists(), "No 3rd log");
    }
    
//------------------------------------------------------------------------------

}
