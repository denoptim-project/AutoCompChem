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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.worker.DummyWorker;
import autocompchem.worker.WorkerConstants;


/**
 * Unit Test for the factory of the jobs.
 * 
 * @author Marco Foscato
 */

public class JobFactoryTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;
    
//-----------------------------------------------------------------------------
    
    //@Test
    public void testCreateJob() throws Exception
    {
    	Job job = JobFactory.createJob(new SoftwareId("something"));
    	assertTrue("something".equals(job.getAppID().toString()), 
    			"Creation of Undefined job");
    	
    	job = JobFactory.createJob(SoftwareId.SHELL);
    	assertTrue(SoftwareId.SHELL.equals(job.getAppID()), 
    			"Creation of SHELL job");

    	job = JobFactory.createJob(SoftwareId.ACC);
    	assertTrue(SoftwareId.ACC.equals(job.getAppID()), 
    			"Creation of ACC job");
    	
    	job = new Job();
    	assertTrue(SoftwareId.UNDEFINED.equals(job.getAppID()), 
    			"Creation of Undefined job");
    }
    
//-----------------------------------------------------------------------------
    
    /*
     * The difference between this test and the testJobCreationFromJDFile is 
     * that here we have only the parameters and no JOBSTART/JOBEND strings.
     * So, the entire text is to be understood as a single text block that
     * pertains a single job.
     */
    
    //@Test
    public void testJobCreatsSimpleFromParamsFile() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        File paramFile = new File(tempDir.getAbsolutePath() + SEP + "acc.par");

        Job job = null;
        try 
        {
            FileWriter writer = new FileWriter(paramFile);

            writer.write(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
            		+ DummyWorker.DUMMYTASKTASK.casedID + NL);
            writer.write("key1" + ParameterConstants.SEPARATOR 
            		+ "value1" + NL);
            writer.write("key2" + ParameterConstants.SEPARATOR 
            		+ "value2a value2b" + NL);
            writer.close();

            job = JobFactory.buildFromFile(paramFile);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Exception. Unable to work with tmp files.");
        }  

        assertNotNull(job,"Job is null");
        assertEquals(0, job.getNumberOfSteps(), "Number of sub steps");
        assertEquals(SoftwareId.ACC, job.getAppID(),
        		"App for master job");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testJobCreateMonitoringJob() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        File paramFile = new File(tempDir.getAbsolutePath() + SEP + "acc.par");

        Job job = null;
        try 
        {
            FileWriter writer = new FileWriter(paramFile);

            writer.write(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
            		+ JobEvaluator.EVALUATEJOBTASK.casedID + NL);
            writer.write(MonitoringJob.DELAYPAR + ParameterConstants.SEPARATOR
            		+ "3" + NL);
            writer.write(MonitoringJob.DELAYUNITS 
            		+ ParameterConstants.SEPARATOR 
            		+ "SECONDS" + NL);
            writer.write(MonitoringJob.PERIODPAR + ParameterConstants.SEPARATOR
            		+ "1" + NL);
            writer.write(MonitoringJob.PERIODUNITS 
            		+ ParameterConstants.SEPARATOR
            		+ "MINUTES" + NL);
            writer.close();

            job = JobFactory.buildFromFile(paramFile);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Exception. Unable to work with tmp files.");
        }  

        assertNotNull(job,"Job is null");
        assertEquals(MonitoringJob.class, job.getClass(), 
        		"Type of job object");
        assertEquals(0, job.getNumberOfSteps(), "Number of sub steps");
        assertEquals(SoftwareId.ACC, job.getAppID(), "App for job");
        assertEquals(3000, ((MonitoringJob) job).getDelay(),
        		"Value of delay in milliseconds");
        assertEquals(60000, ((MonitoringJob) job).getPeriod(),
        		"Value of period in milliseconds");
    }
    
//-----------------------------------------------------------------------------
    
    //@Test
    public void testJobCreationFromJDFile() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define pathnames
        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "acc.par");

        try 
        {
            FileWriter writer = new FileWriter(jdFile);

            //This will be ignores as it is outside on a jobstart-jobend block
            writer.write("keyZero" + ParameterConstants.SEPARATOR 
            		+ "valueZero" + NL);
            
            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write(ParameterConstants.RUNNABLEAPPIDKEY 
            		+ ParameterConstants.SEPARATOR 
            		+ SoftwareId.ACC + NL);
            writer.write("keyOne" + ParameterConstants.SEPARATOR 
            		+ "valueOne" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);

            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write(ParameterConstants.RUNNABLEAPPIDKEY 
            		+ ParameterConstants.SEPARATOR 
            		+ SoftwareId.SHELL + NL);
            writer.write("keyTwo" + ParameterConstants.SEPARATOR 
            		+ "valueTwo" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            
            //This will be ignores as it is outside on a jobstart-jobend block
            writer.write("keyEnd" + ParameterConstants.SEPARATOR 
            		+ "valueEnd" + NL);

            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write(ParameterConstants.RUNNABLEAPPIDKEY 
            		+ ParameterConstants.SEPARATOR 
            		+ SoftwareId.UNDEFINED + NL);
            writer.write("keyThree" + ParameterConstants.SEPARATOR 
            		+ "valueThree" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            writer.close();

            Job job = JobFactory.buildFromFile(jdFile);
            
            assertEquals(3, job.getNumberOfSteps(), "Number of steps");
            assertEquals(job.getStep(0).getAppID(), SoftwareId.ACC,
            		"App for first step");
            assertEquals(job.getStep(1).getAppID(), SoftwareId.SHELL,
            		"App for second step");
            assertEquals(job.getStep(2).getAppID(), SoftwareId.UNDEFINED,
            		"App for third step");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }    
    }
        
//-----------------------------------------------------------------------------

    //@Test
    public void testMultiStepJobFromJDFile() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define path in tmp dir
        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "text.jd");

        try 
        {
            FileWriter writer = new FileWriter(jdFile);
            writer.write(ParameterConstants.COMMENTLINE + " bla bla" + NL);
            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write(ParameterConstants.COMMENTLINE + " comment 2" + NL);
            writer.write("key1" + ParameterConstants.SEPARATOR + "value1" + NL);
            writer.write("key2" + ParameterConstants.SEPARATOR + "value2" + NL);
            writer.write(ParameterConstants.STARTMULTILINE 
                        + "key3" + ParameterConstants.SEPARATOR + "value3" + NL 
                        + "value3b 3b 3b" + NL + "value3c 3c 3c 3c " + NL  
                        + ParameterConstants.ENDMULTILINE + NL);
            writer.write(ParameterConstants.ENDJOB + NL);

            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("key4" + ParameterConstants.SEPARATOR + "value4" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);

            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("key5" + ParameterConstants.SEPARATOR + "value5" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            writer.close();

            Job job = JobFactory.buildFromFile(jdFile);

            assertEquals(3,job.getNumberOfSteps(),"Number of 1st level jobs");
            assertEquals("value2",
                        job.getStep(0).getParameter("key2").getValueAsString());
            assertEquals("value4",
                        job.getStep(1).getParameter("key4").getValueAsString());
            assertEquals("value5",
                        job.getStep(2).getParameter("key5").getValueAsString());
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }
    }

//------------------------------------------------------------------------------

    //@Test
    public void testNestedJobsFromJDFiles() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define path in tmp dir
        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "nested.jd");

        try 
        {
            FileWriter writer = new FileWriter(jdFile);
            //  beginning Job1
            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("key1" + ParameterConstants.SEPARATOR + "value1" + NL);
            writer.write("key2" + ParameterConstants.SEPARATOR + "value2" + NL);
            writer.write(ParameterConstants.STARTMULTILINE 
                        + "key3" + ParameterConstants.SEPARATOR + "value3" + NL 
                        + "value3b 3b 3b" + NL + "value3c 3c 3c 3c " + NL  
                        + ParameterConstants.ENDMULTILINE + NL);
            //  - beginning NESTED Job1.1
            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("k1A" + ParameterConstants.SEPARATOR + "v1A" + NL);
            //  - - beginning NESTED Job1.1.1
            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("k2A" + ParameterConstants.SEPARATOR + "v2A" + NL);
            //  - - - beginning NESTED Job1.1.1.1
            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("k3A" + ParameterConstants.SEPARATOR + "v3A" + NL);
            writer.write("k3B" + ParameterConstants.SEPARATOR + "v3B" + NL);
            writer.write(ParameterConstants.STARTMULTILINE 
                        + "k3C" + ParameterConstants.SEPARATOR + "v3C" + NL 
                        + "v3C second line" + NL + "v3C third line " + NL  
                        + ParameterConstants.ENDMULTILINE + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            //  - - - end of    NESTED Job1.1.1.1
            writer.write("k2B" + ParameterConstants.SEPARATOR + "v2B" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            //  - - end of    NESTED Job1.1
            writer.write("k1B" + ParameterConstants.SEPARATOR + "v1B" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            //  - end of    NESTED Job1.1
            writer.write("key6" + ParameterConstants.SEPARATOR + "value6" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            //  end of    NESTED Job1
            writer.close();

            Job job = JobFactory.buildFromFile(jdFile);

            assertEquals("value1",
                        job.getParameter("key1").getValueAsString());
            assertEquals("value6",
                        job.getParameter("key6").getValueAsString());
            assertEquals(1,job.getNumberOfSteps(),"Number of 1st level jobs");

            assertEquals("v1A",
                        job.getStep(0).getParameter("k1A").getValueAsString());
            assertEquals("v1B",
                        job.getStep(0).getParameter("k1B").getValueAsString());
            assertEquals(1,job.getStep(0).getNumberOfSteps(),
                                                    "Number of 2nd level jobs");

            assertEquals("v2A",job.getStep(0).getStep(0)
                                      .getParameter("k2A").getValueAsString());
            assertEquals("v2B",job.getStep(0).getStep(0)
                                      .getParameter("k2B").getValueAsString());
            assertEquals(1,job.getStep(0).getStep(0).getNumberOfSteps(),
                                                    "Number of 3rd level jobs");

            assertEquals("v3A",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("k3A").getValueAsString());
            assertEquals("v3B",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("k3B").getValueAsString());
            assertEquals(0,
                       job.getStep(0).getStep(0).getStep(0).getNumberOfSteps(),
                                                    "Number of 4th level jobs");

            //Extend same file 
            FileWriter writer2 = new FileWriter(jdFile,true);
            writer2.write(ParameterConstants.STARTJOB + NL);
            writer2.write("k4B" + ParameterConstants.SEPARATOR + "v4B" + NL);
            writer2.write("k4A" + ParameterConstants.SEPARATOR + "v4A" + NL);
            //  - beginning NESTED Job1.1
            writer2.write(ParameterConstants.STARTJOB + NL);
            writer2.write("k5A" + ParameterConstants.SEPARATOR + "v5A" + NL);
            writer2.write("k5B" + ParameterConstants.SEPARATOR + "v5B" + NL);
            //  - - beginning NESTED Job1.1.1
            writer2.write(ParameterConstants.STARTJOB + NL);
            writer2.write("k6A" + ParameterConstants.SEPARATOR + "v6A" + NL);
            writer2.write("k6B" + ParameterConstants.SEPARATOR + "v6B" + NL);
            writer2.write(ParameterConstants.ENDJOB + NL);
            //  - - end of    NESTED Job1.1
            writer2.write(ParameterConstants.ENDJOB + NL);
            //  - end of    NESTED Job1.1
            writer2.write(ParameterConstants.ENDJOB + NL);
            writer2.close();

            // Reset job
            job = new Job();
            job = JobFactory.buildFromFile(jdFile);

            // Total number of branches
            assertEquals(2,job.getNumberOfSteps(),"B-Num. of 0th level jobs");

            // First branch is as above but shifted down one level
            assertEquals("value1",job.getStep(0)
                                      .getParameter("key1").getValueAsString());
            assertEquals("value6",job.getStep(0)
                                      .getParameter("key6").getValueAsString());
            assertEquals(1,job.getStep(0)
                                .getNumberOfSteps(),"B-Num. of 1st level jobs");

            assertEquals("v1A",job.getStep(0)
                           .getStep(0).getParameter("k1A").getValueAsString());
            assertEquals("v1B",job.getStep(0)
                           .getStep(0).getParameter("k1B").getValueAsString());
            assertEquals(1,job.getStep(0).getStep(0).getNumberOfSteps(),
                                                    "B-Num. of 2nd level jobs");

            assertEquals("v2A",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("k2A").getValueAsString());
            assertEquals("v2B",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("k2B").getValueAsString());
            assertEquals(1,job.getStep(0).getStep(0).getStep(0)
                                .getNumberOfSteps(),"B-Num. of 3rd level jobs");

            assertEquals("v3A",job.getStep(0).getStep(0).getStep(0).getStep(0)
                                      .getParameter("k3A").getValueAsString());
            assertEquals("v3B",job.getStep(0).getStep(0).getStep(0).getStep(0)
                                      .getParameter("k3B").getValueAsString());
            assertEquals(0,
             job.getStep(0).getStep(0).getStep(0).getStep(0).getNumberOfSteps(),
                                                    "B-Num. of 4th level jobs");
            // Second branch
            assertEquals("v4A",job.getStep(1)
                                      .getParameter("k4A").getValueAsString());
            assertEquals("v4B",job.getStep(1)
                                      .getParameter("k4B").getValueAsString());
            assertEquals(1,job.getStep(1)
                                .getNumberOfSteps(),"C-Num. of 1st level jobs");

            assertEquals("v5A",job.getStep(1)
                           .getStep(0).getParameter("k5A").getValueAsString());
            assertEquals("v5B",job.getStep(1)
                           .getStep(0).getParameter("k5B").getValueAsString());
            assertEquals(1,job.getStep(1).getStep(0).getNumberOfSteps(),
                                                    "C-Num. of 2nd level jobs");

            assertEquals("v6A",job.getStep(1).getStep(0).getStep(0)
                                      .getParameter("k6A").getValueAsString());
            assertEquals("v6B",job.getStep(1).getStep(0).getStep(0)
                                      .getParameter("k6B").getValueAsString());
            assertEquals(0,job.getStep(1).getStep(0).getStep(0)
                                .getNumberOfSteps(),"C-Num. of 3rd level jobs");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }
    }

//------------------------------------------------------------------------------

}
