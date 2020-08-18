package autocompchem.chemsoftware.orca;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;
import autocompchem.run.JobFactory;


/**
 * Unit Test for Orca output handler
 * 
 * @author Marco Foscato
 */

public class OrcaOutputHandlerTest 
{
	private final String NL = System.getProperty("line.separator");
	
    @TempDir 
    File tempDir;
    
//-----------------------------------------------------------------------------
    
    //TODO remove
    
    //@Test
    public void test() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	String tmpPathName = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "orca.log";
    	
    	StringBuilder sb = new StringBuilder();
    	
    	//TODO: replace with some actual of get rid of this class altogether!
    	
    	sb.append("line 1").append(NL);
    	sb.append("line 2").append(NL);
    	sb.append("line 3").append(NL);
    	sb.append("line 4").append(NL);
    	sb.append("line 5").append(NL);
    	sb.append("line 6");
    	IOtools.writeTXTAppend(tmpPathName, sb.toString(), false);
    	
    	//TODO del
    	tmpPathName = "/Users/marco/tools/AutoCompChem_multiTask/test/t117-orca.log";
    	
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter(new Parameter(WorkerConstants.PARTASK,
    			TaskID.ANALYSEORCAOUTPUT.toString()));
    	ps.setParameter(new Parameter("VERBOSITY","1"));
    	ps.setParameter(new Parameter(ChemSoftConstants.PARJOBOUTPUTFILE, 
    			tmpPathName));
    	
    	Worker worker = WorkerFactory.createWorker(TaskID.ANALYSEORCAOUTPUT);
        worker.setParameters(ps);
        worker.initialize();
        worker.performTask();
        
        //TODO
        
        assertTrue(false,"UNIT TEST STILL TO IMPLEMENT");
    }

//------------------------------------------------------------------------------

}
