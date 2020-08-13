package autocompchem.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftConstants;
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

}
