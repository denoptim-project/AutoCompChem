package autocompchem.modeling.basisset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.Constraint.ConstraintType;

public class ConstraintsTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testCompareTo() throws Exception
    {
    	Constraint c1 = new Constraint(0,1,2,3,ConstraintType.DIHEDRAL,0.1,"opt");
    	Constraint c2 = new Constraint(0,1,2,3,ConstraintType.DIHEDRAL,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(3,2,1,0,ConstraintType.DIHEDRAL,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(0,1,2,4,ConstraintType.DIHEDRAL,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    	
    	c1 = new Constraint(0,1,2,-1,ConstraintType.ANGLE,0.1,"opt");
    	c2 = new Constraint(0,1,2,-1,ConstraintType.ANGLE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(2,1,0,-1,ConstraintType.ANGLE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(0,1,3,-1,ConstraintType.ANGLE,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    	
    	c1 = new Constraint(0,1,-1,-1,ConstraintType.DISTANCE,0.1,"opt");
    	c2 = new Constraint(0,1,-1,-1,ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(1,0,-1,-1,ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(0,2,-1,-1,ConstraintType.DISTANCE,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    	
    	c1 = new Constraint(0,-1,-1,-1,ConstraintType.FROZENATM,0.1,"opt");
    	c2 = new Constraint(0,-1,-1,-1,ConstraintType.FROZENATM,0.1,"opt");
    	assertEquals(0,c1.compareTo(c2));
    	c2 = new Constraint(1,-1,-1,-1,ConstraintType.FROZENATM,0.1,"opt");
    	assertEquals(-1,c1.compareTo(c2));
    	assertEquals(1,c2.compareTo(c1));
    }
    
//------------------------------------------------------------------------------

}
