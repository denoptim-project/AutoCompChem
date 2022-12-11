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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

}
