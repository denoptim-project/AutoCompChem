package autocompchem.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.SoftwareId;
import autocompchem.worker.DummyWorker;
import autocompchem.worker.WorkerConstants;

/**
 * Unit Test for methods in ACCMain. 
 * 
 * @author Marco Foscato
 */

public class ACCMainTest 
{

    private final String NL = System.getProperty("line.separator");
    
    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testCLIArgsParsing() throws Exception
    {
    	String[] args = {"-00", "-t", "DummyTask", 
    			"--input", "~/path/input.in", 
    			"--out", "file.out",
    			"--long", "\"many", "words", "all", "quoted\"",
    			"--z", "--z2"};

    	// NB: do this to trigger the generation of the task even if we do not
    	// use it here. When running all the tests this is not even needed as
    	// the next test, which runs before this one, will create the task.
    	@SuppressWarnings("unused")
		String task = DummyWorker.DUMMYTASKTASK.casedID;
    	
    	Job job = null;
		try {
			job = ACCMain.parseCLIArgs(args);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse CLI arguments: " 
					+ e.getMessage(), e);
		}
		if (job == null) {
			throw new RuntimeException("Job from parsing CLI args is null.");
		}
		ParameterStorage params = job.getParameters();
    	
    	assertEquals(SoftwareId.ACC, job.getAppID(),"Job APP");
    	assertTrue(params.contains("long"),"Parsed long and quoted option.");
    	assertEquals(4,params.getParameter("long").getValueAsString()
    			.split("\\s+").length,"Length of long and quoted option.");

    	assertEquals("file.out",params.getParameter("out").getValueAsString(),
    			"Value of option '--out'.");
    	assertTrue(params.contains("00"),"Parsed value-less option (first)");
    	assertTrue(params.contains("z2"),"Parsed value-less option (last)");
    }

//------------------------------------------------------------------------------

    @Test
    public void testCLIArgsParsingAndParFile() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        final String RTN = "FromCLI";
        final String EXT = "_suffix.ext";
        
        String tmpPathName = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "acc.params";
        StringBuilder sb = new StringBuilder();
        sb.append(ParameterConstants.RUNNABLEAPPIDKEY
        		+ParameterConstants.SEPARATOR
        		+SoftwareId.ACC+NL);
        sb.append(WorkerConstants.PARTASK
        		+ParameterConstants.SEPARATOR
        		+DummyWorker.DUMMYTASKTASK.casedID +NL);
        sb.append("CUSTOM_PAR"
        		+ParameterConstants.SEPARATOR
        		+"bla bla ribla"+NL);
        sb.append("P1"
        		+ParameterConstants.SEPARATOR
        		+"value from file"+NL);
        sb.append("P2"
        		+ParameterConstants.SEPARATOR
        		+ParameterConstants.STRINGFROMCLI+EXT);
        IOtools.writeTXTAppend(new File(tmpPathName),sb.toString(),false);
        
        String[] args = {"-p3","\"param","from","command","line\"",
        		"-P1","value_from_CLI","-p", tmpPathName,
        		"--"+ParameterConstants.STRINGFROMCLI, RTN};
    	
    	Job job = null;
		try {
			job = ACCMain.parseCLIArgs(args);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse CLI arguments: " 
					+ e.getMessage(), e);
		}
		if (job == null) {
			throw new RuntimeException("Job from parsing CLI args is null.");
		}
    	
    	assertEquals(SoftwareId.ACC,job.getAppID(),"Job APP");
    	assertEquals(DummyWorker.DUMMYTASKTASK.casedID,job.getParameter(
    			WorkerConstants.PARTASK).getValueAsString(),
    			"Task ID");
    	assertEquals("bla bla ribla",job.getParameter(
    			"CUSTOM_PAR").getValueAsString(),
    			"Parameter in params file");
    	assertTrue(job.hasParameter("P1"),"Has parameter P1");
    	assertEquals("value_from_CLI",job.getParameter("P1").getValueAsString(),
    			"Parameter in command line overwrites value from params file");
    	assertTrue(job.hasParameter("P2"),"Has parameter P2");
    	assertEquals(RTN+EXT,job.getParameter("P2").getValueAsString(),
    			"Parameter in command line overwrites value from params file");
    	assertTrue(job.hasParameter("P3"),"Has parameter P3");
    	assertEquals("param from command line",job.getParameter(
    			"P3").getValueAsString(),
    			"Parameter in command line");
    }
    
//------------------------------------------------------------------------------

}
