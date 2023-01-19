package autocompchem.modeling.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.atomtuple.AtomTupleMatchingRule.RuleType;
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

    	def = new ConstrainDefinition("[#1]", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(1, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertFalse(def.hasValue());

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
    	
    	 def = new ConstrainDefinition("1 value:-1.23", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(1, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());

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

    	def = new ConstrainDefinition("[#1] value: -1.23", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(1, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertFalse(def.limitToBonded());
    	assertFalse(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    	assertEquals(-1.23, def.getValue());

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
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 getCurrentValue notanic "
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

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " value:-1.23 notanic onlybonded getCurrentValue", 0);
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
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 getCurrentValue notanic "
    			+ "onlybonded suffix: A B C D ", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertEquals("A B C D", def.getSuffix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " value:-1.23 notanic suffix: A B C D "
    			+ "onlybonded getCurrentValue", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(4, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals("A B C D", def.getSuffix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	
    	
    	def = new ConstrainDefinition("1 2 3 4 5 getCurrentValue notanic "
    			+ "onlybonded suffix: AFTER  AFTER1  "
    			+ "prefix: BEFORE BEFORE2", 0);
    	assertEquals(RuleType.ID,def.getType());
    	assertNull(def.getSMARTS());
    	assertEquals(5, def.getAtomIDs().size());
    	assertEquals(1, def.getAtomIDs().get(0));
    	assertEquals(2, def.getAtomIDs().get(1));
    	assertEquals(3, def.getAtomIDs().get(2));
    	assertEquals(4, def.getAtomIDs().get(3));
    	assertEquals(5, def.getAtomIDs().get(4));
    	assertEquals("BEFORE BEFORE2", def.getPrefix());
    	assertEquals("AFTER AFTER1", def.getSuffix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());

    	def = new ConstrainDefinition("[#1] [#2] [#3] [#4,$([*]:Cl)]"
    			+ " prefix:BEFORE BEFORE2 value:-1.23 notanic "
    			+ "onlybonded getCurrentValue suffix:AFTER  AFTER1", 0);
    	assertEquals(RuleType.SMARTS,def.getType());
    	assertNull(def.getAtomIDs());
    	assertEquals(4, def.getSMARTS().size());
    	assertEquals(new SMARTS("[#1]"), def.getSMARTS().get(0));
    	assertEquals(new SMARTS("[#2]"), def.getSMARTS().get(1));
    	assertEquals(new SMARTS("[#3]"), def.getSMARTS().get(2));
    	assertEquals(new SMARTS("[#4,$([*]:Cl)]"), def.getSMARTS().get(3));
    	assertEquals("BEFORE BEFORE2", def.getPrefix());
    	assertEquals("AFTER AFTER1", def.getSuffix());
    	assertTrue(def.limitToBonded());
    	assertTrue(def.usesCurrentValue());
    	assertTrue(def.hasValue());
    }
    
//------------------------------------------------------------------------------

}
