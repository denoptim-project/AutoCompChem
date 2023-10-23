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

import autocompchem.io.ACCJson;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.AppID;

public class InheritJobParameterTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	InheritJobParameter tA = new InheritJobParameter("parName");
    	InheritJobParameter tB = new InheritJobParameter("parName");

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new InheritJobParameter("different");
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	InheritJobParameter original = new InheritJobParameter("parName");
    	String json = writer.toJson(original);
    	InheritJobParameter fromJson = reader.fromJson(json, 
    			InheritJobParameter.class);
    	assertEquals(original, fromJson);
    	
    	IJobSettingsInheritTask fromJson2 = reader.fromJson(json, 
    			IJobSettingsInheritTask.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges() throws Exception
    {
    	Job sourceJob = JobFactory.createJob(AppID.ACC);
    	sourceJob.setParameter("ParamA", "valueA");
    	sourceJob.setParameter("ParamB", "valueB");
    	
    	Job destinationJob = JobFactory.createJob(AppID.ACC);
    	destinationJob.setParameter("ParamC", "valueC");
    	destinationJob.setParameter("ParamB", "oldValueB");
    	
    	// Source does not have the parameter required
    	InheritJobParameter task0 = new InheritJobParameter("NOT_THERE");
    	task0.inheritSettings(sourceJob, destinationJob);
    	assertFalse(sourceJob.hasParameter("NOT_THERE"));
    	assertFalse(destinationJob.hasParameter("NOT_THERE"));
    	
    	// Add new parameter
    	InheritJobParameter task1 = new InheritJobParameter("ParamA");
    	task1.inheritSettings(sourceJob, destinationJob);
    	assertTrue(sourceJob.hasParameter("ParamA"));
    	assertTrue(destinationJob.hasParameter("ParamA"));
    	assertTrue(sourceJob.getParameter("ParamA").equals(
    			destinationJob.getParameter("ParamA")));
    	
    	// Replace value of existing one
    	InheritJobParameter task2 = new InheritJobParameter("ParamB");
    	task2.inheritSettings(sourceJob, destinationJob);
    	assertTrue(sourceJob.hasParameter("ParamB"));
    	assertTrue(destinationJob.hasParameter("ParamB"));
    	assertTrue(sourceJob.getParameter("ParamB").equals(
    			destinationJob.getParameter("ParamB")));
    }
    
//------------------------------------------------------------------------------

}
