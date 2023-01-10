package autocompchem.modeling.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.Constraint.ConstraintType;

public class ConstraintsDefinitionTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testMakeConstraints() throws Exception
    {
		ConstrainDefinition cstrDef = new ConstrainDefinition(
		"[C] [Ru] [C] [N] onlybonded options:key5 key6", 0);
		
    	Constraint c = cstrDef.makeConstraintFromIDs(
    			new ArrayList<Integer>(Arrays.asList(2, 4, 1, 3)),
    			true, Double.parseDouble("1.2345"));
    	
    	assertEquals(4, c.getNumberOfIDs());
    	assertEquals("key5 key6", c.getOpt());
    	assertEquals(2, c.getAtomIDs()[0]);
    	assertEquals(4, c.getAtomIDs()[1]);
    	assertEquals(1, c.getAtomIDs()[2]);
    	assertEquals(3, c.getAtomIDs()[3]);
    	assertEquals(ConstraintType.DIHEDRAL, c.getType());
    	assertTrue(Math.abs(0.0-c.getValue())<0.0001);
    	
    	cstrDef = new ConstrainDefinition(
    			"[C] [Ru] [C] [N] -9.876 onlybonded options:key5 key6", 0);
    	c = cstrDef.makeConstraintFromIDs(
    			new ArrayList<Integer>(Arrays.asList(2, 4, 1, 3)),
    			true, Double.parseDouble("1.2345"));
    	
    	assertEquals(4, c.getNumberOfIDs());
    	assertEquals("key5 key6", c.getOpt());
    	assertEquals(2, c.getAtomIDs()[0]);
    	assertEquals(4, c.getAtomIDs()[1]);
    	assertEquals(1, c.getAtomIDs()[2]);
    	assertEquals(3, c.getAtomIDs()[3]);
    	assertEquals(ConstraintType.DIHEDRAL, c.getType());
    	assertTrue(Math.abs(-9.876-c.getValue())<0.0001);
    	
    	c = cstrDef.makeConstraintFromIDs(
    			new ArrayList<Integer>(Arrays.asList(2, 4, 1, 3)),
    			true, null);
    	
    	assertEquals(4, c.getNumberOfIDs());
    	assertEquals("key5 key6", c.getOpt());
    	assertEquals(2, c.getAtomIDs()[0]);
    	assertEquals(4, c.getAtomIDs()[1]);
    	assertEquals(1, c.getAtomIDs()[2]);
    	assertEquals(3, c.getAtomIDs()[3]);
    	assertEquals(ConstraintType.DIHEDRAL, c.getType());
    	assertTrue(Math.abs(-9.876-c.getValue())<0.0001);
    	
    	cstrDef = new ConstrainDefinition(
    			"[C] [Ru] [C] [N] -9.876 usecurrentvalue onlybonded options:key5",
    			0);
    	c = cstrDef.makeConstraintFromIDs(
    			new ArrayList<Integer>(Arrays.asList(2, 4, 1, 3)),
    			true, Double.parseDouble("1.2345"));
    	
    	assertEquals(4, c.getNumberOfIDs());
    	assertEquals("key5", c.getOpt());
    	assertEquals(2, c.getAtomIDs()[0]);
    	assertEquals(4, c.getAtomIDs()[1]);
    	assertEquals(1, c.getAtomIDs()[2]);
    	assertEquals(3, c.getAtomIDs()[3]);
    	assertEquals(ConstraintType.DIHEDRAL, c.getType());
    	assertTrue(Math.abs(1.2345-c.getValue())<0.0001);
    	
    	boolean found = false;
    	try {
    		c = cstrDef.makeConstraintFromIDs(
	    			new ArrayList<Integer>(Arrays.asList(2, 4, 1, 3)),
	    			true, null);
    	} catch (IllegalArgumentException iae)
    	{
    		if (iae.getMessage().contains("given current value is null"))
    			found = true;
    	}
    	assertTrue(found, "We throw exception when null current value is given "
    			+ "to a rule that requires current value.");
    
    }
    
//------------------------------------------------------------------------------

}
