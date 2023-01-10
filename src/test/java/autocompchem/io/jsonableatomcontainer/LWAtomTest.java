package autocompchem.io.jsonableatomcontainer;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;


public class LWAtomTest 
{
  
//------------------------------------------------------------------------------

    /**
     * @return an atom that is filled with non-sense values.
     */
    public static LWAtom getTestLWAtom()
    {
    	return new LWAtom("Unl", new Point3d(1.2, 3.4, 5.6));
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	LWAtom aA = getTestLWAtom();
    	LWAtom aB = getTestLWAtom();
    	
    	assertTrue(aA.equals(aA));
    	assertTrue(aA.equals(aB));
    	assertTrue(aB.equals(aA));
    	
    	aB.elSymbol = "W";
    	assertFalse(aA.equals(aB));
    	
    	aB = getTestLWAtom();
    	aB.p3d.x = 123.0;
    	assertFalse(aA.equals(aB));

    	aB = getTestLWAtom();
    	aB.p3d.y = 123.0;
    	assertFalse(aA.equals(aB));

    	aB = getTestLWAtom();
    	aB.p3d.z = 123.0;
    	assertFalse(aA.equals(aB));

    	aB = getTestLWAtom();
    	aB.p3d.z = aA.p3d.z;
    	assertTrue(aA.equals(aB));
    }
    	
//------------------------------------------------------------------------------

}
