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
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveComponentType;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.IDirectiveComponent;
import autocompchem.wiro.chem.Keyword;

public class AddDirectiveComponentTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	AddDirectiveComponent tA = new AddDirectiveComponent(
    			"Dir:First|Dir:Second", 
    			new Keyword("KeyName", true, "KeyValue"));
    	AddDirectiveComponent tB = new AddDirectiveComponent(
    			"Dir:First|Dir:Second", 
    			new Keyword("KeyName", true, "KeyValue"));

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new AddDirectiveComponent("Dir:First|Dir:Third", 
    			new Keyword("KeyName", true, "KeyValue"));
    	assertFalse(tA.equals(tB));
    	
    	tB = new AddDirectiveComponent("Dir:First|Dir:Second", 
    			new Keyword("different", true, "KeyValue"));
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	AddDirectiveComponent original = new AddDirectiveComponent(
    			"Dir:First|Dir:Second", 
    			new Keyword("KeyName", true, "KeyValue"));
    	String json = writer.toJson(original);
    	AddDirectiveComponent fromJson = reader.fromJson(json, 
    			AddDirectiveComponent.class);
    	assertEquals(original, fromJson);

    	IJobEditingTask fromJson2 = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(original, fromJson2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges() throws Exception
    {
    	CompChemJob job = new CompChemJob();
    	job.setParameter("ParamA", "valueA");
    	job.setParameter("ParamB", "valueB");
    	Directive dA = new Directive("dA");
    	dA.addKeyword(new Keyword("KeyAA", false, 1.234));
    	dA.addKeyword(new Keyword("KeyAB", false, "value"));
    	job.addDirective(dA);
    	Directive dB = new Directive("dB");
    	dB.addKeyword(new Keyword("KeyBA", false, 4.56));
    	dB.addKeyword(new Keyword("KeyBB", false, "other"));
    	Directive dC = new Directive("dC");
    	dC.addKeyword(new Keyword("KeyC", false, "further"));
    	dB.addSubDirective(dC);
    	job.addDirective(dB);
    	
    	/* This is the structure of the directives
    	 * 
    	 *  dA
    	 *  
    	 *  dB -- dC
    	 *  
    	 */
    	
    	// Add new keyword in existing directive
    	Keyword key = new Keyword("NewKey", true, "KeyValue");
    	AddDirectiveComponent task = new AddDirectiveComponent("Dir:dA", key);
    	task.applyChange(job);
    	List<IDirectiveComponent> found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dA|Key:NewKey"));
    	assertEquals(1,found.size());
    	assertEquals(key,found.get(0));
    	
    	// Add new keyword under non-existing directive structure
    	Keyword key2 = new Keyword("NewKey2", true, "KeyValue2");
    	AddDirectiveComponent task2 = new AddDirectiveComponent("Dir:Z|Dir:ZZ", 
    			key2);
    	task2.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:Z|Dir:ZZ|Key:NewKey2"));
    	assertEquals(1, found.size());
    	assertEquals(key2, found.get(0));
    	
    	// Add beside existing keyword
    	Keyword key3 = new Keyword("KeyAA", true, "NewValue");
    	AddDirectiveComponent task3 = new AddDirectiveComponent("Dir:dA", key3);
    	task3.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dA|Key:KeyAA"));
    	assertEquals(2,found.size());
    	assertTrue(key3==found.get(1));
    	assertFalse(key3==found.get(0));
    	assertEquals(key3.getValue(), ((Keyword)found.get(1)).getValue());
    	
    	// Add new DirectiveData in existing directive
    	DirectiveData dd = new DirectiveData("AddedDD", new ArrayList<String>(
    			Arrays.asList("line One", "line two", "line three")));
    	AddDirectiveComponent task4 = new AddDirectiveComponent("Dir:dA", dd);
    	task4.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dA|"
    					+ DirectiveComponentType.DIRECTIVEDATA.shortString 
    					+ ":AddedDD"));
    	assertEquals(1,found.size());
    	assertEquals(dd, found.get(0));
    	
    	// Add new Directive in existing directive
    	Directive dir = new Directive("AddedDir");
    	dir.addKeyword(new Keyword("123Key", true, 456));
    	dir.addKeyword(new Keyword("Other", false, "bla"));
    	AddDirectiveComponent task5 = new AddDirectiveComponent("Dir:dA", dir);
    	task5.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dA|Dir:AddedDir"));
    	assertEquals(1,found.size());
    	assertEquals(dir, found.get(0));
    	
    	// Add new Directive in existing directive
    	Directive dirRoot = new Directive("NewBaseDir");
    	dirRoot.addKeyword(new Keyword("Somthing", true, 1.23));
    	dirRoot.addKeyword(new Keyword("Else", false, "ribla"));
    	AddDirectiveComponent task6 = new AddDirectiveComponent(".", dirRoot);
    	task6.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:NewBaseDir"));
    	assertEquals(1,found.size());
    	assertEquals(dirRoot, found.get(0));
    }
    
//------------------------------------------------------------------------------

}
