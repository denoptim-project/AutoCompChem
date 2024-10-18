package autocompchem.datacollections;


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

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.ACCJson;


/**
 * Unit Test for Parameter class 
 * 
 * @author Marco Foscato
 */

public class ParameterStorageTest 
{
//------------------------------------------------------------------------------

    @Test
    public void testCaseInsensitivity() throws Exception
    {
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter("Key1", "1.123");
    	
    	assertTrue(ps.contains("KEY1"),"Case insensitive contains method (A)");
    	assertTrue(ps.contains("kEy1"),"Case insensitive contains method (B)");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetRefNamesSet() throws Exception
    {
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter("KEY1", "1.123");
    	ps.setParameter("KEY2", 1.123);
    	ps.setParameter("KEY3", 206);
    	
    	assertEquals(3, ps.getRefNamesSet().size());
    	assertTrue(ps.getRefNamesSet().contains("KEY1"));
    	assertTrue(ps.getRefNamesSet().contains("KEY2"));
    	assertTrue(ps.getRefNamesSet().contains("KEY3"));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetParameterOrNull() throws Exception
    {
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter("KEY1", "1.123");
    	ps.setParameter("KEY2", 1.123);
    	ps.setParameter("KEY3", 206);
    	NamedData rp1 = ps.getParameterOrNull("key1");
    	NamedData rp2 = ps.getParameterOrNull("key2");
    	NamedData rp3 = ps.getParameterOrNull("key3");
    	NamedData rp4 = ps.getParameterOrNull("key4");
    	
    	assertTrue(rp1.getValue() instanceof String); 
        assertTrue(rp2.getValue() instanceof Double);
        assertTrue(rp3.getValue() instanceof Integer);
        assertTrue(rp4==null);
    }
    
//------------------------------------------------------------------------------

    public static ParameterStorage getTestParameterStorage()
    {
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter("KEY1", "1.123");
    	ps.setParameter("KEY2", 1.123);
    	ps.setParameter("KEY3", 206);
    	return ps;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ParameterStorage psA = getTestParameterStorage();
    	ParameterStorage psB = getTestParameterStorage();

    	assertTrue(psA.equals(psA));
    	assertTrue(psA.equals(psB));
    	assertTrue(psB.equals(psA));
    	assertFalse(psA.equals(null));
    	
    	psB.setParameter("newParam", null);
    	assertFalse(psA.equals(psB));
    	
    	psB = getTestParameterStorage();
    	psB.getParameter("KEY1").setValue("changed");
    	assertFalse(psA.equals(psB));
    	
    	psB = getTestParameterStorage();
    	psB.getParameter("KEY2").setReference("changed");
    	assertFalse(psA.equals(psB));
    	
    	psB = getTestParameterStorage();
    	psB.removeData("KEY2");
    	assertFalse(psA.equals(psB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	ParameterStorage original = getTestParameterStorage();
    	ParameterStorage cloned = original.clone();
    	assertEquals(original, cloned);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter("KEY1", "1.123");
    	ps.setParameter("KEY2", 1.123);
    	ps.setParameter("KEY3", 206);
    	
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	String jsonStr = writer.toJson(ps);
    	ParameterStorage ps2 = reader.fromJson(jsonStr, ParameterStorage.class);
    	
    	assertEquals(3, ps.getRefNamesSet().size());
    	assertEquals(ps.getRefNamesSet().size(), ps2.getRefNamesSet().size());
    	for (String key : ps.getRefNamesSet())
    	{
    		assertTrue(ps2.contains(key));
    		assertEquals(ps.getParameter(key).getValueAsString(), 
    				ps2.getParameter(key).getValueAsString());
    	}
    }

//------------------------------------------------------------------------------

}
