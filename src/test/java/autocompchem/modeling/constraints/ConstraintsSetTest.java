package autocompchem.modeling.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.Constraint.ConstraintType;

public class ConstraintsSetTest 
{

//------------------------------------------------------------------------------

	/**
	 * Created a dummy set of constraints filled with non-sense but plausible 
	 * values.
	 * @return a dummy set of constraints.
	 */
    public static ConstraintsSet getTestConstraintSet()
    {
    	ConstraintsSet cs = new ConstraintsSet();
    	cs.setNumAtoms(26);
    	cs.add(new Constraint(new int[] {0, 1, 2, 3},
    			ConstraintType.UNDEFINED, 0.1, "opt"));
    	cs.add(new Constraint(new int[] {4, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt"));
    	cs.add(new Constraint(new int[] {6, 1, 2,-1}, 
    			ConstraintType.ANGLE, 0.1, "opt"));
    	cs.add(new Constraint(new int[] {4, 1,-1,-1}, 
    			ConstraintType.DISTANCE, 0.1, "opt"));
    	cs.add(new Constraint(0, 1, 2, 3, true));
    	cs.add(new Constraint(5, 3, 2, 3, false));
    	cs.add(new Constraint(4, 1, 2));
    	cs.add(new Constraint(6, 1));
    	return cs;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ConstraintsSet c1 = getTestConstraintSet();
    	ConstraintsSet c2 = getTestConstraintSet();
    	
    	assertTrue(c1.equals(c2));
    	assertTrue(c2.equals(c1));
    	assertTrue(c1.equals(c1));
    	
    	c2 = getTestConstraintSet();
    	c2.setNumAtoms(c1.getNumAtoms()+1);
    	assertFalse(c1.equals(c2));

    	c2 = getTestConstraintSet();
    	c2.add(new Constraint(1));
    	assertFalse(c1.equals(c2));
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testCompareTo() throws Exception
    {
    	Constraint c1 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
    	Constraint c2 = new Constraint(new int[] {0, 1, 2, 3}, 
    			ConstraintType.DIHEDRAL, 0.1, "opt");
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
    	
    	c1 = new Constraint(new int[] {0,1},
    			ConstraintType.DISTANCE,0.1,"opt");
    	c2 = new Constraint(new int[] {0,1},
    			ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {1,0},
    			ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(new int[] {0,2},
    			ConstraintType.DISTANCE,0.1,"opt");
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

}
