package autocompchem.chemsoftware;

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

import java.util.ArrayList;

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
    			.getKeyword("jobType").getValueAsString(),
    			"Check imported dir (A)");
    	assertEquals("0",((CompChemJob)job.getStep(1)).getDirective("*")
    			.getKeyword("charge").getValueAsString(),
    			"Check imported dir (B)");
    	assertEquals("Opt",((CompChemJob)job.getStep(2)).getDirective("!")
    	    			.getKeyword("jobType").getValueAsString(),
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

}
