package autocompchem.run;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Disabled;

import java.io.FileWriter;
import java.io.File;
import java.util.ArrayList;

import autocompchem.files.FilesAnalyzer;
import autocompchem.parameters.ParameterConstants;


/**
 * Unit Test for the factory of the job factory.
 * 
 * @author Marco Foscato
 */

public class JobFactoryTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;

    @Test
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

            Job job = JobFactory.buildFromFile(jdFile.getAbsolutePath());

            assertEquals(3,job.getNumberOfSteps(),"Number of 1st level jobs");
            assertEquals("value2",
                        job.getStep(0).getParameter("KEY2").getValueAsString());
            assertEquals("value4",
                        job.getStep(1).getParameter("KEY4").getValueAsString());
            assertEquals("value5",
                        job.getStep(2).getParameter("KEY5").getValueAsString());
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }
    }

//------------------------------------------------------------------------------

    @Test
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

            Job job = JobFactory.buildFromFile(jdFile.getAbsolutePath());

            assertEquals("value1",
                        job.getParameter("KEY1").getValueAsString());
            assertEquals("value6",
                        job.getParameter("KEY6").getValueAsString());
            assertEquals(1,job.getNumberOfSteps(),"Number of 1st level jobs");

            assertEquals("v1A",
                        job.getStep(0).getParameter("K1A").getValueAsString());
            assertEquals("v1B",
                        job.getStep(0).getParameter("K1B").getValueAsString());
            assertEquals(1,job.getStep(0).getNumberOfSteps(),
                                                    "Number of 2nd level jobs");

            assertEquals("v2A",job.getStep(0).getStep(0)
                                      .getParameter("K2A").getValueAsString());
            assertEquals("v2B",job.getStep(0).getStep(0)
                                      .getParameter("K2B").getValueAsString());
            assertEquals(1,job.getStep(0).getStep(0).getNumberOfSteps(),
                                                    "Number of 3rd level jobs");

            assertEquals("v3A",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("K3A").getValueAsString());
            assertEquals("v3B",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("K3B").getValueAsString());
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
            job = JobFactory.buildFromFile(jdFile.getAbsolutePath());

            // Total number of branches
            assertEquals(2,job.getNumberOfSteps(),"B-Num. of 0th level jobs");

            // First branch is as above but shifted down one level
            assertEquals("value1",job.getStep(0)
                                      .getParameter("KEY1").getValueAsString());
            assertEquals("value6",job.getStep(0)
                                      .getParameter("KEY6").getValueAsString());
            assertEquals(1,job.getStep(0)
                                .getNumberOfSteps(),"B-Num. of 1st level jobs");

            assertEquals("v1A",job.getStep(0)
                           .getStep(0).getParameter("K1A").getValueAsString());
            assertEquals("v1B",job.getStep(0)
                           .getStep(0).getParameter("K1B").getValueAsString());
            assertEquals(1,job.getStep(0).getStep(0).getNumberOfSteps(),
                                                    "B-Num. of 2nd level jobs");

            assertEquals("v2A",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("K2A").getValueAsString());
            assertEquals("v2B",job.getStep(0).getStep(0).getStep(0)
                                      .getParameter("K2B").getValueAsString());
            assertEquals(1,job.getStep(0).getStep(0).getStep(0)
                                .getNumberOfSteps(),"B-Num. of 3rd level jobs");

            assertEquals("v3A",job.getStep(0).getStep(0).getStep(0).getStep(0)
                                      .getParameter("K3A").getValueAsString());
            assertEquals("v3B",job.getStep(0).getStep(0).getStep(0).getStep(0)
                                      .getParameter("K3B").getValueAsString());
            assertEquals(0,
             job.getStep(0).getStep(0).getStep(0).getStep(0).getNumberOfSteps(),
                                                    "B-Num. of 4th level jobs");
            // Second branch
            assertEquals("v4A",job.getStep(1)
                                      .getParameter("K4A").getValueAsString());
            assertEquals("v4B",job.getStep(1)
                                      .getParameter("K4B").getValueAsString());
            assertEquals(1,job.getStep(1)
                                .getNumberOfSteps(),"C-Num. of 1st level jobs");

            assertEquals("v5A",job.getStep(1)
                           .getStep(0).getParameter("K5A").getValueAsString());
            assertEquals("v5B",job.getStep(1)
                           .getStep(0).getParameter("K5B").getValueAsString());
            assertEquals(1,job.getStep(1).getStep(0).getNumberOfSteps(),
                                                    "C-Num. of 2nd level jobs");

            assertEquals("v6A",job.getStep(1).getStep(0).getStep(0)
                                      .getParameter("K6A").getValueAsString());
            assertEquals("v6B",job.getStep(1).getStep(0).getStep(0)
                                      .getParameter("K6B").getValueAsString());
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
