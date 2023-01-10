package autocompchem.io.jsonableatomcontainer;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;


public class JSONableIAtomContainerTest 
{
  
	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    /**
     * @return an atom container for testing purposes.
     */
    public static IAtomContainer getTestIAtomContainer()
    {
    	IAtomContainer iac = chemBuilder.newAtomContainer();
    	IAtom a = chemBuilder.newAtom();
    	a.setSymbol("O");
        a.setPoint3d(new Point3d(-2.0, -1.2, 0.3));
        iac.addAtom(a);
    	IAtom a1 = chemBuilder.newAtom();
    	a1.setSymbol("C");
        a1.setPoint3d(new Point3d(0.0, 1.2, 2.3));
        iac.addAtom(a1);
    	IAtom a2 = chemBuilder.newAtom();
    	a2.setSymbol("H");
        a2.setPoint3d(new Point3d(2.0, 3.2, 5.3));
        iac.addAtom(a2);
    	IAtom a3 = chemBuilder.newAtom();
    	a3.setSymbol("Mo");
        a3.setPoint3d(new Point3d(4.0, 5.2, 7.3));
        iac.addAtom(a3);
    	IAtom a4 = chemBuilder.newAtom();
    	a4.setSymbol("H");
        a4.setPoint3d(new Point3d(6.0, 7.2, 9.3));
        iac.addAtom(a4);
        iac.addBond(0, 1, IBond.Order.SINGLE);
        iac.addBond(3, 1, IBond.Order.SINGLE);
        iac.addBond(4, 2, IBond.Order.DOUBLE);
        iac.addBond(1, 2, IBond.Order.SINGLE);
    	return iac;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return an atom container for testing purposes.
     */
    public static JSONableIAtomContainer getTestJSONableIAtomContainer()
    {
    	return new JSONableIAtomContainer(getTestIAtomContainer());
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	JSONableIAtomContainer aA = getTestJSONableIAtomContainer();
    	JSONableIAtomContainer aB = getTestJSONableIAtomContainer();
    	
    	assertTrue(aA.equals(aA));
    	assertTrue(aA.equals(aB));
    	assertTrue(aB.equals(aA));
    	
    	aB.getAtom(0).setSymbol("W");
    	assertFalse(aA.equals(aB));
    	
    	aB = getTestJSONableIAtomContainer();
    	aB.getAtom(0).getPoint3d().x = 12345678.90;
    	assertFalse(aA.equals(aB));
    }
    	
//------------------------------------------------------------------------------

}
