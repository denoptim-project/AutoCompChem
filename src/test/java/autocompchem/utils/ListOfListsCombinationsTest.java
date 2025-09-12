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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for iterator over combinations in a list of lists.
 * 
 * @author Marco Foscato
 */

public class ListOfListsCombinationsTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testIterator() throws Exception
    {
    	List<List<String>> listOfLists = new ArrayList<List<String>>();
    	listOfLists.add(Arrays.asList("A","B","C"));
    	listOfLists.add(Arrays.asList("1","2"));
    	listOfLists.add(Arrays.asList("a","b","c","d"));
    	listOfLists.add(Arrays.asList("@"));
    	
    	Iterator<List<String>> iter = new ListOfListsCombinations<String>(
    			listOfLists);
    	
    	List<String> results = new ArrayList<String>();
    	while (iter.hasNext())
    	{
    		List<String> combination = iter.next();
    		String s = StringUtils.mergeListToString(combination, "");
    		results.add(s);
    	}
    	
    	List<String> expected = Arrays.asList("A1a@",
    			"A1b@",
    			"A1c@",
    			"A1d@",
    			"A2a@",
    			"A2b@",
    			"A2c@",
    			"A2d@",
    			"B1a@",
    			"B1b@",
    			"B1c@",
    			"B1d@",
    			"B2a@",
    			"B2b@",
    			"B2c@",
    			"B2d@",
    			"C1a@",
    			"C1b@",
    			"C1c@",
    			"C1d@",
    			"C2a@",
    			"C2b@",
    			"C2c@",
    			"C2d@");
    	
    	assertEquals(expected.size(), results.size());
    	for (int i=0; i<expected.size(); i++)
    		assertEquals(expected.get(i), results.get(i));
    	
    	// From here we test iteration over selected combinations
    	
    	List<int[]> selectedCombos = new ArrayList<int[]>();
    	selectedCombos.add(new int[]{0, 0, 3, 0});
    	selectedCombos.add(new int[]{2, 1, 3, 0});
    	selectedCombos.add(new int[]{1, 0, 1, 0});
    	
    	expected = Arrays.asList(
    			"A1d@",
    			"C2d@",
    			"B1b@");
    	
    	results = new ArrayList<String>();
    	
    	Iterator<List<String>> iterSel = new ListOfListsCombinations<String>(
    			listOfLists, selectedCombos);
    	while (iterSel.hasNext())
    	{
    		List<String> combination = iterSel.next();
    		String s = StringUtils.mergeListToString(combination, "");
    		results.add(s);
    	}
    	
    	assertEquals(expected.size(), results.size());
    	for (int i=0; i<expected.size(); i++)
    		assertEquals(expected.get(i), results.get(i));
    }

//------------------------------------------------------------------------------

    @Test
    public void testInconsistentArgs() throws Exception
    {
    	List<List<String>> listOfLists = new ArrayList<List<String>>();
    	listOfLists.add(Arrays.asList("A","B","C"));
    	listOfLists.add(Arrays.asList("1","2"));
    	listOfLists.add(Arrays.asList("a","b","c","d"));
    	listOfLists.add(Arrays.asList("@"));    	

    	// Test inconsistent size of the combination identifier (too short)
    	List<int[]> selectedCombos = new ArrayList<int[]>();
    	selectedCombos.add(new int[2]);
    	
        assertThrows(IllegalArgumentException.class, 
        		() -> new ListOfListsCombinations<String>(listOfLists, 
        				selectedCombos)); 	

    	// Test inconsistent size of the combination identifier (too long)
    	List<int[]> selectedCombos2 = new ArrayList<int[]>();
    	selectedCombos2.add(new int[5]);
    	
        assertThrows(IllegalArgumentException.class, 
        		() -> new ListOfListsCombinations<String>(listOfLists, 
        				selectedCombos2));

    	// Test inconsistent index in the combination identifier (negative)
    	List<int[]> selectedCombos3 = new ArrayList<int[]>();
    	selectedCombos3.add(new int[] {0, -1, 0, 0});
    	
        assertThrows(IndexOutOfBoundsException.class, 
        		() -> new ListOfListsCombinations<String>(listOfLists, 
        				selectedCombos3));

    	// Test inconsistent index in the combination identifier (out of range)
    	List<int[]> selectedCombos4 = new ArrayList<int[]>();
    	selectedCombos4.add(new int[] {0, 0, 4, 0});
    	
        assertThrows(IndexOutOfBoundsException.class, 
        		() -> new ListOfListsCombinations<String>(listOfLists, 
        				selectedCombos4));
    }

//------------------------------------------------------------------------------

    @Test
    public void testIteratorOnEmpty() throws Exception
    {
    	List<List<String>> listOfLists = new ArrayList<List<String>>();
    	
    	Iterator<List<String>> iter = new ListOfListsCombinations<String>(
    			listOfLists);
    	assertFalse(iter.hasNext());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testIteratorOnEmptyInner() throws Exception
    {
    	List<List<String>> listOfLists = new ArrayList<List<String>>();
    	listOfLists.add(Arrays.asList());
    	
    	Iterator<List<String>> iter = new ListOfListsCombinations<String>(
    			listOfLists);
    	
    	assertFalse(iter.hasNext());
    }
    
//------------------------------------------------------------------------------

}
