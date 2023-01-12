package autocompchem.modeling.atomtuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.Constraint.ConstraintType;

public class AnnotatedAtomTupleTest 
{

//------------------------------------------------------------------------------

	/**
	 * @return an object good for testing, but otherwise meaningless.
	 */
    public static AnnotatedAtomTuple getTestAnnotatedAtomTuple()
    {
    	Set<String> booleanAttributes = new HashSet<String>();
    	booleanAttributes.add("AttA");
    	booleanAttributes.add("AttB");
    	Map<String, String> valuedAttributes = new HashMap<String, String>();
    	valuedAttributes.put("AttC", "valueC valueCC");
    	valuedAttributes.put("AttD", "valueD");
    	valuedAttributes.put("AttE", "");
    	AnnotatedAtomTuple aat = new AnnotatedAtomTuple(new int[] {1,2,3,4,5,6},
    			booleanAttributes, valuedAttributes);
    	return aat;
    }
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	AnnotatedAtomTuple c1 = getTestAnnotatedAtomTuple();
    	AnnotatedAtomTuple c2 = getTestAnnotatedAtomTuple();
    	
    	assertTrue(c1.equals(c2));
    	assertTrue(c2.equals(c1));
    	assertTrue(c1.equals(c1));
    	assertFalse(c1.equals(null));
    	
    	c2.removeValuelessAttribute("AttA");
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestAnnotatedAtomTuple();
    	c2.setValuelessAttribute("AttX");
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestAnnotatedAtomTuple();
    	c2.setValueOfAttribute("AttC", "other");
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestAnnotatedAtomTuple();
    	c2.setValueOfAttribute("AttX", "other");
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestAnnotatedAtomTuple();
    	c2.getAtomIDs().set(0, -1);
    	assertFalse(c1.equals(c2));
    	
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testCompareTo() throws Exception
    {
    	AnnotatedAtomTuple c1 = getTestAnnotatedAtomTuple();
    	AnnotatedAtomTuple c2 = getTestAnnotatedAtomTuple();
    	
    	assertEquals(0, c1.compareTo(c2));

    	c2.getAtomIDs().set(0, -1);
    	assertEquals(1, c1.compareTo(c2));
    	
    	c2.getAtomIDs().set(0, 100);
    	assertEquals(-1, c1.compareTo(c2));
    }
    
//------------------------------------------------------------------------------

}
