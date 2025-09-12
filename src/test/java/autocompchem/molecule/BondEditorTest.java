package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.molecule.connectivity.BondEditingRule;
import autocompchem.smarts.SMARTS;

/**
 * Unit Test for {@link BondEditor}.
 * 
 * @author Marco Foscato
 */

public class BondEditorTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------
	
	/**
	 * 0 1 2 3 4 5  6
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
	public void testEditBonds() throws Exception
	{
    	IAtomContainer mol = getTestMol();
    	
    	Map<String,BondEditingRule> bondEditingrules = 
    			new HashMap<String,BondEditingRule>();
    	bondEditingrules.put("R0", new BondEditingRule(
			 new SMARTS[] {new SMARTS("C#C")},
			 null,
			 null,
			 true,
			 0));
    	bondEditingrules.put("R1", new BondEditingRule(
   			 new SMARTS[] {new SMARTS("C-C")},
   			 null,
   			 IBond.Stereo.UP_INVERTED,
   			 false,
   			 1));
    	bondEditingrules.put("R2", new BondEditingRule(
  			 new SMARTS[] {new SMARTS("[Ru]~[#8]")},
  			 IBond.Order.SINGLE,
  			 null,
  			 false,
  			 2));
    	bondEditingrules.put("R3", new BondEditingRule(
 			 new SMARTS[] {new SMARTS("[#8]"), new SMARTS("[$(C-C#C)]")},
 			 IBond.Order.QUADRUPLE,
 			 null,
 			 false,
 			 2));
    	
    	BondEditor.editBonds(mol, bondEditingrules);
    	
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
    	
    	
    	mol = getTestMol();
    	bondEditingrules = new HashMap<String,BondEditingRule>();
    	bondEditingrules.put("R0", new BondEditingRule(
			 new int[] {1, 2},
			 null,
			 null,
			 true,
			 0));
    	bondEditingrules.put("R1", new BondEditingRule(
   			 new int[] {2, 3},
   			 null,
   			 IBond.Stereo.UP_INVERTED,
   			 false,
   			 1));
    	bondEditingrules.put("R2", new BondEditingRule(
  			 new int[] {6, 5},
  			 IBond.Order.SINGLE,
  			 null,
  			 false,
  			 2));
    	bondEditingrules.put("R3", new BondEditingRule(
 			 new int[] {3, 6},
 			 IBond.Order.SINGLE,
 			 null,
 			 false,
 			 2));
    	
    	BondEditor.editBonds(mol, bondEditingrules);
    	
    	assertEquals(5, mol.getBondCount());
    	assertEquals(1, mol.getConnectedBondsList(mol.getAtom(1)).size());
    	assertFalse(mol.getConnectedAtomsList(mol.getAtom(1)).contains(
    			mol.getAtom(2)));
    	assertEquals(2, mol.getConnectedBondsList(mol.getAtom(6)).size());
    	assertTrue(mol.getConnectedAtomsList(mol.getAtom(6)).contains(
    			mol.getAtom(3)));
    	assertEquals(IBond.Order.SINGLE, 
    			mol.getBond(mol.getAtom(6), mol.getAtom(5)).getOrder());
    	 
	}

//------------------------------------------------------------------------------

}
