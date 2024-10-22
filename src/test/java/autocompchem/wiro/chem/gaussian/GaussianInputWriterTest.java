package autocompchem.wiro.chem.gaussian;


import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileAnalyzer;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.modeling.constraints.ConstraintsGenerator;
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

public class GaussianInputWriterTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;
    
//-----------------------------------------------------------------------------

    @Test
    public void testPrepareInputTask() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory");
        
        File jdFile = new File(tempDir.getAbsolutePath() + SEP + "ccj" 
        		+ WIROConstants.JSONJDEXTENSION);
        File parFile = new File(tempDir.getAbsolutePath() + SEP + "acc.par");
    	File molFile = new File(tempDir.getAbsolutePath() + SEP + "mol.sdf");
    	final String INPNAMEROOT = "molinp";
    	String inpRoot = tempDir.getAbsolutePath() + SEP + INPNAMEROOT;

    	IAtomContainer mol = new AtomContainer();
    	mol.addAtom(new Atom("C", new Point3d(1.0,2.0,3.0)));
    	mol.addAtom(new Atom("O", new Point3d(2.5,2.0,3.0)));
    	mol.addBond(0, 1, IBond.Order.TRIPLE);
    	mol.addAtom(new Atom("He", new Point3d(10.0,2.0,3.0)));
    	mol.addAtom(new Atom("He", new Point3d(11.0,2.0,3.0)));
    	mol.addAtom(new Atom("He", new Point3d(12.0,2.0,3.0)));
    	mol.addAtom(new Atom("He", new Point3d(13.0,2.0,3.0)));
    	mol.addAtom(new Atom("He", new Point3d(14.0,2.0,3.0)));
    	mol.addAtom(new Atom("He", new Point3d(15.0,2.0,3.0)));
    	mol.addAtom(new Atom("He", new Point3d(16.0,2.0,3.0)));
    	IOtools.writeSDFAppend(molFile, mol, false);
    	
    	CompChemJob ccj = createTestJob();
    	IOtools.writeJobToJSON(ccj, jdFile);
    	
    	List<String> parLines = new ArrayList<String>();
    	parLines.add(WorkerConstants.PARTASK + ParameterConstants.SEPARATOR
    			+ GaussianInputWriter.PREPAREINPUTGAUSSIANTASK.casedID);
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
        
        File inpFile = new File(inpRoot + GaussianConstants.GAUINPEXTENSION);
        
        assertTrue(inpFile.exists(),"INP file exists");
        assertEquals(2, FileAnalyzer.count(inpFile.getAbsolutePath(),
        		GaussianConstants.STEPSEPARATOR));
        assertEquals(7, FileAnalyzer.count(inpFile.getAbsolutePath(),"LANLMB"));
        assertEquals(7, FileAnalyzer.count(inpFile.getAbsolutePath(),"He*"));
        assertEquals(2, FileAnalyzer.count(inpFile.getAbsolutePath(),"3 4 5"));
        assertEquals(1, FileAnalyzer.count(inpFile.getAbsolutePath(),
        		" archive npa"));
    }
    
//------------------------------------------------------------------------------
    
    public CompChemJob createTestJob()
    {
    	//
    	// Start definition of first step
    	//
    	
    	Directive dLink = new Directive(GaussianConstants.DIRECTIVELINK0);
    	Directive dRoute = new Directive(GaussianConstants.DIRECTIVEROUTE);
    	Directive dTitle = new Directive(GaussianConstants.DIRECTIVETITLE);
    	
    	dLink.setKeyword(new Keyword("chk", true, "randomname.chk"));
    	dLink.setKeyword(new Keyword("Nprocshared", true, "36"));
    	dLink.setKeyword(new Keyword("mem", true, "28GB"));
    	
    	// These are intentionally disordered to verify reordering
    	dRoute.setKeyword(new Keyword(GaussianConstants.KEYMODELMETHOD, 
    			false, "HF"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.KEYMODELBASISET,
    			false, "LANL2MB 5d 7f"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.KEYJOBTYPE, 
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
    	
    	Directive dRoute2 = new Directive(GaussianConstants.DIRECTIVEROUTE);
    	Directive dTitle2 = new Directive(GaussianConstants.DIRECTIVETITLE);
    	Directive dOpts2 = new Directive(GaussianConstants.DIRECTIVEOPTS);

    	dRoute2.setKeyword(new Keyword(GaussianConstants.KEYMODELMETHOD, 
    			false, "OLYP"));
    	dRoute2.setKeyword(new Keyword(GaussianConstants.KEYMODELBASISET,
    			false, "GEN 5d 7f"));
    	dRoute2.setKeyword(new Keyword(GaussianConstants.KEYJOBTYPE, 
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
    	DirectiveData ddNBO = new DirectiveData("NBO");
    	ddNBO.setValue("$NBO archive npa $END");
    	dOpts2.addDirectiveData(ddNBO);
    	
    	DirectiveData ddBasisSet = new DirectiveData(GaussianConstants.DDBASISSET);
    	ParameterStorage atp_basis = new ParameterStorage();
    	atp_basis.setParameter(ChemSoftConstants.JDACCTASK, 
    			BasisSetGenerator.GENERATEBASISSETTASKNAME);
    	atp_basis.setParameter(BasisSetConstants.ATMSPECBS, 
    					"SMARTS [#8] name STO-3G" + NL
    					+ "SMARTS [#6] name STO-32G" + NL
    					+ "SMARTS [!$([#6,#8])] name LANLMB");
    	ddBasisSet.setTaskParams(atp_basis);
    	dOpts2.addDirectiveData(ddBasisSet);
    	
    	DirectiveData ddModRedundant = new DirectiveData("ModRedundant");
    	ParameterStorage atp_modRed = new ParameterStorage();
    	atp_modRed.setParameter(ChemSoftConstants.JDACCTASK, 
    			 ConstraintsGenerator.GENERATECONSTRAINTSTASKNAME);
    	atp_modRed.setParameter(AtomTupleConstants.KEYRULETYPESMARTS,
    					"[$([#6](~[#1])(~[#1])~[#1])]" + NL
    					+ "[#6] [#7] [#6] onlybonded" + NL
    					+ "[#6] [#8] suffix:F more");
    	atp_modRed.setParameter(AtomTupleConstants.KEYRULETYPEATOMIDS, 
				"1" + NL
				+ "2 3 4 suffix:126.0 F" + NL
				+ "2 3 4 5 suffix:-0.123 A b c d");
    	ddModRedundant.setTaskParams(atp_modRed);
    	dOpts2.addDirectiveData(ddModRedundant);
    	
    	CompChemJob ccj2 = new CompChemJob();
    	ccj2.setDirective(dOpts2);
    	ccj2.setDirective(dTitle2);
    	ccj2.setDirective(dRoute2);
    	
    	//
    	// Start definition of second step
    	//
    	
    	Directive dRoute3 = new Directive(GaussianConstants.DIRECTIVEROUTE);
    	Directive dTitle3 = new Directive(GaussianConstants.DIRECTIVETITLE);
    	// The following three are intentionally left empty
    	Directive dMolSpec3 = new Directive(GaussianConstants.DIRECTIVEMOLSPEC);
    	Directive dLink3 = new Directive(GaussianConstants.DIRECTIVELINK0);
    	Directive dOpts3 = new Directive(GaussianConstants.DIRECTIVEOPTS);
    	
    	dRoute3.setKeyword(new Keyword(GaussianConstants.KEYMODELMETHOD, 
    			false, "HF"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.KEYMODELBASISET,
    			false, "LANL2MB 5d 7f"));
    	dRoute.setKeyword(new Keyword(GaussianConstants.KEYJOBTYPE, 
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
