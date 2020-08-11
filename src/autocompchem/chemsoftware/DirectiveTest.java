package autocompchem.chemsoftware;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import javax.sound.sampled.Line;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;


/**
 * Unit Test for Directive
 * 
 * @author Marco Foscato
 */

public class DirectiveTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromText() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(ChemSoftConstants.JDLABLOUDKEY + "key1"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD1 "
    	    	+ ChemSoftConstants.JDLABLOUDKEY + "key1b "
    	    	+ ChemSoftConstants.JDKEYVALSEPARATOR + "val 1b");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD2 "
    	    	+ ChemSoftConstants.JDLABLOUDKEY + "key1c"
    	    	+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1c");
    	lines.add(ChemSoftConstants.JDLABLOUDKEY + "key2"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val2");
    	lines.add(ChemSoftConstants.JDLABMUTEKEY + "key3"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val3");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD1 "
    	    	+ ChemSoftConstants.JDLABLOUDKEY + "key2b "
    	    	+ ChemSoftConstants.JDKEYVALSEPARATOR + "val 2b");
    	lines.add(ChemSoftConstants.JDLABDATA + "data1 "
    	    	+ ChemSoftConstants.JDDATAVALSEPARATOR + "data 1st bla");
    	lines.add(ChemSoftConstants.JDLABDATA + "data2 "
    	    	+ ChemSoftConstants.JDDATAVALSEPARATOR + "data 2nd bla");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD2 "
    			+ ChemSoftConstants.JDLABDATA + "data2 "
    	    	+ ChemSoftConstants.JDDATAVALSEPARATOR + "data 2nd bla");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD3 ");  

    	Directive d = new Directive("testDirective",lines);
    	
    	assertEquals(3,d.getAllKeywords().size(),"Number of keywords (A)");
    	assertEquals("key1",d.getAllKeywords().get(0).getName(),
    			"First keyword (A)");
    	assertEquals("key3",d.getAllKeywords().get(2).getName(),
    			"Last keyword (A)");
    	
    	assertEquals(3,d.getAllSubDirectives().size(),"Number of subdirs (A)");
    	assertEquals("subD1",d.getAllSubDirectives().get(0).getName(),
    			"Name of subdir (A)");
    	assertEquals(2,d.getAllSubDirectives().get(0).getAllKeywords().size(),
    			"Number of keywords in subdir (A)");
    	assertEquals(new ArrayList<String>(Arrays.asList("val","1b")),
    			d.getSubDirective("subD1").getKeyword("key1b").getValue(),
    			"Value of key in subdir (A)");
    	assertEquals(new ArrayList<String>(Arrays.asList("val","2b")),
    			d.getSubDirective("subD1").getKeyword("key2b").getValue(),
    			"Value of key in subdir (B)");
    	
    	assertEquals(2,d.getAllDirectiveDataBlocks().size(),
    			"Number of data blocks (A)");
    	assertEquals(0,
    			d.getSubDirective("subD1").getAllDirectiveDataBlocks().size(),
    			"Number of data blocks (B)");
    	assertEquals(1,
    			d.getSubDirective("subD2").getAllDirectiveDataBlocks().size(),
    			"Number of data blocks (C)");
    }
    
  //------------------------------------------------------------------------------

    @Test
    public void testConstructorFromTextWithMultiline() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(ChemSoftConstants.JDLABLOUDKEY + "key1"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1");
    	lines.add(ChemSoftConstants.JDLABDATA + "dat1 "
    			+ ChemSoftConstants.JDDATAVALSEPARATOR 
    			+ ChemSoftConstants.JDLABOPENBLOCK + "line 1");
    	lines.add("Line 2");
    	lines.add("Line 3");
    	lines.add(ChemSoftConstants.JDLABCLOSEBLOCK);
    	
    	ArrayList<String> linesB = new ArrayList<String>();
    	linesB.add(ChemSoftConstants.JDLABLOUDKEY + "key1"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1");
    	linesB.add(ChemSoftConstants.JDLABOPENBLOCK 
    			+ ChemSoftConstants.JDLABDATA + "dat1 "
    			+ ChemSoftConstants.JDDATAVALSEPARATOR + "line 1");
    	linesB.add("Line 2");
    	linesB.add("Line 3");
    	linesB.add(ChemSoftConstants.JDLABCLOSEBLOCK);
    	
    	Directive d = new Directive("test",lines);
    	Directive dB = new Directive("test",linesB);
    	
    	assertTrue(d.equals(dB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCustomEquals() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(ChemSoftConstants.JDLABLOUDKEY + "key1"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1");
    	lines.add(ChemSoftConstants.JDLABLOUDKEY + "key2"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val2");
    	lines.add(ChemSoftConstants.JDLABMUTEKEY + "key3"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val3");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD1 ");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "subD2 ");
    	lines.add(ChemSoftConstants.JDLABDATA + "dat1 "
    			+ ChemSoftConstants.JDDATAVALSEPARATOR + "bla");
    	
    	ArrayList<String> linesB = new ArrayList<String>();
    	linesB.add(ChemSoftConstants.JDLABLOUDKEY + "key1b"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1");
    	linesB.add(ChemSoftConstants.JDLABLOUDKEY + "key2b"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val2");
    	linesB.add(ChemSoftConstants.JDLABMUTEKEY + "key3b"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val3");
    	linesB.add(ChemSoftConstants.JDLABDIRECTIVE + "subD1b");
    	linesB.add(ChemSoftConstants.JDLABDIRECTIVE + "subD2bb");
    	linesB.add(ChemSoftConstants.JDLABDATA + "dat1b"
    			+ ChemSoftConstants.JDDATAVALSEPARATOR + "blabla");    	
    	
    	Directive dA = new Directive("testDirective",lines);
    	Directive dB = new Directive("testDirective",linesB);
    	
    	assertTrue(dA.equals(dB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testToLinesJD() throws Exception
    {    	
    	Directive d = new Directive("testDirective");
    	d.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList("a","b"))));
    	d.addKeyword(new Keyword("key2", true, 
    			new ArrayList<String>(Arrays.asList("c","d"))));
    	d.addDirectiveData(new DirectiveData("data",  
    			new ArrayList<String>(Arrays.asList("A1","A2","A3"))));
    	Directive sa = new Directive("subA");
    	sa.addKeyword(new Keyword("sKey", false, 
    			new ArrayList<String>(Arrays.asList("0","1"))));
    	d.addSubDirective(sa);
    	
    	assertEquals(7,d.toLinesJobDetails().size(),"Number of lines in JD.");
    	assertEquals(2,countLinesWithString(
    			d,ChemSoftConstants.JDLABLOUDKEY),
    			"Number of lines in JD(A).");
    	assertEquals(1,countLinesWithString(
    			d,ChemSoftConstants.JDLABMUTEKEY),
    			"Number of lines in JD(B).");
    	assertEquals(1,countLinesWithString(
    			d,ChemSoftConstants.JDLABDATA),
    			"Number of lines in JD(C).");
    	assertEquals(4,countLinesWithString(
    			d,ChemSoftConstants.JDLABDIRECTIVE),
    			"Number of lines in JD(D).");
    	assertEquals(1,countLinesWithString(
    			d,ChemSoftConstants.JDLABOPENBLOCK),
    			"Number of lines in JD(E).");
    	assertEquals(1,countLinesWithString(
    			d,ChemSoftConstants.JDLABCLOSEBLOCK),
    			"Number of lines in JD(F).");
    	
    	Directive d2 = new Directive(d.toLinesJobDetails());
    	
    	assertTrue(d.equals(d2),"Equality");
    }
    
//------------------------------------------------------------------------------
    
    private int countLinesWithString(Directive d, String s)
    {
    	int n = 0;
    	for (String l : d.toLinesJobDetails())
		{
			if (l.contains(s))
				n++;
		}
    	return n;
    }
    
//------------------------------------------------------------------------------

}
