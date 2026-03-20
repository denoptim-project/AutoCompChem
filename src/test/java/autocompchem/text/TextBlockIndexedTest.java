package autocompchem.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.jupiter.api.Test;

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
      String replaceKey = "__REPLACE_KEY__";
    	tb.appendText("My line _" + replaceKey + "_ and the rest" + replaceKey);
      tb.appendText("Other " + replaceKey);
        
    	tb.replaceAll(replaceKey,"NEWSTR");
    	
    	assertTrue(tb.getText().get(0).matches(".*NEWSTR.*"),
    			"Replacement occurred");
          assertTrue(tb.getText().get(1).matches(".*NEWSTR.*"),
              "Replacement occurred");
      assertEquals(StringUtils.countMatches(StringUtils.mergeListToString(
        tb.getText(), " "), "NEWSTR"), 3, "Number of replaced strings");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReplaceAllInNested() throws Exception
    {
      String replaceKey = "__REPLACE_KEY__";
        String NESTEDBLOCKS = "#beginning" + NL
                + "OPB1" + NL
                 + "inside block 1 _" + replaceKey 
                 + "_ _ _ " 
                 + replaceKey + NL
                 + "OPB2" + NL
                  + "inside nested block 2 " + replaceKey 
                  + NL
                 + "CLB2" + NL
                 + "after nested block 2" + NL
                 + "OPB2" + NL
                  + "inside nested block 2b " + replaceKey 
                  + NL
                   + "OPB3" + NL
                   + "inside nested block 3 " + replaceKey 
                   + NL
                 + "CLB2" + NL
                   + "after 2b but still in 3" + NL
                   + "CLB3" + NL
                 + "after nested block 3" + NL
                + "CLB1" + NL
                + "outside 1 " + replaceKey + NL
                + "#end ";
        BufferedReader br = new BufferedReader(new StringReader(NESTEDBLOCKS));
        List<TextBlockIndexed> blocks = TextAnalyzer.extractTextBlocks(br,
                                                slPts,sPats,ePats,false,false);
        
        final String NEWSTR = "MyNewString";
        for (TextBlockIndexed tb : blocks)
        {
        	tb.replaceAll(replaceKey, NEWSTR);
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
