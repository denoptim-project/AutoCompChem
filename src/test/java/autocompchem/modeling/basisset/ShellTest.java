package autocompchem.modeling.basisset;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;


public class ShellTest 
{
  
//------------------------------------------------------------------------------

    /**
     * @return a shell that is filled with non-sense values.
     */
    public static Shell getTestShell()
    {
    	Shell p = new Shell("P");
    	p.add(new Primitive("P", 1, Arrays.asList(1.2, 3.4, 5.6), 0.5678, 1, 2));
    	p.add(new Primitive("P", 2, 55.66, 0.089, 2, 3));
    	p.add(new Primitive("P", 3, Arrays.asList(-0.05), 0.456, 7, 8));
    	
    	return p;
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	Shell sA = getTestShell();
    	Shell sB = getTestShell();
    	
    	assertTrue(sA.equals(sA));
    	assertTrue(sA.equals(sB));
    	assertTrue(sB.equals(sA));
    	
    	sB.setType("blabla");
    	assertFalse(sA.equals(sB));
    	
    	sB = getTestShell();
    	sB.setScaleFact(123);
    	assertFalse(sA.equals(sB));

    	sB = getTestShell();
    	sB.getPrimitives().get(0).setAngularMomentum(123456);
    	assertFalse(sA.equals(sB));

    	sB = getTestShell();
    	sB.add(new Primitive());
    	assertFalse(sA.equals(sB));
    }
    	
//------------------------------------------------------------------------------

}
