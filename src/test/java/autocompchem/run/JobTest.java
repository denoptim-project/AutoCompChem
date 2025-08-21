package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.utils.NumberUtils;


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
    @DisabledOnOs(WINDOWS)
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
            Job job = JobFactory.createJob(SoftwareId.ACC);
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
    @DisabledOnOs(WINDOWS)
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
            Job job = JobFactory.createJob(SoftwareId.ACC,nThreads);
            Job subJob1 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+1);
            job.addStep(subJob1);
            Job subJob2 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+2);
            job.addStep(subJob2);
            Job subJob3 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+3);
            job.addStep(subJob3);
            Job subJob4 = new ShellJob(shellFlvr,script.getAbsolutePath(),
                    newFile+4);
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
    
    @Test
    public void testSetParameter() 
    {
    	Job j1 = new Job();
    	Job j1_1 = new Job();
    	Job j1_2 = new Job();
    	Job j1_3 = new Job();
    	
    	j1.addStep(j1_1);
    	j1.addStep(j1_2);
    	j1.addStep(j1_3);
    	
    	String parName1 = "PARNAME1";
    	j1.setParameter(parName1, 123, false);
    	assertTrue(j1.hasParameter(parName1));
    	assertFalse(j1_1.hasParameter(parName1));
    	assertFalse(j1_2.hasParameter(parName1));
    	assertFalse(j1_3.hasParameter(parName1));

    	String parName2 = "PARNAME2";
    	j1.setParameter(parName2, 456, true);
    	assertTrue(j1.hasParameter(parName2));
    	assertTrue(j1_1.hasParameter(parName2));
    	assertTrue(j1_2.hasParameter(parName2));
    	assertTrue(j1_3.hasParameter(parName2));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetInnermostFirstStep() 
    {
    	Job j1 = new Job();
    	Job j1_1 = new Job();
    	Job j1_2 = new Job();
    	Job j1_3 = new Job();
    	Job j1_1_1 = new Job();
    	Job j1_1_2 = new Job();
    	Job j1_1_2_1 = new Job();
    	Job j1_3_1 = new Job();
    	Job j1_3_1_1 = new Job();
    	Job j1_3_1_1_1 = new Job();
    	
    	j1.addStep(j1_1);
    	j1.addStep(j1_2);
    	j1.addStep(j1_3);
    	

    	j1_1.addStep(j1_1_1);
    	j1_1.addStep(j1_1_2);
    	j1_1_2.addStep(j1_1_2_1);

    	j1_3.addStep(j1_3_1);
    	j1_3_1.addStep(j1_3_1_1);
    	j1_3_1_1.addStep(j1_3_1_1_1);
    	
    	Job result = j1.getInnermostFirstStep();
    	assertTrue(j1_1_1 == result);
    	assertFalse(j1_1_2_1 == result);
    	assertFalse(j1_3_1_1_1 == result);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetExposedData() 
    {
    	Job job = new Job();
    	
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key1", 
    			1.234)); 
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key2", 
    			new ArrayList<Integer>(Arrays.asList(11, 21, 31))));
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key3", 
    			new HashMap<String, Integer>() {{
    			    put("a", 12); put("b", 22);
    			}}));
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key4", 
    			new HashMap<Integer, Integer>() {{
    			    put(1, 13); put(-2, 23); put(0, -1);
    			}}));
    	
    	Object data = job.getExposedData(new String[] {"Lev0Key1"});
    	assertTrue(data instanceof Double);
    	assertTrue(NumberUtils.closeEnough(1.234, (Double) data));
    	
    	data = job.getExposedData(new String[]{"Lev0Key2"});
    	assertTrue(data instanceof ArrayList);
    	assertEquals(3, ((List<?>) data).size());
    	data = job.getExposedData(new String[]{"Lev0Key2", "2"});
    	assertTrue(data instanceof Integer);
    	assertEquals(31, (Integer) data);

    	data = job.getExposedData(new String[]{"Lev0Key3"});
    	assertTrue(data instanceof Map);
    	data = job.getExposedData(new String[]{"Lev0Key3", "a"});
    	assertTrue(data instanceof Integer);
    	assertEquals(12, (Integer) data);

    	data = job.getExposedData(new String[]{"Lev0Key4"});
    	assertTrue(data instanceof Map);
    	data = job.getExposedData(new String[]{"Lev0Key4", "0"});
    	assertTrue(data instanceof Integer);
    	assertEquals(-1, (Integer) data);
    	
    	
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key5", 
    			new NamedData("Lev1Key5", 
    			12.34))); 
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key6", 
    			new NamedData("Lev1Key6", 
    			new ArrayList<Integer>(Arrays.asList(111, 211, 311)))));
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key7", 
    			new NamedData("Lev1Key7", 
    			new HashMap<String, Integer>() {{
    			    put("a", 121); put("b", 221);
    			}})));
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key8", 
    			new NamedData("Lev1Key8", 
    			new HashMap<Integer, Integer>() {{
    			    put(1, 131); put(-2, 231); put(0, -11);
    			}})));
    	
    	data = job.getExposedData(new String[] {"Lev0Key5", "Lev1Key5"});
    	assertTrue(data instanceof Double);
    	assertTrue(NumberUtils.closeEnough(12.34, (Double) data));
    	
    	data = job.getExposedData(new String[]{"Lev0Key6", "Lev1Key6"});
    	assertTrue(data instanceof ArrayList);
    	data = job.getExposedData(new String[]{"Lev0Key6", "Lev1Key6", "2"});
    	assertTrue(data instanceof Integer);
    	assertEquals(311, (Integer) data);

    	data = job.getExposedData(new String[]{"Lev0Key7", "Lev1Key7"});
    	assertTrue(data instanceof Map);
    	data = job.getExposedData(new String[]{"Lev0Key7", "Lev1Key7", "a"});
    	assertTrue(data instanceof Integer);
    	assertEquals(121, (Integer) data);

    	data = job.getExposedData(new String[]{"Lev0Key8", "Lev1Key8"});
    	assertTrue(data instanceof Map);
    	data = job.getExposedData(new String[]{"Lev0Key8", "Lev1Key8", "0"});
    	assertTrue(data instanceof Integer);
    	assertEquals(-11, (Integer) data);
    	
    	
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key13", 
    			new NamedData("Lev1Key13", 
    					new NamedData("Lev2Key13", 
    							new NamedData("Lev3Key13",
    			123.4))))); 
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key14", 
    			new NamedData("Lev1Key14", 
    					new NamedData("Lev2Key14", 
    							new NamedData("Lev3Key14",
    			new ArrayList<Integer>(Arrays.asList(113, 213, 313)))))));
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key15", 
    			new NamedData("Lev1Key15", 
    					new NamedData("Lev2Key15", 
    							new NamedData("Lev3Key15",
    			new HashMap<String, Integer>() {{
    			    put("a", 123); put("b", 223);
    			}})))));
    	job.exposedOutput.putNamedData(new NamedData("Lev0Key16", 
    			new NamedData("Lev1Key16", 
    					new NamedData("Lev2Key16", 
    							new NamedData("Lev3Key16",
    			new HashMap<Integer, Integer>() {{
    			    put(1, 133); put(2, 233); put(0, -13);
    			}})))));
    	
    	data = job.getExposedData(new String[] {"Lev0Key13", "Lev1Key13", "Lev2Key13", "Lev3Key13"});
    	assertTrue(data instanceof Double);
    	assertTrue(NumberUtils.closeEnough(123.4, (Double) data));
    	
    	data = job.getExposedData(new String[]{"Lev0Key14", "Lev1Key14", "Lev2Key14", "Lev3Key14"});
    	assertTrue(data instanceof ArrayList);
    	data = job.getExposedData(new String[]{"Lev0Key14", "Lev1Key14", "Lev2Key14", "Lev3Key14", "2"});
    	assertTrue(data instanceof Integer);
    	assertEquals(313, (Integer) data);

    	data = job.getExposedData(new String[]{"Lev0Key15", "Lev1Key15", "Lev2Key15", "Lev3Key15"});
    	assertTrue(data instanceof Map);
    	data = job.getExposedData(new String[]{"Lev0Key15", "Lev1Key15", "Lev2Key15", "Lev3Key15", "b"});
    	assertTrue(data instanceof Integer);
    	assertEquals(223, (Integer) data);

    	data = job.getExposedData(new String[]{"Lev0Key16", "Lev1Key16", "Lev2Key16", "Lev3Key16"});
    	assertTrue(data instanceof Map);
    	data = job.getExposedData(new String[]{"Lev0Key16", "Lev1Key16", "Lev2Key16", "Lev3Key16", "2"});
    	assertTrue(data instanceof Integer);
    	assertEquals(233, (Integer) data);
    	
    	
    	// Test results in case of no match
    	data = job.getExposedData(new String[]{"Lev0KeyNotThere"});
    	assertNull(data);
    	
    	// Test results in case of not enough levels
    	data = job.getExposedData(new String[]{"Lev0Key16", "Lev0KeyNotThere"});
    	assertNull(data);
    	data = job.getExposedData(new String[]{"Lev0Key16", "Lev1Key16", "Lev0KeyNotThere"});
    	assertNull(data);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetExposedData_nestedJobs() 
    {
    	Job containerJob = new Job();
    	String dataInContainerJob = "1234";
    	containerJob.exposedOutput.putNamedData(new NamedData("DataName", 
    			dataInContainerJob));
    	
    	Job siblinJob = new Job();
    	int dataInSiblinJob = 123;
    	siblinJob.exposedOutput.putNamedData(new NamedData("DataName", 
    			dataInSiblinJob));
    	siblinJob.exposedOutput.putNamedData(new NamedData("Lev0Key16", 
    			new NamedData("Lev1Key16", 
    					new NamedData("Lev2Key16", 
    			new HashMap<Integer, Integer>() {{
    			    put(1, 133); put(2, 233); put(0, -13);
    			}}))));
    	
    	Job job = new Job();
    	double dataInFocusJob = 4.567;
    	job.exposedOutput.putNamedData(new NamedData("DataName", 
    			dataInFocusJob));
    	
    	// Define relationships
    	containerJob.addStep(siblinJob);
    	containerJob.addStep(job);
    	
    	// Test exposed data from "this" very job
    	Object result = job.getExposedData("#0", new String[]{"DataName"});
    	assertTrue(result instanceof Double);
    	assertTrue(NumberUtils.closeEnough(dataInFocusJob, (Double)result));
    	
    	// Test exposed data from parent job
    	result = job.getExposedData("#-1", new String[]{"DataName"});
    	assertTrue(result instanceof String);
    	assertEquals(dataInContainerJob, (String)result);

    	// Test exposed data from sibling job
    	result = job.getExposedData("#-1.0", new String[]{"DataName"});
    	assertTrue(result instanceof Integer);
    	assertEquals(dataInSiblinJob, (Integer)result);

    	// Test de-tour leading to original job
    	result = job.getExposedData("#-1.1", new String[]{"DataName"});
    	assertTrue(result instanceof Double);
    	assertTrue(NumberUtils.closeEnough(dataInFocusJob, (Double)result));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testFetchValuesFromJobsTree() 
    {
    	Job containerob = new Job();
    	containerob.exposedOutput.putNamedData(new NamedData("DataName", 
    			new NamedData("NestedData", 
    			new ArrayList<NamedData>(Arrays.asList(
    					new NamedData("DataItem1", 111),
    					new NamedData("DataItem1", 222),
    					new NamedData("DataItem1", 333))))));
    	
    	Job siblinJob = new Job();
    	siblinJob.exposedOutput.putNamedData(new NamedData("Foo", 
    			new NamedData("Bar", 123)));
    	
    	Job job = new Job();
    	job.exposedOutput.putNamedData(new NamedData("DataName", 4.567));
    	job.exposedOutput.putNamedData(new NamedData("Foo", 
    			new NamedData("Bar", 456)));
    	job.setParameter("KEY1", "value 1");
    	job.setParameter("KEY2", 2);
    	job.setParameter("KEY3", 3.3);
    	job.exposedOutput.putNamedData(new NamedData("DoubleAsString", "8.901"));
    	
    	containerob.addStep(siblinJob);
    	containerob.addStep(job);
   
    	job.fetchValuesFromJobsTree();

    	// Test no change is none is meant
    	assertEquals(3, job.getParameters().size());
    	assertEquals("value 1", job.getParameter("Key1").getValueAsString());
    	assertEquals(2, job.getParameter("Key2").getValue());
    	assertTrue(NumberUtils.closeEnough(3.3, 
    			(Double) job.getParameter("Key3").getValue()));
    	
    	// Test fetching from within the job (which useless spaces)
    	job.setParameter("KEY4", "value is "+Job.GETACCJOBSDATA+"( #0 , DataName )_other");
    	job.fetchValuesFromJobsTree();
    	String modifiedValue = job.getParameter("Key4").getValueAsString();
    	assertTrue(modifiedValue.startsWith("value is 4.56"));
    	assertTrue(modifiedValue.endsWith("_other"));
    	assertTrue(NumberUtils.closeEnough(4.567, Double.parseDouble(
    			modifiedValue.replaceAll("value is ", "").replaceAll("_other", ""))));
    	
    	// Test fetching from container job (leading/trailing spaces)
    	job.setParameter("KEY5", Job.GETACCJOBSDATA+"( # -1 ,DataName ,"
    			+ " NestedData, 2 , DataItem1 )_leftover");
    	job.fetchValuesFromJobsTree();
    	assertEquals("333_leftover", job.getParameter("Key5").getValueAsString());
    	
    	// Test fetching from sibling job and multiple replacements
    	job.setParameter("KEY6", Job.GETACCJOBSDATA+"(#-1.0,Foo,Bar) and " 
    	+ Job.GETACCJOBSDATA + "(#0,Foo,Bar):(foo)");
    	job.fetchValuesFromJobsTree();
    	assertEquals("123 and 456:(foo)", job.getParameter("Key6").getValueAsString());
    		
    	job.setParameter("KEY7", "Some text:" 
    		+ Job.GETACCJOBSDATA + "(#-1,DataName,NestedData,0,DataItem1) and " 
    		+ Job.GETACCJOBSDATA + "(#-1.0,Foo,Bar):(foo) "
    		+ Job.GETACCJOBSDATA + "(#0,DoubleAsString)");
    	job.fetchValuesFromJobsTree();
    	assertEquals("Some text:111 and 123:(foo) 8.901", 
    			job.getParameter("Key7").getValueAsString());
    	
    	// Previously altered params did not change
    	assertEquals("value 1", job.getParameter("Key1").getValueAsString());
    	assertTrue(NumberUtils.closeEnough(4.567, Double.parseDouble(
    			modifiedValue.replaceAll("value is ", "").replaceAll("_other", ""))));
    	assertEquals("123 and 456:(foo)", job.getParameter("Key6").getValueAsString());
    	
    	// Test behavior with inconsistent input
    	job.setParameter("KEY8", Job.GETACCJOBSDATA + "(#0.1.1,DataName)");
    	job.fetchValuesFromJobsTree();
    	assertEquals("null", job.getParameter("Key8").getValueAsString());
    	
    	job.setParameter("KEY8", Job.GETACCJOBSDATA + "(#0,not matched)");
    	job.fetchValuesFromJobsTree();
    	assertEquals("null", job.getParameter("Key8").getValueAsString());
    	
    }
    
//------------------------------------------------------------------------------

}
