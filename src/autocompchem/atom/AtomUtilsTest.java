package autocompchem.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;

/**
 * Unit Test for atom utilities
 * 
 * @author Marco Foscato
 */

public class AtomUtilsTest 
{

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

}
