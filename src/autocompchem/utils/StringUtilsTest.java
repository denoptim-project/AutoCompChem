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
    public void testCount() throws Exception
    {
    	String s = "TRGAadfTR_G sdg hjnujTRGk fhjx vbn xTRGvbx vbn xbn TRG";
    	assertEquals(4,StringUtils.countMatches(s, "TRG"),"Number of matches");
    }
    
//------------------------------------------------------------------------------

}
