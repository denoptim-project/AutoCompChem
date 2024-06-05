package autocompchem.io;

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

import com.google.gson.Gson;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.run.ACCJob;
import autocompchem.run.AppID;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.MonitoringJob;
import autocompchem.run.ShellJob;


/**
 * Unit Test for JSON reader/writer
 * 
 * @author Marco Foscato
 */

public class ACCJsonTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testHandlingOfJobs() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        File jsonfile = new File(tempDir.getAbsolutePath() + SEP + "file.json");
    	Job job = JobFactory.createJob(AppID.ACC);
        job.addStep(new ShellJob("/bin/bash", "some/path/name.sh", "-lr --mm"));
        Job nest = new ACCJob();
        nest.addStep(new ACCJob());
        nest.addStep(new ACCJob());
        Job nest2 = new ACCJob();
        nest2.addStep(new ACCJob());
        nest.addStep(nest2);
        job.addStep(nest);
        job.addStep(new MonitoringJob());
        job.addStep(new EvaluationJob());
        job.addStep(JobFactory.createJob(AppID.ACC));
        CompChemJob ccj = new CompChemJob();
        ccj.setDirective(new Directive("DUMMY"));
        job.addStep(ccj);
        CompChemJob ccj2 = new CompChemJob();
        Directive d2 = new Directive("Second");
        d2.addKeyword(new Keyword("SecondKey",false,"value2"));
        ccj2.setDirective(d2);
        job.addStep(ccj2);
        CompChemJob ccj3 = new CompChemJob();
        Directive d3 = new Directive("Third");
        d3.addDirectiveData(new DirectiveData("Data3", 
        		new ArrayList<String>(Arrays.asList("1","two","3 and four"))));
        ccj3.setDirective(d3);
        job.addStep(ccj3);
        
        Gson writer = ACCJson.getWriter();
        
        IOtools.writeTXTAppend(jsonfile, writer.toJson(job), false);
        
        Job job2 = (Job) IOtools.readJsonFile(jsonfile, Job.class);
        
        assertTrue(job.equals(job2));
    }
    
//------------------------------------------------------------------------------

}
