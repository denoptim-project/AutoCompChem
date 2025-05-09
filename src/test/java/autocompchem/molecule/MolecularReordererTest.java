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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.atom.AtomUtils;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.io.IOtools;

/**
 * Unit Test for MolecularReordered
 * 
 * @author Marco Foscato
 */

public class MolecularReordererTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();

	
//------------------------------------------------------------------------------

	/*
	 * 
	 *           Cl0
	 *           |
	 *      Br4--Ru3--C1#O2
	 *           ||
	 *           C7
	 *          /  \
	 *        H5    F6
	 * 
	 */
	public IAtomContainer makeTestIAC() throws Exception
	{	
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("Cl")); // 0
    	mol.addAtom(new Atom("C"));  // 1
    	mol.addAtom(new Atom("O"));  // 2
    	mol.addAtom(new Atom("Ru")); // 3
    	mol.addAtom(new Atom("Br")); // 4
    	mol.addAtom(new Atom("H"));  // 5
    	mol.addAtom(new Atom("F"));  // 6
    	mol.addAtom(new Atom("C"));  // 7
    	mol.addBond(5, 7, IBond.Order.SINGLE);
    	mol.addBond(7, 6, IBond.Order.SINGLE);
    	mol.addBond(0, 3, IBond.Order.SINGLE);
    	mol.addBond(1, 2, IBond.Order.TRIPLE);
    	mol.addBond(3, 1, IBond.Order.SINGLE);
    	mol.addBond(3, 7, IBond.Order.DOUBLE);
    	mol.addBond(4, 3, IBond.Order.SINGLE);
    	return mol;
	}
	
//------------------------------------------------------------------------------

	public IAtomContainer makeReorderedTestIAC() throws Exception
	{	
		IAtomContainer mol = makeTestIAC();
    	MolecularReorderer actor = new MolecularReorderer();
    	return actor.reorderContainer(mol);
	}
	
//------------------------------------------------------------------------------
	
	@Test
	public void testReorderContainer() throws Exception
	{
		IAtomContainer mol = makeTestIAC();
    	
    	MolecularReorderer actor = new MolecularReorderer();
    	IAtomContainer reordered = actor.reorderContainer(mol, Arrays.asList(
    			mol.getAtom(2)));
    	
    	String expected = "OCRuBrClCFH";
    	String actual = "";
    	for (IAtom atm : reordered.atoms())
    		actual = actual + atm.getSymbol();
    	
    	assertEquals(expected, actual);
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testGetAtomReorderingMap() throws Exception
    {
		IAtomContainer molA = makeTestIAC();
		// NB: here we rely on the success of the makeReorderedTestIAC which
		// is granted by testReorderContainer
		IAtomContainer molB = makeReorderedTestIAC();
		
		Map<Integer,Integer> map = MolecularReorderer.getAtomReorderingMap(molB);
    	
		assertEquals(molA.getAtomCount(), map.size());
		
    	for (Entry<Integer,Integer> e : map.entrySet())
    	{
    		assertEquals(molA.getAtom(e.getKey()).getSymbol(),
    				molB.getAtom(e.getValue()).getSymbol());
    	}
	}
	
//------------------------------------------------------------------------------

}
