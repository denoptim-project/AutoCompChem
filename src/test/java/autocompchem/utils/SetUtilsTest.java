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
 * Unit Test for set utilities
 * 
 * @author Marco Foscato
 */

public class SetUtilsTest 
{
	
//------------------------------------------------------------------------------
	
	@Test
	public void testGetIntersection() throws Exception
	{
		Set<Integer> setA = new HashSet<>(Arrays.asList(1,2,3,4,5,6));
		Set<Integer> setB = new HashSet<>(Arrays.asList(1,20,3,40,50,6));
		Set<Integer> expected = new HashSet<>(Arrays.asList(1,3,6));
		Set<Integer> actual = SetUtils.getIntersection(setA, setB);
		assertEquals(expected,actual);
		assertEquals(6,setA.size());
		assertEquals(6,setB.size());
		
		Set<String> setAS = new HashSet<>(Arrays.asList("1","2","3","4","5"));
		Set<String> setBS = new HashSet<>(Arrays.asList("4","5"));
		Set<String> expectedS = new HashSet<>(Arrays.asList("4","5"));
		Set<String> actualS = SetUtils.getIntersection(setAS, setBS);
		assertEquals(expectedS,actualS);
		assertEquals(5,setAS.size());
		assertEquals(2,setBS.size());
		
		setAS = new HashSet<>(Arrays.asList("1","2","3","4","5"));
		setBS = new HashSet<>(Arrays.asList("6","7"));
		expectedS = new HashSet<>();
		actualS = SetUtils.getIntersection(setAS, setBS);
		assertEquals(expectedS,actualS);
		
		setAS = new HashSet<>();
		setBS = new HashSet<>();
		expectedS = new HashSet<>();
		actualS = SetUtils.getIntersection(setAS, setBS);
		assertEquals(expectedS,actualS);
		assertEquals(0,setAS.size());
		assertEquals(0,setBS.size());
	}
    
//------------------------------------------------------------------------------

}
