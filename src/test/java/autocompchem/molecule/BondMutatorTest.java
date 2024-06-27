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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.modeling.AtomSpecificStringGenerator;
import autocompchem.molecule.AtomContainerInputProcessor.MultiGeomMode;
import autocompchem.run.Job;
import autocompchem.worker.DummyWorker;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * Unit Test for {@link BondMutator}.
 * 
 * @author Marco Foscato
 */

public class BondMutatorTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------
	
	/**
	 * 
	 * H-C#C-C O=Ru=O
	 * 
	 * @return a container with the above structure but no coordinates.
	 */
	public IAtomContainer getTestMol()
	{
		IAtomContainer molA = chemBuilder.newAtomContainer();
    	molA.addAtom(new Atom("H"));
    	molA.addAtom(new Atom("C"));
    	molA.addAtom(new Atom("C"));
    	molA.addAtom(new Atom("C"));
    	molA.addAtom(new Atom("O"));
    	molA.addAtom(new Atom("Ru"));
    	molA.addAtom(new Atom("O"));

    	molA.addBond(0, 1, IBond.Order.SINGLE);
    	molA.addBond(1, 2, IBond.Order.TRIPLE);
    	molA.addBond(2, 3, IBond.Order.SINGLE);
    	molA.addBond(4, 5, IBond.Order.DOUBLE);
    	molA.addBond(5, 6, IBond.Order.DOUBLE);
    	
    	return molA;
	}
	
//------------------------------------------------------------------------------
	
	@Test
	public void testMutateBond() throws Exception
	{
    	IAtomContainer mol = getTestMol();
    	
    	Map<String, String> smarts = new HashMap<String, String>();
    	smarts.put("triple bond", "C#C");
    	smarts.put("CC", "C-C");
    	smarts.put("RuO", "[Ru]~[#8]");
    	smarts.put("newBond", "[#8] [$(C-C#C)]");
    	
    	Map<String, Object> newFeatures = new HashMap<String, Object>();
    	newFeatures.put("triple bond", "remove");
    	newFeatures.put("CC", IBond.Stereo.UP_INVERTED);
    	newFeatures.put("RuO", IBond.Order.SINGLE);
    	newFeatures.put("newBond", IBond.Order.QUADRUPLE);
    	
    	BondMutator.editBonds(mol, smarts, newFeatures);
    	
    	assertEquals(6, mol.getBondCount());
    	assertEquals(1, mol.getConnectedBondsList(mol.getAtom(1)).size());
    	assertFalse(mol.getConnectedAtomsList(mol.getAtom(1)).contains(
    			mol.getAtom(2)));
    	assertEquals(IBond.Order.SINGLE, 
    			mol.getBond(mol.getAtom(4), mol.getAtom(5)).getOrder());
    	assertEquals(IBond.Order.SINGLE, 
    			mol.getBond(mol.getAtom(6), mol.getAtom(5)).getOrder());
    	assertEquals(IBond.Order.QUADRUPLE, 
    			mol.getBond(mol.getAtom(3), mol.getAtom(4)).getOrder());
    	assertEquals(IBond.Order.QUADRUPLE, 
    			mol.getBond(mol.getAtom(3), mol.getAtom(6)).getOrder());
	}

//------------------------------------------------------------------------------

}
