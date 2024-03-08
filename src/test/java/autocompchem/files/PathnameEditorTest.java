package autocompchem.files;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Unit Test for the {@link PathnameEditor}
 * 
 * @author Marco Foscato
 */

public class PathnameEditorTest 
{
	final String SEP = File.separator;
	
//------------------------------------------------------------------------------

    @Test
    public void testGetPathName() throws Exception
    {
    	String input = "this"+SEP+"is"+SEP+"my"+SEP+"pathname";
    	
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter(WorkerConstants.PARTASK, 
    			PathnameEditor.GETPATHNAMETASK.casedID);
    	ps.setParameter("pathname", input);
    	
    	PathnameEditor pne = new PathnameEditor();
    	pne.setParameters(ps);
    	pne.initialize();
    	
    	assertEquals(input, pne.getPathName());
    	
    	ps.setParameter("prefix", "PRE");
    	ps.setParameter("suffix", " post");
    	ps.setParameter("quotation", "\"");
    	pne.setParameters(ps);
    	pne.initialize();
    	
    	assertEquals("\"PRE"+input+" post\"", pne.getPathName());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testInstanceCreation() throws Exception
    {
    	String input = "this"+SEP+"is"+SEP+"my"+SEP+"pathname";
    	
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter(WorkerConstants.PARTASK, 
    			PathnameEditor.GETPATHNAMETASK.casedID);
    	ps.setParameter("pathname", input);
    	
    	Worker worker = WorkerFactory.createWorker(ps, null);
    	
    	assertTrue(worker instanceof PathnameEditor);
    	
    	PathnameEditor pne = (PathnameEditor) worker;
    	assertEquals(input, pne.getPathName());
    }
    
//------------------------------------------------------------------------------

}
