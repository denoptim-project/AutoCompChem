package autocompchem.molecule.conformation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.molecule.conformation.ConformationalCoordinate.ConformationalCoordType;

public class ConformationalCoordinateTest 
{

//------------------------------------------------------------------------------

	/**
	 * Created a dummy set of constraints filled with non-sense but plausible 
	 * values.
	 * @return a dummy set of constraints.
	 */
    public static ConformationalCoordinate getTestConformationalCoordinate()
    {
    	ConformationalCoordinate cs = new ConformationalCoordinate(
    			new int[] {0, 1, 2, 3});
    	cs.setNumAtoms(22);
    	return cs;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ConformationalCoordinate c1 = getTestConformationalCoordinate();
    	ConformationalCoordinate c2 = getTestConformationalCoordinate();
    	
    	assertTrue(c1.equals(c2));
    	assertTrue(c2.equals(c1));
    	assertTrue(c1.equals(c1));
    	assertFalse(c1.equals(null));
    	
    	c2 = getTestConformationalCoordinate();
    	c2.setFold(-3);
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestConformationalCoordinate();
    	c2.setType(ConformationalCoordType.FLIP);
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestConformationalCoordinate();
    	c2.setNumAtoms(-10); //nonsense but ok for test
    	assertFalse(c1.equals(c2));
    	
    	ConformationalCoordinate c3 = new ConformationalCoordinate(
    			new int[] {0, 1, 2, 4});
    	assertFalse(c1.equals(c3));
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	ConformationalCoordinate c1 = getTestConformationalCoordinate();
    	ConformationalCoordinate cl1 = c1.clone();
    	assertTrue(c1.equals(cl1));
    	assertFalse(c1 == cl1);
    	
    	c1.setType(ConformationalCoordType.FLIP); //nonsense, but just for test
    	assertFalse(c1.equals(cl1));
    	
    	c1 = getTestConformationalCoordinate();
    	assertTrue(c1.equals(cl1));
    	c1.setFold(22);
    	assertFalse(c1.equals(cl1));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	ConformationalCoordinate original = getTestConformationalCoordinate();
    	
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
	    String json = writer.toJson(original);
	    ConformationalCoordinate fromJson = reader.fromJson(json, 
	    		ConformationalCoordinate.class);
	    assertEquals(original, fromJson);

	    ConformationalCoordinate clone = original.clone();
	    assertEquals(clone, fromJson);
    }
    
//------------------------------------------------------------------------------

}
