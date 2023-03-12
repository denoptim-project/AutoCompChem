package autocompchem.perception.circumstance;


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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.DataArchivingRule.Type;


public class CountTextMatchesTest 
{
 
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	CountTextMatches a1 = new CountTextMatches("pattern", 2, 6, 
    			InfoChannelType.LOGFEED);
    	CountTextMatches a2 = new CountTextMatches("pattern", 2, 6, 
    			InfoChannelType.LOGFEED);

    	assertTrue(a1.equals(a2));
    	assertTrue(a2.equals(a1));
    	assertTrue(a1.equals(a1));
    	assertFalse(a1.equals(null));
    	
    	a2 = new CountTextMatches("different", 2, 6, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));

    	a2 = new CountTextMatches("pattern", 3, 6, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));
    	
    	a2 = new CountTextMatches("pattern", 2, 3, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));

    	a2 = new CountTextMatches("pattern", 2, 6, InfoChannelType.OUTPUTFILE);
    	assertFalse(a1.equals(a2));
    	
    	
    	a1 = new CountTextMatches("pattern", 10, true, InfoChannelType.LOGFEED);
    	a2 = new CountTextMatches("pattern", 10, true, InfoChannelType.LOGFEED);
    	
    	assertTrue(a1.equals(a2));
    	assertTrue(a2.equals(a1));
    	assertTrue(a1.equals(a1));
    	assertFalse(a1.equals(null));
    	
    	a2 = new CountTextMatches("pattern", 10, false, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));
    	
    	a2 = new CountTextMatches("pattern", 20, true, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	CountTextMatches original = new CountTextMatches("pattern", 2, 6, 
    			InfoChannelType.LOGFEED);
    	String json = writer.toJson(original);
    	
    	CountTextMatches fromJson = reader.fromJson(json, CountTextMatches.class);
    	assertEquals(original, fromJson);
    }
    
//------------------------------------------------------------------------------

}
