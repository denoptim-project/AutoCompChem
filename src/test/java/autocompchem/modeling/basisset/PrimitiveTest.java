package autocompchem.modeling.basisset;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;


public class PrimitiveTest 
{
  
//------------------------------------------------------------------------------

    /**
     * @return a shell that is filled with non-sense values.
     */
    public static Primitive getTestPrimitive()
    {
    	return new Primitive("P", 1, Arrays.asList(1.2, 3.4), 0.5678, 1, 2);
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	Primitive pA = getTestPrimitive();
    	Primitive pB = getTestPrimitive();
    	
    	assertTrue(pA.equals(pA));
    	assertTrue(pA.equals(pB));
    	assertTrue(pB.equals(pA));
    	
    	pB.setAngularMomentum(123456);
    	assertFalse(pA.equals(pB));

    	pB = getTestPrimitive();
    	pB.setCoefficient(123456);
    	assertFalse(pA.equals(pB));
    	
    	pB = getTestPrimitive();
    	pB.setCoefficients(new ArrayList<Double>(Arrays.asList(1.2, 3.4)));
    	assertTrue(pA.equals(pB));
    	
    	pB = getTestPrimitive();
    	pB.setCoefficients(new ArrayList<Double>(Arrays.asList(3.4, 5.6)));
    	assertFalse(pA.equals(pB));
    	
    	pB = getTestPrimitive();
    	pB.setCoeffPrecision(999);
    	assertFalse(pA.equals(pB));
    	
    	pB = getTestPrimitive();
    	pB.setExponent(123456);
    	assertFalse(pA.equals(pB));
    	
    	pB = getTestPrimitive();
    	pB.setExpPrecision(999);
    	assertFalse(pA.equals(pB));
    	
    	pB = getTestPrimitive();
    	pB.setType("blabla");
    	assertFalse(pA.equals(pB));
    }
    	
//------------------------------------------------------------------------------

}
