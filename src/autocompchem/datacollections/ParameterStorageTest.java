package autocompchem.datacollections;


/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.AtomContainer;

import autocompchem.datacollections.NamedData.NamedDataType;


/**
 * Unit Test for Parameter class 
 * 
 * @author Marco Foscato
 */

public class ParameterStorageTest 
{
    
//------------------------------------------------------------------------------

    @Test
    public void testGetAllParameters() throws Exception
    {

        Parameter p1 = new Parameter("KEY1", NamedDataType.STRING,1.123);      
        Parameter p2 = new Parameter("KEY2", NamedDataType.DOUBLE,1.123);
        Parameter p3 = new Parameter("KEY3", NamedDataType.INTEGER,206);
        
    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter("k1", p1);
    	ps.setParameter("k2", p2);
    	ps.setParameter("k3", p3);
    	
    	NamedData nd1 = new NamedData("ND1", NamedDataType.STRING, "val1");
    	NamedData nd2 = new NamedData("ND2", NamedDataType.INTEGER, "int1");
    	NamedDataCollector dc = (NamedDataCollector) ps;
    	dc.putNamedData("N1", nd1);
    	dc.putNamedData("N2", nd2);
    	
    	assertEquals(5, dc.getAllNamedData().size(), 
    			"Number of items from getAllNamedData");
    	assertEquals(3, ps.getAllParameters().size(), 
    			"Number of items from getAllParameters");
    	
    	Parameter rp1 = ps.getAllParameters().get("k1");
    	Parameter rp2 = ps.getAllParameters().get("k2");
    	Parameter rp3 = ps.getAllParameters().get("k3");
    	
    	assertTrue(rp1.getValueAsObjectSubclass() instanceof String, 
    			"Verify type p1"); 
        assertTrue(rp2.getValueAsObjectSubclass() instanceof Double, 
        		"Verify type p2");
        assertTrue(rp3.getValueAsObjectSubclass() instanceof Integer, 
        		"Verify type p3");

    }

//------------------------------------------------------------------------------

}
