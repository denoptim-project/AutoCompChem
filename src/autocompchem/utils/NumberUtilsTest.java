package autocompchem.utils;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for number utilities
 * 
 * @author Marco Foscato
 */

public class NumberUtilsTest 
{

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

}
