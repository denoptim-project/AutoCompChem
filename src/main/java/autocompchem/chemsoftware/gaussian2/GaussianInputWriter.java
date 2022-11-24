package autocompchem.chemsoftware.gaussian2;

/*
 *   Copyright (C) 2021  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.chemsoftware.gaussian.GaussianConstants;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.io.IOtools;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software Gaussian.
 *
 * @author Marco Foscato
 */

public class GaussianInputWriter extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTGAUSSIAN)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on XTB conventions.
     */

    public GaussianInputWriter() 
    {
		inpExtrension = GaussianConstants.GAUINPEXTENSION;
		outExtension = GaussianConstants.OUTEXTENSION;
	}
    
//------------------------------------------------------------------------------
    
    protected void printInputForOneMol(IAtomContainer mol, 
    		String outFileName, String outFileNameRoot)
    {		
		CompChemJob molSpecJob = ccJob.clone();

		Parameter pathnamePar = new Parameter(
				ChemSoftConstants.PAROUTFILEROOT,outFileNameRoot);
		molSpecJob.setParameter(pathnamePar);
		
		Object pCharge = mol.getProperty(ChemSoftConstants.PARCHARGE);
		if (pCharge != null)
		{
			try {
				Integer.valueOf(pCharge.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pCharge.toString() + "' as charge. Check "
						+ "value of property '" + ChemSoftConstants.PARCHARGE
						+ "'.", -1);
			}
			Directive d = new Directive("charge");
			d.addKeyword(new Keyword("value", false, pCharge.toString()));
			molSpecJob.setDirective(d, true);
		}
		
		Object pSpin = mol.getProperty(ChemSoftConstants.PARSPINMULT);
		if (pSpin != null)
		{
			int spinMult = 0;
			try {
				spinMult = Integer.valueOf(pSpin.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pSpin.toString() + "' as spin multiplicity. Check "
						+ "value of property '" + ChemSoftConstants.PARSPINMULT
						+ "'.", -1);
			}
			int numUnpairedEls = spinMult - 1;
			Directive d = new Directive("spin");
			d.addKeyword(new Keyword("value", false, numUnpairedEls + ""));
			molSpecJob.setDirective(d, true);
		}
		
		// These calls take care also of the sub-jobs/directives
		molSpecJob.processDirectives(mol);
		
		IOtools.writeTXTAppend(outFileName, getTextForInput(molSpecJob), true);
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForInput(CompChemJob job)
    {	
        ArrayList<String> lines= new ArrayList<String>();
        for (int step = 0; step<job.getNumberOfSteps(); step++)
        {
            if (step != 0)
            {
                lines.add(GaussianConstants.STEPSEPARATOR);
            }
            lines.addAll(getTextForStep(job.getStep(step)));
        }
        return lines;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the method the encodes the syntax of the Gaussian input file for 
     * a single step job (i.e., the given {@link Job} is not expected to contain
     * embedded {@link Job}s). 
     * Here we translate all comp.chem.-software agnostic components to 
     * Gaussian-specific format.
     * @param step the Gaussian job step object.
     * @return the list of lines for the input file
     */
    
    private ArrayList<String> getTextForStep(Job step)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	//TODO-gg
    	return lines;
    }
    
//------------------------------------------------------------------------------

}
