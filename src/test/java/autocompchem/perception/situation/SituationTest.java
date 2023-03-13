package autocompchem.perception.situation;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.perception.circumstance.Circumstance;
import autocompchem.perception.circumstance.CircumstanceConstants;
import autocompchem.perception.circumstance.CountTextMatches;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.run.ActionConstants;
import autocompchem.run.Job;
import autocompchem.run.jobediting.DeleteJobParameter;
import autocompchem.run.jobediting.InheritDirectiveComponent;
import autocompchem.run.jobediting.SetDirectiveComponent;
import autocompchem.run.jobediting.SetJobParameter;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.ActionTest;
import autocompchem.run.jobediting.DataArchivingRule;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;

/**
 * Unit Test for Situation class
 * 
 * @author Marco Foscato
 */

public class SituationTest 
{

    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");
    private final String S = SituationConstants.SEPARATOR;

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------
    
    public static Situation getTestSituation()
    {
        Situation s = new Situation();
        s.setRefName("ERR-2.6");
        s.setDescription("This is a dummy situation for unit testing.");
        s.setLogicalExpression("${v0 && v1}");
        s.setType("ERROR");
        s.addCircumstance(new MatchText("patternToMatch", true,
        		InfoChannelType.OUTPUTFILE));
        s.addCircumstance(new CountTextMatches("counter", 3, 6,
        		InfoChannelType.OUTPUTFILE));
        s.setReaction(ActionTest.getTestAction());
        return s;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	Situation a1 = getTestSituation();
    	Situation a2 = getTestSituation();

        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
        assertTrue(a1.equals(a1));
        assertFalse(a1.equals(null));

        a2 = getTestSituation();
        a2.setDescription("new description"); 
        assertFalse(a1.equals(a2));
        
        a2 = getTestSituation();
        a2.setLogicalExpression("${v0 || v1}");
        assertFalse(a1.equals(a2));
        
        a2 = getTestSituation();
        a2.setRefName("unknown");
        assertFalse(a1.equals(a2));
        
        a2 = getTestSituation();
        a2.setType("Other Error");
        assertFalse(a1.equals(a2));
        
        a2 = getTestSituation();
        a2.addCircumstance(new MatchText("Other pattern", true,
        		InfoChannelType.OUTPUTFILE));
        assertFalse(a1.equals(a2));
        
        a2 = getTestSituation();
        a2.setReaction(new Action(ActionType.SKIP, ActionObject.FOCUSJOB));
        assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testJSONRoundTrip() throws Exception
    {
         Situation original = new Situation();
         original.addCircumstance(new MatchText("patternToMatch", true, 
                 InfoChannelType.OUTPUTFILE));
         original.addCircumstance(new CountTextMatches("counter", 3, 6, 
                 InfoChannelType.OUTPUTFILE));
         original.setReaction(ActionTest.getTestAction());
         
         Gson writer = ACCJson.getWriter();
         Gson reader = ACCJson.getReader();
          
         String json = writer.toJson(original);
          
         //TODO-gg del
         System.out.println(original.getClass().getName()+": "+json);
          
         Situation fromJson = reader.fromJson(json, Situation.class);
         assertEquals(original, fromJson);
    }

//-----------------------------------------------------------------------------
    
    @Test
    public void testIsOccurring() throws Exception
    {
        Situation sit = new Situation();
        sit.addCircumstance(new Circumstance(InfoChannelType.ANY));
        sit.addCircumstance(new Circumstance(InfoChannelType.LOGFEED));
        sit.addCircumstance(new Circumstance(InfoChannelType.OUTPUTFILE));
        sit.addCircumstance(new Circumstance(InfoChannelType.INPUTFILE));
        sit.addCircumstance(new Circumstance(InfoChannelType.ENVIRONMENT));

        ArrayList<Boolean> fingerprint0 = new ArrayList<Boolean>();
        fingerprint0.add(true);
        fingerprint0.add(true);
        fingerprint0.add(true);
        fingerprint0.add(true);
        fingerprint0.add(true);

        assertEquals(true, sit.isOccurring(fingerprint0), "lack of expression");
        
        ArrayList<Boolean> fingerprint = new ArrayList<Boolean>();
        fingerprint.add(true);
        fingerprint.add(false);
        fingerprint.add(true);
        fingerprint.add(true);
        fingerprint.add(true);

        sit.setLogicalExpression("${v0}");
        assertEquals(true,sit.isOccurring(fingerprint),"single true");

        sit.setLogicalExpression("${v1}");
        assertEquals(false,sit.isOccurring(fingerprint),"single false");

        sit.setLogicalExpression("${v0 && !v1}");
        assertEquals(true,sit.isOccurring(fingerprint),".AND. and .NOT.");

        sit.setLogicalExpression("${v0 && !v1 && v2 && v3 && v4}");
        assertEquals(true,sit.isOccurring(fingerprint),"five .AND.");

        sit.setLogicalExpression("${v0 || v1}");
        assertEquals(true,sit.isOccurring(fingerprint),".OR.");

        sit.setLogicalExpression("${v0 && (4 > 2)}");
        assertEquals(true,sit.isOccurring(fingerprint),
        		"mixing numerical and boolean");
    }

//------------------------------------------------------------------------------

}
