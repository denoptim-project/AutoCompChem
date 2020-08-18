package autocompchem.datacollections;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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


import java.io.File;
import java.util.ArrayList;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.perception.situation.Situation;
import autocompchem.text.TextBlock;


/**
 * Unit Test for NamedData class 
 * 
 * @author Marco Foscato
 */

public class NamedDataCollectorTest 
{

//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {  
    	NamedDataCollector ndc = new NamedDataCollector();
    	ndc.putNamedData(new NamedData("D", 1.2));
    	
    	IAtomContainer mol = new AtomContainer();
    	Atom atm1 = new Atom("C");
    	Atom atm2 = new Atom("O");
    	mol.addAtom(atm1);
    	mol.addAtom(atm2);
    	ndc.putNamedData(new NamedData("IAC",mol));
    	
    	ListOfDoubles ld = new ListOfDoubles();
    	ld.add(0.1);
    	ld.add(0.2);
    	ndc.putNamedData(new NamedData("LD",ld));
    	
    	NormalMode nm1 = new NormalMode();
    	nm1.append(new Point3d(1,1,1));
    	nm1.append(new Point3d(1.1,1.1,1.1));
    	NormalMode nm2 = new NormalMode();
    	nm2.append(new Point3d(2,2,2));
    	nm2.append(new Point3d(2.1,2.1,2.1));
    	NormalModeSet nms = new NormalModeSet();
    	nms.add(nm1);
    	nms.add(nm2);
    	ndc.putNamedData(new NamedData("NMS",nms));
    	
    	NamedDataCollector cndc = ndc.clone();

    	ndc.putNamedData(new NamedData("D", 2.6));
    	Atom atm3 = new Atom("H");
    	mol.addAtom(atm3);
    	ld.add(2.2);
    	nms.setComponent(2, 0, 0, 3.1);
    	
    	
    	assertTrue((1.2-((Double) cndc.getNamedData("D").getValue()))<0.0001,
    			"Checking cloned NamedData with Double");
    	assertEquals(2, 
    			((IAtomContainer) cndc.getNamedData("IAC").getValue()).getAtomCount(),
    			"Checking cloned IAtomCOntainer");
    	assertEquals(3, 
    			((IAtomContainer) ndc.getNamedData("IAC").getValue()).getAtomCount(),
    			"Checking original IAtomCOntainer");
    	assertEquals(2, 
    			((ListOfDoubles) cndc.getNamedData("LD").getValue()).size(),
    			"Checking cloned list of doubles");
    	assertEquals(3, 
    			((ListOfDoubles) ndc.getNamedData("LD").getValue()).size(),
    			"Checking original list of doubles");
    	assertEquals(3, 
    			((NormalModeSet) ndc.getNamedData("NMS").getValue()).size(),
    			"Checking original list of modess");
    	assertEquals(2, 
    			((NormalModeSet) cndc.getNamedData("NMS").getValue()).size(),
    			"Checking cloned list of modess");
    }

//------------------------------------------------------------------------------

}
