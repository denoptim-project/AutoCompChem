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
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;

public class DataArchivingRuleTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	DataArchivingRule tA = new DataArchivingRule(ArchivingTaskType.COPY,"*blabla*");
    	DataArchivingRule tB = new DataArchivingRule(ArchivingTaskType.COPY,"*blabla*");

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new DataArchivingRule(ArchivingTaskType.MOVE,"*blabla*");
    	assertFalse(tA.equals(tB));
    	
    	tB = new DataArchivingRule(ArchivingTaskType.COPY,"*blablabla*");
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	DataArchivingRule original = new DataArchivingRule(
    			ArchivingTaskType.COPY,"*bla*");
    	String json = writer.toJson(original);
    	
    	DataArchivingRule fromJson = reader.fromJson(json, 
    			DataArchivingRule.class);
    	assertEquals(original, fromJson);
    	
    	// Check case-insensitivity of enum strings
    	json = json.replaceAll(ArchivingTaskType.COPY.toString(),
    			ArchivingTaskType.COPY.toString().toLowerCase());
    	
    	fromJson = reader.fromJson(json, DataArchivingRule.class);
    	assertEquals(original, fromJson);
    }
    
//------------------------------------------------------------------------------

}
