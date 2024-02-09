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

public class AnnotatedAtomTupleListTest 
{
	
//------------------------------------------------------------------------------

    /**
     * @return an object good for testing, but otherwise meaningless.
     */
	public static AnnotatedAtomTupleList getTestAnnotatedAtomTupleList()
	{
	    Set<String> booleanAttributes = new HashSet<String>();
	    booleanAttributes.add("AttA".toUpperCase());
	    booleanAttributes.add("AttB".toUpperCase());
	    Map<String, String> valuedAttributes = new HashMap<String, String>();
	    valuedAttributes.put("AttC".toUpperCase(), "valueC valueC2");
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
	    AnnotatedAtomTuple aatA = new AnnotatedAtomTuple(
	    		new int[] {1,2,3,4,5,6}, 
	    		new ArrayList<String>(Arrays.asList("F","r","i","u","l","i")),
	            booleanAttributes, valuedAttributes, ct, 60);
	
	    Set<String> booleanAttributesB = new HashSet<String>();
	    booleanAttributesB.add("AttBA".toUpperCase());
	    booleanAttributesB.add("AttBB".toUpperCase());
	    Map<String, String> valuedAttributesB = new HashMap<String, String>();
	    valuedAttributesB.put("AttBC".toUpperCase(), "valueBC valueBC2");
	    valuedAttributesB.put("AttBD".toUpperCase(), "valueBD");
	    valuedAttributesB.put("AttBE".toUpperCase(), "B");
	    AnnotatedAtomTuple aatB = new AnnotatedAtomTuple(new int[] {10,22},
	    		null, booleanAttributesB, valuedAttributesB, null, 60);
	
	    Set<String> booleanAttributesC = new HashSet<String>();
	    booleanAttributesC.add("AttCA".toUpperCase());
	    booleanAttributesC.add("AttCB".toUpperCase());
	    Map<String, String> valuedAttributesC = new HashMap<String, String>();
	    valuedAttributesC.put("AttCC".toUpperCase(), "valueCC valueCC2");
	    valuedAttributesC.put("AttCD".toUpperCase(), "valueCD");
	    valuedAttributesC.put("AttCE".toUpperCase(), "C");
	    AnnotatedAtomTuple aatC = new AnnotatedAtomTuple(new int[] {33,44,55},
	    		null, booleanAttributesC, valuedAttributesC, null, 60);
	    AnnotatedAtomTupleList result = new AnnotatedAtomTupleList();
	    result.add(aatA);
	    result.add(aatB);
	    result.add(aatC);
	
	    return result;
}

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {	
    	AnnotatedAtomTupleList s1 = getTestAnnotatedAtomTupleList();
    	AnnotatedAtomTupleList s2 = getTestAnnotatedAtomTupleList();
    	
    	assertTrue(s1.equals(s2));
    	assertTrue(s2.equals(s1));
    	assertTrue(s1.equals(s1));
    	assertFalse(s1.equals(null));    	

    	s2.get(1).removeValuelessAttribute("AttBA");
    	assertFalse(s1.equals(s2));
    	
    	s2 = getTestAnnotatedAtomTupleList();
    	s2.get(2).setValuelessAttribute("newOne");
    	assertFalse(s1.equals(s2));
    	
    	s2 = getTestAnnotatedAtomTupleList();
    	s2.get(0).setValueOfAttribute("myne", "123");
    	assertFalse(s1.equals(s2));
    	
    	s2 = getTestAnnotatedAtomTupleList();
    	s2.get(0).getNeighboringRelations().clear();
    	assertFalse(s1.equals(s2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	AnnotatedAtomTupleList original = getTestAnnotatedAtomTupleList();
    	AnnotatedAtomTupleList cloned = original.clone();
    	assertEquals(original, cloned);
    	
    	cloned.get(0).removeValuelessAttribute("AttA");
    	assertFalse(original.equals(cloned));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	AnnotatedAtomTupleList original = getTestAnnotatedAtomTupleList();
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
	    String json = writer.toJson(original);
	    AnnotatedAtomTupleList fromJson = reader.fromJson(json, 
	    		AnnotatedAtomTupleList.class);
	    assertEquals(original, fromJson);

	    AnnotatedAtomTupleList clone = original.clone();
	    assertEquals(clone, fromJson);
    }
    
//------------------------------------------------------------------------------

}
