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

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftConstants;


/**
 * Unit Test for text analysis.
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

//------------------------------------------------------------------------------

    private final ArrayList<String> KEYVALKUELINES = new ArrayList<String>(Arrays.asList(
    		"#comment",
    		"key1: value1",
    		"key2:value2",
    		"$STARTkey3: first line of val3",
    		"second line of val3",
    		"$END",
    		"$STARTkey4: #first line of val4",
    		"second: line of val4",
    		"$STARTnestedKey: first line of nested",
    		"second line of nested",
    		"$STARTdeeplyNestedKey: first line of deeplynested",
    		"second line of deeplynested",
    		"$END$END",
    		"lastkey: lastval$END",
    		"key5:    value5",
    		"#",
    		"# comment2"));

//------------------------------------------------------------------------------

    @Test
    public void testReadKeyValueEmbeddedStartOfMultiline() throws Exception
    {
       	ArrayList<String> linesA = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"$END"));
       	
       	ArrayList<String> linesB = new ArrayList<String>(Arrays.asList(
        		"n1: $STARTv1: vv1",
        		"v1b: vv1b",
        		"$END"));
       	
       	ArrayList<ArrayList<String>> equivalentForms = 
       			new ArrayList<ArrayList<String>>();
       	equivalentForms.add(linesA);
       	equivalentForms.add(linesB);
       	
    	for (int i=0; i<equivalentForms.size(); i++)
    	{
    		ArrayList<String> lines = equivalentForms.get(i);
       	
    		ArrayList<ArrayList<String>>form = TextAnalyzer.readKeyValue(lines,
    			":","#","$START","$END");
    		
	       	assertEquals(1,form.size(), "Number of key:val pairs");
	       	assertEquals("n1",form.get(0).get(0), "First key");
	    	assertFalse(form.get(0).get(1).contains("$START"), "No leftover $START "+i);
	    	assertFalse(form.get(0).get(1).contains("$END"), "No leftover $END "+i);
    	}
       	
    	ArrayList<String> linesC = new ArrayList<String>(Arrays.asList(
        		"$STARTn1:$STARTn2: v2 vv2",
        		"v2b: vv2",
        		"$END$END"));
    	ArrayList<String> linesD = new ArrayList<String>(Arrays.asList(
        		"n1:$STARTn2:$STARTv2 vv2",
        		"v2b: vv2",
        		"$END$END"));
    	
       	equivalentForms = new ArrayList<ArrayList<String>>();
       	equivalentForms.add(linesC);
       	equivalentForms.add(linesD);
       	
    	for (int i=0; i<equivalentForms.size(); i++)
    	{
    		ArrayList<String> lines = equivalentForms.get(i);
       	
    		ArrayList<ArrayList<String>>form = TextAnalyzer.readKeyValue(lines,
    			":","#","$START","$END");
	       	assertEquals(1,form.size(), "Number of key:val pairs (B) "+i);
	       	assertEquals("n1",form.get(0).get(0), "First key (B) "+i);
	    	assertTrue(form.get(0).get(1).contains("$START"), "Surviving $START (B) "+i);
	    	assertTrue(form.get(0).get(1).contains("$END"), "Surviving $END (B)"+i);
    	}
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testCountStringInLine() throws Exception
    {
    	String line = "$END dsdf $END asdas$ ENDdfg $END";
    	assertEquals(3,TextAnalyzer.countStringInLine("$END", line),"Case-1");
    	line = "dsdf $END asdas$ ENDdfg $ENDfafdbv";
    	assertEquals(2,TextAnalyzer.countStringInLine("$END", line),"Case-2");
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testReadKeyValue() throws Exception
    {
    	// Here we test equivalent notations for ending nested multiline blocks
       	ArrayList<String> linesA = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"$STARTn2: v2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b$END$END$END"));
       	
       	ArrayList<String> linesB = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"$STARTn2: v2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b",
        		"$END",
        		"$END",
        		"$END"));
       	
       	ArrayList<String> linesC = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"$STARTn2: v2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b",
        		"$END$END",
        		"$END"));
       	
       	ArrayList<String> linesD = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"$STARTn2: v2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b",
        		"$END",
        		"$END$END"));
       	
       	ArrayList<ArrayList<String>> equivalentForms = 
       			new ArrayList<ArrayList<String>>();
       	equivalentForms.add(linesA);
       	equivalentForms.add(linesB);
       	equivalentForms.add(linesC);
       	equivalentForms.add(linesD);
       		
    	for (int iList=0; iList<equivalentForms.size(); iList++)
    	{
    		ArrayList<String> lines = equivalentForms.get(iList);
    	
    		ArrayList<ArrayList<String>> form = TextAnalyzer.readKeyValue(
    			lines,":","#","$START","$END");
	    	
	    	assertEquals(1,form.size(),"Number of key:value pairs (Nest 1-List"
	    	+ iList + ")");
	    	assertEquals("n1",form.get(0).get(0),
	    			"Key 1 (Nest 1-List"+iList+")");
	    	
	    	form = TextAnalyzer.readKeyValue(new ArrayList<String>(
	    			Arrays.asList(form.get(0).get(1).split(
	    			System.getProperty("line.separator")))),
	    			":","#","$START","$END");

	    	assertEquals(3,form.size(),"Number of key:value pairs (Nest 2-List"
	    	    	+ iList + ")");	    	
	    	assertEquals("v1",form.get(0).get(0),
	    			"Key 1 (Nest 2-List"+iList+")");
	    	assertEquals("v1b",form.get(1).get(0),
	    			"Key 2 (Nest 2-List"+iList+")");
	    	assertEquals("n2",form.get(2).get(0),
	    			"Key 3 (Nest 2-List"+iList+")");
	    	
	    	form = TextAnalyzer.readKeyValue(new ArrayList<String>(
	    			Arrays.asList(form.get(2).get(1).split(
	    			System.getProperty("line.separator")))),
	    			":","#","$START","$END");
	    	
	    	assertEquals(3,form.size(),"Number of key:value pairs (Nest 3-List"
	    	    	+ iList + ")");
	    	assertEquals("v2",form.get(0).get(0),
	    			"Key 1 (Nest 3-List"+iList+")");
	    	assertEquals("v2b",form.get(1).get(0),
	    			"Key 2 (Nest 3-List"+iList+")");
	    	assertEquals("n3",form.get(2).get(0),
	    			"Key 3 (Nest 3-List"+iList+")");
    	}
    
    	// Here we consider the equivalent ways of starting nested blocks
    	ArrayList<String> linesAA = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"$STARTn2: v2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b$END$END$END"));
    	ArrayList<String> linesAB = new ArrayList<String>(Arrays.asList(
        		"$STARTn1:$STARTn2: v2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b$END$END",
        		"v1: vv1",
         		"v1b: vv1b",
         		"$END"));
    	ArrayList<String> linesAC = new ArrayList<String>(Arrays.asList(
        		"$STARTn1:$STARTn2:$STARTn3: v3",
        		"v3b$END",
        		"v2: vv2",
         		"v2b: vv2",
         		"$END",
        		"v1: vv1",
        		"v1b: vv1b",
        		"$END"));
    	ArrayList<String> linesAD = new ArrayList<String>(Arrays.asList(
        		"$STARTn1: v1: vv1",
        		"v1b: vv1b",
        		"n2: $STARTv2: vv2",
        		"v2b: vv2",
        		"$STARTn3: v3",
        		"v3b$END$END$END"));
    	
    	equivalentForms = new ArrayList<ArrayList<String>>();
       	equivalentForms.add(linesAA);
       	equivalentForms.add(linesAB);
       	equivalentForms.add(linesAC);
       	equivalentForms.add(linesAD);
       
    	for (int iList=0; iList<equivalentForms.size(); iList++)
    	{
    		ArrayList<String> lines = equivalentForms.get(iList);
    	
    		ArrayList<ArrayList<String>> form = TextAnalyzer.readKeyValue(
    			lines,":","#","$START","$END");
    		
	    	assertEquals(1,form.size(),"Number of key:value pairs (Nest 1-List"
	    	+ iList + ")");
	    	
	    	int iN1 = -1;
	    	for (int iAr=0; iAr<form.size(); iAr++)
	    	{
	    		ArrayList<String> a = form.get(iAr);
	    		if (a.get(0).equals("n1"))
	    			iN1 = iAr;
	    	}
	    	assertTrue(iN1>-1,"Locating nest N1");
	    	
	    	form = TextAnalyzer.readKeyValue(new ArrayList<String>(
	    			Arrays.asList(form.get(iN1).get(1).split(
	    			System.getProperty("line.separator")))),
	    			":","#","$START","$END");
	    	
	    	assertEquals(3,form.size(),"Number of key:value pairs (Nest 2-List"
	    	    	+ iList + ")");
	    	
	    	int iN2 = -1;
	    	for (int iAr=0; iAr<form.size(); iAr++)
	    	{
	    		ArrayList<String> a = form.get(iAr);
	    		if (a.get(0).equals("n2"))
	    			iN2 = iAr;
	    	}
	    	assertTrue(iN2>-1,"Locating nest N2");
	    	
	    	form = TextAnalyzer.readKeyValue(new ArrayList<String>(
	    			Arrays.asList(form.get(iN2).get(1).split(
	    			System.getProperty("line.separator")))),
	    			":","#","$START","$END");
	    	
	    	assertEquals(3,form.size(),"Number of key:value pairs (Nest 3-List"
	    	    	+ iList + ")");
	    	
	    	int iN3 = -1;
	    	for (int iAr=0; iAr<form.size(); iAr++)
	    	{
	    		ArrayList<String> a = form.get(iAr);
	    		if (a.get(0).equals("n3"))
	    			iN3 = iAr;
	    	}
	    	assertTrue(iN3>-1,"Locating nest N3");
	    	assertEquals(2,form.get(iN3).get(1).split(
	    			System.getProperty("line.separator")).length,"Size of nest "
	    			+ "(List" + iList + ")");
    	}
    	
    	// Here we mix all together
    	ArrayList<ArrayList<String>> form = TextAnalyzer.readKeyValue(
    			KEYVALKUELINES,":","#","$START","$END");
    	
    	assertEquals(5,form.size(),"Number of key:value pairs.");
    	assertEquals("key1",form.get(0).get(0),"Key1 extracted");
    	assertEquals("key5",form.get(4).get(0),"Key5 extracted");
    	assertEquals("value1",form.get(0).get(1),"Value of KEY1");
    	assertEquals(2,form.get(2).get(1).split(
    			System.getProperty("line.separator")).length,
    			"Size of multiline block (A)");
    	assertEquals(8,form.get(3).get(1).split(
    			System.getProperty("line.separator")).length,
    			"Size of multiline block (B)");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReadKeyValuePairs() throws Exception
    {

    	TreeMap<String, String> pairs = TextAnalyzer.readKeyValuePairs(
    			KEYVALKUELINES,":","#","$START","$END");
    	
    	assertTrue(pairs.containsKey("key1"),"First key");
    	assertTrue(pairs.containsKey("key5"),"Last key");
    	assertEquals(2,pairs.get("key3").split(
    			System.getProperty("line.separator")).length,
    			"Size of multiline block (A)");
    	
    	String nestedBLock = pairs.get("key4");
    	TreeMap<String, String> nestedPairs = TextAnalyzer.readKeyValuePairs(
    			new ArrayList<String>(Arrays.asList(nestedBLock.split(
    					System.getProperty("line.separator")))),
    			":","#","$START","$END");
    	assertEquals(3,nestedPairs.size(),"Number of key:value pairs in nested.");
    	assertTrue(nestedPairs.containsKey("second"),"Nested key");
    	assertTrue(nestedPairs.containsKey("nestedKey"),"Nested multiline block");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReadTextWithMultilineBlocksBis() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add("A B C");
    	lines.add("#commented");
    	lines.add("$STARTMLB_1.1");
    	lines.add("MLB_1.2");
    	lines.add("MLB_1.3");
    	lines.add("$END");
    	lines.add("MLB_2.1$START");
    	lines.add("MLB_2.2");
    	lines.add("MLB_2.3");
    	lines.add("$END");
       	lines.add("MLB_3.1$START");
    	lines.add("MLB_3.2");
    	lines.add("MLB_3.3$END");
       	lines.add("$STARTMLB_4.1");
    	lines.add("MLB_4.2");
    	lines.add("MLB_4.3$END");
    	lines.add("$STARTMLB_5.1");
    	lines.add("$STARTMLB_N1.1");
    	lines.add("MLB_N1.2");
    	lines.add("MLB_N1.3");
    	lines.add("$END");
    	lines.add("MLB_5.2");
    	lines.add("MLB_5.3");
    	lines.add("$END");
    	lines.add("$STARTMLB_6.1$STARTMLB_N2.1");
    	lines.add("MLB_N2.2");
    	lines.add("");
    	lines.add("MLB_N2.3");
    	lines.add("$ENDMLB_6.2");
    	lines.add("MLB_6.3$END");
    	lines.add("$STARTMLB_7.1$STARTMLB_N3.1");
    	lines.add("MLB_N3.2");
    	lines.add("MLB_N3.3");
    	lines.add("$ENDMLB_6.2$END");
    	
    	ArrayList<String> form = TextAnalyzer.readTextWithMultilineBlocks(lines,
    			"#","$START","$END");
    	
    	assertEquals(8,form.size());
    	assertTrue(form.contains("A B C"),"ERR-1");
    	assertEquals(3,
    			form.get(1).split(System.getProperty("line.separator")).length,
    			"ERR-2"); //MLB_1.1
    	assertEquals(3,
    			form.get(2).split(System.getProperty("line.separator")).length,
    			"ERR-3"); //MLB_2.1
    	assertEquals(3,
    			form.get(3).split(System.getProperty("line.separator")).length,
    			"ERR-4"); //MLB_3.1
    	assertEquals(3,
    			form.get(4).split(System.getProperty("line.separator")).length,
    			"ERR-5"); //MLB_4.1
    	assertEquals(7,
    			form.get(5).split(System.getProperty("line.separator")).length,
    			"ERR-6"); //MLB_5.1
    	assertTrue(form.get(5).contains("$START"),"ERR-7"); //MLB_5.1
    	assertTrue(form.get(5).contains("$END"),"ERR-8"); //MLB_5.1
    	assertEquals(6,
    			form.get(6).split(System.getProperty("line.separator")).length,
    			"ERR-9"); //MLB_6.1
    	assertTrue(form.get(6).contains("$START"),"ERR-10"); //MLB_6.1
    	assertTrue(form.get(6).contains("$END"),"ERR-11"); //MLB_6.1
    	assertEquals(4,
    			form.get(7).split(System.getProperty("line.separator")).length,
    			"ERR-12"); //MLB_7.1
    	assertTrue(form.get(7).contains("$START"),"ERR-13"); //MLB_7.1
    	assertTrue(form.get(7).contains("$END"),"ERR-14"); //MLB_7.1
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testReadTextWithMultilineBlocks() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(ChemSoftConstants.JDCOMMENT+ " ot-comment one");
    	lines.add(ChemSoftConstants.JDOPENBLOCK + "first");
    	lines.add(ChemSoftConstants.JDCOMMENT+ " in-comment one");
    	lines.add("firstB");
    	lines.add(ChemSoftConstants.JDCOMMENT+ " dp-comment one");
    	lines.add(ChemSoftConstants.JDOPENBLOCK + "second");
    	lines.add("secondB");
    	lines.add(ChemSoftConstants.JDOPENBLOCK + "third");
    	lines.add("thirdB");
    	lines.add(ChemSoftConstants.JDOPENBLOCK + "forth");
    	lines.add("forthB");
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	lines.add(ChemSoftConstants.JDCOMMENT+ " dp-comment two");
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	lines.add(ChemSoftConstants.JDCOMMENT+ " in-comment two");
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	lines.add(ChemSoftConstants.JDCOMMENT+ " ot-comment two");
    	
    	for (int i=0; i<3; i++)
    	{
            lines = TextAnalyzer.readTextWithMultilineBlocks(lines,
            		ChemSoftConstants.JDCOMMENT, 
            		ChemSoftConstants.JDOPENBLOCK, 
            		ChemSoftConstants.JDCLOSEBLOCK);
        	
        	assertEquals(1,lines.size(),"Size ("+i+")");

        	assertEquals(3,TextAnalyzer.countStringInLine(
        			ChemSoftConstants.JDOPENBLOCK, lines.get(0)),
        			"Number of directive labels");
        	assertEquals(3,TextAnalyzer.countStringInLine(
        			ChemSoftConstants.JDCLOSEBLOCK, lines.get(0)),
        			"Number of directive labels");

        	assertEquals(0,TextAnalyzer.countStringInLine("ot-comment", 
        			lines.get(0)),"Number of outside comments");
        	assertEquals(2,TextAnalyzer.countStringInLine("in-comment", 
        			lines.get(0)),"Number of nested comments");
        	assertEquals(2,TextAnalyzer.countStringInLine("dp-comment", 
        			lines.get(0)),"Number of deep comments");
    	}
    	
    	lines.clear();
    	lines.add(ChemSoftConstants.JDOPENBLOCK +  "third_"
    			+ ChemSoftConstants.JDOPENBLOCK + "second_"
    			+ ChemSoftConstants.JDOPENBLOCK + "first_"
    			+ "first" +ChemSoftConstants.JDCLOSEBLOCK
    			+ "second" +ChemSoftConstants.JDCLOSEBLOCK
    			+ "third" +ChemSoftConstants.JDCLOSEBLOCK);
    	
    	for (int i=0; i<3; i++)
    	{
    		lines = TextAnalyzer.readTextWithMultilineBlocks(lines,
            		ChemSoftConstants.JDCOMMENT, 
            		ChemSoftConstants.JDOPENBLOCK, 
            		ChemSoftConstants.JDCLOSEBLOCK);

        	assertEquals(1,lines.size(),"Size (B) ("+i+")");

        	assertEquals(3,TextAnalyzer.countStringInLine(
        			ChemSoftConstants.JDOPENBLOCK, lines.get(0)),
        			"Number of directive labels (B)");
        	assertEquals(3,TextAnalyzer.countStringInLine(
        			ChemSoftConstants.JDCLOSEBLOCK, lines.get(0)),
        			"Number of directive labels (B)");
    	}    
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGrep() throws Exception
    {
    	BufferedReader br = new BufferedReader(new StringReader(TEXT));
    	Set<String> p = new HashSet<String>(Arrays.asList("bla","here"));
    	ArrayList<String> m = TextAnalyzer.grep(br, p);
    	
    	assertEquals(3,m.size(),"Size of matches list");
    	assertTrue(m.contains("Second line bla bla"),"First match");
    	assertTrue(m.contains("5th line here"),"Last match");
    }

//------------------------------------------------------------------------------

}
