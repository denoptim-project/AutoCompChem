package autocompchem.modeling.atomtuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AtomTupleGenerator.Mode;
import autocompchem.molecule.MolecularMeter;
import autocompchem.molecule.MolecularUtils;
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
    	
    	List<String> expected = Arrays.asList("1,"+AtomTupleConstants.KEYRULENAME+"=idBased_1,",
    			"1,3,5,7,KEY3,KEY1:=value1 value1b,KEY2:=value2,"+AtomTupleConstants.KEYRULENAME+"=idBased_2,",
    			"0,1,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"0,3,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"6,0,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"6,1,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"6,3,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"7,0,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"7,1,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"7,3,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"8,0,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"8,1,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"8,3,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"6,0,1,5,KEY6,KEY4:=value4 value4b,KEY5:=value5,"+AtomTupleConstants.KEYRULENAME+"=smBased_2,",
    			"7,0,1,5,KEY6,KEY4:=value4 value4b,KEY5:=value5,"+AtomTupleConstants.KEYRULENAME+"=smBased_2,",
    			"8,0,1,5,KEY6,KEY4:=value4 value4b,KEY5:=value5,"+AtomTupleConstants.KEYRULENAME+"=smBased_2,");
    
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
    	
    	expected = Arrays.asList("1,ONLYBONDED,"+AtomTupleConstants.KEYRULENAME+"=idBased_1,",
    			"0,1,ONLYBONDED,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"6,0,ONLYBONDED,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"7,0,ONLYBONDED,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,",
    			"8,3,ONLYBONDED,"+AtomTupleConstants.KEYRULENAME+"=smBased_1,");
    	
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

    @Test
    public void testCreateTuples_setsMode() throws Exception
    {
    	List<AtomTupleMatchingRule> rules = 
    			new ArrayList<AtomTupleMatchingRule>();
    	AtomTupleMatchingRule rule1 = new AtomTupleMatchingRule("idBased_1", 
    			new int[] {1, 2, 3, 7});
    	rules.add(rule1);
    	AtomTupleMatchingRule rule3 = new AtomTupleMatchingRule("smBased_1", 
    			new SMARTS[]{new SMARTS("[#1,#6]"), new SMARTS("[#6,#7,#8]")});
    	rules.add(rule3);
    	AtomTupleMatchingRule rule4 = new AtomTupleMatchingRule("smBased_2", 
    			new SMARTS[]{new SMARTS("[#1][#6][Cl]"), new SMARTS("[#8]"),
    					new SMARTS("[Mo]~[#7]")});
    	rules.add(rule4);
    	
        /**
         * 
         * <pre>
         *      H      H
         *     /      /
         * Cl-C-O-Mo=N-F
         *     \
         *      H
         * </pre>
         */
		IAtomContainer iac = getTestIAtomContainer();
    	List<AnnotatedAtomTuple> tuples = AtomTupleGenerator.createTuples(
    			iac, rules, null, Mode.SETS);
    
    	assertEquals(3, tuples.size());
    	assertEquals(4, tuples.get(0).getNumberOfIDs());
    	assertEquals(6, tuples.get(1).getNumberOfIDs());
    	assertEquals(7, tuples.get(2).getNumberOfIDs());
	}

//------------------------------------------------------------------------------

	@Test
	public void testCreateTuples_geomConditions() throws Exception
	{
    	IAtomContainer iac = chemBuilder.newAtomContainer();
    	IAtom a1 = chemBuilder.newAtom();
    	a1.setSymbol("C");
        a1.setPoint3d(new Point3d(0.0, 0.0, 0.0));
        iac.addAtom(a1);
    	IAtom a2 = chemBuilder.newAtom();
    	a2.setSymbol("O");
        a2.setPoint3d(new Point3d(1.8, 0.0, 0.0));
        iac.addAtom(a2);
    	IAtom a3 = chemBuilder.newAtom();
    	a3.setSymbol("O");
        a3.setPoint3d(new Point3d(1.2, 0.0, 0.0));
        iac.addAtom(a3);
    	IAtom a4 = chemBuilder.newAtom();
    	a4.setSymbol("H");
        a4.setPoint3d(new Point3d(0.0,  1.09, 0.0));
        iac.addAtom(a4);
    	IAtom a5 = chemBuilder.newAtom();
    	a5.setSymbol("H");
        a5.setPoint3d(new Point3d(0.0,  -1.09, 0.10)); //to get negative dihedral
        iac.addAtom(a5);
    	IAtom a6 = chemBuilder.newAtom();
    	a6.setSymbol("H");
        a6.setPoint3d(new Point3d(-1.0,  0.0, 2.18));
        iac.addAtom(a6);
    	IAtom a7 = chemBuilder.newAtom();
    	a7.setSymbol("H");
        a7.setPoint3d(new Point3d(0.5, 0.5, 0.0));
        iac.addAtom(a7);
    	IAtom a8 = chemBuilder.newAtom();
    	a8.setSymbol("O");
        a8.setPoint3d(new Point3d(2.2, 1.5, 0.0));
        iac.addAtom(a8);
    	IAtom a9 = chemBuilder.newAtom();
    	a9.setSymbol("H");
        a9.setPoint3d(new Point3d(1.0, 1.5, 0.0));
        iac.addAtom(a9);
    	IAtom a10 = chemBuilder.newAtom();
    	a10.setSymbol("C");
        a10.setPoint3d(new Point3d(2.0, 1.5, 1.09));
        iac.addAtom(a10);
		IAtom a11 = chemBuilder.newAtom();
		a11.setSymbol("H");
		a11.setPoint3d(new Point3d(3.0, 1.5, 0.0));
		iac.addAtom(a11);
		IAtom a12 = chemBuilder.newAtom();
		a12.setSymbol("H");
		a12.setPoint3d(new Point3d(3.0, 2.5, 1.09));
		iac.addAtom(a12);
        
        iac.addBond(0, 1, IBond.Order.SINGLE);
        iac.addBond(0, 1, IBond.Order.SINGLE);
        iac.addBond(0, 2, IBond.Order.DOUBLE);
        iac.addBond(0, 3, IBond.Order.SINGLE);
        iac.addBond(0, 4, IBond.Order.SINGLE);
        iac.addBond(0, 5, IBond.Order.SINGLE);
        iac.addBond(0, 6, IBond.Order.SINGLE);
        iac.addBond(7, 1, IBond.Order.SINGLE);
        iac.addBond(8, 7, IBond.Order.SINGLE);
        iac.addBond(9, 7, IBond.Order.SINGLE);
		iac.addBond(10, 9, IBond.Order.SINGLE);
		iac.addBond(11, 9, IBond.Order.SINGLE);

		//TODO-gg del
		IOtools.writeSDFAppend(new File("/tmp/mol.sdf"), iac, false);

		List<AtomTupleMatchingRule> rules = new ArrayList<AtomTupleMatchingRule>();

    	AtomTupleMatchingRule ruleCH = new AtomTupleMatchingRule(
			"[#1][#6]" , "CH_all");
    	rules.add(ruleCH);

		AtomTupleMatchingRule ruleCHMin = new AtomTupleMatchingRule(
			"[#1][#6] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DISTANCE + " 0 1 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MIN , 
			"CH_Min");
    	rules.add(ruleCHMin);

		AtomTupleMatchingRule ruleCHMax = new AtomTupleMatchingRule(
			"[#1][#6] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DISTANCE + " 0 1 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MAX , 
			"CH_Max");
    	rules.add(ruleCHMax);

		AtomTupleMatchingRule ruleCHCloseTo = new AtomTupleMatchingRule(
			"[#1][#6] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DISTANCE + " 0 1 "
			+ AtomTupleGeomCondition.GeomConditionOperator.CLOSE_TO + " 1.48" , 
			"CH_CloseTo");
    	rules.add(ruleCHCloseTo);

		AtomTupleMatchingRule ruleCHLessThan = new AtomTupleMatchingRule(
			"[#1][#6] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DISTANCE + " 0 1 "
			+ AtomTupleGeomCondition.GeomConditionOperator.LESS_THAN + " 1.4" , 
			"CH_LessThan");
    	rules.add(ruleCHLessThan);

		AtomTupleMatchingRule ruleCHMoreThan = new AtomTupleMatchingRule(
			"[#1][#6] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DISTANCE + " 0 1 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MORE_THAN + " 1.5" , 
			"CH_MoreThan");
    	rules.add(ruleCHMoreThan);

		AtomTupleMatchingRule ruleHCOall = new AtomTupleMatchingRule(
			"[#1][#6][#8]", 
			"HCO_all");
    	rules.add(ruleHCOall);

		AtomTupleMatchingRule ruleHCOMin = new AtomTupleMatchingRule(
			"[#1][#6][#8] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.ANGLE + " 0 1 2 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MIN , 
			"HCO_Min");
    	rules.add(ruleHCOMin);

		AtomTupleMatchingRule ruleHCOMax = new AtomTupleMatchingRule(
			"[#1][#6][#8] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.ANGLE + " 0 1 2 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MAX , 
			"HCO_Max");
    	rules.add(ruleHCOMax);

		AtomTupleMatchingRule ruleHCOCloseTo = new AtomTupleMatchingRule(
			"[#1][#6][#8] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.ANGLE + " 0 1 2 "
			+ AtomTupleGeomCondition.GeomConditionOperator.CLOSE_TO + " 45" , 
			"HCO_CloseTo");
    	rules.add(ruleHCOCloseTo);

		AtomTupleMatchingRule ruleHCOLessThan = new AtomTupleMatchingRule(
			"[#1][#6][#8] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.ANGLE + " 0 1 2 "
			+ AtomTupleGeomCondition.GeomConditionOperator.LESS_THAN + " 95.0" , 
			"HCO_LessThan");
    	rules.add(ruleHCOLessThan);

		AtomTupleMatchingRule ruleHCOMoreThan = new AtomTupleMatchingRule(
			"[#1][#6][#8] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.ANGLE + " 0 1 2 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MORE_THAN + " 95.0" , 
			"HCO_MoreThan");
    	rules.add(ruleHCOMoreThan);

		AtomTupleMatchingRule ruleOOCHall = new AtomTupleMatchingRule(
			"[#8][#8][#6][#1]", 
			"OOCH_all");
    	rules.add(ruleOOCHall);

		AtomTupleMatchingRule ruleOOCMin = new AtomTupleMatchingRule(
			"[#8][#8][#6][#1] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DIHEDRAL + " 0 1 2 3 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MIN , 
			"OOCH_Min");
    	rules.add(ruleOOCMin);

		AtomTupleMatchingRule ruleOOCMax = new AtomTupleMatchingRule(
			"[#8][#8][#6][#1] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DIHEDRAL + " 0 1 2 3 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MAX , 
			"OOCH_Max");
    	rules.add(ruleOOCMax);

		AtomTupleMatchingRule ruleOOCHCloseTo = new AtomTupleMatchingRule(
			"[#8][#8][#6][#1] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DIHEDRAL + " 0 1 2 3 "
			+ AtomTupleGeomCondition.GeomConditionOperator.CLOSE_TO + " 0.0" , 
			"OOCH_CloseTo");
    	rules.add(ruleOOCHCloseTo);

		AtomTupleMatchingRule ruleOOCHLessThan = new AtomTupleMatchingRule(
			"[#8][#8][#6][#1] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DIHEDRAL + " 0 1 2 3 "
			+ AtomTupleGeomCondition.GeomConditionOperator.LESS_THAN + " 0.0" , 
			"OOCH_LessThan");
    	rules.add(ruleOOCHLessThan);

		AtomTupleMatchingRule ruleOOCHMoreThan = new AtomTupleMatchingRule(
			"[#8][#8][#6][#1] " + AtomTupleConstants.KEYGEOMETRYCONDITIONS + ": "
			+ AtomTupleGeomCondition.GeomConditionType.DIHEDRAL + " 0 1 2 3 "
			+ AtomTupleGeomCondition.GeomConditionOperator.MORE_THAN + " 90.0" , 
			"OOCH_MoreThan");
    	rules.add(ruleOOCHMoreThan);

		List<AnnotatedAtomTuple> tuples = AtomTupleGenerator.createTuples(
			iac, rules, null, Mode.TUPLES);

		List<AnnotatedAtomTuple> tuplesCH = getTuplesFromRuleName("CH_all", tuples);
		assertEquals(6, tuplesCH.size());
		List<AnnotatedAtomTuple> tuplesCHMin = getTuplesFromRuleName("CH_Min", tuples);
		assertEquals(1, tuplesCHMin.size());
		assertEquals(2, tuplesCHMin.get(0).getAtomIDs().size());
		assertEquals(6, tuplesCHMin.get(0).getAtomIDs().get(0));
		assertEquals(0, tuplesCHMin.get(0).getAtomIDs().get(1));
		List<AnnotatedAtomTuple> tuplesCHMax = getTuplesFromRuleName("CH_Max", tuples);
		assertEquals(1, tuplesCHMax.size());
		assertEquals(2, tuplesCHMax.get(0).getAtomIDs().size());
		assertEquals(5, tuplesCHMax.get(0).getAtomIDs().get(0));
		assertEquals(0, tuplesCHMax.get(0).getAtomIDs().get(1));
		List<AnnotatedAtomTuple> tuplesCHCloseTo = getTuplesFromRuleName("CH_CloseTo", tuples);
		assertEquals(1, tuplesCHCloseTo.size());	
		assertEquals(2, tuplesCHCloseTo.get(0).getAtomIDs().size());
		assertEquals(10, tuplesCHCloseTo.get(0).getAtomIDs().get(0));
		assertEquals(9, tuplesCHCloseTo.get(0).getAtomIDs().get(1));
		List<AnnotatedAtomTuple> tuplesCHLessThan = getTuplesFromRuleName("CH_LessThan", tuples);
		assertEquals(3, tuplesCHLessThan.size());
		Set<List<Integer>> expectedAtomIDs = new HashSet<List<Integer>>(Arrays.asList(
			Arrays.asList(6, 0), Arrays.asList(3, 0), Arrays.asList(4, 0)));
		for (AnnotatedAtomTuple tuple : tuplesCHLessThan)
		{
			assertEquals(2, tuple.getAtomIDs().size());
			assertTrue(expectedAtomIDs.contains(tuple.getAtomIDs()));
		}
		List<AnnotatedAtomTuple> tuplesCHMoreThan = getTuplesFromRuleName("CH_MoreThan", tuples);
		assertEquals(1, tuplesCHMoreThan.size());
		assertEquals(2, tuplesCHMoreThan.get(0).getAtomIDs().size());
		assertEquals(5, tuplesCHMoreThan.get(0).getAtomIDs().get(0));
		assertEquals(0, tuplesCHMoreThan.get(0).getAtomIDs().get(1));

		List<AnnotatedAtomTuple> tuplesOCHall = getTuplesFromRuleName("HCO_all", tuples);
		assertEquals(6, tuplesOCHall.size());
		List<AnnotatedAtomTuple> tuplesOCHMin = getTuplesFromRuleName("HCO_Min", tuples);
		assertEquals(1, tuplesOCHMin.size());
		assertEquals(3, tuplesOCHMin.get(0).getAtomIDs().size());
		assertEquals(10, tuplesOCHMin.get(0).getAtomIDs().get(0));
		assertEquals(9, tuplesOCHMin.get(0).getAtomIDs().get(1));
		assertEquals(7, tuplesOCHMin.get(0).getAtomIDs().get(2));
		List<AnnotatedAtomTuple> tuplesOCHMax = getTuplesFromRuleName("HCO_Max", tuples);
		assertEquals(1, tuplesOCHMax.size());
		assertEquals(3, tuplesOCHMax.get(0).getAtomIDs().size());
		assertEquals(5, tuplesOCHMax.get(0).getAtomIDs().get(0));
		assertEquals(0, tuplesOCHMax.get(0).getAtomIDs().get(1));
		assertEquals(1, tuplesOCHMax.get(0).getAtomIDs().get(2));
		List<AnnotatedAtomTuple> tuplesOCHCloseTo = getTuplesFromRuleName("HCO_CloseTo", tuples);
		assertEquals(1, tuplesOCHCloseTo.size());
		assertEquals(3, tuplesOCHCloseTo.get(0).getAtomIDs().size());
		assertEquals(6, tuplesOCHCloseTo.get(0).getAtomIDs().get(0));
		assertEquals(0, tuplesOCHCloseTo.get(0).getAtomIDs().get(1));
		assertEquals(1, tuplesOCHCloseTo.get(0).getAtomIDs().get(2));
		List<AnnotatedAtomTuple> tuplesOCHLessThan = getTuplesFromRuleName("HCO_LessThan", tuples);
		assertEquals(5, tuplesOCHLessThan.size());
		expectedAtomIDs = new HashSet<List<Integer>>(Arrays.asList(
			Arrays.asList(3, 0, 1), 
			Arrays.asList(4, 0, 1), 
			Arrays.asList(6, 0, 1), 
			Arrays.asList(10, 9, 7), 
			Arrays.asList(11, 9, 7)));
		for (AnnotatedAtomTuple tuple : tuplesOCHLessThan)
		{
			assertEquals(3, tuple.getAtomIDs().size());
			assertTrue(expectedAtomIDs.contains(tuple.getAtomIDs()));
		}
		List<AnnotatedAtomTuple> tuplesOCHMoreThan = getTuplesFromRuleName("HCO_MoreThan", tuples);
		assertEquals(1, tuplesOCHMoreThan.size());
		assertEquals(3, tuplesOCHMoreThan.get(0).getAtomIDs().size());
		assertEquals(5, tuplesOCHMoreThan.get(0).getAtomIDs().get(0));
		assertEquals(0, tuplesOCHMoreThan.get(0).getAtomIDs().get(1));
		assertEquals(1, tuplesOCHMoreThan.get(0).getAtomIDs().get(2));

		List<AnnotatedAtomTuple> tuplesOOCHall = getTuplesFromRuleName("OOCH_all", tuples);
		assertEquals(6, tuplesOOCHall.size());

//TODO-gg del
        for (AnnotatedAtomTuple tuple : tuplesOOCHall)
        {
            System.out.println("dihedral for " +tuple.getAtomIDs() + " is " 
			+ MolecularUtils.calculateTorsionAngle(iac.getAtom(tuple.getAtomIDs().get(0)), 
													iac.getAtom(tuple.getAtomIDs().get(1)), 
													iac.getAtom(tuple.getAtomIDs().get(2)), 
													iac.getAtom(tuple.getAtomIDs().get(3))));
        }
		List<AnnotatedAtomTuple> tuplesOOCMin = getTuplesFromRuleName("OOCH_Min", tuples);
		assertEquals(1, tuplesOOCMin.size());
		assertEquals(4, tuplesOOCMin.get(0).getAtomIDs().size());
		assertEquals(7, tuplesOOCMin.get(0).getAtomIDs().get(0));
		assertEquals(1, tuplesOOCMin.get(0).getAtomIDs().get(1));
		assertEquals(0, tuplesOOCMin.get(0).getAtomIDs().get(2));
		assertEquals(4, tuplesOOCMin.get(0).getAtomIDs().get(3));
		List<AnnotatedAtomTuple> tuplesOOCMax = getTuplesFromRuleName("OOCH_Max", tuples);
		assertEquals(1, tuplesOOCMax.size());
		assertEquals(4, tuplesOOCMax.get(0).getAtomIDs().size());
		assertEquals(1, tuplesOOCMax.get(0).getAtomIDs().get(0));
		assertEquals(7, tuplesOOCMax.get(0).getAtomIDs().get(1));
		assertEquals(9, tuplesOOCMax.get(0).getAtomIDs().get(2));
		assertEquals(11, tuplesOOCMax.get(0).getAtomIDs().get(3));
		List<AnnotatedAtomTuple> tuplesOOCHCloseTo = getTuplesFromRuleName("OOCH_CloseTo", tuples);
		assertEquals(2, tuplesOOCHCloseTo.size());
		expectedAtomIDs = new HashSet<List<Integer>>(Arrays.asList(
			Arrays.asList(7, 1, 0, 3),
			Arrays.asList(7, 1, 0, 6)));
		for (AnnotatedAtomTuple tuple : tuplesOOCHCloseTo)
		{
			assertEquals(4, tuple.getAtomIDs().size());
			assertTrue(expectedAtomIDs.contains(tuple.getAtomIDs()));
		}
		List<AnnotatedAtomTuple> tuplesOOCHLessThan = getTuplesFromRuleName("OOCH_LessThan", tuples);
		assertEquals(2, tuplesOOCHLessThan.size());
		expectedAtomIDs = new HashSet<List<Integer>>(Arrays.asList(
			Arrays.asList(7, 1, 0, 4),
			Arrays.asList(7, 1, 0, 5)));
		for (AnnotatedAtomTuple tuple : tuplesOOCHLessThan)
		{
			assertEquals(4, tuple.getAtomIDs().size());
			assertTrue(expectedAtomIDs.contains(tuple.getAtomIDs()));
		}
		List<AnnotatedAtomTuple> tuplesOOCHMoreThan = getTuplesFromRuleName("OOCH_MoreThan", tuples);
		assertEquals(2, tuplesOOCHMoreThan.size());
		expectedAtomIDs = new HashSet<List<Integer>>(Arrays.asList(
			Arrays.asList(1, 7, 9, 10),
			Arrays.asList(1, 7, 9, 11)));
		for (AnnotatedAtomTuple tuple : tuplesOOCHMoreThan)
		{
			assertEquals(4, tuple.getAtomIDs().size());
			assertTrue(expectedAtomIDs.contains(tuple.getAtomIDs()));
		}
    }

//------------------------------------------------------------------------------

    private List<AnnotatedAtomTuple> getTuplesFromRuleName(String ruleName, List<AnnotatedAtomTuple> tuples)
    {
        return tuples.stream()
            .filter(tuple -> tuple.getValueOfAttribute(AtomTupleConstants.KEYRULENAME).equals(ruleName))
            .collect(Collectors.toList());
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseAtomTupleGeomConditions_singleGeomCondition()
    {
    	List<AtomTupleGeomCondition> result =
    			AtomTupleGenerator.parseAtomTupleGeomConditions("DISTANCE 0 1 CLOSE_TO 2.5");

    	assertEquals(1, result.size());
    	AtomTupleGeomCondition c = result.get(0);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.DISTANCE,
    			c.type);
    	assertEquals(Arrays.asList(0, 1), c.atomIndexes);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.CLOSE_TO,
    			c.operator);
    	assertEquals(2.5, c.value, 1e-10);
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseAtomTupleGeomConditions_multipleGeomConditions()
    {
    	String text = "Distance 0 1 cloSE_TO 2.5, anGle 1 2 3 LESS_than 90, "
    			+ "DIHEDRAL 0 1 2 3 MAx";
    	List<AtomTupleGeomCondition> result =
    			AtomTupleGenerator.parseAtomTupleGeomConditions(text);

    	assertEquals(3, result.size());

    	AtomTupleGeomCondition c0 = result.get(0);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.DISTANCE,
    			c0.type);
    	assertEquals(Arrays.asList(0, 1), c0.atomIndexes);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.CLOSE_TO,
    			c0.operator);
    	assertEquals(2.5, c0.value, 1e-10);

    	AtomTupleGeomCondition c1 = result.get(1);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.ANGLE,
    			c1.type);
    	assertEquals(Arrays.asList(1, 2, 3), c1.atomIndexes);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.LESS_THAN,
    			c1.operator);
    	assertEquals(90.0, c1.value, 1e-10);

    	AtomTupleGeomCondition c2 = result.get(2);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.DIHEDRAL,
    			c2.type);
    	assertEquals(Arrays.asList(0, 1, 2, 3), c2.atomIndexes);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.MAX,
    			c2.operator);
    	assertNull( c2.value, "Should be null if not specified");
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseAtomTupleGeomConditions_symbols()
    {
    	List<AtomTupleGeomCondition> result =
    			AtomTupleGenerator.parseAtomTupleGeomConditions(
					"distance 0 1 = 3.0, angle 1 2 3 < 90, dihedral 0 1 2 3 > 180");

    	assertEquals(3, result.size());

    	AtomTupleGeomCondition c0 = result.get(0);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.DISTANCE,
    			c0.type);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.CLOSE_TO,
    			c0.operator);
    	assertEquals(3.0, c0.value, 1e-10);

    	AtomTupleGeomCondition c1 = result.get(1);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.ANGLE,
    			c1.type);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.LESS_THAN,
    			c1.operator);
    	assertEquals(90.0, c1.value, 1e-10);

    	AtomTupleGeomCondition c2 = result.get(2);
    	assertEquals(AtomTupleGeomCondition.GeomConditionType.DIHEDRAL,
    			c2.type);
    	assertEquals(AtomTupleGeomCondition.GeomConditionOperator.MORE_THAN,
    			c2.operator);
    	assertEquals(180.0, c2.value, 1e-10);
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseAtomTupleGeomConditions_invalidGeomConditionThrows()
    {
    	assertThrows(IllegalArgumentException.class, () ->
    			AtomTupleGenerator.parseAtomTupleGeomConditions("DISTANCE 0 1"));
    	assertThrows(IllegalArgumentException.class, () ->
    			AtomTupleGenerator.parseAtomTupleGeomConditions("INVALID 0 1 CLOSE_TO 2.5"));
    	assertThrows(IllegalArgumentException.class, () ->
    			AtomTupleGenerator.parseAtomTupleGeomConditions("DISTANCE 0 1 2 3 4"));
    	assertThrows(IllegalArgumentException.class, () ->
    			AtomTupleGenerator.parseAtomTupleGeomConditions("DISTANCE 0 1 CLOSE_TO"));
    	assertThrows(IllegalArgumentException.class, () ->
    			AtomTupleGenerator.parseAtomTupleGeomConditions("DISTANCE 0 1 CLOSE_TO 2.5 3.0"));
    	assertThrows(IllegalArgumentException.class, () ->
    			AtomTupleGenerator.parseAtomTupleGeomConditions("DISTANCE 0 1 CLOSE_TO 2.5 INVALID"));
    }

//------------------------------------------------------------------------------

}
