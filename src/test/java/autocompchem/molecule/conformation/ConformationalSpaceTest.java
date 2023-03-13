package autocompchem.molecule.conformation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.molecule.conformation.ConformationalCoordinate.ConformationalCoordType;

public class ConformationalSpaceTest 
{

//------------------------------------------------------------------------------

	/**
	 * Created a dummy space filled with non-sense but plausible 
	 * values.
	 */
    public static ConformationalSpace getTestConformationalSpace()
    {

    	ConformationalSpace cs = new ConformationalSpace();
    	ConformationalCoordinate cc1 = new ConformationalCoordinate(
    			new int[] {3});
    	ConformationalCoordinate cc2 = new ConformationalCoordinate(
    			new int[] {0, 1, 2, 3});
    	ConformationalCoordinate cc3 = new ConformationalCoordinate(
    			new int[] {5, 6});
    	ConformationalCoordinate cc4 = new ConformationalCoordinate(
    			new int[] {4});
    	ConformationalCoordinate cc5 = new ConformationalCoordinate(
    			new int[] {1, 0}); 
    	ConformationalCoordinate cc6 = new ConformationalCoordinate(
    			new int[] {7, 8}); 
    	ConformationalCoordinate cc7 = new ConformationalCoordinate(
    			new int[] {9, 8}); 
    	cc1.setFold(2);
    	cc2.setFold(3);
    	cc3.setFold(4);
    	cc4.setFold(2);
    	cc5.setFold(5);
    	cc6.setFold(6);
    	cc7.setFold(7);
    	cs.add(cc1);
    	cs.add(cc2);
    	cs.add(cc3);
    	cs.add(cc4);
    	cs.add(cc5);
    	cs.add(cc6);
    	cs.add(cc7);
    	return cs;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAdditionOfRedudnant() throws Exception
    {
    	ConformationalSpace cs = new ConformationalSpace();
    	ConformationalCoordinate cc1 = new ConformationalCoordinate(
    			new int[] {3});
    	ConformationalCoordinate cc2 = new ConformationalCoordinate(
    			new int[] {0, 1, 2, 3});
    	ConformationalCoordinate cc3 = new ConformationalCoordinate(
    			new int[] {5, 6});
    	ConformationalCoordinate cc4 = new ConformationalCoordinate(
    			new int[] {3}); //redundant!
    	ConformationalCoordinate cc5 = new ConformationalCoordinate(
    			new int[] {3, 2, 1, 0}); //redundant!
    	ConformationalCoordinate cc6 = new ConformationalCoordinate(
    			new int[] {2, 1}); //redundant!
    	ConformationalCoordinate cc7 = new ConformationalCoordinate(
    			new int[] {1, 2}); //redundant!
    	cs.add(cc1);
    	cs.add(cc2);
    	cs.add(cc3);
    	cs.add(cc4);
    	cs.add(cc5);
    	cs.add(cc6);
    	cs.add(cc7);
    	assertEquals(3, cs.size());

    	ConformationalCoordinate cc8 = new ConformationalCoordinate(
    			new int[] {4}); 
    	ConformationalCoordinate cc9 = new ConformationalCoordinate(
    			new int[] {4}); //redundant!
    	cs.add(cc8);
    	cs.add(cc9);
    	assertEquals(4, cs.size());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ConformationalSpace c1 = getTestConformationalSpace();
    	ConformationalSpace c2 = getTestConformationalSpace();
    	
    	assertTrue(c1.equals(c2));
    	assertTrue(c2.equals(c1));
    	assertTrue(c1.equals(c1));
    	assertFalse(c1.equals(null));
    	
    	c2 = getTestConformationalSpace();
    	c2.last().setFold(22);
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestConformationalSpace();
    	c2.first().setType(ConformationalCoordType.TORSION);
    	assertFalse(c1.equals(c2));

    	c2 = getTestConformationalSpace();
    	c2.last().setNumAtoms(200);
    	assertFalse(c1.equals(c2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	ConformationalSpace original = getTestConformationalSpace();
    	ConformationalSpace cloned = original.clone();
      	assertEquals(original, cloned);
      	
      	cloned.first().setIndexAt(0, -10);
      	assertFalse(original.equals(cloned));
      	
      	cloned = original.clone();
      	cloned.last().setFold(22);
      	assertFalse(original.equals(cloned));
      	
      	cloned = original.clone();
      	cloned.first().setType(ConformationalCoordType.TORSION);
      	assertFalse(original.equals(cloned));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	ConformationalSpace original = getTestConformationalSpace();
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
	    String json = writer.toJson(original);
	    ConformationalSpace fromJson = reader.fromJson(json, 
	    		ConformationalSpace.class);
	    assertEquals(original, fromJson);

	    ConformationalSpace clone = original.clone();
	    assertEquals(clone, fromJson);
    }
	
//------------------------------------------------------------------------------

}
