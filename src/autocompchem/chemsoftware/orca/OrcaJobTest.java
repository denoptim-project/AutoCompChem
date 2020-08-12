package autocompchem.chemsoftware.orca;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;


/**
 * Unit Test for OrcaJob objects
 * 
 * @author Marco Foscato
 */

public class OrcaJobTest 
{
	private final String NL = System.getProperty("line.separator");
	
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
    			+ ChemSoftConstants.JDLABDATA + "geometry"
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
    			+ ChemSoftConstants.JDLABDATA + "geometry"
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
    			+ ChemSoftConstants.JDLABDATA + "geometry"
    			+ ChemSoftConstants.JDDATAVALSEPARATOR
    			+ ChemSoftConstants.JDOPENBLOCK
    			+ "  C   0.000 0.000 0.000" + NL
    			+ "  O   0.000 0.000 1.130" 
    			+ ChemSoftConstants.JDCLOSEBLOCK);
    	
    	CompChemJob oj = new CompChemJob(lines);
    	
    	assertEquals(3,oj.getNumberOfSteps(),"Number of Orca steps");
    	assertEquals("NumFreq",((CompChemJob)oj.getStep(0)).getDirective("!")
    			.getKeyword("jobType").getValue().get(0),
    			"Check imported dir (A)");
    	assertEquals("0",((CompChemJob)oj.getStep(1)).getDirective("*")
    			.getKeyword("charge").getValue().get(0),
    			"Check imported dir (B)");
    	assertEquals("Opt",((CompChemJob)oj.getStep(2)).getDirective("!")
    	    			.getKeyword("jobType").getValue().get(0),
    	    			"Check imported dir (C)");
    }
    
//------------------------------------------------------------------------------

}
