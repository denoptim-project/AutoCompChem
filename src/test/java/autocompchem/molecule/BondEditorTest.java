package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

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
	public void testEditBonds() throws Exception
	{
    	IAtomContainer mol = getTestMol();
    	
    	Map<String, List<SMARTS>> smarts = new HashMap<String, List<SMARTS>>();
    	smarts.put("triple bond", Arrays.asList(new SMARTS("C#C")));
    	smarts.put("CC", Arrays.asList(new SMARTS("C-C")));
    	smarts.put("RuO", Arrays.asList(new SMARTS("[Ru]~[#8]")));
    	smarts.put("newBond", Arrays.asList(
    			new SMARTS("[#8]"), 
    			new SMARTS("[$(C-C#C)]")));
    	
    	Map<String, Object> newFeatures = new HashMap<String, Object>();
    	newFeatures.put("triple bond", "remove");
    	newFeatures.put("CC", IBond.Stereo.UP_INVERTED);
    	newFeatures.put("RuO", IBond.Order.SINGLE);
    	newFeatures.put("newBond", IBond.Order.QUADRUPLE);
    	
    	BondEditor.editBonds(mol, smarts, newFeatures);
    	
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
