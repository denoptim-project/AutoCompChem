package autocompchem.worker;


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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for the {@link Task}
 * 
 * @author Marco Foscato
 */

public class TaskTest 
{
//-----------------------------------------------------------------------------
    
    @Test
    public void testMake() throws Exception
    {
    	String casedString = "teST001";
    	Task tA = Task.make(casedString, true);
    	Task tB = Task.make("TEst001");
    	Task tC = Task.make(" TEst001");
    	Task tD = Task.make("test001 ");
    	assertSame(tA, tB);
    	assertSame(tA, tC);
    	assertSame(tA, tD);
    	
    	assertEquals(casedString, tA.casedID);
    	assertEquals(casedString, tB.casedID);
    	assertEquals(casedString, tC.casedID);
    	assertEquals(casedString, tD.casedID);
    	
    	assertTrue(tA.testOnly);
    }
    
//-----------------------------------------------------------------------------
      
    @Test
    public void testGetExistingOrMake() throws Exception
    {
    	Task tA = Task.getExistingOrMake("t2b", true, false, false);
    	assertNull(tA);
    	tA = Task.getExistingOrMake("t2b", true, true, false);
     	assertNotNull(tA);
    	assertSame(tA,Task.getExistingOrMake("t2b", true, false, false));
    	assertSame(tA,Task.getExistingOrMake("t2b", true, true, false));
    	assertSame(tA,Task.getExistingOrMake("t2b", true, true, true));
    	assertSame(tA,Task.getExistingOrMake("t2b", true, false, true));
    	
    	assertNull(Task.getExisting("notExisting"));
    	assertNotNull(Task.getExisting("t2b"));
    	assertSame(tA,Task.getExisting("t2b"));
    }
    
//-----------------------------------------------------------------------------
    
    /*
     * Remember that the list of registered tasks is static, so the content of
     * the list depends on which tests we have already run when we run this one
     */
    @Test
    public void testGetRegisteredTasks() throws Exception
    {
    	Task tA = Task.make("testListA", true);
    	Task tB = Task.make("testListB", true);
    	Task tC = Task.make("testListC", true);
    	Task tD = Task.make("testListD", true);
    	
    	List<Task> list = Task.getRegisteredTasks();
    	assertTrue(list.size() > 3);
    	boolean foundA = false;
    	boolean foundB = false;
    	boolean foundC = false;
    	boolean foundD = false;
    	for (Task registeredTask : list)
    	{
    		if (registeredTask.equals(tA)) {
    			foundA = true;
    		}
    		if (registeredTask.equals(tB)) {
    			foundB = true;
    		}
    		if (registeredTask.equals(tC)) {
    			foundC = true;
    		}
    		if (registeredTask.equals(tD)) {
    			foundD = true;
    		}
    	}
    	assertTrue(foundA);
    	assertTrue(foundB);
    	assertTrue(foundC);
    	assertTrue(foundD);
    	
    	// The list should be sorted, so we check the order as well
    	assertTrue(list.indexOf(tA)<list.indexOf(tB));
    	assertTrue(list.indexOf(tB)<list.indexOf(tC));
    	assertTrue(list.indexOf(tC)<list.indexOf(tD));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	Task tA = Task.make("test001");
    	Task tB = tA;
    	Task tC = Task.make("test002");
    	assertEquals(tA, tB);
    	assertSame(tA, tB);
    	assertTrue(tA.equals(tB));
    	assertTrue(tB.equals(tA));
    	assertFalse(tA.equals(tC));
    	assertFalse(tC.equals(tA));
    }
    
//------------------------------------------------------------------------------

}
