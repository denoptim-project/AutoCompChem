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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;

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
import autocompchem.text.TextAnalyzer;


/**
 * Unit Test for the runner of embarrassingly parallel sets of jobs. 
 * 
 * @author Marco Foscato
 */

public class TestJobTest 
{
    private final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    protected File tempDir;
    
//-----------------------------------------------------------------------------

    /*
     * Only meant to test the private class TestJob
     */

    @Test
    public void testLoggingOfTestJob() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String log = tempDir.getAbsolutePath() + SEP + "testjob.log";
    	Job job = new TestJob(log, 1, 80, 99, false);
    	job.run();
    	
    	int n = FileAnalyzer.count(log, TestJob.ITERATIONKEY+"*");
    	assertTrue(n>8);
    	assertTrue(n<12);
    	assertFalse(job.isInterrupted);
    	assertTrue(job.isCompleted());
    }
    
//------------------------------------------------------------------------------

}
