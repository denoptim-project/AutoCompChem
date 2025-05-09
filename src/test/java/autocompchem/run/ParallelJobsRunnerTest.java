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
import java.io.FileWriter;
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
 * Unit Test for the runner of embarrassingly parallel sets of jobs. 
 * 
 * @author Marco Foscato
 */

public class ParallelJobsRunnerTest 
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
     * Only meant to test the private class TestJob
     */

    public void testPrivateClass() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";
    	Job job = new TestJob(roothName,6,3000,500);
    	job.run();
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
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "10");
        for (int i=0; i<3; i++)
        {
        	main.addStep(new TestJob(roothName+i,3));
        }
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
        	assertFalse(main.getStep(i).isInterrupted,
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

        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "3");
        for (int i=0; i<3; i++)
        {
        	main.addStep(new TestJob(roothName+i,5));
        }
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<10,"Lines in log "+i);
        	assertTrue(main.getStep(i).isInterrupted,
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

        //Job main = JobFactory.createJob(RunnableSoftwareId.ACC);
        //main.addStep(new TestJob(roothName+"A"));
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "10");
        for (int i=0; i<6; i++)
        {
        	main.addStep(new TestJob(roothName+i,3));
        }

        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        for (int i=0; i<6; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
        	assertFalse(main.getStep(i).isInterrupted,
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

        //Job main = JobFactory.createJob(RunnableSoftwareId.ACC);
        //main.addStep(new TestJob(roothName+"A"));
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "5");
        for (int i=0; i<6; i++)
        {
        	main.addStep(new TestJob(roothName+i,3));
        }
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        for (int i=0; i<3; i++)
        {
        	int n = FileAnalyzer.count(roothName+i, "Iteration*");
        	assertTrue(n>4,"Lines in log "+i);
        	assertTrue(n<8,"Lines in log "+i);
        	assertFalse(main.getStep(i).isInterrupted,
        			"Interruption flag on job-"+i);
        }
        for (int i=3; i<6; i++)
        {
        	assertTrue(6>FileAnalyzer.count(roothName+i, "Iteration*"),
            		"Lines in log "+i);
        	assertTrue(main.getStep(i).isInterrupted,
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
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "6");
        
        // A "long-lasting" job that will be evaluated
        Job jobToEvaluate = new TestJob(roothName+0,3,0,490);
        main.addStep(jobToEvaluate);
        
        // Prepare ingredients to do perception
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(s);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(roothName+0, InfoChannelType.LOGFEED));
        
        // Make the job that will monitor the ongoing job and trigger an action
        Job monitoringJob = new MonitoringJob(jobToEvaluate, main, sitsDB, icDB, 
        		750, 500);
        main.addStep(monitoringJob);
        
        // Other 'whatever' jobs (i.e., the siblings) that will be killed too
        for (int i=1; i<6; i++) 
        {
        	main.addStep(new TestJob(roothName+i,3,0,200));
        }
        
        assertEquals(7,main.getNumberOfSteps(),"Number of parallel jobs");
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        int iPingFiles = FileUtils.findByGlob(tempDir, baseName+"*", false)
        		.size();
        assertEquals(2, iPingFiles, "Number of initiated TestJobs");
    }
    
//-----------------------------------------------------------------------------

    /*
     * Here we test the request to redo (re-submit) the parallel batch upon
     * force-terminating all of it. This is the scenario where a monitoring
     * job detects a situation that triggers a redo-type of action.
     */
    @Test
    public void testRedoUponNotification() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        // A "long-lasting" job that will be evaluated
        String logOnProductionJob = roothName+"_production";
        TestJob productionJob = new TestJob(logOnProductionJob,5,0,100);
        productionJob.setUserDir(tempDir);
        
        // Conditional rerun 1
        ICircumstance c = new MatchText("Iteration 4", 
        		InfoChannelType.LOGFEED);
        Action act = new Action(ActionType.REDO, ActionObject.PARALLELJOB);
        String newPrefix = "RESTART-";
        act.addJobEditingTask(new SetJobParameter(
        		new NamedData(TestJob.PREFIX, newPrefix)));
        Situation sit1 = new Situation("SitTyp", "Sit-ONE", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)),act);
        
        // Conditional rerun 2
        ICircumstance c2 = new MatchText("RESTART-Iteration 20", 
        		InfoChannelType.LOGFEED);
        Action act2 = new Action(ActionType.REDO, ActionObject.PARALLELJOB);
        newPrefix = "LAST-";
        act2.addJobEditingTask(new SetJobParameter(
        		new NamedData(TestJob.PREFIX, newPrefix)));
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
        
        // The main job
        Job main = JobFactory.createJob(SoftwareId.ACC, 5);
        main.setParameter("WALLTIME", "10");
        
        // Make the job that will monitor the ongoing job and trigger an action
        Job monitoringJob = new MonitoringJob(productionJob, main, sitsDB, icDB, 
        		750, 500);
        
        main.addStep(productionJob);
        main.addStep(monitoringJob);
        
        // Other 'whatever' jobs (i.e., the siblings) that will be killed too
        // and restarted too
        for (int i=1; i<5; i++) 
        {
        	TestJob j = new TestJob(roothName+i,3,0,100);
        	j.setUserDir(tempDir);
        	main.addStep(j);
        }
        
        assertEquals(6, main.getNumberOfSteps(), "Number of parallel jobs");
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        /* Job_#0.1 is the production job (the one monitored)
         * Job_#0.2 is the monitoring job
         * Job_#0.3 is a sibling writing on testjob.log1
         * Job_#0.4 is a sibling writing on testjob.log2
         * Job_#0.5 is a sibling writing on testjob.log3
         * Job_#0.6 is a sibling but does not run (not enough threads to start it)
         */
        
        assertEquals(2, FileUtils.findByGlob(tempDir, 
        		"Job_#0.1*", true).size());
        assertEquals(2, FileUtils.findByGlob(tempDir, 
        		"Job_#0.3*", true).size());
        assertEquals(2, FileUtils.findByGlob(tempDir, 
        		"Job_#0.4*", true).size());
        assertEquals(2, FileUtils.findByGlob(tempDir, 
        		"Job_#0.5*", true).size());
        assertEquals(4, FileUtils.findByGlob(tempDir, "*_1", true).size());
        assertEquals(4, FileUtils.findByGlob(tempDir, "*_2", true).size());
        assertEquals(1, FileUtils.findByGlob(tempDir, 
        		"testjob.log_production").size());
        assertEquals(2, FileUtils.findByGlob(tempDir, 
        		"*/testjob.log_production").size());
    }
    
//-----------------------------------------------------------------------------

    /*
     * Here we test the request to redo (re-submit) only the focus job while the
     * other jobs in the batch go on. This is the scenario where a monitoring
     * job detects a situation that triggers a redo-type of action acting only
     * on the focus job.
     */
    @Test
    public void testRedoFocusJobUponNotification() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        // A "long-lasting" job that will be evaluated
        String logOnProductionJob = roothName + "_production";
        TestJob productionJob = new TestJob(logOnProductionJob,2,0,100);
        productionJob.setUserDir(tempDir);
        
        // Situation to perceive and reaction to it.
        ICircumstance c = new MatchText("Iteration 4", 
        		InfoChannelType.LOGFEED);
        Action act = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
        String newPrefix = "RESTART-";
        act.addJobEditingTask(new SetJobParameter(
        		new NamedData(TestJob.PREFIX, newPrefix)));
        Situation sit1 = new Situation("SitTyp", "Sit-ONE", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)),act);
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(sit1);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(logOnProductionJob, 
        		InfoChannelType.LOGFEED));
        
        // The main job that contains the batch of parallel jobs
        Job main = JobFactory.createJob(SoftwareId.ACC, 4);
        main.setParameter("WALLTIME", "10");
        
        // Make the job that will monitor the ongoing job and trigger an action
        Job monitoringJob = new MonitoringJob(productionJob, main, sitsDB, icDB, 
        		750, 500);
        
        main.addStep(productionJob);
        main.addStep(monitoringJob);
        
        // Other 'whatever' jobs (i.e., the siblings) that go on carelessly
        for (int i=1; i<3; i++) 
        {
        	TestJob j = new TestJob(roothName+i, 2, 0, 100);
        	j.setUserDir(tempDir);
        	main.addStep(j);
        }
        
        assertEquals(4, main.getNumberOfSteps(), "Number of parallel jobs");
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        main.run();
        
        /* 
         * Job_#0.1 is the production job (the one monitored)
         * Job_#0.2 is the monitoring job
         * Job_#0.3 is a sibling writing on testjob.log1
         * Job_#0.4 is a sibling writing on testjob.log2
         */
        
        assertEquals(1, FileUtils.findByGlob(tempDir, "Job_#0.*", true).size());
        assertEquals(1, FileUtils.findByGlob(tempDir, "*_1", true).size());
        assertEquals(1, FileUtils.findByGlob(tempDir, 
        		"testjob.log_production").size());
        assertEquals(1, FileUtils.findByGlob(tempDir, 
        		"*/testjob.log_production").size());
        assertEquals(0, FileAnalyzer.count(tempDir.getAbsolutePath() + SEP 
        		+ "Job_#0.1_1" + SEP + baseName + "_production", 
        		newPrefix+"Iteration 18"));
        assertEquals(1, FileAnalyzer.count(logOnProductionJob, 
        		newPrefix+"Iteration 18"));
        assertEquals(1, FileAnalyzer.count(roothName+1, "Iteration 18"));
        assertEquals(1, FileAnalyzer.count(roothName+2, "Iteration 18"));
    }
    
  //-----------------------------------------------------------------------------

    /*
     * Here we test the request to stop and skip a job belonging to a batch of 
     * parallel siblings.
     */
    @Test
    public void testStopAndSkipUponNotification() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        int walltimeChildJobs = 2;
        int period = 25;
        
        // A job that will be evaluated
        String logOnProductionJob = roothName+"_production";
        TestJob productionJob = new TestJob(logOnProductionJob,
        		walltimeChildJobs, 0, period); 
        productionJob.setUserDir(tempDir);
        
        ICircumstance c = new MatchText("Iteration 10", 
        		InfoChannelType.LOGFEED);
        Action act = new Action(ActionType.SKIP, ActionObject.FOCUSJOB);
        Situation sit1 = new Situation("SitTyp", "Sit-ONE", 
        		new ArrayList<ICircumstance>(Arrays.asList(c)),act);
        
        SituationBase sitsDB = new SituationBase();
        sitsDB.addSituation(sit1);
        InfoChannelBase icDB = new InfoChannelBase();
        icDB.addChannel(new FileAsSource(logOnProductionJob, 
        		InfoChannelType.LOGFEED));        

        // The main job
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "10");
        
        // Make the job that will monitor the ongoing job and trigger an action
        Job monitoringJob = new MonitoringJob(productionJob, main, sitsDB, icDB, 
        		0, period);
        monitoringJob.setParameter(ParameterConstants.TOLERATEMISSINGIC, "true");
        
        main.addStep(monitoringJob);
        main.addStep(productionJob);
        
        // Sibling jobs that will NOT be stopped
        for (int i=1; i<4; i++) 
        {
        	TestJob j = new TestJob(roothName+i, walltimeChildJobs, 0, period);
        	j.setUserDir(tempDir);
        	main.addStep(j);
        }
        
        assertEquals(5, main.getNumberOfSteps(), "Number of parallel jobs");

        // Comment out these to get some log, in case of debugging
        // monitoringJob.setParameter(ParameterConstants.VERBOSITY, "3", true);
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
       
        main.run();
        
        /* Job_#0.1 is the monitoring job
         * Job_#0.2 is the production job (the one monitored)
         * Job_#0.3 is a sibling writing on testjob.log1
         * Job_#0.4 is a sibling writing on testjob.log2
         * Job_#0.5 is a sibling writing on testjob.log3
         * 
         * Job_#0.2 writes the log that is monitored by Job_#0.1. Upon reaching
         * a given number of iterations, the monitoring job triggers a reaction
         * that stops Job_#0.2, but does not do more than that.
         * Therefore, we expect to find no archive folder, but we expect that 
         * the log of Job_#0.2 is substantially shorted than the log of its 
         * sibling Job_#0.3, which must not be stopped by the reaction.
         * 
         * Jobs Job_#0.4, and Job_#0.5 ran do not fill in the number of parallel
         * batches so they run afterwards and should not be stopped by the 
         * reaction to Job_#0.2.
         * 
         * Note that the monitoring job starts before the monitored job. Thus,
         * in the first monitoring iteration, the info channel is not readable,
         * hence the monitoring job has to be tolerant.
         */
        
        assertEquals(0, FileUtils.findByGlob(tempDir, 
        		"Job_#0.2_0", true).size());
        assertEquals(4, FileUtils.findByGlob(tempDir, 
        		"testjob.log*", true).size());
        assertTrue(16>FileAnalyzer.count(tempDir.getAbsolutePath() + SEP 
        		+ "testjob.log_production", "Iteration"));
        assertTrue(30<FileAnalyzer.count(roothName+"1", "Iteration"));
        assertTrue(30<FileAnalyzer.count(roothName+"2", "Iteration"));
        assertTrue(30<FileAnalyzer.count(roothName+"3", "Iteration"));
    }
    
  //-----------------------------------------------------------------------------

    /*
     * Testing reaction to exception in parallel subjob
     */
    @Test
    public void testParallelJobsWithExceptions() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String roothName = tempDir.getAbsolutePath() + SEP + "testjob.log";

        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter(JobsRunner.WALLTIMEPARAM, "5");
        for (int i=0; i<2; i++)
        {
        	main.addStep(new TestJob(roothName+i, 3));
        }
        main.addStep(new TestJobTriggeringException());
        

        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        boolean triggered = false;
        try {
        	main.run();
        } catch (Throwable t) {
        	triggered = true;
			assertTrue(t.getMessage().contains(TestJobTriggeringException.MSG));
        }
        assertTrue(triggered);
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testParallelJobCompletionNotifications() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String baseName ="testjob.log";
        String roothName = tempDir.getAbsolutePath() + SEP + baseName;
        
        Job main = JobFactory.createJob(SoftwareId.ACC, 3);
        main.setParameter("WALLTIME", "6");
        main.setParameter("WAITSTEP", "7");
        // Basically the waiting step is much longer than the time it 
        // will take to do the actual jobs, and also to the time given to the
        // ParallelRunner as a wall time. So, check for completion
        // should be triggered by the actual completion of any job.
       
        main.addStep(new TestJob(roothName+"_A",1,0,200));
        main.addStep(new TestJob(roothName+"_B",2,0,200));
        main.addStep(new TestJob(roothName+"_C",2,0,200));
        
        // Comment out these to get some log, in case of debugging
        main.setParameter(ParameterConstants.VERBOSITY, "0", true);
        
        long startTime = System.nanoTime();
        main.run();
        long runTimeSec = (System.nanoTime() - startTime) / 1000000000;
        
        // According to the previous comment, the main job should be stopped by 
        // the sub jobs notifying completion and, thus, triggering completion of
        // the main job, which should, therefore, be elapse much less then the  
        // waiting step (7 seconds, but to allow for ample fluctuation abode the
        // more-than-2 sec that main should take, we set 4 sec as the threshold).
        assertTrue(runTimeSec < 4);

        // check that every job was actually run
        int iPingFiles = FileUtils.findByGlob(tempDir, baseName+"*", false)
        		.size();
        assertEquals(3, iPingFiles, "Number of initiated TestJobs");
        
        assertTrue(8 > FileAnalyzer.count(roothName+"_A", "Iteration *"));
        assertTrue(8 < FileAnalyzer.count(roothName+"_B", "Iteration *"));
        assertTrue(8 < FileAnalyzer.count(roothName+"_C", "Iteration *"));
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testParallelShellJobs() throws Exception
    {
    	//Check availability of shell on this OS, if not, then skip this test
    	if (!(new File("/bin/sh")).canExecute())
    	{
    		String NL = System.getProperty("line.separator");
    		System.err.println(NL + "WARNING: Skipping test that depends on "
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
            Job job = JobFactory.createJob(SoftwareId.ACC,4);
            for (int i=0; i<10; i++)
            {
                ShellJob sj = new ShellJob(shellFlvr, script.getAbsolutePath(),
                		newFile+i);
                job.addStep(sj);
            }

            // Comment out these to get some log, in case of debugging
            job.setParameter(ParameterConstants.VERBOSITY, "0", true);
            
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
