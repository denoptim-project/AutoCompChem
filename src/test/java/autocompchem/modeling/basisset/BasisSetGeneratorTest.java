package autocompchem.modeling.basisset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;


public class BasisSetGeneratorTest 
{

    private IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
    
//------------------------------------------------------------------------------

    @Test
    public void testAssignBasisSet() throws Exception
    {
        String[] elements = new String[]{"C", "O", "N", "H"};
        IAtomContainer mol = chemBuilder.newAtomContainer();
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            mol.addAtom(atom);
        }
        
        BasisSetGenerator bsg = new BasisSetGenerator();
        bsg.addBSMatchingRule(new BSMatchingRule("rule0", 
        		BasisSetConstants.ATMMATCHBYSYMBOL, "C", 
        		BasisSetConstants.BSSOURCENAME, "6-31+G**"));
        bsg.addBSMatchingRule(new BSMatchingRule("rule1", 
        		BasisSetConstants.ATMMATCHBYSMARTS, "[#7]", 
        		BasisSetConstants.BSSOURCENAME, "LANL2DZ"));
        bsg.addBSMatchingRule(new BSMatchingRule("rule2", 
        		BasisSetConstants.ATMMATCHBYSMARTS, "[#8,#1]", 
        		BasisSetConstants.BSSOURCENAME, "cc-pVTZ"));
        
    	BasisSet bs = bsg.assignBasisSet(mol);
    	
    	assertTrue(bs.hasElement("C"));
    	assertFalse(bs.hasCenter(0,"C"));
    	assertTrue(bs.hasCenter(1,"O"));
    	assertTrue(bs.hasCenter(2,"N"));
    	assertTrue(bs.hasCenter(3,"H"));
    	assertFalse(bs.hasElement("O"));
    	assertFalse(bs.hasElement("N"));
    	assertFalse(bs.hasElement("H"));
    }
    	
//------------------------------------------------------------------------------

}
