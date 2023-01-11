package autocompchem.modeling.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.ConstrainDefinition.RuleType;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.smarts.SMARTS;

public class ConstraintsDefinitionTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromString() throws Exception
    {
    	ConstrainDefinition def = new ConstrainDefinition("1", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(1, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertFalse(def.hasValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("1 2 3 4 5", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertFalse(def.hasValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("[#1]", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(1, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertFalse(def.hasValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)] [#5]", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(5, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals(new SMARTS("[#5]"), def.getSMARTS().get(4));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertFalse(def.hasValue());
    	assertFalse(def.notAnIC());
    	
    	 def = new ConstrainDefinition("1 value:-1.23", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(1, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("1 2 3 4 5 value: -1.23", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("[#1] value: -1.23", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(1, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)] [#5] "
    			+ " value:-1.23", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(5, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals(new SMARTS("[#5]"), def.getSMARTS().get(4));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertFalse(def.notAnIC());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 onlybonded value: -1.23", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertTrue(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertFalse(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)] [#5] "
    			+ " value:-1.23 onlybonded", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(5, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals(new SMARTS("[#5]"), def.getSMARTS().get(4));
    	assertTrue(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertFalse(def.notAnIC());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 notanic onlybonded "
    			+ "value: -1.23", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertTrue(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertTrue(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)] [#5] "
    			+ " value:-1.23 notanic onlybonded", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(5, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals(new SMARTS("[#5]"), def.getSMARTS().get(4));
    	assertTrue(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());
    	assertTrue(def.notAnIC());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 usecurrentvalue notanic "
    			+ "onlybonded", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " value:-1.23 notanic onlybonded usecurrentvalue", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(4, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 usecurrentvalue notanic "
    			+ "onlybonded options: A B C D ", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertEquals("A B C D", def.getOpts());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " value:-1.23 notanic options: A B C D "
    			+ "onlybonded usecurrentvalue", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(4, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals("A B C D", def.getOpts());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 usecurrentvalue notanic "
    			+ "onlybonded options: A B C D prefix:BEFORE BEFORE2", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertEquals("A B C D", def.getOpts());
    	assertEquals("BEFORE BEFORE2", def.getPrefix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " prefix:BEFORE BEFORE2 value:  -1.23 notanic options: A B C D "
    			+ "onlybonded usecurrentvalue", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(4, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals("A B C D", def.getOpts());
    	assertEquals("BEFORE BEFORE2", def.getPrefix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 usecurrentvalue notanic "
    			+ "onlybonded options:  A B C D suffix: AFTER  AFTER1  "
    			+ "prefix: BEFORE BEFORE2", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertEquals("A B C D", def.getOpts());
    	assertEquals("BEFORE BEFORE2", def.getPrefix());
    	assertEquals("AFTER AFTER1", def.getSuffix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " prefix:BEFORE BEFORE2 value:-1.23 notanic options: A B C D "
    			+ "onlybonded usecurrentvalue suffix:AFTER  AFTER1", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(4, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals("A B C D", def.getOpts());
    	assertEquals("BEFORE BEFORE2", def.getPrefix());
    	assertEquals("AFTER AFTER1", def.getSuffix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertTrue(def.notAnIC());
    }
    
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
    			"[C] [Ru] [C] [N] value:-9.876 onlybonded options:key5 key6", 0);
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
    			"[C] [Ru] [C] [N] value:-9.876 usecurrentvalue onlybonded options:key5",
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
    

    	cstrDef = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " prefix:BEFORE BEFORE2 value:-1.23 notanic options: A B C D "
    			+ "onlybonded usecurrentvalue suffix:AFTER  AFTER1", 0);
    	c = cstrDef.makeConstraintFromIDs(
    			new ArrayList<Integer>(Arrays.asList(2, 4, 1, 3)),
    			true, Double.parseDouble("1.2345"));
    	assertEquals(4, c.getNumberOfIDs());
    	assertEquals(2, c.getAtomIDs()[0]);
    	assertEquals(4, c.getAtomIDs()[1]);
    	assertEquals(1, c.getAtomIDs()[2]);
    	assertEquals(3, c.getAtomIDs()[3]);
    	assertEquals(ConstraintType.UNDEFINED, c.getType());
    	assertEquals("A B C D", c.getOpt());
    	assertTrue(Math.abs(1.2345-c.getValue())<0.0001);
    	assertEquals("BEFORE BEFORE2", c.getPrefix());
    	assertEquals("AFTER AFTER1", c.getSuffix());
    }
    
//------------------------------------------------------------------------------

}
