package autocompchem.chemsoftware.gaussian;

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
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.io.IOtools;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software Gaussian.
 *
 * @author Marco Foscato
 */

public class GaussianInputWriter2 extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTGAUSSIAN2)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on XTB conventions.
     */

    public GaussianInputWriter2() 
    {
		inpExtrension = GaussianConstants.GAUINPEXTENSION;
		outExtension = GaussianConstants.OUTEXTENSION;
	}
    
//------------------------------------------------------------------------------
    
    protected void printInputForOneMol(IAtomContainer mol, 
    		String outFileName, String outFileNameRoot)
    {		
		CompChemJob molSpecJob = ccJob.clone();

		//TODO-gg all of this is general enough to be in ChemSoftInputWriter
		// (once the setCharge/Spin are made abstract)
		
		Parameter pathnamePar = new Parameter(
				ChemSoftConstants.PAROUTFILEROOT,outFileNameRoot);
		molSpecJob.setParameter(pathnamePar);
		
		// Here we add atom coordinates to the so-far molecule-agnostic job
		setChemicalSystem(molSpecJob, mol);
		
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
			setChargeDirective(molSpecJob, pCharge.toString());
		}
		
		Object pSpin = mol.getProperty(ChemSoftConstants.PARSPINMULT);
		if (pSpin != null)
		{
			try {
				Integer.valueOf(pSpin.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pSpin.toString() + "' as spin multiplicity. Check "
						+ "value of property '" + ChemSoftConstants.PARSPINMULT
						+ "'.", -1);
			}
			setSpinMultiplicityDirective(molSpecJob, pSpin.toString());
		}
		
		// These calls take care also of the sub-jobs/directives
		molSpecJob.processDirectives(mol);
		
		// Ensure a value of charge and spin has been defined
		setChargeDirective(molSpecJob, "0");
		setSpinMultiplicityDirective(molSpecJob, "1");
		
		IOtools.writeTXTAppend(outFileName, getTextForInput(molSpecJob), true);
    }

//------------------------------------------------------------------------------
    
    /**
     * Sets the charge directive to any step where it is not already defined. 
     * This means that this method does not overwrite existing charge settings
     * @param ccj the job to customize.
     * @param charge the value of the charge to specify.
     */
    private void setChargeDirective(CompChemJob ccj, String charge)
    {
    	setDirectiveIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVEMOLSPEC, 
    			GaussianConstants.MSCHARGEKEY, charge);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the spin multiplicity directive to any step where it is not already 
     * defined. 
     * This means that this method does not overwrite existing settings.
     * @param ccj the job to customize.
     * @param sm the value of the spin multiplicity to specify.
     */
    private void setSpinMultiplicityDirective(CompChemJob ccj, String sm)
    {
    	setDirectiveIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVEMOLSPEC,
    			GaussianConstants.MSSPINMLTKEY, sm);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets a directive data with the representation of the chemical system to
     * work with. Note we here only consider the atoms and their coordinates,
     * not the charge of the spin multiplicity, for which there are dedicated 
     * methods.
     * @param ccj the job to customize.
     * @param iac the atom container to translate into a chemical system 
     * representation suitable for this software.
     */
    private void setChemicalSystem(CompChemJob ccj, IAtomContainer iac)
    {
    	ArrayList<String> list = new ArrayList<String>();
    	for (IAtom atm : iac.atoms())
    	{
    		Point3d p = atm.getPoint3d();
    		String el = AtomUtils.getSymbolOrLabel(atm);
    		list.add(el+"  "+p.x+" "+p.y+" "+p.z);
    	}

    	DirectiveData dd = new DirectiveData("coordinates", list);
    	setDirectiveDataIfNotAlreadyThere((CompChemJob) ccj.getStep(0), 
    			GaussianConstants.DIRECTIVEMOLSPEC, "coordinates", dd);
    }
    
//------------------------------------------------------------------------------
    
    private void setDirectiveDataIfNotAlreadyThere(CompChemJob ccj, String dirName,
    		String dirDataName, DirectiveData dd)
    {
    	//TODO-gg use directivecomponent path
    	if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
    			CompChemJob stepCcj = (CompChemJob) stepJob;
    			Directive dir = stepCcj.getDirective(dirName);
    			if (dir==null)
        		{
        			dir = new Directive(dirName);
            		dir.addDirectiveData(dd);
            		stepCcj.setDirective(dir);
        		} else {
        			DirectiveData oldDd = dir.getDirectiveData(dirDataName);
        			if (oldDd==null)
        			{
                		dir.addDirectiveData(dd);
                	} else {
                		oldDd.setValue(dd.getValue());
                	}
        		}
    		}
    	} else {
    		Directive dir = ccj.getDirective(dirName);
    		if (dir==null)
    		{
    			dir = new Directive(dirName);
        		dir.addDirectiveData(dd);
    			ccj.setDirective(dir);
    		} else {
    			DirectiveData oldDd = dir.getDirectiveData(dirDataName);
    			if (oldDd==null)
    			{
            		dir.addDirectiveData(dd);
            	} else {
            		oldDd.setValue(dd.getValue());
            	}
    		}
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets a keyword in a directive with the given name to any step where it is
     * not already defined. 
     * This means that this method does not overwrite existing charge settings
     * @param ccj the job to customize.
     * @param dirName the name of the directive
     * @param keyName the name of the keywords
     * @param value the value of the keyword to specify.
     */
    private void setDirectiveIfNotAlreadyThere(CompChemJob ccj, String dirName, 
    		String keyName, String value)
    {
    	if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
    			CompChemJob stepCcj = (CompChemJob) stepJob;
    			Directive dir = stepCcj.getDirective(dirName);
    			if (dir==null)
        		{
        			dir = new Directive(dirName);
            		dir.addKeyword(new Keyword(keyName, false, value));
            		stepCcj.setDirective(dir);
        		} else {
        			if (dir.getKeyword(keyName)==null)
        				dir.addKeyword(new Keyword(keyName, false, value));
        		}
    		}
    	} else {
    		Directive dir = ccj.getDirective(dirName);
    		if (dir==null)
    		{
    			dir = new Directive(dirName);
        		dir.addKeyword(new Keyword(keyName, false, value));
    			ccj.setDirective(dir);
    		} else {
    			if (dir.getKeyword(keyName)==null)
    				dir.addKeyword(new Keyword(keyName, false, value));
    		}
    	}
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
            lines.addAll(getTextForStep((CompChemJob)job.getStep(step)));
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
    
    private ArrayList<String> getTextForStep(CompChemJob step)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	Directive lnkDir = step.getDirective(GaussianConstants.DIRECTIVELINK0);
    	if (lnkDir != null)
    	{
    		// We expect only keywords
    		for (Keyword k : lnkDir.getAllKeywords())
    		{
    			if (k.isLoud())
    				lines.add("%" + k.getName() + "=" + k.getValueStr());
    			else
        			lines.add("%" + k.getValueStr());
    		}
    		if (lnkDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Link0 section of Gaussian "
    					+ "job contains data blocks but only keywords are "
    					+ "expected. Check your input!");
    		}
    		if (lnkDir.getAllSubDirectives().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Link0 section of Gaussian "
    					+ "job contains subdirectives but only keywords are "
    					+ "expected. Check your input!");
    		}
    	} // No default link0 section
    	
    	Directive rouDir = step.getDirective(GaussianConstants.DIRECTIVEROUTE);
    	if (rouDir != null)
    	{
    		// First the special keywords
    		String firstLine = "";
    		Keyword pKey = rouDir.getKeyword(GaussianConstants.KEYPRINT);
    		if (pKey!=null)
    		{
    			firstLine = "#"+pKey.getValueStr();
    		}  else {
    			firstLine = "#P";
            }
    		Keyword modKey = rouDir.getKeyword(GaussianConstants.KEYMODELMETHOD);
    		if (modKey!=null)
    		{
    			firstLine = firstLine + " " + modKey.getValueStr() + "/";
    		} else {
    			firstLine = firstLine + " ";
    		}
    		Keyword bsKey = rouDir.getKeyword(GaussianConstants.KEYMODELBASISET);
    		if (bsKey!=null)
    		{
    			// space has been already added, if needed
    			firstLine = firstLine + bsKey.getValueStr();
    		}
    		lines.add(firstLine);
    		Keyword jtKey = rouDir.getKeyword(GaussianConstants.KEYJOBTYPE);
    		if (jtKey!=null)
    		{
    			lines.add("# " + jtKey.getValueStr());
    		}
    		
    		// All other keywords
    		for (Keyword k : rouDir.getAllKeywords())
    		{
    			if (GaussianConstants.SPECIALKEYWORDS.contains(k.getName()))
    				continue;
    			if (k.isLoud())
    			{
    				lines.add("# " + k.getName() + "=" + k.getValueStr());
    			} else {
        			lines.add("# " + k.getValueStr());
    			}
    		}
    		String directiveLine = "# ";
    		for (Directive subDir : rouDir.getAllSubDirectives())
    		{
    			// Gaussian uses only one nesting level!
    			if (subDir.getAllSubDirectives().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Route "
        					+ "section of Gaussian job contains nested "
        					+ "sub-sub-directive/s. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			if (subDir.getAllDirectiveDataBlocks().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Route "
        					+ "section of Gaussian job contains directive "
        					+ "data blocks. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			directiveLine = directiveLine + subDir.getName() + "=(";
    			boolean first = true;
    			for (Keyword k : subDir.getAllKeywords())
    			{
    				String keyStr = "";
    				if (k.isLoud())
    				{
    					keyStr = k.getName() + "=" + k.getValueStr();
    				} else {
    					keyStr = k.getValueStr();
    				}
    				if (first)
    				{
    					directiveLine = directiveLine + keyStr;
    				} else {
    					directiveLine = directiveLine + "," + keyStr;
    				}
    				first = false;
    			}
    			directiveLine = directiveLine + ")";
    		}
    		if (rouDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Route section of Gaussian "
    					+ "job contains data blocks but only keywords are "
    					+ "expected. Check your input!");
    		}
    	} else {
			lines.add("#P");
    	}
		lines.add(""); // Empty line terminating route section
    	
    	Directive titDir = step.getDirective(GaussianConstants.DIRECTIVETITLE);
    	if (titDir != null)
    	{
    		// We expect only ONE keywords
    		if (titDir.getAllKeywords().size()>1)
    		{
    			throw new IllegalArgumentException(
    					"Directive for title section of Gaussian "
    					+ "job contains more than one keyword. "
    					+ "Check your input!");
    		}
    		Keyword k = titDir.getAllKeywords().get(0);
    		lines.add(k.getValueStr());
    		
    		if (titDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for title section of Gaussian "
    					+ "job contains data blocks but only keywords are "
    					+ "expected. Check your input!");
    		}
    		if (titDir.getAllSubDirectives().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for title section of Gaussian "
    					+ "job contains subdirectives but only keywords are "
    					+ "expected. Check your input!");
    		}
    	} else {
			lines.add("No title");
    	}
		lines.add(""); // Empty line terminating title section
		
		Directive molDir = step.getDirective(GaussianConstants.DIRECTIVEMOLSPEC);
    	if (molDir != null)
    	{
    		// We expect TWO keywords
    		if (molDir.getAllKeywords().size()!=2)
    		{
    			throw new IllegalArgumentException(
    					"Directive for molecular specification "
    					+ "section of Gaussian "
    					+ "job contains N!=2 keywords. "
    					+ "Check your input!");
    		}
    		Keyword kCharge = molDir.getKeyword(GaussianConstants.MSCHARGEKEY);
	    	Keyword kSpinMult = molDir.getKeyword(GaussianConstants.MSSPINMLTKEY);
    		lines.add(kCharge.getValueStr() + " " + kSpinMult.getValueStr());
    		
    		if (molDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			for (DirectiveData dd : molDir.getAllDirectiveDataBlocks())
    			{
    				lines.addAll(dd.getLines());
    			}
    		}
    		if (molDir.getAllSubDirectives().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for molecular specification "
    					+ "section of Gaussian "
    					+ "job contains subdirectives but only keywords "
    					+ "and ditrective data blocks are "
    					+ "expected. Check your input!");
    		}
    	} // Since we always define charge and spin this directive is never null
		lines.add(""); // Empty line terminating molecular specification section
    	
    	Directive optDir = step.getDirective(GaussianConstants.DIRECTIVEOPTS);
    	if (optDir != null)
    	{
    		// For the moment we expect only directive data in this section
    		Set<String> optNames = new HashSet<String>();
    		optDir.getAllDirectiveDataBlocks().stream().forEach(
    				dd -> optNames.add(dd.getName()));
    		List<String> sortedOptNames = sortOpts(optNames);
    		for (String ddName : sortedOptNames)
    		{
    			lines.addAll(optDir.getDirectiveData(ddName).getLines());
    		}
    		
    		// Dealing with keywords even if we now do not expect them to be
    		// present. They might result from the attempt to achieve special 
    		//results
    		for (Keyword k : optDir.getAllKeywords())
    		{
    			if (k.isLoud())
    				lines.add(k.getName() + "=" + k.getValueStr());
    			else
        			lines.add(k.getValueStr());
    		}
    		
    		// Dealing with subdirective even if we now do not expect them to be
    		// present. They might result from the attempt to achieve special 
    		//results
    		String directiveLine = "";
    		for (Directive subDir : rouDir.getAllSubDirectives())
    		{
    			// Gaussian uses only one nesting level!
    			if (subDir.getAllSubDirectives().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Option "
        					+ "section of Gaussian job contains nested "
        					+ "sub-sub-directive/s. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			if (subDir.getAllDirectiveDataBlocks().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Option "
        					+ "section of Gaussian job contains directive "
        					+ "data blocks. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			directiveLine = directiveLine + subDir.getName() + "=(";
    			boolean first = true;
    			for (Keyword k : subDir.getAllKeywords())
    			{
    				String keyStr = "";
    				if (k.isLoud())
    				{
    					keyStr = k.getName() + "=" + k.getValueStr();
    				} else {
    					keyStr = k.getValueStr();
    				}
    				if (first)
    				{
    					directiveLine = directiveLine + keyStr;
    				} else {
    					directiveLine = directiveLine + "," + keyStr;
    				}
    				first = false;
    			}
    			directiveLine = directiveLine + ")";
    		}
    	} // No default Option section
    	lines.add(""); // Empty line terminating option section
    	
    	return lines;
    }
    
//------------------------------------------------------------------------------

    /**
     * Order the keys of the option blocks according to the presumed 
     * expectations of Gaussian (i.e., basis set before PCM)
     * @param keySet the set of keys
     * @return the reordered list
     */

    private List<String> sortOpts(Set<String> keySet)
    {
        List<String> sortedKeys = new ArrayList<String>();
        if (keySet.contains(GaussianConstants.MODREDUNDANTKEY))
        {
            sortedKeys.add(GaussianConstants.MODREDUNDANTKEY);
        }
        if (keySet.contains(GaussianConstants.BASISOPTKEY))
        {
            sortedKeys.add(GaussianConstants.BASISOPTKEY);
        }
        if (keySet.contains(GaussianConstants.PCMOPTKEY))
        {
            sortedKeys.add(GaussianConstants.PCMOPTKEY);
        }
        for (String key : keySet)
        {
            if (key.equals(GaussianConstants.PCMOPTKEY) 
                || key.equals(GaussianConstants.BASISOPTKEY)
                || key.equals(GaussianConstants.MODREDUNDANTKEY))
            {
                continue;
            }
            sortedKeys.add(key);
        }
        return sortedKeys;
    }
//------------------------------------------------------------------------------

}
