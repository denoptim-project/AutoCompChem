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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

import autocompchem.datacollections.Parameter;
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
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;
import autocompchem.run.Job.RunnableAppID;


/**
 * Unit Test for the runner of embarrassingly parallel sets of jobs. 
 * 
 * @author Marco Foscato
 */

public class ParallelRunnerTest 
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
    
    private final boolean debug = false;
    

//-----------------------------------------------------------------------------

    /**
     * A dummy test job that is parallelizable and simply logs into a file
     * every ca. half second, by default, but timings can be set. 
     * The job lasts a time defined in the constructor.
     */
    
    private class TestJob extends Job
    {    	
    	protected int i = 0;
    	protected int wallTime = 0;
    	protected int delay = 500;
    	protected int period = 490;
    	protected String logPathName = "noLogName";
    	protected Date date = new Date();
    	protected SimpleDateFormat df = 
    			new SimpleDateFormat(" HH:mm:ss.SSS ");
    	
    	public TestJob(String logPathName, int wallTime)
    	{
    		super();
    		this.logPathName = logPathName;
    		this.wallTime = wallTime;
    		setParallelizable(true);
    		setNumberOfThreads(1);
    	}
    	
    	public TestJob(String logPathName, int wallTime, int delay, int period)
    	{
    		super();
    		this.logPathName = logPathName;
    		this.wallTime = wallTime;
    		this.delay = delay;
    		this.period = period;
    		setParallelizable(true);
    		setNumberOfThreads(1);
    	}
    	
    	@Override
    	public void runThisJobSubClassSpecific()
    	{	
    		if (debug)
    		{
	    		date = new Date();
	    		System.out.println("RUNNING TestJobLog: "+df.format(date));
	    		System.out.println("Pathname: "+logPathName);
    		}
    		
    		// The dummy command will just ping every half second
    		ScheduledThreadPoolExecutor stpe = 
    				new ScheduledThreadPoolExecutor(1);
    		stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    		stpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    		stpe.scheduleAtFixedRate( new Task(), delay, period,
    				TimeUnit.MILLISECONDS);
    		
    		CountDownLatch cdl = new CountDownLatch(1);
            try {
				while (!cdl.await(wallTime, TimeUnit.SECONDS)) 
				{
					stpe.shutdownNow();
					break;
				}
			} catch (InterruptedException e) {
				//Ignoring exception: interruption is signalled by Job
				//e.printStackTrace();
			} finally {
				stpe.shutdownNow();
			}
    	}
    	
    	private class Task implements Runnable
    	{
			@Override
			public void run() 
			{
				i++;
				IOtools.writeTXTAppend(logPathName, "Iteration "+i, true);
			}
    	}
    }
    
//-----------------------------------------------------------------------------

    /*
     * Only meant to test the private class TestJob
     */
    
    public void testPrivateClass() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";
    	System.out.println("STARTING TestJob");
    	Job job = new TestJob(roothName,6,3000,500);
    	job.run();
    	System.out.println(" END of TestJob");
    }

//-----------------------------------------------------------------------------

    /*
     * Case tested:
     * - all jobs fit into max number of threads 
     * - runtime of all jobs is < wall time of ParallelRunner
     */
    @Test
    public void testParallelJobsA() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";
        
        Job master = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        master.setParameter(new Parameter("WALLTIME", "10"));
        for (int i=0; i<3; i++)
        {
        	master.addStep(new TestJob(roothName+i,3));
        }
        
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	assertEquals(6,FileAnalyzer.count(roothName+i, "Iteration*"),
            		"Lines in log "+i);
        	assertFalse(master.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
    }
    
//-----------------------------------------------------------------------------

    /*
     * Case tested:
     * - all jobs fit into max number of threads 
     * - runtime of all jobs is > wall time of ParallelRunner
     */
    @Test
    public void testParallelJobsB() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";

        Job master = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        master.setParameter(new Parameter("WALLTIME", "3"));
        for (int i=0; i<3; i++)
        {
        	master.addStep(new TestJob(roothName+i,5));
        }
        
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	assertEquals(6,FileAnalyzer.count(roothName+i, "Iteration*"),
            		"Lines in log "+i);
        	assertTrue(master.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
    }
    
//-----------------------------------------------------------------------------

    /*
     * Case tested:
     * - more jobs that threads, jobs need to queue 
     * - runtime for running all jobs < wall time of ParallelRunner
     */
    @Test
    public void testParallelJobsC() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";

        //Job master = JobFactory.createJob(RunnableAppID.ACC);
        //master.addStep(new TestJob(roothName+"A"));
        
        
        Job master = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        master.setParameter(new Parameter("WALLTIME", "10"));
        for (int i=0; i<6; i++)
        {
        	master.addStep(new TestJob(roothName+i,3));
        }
        
        master.run();
        
        for (int i=0; i<6; i++)
        {
        	assertEquals(6,FileAnalyzer.count(roothName+i, "Iteration*"),
            		"Lines in log "+i);
        	assertFalse(master.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
    }
    
//-----------------------------------------------------------------------------

    /*
     * Case tested:
     * - more jobs that threads, jobs need to queue 
     * - runtime for running all jobs > wall time of ParallelRunner
     */
    @Test
    public void testParallelJobsD() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";

        //Job master = JobFactory.createJob(RunnableAppID.ACC);
        //master.addStep(new TestJob(roothName+"A"));
        
        
        Job master = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        master.setParameter(new Parameter("WALLTIME", "5"));
        for (int i=0; i<6; i++)
        {
        	master.addStep(new TestJob(roothName+i,3));
        }
        
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	assertEquals(6,FileAnalyzer.count(roothName+i, "Iteration*"),
            		"Lines in log "+i);
        	assertFalse(master.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
        for (int i=3; i<6; i++)
        {
        	assertTrue(6>FileAnalyzer.count(roothName+i, "Iteration*"),
            		"Lines in log "+i);
        	assertTrue(master.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
    }
    
//-----------------------------------------------------------------------------

    /*
     * Here we test the notification hook: if a job terminates with a request 
     * to kill itself and its siblings (i.e., the jobs running in parallel with
     * that job, and controlled by the same ParallelRunner), then the 
     * ParallelRunner wakes up and kills all the running/future tasks 
     * and shuts down the execution service.
     */
    @Test
    public void testParallelJobActionNotifications() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        Job master = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        master.setParameter(new Parameter("WALLTIME", "6"));
        
        // A "long-lasting" job that will be evaluated
        Job jobToEvaluate = new TestJob(roothName+0,3,0,490);
        master.addStep(jobToEvaluate);
        
        // Prepare ingredients to do perception
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(s);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(roothName+0,InfoChannelType.LOGFEED));
        
        // Make the job that will monitor the ongoing job and trigger an action
        Job monitoringJob = new MonitoringJob(jobToEvaluate, sitsDB, icDB, 
        		750, 500);
        master.addStep(monitoringJob);
        
        // Other 'whatever' jobs (i.e., the siblings) that will be killed too
        for (int i=1; i<6; i++) 
        {
        	master.addStep(new TestJob(roothName+i,3,0,200));
        }
        
        assertEquals(7,master.getNumberOfSteps(),"Number of parallel jobs");
        
        master.run();
        
        int iPingFiles = FileUtils.find(tempDir, baseName).size();
        assertEquals(2,iPingFiles,"Number of initiated TestJobs");
    }
    
//-----------------------------------------------------------------------------

    /*
     * Here we test the notification hook: if a job terminates with a request 
     * to kill itself and its siblings (i.e., the jobs running in parallel with
     * that job, and controlled by the same ParallelRunner), then the 
     * ParallelRunner wakes up and kills all the running/future tasks 
     * and shuts down the execution service.
     */
    @Test
    public void testParallelJobCompletionNotifications() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        Job master = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        master.setParameter(new Parameter("WALLTIME", "6"));
        master.setParameter(new Parameter("WAITSTEP", "7"));
        // Basically the waiting step is much longer than the time it 
        // will take to do the actual jobs, and also to the time given to the
        // ParallelRunner as a wall time. So, check for completion
        // should be triggered by the actual completion of any job.
       
        master.addStep(new TestJob(roothName+"_A",1,0,200));
        master.addStep(new TestJob(roothName+"_B",2,0,200));
        master.addStep(new TestJob(roothName+"_C",2,0,200));
        
        master.run();

        int iPingFiles = FileUtils.find(tempDir, baseName).size();
        assertEquals(3,iPingFiles,"Number of initiated TestJobs");
        
        //TODO add more checking. This will be made available once we'll
        // implement job specific logging
    }
    
    
//-----------------------------------------------------------------------------

    @Test
    public void testParallelShellJobs() throws Exception
    {
    	
    	//Check availability of shell on this OS, if not, then skip this test
    	if (!(new File("/bin/sh")).canExecute())
    	{
    		String NL = System.getProperty("line.separator");
    		System.out.println(NL + "WARNING: Skipping test that depends on "
    				+ "/bin/sh, which is not found." + NL);
    		
    		return;
    	}
    	
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

            // Choose shell flavour
            String shellFlvr = "/bin/sh";
            //FIXME: check for available interpreters.

            // Nest 4 shell jobs in an undefined job
            Job job = JobFactory.createJob(Job.RunnableAppID.ACC,4);
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
