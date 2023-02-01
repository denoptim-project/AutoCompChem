package autocompchem.chemsoftware;

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
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.run.jobediting.Action;
import autocompchem.worker.TaskID;

/**
 * Unit tests for directive component address
 */

public class DirComponentAddressTest 
{
	
//------------------------------------------------------------------------------

    public DirComponentAddress getTestAddress()
    {
    	DirComponentAddress address = new DirComponentAddress();
    	address.addStep(new DirComponentTypeAndName("Lev0Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	address.addStep(new DirComponentTypeAndName("Lev1Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	address.addStep(new DirComponentTypeAndName("Lev2Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	address.addStep(new DirComponentTypeAndName("KeyRef", 
    			DirectiveComponentType.KEYWORD));
    	return address;
    }

//------------------------------------------------------------------------------

    @Test
    public void testAddStep() throws Exception
    {
    	DirComponentAddress address = new DirComponentAddress();
    	address.addStep(new DirComponentTypeAndName("Lev0Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	assertEquals(1, address.size());

    	address.addStep("Lev0Dir", DirectiveComponentType.DIRECTIVE);
    	assertEquals(2, address.size());
    	
    	address.addStep("Lev0Dir", "Dir");
    	assertEquals(3, address.size());
    	
    	boolean found = false;
    	try
    	{
    		address.addStep("Lev0Dir", "invalid");
    	} catch (IllegalArgumentException e) {
    		if (e.getMessage().contains("cannot be converted to"))
    			found = true;
    	}
    	assertTrue(found);
    	assertEquals(3, address.size());  	
    }
      
//------------------------------------------------------------------------------

    @Test
    public void testToString() throws Exception
    {
    	String s = getTestAddress().toString();
    	assertEquals("Dir:Lev0Dir|Dir:Lev1Dir|Dir:Lev2Dir|Key:KeyRef",s);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	DirComponentAddress d1 = getTestAddress();
    	DirComponentAddress d2 = getTestAddress();

    	assertTrue(d1.equals(d1));
    	assertTrue(d1.equals(d2));
    	assertTrue(d2.equals(d1));
    	assertFalse(d1.equals(null));
    	
    	d2 = new DirComponentAddress();
    	d2.addStep(new DirComponentTypeAndName("Lev0Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	d2.addStep(new DirComponentTypeAndName("Lev1Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	d2.addStep(new DirComponentTypeAndName("KeyRef", 
    			DirectiveComponentType.KEYWORD));
    	assertFalse(d1.equals(d2));

    	d2 = new DirComponentAddress();
    	d2.addStep(new DirComponentTypeAndName("Lev0Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	d2.addStep(new DirComponentTypeAndName("Lev1Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	d2.addStep(new DirComponentTypeAndName("Lev2Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	d2.addStep(new DirComponentTypeAndName("KeyRef", 
    			DirectiveComponentType.DIRECTIVEDATA));
    	assertFalse(d1.equals(d2));
    	
    	d1 = new DirComponentAddress();
    	d1.addStep(new DirComponentTypeAndName("Lev0Dir", 
    			DirectiveComponentType.DIRECTIVE));
    	d2 = new DirComponentAddress();
    	d2.addStep("Lev0Dir", DirectiveComponentType.DIRECTIVE);
    	assertTrue(d1.equals(d2));
    	
    	d2 = new DirComponentAddress();
    	d2.addStep("Lev0Dir", "Dir");
    	assertTrue(d1.equals(d2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	DirComponentAddress act = getTestAddress();
    	String json = writer.toJson(act);
    	DirComponentAddress fromJson = reader.fromJson(json, 
    			DirComponentAddress.class);
    	assertEquals(act,fromJson);
    }
    
//------------------------------------------------------------------------------

}
