package autocompchem.io.jsonableatomcontainer;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IBond;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;


public class LWBondTest 
{
  
//------------------------------------------------------------------------------

    /**
     * @return a bond that is filled with non-sense values.
     */
    public static LWBond getTestLWBond()
    {
    	return new LWBond(23, 45, IBond.Order.TRIPLE);
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	LWBond bA = getTestLWBond();
    	LWBond bB = getTestLWBond();
    	
    	assertTrue(bA.equals(bA));
    	assertTrue(bA.equals(bB));
    	assertTrue(bB.equals(bA));
    	
    	bB.atomIds[0] = 1;
    	assertFalse(bA.equals(bB));

    	bB = getTestLWBond();
    	bB.atomIds[1] = 1;
    	assertFalse(bA.equals(bB));
    	
    	bB = getTestLWBond();
    	bB.atomIds[1] = bA.atomIds[1];
    	assertTrue(bA.equals(bB));
    	
    	bB = getTestLWBond();
    	bB.bo = IBond.Order.DOUBLE;
    	assertFalse(bA.equals(bB));
    }
    	
//------------------------------------------------------------------------------

}
