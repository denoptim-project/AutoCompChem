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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.ACCFileType;


/**
 * Unit Test for job definition conversion tool
 * 
 * @author Marco Foscato
 */

public class JobDefinitionConverterTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testJobCreationFromJDFile() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define pathnames
        File paramFile = new File(tempDir.getAbsolutePath() + SEP + "job.par");
        File jsonFile = new File(tempDir.getAbsolutePath() + SEP + "job.json");

        try 
        {
            FileWriter writer = new FileWriter(paramFile);

            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write("keyZero" + ParameterConstants.SEPARATOR 
            		+ "valueZero" + NL);
            writer.write(ParameterConstants.PARALLELIZE 
            		+ ParameterConstants.SEPARATOR + "2" + NL);
            
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

            writer.write(ParameterConstants.STARTJOB + NL);
            writer.write(ParameterConstants.RUNNABLEAPPIDKEY 
            		+ ParameterConstants.SEPARATOR 
            		+ SoftwareId.UNDEFINED + NL);
            writer.write("keyThree" + ParameterConstants.SEPARATOR 
            		+ "valueThree" + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            writer.write(ParameterConstants.ENDJOB + NL);
            writer.close();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }    
        
        JobDefinitionConverter.convertJobDefinitionFile(paramFile, jsonFile, 
        		ACCFileType.JSON);

        Job jobParam = JobFactory.buildFromFile(paramFile);
        Job jobJSON = JobFactory.buildFromFile(jsonFile);
        
        assertEquals(3, jobJSON.getNumberOfSteps(), "Number of steps");
        assertEquals(jobJSON.getStep(0).getAppID(), SoftwareId.ACC,
        		"App for first step");
        assertEquals(jobJSON.getStep(1).getAppID(), SoftwareId.SHELL,
        		"App for second step");
        assertEquals(jobJSON.getStep(2).getAppID(), SoftwareId.UNDEFINED,
        		"App for third step");
        
        assertTrue(jobParam.equals(jobJSON));
    }
  
//------------------------------------------------------------------------------

}
