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
import autocompchem.text.TextAnalyzer;


/**
 * Unit Test for DirectiveFactory
 * 
 * @author Marco Foscato
 */

public class DirectiveFactoryTest 
{
	private final String NL = System.getProperty("line.separator");
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

    	Directive d = DirectiveFactory.buildFromJDText("testDirective",lines);
    	
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
    			+ ChemSoftConstants.JDOPENBLOCK + "line 1");
    	lines.add("Line 2");
    	lines.add("Line 3");
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	
    	ArrayList<String> linesB = new ArrayList<String>();
    	linesB.add(ChemSoftConstants.JDLABLOUDKEY + "key1"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "val1");
    	linesB.add(ChemSoftConstants.JDOPENBLOCK 
    			+ ChemSoftConstants.JDLABDATA + "dat1 "
    			+ ChemSoftConstants.JDDATAVALSEPARATOR + "line 1");
    	linesB.add("Line 2");
    	linesB.add("Line 3");
    	linesB.add(ChemSoftConstants.JDCLOSEBLOCK);
    	
    	Directive d = DirectiveFactory.buildFromJDText("test",lines);
    	Directive dB = DirectiveFactory.buildFromJDText("test",linesB);
    	
    	assertTrue(d.equals(dB));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testBuildAllFromJDText() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_1 " 
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_1.1 "
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_1.1.1 ");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_1 " 
    			+ ChemSoftConstants.JDLABLOUDKEY + " key1 " 
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "value1A 123 @#£ 456");
    	
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_1 " 
    			+ ChemSoftConstants.JDLABDATA + " data1 " 
    			+ ChemSoftConstants.JDDATAVALSEPARATOR 
    			+ ChemSoftConstants.JDOPENBLOCK + " first part ");
    	lines.add(" second part ");
    	lines.add(" third part " + ChemSoftConstants.JDCLOSEBLOCK);
    	
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_1 " 
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_1.1 "
    			+ ChemSoftConstants.JDLABMUTEKEY + "muteKey" 
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "mute Key Value");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_1 " 
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_1.1 "
    			+ ChemSoftConstants.JDLABLOUDKEY + "loudKey" 
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "loud Key Value");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_2 "
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_2.1 "
    			+ ChemSoftConstants.JDLABLOUDKEY + "loudKey_2"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "loud Key value 2");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_2 "
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_2.2 "
    			+ ChemSoftConstants.JDLABLOUDKEY + "loudKey_3"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "loud Key value 3");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_3 ");
    	
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + " dir_3 "
    			+ ChemSoftConstants.JDLABDATA + "data block n. 3 "
    			+ ChemSoftConstants.JDDATAVALSEPARATOR 
    			+ ChemSoftConstants.JDOPENBLOCK + "line #1");
    	lines.add("  line #2");
    	lines.add("   *   line #3");
    	lines.add(ChemSoftConstants.JDOPENBLOCK + "nested multiline block ");
    	lines.add("with some more ");
    	lines.add("lines, possibly embedding labels");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "embeddeDir"
    			+ ChemSoftConstants.JDCLOSEBLOCK);
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	
    	lines.add(ChemSoftConstants.JDOPENBLOCK 
    			+ ChemSoftConstants.JDLABDIRECTIVE + " dir_4 "
    			+ ChemSoftConstants.JDLABDATA + "data block n. 3 "
    			+ ChemSoftConstants.JDDATAVALSEPARATOR + "line #1");
    	lines.add("  line #2");
    	lines.add("   *   line #3");
    	lines.add(ChemSoftConstants.JDOPENBLOCK + "nested multiline block ");
    	lines.add("with some more ");
    	lines.add("lines, possibly embedding labels");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "embeddeDir"
    			+ ChemSoftConstants.JDCLOSEBLOCK);
    	lines.add(ChemSoftConstants.JDCLOSEBLOCK);
    	
    	ArrayList<Directive> dirs = DirectiveFactory.buildAllFromJDText(lines);
    	
    	assertEquals(4,dirs.size(), "Numer of outermost Directives.");
    }
    
//------------------------------------------------------------------------------

}
