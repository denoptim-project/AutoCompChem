package autocompchem.run.jobediting;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*   
 *   Copyright (C) 2023  Marco Foscato 
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.run.AppID;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;


/**
 * Unit Test for ActionApplier
 * 
 * @author Marco Foscato
 */

public class ActionApplierTest 
{
 
    private final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    File tempDir;

//------------------------------------------------------------------------------

    /**
     * Writes a dummy file in the temp file system.
     * @param filename name (not path!)
     * @param content to write in the file.
     * @throws IOException 
     */
    private void writeDummyFile(String filename, String content) throws IOException
    {
    	File file = new File(tempDir.getAbsolutePath() + SEP + filename);
    	writeDummyFile(file,content);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes a dummy file in the temp file system.
     * @param file the target file.
     * @param content to write in the file.
     * @throws IOException 
     */
    private void writeDummyFile(File file, String content) throws IOException
    {
    	FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testArchivePreviousResults() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");
        
    	Job job = JobFactory.createJob(AppID.ACC);
    	job.setUserDirAndStdFiles(tempDir);
    	
    	String labM = "toMv";
    	String labC = "toCp";
    	String labD = "toDel";
    	List<String> labels = new ArrayList<>(Arrays.asList(labM,labC,labD));
    	
    	// Create some dummy files as if the had been created by the job
    	for (int i=0; i<3; i++)
    	{
    		for (String label : labels)
    		{
    			writeDummyFile("file"+i+label+"M.dat", "i:"+i+" Label:"+label);
    			writeDummyFile("file"+i+label+"E", "i:"+i+" Label:"+label);
    			writeDummyFile(label+"Sfile"+i, "i:"+i+" Label:"+label);
	    	}
    	}
    	writeDummyFile(job.getStdErr(),"There is no ERROR");
    	writeDummyFile(job.getStdOut(),"This is the log from a dummy job");
    	
    	// Define the rules for choosing what to do with the files
    	Set<String> ratternaToArchive = new HashSet<>(Arrays.asList(
    			"*"+labM+"E", "*"+labM+"M*", labM+"S*"));
    	Set<String> ratternaToCopy = new HashSet<>(Arrays.asList(
    			"*"+labC+"E", "*"+labC+"M*", labC+"S*"));
    	Set<String> ratternaToTrash = new HashSet<>(Arrays.asList(
    			"*"+labD+"E", "*"+labD+"M*", labD+"S*"));
    	
    	ActionApplier.archivePreviousResults(job, 6, ratternaToCopy, 
    			ratternaToArchive, ratternaToTrash);
    	
    	File archiveDir = new File(tempDir+SEP+"Job_#0_6");
    	assertTrue(archiveDir.exists());
        assertEquals(1, FileUtils.findByGlob(tempDir, "Job_#*", true).size());
        assertEquals(0, FileUtils.findByGlob(tempDir, "*toMv*", true).size());
        assertEquals(9, FileUtils.findByGlob(archiveDir, "*toMv*", true).size());
        assertEquals(9, FileUtils.findByGlob(tempDir, "*toCp*", true).size());
        assertEquals(9, FileUtils.findByGlob(archiveDir, "*toCp*", true).size());
        assertEquals(9, FileUtils.findByGlob(tempDir, "Job_*/*toCp*", true).size());
        assertEquals(0, FileUtils.findByGlob(tempDir, "*toDel*", true).size());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testPerformAction_ArchivingTasks()  throws Exception
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");
        
        // Make a dummy collection of jobs
    	Job siblingJobA = JobFactory.createJob(AppID.ACC);
    	siblingJobA.setUserDirAndStdFiles(tempDir);
    	Job siblingJobB = JobFactory.createJob(AppID.ACC);
    	siblingJobB.setUserDirAndStdFiles(tempDir);
    	Job focusJob = JobFactory.createJob(AppID.ACC);
    	focusJob.setUserDirAndStdFiles(tempDir);
    	Job parentJob = JobFactory.createJob(AppID.ACC);
    	parentJob.addStep(siblingJobA);
    	parentJob.addStep(siblingJobB);
    	parentJob.addStep(focusJob);
    	
    	// Create some dummy files as if they had been created by the jobs
    	String labM = "toMv";
    	String labC = "toCp";
    	String labD = "toDel";
    	List<String> labels = new ArrayList<>(Arrays.asList(labM,labC,labD));
    	for (int i=0; i<2; i++)
    	{
    		for (String label : labels)
    		{
    			writeDummyFile("file"+i+label+"M.dat", "i:"+i+" Label:"+label);
    			writeDummyFile("file"+i+label+"E", "i:"+i+" Label:"+label);
    			writeDummyFile(label+"Sfile"+i, "i:"+i+" Label:"+label);
	    	}
    	}
    	writeDummyFile(focusJob.getStdErr(),"Content of focusJob STDERR");
    	writeDummyFile(focusJob.getStdOut(),"Content of focusJob STDOUT");
    	
    	writeDummyFile(siblingJobA.getStdErr(),"Content of siblingJobA STDERR");
    	writeDummyFile(siblingJobA.getStdOut(),"Content of siblingJobA STDOUT");
    	
    	writeDummyFile(siblingJobB.getStdErr(),"Content of siblingJobB STDERR");
    	writeDummyFile(siblingJobB.getStdOut(),"Content of siblingJobB STDOUT");
    	
    	// Define the action
    	Action action = new Action();
    	action.setType(ActionType.REDO);
    	action.setObject(ActionObject.PARALLELJOB);
    	action.addJobArchivingDetails(
    			new DataArchivingRule(ArchivingTaskType.DELETE, "*"+labD+"*"));
    	action.addJobArchivingDetails(
    			new DataArchivingRule(ArchivingTaskType.MOVE, "*"+labM+"*"));
    	action.addJobArchivingDetails(
    			new DataArchivingRule(ArchivingTaskType.COPY, "*"+labC+"*"));
    	
    	// Dummy job only to satisfy the fingerprint of the method
    	EvaluationJob dummy = new EvaluationJob();
    	
    	
    	// Do the magic
    	ActionApplier.performActionOnParallelBatch(action, parentJob, focusJob, 
    			dummy, 0);
    	
        assertEquals(3, FileUtils.findByGlob(tempDir, "Job_*", true).size());
        assertEquals(0, FileUtils.findByGlob(tempDir, "*toMv*", true).size());
        assertEquals(6, FileUtils.findByGlob(tempDir, "*/*toMv*", true).size());
        assertEquals(6, FileUtils.findByGlob(tempDir, "*toCp*", true).size());
        assertEquals(18, FileUtils.findByGlob(tempDir, "*/*toCp*", true).size());
        assertEquals(0, FileUtils.findByGlob(tempDir, "*toDel*", true).size());
        assertEquals(0, FileUtils.findByGlob(tempDir, "*.err", true).size());
        assertEquals(0, FileUtils.findByGlob(tempDir, "*.log", true).size());
        assertEquals(3, FileUtils.findByGlob(tempDir, "*/*.err", true).size());
        assertEquals(3, FileUtils.findByGlob(tempDir, "*/*.log", true).size());
    }
    
//------------------------------------------------------------------------------
      
    @Test
    public void testPerformAction_InheritJobParameters()  throws Exception
    {
    	// Define the dummy job workflow we'll be editing
      	Job step1 = JobFactory.createJob(AppID.ACC);
      	step1.setParameter("Provenance", "OriginalWorkFlow_1");
      	step1.setParameter(new NamedData("ParamA", 1.2));
      	step1.setParameter(new NamedData("ParamB", "AB"));
      	Job step2 = JobFactory.createJob(AppID.ACC);
      	step2.setParameter("Provenance", "OriginalWorkFlow_2");
      	step2.setParameter(new NamedData("ParamA", 3.4));
      	step2.setParameter(new NamedData("ParamB", "CORRECT"));
      	Job step3 = JobFactory.createJob(AppID.ACC);
      	step3.setParameter("Provenance", "OriginalWorkFlow_3");
      	step3.setParameter(new NamedData("ParamA", 5.6));
      	step3.setParameter(new NamedData("ParamC", "WRONG"));
      	Job parentJob = JobFactory.createJob(AppID.ACC);
      	parentJob.addStep(step1);
      	parentJob.addStep(step2);
      	parentJob.addStep(step3);
      	List<Job> jobs = new ArrayList<>(Arrays.asList(parentJob));
      	
      	// Define the action that adds a pre-refinement workflow
      	Job pre1 = JobFactory.createJob(AppID.ACC);
      	pre1.setParameter("Provenance", "Healer_1");
      	pre1.setParameter(new NamedData("ParamC", 0.12));
      	Job pre2 = JobFactory.createJob(AppID.ACC);
      	pre2.setParameter("Provenance", "Healer_2");
      	pre1.setParameter("ParamB", "YY");
    	Action action = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
    	action.addPrerefinementStep(pre1);
    	action.addPrerefinementStep(pre2);
    	action.addSettingsInheritedTask(new InheritJobParameter("ParamB"));
    	action.addSettingsInheritedTask(new InheritJobParameter("ParamC"));
    	
    	// Do the magic
    	ActionApplier.performActionOnSerialWorkflow(action, parentJob, 1, 1);
    
    	assertEquals(4,parentJob.getNumberOfSteps());
    	
    	assertTrue(parentJob.getStep(0).hasParameter("ParamB"));
    	assertEquals("CORRECT",
    			parentJob.getStep(0).getParameter("ParamB").getValueAsString());
    	assertTrue(parentJob.getStep(1).hasParameter("ParamB"));
    	assertEquals("CORRECT",
    			parentJob.getStep(1).getParameter("ParamB").getValueAsString());
    	assertTrue(parentJob.getStep(2).hasParameter("ParamB"));
    	assertEquals("CORRECT",
    			parentJob.getStep(2).getParameter("ParamB").getValueAsString());
    	assertFalse(parentJob.getStep(3).hasParameter("ParamB"));
    	
    	assertTrue(parentJob.getStep(0).hasParameter("ParamC"));
    	assertFalse(parentJob.getStep(1).hasParameter("ParamC"));
    	assertFalse(parentJob.getStep(2).hasParameter("ParamC"));
    	assertTrue(parentJob.getStep(3).hasParameter("ParamC"));
    }
    
//------------------------------------------------------------------------------

}
