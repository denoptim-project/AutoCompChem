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
    	
    	String result = NumberUtils.calculateNewValueWithUnits(exp, "1");
    	String[] splitBuNonDigits = result.trim().split("[a-z,A-Z]");
    	assertEquals(1, splitBuNonDigits.length);
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, "1Å");
    	assertEquals(result.length()-1,result.indexOf("Å"));
    	assertEquals(0,result.indexOf("3"));
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, "1 Å");
    	assertEquals(result.length()-1,result.indexOf("Å"));
    	assertEquals(0,result.indexOf("3"));
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, "Å1");
    	assertEquals(0,result.indexOf("Å"));
    	assertEquals(1,result.indexOf("3"));
    	
    	result = NumberUtils.calculateNewValueWithUnits(exp, "Å 1");
    	assertEquals(0,result.indexOf("Å"));
    	assertEquals(2,result.indexOf("3"));
    	
    	assertEquals("Å 3.12", NumberUtils.calculateNewValueWithUnits(exp, 
    			"Å 1.12"));
    	assertEquals("3GB", NumberUtils.calculateNewValueWithUnits(exp, 
    			"1GB"));
    	assertEquals("1002 MB", NumberUtils.calculateNewValueWithUnits(exp, 
    			"1000 MB"));
    	assertEquals("1002.4m", 
    			NumberUtils.calculateNewValueWithUnits("${x + 2.2}", 
    			"1000.2m"));
    	assertEquals("0.00097 kcal/mol", 
    			NumberUtils.calculateNewValueWithUnits("${x + 0.0022}", 
    			"-0.00123 kcal/mol"));
    	assertEquals("9.70E-6 kcal/mol", 
    			NumberUtils.calculateNewValueWithUnits("${x + 2.2E-5}", 
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
    	
    	assertTrue(NumberUtils.closeEnough(oldVal, (Double)
    			NumberUtils.calculateNewValue(expression, oldVal)));

    	oldVal = 10.0;
    	expression = "${x*x}";
    	assertTrue(NumberUtils.closeEnough(100.0, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));

    	oldVal = 10.2;
    	expression = "${x/2 - 5}";
    	assertTrue(NumberUtils.closeEnough(0.1, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));

    	oldVal = 2.0;
    	expression = "${x*2 + x*2}";
    	assertTrue(NumberUtils.closeEnough(8.0, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testCalculateNewValue_FunctionMapper() throws Exception
    {	
    	// Test sin function
    	// sin(0) = 0
    	String expression = "${sin(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// sin(π/2) ≈ 1
    	expression = "${sin(" + Math.PI/2 + ")}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// sin(π) ≈ 0
    	expression = "${sin(" + Math.PI + ")}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// sin(π/6) = 0.5
    	expression = "${sin(" + Math.PI/6 + ")}";
    	assertTrue(NumberUtils.closeEnough(0.5, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test cos function
    	// cos(0) = 1
    	expression = "${cos(0)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// cos(π/2) ≈ 0
    	expression = "${cos(" + Math.PI/2 + ")}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// cos(π) ≈ -1
    	expression = "${cos(" + Math.PI + ")}";
    	assertTrue(NumberUtils.closeEnough(-1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// cos(π/3) = 0.5
    	expression = "${cos(" + Math.PI/3 + ")}";
    	assertTrue(NumberUtils.closeEnough(0.5, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test tan function
    	// tan(0) = 0
    	expression = "${tan(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// tan(π/4) ≈ 1
    	expression = "${tan(" + Math.PI/4 + ")}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test trigonometric functions with variables
    	// sin(x) where x = π/2
    	double oldVal = Math.PI / 2;
    	expression = "${sin(x)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));
    	
    	// cos(x) where x = 0
    	oldVal = 0.0;
    	expression = "${cos(x)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));
    	
    	// tan(x) where x = π/4
    	oldVal = Math.PI / 4;
    	expression = "${tan(x)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));
    	
    	// Test combined expressions with trigonometric functions
    	// sin(x) + cos(x) where x = π/4, should be approximately √2 ≈ 1.414
    	oldVal = Math.PI / 4;
    	expression = "${sin(x) + cos(x)}";
    	double expected = Math.sin(Math.PI/4) + Math.cos(Math.PI/4);
    	assertTrue(NumberUtils.closeEnough(expected, 
    			(Double) NumberUtils.calculateNewValue(expression, oldVal)));

    	// Test inverse trigonometric functions
    	// asin(1) = π/2
    	expression = "${asin(1)}";
    	assertTrue(NumberUtils.closeEnough(Math.PI/2, (Double)
    			NumberUtils.calculateValueOfExpression(expression)));
    	
    	// asin(0) = 0
    	expression = "${asin(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// acos(0) = π/2
    	expression = "${acos(0)}";
    	assertTrue(NumberUtils.closeEnough(Math.PI/2, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// acos(1) = 0
    	expression = "${acos(1)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// atan(1) = π/4
    	expression = "${atan(1)}";
    	assertTrue(NumberUtils.closeEnough(Math.PI/4, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// atan(0) = 0
    	expression = "${atan(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// atan2(1, 1) = π/4
    	expression = "${atan2(1, 1)}";
    	assertTrue(NumberUtils.closeEnough(Math.PI/4, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// atan2(0, 1) = 0
    	expression = "${atan2(0, 1)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test exponential and logarithmic functions
    	// exp(0) = 1
    	expression = "${exp(0)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// exp(1) ≈ e
    	expression = "${exp(1)}";
    	assertTrue(NumberUtils.closeEnough(Math.E, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// log(1) = 0 (natural logarithm)
    	expression = "${log(1)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// log(e) = 1
    	expression = "${log(" + Math.E + ")}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// ln(e) = 1 (alias for log)
    	expression = "${ln(" + Math.E + ")}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// log10(1) = 0
    	expression = "${log10(1)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// log10(10) = 1
    	expression = "${log10(10)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// log10(100) = 2
    	expression = "${log10(100)}";
    	assertTrue(NumberUtils.closeEnough(2.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test power functions
    	// sqrt(4) = 2
    	expression = "${sqrt(4)}";
    	assertTrue(NumberUtils.closeEnough(2.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// sqrt(0) = 0
    	expression = "${sqrt(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// sqrt(9) = 3
    	expression = "${sqrt(9)}";
    	assertTrue(NumberUtils.closeEnough(3.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// pow(2, 3) = 8
    	expression = "${pow(2, 3)}";
    	assertTrue(NumberUtils.closeEnough(8.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// pow(5, 2) = 25
    	expression = "${pow(5, 2)}";
    	assertTrue(NumberUtils.closeEnough(25.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// pow(10, 0) = 1
    	expression = "${pow(10, 0)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test hyperbolic functions
    	// sinh(0) = 0
    	expression = "${sinh(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// cosh(0) = 1
    	expression = "${cosh(0)}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// tanh(0) = 0
    	expression = "${tanh(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test with known values for hyperbolic functions
    	double testVal = 1.0;
    	expression = "${sinh(" + testVal + ")}";
    	assertTrue(NumberUtils.closeEnough(Math.sinh(testVal), 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	expression = "${cosh(" + testVal + ")}";
    	assertTrue(NumberUtils.closeEnough(Math.cosh(testVal), 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	expression = "${tanh(" + testVal + ")}";
    	assertTrue(NumberUtils.closeEnough(Math.tanh(testVal), 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test rounding and absolute value functions
    	// abs(5) = 5
    	expression = "${abs(5)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// abs(-5) = 5
    	expression = "${abs(-5)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// abs(0) = 0
    	expression = "${abs(0)}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// ceil(4.3) = 5
    	expression = "${ceil(4.3)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// ceil(-4.3) = -4
    	expression = "${ceil(-4.3)}";
    	assertTrue(NumberUtils.closeEnough(-4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// ceil(4.0) = 4
    	expression = "${ceil(4.0)}";
    	assertTrue(NumberUtils.closeEnough(4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// floor(4.7) = 4
    	expression = "${floor(4.7)}";
    	assertTrue(NumberUtils.closeEnough(4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// floor(-4.7) = -5
    	expression = "${floor(-4.7)}";
    	assertTrue(NumberUtils.closeEnough(-5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// floor(4.0) = 4
    	expression = "${floor(4.0)}";
    	assertTrue(NumberUtils.closeEnough(4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// round(4.3) = 4
    	expression = "${round(4.3)}";
    	assertTrue(NumberUtils.closeEnough(4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// round(4.7) = 5
    	expression = "${round(4.7)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// round(-4.3) = -4
    	expression = "${round(-4.3)}";
    	assertTrue(NumberUtils.closeEnough(-4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// round(-4.7) = -5
    	expression = "${round(-4.7)}";
    	assertTrue(NumberUtils.closeEnough(-5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test min/max functions
    	// max(5, 3) = 5
    	expression = "${max(5, 3)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// max(-5, -3) = -3
    	expression = "${max(-5, -3)}";
    	assertTrue(NumberUtils.closeEnough(-3.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// max(5, 5) = 5
    	expression = "${max(5, 5)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// min(5, 3) = 3
    	expression = "${min(5, 3)}";
    	assertTrue(NumberUtils.closeEnough(3.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// min(-5, -3) = -5
    	expression = "${min(-5, -3)}";
    	assertTrue(NumberUtils.closeEnough(-5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// min(5, 5) = 5
    	expression = "${min(5, 5)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// sqrt(x) where x = 4
    	expression = "${sqrt(4.0)}";
    	assertTrue(NumberUtils.closeEnough(2.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// exp(x) where x = 1
    	expression = "${exp(1.0)}";
    	assertTrue(NumberUtils.closeEnough(Math.E, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// log(x) where x = e
    	expression = "${log(" + Math.E + ")}";
    	assertTrue(NumberUtils.closeEnough(1.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// abs(x) where x = -5
    	expression = "${abs(-5.0)}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// Test combined expressions with multiple functions
    	// sqrt(pow(3, 2)) = 3
    	expression = "${sqrt(pow(3, 2))}";
    	assertTrue(NumberUtils.closeEnough(3.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// exp(log(5)) = 5
    	expression = "${exp(log(5))}";
    	assertTrue(NumberUtils.closeEnough(5.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// abs(sin(π)) = 0
    	expression = "${abs(sin(" + Math.PI + "))}";
    	assertTrue(NumberUtils.closeEnough(0.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    	
    	// max(sqrt(16), pow(2, 2)) = max(4, 4) = 4
    	expression = "${max(sqrt(16), pow(2, 2))}";
    	assertTrue(NumberUtils.closeEnough(4.0, 
    			(Double) NumberUtils.calculateValueOfExpression(expression)));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testParseArrayOfInt() throws Exception
    {
    	int[] array = NumberUtils.parseArrayOfInt("[100, 2, -3]");
    	assertEquals(3, array.length);
    	assertEquals(100, array[0]);
    	assertEquals(2, array[1]);
    	assertEquals(-3, array[2]);
    	
    	array = NumberUtils.parseArrayOfInt(" [   100,2, -3 ]   ");
    	assertEquals(3, array.length);
    	assertEquals(100, array[0]);
    	assertEquals(2, array[1]);
    	assertEquals(-3, array[2]);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testParseArrayOfDouble() throws Exception
    {
    	double[] array = NumberUtils.parseArrayOfDouble("[10.0, 2.2, -3.3]");
    	assertEquals(3, array.length);
    	assertTrue(NumberUtils.closeEnough(10.0, array[0]));
    	assertTrue(NumberUtils.closeEnough(2.2, array[1]));
    	assertTrue(NumberUtils.closeEnough(-3.3, array[2]));
    	
    	array = NumberUtils.parseArrayOfDouble("  [  10.0  , 2.2  ,  -3.3  ] ");
    	assertEquals(3, array.length);
    	assertTrue(NumberUtils.closeEnough(10.0, array[0]));
    	assertTrue(NumberUtils.closeEnough(2.2, array[1]));
    	assertTrue(NumberUtils.closeEnough(-3.3, array[2]));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testcalculateNewValue() throws Exception
    {
    	// Test 1: Simple expression without formatting
    	String expr = "${x + 2}";
    	Double oldVal = 10.0;
    	String result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	// Should return "12.0" as string (default formatting)
    	assertTrue(result.contains("12"));
    	
    	// Test 2: Using format function with fixed decimal places
    	expr = "${format('0.00', x + 2)}";
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertEquals("12.00", result);
    	
    	// Test 3: Using format function with up to 2 decimal places
    	expr = "${format('#.##', x * 1.234)}";
    	oldVal = 10.0;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertEquals("12.34", result);

    	// Test 3b: Using format function to round to integer
    	expr = "${format('0', x * 1.289)}";
    	oldVal = 10.0;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
		// NB: rounding!
    	assertEquals("13", result);

    	// Test 3b: Using format function to round
    	expr = "${format('0.0', 12.89)}";
    	oldVal = 10.0;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
		// NB: rounding!
    	assertEquals("12.9", result);
    	
    	// Test 4: Using format function with scientific notation
    	expr = "${format('0.0E0', x * 100)}";
    	result = NumberUtils.calculateNewValue(expr, 1.0).toString();
    	assertTrue(result.contains("E"));
    	assertTrue(result.contains("1"));
    	
    	// Test 5: Complex expression with formatting
    	expr = "${format('0.000', x * 2.5 + 1)}";
    	oldVal = 3.0;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertEquals("8.500", result);
    	
    	// Test 6: Negative numbers with formatting
    	expr = "${format('0.00', x - 5)}";
    	oldVal = 2.0;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertEquals("-3.00", result);
    	
    	// Test 7: Zero with formatting
    	expr = "${format('0.00', x - x)}";
    	oldVal = 5.5;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertEquals("0.00", result);
    	
    	// Test 8: Large number with formatting
    	expr = "${format('#,##0.00', x * 1000)}";
    	oldVal = 1234.567;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	// Format adds commas, so check for the digits without commas
		assertTrue(result.contains("1,234,567"));
		assertTrue(result.replace(",", "").contains("1234567"));
    	
    	// Test 9: Small number with formatting
    	expr = "${format('0.0000', x / 1000)}";
    	oldVal = 1.23456;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertTrue(result.contains("0.0012"));
    	
    	// Test 10: Simple expression (no format function) - should still work
    	expr = "${x * 2}";
    	oldVal = 5.0;
    	result = NumberUtils.calculateNewValue(expr, oldVal).toString();
    	assertTrue(result.contains("10"));
    }
    
//------------------------------------------------------------------------------

}
