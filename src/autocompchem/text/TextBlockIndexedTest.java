package autocompchem.text;

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
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.utils.StringUtils;


/**
 * Unit Test for indexed text blocks
 * 
 * @author Marco Foscato
 */

public class TextBlockIndexedTest 
{
    private final String NL = System.getProperty("line.separator");

    private final ArrayList<String> slPts = new ArrayList<String>(Arrays.asList(
                "(.*)KEY1(.*)","(.*)NESTEDKEY(.*)"));
    private final ArrayList<String> sPats = new ArrayList<String>(Arrays.asList(
                "(.*)OPB1(.*)","(.*)OPB2(.*)","(.*)OPB3(.*)"));
    private final ArrayList<String> ePats = new ArrayList<String>(Arrays.asList(
                "(.*)CLB1(.*)","(.*)CLB2(.*)","(.*)CLB3(.*)"));

//------------------------------------------------------------------------------

    @Test
    public void testReplaceAll() throws Exception
    {
    	TextBlockIndexed tb = new TextBlockIndexed();
    	tb.appendText("My line _" + ParameterConstants.STRINGFROMCLI
    			+ "_ and the rest");
        
    	tb.replaceAll(ParameterConstants.STRINGFROMCLI,"NEWSTR");
    	
    	assertTrue(tb.getText().get(0).matches(".*NEWSTR.*"),
    			"Replacement occurred");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReplaceAllInNested() throws Exception
    {
        String NESTEDBLOCKS = "#beginning" + NL
                + "OPB1" + NL
                 + "inside block 1 _" + ParameterConstants.STRINGFROMCLI 
                 + "_ _ _ " 
                 + ParameterConstants.STRINGFROMCLI + NL
                 + "OPB2" + NL
                  + "inside nested block 2 " + ParameterConstants.STRINGFROMCLI 
                  + NL
                 + "CLB2" + NL
                 + "after nested block 2" + NL
                 + "OPB2" + NL
                  + "inside nested block 2b " + ParameterConstants.STRINGFROMCLI 
                  + NL
                   + "OPB3" + NL
                   + "inside nested block 3 " + ParameterConstants.STRINGFROMCLI 
                   + NL
                 + "CLB2" + NL
                   + "after 2b but still in 3" + NL
                   + "CLB3" + NL
                 + "after nested block 3" + NL
                + "CLB1" + NL
                + "outside 1 " + ParameterConstants.STRINGFROMCLI + NL
                + "#end ";
        BufferedReader br = new BufferedReader(new StringReader(NESTEDBLOCKS));
        ArrayList<TextBlockIndexed> blocks = TextAnalyzer.extractTextBlocks(br,
                                                slPts,sPats,ePats,false,false);
        
        final String NEWSTR = "MyNewString";
        for (TextBlockIndexed tb : blocks)
        {
        	tb.replaceAll(ParameterConstants.STRINGFROMCLI, NEWSTR);
        }
        
        BufferedReader brTb = new BufferedReader(
        		new StringReader(blocks.toString()));
        
        assertEquals(6,TextAnalyzer.count(brTb, NEWSTR), 
        "Number of lines with replaced strings");
        assertEquals(9,StringUtils.countMatches(blocks.toString(),NEWSTR), 
                "Number of replaced strings");
    }

//------------------------------------------------------------------------------

}
