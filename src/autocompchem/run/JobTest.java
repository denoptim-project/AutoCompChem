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
 * Unit Test for the Job. 
 * 
 * @author Marco Foscato
 */

public class JobTest 
{

    private final String SEP = System.getProperty("file.separator");

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testSequentialShellJobs() throws Exception
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

            //FIXME: this makes unit testing platform dependent!!!
            
            // Choose shell flavour
            String shellFlvr = "/bin/sh";
/*
//TODO: maybe one day we'll check for available interpreters, and run the test only if we find a good one. 
// Now it doesnt work.
            ArrayList<String> shells = new ArrayList<String>();
            shells.add("bash");
            shells.add("csh");
            shells.add("sh");
            boolean notFound = true;
            for (String s : shells)
            {
                Process p = Runtime.getRuntime().exec(s);
                p.destroy();
                
                if (p.exitValue() == 0)
                {
                    shellFlvr = s;
                    notFound = false;
                    break;
                }

            }
            if (notFound)
            {
                assertFalse(true, "Could not find known shell flavour.");
            }
*/

            // Nest 4 shell jobs in an undefined job
            Job job = JobFactory.createJob(Job.RunnableAppID.ACC);
            job.setVerbosity(0);
            job.addStep(new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+1));
            job.addStep(new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+2));
            job.addStep(new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+3));
            job.addStep(new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+4));

            // Nest 2 shell jobs in the fifth shell job
            Job fifthJob = new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                     newFile+5);
            fifthJob.addStep(new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+6));
            fifthJob.addStep(new ShellJob(shellFlvr,script.getAbsolutePath(),
                                                                    newFile+7));
            job.addStep(fifthJob);

            //Run the job serially
            job.run();


            // Verify result
            for (int i=1; i<8; i++)
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

    @Test
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

    @Test
    public void testParallelizedJob() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define paths in tmp dir
        File script = new File(tempDir.getAbsolutePath() + SEP + "script.sh");
        String newFile = tempDir.getAbsolutePath() + SEP + "dataPal";

        try 
        {
            // Make a SHELL script that is only writing the date on a given file
            FileWriter writer = new FileWriter(script);
            writer.write("date > $1; sleep 1s; date >> $1");
            writer.close();

            //FIXME: this makes unit testing platform dependent!!!
            
            // Choose shell flavour
            String shellFlvr = "/bin/sh";
            
            // Nest 4 shell jobs in an undefined job
            int nThreads = 2; //NB: do no change
            Job job = JobFactory.createJob(Job.RunnableAppID.ACC,nThreads);
            job.setVerbosity(0);
            Job subJob1 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+1);
            subJob1.setParallelizable(true);
            job.addStep(subJob1);
            Job subJob2 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+2);
            subJob2.setParallelizable(true);
            job.addStep(subJob2);
            Job subJob3 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+3);
            subJob3.setParallelizable(true);
            job.addStep(subJob3);
            Job subJob4 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+4);
            subJob4.setParallelizable(true);
            job.addStep(subJob4);
            
            //Run the job with parallel sub-jobs
            job.run();
            
            //NB: this is only to allow looking in the tmp files to see that
            //    two threads are started simultaneously, while the next two
            //    are started again simultaneously, but only when the first
            //    two are finished.
            //System.out.println("TMP: "+tempDir.getAbsolutePath());
            //IOtools.pause();


            // Verify result
            for (int i=1; i<5; i++)
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
