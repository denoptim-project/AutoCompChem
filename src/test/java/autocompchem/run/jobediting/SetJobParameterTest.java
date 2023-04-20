package autocompchem.run.jobediting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.AppID;

public class SetJobParameterTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	SetJobParameter tA = new SetJobParameter(new NamedData("parName", 1.23));
    	SetJobParameter tB = new SetJobParameter(new NamedData("parName", 1.23));

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new SetJobParameter(new NamedData("different", 1.23));
    	assertFalse(tA.equals(tB));
    	
    	tB = new SetJobParameter(new NamedData("parName", 4.56));
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	SetJobParameter original = new SetJobParameter(
    			new NamedData("ParamToSet", "valueOfParam"));
    	String json = writer.toJson(original);
    	SetJobParameter fromJson = reader.fromJson(json, SetJobParameter.class);
    	assertEquals(original, fromJson);
    	
    	IJobEditingTask fromJson2 = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges() throws Exception
    {
    	Job job = JobFactory.createJob(AppID.ACC);
    	job.setParameter("ParamA", "valueA");
    	job.setParameter("ParamB", "valueB");
    	
    	// Add new parameter
    	NamedData parC = new NamedData("ParamC", 1.23);
    	SetJobParameter task = new SetJobParameter(parC);
    	task.applyChange(job);
    	assertTrue(job.hasParameter(parC.getReference()));
    	assertTrue(parC==job.getParameter(parC.getReference()));
    	
    	// Replace value of existing one
    	NamedData newParA = new NamedData("ParamA", "newValue");
    	SetJobParameter task2 = new SetJobParameter(newParA);
    	task2.applyChange(job);
    	assertTrue(job.hasParameter(newParA.getReference()));
    	assertEquals(newParA.getValue(),
    			job.getParameter(newParA.getReference()).getValue());
    }
    
//------------------------------------------------------------------------------

}
