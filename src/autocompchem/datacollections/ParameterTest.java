package autocompchem.datacollections;


import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class ParameterTest 
{
    
//------------------------------------------------------------------------------

    @Test
    public void testParameterType() throws Exception
    {
        Parameter p = new Parameter("KEY", NamedDataType.STRING,"1.123");
        Object o = p.getValue();
        assertTrue(o instanceof String, "Verify String type");
       
        p = new Parameter("KEY", NamedDataType.DOUBLE,1.123);
        o = p.getValue();
        assertTrue(o instanceof Double, "Verify Double type");
        
        p = new Parameter("KEY", NamedDataType.INTEGER,206);
        o = p.getValue();
        assertTrue(o instanceof Integer, "Verify Integer type");
        
        p = new Parameter("KEY", NamedDataType.BOOLEAN,true);
        o = p.getValue();
        assertTrue(o instanceof Boolean, "Verify Boolean type");
        
        IAtomContainer mol = new AtomContainer();
        IAtom atm1 = new Atom("C");
        IAtom atm2 = new Atom("C");
        IAtom atm3 = new Atom("C");
        mol.addAtom(atm1);
        mol.addAtom(atm2);
        mol.addAtom(atm3);
        IBond bnd1 = new Bond(atm1, atm2);
        IBond bnd2 = new Bond(atm1, atm3);
        mol.addBond(bnd1);
        mol.addBond(bnd2);
        
        p = new Parameter("KEY", NamedDataType.IATOMCONTAINER,mol);
        o = p.getValue();
        
        assertTrue(o instanceof IAtomContainer, "Verify IAC type");
        IAtomContainer molObj = (IAtomContainer) o;
        assertEquals(3,molObj.getAtomCount(), "Number of atoms");
        assertEquals(2,molObj.getBondCount(), "Number of bonds");
    }

//------------------------------------------------------------------------------

}
