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

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.molecule.connectivity.NearestNeighborMap;

public class AnnotatedAtomTupleTest 
{

    private IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

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

    @Test
    public void testNeighborsDetection() throws Exception
    {
		IAtomContainer mol = builder.newAtomContainer();
		String[] elements = new String[]{"C", "O", "N", "P", "H"};
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = builder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(new Point3d(0.0, 0.0, Double.valueOf(i)));
            mol.addAtom(atom);
            if (i>0 && i<4)
                mol.addBond(i-1, i, IBond.Order.SINGLE);
        }
        mol.addBond(2, 4, IBond.Order.SINGLE);
        
        /*
         *         H4
         *        /
         * C0-O1-N2-P3
         * 
         */
        
        AnnotatedAtomTuple tuple = new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(1)), mol);
        //NB: arguments of areNeighbors() are list indexes (not atom indexes!)
        assertTrue(tuple.areNeighbors(0, 1)); 
        
        tuple = new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(2)), mol);
        assertFalse(tuple.areNeighbors(0, 1)); 
        
        tuple = new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(1), mol.getAtom(0)), mol);
        assertTrue(tuple.areNeighbors(0, 1)); 
        
        tuple = new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(1), mol.getAtom(4)), mol);
        assertFalse(tuple.areNeighbors(0, 1)); 
        
        tuple = new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(1), mol.getAtom(2), mol.getAtom(3), 
        		mol.getAtom(4)), mol);
        assertTrue(tuple.areNeighbors(0, 1)); 
        assertTrue(tuple.areNeighbors(1, 2));
        assertTrue(tuple.areNeighbors(2, 3));
        assertTrue(tuple.areNeighbors(2, 4)); 
        assertFalse(tuple.areNeighbors(0, 2)); 
        assertFalse(tuple.areNeighbors(0, 3));
        assertFalse(tuple.areNeighbors(3, 1)); 
        assertFalse(tuple.areNeighbors(3, 0)); 
    }
    
//------------------------------------------------------------------------------

}
