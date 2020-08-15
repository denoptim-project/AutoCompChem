package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileAnalyzer;


/**
 * Unit Test for ShellJob. 
 * 
 * @author Marco Foscato
 */

public class ShellJobTest 
{

    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testExposeShellOuputEnv() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define paths in tmp dir
        File script = new File(tempDir.getAbsolutePath() + SEP + "script.sh");
        try 
        {
            // Make a SHELL script that is only writing the date on a given file
            FileWriter writer = new FileWriter(script);
            writer.write("echo \"RESULT-A=123.456\""+NL);
            writer.write("echo \"RESULT-B=goodResult\""+NL);
            writer.write("echo \"Done with script!\"");
            writer.close();

            //FIXME: this makes unit testing platform dependent!!!
            
            // Choose shell flavour
            String shellFlvr = "/bin/sh";
/*
//TODO: maybe one day we'll check for available interpreters, and run the test 
only if we find a good one. 
See {@Link JobTest} for a possible solution.
*/

            Job job = new ShellJob(shellFlvr,script.getAbsolutePath(),"");
            job.setUserDir(tempDir);
            job.setRedirectOutErr(true);
            job.run();
            
            File outFile = (File) job.getOutput("LOG").getValueAsObjectSubclass();
            assertTrue(outFile.exists(), "Log file should exist");
            

            ArrayList<String> resLines = FileAnalyzer.grep(outFile.getAbsolutePath(), 
            		new HashSet<String>(Arrays.asList("RESULT")));
            
            assertEquals(2,resLines.size(),"Number of matches in log file");
            String[] rA = resLines.get(0).split("=");
            String[] rB = resLines.get(1).split("=");
            String a = rA[1];
            String b = rB[1];
            assertTrue(a.equals("123.456"),"Check extracted result A");
            assertTrue(b.equals("goodResult"),"Check extracted result B");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }
    }

//------------------------------------------------------------------------------

    //@Test
    public void testParallelizableSubJobs() throws Exception
    {
        Job job = JobFactory.createJob(Job.RunnableAppID.ACC);
        job.addStep(JobFactory.createJob(Job.RunnableAppID.ACC,true));
        job.addStep(JobFactory.createJob(Job.RunnableAppID.ACC,true));
        job.addStep(JobFactory.createJob(Job.RunnableAppID.ACC,true));
        assertTrue(job.parallelizableSubJobs());

        job.addStep(JobFactory.createJob(Job.RunnableAppID.ACC,false));
        assertFalse(job.parallelizableSubJobs());
    }

//------------------------------------------------------------------------------

}
