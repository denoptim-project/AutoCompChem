package autocompchem.chemsoftware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.run.Job;
import autocompchem.text.TextBlock;


/**
 * Unit Test for Directive
 * 
 * @author Marco Foscato
 */

public class DirectiveTest 
{
	private final String NL = System.getProperty("line.separator");
	
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
    	
    	Directive dA = DirectiveFactory.buildFromJDText("test",lines);
    	Directive dB = DirectiveFactory.buildFromJDText("test",linesB);
    	
    	assertTrue(dA.equals(dB),"Custom Equality");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testToLinesJD() throws Exception
    {    	
    	Directive d = new Directive("testDirective");
    	d.addKeyword(new Keyword("key1", true, "a b"));
    	d.addKeyword(new Keyword("key2", true, "c d"));
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
    	assertEquals(2,countLinesWithString(
    			d,ChemSoftConstants.JDOPENBLOCK),
    			"Number of lines in JD(E).");
    	assertEquals(2,countLinesWithString(
    			d,ChemSoftConstants.JDCLOSEBLOCK),
    			"Number of lines in JD(F).");
    	
    	Directive d2 = DirectiveFactory.buildFromJDText(d.toLinesJobDetails());
    	
    	assertTrue(d.equals(d2),"Equality of regenerated from JD string.");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testPerformACCTask() throws Exception
    {    
    	Directive d = new Directive("testDirective");
    	d.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList("a","b"))));
    	d.addKeyword(new Keyword("key2", true, 
    			new ArrayList<String>(Arrays.asList("c","d"))));
    	d.addDirectiveData(new DirectiveData("data", new ArrayList<String>(
    			Arrays.asList(ChemSoftConstants.JDOPENBLOCK
    					+ ChemSoftConstants.JDLABACCTASK
    					+ ParameterConstants.SEPARATOR
    					+ ChemSoftConstants.PARGETFILENAMEROOT + NL
    					+ ChemSoftConstants.PARGETFILENAMEROOTSUFFIX 
    					+ ParameterConstants.SEPARATOR + ".sfx" 
    					+ ChemSoftConstants.JDCLOSEBLOCK))));
    	
    	//WARNING: the task is defined in a nested multiline block in the
    	// DirectiveData value. So, wo define a task in a job details file
    	// one need to nest the ACCTask's parameter into TWO multiline blocks:
    	// - one for delimiting the ACCTask-defining lines,
    	// - one for delimiting the lines that belong to the DirectiveData value
    	
    	Job j = new Job();
    	j.setParameter(new Parameter(ChemSoftConstants.PAROUTFILEROOT, "/path "
    			+ "/to/filenameRoot"));
    	
    	d.performACCTasks(null, j);
    	
    	assertTrue(d.getDirectiveData("data").getValue().toString()
    			.contains(".sfx"),
    			"Task changing DirectiveData");
    	
    	Directive d2 = new Directive("testDirective");
    	d2.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList(
    					ChemSoftConstants.JDOPENBLOCK
    					+ ChemSoftConstants.JDLABACCTASK
    					+ ParameterConstants.SEPARATOR
    					+ ChemSoftConstants.PARGETFILENAMEROOT,
    					ChemSoftConstants.PARGETFILENAMEROOTSUFFIX 
    					+ ParameterConstants.SEPARATOR + "_job2.xyz"
    					+ ChemSoftConstants.JDCLOSEBLOCK))));

    	d2.performACCTasks(null, j);
    	
    	assertTrue(d2.getKeyword("key1").getValue().toString().contains(
    			"filenameRoot_job2.xyz"),
    			"Task changing DirectiveData");
    	
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

    @Test
    public void testGetComponent() throws Exception
    {
    	Directive d = new Directive("testDirective");
    	d.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList("a","b"))));
    	d.addKeyword(new Keyword("key2", true, 
    			new ArrayList<String>(Arrays.asList("c","d"))));
    	d.addDirectiveData(new DirectiveData("data", new ArrayList<String>(
    			Arrays.asList("A","B","C"))));
    	
    	IDirectiveComponent c = d.getComponent("key2", 
    			DirectiveComponentType.KEYWORD);
    	assertEquals("c",((Keyword) c).getValue().get(0),"Retrieve Keyword");
    	

    	IDirectiveComponent dd = d.getComponent("data", 
    			DirectiveComponentType.DIRECTIVEDATA);
    	assertEquals("A",((TextBlock) 
    			((DirectiveData) dd).getValue()).get(0),
    			"Retrieve DirectiveData");
    	
    	IDirectiveComponent x = d.getComponent("notThere", 
    			DirectiveComponentType.DIRECTIVEDATA);
    	
    	assertNull(x,"Retriving missing object");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testDeleteComponent() throws Exception
    {
    	Directive d = new Directive("testDirective");
    	d.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList("a","b"))));
    	d.addKeyword(new Keyword("key2", true, 
    			new ArrayList<String>(Arrays.asList("c","d"))));
    	d.addDirectiveData(new DirectiveData("data", new ArrayList<String>(
    			Arrays.asList("A","B","C"))));
    	
    	assertEquals(1,d.getAllDirectiveDataBlocks().size(),
    			"Number of DirectiveData blocks (A)");
    	assertEquals(2,d.getAllKeywords().size(),
    			"Number of Keywords (A)");
    	
    	IDirectiveComponent comp = d.getKeyword("key2");
    	d.deleteComponent(comp);
    	
    	assertEquals(1,d.getAllDirectiveDataBlocks().size(),
    			"Number of DirectiveData blocks (B)");
    	assertEquals(1,d.getAllKeywords().size(),
    			"Number of Keywords (B)");
    	
    	IDirectiveComponent comp2 = d.getDirectiveData("data");
    	d.deleteComponent(comp2);
    	
    	assertEquals(0,d.getAllDirectiveDataBlocks().size(),
    			"Number of DirectiveData blocks (A)");
    	assertEquals(1,d.getAllKeywords().size(),
    			"Number of Keywords (A)");
    	
    }
    
//------------------------------------------------------------------------------

}
