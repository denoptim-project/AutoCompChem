package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
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

    //TODO-gg add other senarios
    
//------------------------------------------------------------------------------

}
