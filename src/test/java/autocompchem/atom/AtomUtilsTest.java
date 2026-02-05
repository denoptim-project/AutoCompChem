package autocompchem.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

/**
 * Unit Test for atom utilities
 * 
 * @author Marco Foscato
 */

public class AtomUtilsTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    @Test
    public void testIdentificationOfDummies() throws Exception
    {
    	IAtom atm = new Atom("C");
    	IAtom du = new PseudoAtom(AtomConstants.DUMMYATMLABEL);
    	IAtom duNonDu = new PseudoAtom("R");
    	IAtom atp = new PseudoAtom(AtomConstants.ATTACHMENTPOINTLABEL);
    	
    	assertTrue(AtomUtils.isAccDummy(du),
    			"PseudoAtom w/ proper label is dummy.");
    	assertTrue(AtomUtils.isAttachmentPoint(atp),
    			"PseudoAtom w/ proper label is attachment point.");
    	assertTrue(!AtomUtils.isAccDummy(duNonDu),
    			"PseudoAtom w/o proper label is NOT dummy.");
    	assertFalse(AtomUtils.isAccDummy(atm),"Is proper atom");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testMakeIAtom() throws Exception
    {
    	IAtom atm = AtomUtils.makeIAtom("C");
    	assertTrue(atm instanceof Atom);
    	assertNull(atm.getPoint2d());
    	assertNull(atm.getPoint3d());
    	
    	atm = AtomUtils.makeIAtom("Dummy");
    	assertTrue(atm instanceof PseudoAtom);
    	assertNull(atm.getPoint2d());
    	assertNull(atm.getPoint3d());
    	
    	Point3d p3d =  new Point3d(1.0, 2.0, 3.0);
    	atm = AtomUtils.makeIAtom("C", p3d);
    	assertTrue(atm instanceof Atom);
    	assertTrue(closeEnough(atm.getPoint3d(),p3d));
    	
    	atm = AtomUtils.makeIAtom("Dummy", new Point3d(p3d));
    	assertTrue(atm instanceof PseudoAtom);
    	assertTrue(closeEnough(atm.getPoint3d(),p3d));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetSymbolOrLabel() throws Exception
    {
    	IAtom atm = new Atom();
    	IAtom atmC = new Atom("C");
    	IAtom du = new PseudoAtom(AtomConstants.DUMMYATMLABEL);
    	IAtom duNonDu = new PseudoAtom();
    	IAtom atp = new PseudoAtom(AtomConstants.ATTACHMENTPOINTLABEL);
    	
    	assertNull(AtomUtils.getSymbolOrLabel(atm),
    			"Return null for atm = new Atom().");
    	assertEquals("C",AtomUtils.getSymbolOrLabel(atmC),
    			"Return symbol for notmal atoms");
    	assertEquals(AtomConstants.DUMMYATMLABEL,AtomUtils.getSymbolOrLabel(du),
    			"Return label dor ACC's dummy atoms.");
    	assertNotNull(AtomUtils.getSymbolOrLabel(duNonDu),
    			"Return symbol for general pseudoatoms.");
    	assertEquals(AtomConstants.ATTACHMENTPOINTLABEL,
    			AtomUtils.getSymbolOrLabel(atp),
    			"Return label for attachment points");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetCoords3d() throws Exception
    {
    	IAtom atm = chemBuilder.newAtom();
    	assertTrue(closeEnough(AtomUtils.getCoords3d(atm), new Point3d(0,0,0)));
    	
    	atm.setPoint2d(new Point2d(1.2, 2.3));
    	assertTrue(closeEnough(AtomUtils.getCoords3d(atm), 
    			new Point3d(1.2, 2.3,0)));
    	
    	atm.setPoint3d(new Point3d(1.2, 2.3, -3.4));
    	assertTrue(closeEnough(AtomUtils.getCoords3d(atm), 
    			new Point3d(1.2, 2.3,  -3.4)));
    }
    
//------------------------------------------------------------------------------
    
    private boolean closeEnough(Point3d pA, Point3d pB)
    {
    	if (pA==null)
    		return false;
    	if (pB==null)
    		return false;
    	return pA.distance(pB) < 0.0002;
    }
    
//------------------------------------------------------------------------------

}
