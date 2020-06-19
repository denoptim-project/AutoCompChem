package autocompchem.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
import java.util.TreeMap;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for text analyzer
 * 
 * @author Marco Foscato
 */

public class TextAnalyzerTest 
{
    private final String NL = System.getProperty("line.separator");
    private final String TEXT = "First line" + NL + "Second line bla bla" 
		+ NL + "3rd line" + NL + "4th line here" + NL + "5th line here";

    private final String LONGTEXT = "l0 text " + NL + "l1 text KEY1" + NL 
	+ "l2 text bla " + NL + "l3 text OPB1" + NL + "l4 text b1-1" + NL 
	+ "l5 text b1-2" + NL + "l6 text CLB1 sdfasf" + NL + "l7 text bla bla" 
	+ NL + "l8 text OPB2" + NL + "l9 text b2-1" + NL + "l10 text b2-2" 
	+ NL + "l11 text b2-3 NESTEDKEY" + NL + "l12 text b2-4" + NL 
	+ "l13 text b2-5" + NL + "l14 text b2-6 OPB3" + NL 
	+ "l15 text b2-7 b3-1" + NL + "l16 text b2-8 b3-2 NESTEDKEY" + NL 
	+ "l17 text CLB2 b3-3" + NL + "l18 text      b3-4" + NL 
	+ "l19 text      CLB3" + NL + "l20 text OPB3 OPB2" + NL 
	+ "l21 text b3-1 b2-1" + NL + "l22 text b3-2 b2-2" + NL 
	+ "l23 text b3-3 CLB2" + NL + "l24 text b3-4" + NL + "l25 text CLB3" 
	+ NL + "l26 text OPB1" + NL + "l27 text b1-1 OPB2" + NL 
	+ "l28 text b1-2 b2-1" + NL + "l29 text CLB2 CLB1 KEY1" + NL 
	+ "l30 text" + NL + "" + NL + ""; 

    private final ArrayList<String> slPts = new ArrayList<String>(Arrays.asList(
                "(.*)KEY1(.*)","(.*)NESTEDKEY(.*)"));
    private final ArrayList<String> sPats = new ArrayList<String>(Arrays.asList(
                "(.*)OPB1(.*)","(.*)OPB2(.*)","(.*)OPB3(.*)"));
    private final ArrayList<String> ePats = new ArrayList<String>(Arrays.asList(
                "(.*)CLB1(.*)","(.*)CLB2(.*)","(.*)CLB3(.*)"));

//------------------------------------------------------------------------------

    @Test
    public void testCountMatches() throws Exception
    {
        BufferedReader br = new BufferedReader(new StringReader(TEXT));
	assertEquals(5,TextAnalyzer.count(br,"line"),"Total matches");
        br.close();
    }

//------------------------------------------------------------------------------

    @Test
    public void testCountMatchesInLine() throws Exception
    {
        String l = "G.HsFFG.Hd.,-@ sdgHEG HsGhHdfv v \tG.H xFfgHsdgG.H";
	assertEquals(8,TextAnalyzer.countInLine("H",l),"#Matches (1)");
	assertEquals(4,TextAnalyzer.countInLine("\\.H",l),"#Matches (2)");
	assertEquals(6,TextAnalyzer.countInLine("G.H",l),"#Matches (3)");
	assertEquals(1,TextAnalyzer.countInLine("H$",l),"#Matches (4)");
	assertEquals(2,TextAnalyzer.countInLine("gH",l),"#Matches (5)");

	l = "here asasd here";
	assertEquals(2,TextAnalyzer.countInLine("here",l),"#Matches (6)");
	// The following are overlapping queries, thus only the first one hits
	assertEquals(1,TextAnalyzer.countInLine(".*here",l),"#Matches (7)");
	assertEquals(1,TextAnalyzer.countInLine("here.*",l),"#Matches (8)");
	assertEquals(1,TextAnalyzer.countInLine(".*here.*",l),"#Matches (9)");

	l = "here asda here sd asd here";
	assertEquals(3,TextAnalyzer.countInLine("here",l),"#Matches (10)");
	// The following are overlapping queries, thus only the first one hits
	assertEquals(1,TextAnalyzer.countInLine(".*here",l),"#Matches (11)");
	assertEquals(1,TextAnalyzer.countInLine("here.*",l),"#Matches (12)");
	assertEquals(1,TextAnalyzer.countInLine(".*here.*",l),"#Matches (13)");

    }

//------------------------------------------------------------------------------

    @Test
    public void testCountMultiMatches() throws Exception
    {
	ArrayList<String> queries = new ArrayList<String>();
	queries.add("here");
	queries.add("line");

        BufferedReader br = new BufferedReader(new StringReader(TEXT));
	ArrayList<ArrayList<Integer>> counts = TextAnalyzer.count(br,queries);
        br.close();

        assertEquals(2,counts.get(0).size(),"Total matches of 'here'");
        assertEquals(5,counts.get(1).size(),"Total matches of 'line'");
        assertEquals(4,counts.get(0).get(0),"Line number containing 'here'");
        assertEquals(5,counts.get(0).get(1),"Line number containing 'here'");

    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractionMapOfTxtBlocks() throws Exception
    {
        BufferedReader br = new BufferedReader(new StringReader(LONGTEXT));
        TreeMap<String,ArrayList<String>> blocks =
                    TextAnalyzer.extractMapOfTxtBlocksWithDelimiters(br,
                                                slPts,sPats,ePats,false,false);
        br.close();

        Map<String,Integer> correct = new HashMap<String,Integer>();
        correct.put("0_0_0",1);
        correct.put("1_2_0",2);
        correct.put("2_3_0",8);
        correct.put("3_1_0",1);
        correct.put("4_4_0",4);
        correct.put("5_1_1",1);
        correct.put("6_3_1",2);
        correct.put("6_4_1",4);
        correct.put("7_2_1",2);
        correct.put("8_3_2",1);
        correct.put("9_0_1",1);

        for (String s : blocks.keySet())
        {
            assertEquals(correct.get(s),blocks.get(s).size());
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractOveralppingTextBlocks() throws Exception
    {
        String NESTEDBLOCKS = "#beginning" + NL
                + "OPB1" + NL
                 + "inside block 1" + NL
                 + "OPB2" + NL
                  + "inside nested block 2" + NL
                 + "CLB2" + NL
                 + "after nested block 2" + NL
                 + "OPB2" + NL
                  + "inside nested block 2b" + NL
                   + "OPB3" + NL
                   + "inside nested block 3" + NL
                 + "CLB2" + NL
                   + "after 2b but still in 3" + NL
                   + "CLB3" + NL
                 + "after nested block 3" + NL
                + "CLB1" + NL
                + "outside 1" + NL
                + "#end ";
        BufferedReader br = new BufferedReader(new StringReader(NESTEDBLOCKS));
        ArrayList<TextBlock> blocks = TextAnalyzer.extractTextBlocks(br,
                                                slPts,sPats,ePats,false,false);
        br.close();

	assertEquals(4,blocks.size(),"Tutal number of blocks");
	for (TextBlock tb : blocks)
	{
	    String key = tb.getIndexA() + "_" 
		       + tb.getIndexB() + "_" 
		       + tb.getIndexC();
	    switch (key)
	    {
		case ("0_2_0"):
	            assertEquals(0,tb.getNestedBlocks().size(), "Number of "
			+ "nested blocks.");
		    assertEquals(13,tb.getText().size(),"Number of text lines");
		    break;

                case ("1_3_0"):
                    assertEquals(0,tb.getNestedBlocks().size(), "Number of "
                        + "nested blocks.");
                    assertEquals(1,tb.getText().size(),"Number of text lines");
                    break;

                case ("2_3_1"):
                    assertEquals(0,tb.getNestedBlocks().size(), "Number of "
                        + "nested blocks.");
                    assertEquals(3,tb.getText().size(),"Number of text lines");
                    break;

                case ("3_4_0"):
                    assertEquals(0,tb.getNestedBlocks().size(), "Number of "
                        + "nested blocks.");
                    assertEquals(3,tb.getText().size(),"Number of text lines");
                    break;

		default:
		    assertTrue(false, "Unexpected key in TextBlocks");
		    break;
	    }
	}
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractNestedTextBlocks() throws Exception
    {
	String NESTEDBLOCKS = "#beginning" + NL
		+ "OPB1" + NL
		 + "inside block 1" + NL
		 + "OPB1" + NL
		  + "inside nested block 1.1" + NL
		 + "CLB1" + NL
		 + "after nested block 1.1" + NL
		 + "OPB1" + NL
		  + "inside nested block 1.2" + NL
		  + "OPB1" + NL
		   + "inside nested block 1.2.1" + NL
		  + "CLB1" + NL
		  + "still inside nested block 1.2" + NL
		 + "CLB1" + NL
		 + "after nested block 1.2" + NL
		 + "OPB2" + NL
		 + "inside nested block 2" + NL
		 + "OPB1" + NL
		  + "inside nested block 1.3" + NL
		 + "CLB1" + NL
		 + "still inside nested block 2" + NL
		 + "CLB2" + NL
		 + "after nested block 2" + NL
		 + "OPB3" + NL
		 + "inside nested block 3" + NL
		 + "OPB1" + NL
		  + "inside nested block 1.4 and inside 3" + NL
		 + "CLB3" + NL
		  + "inside nested block 1.4 but outside 3" + NL
		 + "CLB1" + NL
		 + "after nested block 3" + NL
		+ "CLB1";
        BufferedReader br = new BufferedReader(new StringReader(NESTEDBLOCKS));
        ArrayList<TextBlock> blocks = TextAnalyzer.extractTextBlocks(br,
                                                slPts,sPats,ePats,false,false);
        br.close();


//TODO del
/*System.out.println(NL+NL+"Total first level blocks: "+blocks.size()+NL);
for(TextBlock tb : blocks)
    System.out.println("Final Block: "+tb.toString()+NL);
//System.exit(0);
*/

        assertEquals(3,blocks.size(),"Tutal number of 1st level blocks");
        for (TextBlock tb : blocks)
        {
            String key = tb.getIndexA() + "_"
                       + tb.getIndexB() + "_"
                       + tb.getIndexC();
            switch (key)
            {
                case ("0_2_0"):
                    assertEquals(4,tb.getNestedBlocks().size(), "Number of "
                        + "nested blocks.");
                    assertEquals(11,tb.getText().size(),"Number of text lines");
                    break;

                case ("4_3_0"):
                    assertEquals(0,tb.getNestedBlocks().size(), "Number of "
                        + "nested blocks.");
                    assertEquals(5,tb.getText().size(),"Number of text lines");
                    break;

                case ("6_4_0"):
                    assertEquals(0,tb.getNestedBlocks().size(), "Number of "
                        + "nested blocks.");
                    assertEquals(3,tb.getText().size(),"Number of text lines");
                    break;

                default:
                    assertTrue(false, "Unexpected key in TextBlocks");
                    break;
            }
	}
    }

//TODO test grep

//------------------------------------------------------------------------------

}
