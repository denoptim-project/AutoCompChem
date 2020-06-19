package autocompchem.run;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit Test for the runner of embarassingly parallel sets of jobs. 
 * 
 * @author Marco Foscato
 */

public class ParallelRunnerTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;

    @Test
    public void testParallelShellJobs() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

	//Define paths in tmp dir
	File script = new File(tempDir.getAbsolutePath() + SEP + "script.sh");
	String newFile = tempDir.getAbsolutePath() + SEP + "dateFile";

	try 
	{
            // Make a SHELL script that is only writing the date on a given file
            FileWriter writer = new FileWriter(script);
            writer.write("date > $1");
            writer.close();

	    // Choose shell flavor
	    String shellFlvr = "/bin/sh";
	    //TODO: check for available interpreters.

	    // Nest 4 shell jobs in an undefined job
            Job job = new Job(Job.RunnableAppID.ACC,4);
	    for (int i=0; i<10; i++)
	    {
	        ShellJob sj = new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+i);
		sj.setParallelizable(true);
                job.addStep(sj);
	    }

	    // Submit all via the Job
	    job.run();

	    // Verify result
	    for (int i=0; i<10; i++)
	    {
		File f = new File(newFile+i);
                assertTrue(f.exists(),"ShellJob output file exists ("+i+") in "
						   + tempDir.getAbsolutePath());
	    }
        }
        catch (Throwable t)
        {
	    t.printStackTrace();
	    assertFalse(true, "Unable to work with tmp files.");
        }
    }

//------------------------------------------------------------------------------

}
