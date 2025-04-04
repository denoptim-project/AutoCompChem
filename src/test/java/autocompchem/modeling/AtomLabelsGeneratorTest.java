package autocompchem.modeling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

public class AtomLabelsGeneratorTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------

    @Test
    public void testGenerateAtomicNumberLabels() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("Ru"));
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new PseudoAtom("Xx")); 
    	
    	List<String> labs = AtomLabelsGenerator.generateAtomicNumberLabels(mol);
    	
    	assertEquals(mol.getAtomCount(), labs.size());
    	
    	List<String> expected = new ArrayList<String>(Arrays.asList(
    			"1", "6", "8", "44", "0", "0"));
    	for (int iAtm=0; iAtm<labs.size(); iAtm++)
    	{
    		assertEquals(expected.get(iAtm), labs.get(iAtm));
    	}
    }
    
	
//------------------------------------------------------------------------------

    @Test
    public void testGenerateElementBasedLabels() throws Exception
    {
    	IAtomContainer mol = chemBuilder.newAtomContainer();
      	mol.addAtom(new Atom("H"));
      	mol.addAtom(new Atom("C"));
      	mol.addAtom(new Atom("O"));
      	mol.addAtom(new Atom("H"));
      	mol.addAtom(new PseudoAtom("Du"));
      	mol.addAtom(new Atom("C")); 
      	
      	List<String> labs = AtomLabelsGenerator.generateElementBasedLabels(mol,
      			true); // Zero-based indexing
      	
      	assertEquals(mol.getAtomCount(), labs.size());
      	
      	List<String> expected = new ArrayList<String>(Arrays.asList(
      			"H0", "C0", "O0", "H1", "Du0", "C1"));
      	for (int iAtm=0; iAtm<labs.size(); iAtm++)
      	{
      		assertEquals(expected.get(iAtm), labs.get(iAtm));
      	}
    } 	
    
//------------------------------------------------------------------------------

    @Test
    public void testGenerateIndexBasedLabels() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new Atom("C")); 
    	
    	List<String> labs = AtomLabelsGenerator.generateIndexBasedLabels(mol,
    			false); // 1-based indexing
    	
    	assertEquals(mol.getAtomCount(), labs.size());
    	
    	List<String> expected = new ArrayList<String>(Arrays.asList(
    			"H1", "C2", "O3", "H4", "Du5", "C6"));
    	for (int iAtm=0; iAtm<labs.size(); iAtm++)
    	{
    		assertEquals(expected.get(iAtm), labs.get(iAtm));
    	}
    }
//------------------------------------------------------------------------------

}
