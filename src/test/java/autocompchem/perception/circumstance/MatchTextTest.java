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
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;


public class MatchTextTest 
{
 
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	MatchText a1 = new MatchText("pattern", true, InfoChannelType.LOGFEED);
    	MatchText a2 = new MatchText("pattern", true, InfoChannelType.LOGFEED);

    	assertTrue(a1.equals(a2));
    	assertTrue(a2.equals(a1));
    	assertTrue(a1.equals(a1));
    	assertFalse(a1.equals(null));
    	
    	a2 = new MatchText("different", true, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));

    	a2 = new MatchText("pattern", false, InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));
    	
    	a2 = new MatchText("pattern", true, InfoChannelType.OUTPUTFILE);
    	assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	MatchText original = new MatchText("pattern", true, 
    			InfoChannelType.LOGFEED);
    	String json = writer.toJson(original);
    	
    	ICircumstance fromJson = reader.fromJson(json, ICircumstance.class);
    	assertEquals(original, fromJson);
    	
    	MatchText fromJson2 = reader.fromJson(json, MatchText.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

}
