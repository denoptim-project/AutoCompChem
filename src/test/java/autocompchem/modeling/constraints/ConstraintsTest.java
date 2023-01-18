package autocompchem.modeling.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.molecule.connectivity.ConnectivityTable;

public class ConstraintsTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	Constraint c1 = new Constraint(new int[] {0, 1, 2, 3},
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	Constraint c2 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1,"opt");
    	
    	assertTrue(c1.equals(c2));
    	assertTrue(c2.equals(c1));
    	assertTrue(c1.equals(c1));
    	assertFalse(c1.equals(null));
    	
    	c2 = new Constraint(new int[] {4, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	assertFalse(c1.equals(c2));
    	
    	c2 = new Constraint(new int[] {0, 4, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	assertFalse(c1.equals(c2));

    	c2 = new Constraint(new int[] {0, 1, 4, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	assertFalse(c1.equals(c2));

    	c2 = new Constraint(new int[] {0, 1, 2, 4}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	assertFalse(c1.equals(c2));

    	c2 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.ANGLE, 0.1, "opt");
    	assertFalse(c1.equals(c2));

    	c2 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.0, "opt");
    	assertFalse(c1.equals(c2));

    	c2 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opts");
    	assertFalse(c1.equals(c2));
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	Constraint c1 = new Constraint(new int[] {0, 1, 2, 3},
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	
    	Constraint cl1 = c1.clone();
    	assertTrue(c1.equals(cl1));
    	
    	Constraint c2 = new Constraint(new int[] {0, 1, 2, 3},
    			ConstraintType.UNDEFINED, 0.1, "opt");

    	Constraint cl2 = c2.clone();
    	assertTrue(c2.equals(cl2));
    	assertFalse(cl2.equals(cl1));
    }
   
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	Constraint original = new Constraint(new int[] {0, 1, 2, 3},
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	original.setNumAtoms(26);
    	
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
	    String json = writer.toJson(original);
	    Constraint fromJson = reader.fromJson(json, Constraint.class);
	    assertEquals(original, fromJson);

	    Constraint clone = original.clone();
	    assertEquals(clone, fromJson);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCompareTo() throws Exception
    {
    	Constraint c1 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	Constraint c2 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1,
    			"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {3,2,1,0},
    			ConstraintType.DIHEDRAL,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {0,1,2,4},
    			ConstraintType.DIHEDRAL,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    	
    	c1 = new Constraint(new int[] {0,1,2},ConstraintType.ANGLE,0.1,"opt");
    	c2 = new Constraint(new int[] {0,1,2},ConstraintType.ANGLE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {2,1,0},ConstraintType.ANGLE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {0,1,3},ConstraintType.ANGLE,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    	
    	c1 = new Constraint(new int[] {0,1},ConstraintType.DISTANCE,0.1,"opt");
    	c2 = new Constraint(new int[] {0,1},ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {1,0},ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {0,2},ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    	
    	c1 = new Constraint(new int[] {0},ConstraintType.FROZENATM,0.1,"opt");
    	c2 = new Constraint(new int[] {0},ConstraintType.FROZENATM,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {1},ConstraintType.FROZENATM,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetConstraintType() throws Exception
    {
    	List<Integer> ids = new ArrayList<>(Arrays.asList(1));
    	ConnectivityTable ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {});
    	assertEquals(ConstraintType.FROZENATM, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1));
    	assertEquals(ConstraintType.FROZENATM, 
    			Constraint.getConstraintType(ids, null));
    	
    	ids = new ArrayList<>(Arrays.asList(1, 2));
    	ct.addNeighborningRelation(1, new int[] {2});
    	assertEquals(ConstraintType.DISTANCE, 
    			Constraint.getConstraintType(ids, null));
    	
    	ids = new ArrayList<>(Arrays.asList(1, 2));
    	ct = new ConnectivityTable();
    	assertEquals(ConstraintType.DISTANCE, 
    			Constraint.getConstraintType(ids, ct));
    	
    	ids = new ArrayList<>(Arrays.asList(1, 2));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {2});
    	assertEquals(ConstraintType.DISTANCE, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3));
    	assertEquals(ConstraintType.ANGLE, 
    			Constraint.getConstraintType(ids, null));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3));
    	ct = new ConnectivityTable();
    	assertEquals(ConstraintType.ANGLE, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {2});
    	assertEquals(ConstraintType.ANGLE, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {2});
    	ct.addNeighborningRelation(2, new int[] {3});
    	assertEquals(ConstraintType.ANGLE, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {2});
    	ct.addNeighborningRelation(1, new int[] {3});
    	assertEquals(ConstraintType.ANGLE, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
    	assertEquals(ConstraintType.UNDEFINED, 
    			Constraint.getConstraintType(ids, null));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
    	ct = new ConnectivityTable();
    	assertEquals(ConstraintType.UNDEFINED, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {2});
    	ct.addNeighborningRelation(2, new int[] {3});
    	ct.addNeighborningRelation(3, new int[] {4});
    	assertEquals(ConstraintType.DIHEDRAL, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {2});
    	ct.addNeighborningRelation(2, new int[] {3});
    	ct.addNeighborningRelation(2, new int[] {4});
    	assertEquals(ConstraintType.IMPROPERTORSION, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
    	ct = new ConnectivityTable();
    	ct.addNeighborningRelation(1, new int[] {3});
    	ct.addNeighborningRelation(2, new int[] {3});
    	ct.addNeighborningRelation(3, new int[] {4});
    	assertEquals(ConstraintType.IMPROPERTORSION, 
    			Constraint.getConstraintType(ids, ct));

    	ids = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
    	boolean hasThrown = false;
    	try {
    		Constraint.getConstraintType(ids, null);
    	} catch (IllegalArgumentException e) {
    		if (e.getMessage().startsWith("Unexpected number of atom IDs"))
    			hasThrown = true;
    	}
    	assertTrue(hasThrown);
    	
    	ids = new ArrayList<>();
    	hasThrown = false;
    	try {
    		Constraint.getConstraintType(ids, null);
    	} catch (IllegalArgumentException e) {
    		if (e.getMessage().startsWith("Unexpected number of atom IDs"))
    			hasThrown = true;
    	}
    	assertTrue(hasThrown);
    }
//------------------------------------------------------------------------------

}
