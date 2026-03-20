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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Test
    public void testBuildFromJSONFile_appliesMultipleReplacementsInOrder()
            throws IOException
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");

        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "repl.json");
        try (FileWriter w = new FileWriter(jdFile))
        {
            w.write("{"
                    + "\"jobType\": \"ACCJob\","
                    + "\"params\": ["
                    + "{\"reference\": \"" + WorkerConstants.PARTASK + "\", "
                    + "\"value\": \"" + DummyWorker.DUMMYTASKTASK.casedID + "\"},"
                    + "{\"reference\": \"P1\", \"value\": \"<<<A>>>/file.txt\"},"
                    + "{\"reference\": \"P2\", \"value\": \"<<<B>>>\"}"
                    + "]"
                    + "}");
        }

        Map<String, String> repl = new LinkedHashMap<>();
        repl.put("<<<A>>>", "/data/run");
        repl.put("<<<B>>>", "done");

        Job job = JobFactory.buildFromJSONFile(jdFile, repl);

        assertNotNull(job);
        assertEquals("/data/run/file.txt",
                job.getParameter("P1").getValueAsString(),
                "Both placeholders in one value are replaced");
        assertEquals("done", job.getParameter("P2").getValueAsString());
    }

//------------------------------------------------------------------------------

    @Test
    public void testBuildFromJSONFile_replacementsAreSequential()
            throws IOException
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");

        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "order.json");
        try (FileWriter w = new FileWriter(jdFile))
        {
            w.write("{"
                    + "\"jobType\": \"ACCJob\","
                    + "\"params\": ["
                    + "{\"reference\": \"" + WorkerConstants.PARTASK + "\", "
                    + "\"value\": \"" + DummyWorker.DUMMYTASKTASK.casedID + "\"},"
                    + "{\"reference\": \"CHAIN\", \"value\": \"__WHOLE__\"}"
                    + "]"
                    + "}");
        }

        Map<String, String> repl = new LinkedHashMap<>();
        repl.put("__WHOLE__", "__PART__");
        repl.put("__PART__", "__FINAL__");

        Job job = JobFactory.buildFromJSONFile(jdFile, repl);

        assertEquals("__FINAL__", job.getParameter("CHAIN").getValueAsString(),
                "Second replacement must see text produced by the first");
    }

//------------------------------------------------------------------------------

    @Test
    public void testBuildFromParametersFile_appliesMultipleReplacements()
            throws IOException
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");

        File paramFile = new File(tempDir.getAbsolutePath() + SEP + "repl.par");
        try (FileWriter writer = new FileWriter(paramFile))
        {
            writer.write(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
                    + DummyWorker.DUMMYTASKTASK.casedID + NL);
            writer.write("INPATH" + ParameterConstants.SEPARATOR
                    + "<<<ROOT>>>/input.dat" + NL);
            writer.write("STEM" + ParameterConstants.SEPARATOR + "<<<TAG>>>" + NL);
        }

        Map<String, String> repl = new LinkedHashMap<>();
        repl.put("<<<ROOT>>>", "/tmp/wd");
        repl.put("<<<TAG>>>", "mol42");

        Job job = JobFactory.buildFromParametersFile(paramFile, repl);

        assertNotNull(job);
        assertEquals("/tmp/wd/input.dat",
                job.getParameter("INPATH").getValueAsString());
        assertEquals("mol42", job.getParameter("STEM").getValueAsString());
    }

//------------------------------------------------------------------------------

    @Test
    public void testBuildFromParametersFile_replacementTreatsOldAsLiteralSubstring()
            throws IOException
    {
        assertTrue(this.tempDir.isDirectory(), "Should be a directory ");

        File paramFile = new File(tempDir.getAbsolutePath() + SEP + "regex.par");
        try (FileWriter writer = new FileWriter(paramFile))
        {
            writer.write(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
                    + DummyWorker.DUMMYTASKTASK.casedID + NL);
            writer.write("NOTE" + ParameterConstants.SEPARATOR
                    + "see (a+b) for details" + NL);
        }

        Map<String, String> repl = new LinkedHashMap<>();
        repl.put("a+b", "PLUS");

        Job job = JobFactory.buildFromParametersFile(paramFile, repl);

        assertEquals("see (PLUS) for details",
                job.getParameter("NOTE").getValueAsString(),
                "Old string must be literal, not a regex (a+b is not 'one or more a')");
    }

//------------------------------------------------------------------------------

}
