package autocompchem.wiro.chem.xtb;


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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.FileAnalyzer;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.wiro.WIROConstants;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.Keyword;
import autocompchem.worker.WorkerConstants;


/**
 * Unit Test for the writer of computational chemistry software input files.
 * 
 * @author Marco Foscato
 */

public class XTBInputWriterTest 
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir 
    File tempDir;
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testJobDetailsSources() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory");
        File tmpDir1 = new File(tempDir.getAbsolutePath() + SEP + "1");
        File tmpDir2 = new File(tempDir.getAbsolutePath() + SEP + "2");
        tmpDir1.mkdir();
        tmpDir2.mkdir();
        
        File jdFile = new File(tmpDir1.getAbsolutePath() + SEP + "ccj.jd");
        File parFile = new File(tmpDir1.getAbsolutePath() + SEP + "acc.par");
    	File molFile = new File(tmpDir1.getAbsolutePath() + SEP + "mol.sdf");
    	final String INPNAMEROOT = "molinp";
    	String inpRoot = tmpDir1.getAbsolutePath() + SEP + INPNAMEROOT;

    	IAtomContainer mol = new AtomContainer();
    	mol.addAtom(new Atom("C", new Point3d(1.0,2.0,3.0)));
    	mol.addAtom(new Atom("O", new Point3d(2.5,2.0,3.0)));
    	mol.addBond(0, 1, IBond.Order.TRIPLE);
    	IOtools.writeSDFAppend(molFile, mol, false);
    	
    	Directive d1 = new Directive("opt");
    	d1.addKeyword(new Keyword("logfile", true, "trajectory.xyz"));
    	Directive d2 = new Directive("charge");
    	// The syntax is inconsistent in current XTB implementation: charge and 
    	// spin multiplicity (well, actually the N_alphs-N_beta electrons) are
    	// contradicting the declaration that after '$' "the instruction name 
    	// is the rest of the register"
    	d2.addKeyword(new Keyword("value", false, "0"));
    	Directive ds = new Directive("spin");
    	ds.addKeyword(new Keyword("value", false, "1"));
    	Directive d3 = new Directive("testDirective");
    	d3.addKeyword(new Keyword("key1", true, "a b"));
    	d3.addKeyword(new Keyword("key2", true, "1 2"));
    	d3.addDirectiveData(new DirectiveData("data",  
    			new ArrayList<String>(Arrays.asList("A1","A2","A3"))));
    	Directive sd = new Directive("subA");
    	sd.addKeyword(new Keyword("sKey", false, 
    			new ArrayList<String>(Arrays.asList("0","1"))));
    	d3.addSubDirective(sd);
    	Directive df = new Directive("fix");
    	df.addKeyword(new Keyword("elements", true, new ArrayList<String>(
    			Arrays.asList("O","C","P"))));
    	
    	CompChemJob ccj = new CompChemJob();
    	ccj.setDirective(d1);
    	ccj.setDirective(d2);
    	ccj.setDirective(d3);
    	ccj.setDirective(ds);
    	ccj.setDirective(df);
    	
    	IOtools.writeTXTAppend(jdFile, 
    			ccj.toLinesJobDetails(), false);
    	
    	ArrayList<String> parLines = new ArrayList<String>();
    	parLines.add(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
        		+ XTBInputWriter.PREPAREINPUTXTBTASK.casedID);
    	parLines.add(ChemSoftConstants.PARGEOMFILE 
        		+ ParameterConstants.SEPARATOR + molFile.getAbsolutePath());
    	parLines.add(WIROConstants.PAROUTFILEROOT
    			+ ParameterConstants.SEPARATOR + inpRoot);
    	parLines.add(WIROConstants.PARJOBDETAILSFILE
        		+ ParameterConstants.SEPARATOR + jdFile.getAbsolutePath());
    	
        IOtools.writeTXTAppend(parFile, parLines, false);

        assertTrue(molFile.exists(),"Mol file exists");
        assertTrue(parFile.exists(),"Par file exists");
        assertTrue(jdFile.exists(),"JD file exists");
        
        Job job = JobFactory.buildFromFile(parFile);
        job.run();
        
        File inpFile = new File(inpRoot + XTBConstants.INPEXTENSION);
        assertTrue(inpFile.exists(), "Xcontrol file exists");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),"$chrg 0"), 
        		"Generation of charge information");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),"$spin 1"), 
        		"Generation of spin-related information");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),
        		"logfile=trajectory.xyz"),"Generation of logfile option");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),
        		"elements: O,C,P"),"Keyword specific separator");
        
        List<String> linesInp = IOtools.readTXT(inpFile);
 
        // Now we do the almost the same, but we give the job details 
        // just a nested block
        // of text in side the job-defining parameters file (paramFile2)
        
        File parFile2 = new File(tmpDir2.getAbsolutePath() + SEP + "acc2.par");
    	String inpRoot2 = tmpDir2.getAbsolutePath() + SEP + INPNAMEROOT;
        
      	ArrayList<String> parLines2 = new ArrayList<String>();
    	parLines2.add(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
        		+ XTBInputWriter.PREPAREINPUTXTBTASK.casedID);
    	parLines2.add(ChemSoftConstants.PARGEOMFILE 
        		+ ParameterConstants.SEPARATOR +  molFile.getAbsolutePath());
    	parLines2.add(WIROConstants.PAROUTFILEROOT
    			+ ParameterConstants.SEPARATOR + inpRoot2);
    	parLines2.add(ChemSoftConstants.PARJOBDETAILS
        		+ ParameterConstants.SEPARATOR 
        		+ ParameterConstants.STARTMULTILINE);
    	parLines2.addAll(ccj.toLinesJobDetails());
    	parLines2.add(ParameterConstants.ENDMULTILINE);

        IOtools.writeTXTAppend(parFile2, parLines2, false);
        
        Job job2 = JobFactory.buildFromFile(parFile2);
        job2.run();
        
        File inpFile2 = new File(inpRoot2 + XTBConstants.INPEXTENSION);
        assertTrue(inpFile2.exists(),"Inp file 2 exists");
        assertTrue(1 == FileAnalyzer.count(inpFile2.getAbsolutePath(),"$chrg 0"), 
        		"Generation of charge information");
        assertTrue(1 == FileAnalyzer.count(inpFile2.getAbsolutePath(),"$spin 1"), 
        		"Generation of spin-related information");
        assertTrue(1 == FileAnalyzer.count(inpFile2.getAbsolutePath(),
        		"logfile=trajectory.xyz"),"Generation of logfile option");
        assertTrue(1 == FileAnalyzer.count(inpFile2.getAbsolutePath(),
        		"elements: O,C,P"),"Keyword specific separator");
        
        List<String> linesInp2 = IOtools.readTXT(inpFile);
        
        assertEquals(linesInp.size(),linesInp2.size(), 
        		"Number of lines in generated input file");
        for (int i=0; i<linesInp.size(); i++)
        {
        	String line = linesInp.get(i);
        	String line2 = linesInp2.get(i);
        	assertEquals(line,line2,"Line of generated input ('"+line+"'");
        }
    }

//------------------------------------------------------------------------------

}
