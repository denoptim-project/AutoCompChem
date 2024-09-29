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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.IDirectiveComponent;
import autocompchem.wiro.chem.Keyword;

public class InheritDirectiveComponentTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	InheritDirectiveComponent tA = new InheritDirectiveComponent(
    			DirComponentAddress.fromString("Dir:First|Dir:Second"));
    	InheritDirectiveComponent tB = new InheritDirectiveComponent(
    			DirComponentAddress.fromString("Dir:First|Dir:Second"));

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new InheritDirectiveComponent(
    			DirComponentAddress.fromString("Dir:Other|Dir:Second"));
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	InheritDirectiveComponent original = new InheritDirectiveComponent(
    			DirComponentAddress.fromString("Dir:First|Dir:Second"));
    	String json = writer.toJson(original);
    	InheritDirectiveComponent fromJson = reader.fromJson(json, 
    			InheritDirectiveComponent.class);
    	assertEquals(original, fromJson);
    	
    	IJobSettingsInheritTask fromJson2 = reader.fromJson(json, 
    			IJobSettingsInheritTask.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges() throws Exception
    {
    	CompChemJob srcJob = new CompChemJob();
    	Directive dA = new Directive("dA");
    	dA.addKeyword(new Keyword("KeyAA", false, 1.234));
    	dA.addKeyword(new Keyword("KeyAB", false, "value"));
    	srcJob.addDirective(dA);
    	Directive dB = new Directive("dB");
    	dB.addKeyword(new Keyword("KeyBA", false, 4.56));
    	dB.addKeyword(new Keyword("KeyBB", false, "other"));
    	dB.addDirectiveData(new DirectiveData("DirDat", new ArrayList<String>(
    			Arrays.asList("1","two"))));
    	Directive dC = new Directive("dC");
    	dC.addKeyword(new Keyword("KeyC", false, "further")); // this value is used below
    	dB.addSubDirective(dC);
    	Directive dD = new Directive("dD");
    	dD.addKeyword(new Keyword("KeyD0", false, "D"));
    	dD.addKeyword(new Keyword("KeyD", false, "DDD")); // this value is used below
    	dB.addSubDirective(dD);
    	srcJob.addDirective(dB);

    	Directive dE = new Directive("dE");
    	dE.addKeyword(new Keyword("KeyE1", false, "EE"));
    	srcJob.addDirective(dE);
    	
    	Directive dE1 = new Directive("dE");
    	dE1.addKeyword(new Keyword("KeyE2", false, "EE"));
    	srcJob.addDirective(dE1);
    	
    	Directive dE2 = new Directive("dE");
    	dE2.addKeyword(new Keyword("KeyE3", false, "EE"));
    	srcJob.addDirective(dE2);
    	
        Directive dF = new Directive("dF");
        dF.addKeyword(new Keyword("KeyF1", false, "FF"));
        srcJob.addDirective(dF);

        Directive dF1 = new Directive("dF");
        dF1.addKeyword(new Keyword("KeyF2", false, "FF"));
        srcJob.addDirective(dF1);

        Directive dF2 = new Directive("dF");
        dF2.addKeyword(new Keyword("KeyF3", false, "FF"));
        srcJob.addDirective(dF2);
    	
    	
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
    	 *  dF
    	 *  
    	 *  dF
    	 *  
    	 *  dF
    	 */
    	
    	CompChemJob dstJob = new CompChemJob();
    	Directive dA2 = new Directive("dA");
    	dA2.addKeyword(new Keyword("KeyAA", false, 2.345));
    	dA2.addKeyword(new Keyword("KeyAB", false, "different"));
    	dstJob.addDirective(dA2);
    	Directive dF3 = new Directive("dF");
        dF3.addKeyword(new Keyword("KeyF2", false, "OriginalFromDest"));
        dstJob.addDirective(dF3);
    	
    	// Source does not have the component required
    	DirComponentAddress adrs0 = DirComponentAddress.fromString(
    			"Dir:dA|Dir:nonexisting");
    	InheritDirectiveComponent task0 = new InheritDirectiveComponent(adrs0);
    	task0.inheritSettings(srcJob, dstJob);
    	assertEquals(0, srcJob.getDirectiveComponents(adrs0).size());
    	assertEquals(0, dstJob.getDirectiveComponents(adrs0).size());
    	
    	// Inherit an new Keyword
    	DirComponentAddress adrs1 = DirComponentAddress.fromString(
    			"Dir:dB|Dir:dC|Key:KeyC");
    	InheritDirectiveComponent task1 = new InheritDirectiveComponent(adrs1);
    	task1.inheritSettings(srcJob, dstJob);
    	List<IDirectiveComponent> found = srcJob.getDirectiveComponents(adrs1);
    	assertEquals(1, found.size());
    	assertEquals("further", ((Keyword) found.get(0)).getValueAsString());
    	found = dstJob.getDirectiveComponents(adrs1);
    	assertEquals(1, found.size());
    	assertEquals("further", ((Keyword) found.get(0)).getValueAsString());
    	
    	// Inherit an new DirectiveData
    	DirComponentAddress adrs2 = DirComponentAddress.fromString(
    			"Dir:dB|Dat:DirDat");
    	InheritDirectiveComponent task2 = new InheritDirectiveComponent(adrs2);
    	task2.inheritSettings(srcJob, dstJob);
    	found = srcJob.getDirectiveComponents(adrs2);
    	assertEquals(1, found.size());
    	assertTrue(found.get(0) instanceof DirectiveData);
    	assertEquals("two", 
    			((DirectiveData) found.get(0)).getValueAsLines().get(1));
    	found = dstJob.getDirectiveComponents(adrs2);
    	assertEquals(1, found.size());
    	assertTrue(found.get(0) instanceof DirectiveData);
    	assertEquals("two", 
    			((DirectiveData) found.get(0)).getValueAsLines().get(1));
    	
    	// Inherit an new Directive
    	DirComponentAddress adrs3 = DirComponentAddress.fromString(
    			"Dir:dB|Dir:dD");
    	InheritDirectiveComponent task3 = new InheritDirectiveComponent(adrs3);
    	task3.inheritSettings(srcJob, dstJob);
    	found = srcJob.getDirectiveComponents(adrs3);
    	assertEquals(1, found.size());
    	found = dstJob.getDirectiveComponents(adrs3);
    	assertEquals(1, found.size());
    	DirComponentAddress adrsOfKey = DirComponentAddress.fromString(
    			"Dir:dB|Dir:dD|Key:KeyD");
    	found = srcJob.getDirectiveComponents(adrsOfKey);
    	assertEquals(1, found.size());
    	assertEquals("DDD", ((Keyword) found.get(0)).getValueAsString());
    	found = dstJob.getDirectiveComponents(adrsOfKey);
    	assertEquals(1, found.size());
    	assertEquals("DDD", ((Keyword) found.get(0)).getValueAsString());
    	
    	// Add beside existing one
    	DirComponentAddress adrs4 = DirComponentAddress.fromString(
    			"Dir:dA|Key:KeyAB");
    	InheritDirectiveComponent task4 = new InheritDirectiveComponent(adrs4);
    	task4.inheritSettings(srcJob, dstJob);
    	found = srcJob.getDirectiveComponents(adrs4);
    	assertEquals(1, found.size());
    	assertEquals("value", ((Keyword) found.get(0)).getValueAsString());
    	found = dstJob.getDirectiveComponents(adrs4);
    	assertEquals(2, found.size());
    	assertEquals("different", ((Keyword) found.get(0)).getValueAsString());
    	assertEquals("value", ((Keyword) found.get(1)).getValueAsString());
    	
    	// Multiple components match query address
    	DirComponentAddress adrs5 = DirComponentAddress.fromString(
    			"Dir:dE");
    	InheritDirectiveComponent task5 = new InheritDirectiveComponent(adrs5);
    	task5.inheritSettings(srcJob, dstJob);
    	found = srcJob.getDirectiveComponents(adrs5);
    	assertEquals(3, found.size());
    	found = dstJob.getDirectiveComponents(adrs5);
    	assertEquals(3, found.size());
    	
    	// Multiple components match query address and one in destination job
    	DirComponentAddress adrs6 = DirComponentAddress.fromString(
    			"Dir:dF");
    	InheritDirectiveComponent task6 = new InheritDirectiveComponent(adrs6);
    	task6.inheritSettings(srcJob, dstJob);
    	found = srcJob.getDirectiveComponents(adrs6);
    	assertEquals(3, found.size());
    	found = dstJob.getDirectiveComponents(adrs6);
    	// NB: we ADD three directives 'dF' and one is already there.
    	assertEquals(4, found.size());
    	
    	// Multiple components with same name on both jobs
    	CompChemJob srcJobB = new CompChemJob();
    	Directive dAB = new Directive("dA");
    	dAB.addKeyword(new Keyword("KeyA", false, 0));
    	dAB.addKeyword(new Keyword("KeyA", false, 1));
    	dAB.addKeyword(new Keyword("KeyA", false, 2));
    	dAB.addKeyword(new Keyword("KeyA", false, 3));
    	srcJobB.addDirective(dAB);
    	
    	CompChemJob dstJobB = new CompChemJob();
    	Directive dA2B = new Directive("dA");
    	dA2B.addKeyword(new Keyword("KeyA", false, -1));
    	dstJobB.addDirective(dA2B);
    	
    	DirComponentAddress adrs7 = DirComponentAddress.fromString(
    			"Dir:dA|Key:KeyA");
    	InheritDirectiveComponent task7 = new InheritDirectiveComponent(adrs7);
    	assertEquals(4, srcJobB.getDirectiveComponents(adrs7).size());
    	assertEquals(1, dstJobB.getDirectiveComponents(adrs7).size());
    	
    	task7.inheritSettings(srcJobB, dstJobB);
    	
    	assertEquals(4, srcJobB.getDirectiveComponents(adrs7).size());
    	Set<String> expectedSrcVals = new HashSet<>(Arrays.asList("0","1","2","3"));
    	Set<String> actualSrcVals = srcJobB.getDirectiveComponents(adrs7).stream()
    		.map(k -> ((Keyword)k).getValueAsString())
    		.collect(Collectors.toSet());
    	assertEquals(expectedSrcVals, actualSrcVals);

    	assertEquals(5, dstJobB.getDirectiveComponents(adrs7).size());
    	Set<String> expectedDstVals = new HashSet<>(expectedSrcVals);
    	expectedDstVals.add("-1");
    	Set<String> actualDstVals = dstJobB.getDirectiveComponents(adrs7).stream()
    		.map(k -> ((Keyword)k).getValueAsString())
    		.collect(Collectors.toSet());
    	assertEquals(expectedDstVals, actualDstVals);
    }
    
//------------------------------------------------------------------------------

}
