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
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobEditTask.TargetType;


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
    
    /**
     * The name of the parameter determining the prefic in the test job logs.
     */
    private String PREFIX = "prefixForLogRecords";
    
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
    	protected Date date = new Date();
    	protected SimpleDateFormat df = 
    			new SimpleDateFormat(" HH:mm:ss.SSS ");
    	
    	/**
    	 * 
    	 * @param logPathName
    	 * @param wallTime
    	 */
    	public TestJob(String logPathName, int wallTime)
    	{
    		super();
    		stdout = new File(logPathName);
    		this.wallTime = wallTime;
    		setParallelizable(true);
    		setNumberOfThreads(1);
    		setParameter(PREFIX, "");
    	}
    	
    	/**
    	 * 
    	 * @param logPathName
    	 * @param wallTime
    	 * @param delay
    	 * @param period
    	 */
    	public TestJob(String logPathName, int wallTime, int delay, int period)
    	{
    		this(logPathName, wallTime);
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
	    		System.out.println("Pathname: "+stdout);
    		}
    		
    		// The dummy command will just ping every N-milliseconds
    		ScheduledThreadPoolExecutor stpe = 
    				new ScheduledThreadPoolExecutor(1);
    		stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    		stpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    		Task tsk = new Task();
    		tsk.prefix = getParameter(PREFIX).getValueAsString();
    		stpe.scheduleAtFixedRate(tsk, delay, period,
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
    		public String prefix = "";
    		
			@Override
			public void run() 
			{
				i++;
				IOtools.writeTXTAppend(stdout.getAbsolutePath(), 
						prefix + "Iteration " + i, 
						true);
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
        master.setParameter("WALLTIME", "10");
        for (int i=0; i<3; i++)
        {
        	master.addStep(new TestJob(roothName+i,3));
        }
        
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
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
        master.setParameter("WALLTIME", "3");
        for (int i=0; i<3; i++)
        {
        	master.addStep(new TestJob(roothName+i,5));
        }
        
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<10,"Lines in log "+i);
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
        master.setParameter("WALLTIME", "10");
        for (int i=0; i<6; i++)
        {
        	master.addStep(new TestJob(roothName+i,3));
        }
        
        master.run();
        
        for (int i=0; i<6; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
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
        master.setParameter("WALLTIME", "5");
        for (int i=0; i<6; i++)
        {
        	master.addStep(new TestJob(roothName+i,3));
        }
        
        master.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
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
        master.setParameter("WALLTIME", "6");
        
        // A "long-lasting" job that will be evaluated
        Job jobToEvaluate = new TestJob(roothName+0,3,0,490);
        master.addStep(jobToEvaluate);
        
        // Prepare ingredients to do perception
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(s);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(roothName+0, InfoChannelType.LOGFEED));
        
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
        
        int iPingFiles = FileUtils.find(tempDir, baseName, false).size();
        assertEquals(2, iPingFiles, "Number of initiated TestJobs");
    }
    
//-----------------------------------------------------------------------------

    /*
     * Here we test the request to redo (re-submit) the parallel batch upon
     * force-terminating all of it. This is the scenario where a monitoring
     * job detects a situation that an redo-type of action.
     */
    @Test
    public void testRedoUponNotification() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        //TODO-gg del
        System.out.println("WDIR: "+tempDir.getAbsolutePath());
        
        // A "long-lasting" job that will be evaluated
        String logOnProductionJob = roothName+"_production";
        TestJob productionJob = new TestJob(logOnProductionJob,5,0,240);
        productionJob.setUserDir(tempDir);
        
        // Conditional rerun 1
        ICircumstance c = new MatchText("Iteration 4", 
        		InfoChannelType.LOGFEED);
        Action act = new Action(ActionType.REDO, ActionObject.PARALLELJOB);
        String newPrefix = "RESTART-";
        act.addJobEditingTask(PREFIX, TargetType.PARAMETER, newPrefix);
        Situation sit1 = new Situation("SitTyp", "Sit-ONE", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)),act);
        
        // Conditional rerun 2
        ICircumstance c2 = new MatchText("RESTART-Iteration 10", 
        		InfoChannelType.LOGFEED);
        Action act2 = new Action(ActionType.REDO, ActionObject.PARALLELJOB);
        newPrefix = "LAST-";
        act2.addJobEditingTask(PREFIX, TargetType.PARAMETER, newPrefix);
        Situation sit2 = new Situation("SitTyp", "Sit-TWO", 
        		new ArrayList<ICircumstance>(Arrays.asList(c2)),act2);
        
        // Conditional stop all, i.e., end before reaching walltime.
        ICircumstance c3 = new MatchText("LAST-Iteration 15", 
        		InfoChannelType.LOGFEED);
        Action act3 = new Action(ActionType.STOP, ActionObject.PARALLELJOB);
        Situation sit3 = new Situation("SitTyp", "Sit-THREE",
        		new ArrayList<ICircumstance>(Arrays.asList(c3)),act3);
        
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(sit3);
        sitsDB.addSituation(sit2);
        sitsDB.addSituation(sit1);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(logOnProductionJob, 
        		InfoChannelType.LOGFEED));
        
        // Make the job that will monitor the ongoing job and trigger an action
        Job monitoringJob = new MonitoringJob(productionJob, sitsDB, icDB, 
        		750, 500);
        //monitoringJob.setParameter(ParameterConstants.VERBOSITY, "1");
        
        // The main job
        Job main = JobFactory.createJob(RunnableAppID.ACC, 3, true);
        main.setParameter("WALLTIME", "10");
        //main.setParameter(ParameterConstants.VERBOSITY, "3");
        main.addStep(productionJob);
        main.addStep(monitoringJob);
        
        // Other 'whatever' jobs (i.e., the siblings) that will be killed too
        // and restarted too
        for (int i=1; i<5; i++) 
        {
        	TestJob j = new TestJob(roothName+i,3,0,200);
        	j.setUserDir(tempDir);
        	main.addStep(j);
        }
        
        assertEquals(6,main.getNumberOfSteps(), "Number of parallel jobs");
        
        main.run();
        
        assertEquals(2, FileUtils.find(tempDir, "Job_#0.1*", true).size(), 
        		"Number of folders for production job");
        assertEquals(3, FileUtils.find(tempDir, "testjob.log_production").size(), 
        		"Number of files for production job");
        assertEquals(2, FileUtils.find(tempDir, "*_1", true).size(), 
        		"Number of folders from first run");
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
        master.setParameter("WALLTIME", "6");
        master.setParameter("WAITSTEP", "7");
        // Basically the waiting step is much longer than the time it 
        // will take to do the actual jobs, and also to the time given to the
        // ParallelRunner as a wall time. So, check for completion
        // should be triggered by the actual completion of any job.
       
        master.addStep(new TestJob(roothName+"_A",1,0,200));
        master.addStep(new TestJob(roothName+"_B",2,0,200));
        master.addStep(new TestJob(roothName+"_C",2,0,200));
        
        master.run();

        int iPingFiles = FileUtils.find(tempDir, baseName, false).size();
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
