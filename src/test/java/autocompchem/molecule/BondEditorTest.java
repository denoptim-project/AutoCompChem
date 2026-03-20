package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
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

		List<AtomTupleMatchingRule> rules = new ArrayList<AtomTupleMatchingRule>();
		rules.add(new AtomTupleMatchingRule("R0", new SMARTS[] {new SMARTS("C#C")}, 
			null, Set.of(BondEditor.KEYREMOVE)));
		rules.add(new AtomTupleMatchingRule("R2", new SMARTS[] {new SMARTS("[Ru]~[#8]")}, 
			Map.of(BondEditor.KEYORDER, IBond.Order.SINGLE.toString()), null));
		rules.add(new AtomTupleMatchingRule("R3", new SMARTS[] {new SMARTS("[#8]"), new SMARTS("[$(C-C#C)]")}, 
		    // Do it like this to test the case-sensitivity of the attribute value.
			Map.of(BondEditor.KEYORDER, "quaDruPle"), null));
    	
    	BondEditor.editBonds(rules, mol);
    	
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

		rules.clear();
		rules.add(new AtomTupleMatchingRule("R0", new int[] {1, 2}, 
			null, Set.of(BondEditor.KEYREMOVE)));
		rules.add(new AtomTupleMatchingRule("R2", new int[] {6, 5}, 
			Map.of(BondEditor.KEYORDER, IBond.Order.SINGLE.toString()), null));
		rules.add(new AtomTupleMatchingRule("R3", new int[] {3, 6}, 
			Map.of(BondEditor.KEYORDER, IBond.Order.QUADRUPLE.toString()), null));

		BondEditor.editBonds(rules, mol);

		assertEquals(5, mol.getBondCount());
    	
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
