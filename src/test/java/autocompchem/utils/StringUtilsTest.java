package autocompchem.utils;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import autocompchem.text.TextAnalyzer;


/**
 * Unit Test for string utilities
 * 
 * @author Marco Foscato
 */

public class StringUtilsTest 
{
	
//------------------------------------------------------------------------------
	
	@Test
	public void testParseArrayOfDoubles() throws Exception
	{
		String s = "1.234 5.678 -1.987 -23 -0.5";
		double[] values = StringUtils.parseArrayOfDoubles(s, "\\s+");
		
		assertEquals(5, values.length);
		assertTrue(NumberUtils.closeEnough(1.234, values[0]));
		assertTrue(NumberUtils.closeEnough(5.678, values[1]));
		assertTrue(NumberUtils.closeEnough(-1.987, values[2]));
		assertTrue(NumberUtils.closeEnough(-23.0, values[3]));
		assertTrue(NumberUtils.closeEnough(-0.5, values[4]));
		
		s = "1.234, 5.678, -1.987, -23, -0.5";
		values = StringUtils.parseArrayOfDoubles(s, ",");

		assertEquals(5, values.length);
		assertTrue(NumberUtils.closeEnough(1.234, values[0]));
		assertTrue(NumberUtils.closeEnough(5.678, values[1]));
		assertTrue(NumberUtils.closeEnough(-1.987, values[2]));
		assertTrue(NumberUtils.closeEnough(-23.0, values[3]));
		assertTrue(NumberUtils.closeEnough(-0.5, values[4]));
		
		s = "val:1.234 blabla:5.678 tomi:-1.987 meni:-23 jacu:-0.5";
		values = StringUtils.parseArrayOfDoubles(s, "(?i)\\w+:");

		assertEquals(5, values.length);
		assertTrue(NumberUtils.closeEnough(1.234, values[0]));
		assertTrue(NumberUtils.closeEnough(5.678, values[1]));
		assertTrue(NumberUtils.closeEnough(-1.987, values[2]));
		assertTrue(NumberUtils.closeEnough(-23.0, values[3]));
		assertTrue(NumberUtils.closeEnough(-0.5, values[4]));
	}

//------------------------------------------------------------------------------

    @Test
    public void testMergeListToString() throws Exception
    {
        ArrayList<String> lst = new ArrayList<String>();
        lst.add("e1");
        lst.add("e2");
        lst.add("e3");
        String sep = "@";
        
        String res = StringUtils.mergeListToString(lst, sep);
        
        assertEquals(3,TextAnalyzer.countStringInLine(sep, res),
        		"Number of separators ");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testMergeIntListToString() throws Exception
    {
        List<Integer> lst = new ArrayList<Integer>();
        lst.add(0);
        lst.add(3);
        lst.add(-3);
        String sep = ", ";
        
        String expected = "1, 4, -2";
        assertEquals(expected, StringUtils.mergeListToString(lst,sep,true,1));

        expected = "0, 3, -3, ";
        assertEquals(expected, StringUtils.mergeListToString(lst,sep,false,0));

        expected = "   1@   4@  -2";
        assertEquals(expected, StringUtils.mergeListToString(lst, 
        		Locale.ENGLISH, "%4d", "@", true, 1));
        
        expected = "__    0@__    3@__   -3@";
        assertEquals(expected, StringUtils.mergeListToString(lst, 
        		Locale.ENGLISH, "__%5d", "@", false, 0));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testMergeListToStringTrimmed() throws Exception
    {
        ArrayList<String> lst = new ArrayList<String>();
        lst.add("e1");
        lst.add("e2");
        lst.add("e3");
        String sep = "@";
        
        String res = StringUtils.mergeListToString(lst, sep, true);
        
        assertEquals(2,TextAnalyzer.countStringInLine(sep, res),
        		"Number of separators ");
    }

//------------------------------------------------------------------------------

    @Test
    public void testCount() throws Exception
    {
    	String s = "TRGAadfTR_G sdg hjnujTRGk fhjx vbn xTRGvbx vbn xbn TRG";
    	assertEquals(4,StringUtils.countMatches(s, "TRG"),"Number of matches");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testMakeStringForIndexes() throws Exception
    {
    	List<Integer> ids = new ArrayList<Integer>(Arrays.asList(
    			1,2,3,            // range A
    			6,7,8,          // range B
    			10, 
    			22,23,24,25,26,   // range C
    			77,
    			56,
    			32,   //In range D
    			45,
    			33,   //In range D
    			67,
    			34)); //In range D
    	
    	List<String> expected = new ArrayList<String>(Arrays.asList(
    			"1-3",
    			"6-8",
    			"10",
    			"22-26",
    			"32-34",
    			"45",
    			"56",
    			"67",
    			"77"));
    	assertEquals(expected, StringUtils.makeStringForIndexes(ids,"-"));
    	
    	expected = new ArrayList<String>(Arrays.asList(
    			"-1:1",
    			"4:6",
    			"8",
    			"20:24",
    			"30:32",
    			"43",
    			"54",
    			"65",
    			"75"));
    	assertEquals(expected, StringUtils.makeStringForIndexes(ids,":",-2));
    	
    	expected = new ArrayList<String>(Arrays.asList(
    			"2-4",
    			"7-9",
    			"11",
    			"23-27",
    			"33-35",
    			"46",
    			"57",
    			"68",
    			"78"));
    	assertEquals(expected, StringUtils.makeStringForIndexes(ids,"-",1));
    	
    	Set<Integer> set = new HashSet<Integer>();
    	set.add(9);
    	set.add(4);
    	set.add(7);
    	set.add(0);
    	set.add(3);
    	set.add(8);
    	set.add(22);
    	set.add(2);
    	expected = new ArrayList<String>(Arrays.asList(
    			"0",
    			"2-4",
    			"7-9",
    			"22"));
    	assertEquals(expected, StringUtils.makeStringForIndexes(set,"-",0));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsAtomID() throws Exception
    {
    	String s = "H1";
    	assertTrue(StringUtils.isAtomID(s));
    	s = "Any134";
    	assertTrue(StringUtils.isAtomID(s));
    	s = "Any0";
    	assertTrue(StringUtils.isAtomID(s));
    	s = "Any134ade";
    	assertFalse(StringUtils.isAtomID(s));
    	s = "134ade";
    	assertFalse(StringUtils.isAtomID(s));
    	s = "123";
    	assertFalse(StringUtils.isAtomID(s));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSplitCharactersAndNumber() throws Exception
    {
    	String s = "abc123";
    	String[] parts = StringUtils.splitCharactersAndNumber(s);
    	assertEquals(2, parts.length);
    	assertEquals("abc", parts[0]);
    	assertEquals("123", parts[1]);
    	
    	s = "abc";
    	parts = StringUtils.splitCharactersAndNumber(s);
    	assertEquals(2, parts.length);
    	assertEquals("abc", parts[0]);
    	assertEquals("", parts[1]);
    	
    	s = "123";
    	parts = StringUtils.splitCharactersAndNumber(s);
    	assertEquals(2, parts.length);
    	assertEquals("", parts[0]);
    	assertEquals("123", parts[1]);
    	
    	s = "abc123cfr";
    	parts = StringUtils.splitCharactersAndNumber(s);
    	assertEquals(2, parts.length);
    	assertEquals("abc", parts[0]);
    	assertEquals("123cfr", parts[1]);
    	
    	s = "abc123cfr567";
    	parts = StringUtils.splitCharactersAndNumber(s);
    	assertEquals(2, parts.length);
    	assertEquals("abc", parts[0]);
    	assertEquals("123cfr567", parts[1]);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetParenthesesContent() throws Exception
    {
    	String s = "no parethesis";
    	String result = StringUtils.getParenthesesContent(s);
    	assertNull(result);
    	
    	s = "other (content with (nested))";
    	result = StringUtils.getParenthesesContent(s);
    	assertNotNull(result);
    	assertEquals("content with (nested)",result);
    	
    	s = "(content with (nested)  ) more";
    	result = StringUtils.getParenthesesContent(s);
    	assertNotNull(result);
    	assertEquals("content with (nested)  ",result);
    	
    	s = "other (content with (nested) more";
    	result = StringUtils.getParenthesesContent(s);
    	assertNotNull(result);
    	assertEquals("content with (nested) more",result);
    	
    	s = "other (content without end";
    	result = StringUtils.getParenthesesContent(s);
    	assertNotNull(result);
    	assertEquals("content without end",result);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetEnclosedContent() throws Exception
    {
    	char openChar = "[".charAt(0);
    	char closeChar = "%".charAt(0);
    	
    	String s = "no match";
    	String result = StringUtils.getEnclosedContent(s, openChar, closeChar);
    	assertNull(result);
    	
    	s = "other [content with [nested%%";
    	result = StringUtils.getEnclosedContent(s, openChar, closeChar);
    	assertNotNull(result);
    	assertEquals("content with [nested%",result);
    	
    	s = "other [content without end";
    	result = StringUtils.getEnclosedContent(s, openChar, closeChar);
    	assertNotNull(result);
    	assertEquals("content without end",result);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testParseBoolean() throws Exception
    {
    	for (String s : StringUtils.TRUE_VALUES)
    	{
    		assertTrue(StringUtils.parseBoolean(s));
    		assertTrue(StringUtils.parseBoolean(s.toLowerCase()));
    	}
    	for (String s : StringUtils.FALSE_VALUES)
    	{
    		assertFalse(StringUtils.parseBoolean(s));
    		assertFalse(StringUtils.parseBoolean(s.toLowerCase()));
    	}
    	
    	boolean hasTrhown = false;
    	try {
    		StringUtils.parseBoolean("string");
    	} catch (IllegalArgumentException iae) {
    		hasTrhown = true;
    	}
    	assertTrue(hasTrhown);
    	
		assertTrue(StringUtils.parseBoolean(null, true));
		assertNull(StringUtils.parseBoolean(null, false));
		assertTrue(StringUtils.parseBoolean("", true));
		hasTrhown = false;
    	try {
    		StringUtils.parseBoolean("", false);
    	} catch (IllegalArgumentException iae) {
    		hasTrhown = true;
    	}
    	assertTrue(hasTrhown);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsValidRegex() throws Exception
    {
    	assertTrue(StringUtils.isValidRegex("foobar"));
    	assertTrue(StringUtils.isValidRegex(".*[a-z]+\\d+"));
    	assertFalse(StringUtils.isValidRegex("*"));
    	assertFalse(StringUtils.isValidRegex("[abc"));
    	assertFalse(StringUtils.isValidRegex("ab\\"));
    }
    			
//------------------------------------------------------------------------------
    
    @Test
    public void testHasSyntaxOfCommandCallWithParenthesesContent() throws Exception
    {
        String commandCall = "GETDATA";
        
        // Test 1: Valid - simple command with parentheses content
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1, arg2)", commandCall));
        
        // Test 2: Valid - case insensitive
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "getdata(arg1, arg2)", commandCall));
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GetData(arg1, arg2)", commandCall));
        
        // Test 3: Valid - with leading/trailing whitespace
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "  GETDATA(arg1, arg2)  ", commandCall));
        // Test 3b: Valid - with space between command and '(' (stripped)
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA (arg1, arg2)", commandCall));
        
        // Test 4: Valid - empty parentheses content
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA()", commandCall));
        
        // Test 5: Valid - nested parentheses
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1, (nested, content))", commandCall));
        
        // Test 6: Valid - single argument
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1)", commandCall));
        
        // Test 7: Invalid - no parentheses
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA", commandCall));
        
        // Test 8: Invalid - no parentheses content after command
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA ", commandCall));
        
        // Test 9: Invalid - wrong command
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "OTHER(arg1, arg2)", commandCall));
        
        // Test 10: Invalid - text after parentheses
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1, arg2) extra", commandCall));
        
        // Test 11: Invalid - text before command
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "prefix GETDATA(arg1, arg2)", commandCall));
        
        // Test 12: Invalid - no opening parenthesis
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA arg1, arg2", commandCall));
        
        // Test 13: Invalid - no closing parenthesis (unclosed)
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1, arg2", commandCall));
        
        // Test 14: Valid - complex content with commas and spaces
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(#0, path, to, data)", commandCall));
        
        // Test 15: Valid - with trailing whitespace after closing paren
        assertTrue(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1)   ", commandCall));
        
        // Test 16: Invalid - content after closing paren (even with whitespace)
        assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
            "GETDATA(arg1) more text", commandCall));
        
		// Test 17: Invalid - content has no command
		assertFalse(StringUtils.hasSyntaxOfCommandCallWithParenthesesContent(
			"(arg1, arg2) more text", commandCall));
    }
    
//------------------------------------------------------------------------------
			
}
