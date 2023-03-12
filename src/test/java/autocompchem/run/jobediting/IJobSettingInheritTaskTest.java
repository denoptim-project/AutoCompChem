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

public class IJobSettingInheritTaskTest 
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
    	
    	IJobSettingsInheritTask act = new InheritDirectiveComponent(
    			DirComponentAddress.fromString("*:*|Dir:DirName"));
    	String json = writer.toJson(act);
    	IJobSettingsInheritTask fromJson = reader.fromJson(json, 
    			IJobSettingsInheritTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new InheritJobParameter("paramToInherit");
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobSettingsInheritTask.class);
    	assertEquals(act, fromJson);
    }
    
//------------------------------------------------------------------------------

}
