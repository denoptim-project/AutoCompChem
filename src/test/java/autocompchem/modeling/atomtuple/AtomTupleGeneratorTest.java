package autocompchem.modeling.atomtuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;

public class AtomTupleGeneratorTest 
{
	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------

    /**
     * Creates a container for a 3D geometry of
     * 
     * <pre>
     *      H      H
     *     /      /
     * Cl-C-O-Mo=N-F
     *     \
     *      H
     * </pre>
     * @return an atom container for testing purposes.
     */
    public static IAtomContainer getTestIAtomContainer()
    {
    	IAtomContainer iac = chemBuilder.newAtomContainer();
    	IAtom a1 = chemBuilder.newAtom();
    	a1.setSymbol("C");
        a1.setPoint3d(new Point3d(2.8449, -0.4400, 0.0217));
        iac.addAtom(a1);
    	IAtom a2 = chemBuilder.newAtom();
    	a2.setSymbol("O");
        a2.setPoint3d(new Point3d(1.7727, 0.5029, -0.0372));
        iac.addAtom(a2);
    	IAtom a3 = chemBuilder.newAtom();
    	a3.setSymbol("Mo");
        a3.setPoint3d(new Point3d(0.5244, -0.2703,  0.0081));
        iac.addAtom(a3);
    	IAtom a4 = chemBuilder.newAtom();
    	a4.setSymbol("N");
        a4.setPoint3d(new Point3d(-0.5733,  0.6950, -0.0523));
        iac.addAtom(a4);
    	IAtom a5 = chemBuilder.newAtom();
    	a5.setSymbol("F");
        a5.setPoint3d(new Point3d(-2.0247, -0.2076,  0.0006));
        iac.addAtom(a5);
    	IAtom a6 = chemBuilder.newAtom();
    	a6.setSymbol("Cl");
        a6.setPoint3d(new Point3d(3.7969,  0.0900, -0.0092));
        iac.addAtom(a6);
    	IAtom a7 = chemBuilder.newAtom();
    	a7.setSymbol("H");
        a7.setPoint3d(new Point3d(2.7765, -1.0104,  0.9480));
        iac.addAtom(a7);
    	IAtom a8 = chemBuilder.newAtom();
    	a8.setSymbol("H");
        a8.setPoint3d(new Point3d(2.7791, -1.1186, -0.8287));
        iac.addAtom(a8);
    	IAtom a9 = chemBuilder.newAtom();
    	a9.setSymbol("H");
        a9.setPoint3d(new Point3d(-0.5345,  1.2319,  0.8011));
        iac.addAtom(a9);
        
        iac.addBond(0, 1, IBond.Order.SINGLE);
        iac.addBond(1, 2, IBond.Order.SINGLE);
        iac.addBond(2, 3, IBond.Order.DOUBLE);
        iac.addBond(3, 4, IBond.Order.SINGLE);
        iac.addBond(0, 5, IBond.Order.SINGLE);
        iac.addBond(0, 6, IBond.Order.SINGLE);
        iac.addBond(0, 7, IBond.Order.SINGLE);
        iac.addBond(3, 8, IBond.Order.SINGLE);
    	return iac;
    }

//------------------------------------------------------------------------------

    @Test
    public void testCreateTuples() throws Exception
    {
    	List<AtomTupleMatchingRule> rules = 
    			new ArrayList<AtomTupleMatchingRule>();
    	AtomTupleMatchingRule rule1 = new AtomTupleMatchingRule("idBased_1", 
    			new int[] {1});
    	rules.add(rule1);
    	AtomTupleMatchingRule rule2 = new AtomTupleMatchingRule("idBased_2", 
    			new int[] {1, 3, 5, 7});
    	rules.add(rule2);
    	AtomTupleMatchingRule rule3 = new AtomTupleMatchingRule("smBased_1", 
    			new SMARTS[]{new SMARTS("[#1,#6]"), new SMARTS("[#6,#7,#8]")});
    	rules.add(rule3);
    	AtomTupleMatchingRule rule4 = new AtomTupleMatchingRule("smBased_2", 
    			new SMARTS[]{new SMARTS("[#1]"), new SMARTS("[#6]"),
    					new SMARTS("[O]"), new SMARTS("[Cl]")});
    	rules.add(rule4);
    	rule2.setValuedAttribute("Key1:".toUpperCase(), "value1 value1b");
    	rule2.setValuedAttribute("Key2:".toUpperCase(), "value2");
    	rule2.setValuelessAttribute("Key3".toUpperCase());
    	rule4.setValuedAttribute("Key4:".toUpperCase(), "value4 value4b");
    	rule4.setValuedAttribute("Key5:".toUpperCase(), "value5");
    	rule4.setValuelessAttribute("Key6".toUpperCase());
    	
    	List<AnnotatedAtomTuple> tuples = AtomTupleGenerator.createTuples(
    			getTestIAtomContainer(), rules);
    	
    	List<String> expected = Arrays.asList("1,",
    			"1,3,5,7,KEY3,KEY1:=value1 value1b,KEY2:=value2,",
    			"0,1,",
    			"0,3,",
    			"6,0,",
    			"6,1,",
    			"6,3,",
    			"7,0,",
    			"7,1,",
    			"7,3,",
    			"8,0,",
    			"8,1,",
    			"8,3,",
    			"6,0,1,5,KEY6,KEY4:=value4 value4b,KEY5:=value5,",
    			"7,0,1,5,KEY6,KEY4:=value4 value4b,KEY5:=value5,",
    			"8,0,1,5,KEY6,KEY4:=value4 value4b,KEY5:=value5,");
    
    	List<String> results = new ArrayList<String>();
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		results.add(getString(tuple));
    	}

    	assertEquals(expected.size(), results.size());
    	for (int i=0; i<expected.size(); i++)
    		assertEquals(expected.get(i), results.get(i));
    	

    	// Now the number of hits is very much reduce by the constraint that
    	// accepts only those tuples corresponding to connected paths.
    	rule1.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule2.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule3.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule4.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	
    	expected = Arrays.asList("1,ONLYBONDED,",
    			"0,1,ONLYBONDED,",
    			"6,0,ONLYBONDED,",
    			"7,0,ONLYBONDED,",
    			"8,3,ONLYBONDED,");
    	
    	tuples = AtomTupleGenerator.createTuples(getTestIAtomContainer(),rules);
    	
    	results = new ArrayList<String>();
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		results.add(getString(tuple));
    	}

    	assertEquals(expected.size(), results.size());
    	for (int i=0; i<expected.size(); i++)
    		assertEquals(expected.get(i), results.get(i));
    	
    	rules = new ArrayList<AtomTupleMatchingRule>();
    	AtomTupleMatchingRule rule5 = new AtomTupleMatchingRule("smBased_5", 
    			new SMARTS[]{new SMARTS("[#1]")});
    	rules.add(rule5);
    	AtomTupleMatchingRule rule6 = new AtomTupleMatchingRule("smBased_6", 
    			new SMARTS[]{new SMARTS("[#1]"), new SMARTS("[#6]")});
    	rules.add(rule6);
    	AtomTupleMatchingRule rule7 = new AtomTupleMatchingRule("smBased_7", 
    			new SMARTS[]{new SMARTS("[#1]"), new SMARTS("[#6]"),
    					new SMARTS("[O]")});
    	rules.add(rule7);
    	AtomTupleMatchingRule rule8 = new AtomTupleMatchingRule("smBased_8", 
    			new SMARTS[]{new SMARTS("[#1]"), new SMARTS("[#6]"),
    					new SMARTS("[O]"), new SMARTS("[Mo]")});
    	rules.add(rule8);
    	AtomTupleMatchingRule rule9 = new AtomTupleMatchingRule("smBased_9", 
    			new SMARTS[]{new SMARTS("[#1]"), new SMARTS("[#6]"),
    					new SMARTS("[O]"), new SMARTS("[Mo]"), 
    					new SMARTS("[N]")});
    	rules.add(rule9);
    	rule5.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule6.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule7.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule8.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule9.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    	rule5.setValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE);
    	rule6.setValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE);
    	rule7.setValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE);
    	rule8.setValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE);
    	rule9.setValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE);
    	
    	List<Double> expectedValues = new ArrayList<Double>();
    	expectedValues.add(null);
    	expectedValues.add(null);
    	expectedValues.add(null);
    	expectedValues.add(1.089984);
    	expectedValues.add(1.089957);
    	expectedValues.add(109.465050);
    	expectedValues.add(109.471866);
    	expectedValues.add(-60.003268);
    	expectedValues.add(60.0020569);
    	expectedValues.add(null);
    	expectedValues.add(null);
    	
    	tuples = AtomTupleGenerator.createTuples(getTestIAtomContainer(),rules);
    	
    	results = new ArrayList<String>();
    	int i=0;
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		String valueStr = tuple.getValueOfAttribute(
    				AtomTupleConstants.KEYCURRENTVALUE);
    		if (valueStr==null)
    		{
    			assertNull(expectedValues.get(i));
    		} else {
    			assertTrue(NumberUtils.closeEnough(Double.parseDouble(valueStr),
    					expectedValues.get(i), 0.0001));
    		}	
    		i++;
    	}
    }
    
//------------------------------------------------------------------------------
    
    private String getString(AnnotatedAtomTuple tuple)
    {

		String s = StringUtils.mergeListToString(tuple.getAtomIDs(), ",");
		
		List<String> sortedValueless = new ArrayList<String>();
		sortedValueless.addAll(tuple.getValuelessAttribute());
		Collections.sort(sortedValueless);
		s = s + StringUtils.mergeListToString(sortedValueless, ",");
		
		List<String> sortedValued = new ArrayList<String>();
		sortedValued.addAll(tuple.getValuedAttributeKeys());
		Collections.sort(sortedValued);
		for (String key : sortedValued)
		{
			s = s + key + "=" + tuple.getValueOfAttribute(key)+ ",";
		}
		return s;
    }
    
//------------------------------------------------------------------------------

}
