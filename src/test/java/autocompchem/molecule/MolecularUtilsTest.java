package autocompchem.molecule;

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

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.atom.AtomUtils;
import autocompchem.geometry.DistanceMatrix;

/**
 * Unit Test for MolecularUtils
 * 
 * @author Marco Foscato
 */

public class MolecularUtilsTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------
	
	@Test
	public void testMakeSimpleCopyWithAtomTags() throws Exception
	{
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("Ru"));
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new PseudoAtom("Xx")); 
    	
    	IAtomContainer tagged = MolecularUtils.makeSimpleCopyWithAtomTags(mol);
    	
    	assertEquals(mol.getAtomCount(),tagged.getAtomCount());
    	for (int iAtm=0; iAtm<mol.getAtomCount(); iAtm++)
    	{
    		IAtom atm = tagged.getAtom(iAtm);
    		IAtom origAtm = mol.getAtom(iAtm);
    		// NB: so far there is only one format for atom tags, so we can
    		// hard-code the expected tag.
    		String expectedTag = AtomUtils.getSymbolOrLabel(origAtm)+(iAtm+1);
    		assertEquals(expectedTag, AtomUtils.getSymbolOrLabel(atm));
    	}
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testElementalSymbols() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("Ru"));
    	//NB: with cdk version > 1.5(?) it is not possible to have custom 
    	// elemental symbols like "Xx" of "Du"
    	mol.addAtom(new PseudoAtom());
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new PseudoAtom("Xx"));    	
    	
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("H"),"Does have H");
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("C"),"Does have C");
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("Ru"),"Does have Ru");
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("R"),"Does have R");
        assertFalse(MolecularUtils.getElementalSymbols(mol, 
        		true).contains("R"),"Does not have R");
    }
//------------------------------------------------------------------------------

	@Test
	public void testMinInterelementalBondDistance() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("C",new Point3d(0,0,0.0)));
    	mol.addAtom(new Atom("C",new Point3d(1.0,0,0)));
    	mol.addAtom(new Atom("C",new Point3d(2.0,0,0)));
    	mol.addAtom(new Atom("Cl",new Point3d(0,-2.0,0)));
    	mol.addAtom(new Atom("H",new Point3d(0,0,1.1)));
    	mol.addAtom(new Atom("H",new Point3d(0,0,1.2)));
		
    	double trsh = 0.00001;
        assertTrue(Math.abs(MolecularUtils.getMinInterelementalDistance(mol, 
        	"C","H") - 1.1) < trsh,"Min Interatomic distance H-C");
        assertTrue(Math.abs(MolecularUtils.getMinInterelementalDistance(mol,
        		"C","Cl") - 2.0) < trsh,"Min Interatomic distance H-C");
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testInteratormicDistanceMatrix() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("C",new Point3d(0,0,0.0)));
    	mol.addAtom(new Atom("C",new Point3d(0,3.0,0)));
    	mol.addAtom(new Atom("C",new Point3d(4.0,0,0)));
    	
    	DistanceMatrix dm = MolecularUtils.getInteratormicDistanceMatrix(mol);
    	
    	double trsh = 0.00001;
        assertTrue(Math.abs(dm.get(0, 1) - 3.0) < trsh,"Distance 1-2");
        assertTrue(Math.abs(dm.get(0, 2) - 4.0) < trsh,"Distance 1-2");
        assertTrue(Math.abs(dm.get(1, 2) - 5.0) < trsh,"Distance 1-2");
    }

//------------------------------------------------------------------------------

}
