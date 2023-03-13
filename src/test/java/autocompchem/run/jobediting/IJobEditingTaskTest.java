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

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;

public class IJobEditingTaskTest 
{
    
//------------------------------------------------------------------------------

	/**
	 * We do this test for the interface because we want to check if a
	 * collection of instances implementing the interface is deserialized
	 * to get the proper types.
	 */
    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	IJobEditingTask act = new SetDirectiveComponent("*:*|Dir:DirName", 
    			new Keyword("KeyName", false, 1.234));
    	String json = writer.toJson(act);
    	IJobEditingTask fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new SetJobParameter(new NamedData("ParamToSet", "valueOfParam"));
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new DeleteJobParameter("NameOfParamToRemove");
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new DeleteDirectiveComponent(
    			DirComponentAddress.fromString("*:*|Dir:DirName"));
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	// Check case-insensitivity of enum strings
    	json = json.replaceAll(JobEditType.REMOVE_DIRECTIVE.toString(),
    			JobEditType.REMOVE_DIRECTIVE.toString().toLowerCase());
    	
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    }
    
//------------------------------------------------------------------------------

}
