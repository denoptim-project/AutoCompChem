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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.Keyword;

public class DeleteDirectiveComponentTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	DeleteDirectiveComponent tA = new DeleteDirectiveComponent(
    			DirComponentAddress.fromString("Dir:First|Dir:Second"));
    	DeleteDirectiveComponent tB = new DeleteDirectiveComponent(
    			DirComponentAddress.fromString("Dir:First|Dir:Second"));

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new DeleteDirectiveComponent(
    			DirComponentAddress.fromString("Dir:Other|Dir:Second"));
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	DeleteDirectiveComponent original = new DeleteDirectiveComponent(
    			DirComponentAddress.fromString("Dir:First|Dir:Second"));
    	String json = writer.toJson(original);
    	DeleteDirectiveComponent fromJson = reader.fromJson(json, 
    			DeleteDirectiveComponent.class);
    	assertEquals(original, fromJson);

    	IJobEditingTask fromJson2 = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges() throws Exception
    {
    	CompChemJob ccj = new CompChemJob();
    	Directive dA = new Directive("dA");
    	dA.addKeyword(new Keyword("KeyAA", false, 1.234));
    	dA.addKeyword(new Keyword("KeyAB", false, "value"));
    	ccj.addDirective(dA);
    	Directive dB = new Directive("dB");
    	dB.addKeyword(new Keyword("KeyBA", false, 4.56));
    	dB.addKeyword(new Keyword("KeyBB", false, "other"));
    	dB.addDirectiveData(new DirectiveData("DirDat", new ArrayList<String>(
    			Arrays.asList("1","two"))));
    	Directive dC = new Directive("dC");
    	dC.addKeyword(new Keyword("KeyC", false, "further")); 
    	dB.addSubDirective(dC);
    	Directive dD = new Directive("dD");
    	dD.addKeyword(new Keyword("KeyD0", false, "D"));
    	dD.addKeyword(new Keyword("KeyD", false, "DDD"));
    	dB.addSubDirective(dD);
    	ccj.addDirective(dB);

    	Directive dE = new Directive("dE");
    	dE.addKeyword(new Keyword("KeyE1", false, "EE"));
    	ccj.addDirective(dE);
    	
    	Directive dE1 = new Directive("dE");
    	dE1.addKeyword(new Keyword("KeyE2", false, "EE"));
    	ccj.addDirective(dE1);
    	
    	Directive dE2 = new Directive("dE");
    	dE2.addKeyword(new Keyword("KeyE3", false, "EE"));
    	ccj.addDirective(dE2);
    	
    	/* This is the structure of the directives in the source job
    	 * 
    	 *  dA
    	 *  
    	 *  dB -- dC
    	 *    \
    	 *     -- dD
    	 *     
    	 *  dE
    	 *  
    	 *  dE
    	 *  
    	 *  dE
    	 *  
    	 */
    	
    	// Source does not have the component required
    	DirComponentAddress adrs = DirComponentAddress.fromString(
    			"Dir:dA|Dir:nonexisting");
    	DeleteDirectiveComponent task = new DeleteDirectiveComponent(adrs);
        
    	assertEquals(0, ccj.getDirectiveComponents(adrs).size());
    	task.applyChange(ccj);
    	assertEquals(0, ccj.getDirectiveComponents(adrs).size());
    	
    	// Delete a Keyword
    	adrs = DirComponentAddress.fromString(
    			"Dir:dA|Key:KeyAB");
    	task = new DeleteDirectiveComponent(adrs);
    	assertEquals(1, ccj.getDirectiveComponents(adrs).size());
    	task.applyChange(ccj);
    	assertEquals(0, ccj.getDirectiveComponents(adrs).size());
    	
    	// Delete a DirectiveData
    	adrs = DirComponentAddress.fromString(
    			"Dir:dB|Dat:DirDat");
    	task = new DeleteDirectiveComponent(adrs);
    	assertEquals(1, ccj.getDirectiveComponents(adrs).size());
    	task.applyChange(ccj);
    	assertEquals(0, ccj.getDirectiveComponents(adrs).size());
    	
    	// Delete a single Directive
    	adrs = DirComponentAddress.fromString(
    			"Dir:dB|Dir:dD");
    	task = new DeleteDirectiveComponent(adrs);
    	assertEquals(1, ccj.getDirectiveComponents(adrs).size());
    	task.applyChange(ccj);
    	assertEquals(0, ccj.getDirectiveComponents(adrs).size());
    	
    	// Delete multiple components match query address
    	adrs = DirComponentAddress.fromString(
    			"Dir:dE");
    	task = new DeleteDirectiveComponent(adrs);
    	assertEquals(3, ccj.getDirectiveComponents(adrs).size());
    	task.applyChange(ccj);
    	assertEquals(0, ccj.getDirectiveComponents(adrs).size());
    }
    
//------------------------------------------------------------------------------

}
