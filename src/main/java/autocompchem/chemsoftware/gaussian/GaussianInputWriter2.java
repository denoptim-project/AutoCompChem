package autocompchem.chemsoftware.gaussian;

import java.io.File;

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
import java.util.Locale;
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
import autocompchem.modeling.basisset.BasisSet;
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
		
		// Here we add strings/pathnames that are molecular specific (e.g., the 
		// name of Gaussian checkpoint)
		setSystemSpecificNames(molSpecJob);
		
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
    	setKeywordIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVEMOLSPEC, 
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
    	setKeywordIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVEMOLSPEC,
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
    	if (!needsGeometry(ccj))
    		return;
    		
    	ArrayList<String> list = new ArrayList<String>();
    	for (IAtom atm : iac.atoms())
    	{
    		Point3d p = atm.getPoint3d();
    		String el = AtomUtils.getSymbolOrLabel(atm);
    		list.add(el + "  " 
    				+ String.format(Locale.ENGLISH,"%17.12f",p.x) + " "
    	    		+ String.format(Locale.ENGLISH,"%17.12f",p.y) + " "
    	    	    + String.format(Locale.ENGLISH,"%17.12f",p.z));
    	}

    	if (ccj.getNumberOfSteps()>0)
    	{
        	DirectiveData dd = new DirectiveData("coordinates", list);
        	setDirectiveDataIfNotAlreadyThere((CompChemJob) ccj.getStep(0), 
        			GaussianConstants.DIRECTIVEMOLSPEC, "coordinates", dd);
    	} else {

        	DirectiveData dd = new DirectiveData("coordinates", list);
        	setDirectiveDataIfNotAlreadyThere(ccj, 
        			GaussianConstants.DIRECTIVEMOLSPEC, "coordinates", dd);
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if a job needs to specify the geometry of the chemical system in
     * its input file of if it takes it from the checkpoint file.
     * @param ccj the job to analyze
     * @return <code>true</code> if the geometry should be in the input file.
     */
    public static boolean needsGeometry(CompChemJob ccj)
    {

    	if (ccj.getNumberOfSteps()>0)
    	{
			CompChemJob stepCcj = (CompChemJob) ccj.getStep(0);
			Directive routeDir = stepCcj.getDirective(
    				GaussianConstants.DIRECTIVEROUTE);
			return needsGeometry(routeDir);
    	} else {
    		return needsGeometry(ccj.getDirective(
    				GaussianConstants.DIRECTIVEROUTE));
    	}
    }
    
//------------------------------------------------------------------------------
    
    private static boolean needsGeometry(Directive routeDir)
    {
    	Keyword geomKey = routeDir.getKeyword(GaussianConstants.GAUKEYGEOM);
		if (geomKey!=null)
		{
			String value = geomKey.getValueStr().toUpperCase();
			if (value.startsWith(GaussianConstants.GAUKEYGEOMCHK) ||
					value.startsWith(GaussianConstants.GAUKEYGEOMCHECK) ||
					value.startsWith(GaussianConstants.GAUKEYGEOMALLCHK) ||
					value.startsWith(GaussianConstants.GAUKEYGEOMSTEP) ||
					value.startsWith(GaussianConstants.GAUKEYGEOMNGEOM) ||
					value.startsWith(GaussianConstants.GAUKEYGEOMMOD))
	        {
				// Geometry will be teken from checkpoint file.
				return false;
	        }
		}
		return true;
    }
    
//------------------------------------------------------------------------------
    
    private void setDirectiveDataIfNotAlreadyThere(CompChemJob ccj, String dirName,
    		String dirDataName, DirectiveData dd)
    {
    	//TODO-gg use directive component path
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
     * Sets the names of checkpoint files
     */
    private void setSystemSpecificNames(CompChemJob ccj)
    {
    	File pathnameRoot = new File(outFileNameRoot);
    	setKeywordIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVELINK0,
    			"chk", true, pathnameRoot.getName());
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
    private void setKeywordIfNotAlreadyThere(CompChemJob ccj, String dirName, 
    		String keyName, String value)
    {
    	setKeywordIfNotAlreadyThere(ccj, dirName, keyName, false, value);
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
    private void setKeywordIfNotAlreadyThere(CompChemJob ccj, String dirName, 
    		String keyName, boolean isLoud, String value)
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
            		dir.addKeyword(new Keyword(keyName, isLoud, value));
            		stepCcj.setDirective(dir);
        		} else {
        			if (dir.getKeyword(keyName)==null)
        				dir.addKeyword(new Keyword(keyName, isLoud, value));
        		}
    		}
    	} else {
    		Directive dir = ccj.getDirective(dirName);
    		if (dir==null)
    		{
    			dir = new Directive(dirName);
        		dir.addKeyword(new Keyword(keyName, isLoud, value));
    			ccj.setDirective(dir);
    		} else {
    			if (dir.getKeyword(keyName)==null)
    				dir.addKeyword(new Keyword(keyName, isLoud, value));
    		}
    	}
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForInput(CompChemJob job)
    {	
        ArrayList<String> lines= new ArrayList<String>();
        for (int step = 0; step<job.getNumberOfSteps(); step++)
        {
        	CompChemJob stepCcj = (CompChemJob)job.getStep(step);
        	//TODO log
        	System.out.println("Preparing input for step " + stepCcj.getId());
        	
            if (step != 0)
            {
                lines.add(GaussianConstants.STEPSEPARATOR);
            }
            lines.addAll(getTextForStep(stepCcj));
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
    	
    	//
    	// Building lines of Link0 section
    	// 
    	
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
    	
    	//
    	// Building lines of Route section
    	//
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
    		for (Directive subDir : rouDir.getAllSubDirectives())
    		{
        		String directiveLine = "# ";
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
        		lines.add(directiveLine);
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
    	
		//
		// Building line of Title section (well, one line plus blank line)
		//
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
		
		//
		// Building lines of Molecular Specification Section
		//
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
    	
		//
		// Building lines of Options section
		//
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
    			DirectiveData dd = optDir.getDirectiveData(ddName);
    			// Some of the directivedata blocks need to be interpreted to
    			// convert the agnostic data into Gaussian slang
    			switch (ddName.toUpperCase())
    			{
	    			case GaussianConstants.DDBASISSET:
	    			{
	    				BasisSet bs = (BasisSet) dd.getValue();
	    				lines.add(bs.toInputFileString("Gaussian"));
	    				//No additional newline: it comes already from bs
	    				break;
	    			}
	    			
	    			case GaussianConstants.DDMODREDUNDANT:
	    			{
	    				ConstraintsSet cs = (ConstraintsSet) dd.getValue();
	                    for (Constraint cns : cs)
	                    {
	                    	String str = "";
	                    	switch (cns.getType())
	                    	{
								case ANGLE:
									str = "A " + (cns.getAtomIDs()[0]+1) + " "
											+ (cns.getAtomIDs()[1]+1) + " "
											+ (cns.getAtomIDs()[2]+1);
									
									break;
								case DIHEDRAL:
									str = "D " + (cns.getAtomIDs()[0]+1) + " "
											+ (cns.getAtomIDs()[1]+1) + " "
											+ (cns.getAtomIDs()[2]+1) + " "
											+ (cns.getAtomIDs()[3]+1);
									break;
								case DISTANCE:
									str = "B " + (cns.getAtomIDs()[0]+1) + " "
											+ (cns.getAtomIDs()[1]+1);
									break;
								case FROZENATM:
									str = "X " + (cns.getAtomIDs()[0]+1);
									break;
								case UNDEFINED:
									break;
								default:
									break;
	                    	}
	                    	
	                    	if (cns.hasOpt())
	                    	{
	                    		str = str + " " + cns.getOpt();
	                    	}
	                    	lines.add(str);
	                    }
	        			lines.add(""); //empty line that terminates this part of option section
	    				break;
	    			}
	    		
	    			default:
	    			{
	    				lines.addAll(optDir.getDirectiveData(ddName).getLines());
	        			lines.add(""); //empty line that terminates this part of option section
	    			}
    			}
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
    			lines.add(""); //empty line that terminates this part of option section
    		}
    		
    		// Dealing with subdirective even if we now do not expect them to be
    		// present. They might result from the attempt to achieve special 
    		//results
    		for (Directive subDir : optDir.getAllSubDirectives())
    		{
    			String directiveLine = "";
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
    			lines.add(directiveLine);
    			lines.add(""); //empty line that terminates this part of option section
    		}
			lines.add(""); //empty line that terminates the option section
    	} // No default Option section
    	
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
        if (keySet.contains(GaussianConstants.DDMODREDUNDANT))
        {
            sortedKeys.add(GaussianConstants.DDMODREDUNDANT);
        }
        if (keySet.contains(GaussianConstants.DDBASISSET))
        {
            sortedKeys.add(GaussianConstants.DDBASISSET);
        }
        if (keySet.contains(GaussianConstants.DDPCM))
        {
            sortedKeys.add(GaussianConstants.DDPCM);
        }
        for (String key : keySet)
        {
            if (key.equals(GaussianConstants.DDPCM) 
                || key.equals(GaussianConstants.DDBASISSET)
                || key.equals(GaussianConstants.DDMODREDUNDANT))
            {
                continue;
            }
            sortedKeys.add(key);
        }
        return sortedKeys;
    }
//------------------------------------------------------------------------------

}
