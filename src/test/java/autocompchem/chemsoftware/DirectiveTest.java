package autocompchem.chemsoftware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.PathnameEditor;
import autocompchem.run.AppID;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.text.TextBlock;
import autocompchem.worker.Task;
import autocompchem.worker.WorkerConstants;


/**
 * Unit Test for Directive
 * 
 * @author Marco Foscato
 */

public class DirectiveTest 
{
	private final static String NL = System.getProperty("line.separator");

    private IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------

    @Test
    public void testEqualsViaBuild() throws Exception
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
    	
    	Directive dA = DirectiveFactory.buildFromJDText("test",lines);
    	Directive dB = DirectiveFactory.buildFromJDText("test",lines);
    	
    	assertTrue(dA.equals(dB));
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {   
    	Directive dA = getTestDirective();
    	Directive dB = getTestDirective();
    	
    	assertTrue(dA.equals(dA));
    	assertTrue(dA.equals(dB));
    	assertTrue(dB.equals(dA));
    	assertFalse(dA.equals(null));
    	
    	dB.addKeyword(new Keyword("added", true, "changed"));
    	assertFalse(dA.equals(dB));
    	
    	dB = getTestDirective();
    	dB.addDirectiveData(new DirectiveData("data",  
    			new ArrayList<String>(Arrays.asList("A1","A2","A3"))));
    	assertFalse(dA.equals(dB));
    	
    	dB = getTestDirective();
    	Directive sa = new Directive("subA");
    	sa.addKeyword(new Keyword("sKey", false, 
    			new ArrayList<String>(Arrays.asList("0","1"))));
    	dB.addSubDirective(sa);
    	assertFalse(dA.equals(dB));
    	
    	dB = getTestDirective();
    	dB.setTaskParams(new ParameterStorage());
    	assertFalse(dA.equals(dB));
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
    	sa.addKeyword(new Keyword("sKey", false, "$START0" + NL + "1$END"));
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

    public static Directive getTestDirective()
    {    
    	Directive d = new Directive("testDirective");
    	
    	d.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList("a","b"))));
    	d.addKeyword(new Keyword("key2", true, 
    			new ArrayList<String>(Arrays.asList("c","d"))));
    	
    	String ddString = "data" 
    			+ ChemSoftConstants.JDDATAVALSEPARATOR
				+ ChemSoftConstants.JDLABACCTASK
				+ ParameterConstants.SEPARATOR
				+ Task.getExisting("addFileName").casedID + NL
				+ ChemSoftConstants.PARGETFILENAMEROOTSUFFIX 
				+ ParameterConstants.SEPARATOR + ".sfx";
    	d.addDirectiveData(DirectiveData.makeFromJDLine(ddString));
    	
    	Directive embedded = new Directive("embeddedDirective");
    	embedded.addKeyword(new Keyword("key1EMB", true, 
    			new ArrayList<String>(Arrays.asList("aEMB","bEMB"))));
    	d.addSubDirective(embedded);
    	
    	ParameterStorage taskParams = new ParameterStorage();
    	taskParams.setParameter("TASK", "DummyTask");
    	taskParams.setParameter("Value1", NamedDataType.INTEGER, 1);
    	taskParams.setParameter("Value2", NamedDataType.DOUBLE, 1.23);
    	taskParams.setParameter("Value3", "abc");
    	d.setTaskParams(taskParams);
    	
    	return d;
    }
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	Directive original = getTestDirective();
    	Directive cloned = original.clone();
    	assertTrue(original.equals(cloned));
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
    	String ddString = "data" 
    			+ ChemSoftConstants.JDDATAVALSEPARATOR
				+ ChemSoftConstants.JDLABACCTASK
				+ ParameterConstants.SEPARATOR
				+ PathnameEditor.GETPATHNAMETASK.casedID + NL
				+ ChemSoftConstants.PARGETFILENAMEROOTSUFFIX 
				+ ParameterConstants.SEPARATOR + ".sfx";
    	d.addDirectiveData(DirectiveData.makeFromJDLine(ddString));
    	
    	Job j = JobFactory.createJob(AppID.ACC);
    	j.setParameter(ChemSoftConstants.PAROUTFILEROOT,"/path/t/filenameRoot");
    	
    	d.performACCTasks(null, j, null);
    	
    	assertTrue(d.getFirstDirectiveData("data").getValue().toString()
    			.contains(".sfx"),
    			"Task changing DirectiveData");
    	
    	Directive d2 = new Directive("testDirective");
    	d2.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList(
    					ChemSoftConstants.JDLABACCTASK
    					+ ParameterConstants.SEPARATOR
    					+ PathnameEditor.GETPATHNAMETASK.casedID,
    					ChemSoftConstants.PARGETFILENAMEROOTSUFFIX 
    					+ ParameterConstants.SEPARATOR + "_job2.xyz"))));

    	d2.performACCTasks(null, j, null);
    	
    	assertTrue(d2.getFirstKeyword("key1").getValue().toString().contains(
    			"filenameRoot_job2.xyz"),
    			"Task changing keyword");
    	

    	Directive d3 = getTestDirective();
    	d3.removeACCTasks();
    	d3.deleteComponent(d3.getAllDirectiveDataBlocks().get(0));

    	ParameterStorage taskParams = new ParameterStorage();
    	taskParams.setParameter(WorkerConstants.PARTASK,
    			//TODO-gg take task from worker
    			Task.make("addAtomSpecificKeywords").casedID);
    	taskParams.setParameter("KeywordName", "ATMSPECKW");
    	taskParams.setParameter("SMARTS", "[#6] options:~~@#BLA");
    	d3.setTaskParams(taskParams);
    	
    	IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("C",new Point3d(0,0,0.0)));
    	mol.addAtom(new Atom("O",new Point3d(5.0,0,0)));
    	mol.addAtom(new Atom("N",new Point3d(10.0,0,0)));
    	mol.addAtom(new Atom("C",new Point3d(12.0,0,0.0)));
    	mol.addAtom(new Atom("C",new Point3d(14.0,0,0.0)));
    	mol.addAtom(new Atom("N",new Point3d(10.0,10,0)));
    	mol.addAtom(new Atom("C",new Point3d(12.0,10,0.0)));
    	
    	d3.performACCTasks(new ArrayList<IAtomContainer>(Arrays.asList(mol)), j,
    			null);
    	
    	assertEquals(6, d3.getAllKeywords().size());
    	int newKeys = 0;
    	for (Keyword k : d3.getAllKeywords())
    	{
    		if (k.getName().startsWith("ATMSPECKW"))
    			newKeys++;
    	}
    	assertEquals(4, newKeys);
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
    	d.addKeyword(new Keyword("key1", true, 
    			new ArrayList<String>(Arrays.asList("x","y"))));
    	d.addKeyword(new Keyword("key2", true, 
    			new ArrayList<String>(Arrays.asList("c","d"))));
    	d.addDirectiveData(new DirectiveData("data", new ArrayList<String>(
    			Arrays.asList("A","B","C"))));

    	List<IDirectiveComponent> cs = d.getComponent("key1", 
    			DirectiveComponentType.KEYWORD);
    	assertEquals(2, cs.size());
    	assertEquals("a b",((Keyword) cs.get(0)).getValueAsString(), 
    			"Retrieve Keyword");
    	assertEquals("x y",((Keyword) cs.get(1)).getValueAsString(), 
    			"Retrieve Keyword");
    	
    	cs = d.getComponent("key2", DirectiveComponentType.KEYWORD);
    	assertEquals(1, cs.size());
    	assertEquals("c d",((Keyword) cs.get(0)).getValueAsString(), 
    			"Retrieve Keyword");
    	
    	List<IDirectiveComponent> dds = d.getComponent("data", 
    			DirectiveComponentType.DIRECTIVEDATA);
    	assertEquals("A",((TextBlock) 
    			((DirectiveData) dds.get(0)).getValue()).get(0),
    			"Retrieve DirectiveData");
    	
    	List<IDirectiveComponent> xs = d.getComponent("notThere", 
    			DirectiveComponentType.DIRECTIVEDATA);
    	
    	assertEquals(0, xs.size(), "Retriving missing object");
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
    	
    	IDirectiveComponent comp = d.getFirstKeyword("key2");
    	d.deleteComponent(comp);
    	
    	assertEquals(1,d.getAllDirectiveDataBlocks().size(),
    			"Number of DirectiveData blocks (B)");
    	assertEquals(1,d.getAllKeywords().size(),
    			"Number of Keywords (B)");
    	
    	IDirectiveComponent comp2 = d.getFirstDirectiveData("data");
    	d.deleteComponent(comp2);
    	
    	assertEquals(0,d.getAllDirectiveDataBlocks().size(),
    			"Number of DirectiveData blocks (A)");
    	assertEquals(1,d.getAllKeywords().size(),
    			"Number of Keywords (A)");
    	
    }
    
//------------------------------------------------------------------------------

}
