package autocompchem.modeling.atomtuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.molecule.connectivity.NearestNeighborMap;

public class AnnotatedAtomTupleTest 
{

//------------------------------------------------------------------------------

	/**
	 * Return a tuple with this connectivity fingerprint:
	 * <pre>
	 *   1-2-3-4
	 *     \  \
	 *      5--6
	 * </pre>
	 * 
	 * @return an object good for testing, but otherwise meaningless.
	 */
    public static AnnotatedAtomTuple getTestAnnotatedAtomTuple()
    {
    	Set<String> booleanAttributes = new HashSet<String>();
    	booleanAttributes.add("AttA".toUpperCase());
    	booleanAttributes.add("AttB".toUpperCase());
    	Map<String, String> valuedAttributes = new HashMap<String, String>();
    	valuedAttributes.put("AttC".toUpperCase(), "valueC valueCC");
    	valuedAttributes.put("AttD".toUpperCase(), "valueD");
    	valuedAttributes.put("AttE".toUpperCase(), "");
    	NearestNeighborMap ct = new NearestNeighborMap();
    	ct.addNeighborningRelation(1, new ArrayList<Integer>(
    			Arrays.asList(2)));
    	ct.addNeighborningRelation(2, new ArrayList<Integer>(
    			Arrays.asList(3,5)));
    	ct.addNeighborningRelation(3, new ArrayList<Integer>(
    			Arrays.asList(4,6)));
    	ct.addNeighborningRelation(5, new ArrayList<Integer>(
    			Arrays.asList(6)));
    	AnnotatedAtomTuple aat = new AnnotatedAtomTuple(new int[] {1,2,3,4,5,6},
    			booleanAttributes, valuedAttributes, ct, 12);
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
    	
    	c2 = getTestAnnotatedAtomTuple();
    	c2.getNeighboringRelations().getNbrsId(2).add(6);
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestAnnotatedAtomTuple();
    	c2.getNeighboringRelations().addNeighborningRelation(2, new int[] {6});
    	assertFalse(c1.equals(c2));

    	c2 = getTestAnnotatedAtomTuple();
    	c2.getNeighboringRelations().clear();
    	assertFalse(c1.equals(c2));

    	c2 = getTestAnnotatedAtomTuple();
    	c2.setNumAtoms(3);
    	assertFalse(c1.equals(c2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	AnnotatedAtomTuple original = getTestAnnotatedAtomTuple();
    	AnnotatedAtomTuple cloned = original.clone();
    	assertEquals(original, cloned);
    	
    	cloned.setIndexAt(0, -10);
    	cloned.setIndexAt(1, -1);
    	cloned.setIndexAt(2, -2);
    	cloned.setIndexAt(3, -3);
    	for (String key : cloned.getValuedAttributeKeys())
    	{
    		cloned.setValueOfAttribute(key, "mod");
    	}
    	cloned.setValuelessAttribute("newValueless");

    	assertFalse(original.equals(cloned));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	AnnotatedAtomTuple original = getTestAnnotatedAtomTuple();
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
	    String json = writer.toJson(original);
	    AnnotatedAtomTuple fromJson = reader.fromJson(json, 
	    		AnnotatedAtomTuple.class);
	    assertEquals(original, fromJson);

    	AnnotatedAtomTuple clone = original.clone();
	    assertEquals(clone, fromJson);
    }
    
//------------------------------------------------------------------------------

}
