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

import autocompchem.files.FileUtils;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;
import autocompchem.run.jobediting.DataArchivingRule.Type;


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
        
    	Job job = new Job();
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
        assertEquals(1, FileUtils.find(tempDir, "Job_#*", true).size());
        assertEquals(0, FileUtils.find(tempDir, "*toMv*", 1, true).size());
        assertEquals(9, FileUtils.find(archiveDir, "*toMv*", true).size());
        assertEquals(9, FileUtils.find(tempDir, "*toCp*", 1, true).size());
        assertEquals(9, FileUtils.find(archiveDir, "*toMv*", true).size());
        assertEquals(18, FileUtils.find(tempDir, "*toCp*", 2, true).size());
        assertEquals(0, FileUtils.find(tempDir, "*toDel*", true).size());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testPerformAction_ArchivingTasks()  throws Exception
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");
        
        // Make a dummy collection of jobs
    	Job siblingJobA = JobFactory.createJob(RunnableAppID.ACC);
    	siblingJobA.setUserDirAndStdFiles(tempDir);
    	Job siblingJobB = JobFactory.createJob(RunnableAppID.ACC);
    	siblingJobB.setUserDirAndStdFiles(tempDir);
    	Job focusJob = JobFactory.createJob(RunnableAppID.ACC);
    	focusJob.setUserDirAndStdFiles(tempDir);
    	//NB: making the jobs be the steps of a parent job would make their IDs
    	// be unique. Here, we want to test also the capability do deal with 
    	// a list of jobs that are not related by a common parent jobs.
    	//Job parentJob = JobFactory.createJob(RunnableAppID.ACC);
    	List<Job> jobs = new ArrayList<>(Arrays.asList(siblingJobA, siblingJobB,
    			focusJob));
    	
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
    	action.addJobArchivingDetails(
    			new DataArchivingRule(Type.DELETE, "*"+labD+"*"));
    	action.addJobArchivingDetails(
    			new DataArchivingRule(Type.MOVE, "*"+labM+"*"));
    	action.addJobArchivingDetails(
    			new DataArchivingRule(Type.COPY, "*"+labC+"*"));
    	
    	// Do the magic
    	ActionApplier.performAction(action, focusJob, 0, jobs, 1);
    	
        assertEquals(3, FileUtils.find(tempDir, "Job_*", true).size());
        assertEquals(0, FileUtils.find(tempDir, "*toMv*", 1, true).size());
        assertEquals(6, FileUtils.find(tempDir, "*toMv*", 2, true).size());
        assertEquals(6, FileUtils.find(tempDir, "*toCp*", 1, true).size());
        assertEquals(24, FileUtils.find(tempDir, "*toCp*", 2, true).size());
        assertEquals(0, FileUtils.find(tempDir, "*toDel*", true).size());
        assertEquals(0, FileUtils.find(tempDir, "*.err", 1, true).size());
        assertEquals(0, FileUtils.find(tempDir, "*.log", 1, true).size());
        assertEquals(3, FileUtils.find(tempDir, "*.err", 2, true).size());
        assertEquals(3, FileUtils.find(tempDir, "*.log", 2, true).size());
    }
    
//------------------------------------------------------------------------------

}
