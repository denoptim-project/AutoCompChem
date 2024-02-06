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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

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
