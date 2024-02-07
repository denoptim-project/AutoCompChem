package autocompchem.chemsoftware.gaussian;


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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.FileAnalyzer;
import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.constraints.ConstraintsGenerator;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.worker.Task;
import autocompchem.worker.WorkerConstants;


/**
 * Unit Test for the writer of computational chemistry software input files.
 * 
 * @author Marco Foscato
 */

public class GaussianInputWriterTest 
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir 
    File tempDir;
    
//-----------------------------------------------------------------------------

    //TODO-gg update or remove @Test
    public void testJobDetailsSource() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory");
        
        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "ccj.jd");
        File parFile = new File(tempDir.getAbsolutePath() + SEP + "acc.par");
    	File molFile = new File(tempDir.getAbsolutePath() + SEP + "mol.sdf");
    	final String INPNAMEROOT = "molinp";
    	String inpRoot = tempDir.getAbsolutePath() + SEP + INPNAMEROOT;

    	IAtomContainer mol = new AtomContainer();
    	mol.addAtom(new Atom("C", new Point3d(1.0,2.0,3.0)));
    	mol.addAtom(new Atom("O", new Point3d(2.5,2.0,3.0)));
    	mol.addBond(0, 1, IBond.Order.TRIPLE);
    	IOtools.writeSDFAppend(molFile, mol, false);
    	
    	CompChemJob ccj = createTestJob();
    	
    	IOtools.writeTXTAppend(jdFile, 
    			ccj.toLinesJobDetails(), false);
    	
    	List<String> parLines = new ArrayList<String>();
    	parLines.add(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
    			+ GaussianInputWriter.PREPAREINPUTGAUSSIANTASK.casedID);
    	parLines.add(ChemSoftConstants.PARGEOMFILE 
        		+ ParameterConstants.SEPARATOR + molFile.getAbsolutePath());
    	parLines.add(ChemSoftConstants.PAROUTFILEROOT
    			+ ParameterConstants.SEPARATOR + inpRoot);
    	parLines.add(ChemSoftConstants.PARJOBDETAILSFILE
        		+ ParameterConstants.SEPARATOR + jdFile.getAbsolutePath());
    	
        IOtools.writeTXTAppend(parFile, parLines, false);

        assertTrue(molFile.exists(),"Mol file exists");
        assertTrue(parFile.exists(),"Par file exists");
        assertTrue(jdFile.exists(),"JD file exists");
        
        Job job = JobFactory.buildFromFile(parFile);
        job.run();
        
        File inpFile = new File(inpRoot + GaussianConstants.GAUINPEXTENSION);
        //TODO-gg
        assertTrue(inpFile.exists(),"Xcontrol file exists");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),"$chrg 0"), 
        		"Generation of charge information");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),"$spin 1"), 
        		"Generation of spin-related information");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),
        		"logfile=trajectory.xyz"),"Generation of logfile option");
        assertTrue(1 == FileAnalyzer.count(inpFile.getAbsolutePath(),
        		"elements: O,C,P"),"Keyword specific separator");
        
        List<String> linesInp = IOtools.readTXT(inpFile);
 
        //TODO-gg this test is not finished: finish it!
    }
    
//------------------------------------------------------------------------------
    
    public CompChemJob createTestJob()
    {
    	//TODO-gg obsolete: tries to reproduce old JobDetails syntax in directives!
    	
    	
    	//
    	// Start definition of first step
    	//
    	
    	Directive dLink = new Directive(GaussianConstants.KEYLINKSEC);
    	Directive dRoute = new Directive(GaussianConstants.KEYROUTESEC);
    	Directive dTitle = new Directive(GaussianConstants.KEYTITLESEC);
    	
    	dLink.setKeyword(new Keyword("chk", true, "randomname.chk"));
    	dLink.setKeyword(new Keyword("Nprocshared", true, "36"));
    	dLink.setKeyword(new Keyword("mem", true, "28GB"));
    	
    	// These are intentionally disordered to verify reordering
    	dRoute.setKeyword(new Keyword(GaussianConstants.SUBKEYMODELMETHOD, 
    			false, "HF"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.SUBKEYMODELBASISET,
    			false, "LANL2MB 5d 7f"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.SUBKEYJOBTYPE, 
    			false, "SP"));
    	dRoute.setKeyword(new Keyword("Print", false, "P"));

    	Directive dSCF = new Directive("SCF");
    	dSCF.addKeyword(new Keyword("VShift", true, "1000"));
    	dSCF.addKeyword(new Keyword("Symm", false, "nosym"));
    	dSCF.addKeyword(new Keyword("MaxCycle", true, "180"));
    	dRoute.addSubDirective(dSCF);

    	dTitle.setKeyword(new Keyword("Title", false, "First step"));
    	
    	CompChemJob ccj = new CompChemJob();
    	// These are intentionally disordered to make sure we reorder them.
    	ccj.setDirective(dTitle);
    	ccj.setDirective(dLink);
    	ccj.setDirective(dRoute);
    	
    	//
    	// Start definition of second step
    	//
    	
    	Directive dRoute2 = new Directive(GaussianConstants.KEYROUTESEC);
    	Directive dTitle2 = new Directive(GaussianConstants.KEYTITLESEC);
    	Directive dOpts2 = new Directive(GaussianConstants.KEYOPTSSEC);

    	dRoute2.setKeyword(new Keyword(GaussianConstants.SUBKEYMODELMETHOD, 
    			false, "OLYP"));
    	dRoute2.setKeyword(new Keyword(GaussianConstants.SUBKEYMODELBASISET,
    			false, "LANL2MB 5d 7f"));
    	dRoute2.setKeyword(new Keyword(GaussianConstants.SUBKEYJOBTYPE, 
    			false, "OPT"));
    	dRoute2.setKeyword(new Keyword("Print", false, "P"));
    	dRoute2.setKeyword(new Keyword(GaussianConstants.GAUKEYGEOM, 
    			true, "check"));
    	dRoute2.setKeyword(new Keyword("guess", true, "read"));

    	Directive dSCF2 = new Directive("SCF");
    	dSCF2.addKeyword(new Keyword("VShift", true, "800"));
    	dSCF2.addKeyword(new Keyword("Symm", false, "nosym"));
    	dSCF2.addKeyword(new Keyword("varacc", false, "novaracc"));
    	dSCF2.addKeyword(new Keyword("MaxCycle", true, "180"));
    	dRoute2.addSubDirective(dSCF2);

    	dTitle2.setKeyword(new Keyword("Title", false, "Second step"));
    	
    	Directive dNBO = new Directive("NBO");
    	dNBO.addDirectiveData(new DirectiveData("NBO", new ArrayList<String>(
    			Arrays.asList("ARCHIVE","npa"))));
    	dOpts2.addSubDirective(dNBO);
    	
    	Directive dBasisSet = new Directive("Basis");
    	dBasisSet.addDirectiveData(new DirectiveData("Basis", new ArrayList<String>(
    			Arrays.asList(ChemSoftConstants.JDLABACCTASK
    					+ ":"
    					+ BasisSetConstants.ATMSPECBS,
    					"SMARTS [#1] name STO-3G",
    					"SMARTS [#7] name STO-33G",
    					"SMRTSS [!$([#1,#7])] name LANLMB"
    					))));
    	dOpts2.addSubDirective(dBasisSet);
    	
    	Directive dModRedundant = new Directive("ModRedundant");
    	dModRedundant.addKeyword(new Keyword("Basis", false, new ArrayList<String>(
    			Arrays.asList(ChemSoftConstants.JDLABACCTASK
    					+ ":"
    	    			+ ConstraintsGenerator.GENERATECONSTRAINTSTASK.casedID,
    					"SMARTS: [#7]",
    					"SMARTS: [$([#6](~[#1])(~[#1])~[#1])]",
    					"SMARTS: [#6] [#7] [#6] onlybonded",
    					"SMARTS: [#6] [#8] options:F more",
    					"AtomIDS: 1",
    					"AtomIDS: 2 3 A",
    					"AtomIDS: 2 3 4 126.0 F",
    					"AtomIDS: 2 3 4 5 -0.123 options:A b c d"
    					))));
    	dOpts2.addSubDirective(dModRedundant);
    	
    	CompChemJob ccj2 = new CompChemJob();
    	ccj2.setDirective(dOpts2);
    	ccj2.setDirective(dTitle2);
    	ccj2.setDirective(dRoute2);
    	
    	//
    	// Start definition of second step
    	//
    	
    	Directive dRoute3 = new Directive(GaussianConstants.KEYROUTESEC);
    	Directive dTitle3 = new Directive(GaussianConstants.KEYTITLESEC);
    	// The following three are intentionally left empty
    	Directive dMolSpec3 = new Directive(GaussianConstants.KEYMOLSEC);
    	Directive dLink3 = new Directive(GaussianConstants.KEYLINKSEC);
    	Directive dOpts3 = new Directive(GaussianConstants.KEYOPTSSEC);
    	
    	dRoute3.setKeyword(new Keyword(GaussianConstants.SUBKEYMODELMETHOD, 
    			false, "HF"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.SUBKEYMODELBASISET,
    			false, "LANL2MB 5d 7f"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.SUBKEYJOBTYPE, 
    			false, "OPT"));
    	dRoute.setKeyword(new Keyword("Print", false, "P"));

    	Directive dOpt3 = new Directive("Opt");
    	dOpt3.addKeyword(new Keyword("forceConstants", false, "ReadFC"));
    	dOpt3.addKeyword(new Keyword("convergence", false, "loose"));
    	dOpt3.addKeyword(new Keyword("MaxCycles", true, "320"));
    	dOpt3.addKeyword(new Keyword("ModRedundant", false, "ModRedundant"));
    	dRoute3.addSubDirective(dOpt3);
    	
    	dTitle3.setKeyword(new Keyword("Title", false, "Third step"));
    	
    	CompChemJob ccj3 = new CompChemJob();
    	ccj3.setDirective(dLink3);
    	ccj3.setDirective(dMolSpec3);
    	ccj3.setDirective(dRoute3);
    	ccj3.setDirective(dOpts3);
    	ccj3.setDirective(dTitle3);
    	
    	//
    	// Assemble the multi-step master job
    	//
    	
    	CompChemJob masterJob = new CompChemJob();
    	masterJob.addStep(ccj);
    	masterJob.addStep(ccj2);
    	masterJob.addStep(ccj3);
    	
    	return masterJob;
    }

//------------------------------------------------------------------------------

}
