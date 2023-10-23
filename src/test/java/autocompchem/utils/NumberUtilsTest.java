package autocompchem.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.el.ExpressionFactory;


/**
 * Unit Test for number utilities
 * 
 * @author Marco Foscato
 */

public class NumberUtilsTest 
{
	//------------------------------------------------------------------------------

    @Test
    public void testIsNumber() throws Exception
    {
    	assertTrue(NumberUtils.isNumber("1.01"));
    	assertTrue(NumberUtils.isNumber("1.00"));
    	assertTrue(NumberUtils.isNumber("1"));
    	assertTrue(NumberUtils.isNumber("1.00E10"));
    	assertTrue(NumberUtils.isNumber("1.01E01"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testIsParsableToInt() throws Exception
    {
    	String s = "1234567890";
    	assertTrue(NumberUtils.isParsableToInt(s),"Simple digits");
    	s = "123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Simple digits w/ point");
    	
    	s = " 1234567890 ";
    	assertTrue(NumberUtils.isParsableToInt(s),"Leading/traling spaces ");
    	s = "123456 7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Space in the middle");
    	
    	s = "+1234567890";
    	assertTrue(NumberUtils.isParsableToInt(s),"Signed digits");
    	s = "+123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Signed digits w/ point");
    	s = "-234567890";
    	assertTrue(NumberUtils.isParsableToInt(s),"Signed(-) digits");
    	s = "-123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Signed(-) digits w/ point");
    	
    	s = "i1234567890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Complex");
    	s = "-i123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Complex w/ point");
    	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetcomplementary() throws Exception
    {
    	List<Integer> ids = new ArrayList<Integer>(Arrays.asList(1,2,3,
    			5, 7, 7, 5));
    	
    	Set<Integer> expected = new HashSet<Integer>();
    	expected.add(0);
    	expected.add(4);
    	expected.add(6);
    	expected.add(8);
    	expected.add(8);
    	
    	assertEquals(expected,NumberUtils.getComplementaryIndexes(ids, 9));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testStringUnits() throws Exception
    {
    	assertEquals("12", NumberUtils.stripUnits(" 12"));
    	assertEquals("12", NumberUtils.stripUnits(" 12 "));
    	assertEquals("-12", NumberUtils.stripUnits("-12"));
    	assertEquals("-12", NumberUtils.stripUnits(" -12"));
    	assertEquals("0.123", NumberUtils.stripUnits("0.123"));
    	assertEquals("-0.123", NumberUtils.stripUnits("-0.123"));
    	assertEquals("0.123", NumberUtils.stripUnits("$0.123"));
    	assertEquals("0.123", NumberUtils.stripUnits("0.123$"));
    	assertEquals("0.123", NumberUtils.stripUnits("USD 0.123"));
    	assertEquals("0.123", NumberUtils.stripUnits("0.123 USD"));

    	assertEquals("0. 1 2 3", NumberUtils.stripUnits(" 0. 1 2 3 "));
    	assertEquals("0. 1 2 3", NumberUtils.stripUnits("eur0. 1 2 3 "));
    	assertEquals("0. 1 2 3", NumberUtils.stripUnits(" 0. 1 2 3eur"));

    	assertEquals("0.12E-12", NumberUtils.stripUnits("0.12E-12"));
    	assertEquals("0.12E-12", NumberUtils.stripUnits("as d gf g0.12E-12"));
    	assertEquals("0.12E-12", NumberUtils.stripUnits("0.12E-12 asd r t%&"));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testCalculateNewValueWithUnits() throws Exception
    {
    	String exp = "${x + 2}";
    	ExpressionFactory ef = ExpressionFactory.newInstance();
    	
    	String result = NumberUtils.calculateNewValueWithUnits(exp, ef, "1");
    	String[] splitBuNonDigits = result.trim().split("[a-z,A-Z]");
    	assertEquals(1, splitBuNonDigits.length);
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, ef, "1Å");
    	assertEquals(result.length()-1,result.indexOf("Å"));
    	assertEquals(0,result.indexOf("3"));
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, ef, "1 Å");
    	assertEquals(result.length()-1,result.indexOf("Å"));
    	assertEquals(0,result.indexOf("3"));
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, ef, "Å1");
    	assertEquals(0,result.indexOf("Å"));
    	assertEquals(1,result.indexOf("3"));
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, ef, "Å 1");
    	assertEquals(0,result.indexOf("Å"));
    	assertEquals(2,result.indexOf("3"));
    	
    	assertEquals("Å 3.12", NumberUtils.calculateNewValueWithUnits(exp, ef, 
    			"Å 1.12"));
    	assertEquals("3GB", NumberUtils.calculateNewValueWithUnits(exp, ef, 
    			"1GB"));
    	assertEquals("1002 MB", NumberUtils.calculateNewValueWithUnits(exp, ef, 
    			"1000 MB"));
    	assertEquals("1002.4m", 
    			NumberUtils.calculateNewValueWithUnits("${x + 2.2}", ef, 
    			"1000.2m"));
    	assertEquals("0.00097 kcal/mol", 
    			NumberUtils.calculateNewValueWithUnits("${x + 0.0022}", ef, 
    			"-0.00123 kcal/mol"));
    	assertEquals("9.70E-6 kcal/mol", 
    			NumberUtils.calculateNewValueWithUnits("${x + 2.2E-5}", ef, 
    			"-1.23E-5 kcal/mol"));
    }
    
//------------------------------------------------------------------------------
    
	@Test
    public void testDetectDecimalFormat() throws Exception
    {
    	assertEquals("1", NumberUtils.detectDecimalFormat("1").format(1.000));
    	assertEquals("1", NumberUtils.detectDecimalFormat(" 1").format(1.000));
    	assertEquals("1", NumberUtils.detectDecimalFormat("1 ").format(1.000));    	

    	// NB: rounding!
    	assertEquals("1235", NumberUtils.detectDecimalFormat("1")
    			.format(1234.56));
    	assertEquals("12,345,678.90", NumberUtils.detectDecimalFormat(
    			"10,000,000.00").format(12345678.90));
    	
    	// Integer positions should be optional
    	assertEquals("1", NumberUtils.detectDecimalFormat("10000000")
    			.format(0.9));
    	assertEquals("1", NumberUtils.detectDecimalFormat("10,000,000")
    			.format(0.9));
    	
    	// Integer positions should be extensible
    	assertEquals("123456", NumberUtils.detectDecimalFormat("10")
    			.format(123456));
    	assertEquals("1235", NumberUtils.detectDecimalFormat("10")
    			.format(1234.56)); // NB: rounding
    	
    	assertEquals("1.1", NumberUtils.detectDecimalFormat("1.1")
    			.format(1.123));
    	assertEquals("1.1", NumberUtils.detectDecimalFormat(" 1.1")
    			.format(1.123));
    	assertEquals("1.1", NumberUtils.detectDecimalFormat("1.1 ")
    			.format(1.123));
    	
    	assertEquals("1.120", NumberUtils.detectDecimalFormat("0.123")
    			.format(1.12));
    	assertEquals("1.000", NumberUtils.detectDecimalFormat("0.123")
    			.format(1));
    	assertEquals("1.000", NumberUtils.detectDecimalFormat("-0.123")
    			.format(1));
    	assertEquals("-1.100", NumberUtils.detectDecimalFormat("-0.123")
    			.format(-1.1));
    	assertEquals("12,345.67", NumberUtils.detectDecimalFormat("1,234.56")
    			.format(12345.67));

    	assertEquals("-1.23E3", NumberUtils.detectDecimalFormat("-1.23E3")
    			.format(-1234.56));
    	assertEquals("-1.23E-3", NumberUtils.detectDecimalFormat("-1.23E-3")
    			.format(-0.00123456));
    	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSpaceBetweenValueAndUnits() throws Exception
    {
    	assertTrue(NumberUtils.spaceBetweenValueAndUnits("2 m", "m"));
    	assertTrue(NumberUtils.spaceBetweenValueAndUnits("m 2", "m"));
    	assertFalse(NumberUtils.spaceBetweenValueAndUnits("m2", "m"));
    	assertFalse(NumberUtils.spaceBetweenValueAndUnits("2m", "m"));
    	assertFalse(NumberUtils.spaceBetweenValueAndUnits("2", "m"));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUnitsAreInFront() throws Exception
    {
    	assertTrue(NumberUtils.unitsAreInFront("$1232.0", "$"));
    	assertTrue(NumberUtils.unitsAreInFront(" $1232.0", "$"));
    	assertTrue(NumberUtils.unitsAreInFront("$ 1232.0", "$"));
    	assertTrue(NumberUtils.unitsAreInFront("$ 1232.0$", "$"));
    	assertTrue(NumberUtils.unitsAreInFront("Å 1", "Å"));
    	assertFalse(NumberUtils.unitsAreInFront("Å 1", "$"));
    	assertFalse(NumberUtils.unitsAreInFront("1", "$"));
    	assertTrue(NumberUtils.unitsAreInFront("$ 0 $ 2 $", "$"));    	

    	assertFalse(NumberUtils.unitsAreInFront("2.0$", "$"));
    	assertFalse(NumberUtils.unitsAreInFront("2.0 $", "$"));
    	assertFalse(NumberUtils.unitsAreInFront("2.4$ $2", "$"));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testCalculateNewValue() throws Exception
    {
    	double oldVal = 10.0;
    	String expression = "${x}";
    	ExpressionFactory expFact = ExpressionFactory.newInstance();
    	
    	assertTrue(NumberUtils.closeEnough(oldVal, 
    			NumberUtils.calculateNewValue(expression, expFact, oldVal)));

    	oldVal = 10.0;
    	expression = "${x*x}";
    	assertTrue(NumberUtils.closeEnough(100.0, 
    			NumberUtils.calculateNewValue(expression, expFact, oldVal)));

    	oldVal = 10.2;
    	expression = "${x/2 - 5}";
    	assertTrue(NumberUtils.closeEnough(0.1, 
    			NumberUtils.calculateNewValue(expression, expFact, oldVal)));

    	oldVal = 2.0;
    	expression = "${a*2 + b*2}"; //NB: both a and b are mapped to same value
    	assertTrue(NumberUtils.closeEnough(8.0, 
    			NumberUtils.calculateNewValue(expression, expFact, oldVal)));
    }
    
//------------------------------------------------------------------------------

}
