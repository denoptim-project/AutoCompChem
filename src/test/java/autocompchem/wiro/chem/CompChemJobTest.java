package autocompchem.wiro.chem;

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
import java.util.List;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for CompChemJob.
 * 
 * @author Marco Foscato
 */

public class CompChemJobTest 
{
	private final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------
    
	/**
	 * Creates a dummy object that has both directives and steps.
	 * @return and object good for testing, not necessarily meaningful in
	 * actual usage.
	 * 
	 * MainJob:
	 * A_kA1,kA1,kA2_dataA
	 *  \
	 *   \-AA_kAA1
	 * Steps:
	 *   Step 1:
	 *   A_kA1,kA1
	 */
    public static CompChemJob getTextCompChemJob()
    {
    	CompChemJob ccj = new CompChemJob();
    	
    	Directive dA = new Directive("A");
    	dA.addKeyword(new Keyword("kA1", false, "valueKA1"));
    	dA.addKeyword(new Keyword("kA1", false, "valueKA1bis"));
    	dA.addKeyword(new Keyword("kA2", false, "valueKA2"));
    	Directive dAA = new Directive("AA");
    	dAA.addKeyword(new Keyword("kAA1", false, "valueKAA1"));
    	dA.addSubDirective(dAA);
    	DirectiveData dd = new DirectiveData("dataA");
    	dd.setValue("Value of directive data");
    	dA.addDirectiveData(dd);
    	
    	ccj.addDirective(dA);
    	
    	for (int i=0; i<3; i++)
    	{
	    	CompChemJob stepI = new CompChemJob();
	    	Directive dConstant = new Directive("A");
	    	dConstant.addKeyword(new Keyword("kA1", false, "valueKA1"));
	    	dConstant.addKeyword(new Keyword("kA1", false, "valueKA1bis"));
	    	stepI.addDirective(dConstant);
	    	/*
	    	Directive dI = new Directive("Dir"+i);
	    	dI.addKeyword(new Keyword("k", false, i));
	    	*/
	    	ccj.addStep(stepI);
    	}
    	
    	return ccj;
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
    	CompChemJob a = getTextCompChemJob();
    	CompChemJob clone = a.clone();
    	
    	assertTrue(a.equals(clone));
    	assertTrue(clone.equals(a));
    	assertTrue(clone.equals(clone));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testEquals() throws Exception
    {
    	CompChemJob a = getTextCompChemJob();
    	CompChemJob b = getTextCompChemJob();
    	
    	assertTrue(a.equals(a));
    	assertTrue(a.equals(b));
    	assertTrue(b.equals(a));
    	assertFalse(a.equals(null));
    	
    	b.getDirective(0).getAllKeywords().get(0).setValue("CHANGED");
    	assertFalse(a.equals(b));
    	
    	b = getTextCompChemJob();
    	b.getDirective(0).addKeyword(new Keyword("NEW", false, "CHANGED"));
    	assertFalse(a.equals(b));

    	b = getTextCompChemJob();
    	b.addDirective(new Directive("NewD"));
    	assertFalse(a.equals(b));
    	
    	b = getTextCompChemJob();
    	b.addStep(new CompChemJob());
    	assertFalse(a.equals(b));

    	b = getTextCompChemJob();
    	a.getDirective(0).addKeyword(new Keyword("NEW", false, "CHANGED"));
    	b.getDirective(0).addKeyword(new Keyword("NEW", false, "CHANGED"));
    	assertTrue(a.equals(b));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromJD() throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(ChemSoftConstants.JDCOMMENT+"this is just a test job details");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "! " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "method"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "XTB2");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "! " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "jobType"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "NumFreq");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "%pal " 
    			+ ChemSoftConstants.JDLABLOUDKEY + "nproc"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "10");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "charge"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "0");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "spinMultiplicity"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "1");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* "
    			+ ChemSoftConstants.JDLABDATA + "geom-0"
    			+ ChemSoftConstants.JDDATAVALSEPARATOR
    			+ ChemSoftConstants.JDOPENBLOCK
    			+ "  C   0.000 0.000 0.000" + NL
    			+ "  O   0.000 0.000 1.130" 
    			+ ChemSoftConstants.JDCLOSEBLOCK);
    	
    	lines.add(ChemSoftConstants.JDLABSTEPSEPARATOR);

    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "! " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "method"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "XTB2");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "! " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "jobType"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "Opt");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "%pal " 
    			+ ChemSoftConstants.JDLABLOUDKEY + "nproc"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "10");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "%geom "
    			+ ChemSoftConstants.JDLABDIRECTIVE + "Constraints"
    			+ ChemSoftConstants.JDLABDATA + "constraindData"
    			+ ChemSoftConstants.JDDATAVALSEPARATOR + "{B 0 1 C}");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "%geom "
    			+ ChemSoftConstants.JDLABDIRECTIVE + "Inhess"
    			+ ChemSoftConstants.JDLABMUTEKEY + "inputHessianMode"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "Read");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "%geom "
    			+ ChemSoftConstants.JDLABDIRECTIVE + "InHessName"
    			+ ChemSoftConstants.JDLABMUTEKEY + "fileName"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "file.hess");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "charge"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "0");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "spinMultiplicity"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "1");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* "
    			+ ChemSoftConstants.JDLABDATA + "geom-1"
    			+ ChemSoftConstants.JDDATAVALSEPARATOR
    			+ ChemSoftConstants.JDOPENBLOCK
    			+ "  C   0.000 0.000 0.000" + NL
    			+ "  O   0.000 0.000 1.130" 
    			+ ChemSoftConstants.JDCLOSEBLOCK);
    	
    	lines.add(ChemSoftConstants.JDLABSTEPSEPARATOR);
    	
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "! " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "method"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "XTB2");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "! " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "jobType"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "Opt");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "%pal " 
    			+ ChemSoftConstants.JDLABLOUDKEY + "nproc"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "10");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "charge"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "0");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* " 
    			+ ChemSoftConstants.JDLABMUTEKEY + "spinMultiplicity"
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "1");
    	lines.add(ChemSoftConstants.JDLABDIRECTIVE + "* "
    			+ ChemSoftConstants.JDLABDATA + "geom-2"
    			+ ChemSoftConstants.JDDATAVALSEPARATOR
    			+ ChemSoftConstants.JDOPENBLOCK
    			+ "  C   0.000 0.000 0.000" + NL
    			+ "  O   0.000 0.000 1.130" 
    			+ ChemSoftConstants.JDCLOSEBLOCK);
    	
    	CompChemJob job = new CompChemJob(lines);
    	
    	assertEquals(3,job.getNumberOfSteps(),"Number of Orca steps");
    	assertEquals("NumFreq",((CompChemJob)job.getStep(0)).getDirective("!")
    			.getFirstKeyword("jobType").getValueAsString(),
    			"Check imported dir (A)");
    	assertEquals("0",((CompChemJob)job.getStep(1)).getDirective("*")
    			.getFirstKeyword("charge").getValueAsString(),
    			"Check imported dir (B)");
    	assertEquals("Opt",((CompChemJob)job.getStep(2)).getDirective("!")
    	    			.getFirstKeyword("jobType").getValueAsString(),
    	    			"Check imported dir (C)");
    	assertTrue(((CompChemJob)job.getStep(0)).getDirective("*")
    			.hasComponent("geom-0",DirectiveComponentType.DIRECTIVEDATA),
    			"Existence of data block (0)");
    	assertTrue(((CompChemJob)job.getStep(1)).getDirective("*")
    			.hasComponent("geom-1",DirectiveComponentType.DIRECTIVEDATA),
    			"Existence of data block (1)");
    	assertTrue(((CompChemJob)job.getStep(2)).getDirective("*")
    			.hasComponent("geom-2",DirectiveComponentType.DIRECTIVEDATA),
    			"Existence of data block (2)");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetDirectiveComponent() throws Exception
    {
    	CompChemJob ccj = new CompChemJob();
    	Directive dA = new Directive("A1");
    	Keyword k1 = new Keyword("K2");
    	Keyword k2 = new Keyword("K2");
    	Keyword k3 = new Keyword("K2");
    	dA.addKeyword(k1);
    	dA.addKeyword(k2);
    	dA.addKeyword(k3);
    	dA.addDirectiveData(new DirectiveData("data2"));
    	dA.addDirectiveData(new DirectiveData("data2"));
    	dA.addDirectiveData(new DirectiveData("data2"));
    	dA.addSubDirective(new Directive("subdir2"));
    	dA.addDirectiveData(new DirectiveData("data2"));
    	dA.addSubDirective(new Directive("subdir2"));
    	Directive dA2 = new Directive("A2");
    	Keyword k22 = new Keyword("K2");
    	dA2.addKeyword(k22);
    	DirectiveData dd2 = new DirectiveData("data2");
    	dA2.addDirectiveData(dd2);
    	dA.addSubDirective(dA2);
    	ccj.addDirective(dA);
    	
    	Directive dB1 = new Directive("B1");
    	Directive dB2 = new Directive("B2");
    	Directive dB3 = new Directive("B3");
    	Directive dB4 = new Directive("B4");
    	Directive dB5 = new Directive("B5");
    	dB1.addSubDirective(dB2);
    	dB2.addSubDirective(dB3);
    	dB3.addSubDirective(dB4);
    	dB4.addSubDirective(dB5);
    	Keyword k6 = new Keyword("K6");
    	dB5.addKeyword(k6);
    	DirectiveData dd6 = new DirectiveData("data6");
    	dB5.addDirectiveData(dd6);
    	Keyword k3b = new Keyword("K3");
    	Keyword k3c = new Keyword("K3");
    	dB2.addKeyword(k3b);
    	dB2.addKeyword(k3c);
    	ccj.addDirective(dB1);
    	
    	Directive dC1 = new Directive("C");
    	Directive dC2 = new Directive("C");
    	Directive dC3 = new Directive("C");
    	Directive dC12 = new Directive("C");
    	Directive dC22 = new Directive("C");
    	Directive dC32 = new Directive("C");
    	Keyword k31 = new Keyword("K3", false, "val1");
    	Keyword k32 = new Keyword("K3", true, "val2");
    	Keyword k33 = new Keyword("K3", false, "val3");
    	dC12.addKeyword(k31);
    	dC22.addKeyword(k32);
    	dC32.addKeyword(k33);
    	dC1.addSubDirective(dC12);
    	dC1.addSubDirective(dC22);
    	dC1.addSubDirective(dC32);
    	ccj.addDirective(dC1);
    	ccj.addDirective(dC2);
    	ccj.addDirective(dC3);
    	
    	Directive dE1 = new Directive("E");
    	Directive dE2 = new Directive("E");
    	Directive dE3 = new Directive("E");
    	Keyword ke3 = new Keyword("KE");
    	DirectiveData dde3 = new DirectiveData("KE");
    	Directive dKE3 = new Directive("KE");
    	dE3.addKeyword(ke3);
    	dE3.addDirectiveData(dde3);
    	dE3.addSubDirective(dKE3);
    	dE2.addSubDirective(dE3);
    	dE1.addSubDirective(dE2);
    	ccj.addDirective(dE1);
    	
    	
    	/* This is the structure of components:
    	 * 
    	 * A1_K2,K2,K2__data2_data2_data2_data2
    	 *  \
    	 *   \--subdir2
    	 *    \
    	 *     \--subdir2
    	 *      \
    	 *       \--A2_K2__data2
    	 *  
    	 * B1
    	 *   \
    	 *    \--B2_K3_K3
    	 *        \
    	 *         \-B3
    	 *            \
    	 *             \-B4
    	 *                \
    	 *                 \-B5_K6_data6
    	 * 
    	 * C
    	 *  \-C_K3
    	 * 
    	 * C
    	 *  \-C_K3
    	 * 
    	 * C
    	 *  \-C_K3
    	 *  
    	 * E
    	 *  \
    	 *   \-E
    	 *      \
    	 *       \-E_KE(key)__KE(dirData)
    	 *          \
    	 *           \-KE(directive)
    	 * 
    	 */
    	
    	
    	DirComponentAddress adrs = new DirComponentAddress();
    	List<IDirectiveComponent> matches = new ArrayList<IDirectiveComponent>();
    	List<IDirectiveComponent> expected = new ArrayList<IDirectiveComponent>();
    	
    	matches = ccj.getDirectiveComponents(adrs);
    	assertEquals(0, matches.size());
    	assertEquals(expected, matches);
    	
    	adrs.addStep("A1","Dir");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.add(dA);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep("A1","Dir");
    	adrs.addStep("A2","Dir");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(dA2);
    	assertEquals(expected, matches);

    	adrs = new DirComponentAddress();
    	adrs.addStep("A1","Dir");
    	adrs.addStep("K2","KEY");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(k1);
    	expected.add(k2);
    	expected.add(k3);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep("A1","Dir");
    	adrs.addStep("A2","Dir");
    	adrs.addStep("K2","KEY");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(k22);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep("A1","Dir");
    	adrs.addStep("A2","Dir");
    	adrs.addStep("data2","Dat");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(dd2);
    	assertEquals(expected, matches);

    	adrs = new DirComponentAddress();
    	adrs.addStep("C","Dir");
    	adrs.addStep("C","Dir");
    	adrs.addStep("K3","key");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(k31);
    	expected.add(k32);
    	expected.add(k33);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep("B1","Dir");
    	adrs.addStep("B2","Dir");
    	adrs.addStep("B3","Dir");
    	adrs.addStep("B4","Dir");
    	adrs.addStep("B5","Dir");
    	adrs.addStep("K6","KEY");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(k6);
    	assertEquals(expected, matches);
    	
    	// From here with wildcard

    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(dA);
    	expected.add(dB1);
    	expected.add(dC1);
    	expected.add(dC2);
    	expected.add(dC3);
    	expected.add(dE1);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep("K3","key");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	// WARNING: order matters
    	expected.add(k3b);
    	expected.add(k3c);
    	expected.add(k31);
    	expected.add(k32);
    	expected.add(k33);
    	assertEquals(expected, matches);

    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(k6);
    	expected.add(dd6);
    	assertEquals(expected, matches);

    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(dB4);
    	expected.add(ke3);
    	expected.add(dde3);
    	expected.add(dKE3);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep("KE","*");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(ke3);
    	expected.add(dde3);
    	expected.add(dKE3);
    	assertEquals(expected, matches);
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep("E","*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"*");
    	adrs.addStep(DirComponentAddress.ANYNAME,"key");
    	matches = ccj.getDirectiveComponents(adrs);
    	expected.clear();
    	expected.add(ke3);
    	assertEquals(expected, matches);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSetDirComponentValue() throws Exception
    {
    	CompChemJob originalCcj = getTextCompChemJob();
    	CompChemJob modifiedCcj = getTextCompChemJob();
    	assertTrue(modifiedCcj.equals(originalCcj));
    	
    	// No value can be set in root: only directives are in root
    	DirComponentAddress adrs = new DirComponentAddress();
    	modifiedCcj.setDirComponentValue(adrs, new Keyword("KEY", false, "Bah"));
    	assertTrue(modifiedCcj.equals(originalCcj));
    	
    	// Setting of a Keyword that does not exist does nothing
    	String expectedValue = "Bah";
     	adrs = new DirComponentAddress();
    	adrs.addStep("A", DirectiveComponentType.DIRECTIVE);
    	modifiedCcj.setDirComponentValue(adrs, new Keyword("K2", false, 
    			expectedValue));
    	assertTrue(modifiedCcj.equals(originalCcj));

    	// Setting of a keyword that does exist changes value of any keyword
    	// that matches the address
     	adrs = new DirComponentAddress();
    	adrs.addStep("A", DirectiveComponentType.DIRECTIVE);
    	modifiedCcj.setDirComponentValue(adrs, new Keyword("kA1", 
    			false, expectedValue));
    	assertFalse(modifiedCcj.equals(originalCcj));
    	adrs.addStep("kA1", DirectiveComponentType.KEYWORD);
    	List<IDirectiveComponent> comps = new ArrayList<IDirectiveComponent>();
    	comps = modifiedCcj.getDirectiveComponents(adrs);
    	assertEquals(2, comps.size());
    	for (IDirectiveComponent k : comps)
    	{
    		assertEquals(expectedValue, 
    				((IValueContainer)k).getValue().toString());
    	}
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAddNewValueContainer() throws Exception
    {
    	CompChemJob originalCcj = getTextCompChemJob();
    	CompChemJob modifiedCcj = getTextCompChemJob();
    	assertTrue(modifiedCcj.equals(originalCcj));
    	
    	// No value can be set in root: only directives are in root
    	DirComponentAddress adrs = new DirComponentAddress();
    	modifiedCcj.addNewValueContainer(adrs, 
    			new Keyword("KEY", false, "Bah"));
    	assertTrue(modifiedCcj.equals(originalCcj));

    	// Setting of a Keyword that does exist does not do anything
    	String expectedValue = "changed_value";
     	adrs = new DirComponentAddress();
    	adrs.addStep("A", DirectiveComponentType.DIRECTIVE);
    	modifiedCcj.addNewValueContainer(adrs, 
    			new Keyword("kA2", false, expectedValue));
    	assertTrue(modifiedCcj.equals(originalCcj));
    	
    	// Setting of a Keyword that does not exist adds that keyword
     	adrs = new DirComponentAddress();
    	adrs.addStep("A", DirectiveComponentType.DIRECTIVE);
    	IValueContainer valueContainer = new Keyword("k2", false, expectedValue);
    	modifiedCcj.addNewValueContainer(adrs, valueContainer);
    	assertFalse(modifiedCcj.equals(originalCcj));
    	adrs.addStep("k2", DirectiveComponentType.KEYWORD);
    	List<IDirectiveComponent> comps = modifiedCcj.getDirectiveComponents(
    			adrs);
    	assertEquals(1, comps.size());
    	assertTrue(valueContainer == comps.get(0));

    	// Setting values to multiple paths matching the address
    	modifiedCcj = getTextCompChemJob();
    	Directive d = new Directive("D");
    	d.addKeyword(new Keyword("dd", false, "ddValue"));
    	modifiedCcj.addDirective(d);
    	Directive d2 = new Directive("D");
    	d2.addKeyword(new Keyword("dd", false, "ddValue"));
    	modifiedCcj.addDirective(d2);
    	adrs = new DirComponentAddress();
    	adrs.addStep("D", DirectiveComponentType.DIRECTIVE);
    	valueContainer = new Keyword("KEY", false, expectedValue);
    	modifiedCcj.addNewValueContainer(adrs, valueContainer);
    	adrs.addStep("KEY", DirectiveComponentType.KEYWORD);
    	comps = modifiedCcj.getDirectiveComponents(adrs);
    	assertEquals(2, comps.size());
    	
    	adrs = new DirComponentAddress();
    	adrs.addStep(DirComponentAddress.ANYNAME, DirectiveComponentType.DIRECTIVE);
    	valueContainer = new Keyword("KEYB", false, expectedValue);
    	modifiedCcj.addNewValueContainer(adrs, valueContainer);
    	adrs.addStep("KEYB", DirectiveComponentType.KEYWORD);
    	comps = modifiedCcj.getDirectiveComponents(adrs);
    	assertEquals(3, comps.size());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testEnsureDirectiveStructure() throws Exception
    {
    	CompChemJob ccj = new CompChemJob();
    	Directive dA1 = new Directive("A1");
    	Directive dA2 = new Directive("A2");
    	Keyword kA1 = new Keyword("KA1");
    	Keyword kA2 = new Keyword("KA2");
    	dA1.addKeyword(kA1);
    	dA1.addSubDirective(dA2);
    	dA2.addKeyword(kA2);
    	ccj.addDirective(dA1);
    	
    	Directive dB1 = new Directive("B1");
    	Directive dB2 = new Directive("B2");
    	Directive dB3 = new Directive("B3");
    	Directive dB4 = new Directive("B4");
    	Directive dB5 = new Directive("B5");
    	dB1.addSubDirective(dB2);
    	dB2.addSubDirective(dB3);
    	dB3.addSubDirective(dB4);
    	dB4.addSubDirective(dB5);
    	ccj.addDirective(dB1);
    	
    	Directive dC1 = new Directive("C");
    	Directive dC2 = new Directive("C");
    	Directive dC3 = new Directive("C");
    	dC1.addSubDirective(dC2);
    	dC1.addSubDirective(dC3);
    	ccj.addDirective(dC1);
    	
    	/* The structure of directives is this one:
    	 *  
    	 *  A1 -- A2
    	 *    \     \
    	 *     kA1   kA2
    	 *     
    	 *  B1 -- B2 -- B3 -- B4 -- B5
    	 *  
    	 *  C -- C
    	 *   \
    	 *    -- C
    	 *  
    	 */
    	
    	DirComponentAddress adrs = DirComponentAddress.fromString(
    			"Dir:D");
    	List<IDirectiveComponent> before = ccj.getDirectiveComponents(adrs);
    	assertEquals(0, before.size());
    	ccj.ensureDirectiveStructure(adrs);
    	List<IDirectiveComponent> after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertEquals("D", after.get(0).getName());
    	
    	adrs = DirComponentAddress.fromString("Dir:A1|Dir:A2");
     	before = ccj.getDirectiveComponents(adrs);
     	assertEquals(1, before.size());
     	ccj.ensureDirectiveStructure(adrs);
     	after = ccj.getDirectiveComponents(adrs);
     	assertEquals(1, after.size());
     	assertEquals("A2", after.get(0).getName());
    	
    	adrs = DirComponentAddress.fromString("Dir:A1|Dir:A2|Key:KZ");
     	before = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, before.size());
     	assertEquals(1, dA2.getAllKeywords().size());
     	assertEquals(0, dA2.getAllSubDirectives().size());
     	assertEquals(0, dA2.getAllDirectiveDataBlocks().size());
     	ccj.ensureDirectiveStructure(adrs);
     	assertEquals(1, dA2.getAllKeywords().size());
     	assertEquals(0, dA2.getAllSubDirectives().size());
     	assertEquals(0, dA2.getAllDirectiveDataBlocks().size());
     	after = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, after.size());
     	
     	adrs = DirComponentAddress.fromString("Dir:A1|Dir:A2|Dir:A3|Dir:A4");
     	before = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, before.size());
     	ccj.ensureDirectiveStructure(adrs);
     	after = ccj.getDirectiveComponents(adrs);
     	assertEquals(1, after.size());
     	assertEquals("A4", after.get(0).getName());
     	
     	adrs = DirComponentAddress.fromString(
     			"*:" + DirComponentAddress.ANYNAME + "|" 
     			+ "*:" + DirComponentAddress.ANYNAME + "|" 
     			+ "*:" + DirComponentAddress.ANYNAME + "|Dir:B4b");
     	before = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, before.size());
     	boolean found = false;
     	try {
     		ccj.ensureDirectiveStructure(adrs);
     	} catch (IllegalArgumentException e)
     	{
     		if (e.getMessage().contains("Found multiple parent"))
     			found = true;
     	}
     	assertTrue(found);
     	
     	adrs = DirComponentAddress.fromString(
     			"*:" + DirComponentAddress.ANYNAME + "|" 
     			+ "*:" + DirComponentAddress.ANYNAME + "|" 
     			+ "*:B3|Dir:B4b");
     	before = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, before.size());
     	ccj.ensureDirectiveStructure(adrs);
     	after = ccj.getDirectiveComponents(adrs);
     	assertEquals(1, after.size());
     	assertEquals("B4b", after.get(0).getName());
     	
    	// Make sure we do not create a structure if the names required are "*"
    	// This DOES add directive "Z" but not its sub directive.
     	adrs = DirComponentAddress.fromString("*:Z|"
     			+ "*:" + DirComponentAddress.ANYNAME + "|Dir:ZZ");
     	before = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, before.size());
     	ccj.ensureDirectiveStructure(adrs);
     	after = ccj.getDirectiveComponents(adrs);
     	assertEquals(0, after.size());
     	after = ccj.getDirectiveComponents(
     			DirComponentAddress.fromString("*:Z|"
     					+ "*:" + DirComponentAddress.ANYNAME));
     	assertEquals(0, after.size());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAddDirectiveComponent() throws Exception
    {
    	CompChemJob ccj = new CompChemJob();
    	
    	Directive dC1 = new Directive("C");
    	Directive dC2 = new Directive("C");
    	Directive dC3 = new Directive("C");
    	Directive dO = new Directive("O");
    	dC1.addSubDirective(dC2);
    	dC1.addSubDirective(dC3);
    	dC1.addSubDirective(dO);
    	ccj.addDirective(dC1);
    	
    	/* The structure of directives is this one:
    	 *  
    	 *  C -- C
    	 *  | \
    	 *  |  -- C
    	 *   \
    	 *    ---O 
    	 *  
    	 */
    	
    	// Test adding new component
    	// On existing parent
    	Directive newDir = new Directive("N1");
    	DirComponentAddress parentAdrs = DirComponentAddress.fromString(
    			"Dir:C");
    	DirComponentAddress adrs = DirComponentAddress.fromString(
    			"Dir:C|Dir:N1");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir, true, true));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	List<IDirectiveComponent> after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDir);

    	newDir = new Directive("N2");
    	parentAdrs = DirComponentAddress.fromString("Dir:C");
    	adrs = DirComponentAddress.fromString("Dir:C|Dir:N2");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir, true, false));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDir);

    	newDir = new Directive("N3");
    	parentAdrs = DirComponentAddress.fromString("Dir:C");
    	adrs = DirComponentAddress.fromString("Dir:C|Dir:N3");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir, false, true));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDir);

    	newDir = new Directive("N4");
    	parentAdrs = DirComponentAddress.fromString("Dir:C");
    	adrs = DirComponentAddress.fromString("Dir:C|Dir:N4");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir, false, false));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDir);
    	
    	// Test overwriting existing component or appending besides
    	// On existing parent
    	Directive newDir0 = new Directive("O");
    	parentAdrs = DirComponentAddress.fromString("Dir:C");
    	adrs = DirComponentAddress.fromString("Dir:C|Dir:O");
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	List<IDirectiveComponent> before = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, before.size());
    	assertTrue(before.get(0)==dO);
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir0, true, true));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)!=before.get(0));
    	assertTrue(after.get(0)==newDir0);

    	Directive newDir1 = new Directive("O");
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	before = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, before.size());
    	assertTrue(before.get(0)==newDir0);
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir1, true, false));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDir1);

    	Directive newDir2 = new Directive("O");
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	before = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, before.size());
    	assertTrue(before.get(0)==newDir1);
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir2, false, true));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(2, after.size());
    	assertTrue(after.get(0)==newDir1); //OK, did not overwrite original
    	assertTrue(after.get(1)==newDir2); //OK, appended new

    	Directive newDir3 = new Directive("O");
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	before = ccj.getDirectiveComponents(adrs);
    	assertEquals(2, before.size());
    	assertTrue(before.get(0)==newDir1);
    	assertTrue(before.get(1)==newDir2);
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDir3, false, false));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(3, after.size());
    	assertTrue(after.get(0)==newDir1); //OK, did not overwrite original
    	assertTrue(after.get(1)==newDir2); //OK, did not overwrite original
    	assertTrue(after.get(2)==newDir3); //OK, appended new
    	
       	// Test adding new component
    	// On un-existing parent
    	Directive newDirR1 = new Directive("R1");
    	parentAdrs = DirComponentAddress.fromString("Dir:Z1");
    	adrs = DirComponentAddress.fromString("Dir:Z1|Dir:R1");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDirR1, true, true));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDirR1);

    	Directive newDirR2 = new Directive("R2");
    	parentAdrs = DirComponentAddress.fromString("Dir:Z2");
    	adrs = DirComponentAddress.fromString("Dir:Z2|Dir:R2");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	assertFalse(ccj.addDirectiveComponent(parentAdrs, newDirR2, true, false));
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(0, after.size());

    	Directive newDirR3 = new Directive("R3");
    	parentAdrs = DirComponentAddress.fromString("Dir:Z3");
    	adrs = DirComponentAddress.fromString("Dir:Z3|Dir:R3");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	assertTrue(ccj.addDirectiveComponent(parentAdrs, newDirR3, false, true));
    	assertTrue(ccj.hasDirectiveStructure(adrs));
    	assertTrue(ccj.hasDirectiveStructure(parentAdrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDirR3);

    	Directive newDirR4 = new Directive("R4");
    	parentAdrs = DirComponentAddress.fromString("Dir:Z4");
    	adrs = DirComponentAddress.fromString("Dir:Z4|Dir:R4");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	assertFalse(ccj.addDirectiveComponent(parentAdrs, newDirR4, false, false));
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(0, after.size());
    	
    	// Testing that we can only add a Directive as root component
    	Keyword keyAdded = new Keyword("NEW-KEY", false, "added keyword value");
    	adrs = new DirComponentAddress();
    	boolean throwed = false;
     	try {
     		ccj.addDirectiveComponent(adrs, keyAdded, true, true);
     	} catch (IllegalArgumentException e)
     	{
     		if (e.getMessage().contains("can be root"))
     			throwed = true;
     	}
     	assertTrue(throwed);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSetDirectiveComponent() throws Exception
    {
    	CompChemJob ccj = new CompChemJob();
    	
    	Directive dC1 = new Directive("C");
    	Directive dC2 = new Directive("C");
    	Directive dC3 = new Directive("C");
    	dC1.addSubDirective(dC2);
    	dC1.addSubDirective(dC3);
    	ccj.addDirective(dC1);
    	
    	/* The structure of directives is this one:
    	 *  
    	 *  C -- C
    	 *   \
    	 *    -- C
    	 *  
    	 */
    	
    	// Test adding on existing location
    	Directive newDir = new Directive("NewDir");
    	DirComponentAddress parentAdrs = DirComponentAddress.fromString(
    			"Dir:C");
    	DirComponentAddress adrs = DirComponentAddress.fromString(
    			"Dir:C|Dir:NewDir");
    	List<IDirectiveComponent> before = ccj.getDirectiveComponents(adrs);
    	assertEquals(0, before.size());
    	assertTrue(ccj.setDirectiveComponent(parentAdrs, newDir));
    	List<IDirectiveComponent> after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)==newDir);
    	
    	// Test overwriting existing component
    	newDir = new Directive("C");
    	parentAdrs = DirComponentAddress.fromString("Dir:C");
    	adrs = DirComponentAddress.fromString("Dir:C|Dir:C");
    	before = ccj.getDirectiveComponents(adrs);
    	assertEquals(2, before.size());
    	assertTrue(ccj.setDirectiveComponent(parentAdrs, newDir));
    	after = ccj.getDirectiveComponents(adrs);
    	assertEquals(1, after.size());
    	assertTrue(after.get(0)!=before.get(0));
    	
    	// Test ignore unexisting location
    	newDir = new Directive("ZZ");
    	parentAdrs = DirComponentAddress.fromString("Dir:C|Dir:Z");
    	adrs = DirComponentAddress.fromString("Dir:C|Dir:Z|Dir:ZZ");
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    	assertFalse(ccj.setDirectiveComponent(parentAdrs, newDir));
    	assertFalse(ccj.hasDirectiveStructure(adrs));
    	assertFalse(ccj.hasDirectiveStructure(parentAdrs));
    }
    
//------------------------------------------------------------------------------

}
