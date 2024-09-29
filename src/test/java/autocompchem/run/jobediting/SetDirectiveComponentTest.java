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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.IDirectiveComponent;
import autocompchem.wiro.chem.Keyword;

public class SetDirectiveComponentTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	SetDirectiveComponent tA = new SetDirectiveComponent(
    			"Dir:First|Dir:Second", 
    			new Keyword("KeyName", true, "KeyValue"));
    	SetDirectiveComponent tB = new SetDirectiveComponent(
    			"Dir:First|Dir:Second", 
    			new Keyword("KeyName", true, "KeyValue"));

    	assertTrue(tA.equals(tA));
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(null));
    	
    	tB = new SetDirectiveComponent("Dir:First|Dir:Third", 
    			new Keyword("KeyName", true, "KeyValue"));
    	assertFalse(tA.equals(tB));
    	
    	tB = new SetDirectiveComponent("Dir:First|Dir:Second", 
    			new Keyword("different", true, "KeyValue"));
    	assertFalse(tA.equals(tB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	SetDirectiveComponent original = new SetDirectiveComponent(
    			"Dir:First|Dir:Second", 
    			new Keyword("KeyName", true, "KeyValue"));
    	String json = writer.toJson(original);
    	SetDirectiveComponent fromJson = reader.fromJson(json, 
    			SetDirectiveComponent.class);
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
    	dA.addKeyword(new Keyword("KeyAA", false, "asd"));
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
    	// Replace existing keyword
    	Keyword key3 = new Keyword("KeyAA", true, "NewValue");
    	SetDirectiveComponent task3 = new SetDirectiveComponent("Dir:dA", key3);
    	task3.applyChange(job);
    	List<IDirectiveComponent> found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dA|Key:KeyAA"));
    	assertEquals(1, found.size());
    	assertTrue(key3==found.get(0));
    	assertEquals(key3.getValue(), ((Keyword)found.get(0)).getValue());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testApplyChanges_editExisting() throws Exception
    {
    	CompChemJob job = new CompChemJob();
    	job.setParameter("ParamA", "valueA");
    	job.setParameter("ParamB", "valueB");
    	Directive dA = new Directive("dA");
    	dA.addKeyword(new Keyword("KeyAA", false, 1.234));
    	job.addDirective(dA);
    	Directive dB = new Directive("dB");
    	dB.addKeyword(new Keyword("KeyBA", false, 4.56));
    	dB.addKeyword(new Keyword("KeyBA", false, 8.56));
    	dB.addKeyword(new Keyword("KeyBA", false, 12.56));
    	Directive dC = new Directive("dC");
    	dC.addKeyword(new Keyword("KeyC", false, "further"));
    	dC.addKeyword(new Keyword("KeyCB", false, "2.86MB%"));
    	dC.addKeyword(new Keyword("KeyCB", false, "$$2.86"));
    	dB.addSubDirective(dC);
    	job.addDirective(dB);
    	
    	/* This is the structure of the directives
    	 * 
    	 *  dA
    	 *  
    	 *  dB -- dC
    	 *  
    	 */
    	
    	Keyword keyA = new Keyword("KeyAA", true, "${2*x + 10}");
    	SetDirectiveComponent taskA = new SetDirectiveComponent("Dir:dA", keyA);
    	taskA.applyChange(job);
    	List<IDirectiveComponent> found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dA|Key:KeyAA"));
    	assertEquals(1, found.size());
    	assertTrue(NumberUtils.closeEnough(12.468, Double.parseDouble(
    			((Keyword)found.get(0)).getValueAsString())));
    	
    	Keyword keyB = new Keyword("KeyBA", true, "${x - 4.56}");
    	SetDirectiveComponent taskB = new SetDirectiveComponent("Dir:dB", keyB);
    	taskB.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dB|Key:KeyBA"));
    	assertEquals(3, found.size());
    	assertTrue(NumberUtils.closeEnough(0.0, Double.parseDouble(
    			((Keyword)found.get(0)).getValueAsString())));
    	assertTrue(NumberUtils.closeEnough(4.0, Double.parseDouble(
    			((Keyword)found.get(1)).getValueAsString())));
    	assertTrue(NumberUtils.closeEnough(8.0, Double.parseDouble(
    			((Keyword)found.get(2)).getValueAsString())));
    	
    	Keyword keyC = new Keyword("KeyCB", true, "${x + 1.14}");
    	SetDirectiveComponent taskC = new SetDirectiveComponent("Dir:dB|Dir:dC",
    			keyC);
    	taskC.applyChange(job);
    	found = job.getDirectiveComponents(
    			DirComponentAddress.fromString("Dir:dB|Dir:dC|Key:KeyCB"));
    	assertEquals(2, found.size());
    	boolean foundA = false;
    	boolean foundB = false;
    	for (IDirectiveComponent dc : found)
    	{
    		String str = ((Keyword) dc).getValueAsString();
    		if (str.equals("4.00MB%"))
    			foundA = true;
    		if (str.equals("$$4.00"))
    			foundB = true;
    	}
    	assertTrue(foundA);
    	assertTrue(foundB);
    }
    
//------------------------------------------------------------------------------

}
