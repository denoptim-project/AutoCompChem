package autocompchem.run.jobediting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.run.Job;

public class DeleteJobParameterTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	DeleteJobParameter tA = new DeleteJobParameter("ParName");
    	DeleteJobParameter tB = new DeleteJobParameter("ParName");

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new DeleteJobParameter("different");
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	DeleteJobParameter original = new DeleteJobParameter("ParName");
    	String json = writer.toJson(original);
    	DeleteJobParameter fromJson = reader.fromJson(json, 
    			DeleteJobParameter.class);
    	assertEquals(original, fromJson);
    	
    	IJobEditingTask fromJson2 = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(original, fromJson2);
    	
    	//TODO-gg del
    	System.out.println(original.getClass().getName()+": "+json);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges() throws Exception
    {
    	Job job = new Job();
    	job.setParameter("ParamA", "valueA");
    	job.setParameter("ParamB", "valueB");
    	
    	// Remove existing parameter
    	DeleteJobParameter task = new DeleteJobParameter("ParamA");
    	task.applyChange(job);
    	assertFalse(job.hasParameter("ParamA"));
    	assertTrue(job.hasParameter("ParamB"));
    	
    	// Attempt to remove non-existing parameter
    	task = new DeleteJobParameter("ParamZ");
    	task.applyChange(job);
    	assertFalse(job.hasParameter("ParamZ"));
    	assertTrue(job.hasParameter("ParamB"));
    }
    
//------------------------------------------------------------------------------

}
