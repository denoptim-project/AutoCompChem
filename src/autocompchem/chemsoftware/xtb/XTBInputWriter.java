package autocompchem.chemsoftware.xtb;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software XTB.
 *
 * @author Marco Foscato
 */

//TODO: write doc

public class XTBInputWriter extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTXTB)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on XTB conventions.
     */

    public XTBInputWriter() 
    {
		inpExtrension = XTBConstants.INPEXTENSION;
		outExtension = XTBConstants.INPEXTENSION;
	}
    
//------------------------------------------------------------------------------
    
    protected void printInputForOneMol(IAtomContainer mol, 
    		String outFileName, String outFileNameRoot)
    {		
		CompChemJob molSpecJob = ccJob.clone();

		Parameter pathnamePar = new Parameter(
				ChemSoftConstants.PAROUTFILEROOT,outFileNameRoot);
		molSpecJob.setParameter(pathnamePar);
		
    	// WARNING: at this time XTB is not capable of running a multi-step jobs
/*		
		for (Job subJob : molSpecJob.getSteps())
		{
			subJob.setParameter(pathnamePar);
		}
*/
		
		Object pCharge = mol.getProperty(ChemSoftConstants.PARCHARGE);
		if (pCharge != null)
		{
			int charge;
			try {
				charge = Integer.valueOf(pCharge.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pCharge.toString() + "' as charge. Check "
						+ "value of property '" + ChemSoftConstants.PARCHARGE
						+ "'.", -1);
			}
			Directive d = new Directive("charge");
			d.addKeyword(new Keyword("value", false, pCharge.toString()));
			molSpecJob.setDirective(d);
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
			molSpecJob.setDirective(d);
		}
		
		// These calls take care also of the sub-jobs/directives
		molSpecJob.processDirectives(mol);
		//molSpecJob.sortDirectivesBy(new XTBDirectiveComparator());
		
    	// WARNING: at this time XTB is not capable of running a multi-step jobs
		ArrayList<String> lines = new ArrayList<String>();
		if (molSpecJob.getNumberOfSteps()>0)
		{
			// If one day XTB becomes able to run multiple steps this is where 
			// we'll collect the input lines for each step and add the step 
			// separator.
			// For now we report the inconsistency.
			Terminator.withMsgAndStatus("ERROR! XTB does not run multi-step "
					+ "jobs, but your input contains more than one step.", -1);
		} else {
			lines.addAll(getTextForInput(molSpecJob));
		}
		IOtools.writeTXTAppend(outFileName, lines, true);
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForInput(CompChemJob job)
    {
    	ArrayList<String> lines = new ArrayList<String>();
		Iterator<Directive> it = job.directiveIterator();
		while (it.hasNext())
		{
			Directive d = it.next();
			lines.addAll(getTextForInput(d,true));
		}
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the method the encodes the syntax of the XTB input file for a 
     * single job directive. Here we translate all comp.chem.-software agnostic 
     * components to XTB-specific format.
     * @param d the syntax-agnostic container of instructions
     * @param outmost set <code>true</code> if the directive is the outermost 
     * and, thus, must be decorated with the '$' character.
     * @return the list of lines for the input file
     */
    
    private ArrayList<String> getTextForInput(Directive d, boolean outmost)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	String dirName = d.getName();
    	if (dirName.startsWith("#"))
    	{
			// This is a comment line for the XTB input.
			// There can be one or more such lines, each strictly filling
			// ONE single line.
			for (Keyword k : d.getAllKeywords())
			{
				if (k.isLoud())
				{
					lines.add("#" + k.getName() + " "
						+ StringUtils.mergeListToString(k.getValue()," "));
				} else
				{
					lines.add("#" + StringUtils.mergeListToString(
							k.getValue()," "));	
				}
			}
			// Sub directives and DirectiveData are not suitable for XTB's
			// "keyword line", so we do not expect them
			String errTail = "found within a comment line. This "
					+ "is unexpected, but if you see an use for it, "
					+ "then, please, implement the conversion to "
					+ "input file text lines.";
			if (d.getAllSubDirectives().size()>0)
			{
				Terminator.withMsgAndStatus("ERROR! Unexpected sub "
						+ "directives "+errTail, -1);
			}
			if (d.getAllDirectiveDataBlocks().size()>0)
			{
				Terminator.withMsgAndStatus("ERROR! Unexpected directive "
						+ "data blocks "+errTail, -1);
			}
			return lines;
    	}
    	
    	// Purge dirName from useless parts
    	if (dirName.startsWith("$"))
    	{
    		//OK, I see what you want to do, but for further processing 
    		// we get rid of the '$'. We put it back anyway for all standard
    		// directives that are at the outermost level.
    		dirName = dirName.substring(1).trim();
    	} 
    	
    	// Here we translate all chem.software agnostic components 
    	// to XTB-specific formated text.
		switch (dirName.toUpperCase())
		{
		
			// WATNING:
			// The syntax is inconsistent in current XTB implementation: charge 
	    	// and spin multiplicity (well, actually the N_alphs-N_beta 
			// electrons) are contradicting the declaration that after '$' 
			// "the instruction name is the rest of the register". In fact, 
			// these instructions are expecting a value after the instruction
			// name.
            case ("CHRG"):
			case ("CHARGE"):
			{
				if (d.getAllKeywords().size() == 1)
				{
					lines.add("$chrg " + d.getAllKeywords().get(0).getValue()
							.get(0));
				} else {
					Terminator.withMsgAndStatus("ERROR! Expecting one keyword "
							+ "in $chrg/$charge directive.",-1);
				}
				break;
			}
			
			case ("SPIN"):
			{
				if (d.getAllKeywords().size() == 1)
				{
					lines.add("$spin " + d.getAllKeywords().get(0).getValue()
							.get(0));
				} else {
					Terminator.withMsgAndStatus("ERROR! Expecting one keyword "
							+ "in $spin directive.",-1);
				}
				break;
			}
			
			case ("CONSTRAIN"):
			{
				String line = "";
				if (outmost)
				{
					line = "$";
				}
				line = line + dirName;
				lines.add(line);
				
				// WARNING! The separator between the keyword name and its value
				// changes with the keyword... pure madness!
				for (Keyword k : d.getAllKeywords())
				{
					String l = XTBConstants.INDENT ;
					if (k.isLoud())
					{
						l = l + getKeyAndSeparator(dirName,k.getName());
					}
					String v = StringUtils.mergeListToString(k.getValue(),",",true);
					while (v.contains(ParameterConstants.STARTMULTILINE))
					{
						v = v.replace(ParameterConstants.STARTMULTILINE, "");
						v = v.replace(ParameterConstants.ENDMULTILINE, "");
					}
					lines.add(l + v );
				}
				
				for (Directive sd1 : d.getAllSubDirectives())
				{
					for (String innerLine : getTextForInput(sd1,false))
					{
						lines.add(XTBConstants.INDENT + innerLine);
					}
				}
				
				// The only data block should be the constraints generated by ACC
				/*
				for (DirectiveData dd : d.getAllDirectiveDataBlocks())
				{
					for (String innerLine : dd.getLines())
					{
						lines.add(XTBConstants.INDENT + innerLine);
					}
				}
				*/
				
				lines.addAll(getTextForConstraintsBlock(d));
				
				lines.add("$end");
				break;
			}
			
			case ("FIX"):
			{
				String line = "";
				if (outmost)
				{
					line = "$";
				}
				line = line + dirName;
				lines.add(line);
				
				// WARNING! The separator between the keyword name and its value
				// changes with the keyword... pure madness!
				for (Keyword k : d.getAllKeywords())
				{
					String l = XTBConstants.INDENT ;
					if (k.isLoud())
					{
						l = l + getKeyAndSeparator(dirName,k.getName());
					}
					String v = StringUtils.mergeListToString(k.getValue(),",",true);
					while (v.contains(ParameterConstants.STARTMULTILINE))
					{
						v = v.replace(ParameterConstants.STARTMULTILINE, "");
						v = v.replace(ParameterConstants.ENDMULTILINE, "");
					}
					lines.add(l + v );
				}
				
				for (Directive sd1 : d.getAllSubDirectives())
				{
					for (String innerLine : getTextForInput(sd1,false))
					{
						lines.add(XTBConstants.INDENT + innerLine);
					}
				}
				
				// The only data block should be the constraints generated by ACC
				/*
				for (DirectiveData dd : d.getAllDirectiveDataBlocks())
				{
					for (String innerLine : dd.getLines())
					{
						lines.add(XTBConstants.INDENT + innerLine);
					}
				}
				*/
				
				lines.addAll(getTextForFixBlock(d));
				lines.add("$end");
				break;
			}
			
			default:
			{
				boolean needEnd = false;
				String line = "";
				if (outmost)
				{
					line = "$";
				}
				line = line + dirName;
				lines.add(line);
				
				// WARNING! The separator between the keyword name and its value
				// changes with the keyword... pure madness!
				for (Keyword k : d.getAllKeywords())
				{
					String l = XTBConstants.INDENT ;
					if (k.isLoud())
					{
						l = l + getKeyAndSeparator(dirName,k.getName());
					}
					String v = StringUtils.mergeListToString(k.getValue(),",",true);
					while (v.contains(ParameterConstants.STARTMULTILINE))
					{
						v = v.replace(ParameterConstants.STARTMULTILINE, "");
						v = v.replace(ParameterConstants.ENDMULTILINE, "");
					}
					lines.add(l + v );
				}
				
				for (Directive sd1 : d.getAllSubDirectives())
				{
					for (String innerLine : getTextForInput(sd1,false))
					{
						lines.add(XTBConstants.INDENT + innerLine);
						needEnd = true;
					}
				}
				
				for (DirectiveData dd : d.getAllDirectiveDataBlocks())
				{
					for (String innerLine : dd.getLines())
					{
						lines.add(XTBConstants.INDENT + innerLine);
						needEnd = true;
					}
				}
				if (needEnd)
				{
					lines.add("$end");
				}
			}
		}
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This method solves the problem of using a different key-value separator 
     * for different keywords. XTB, in fact, has a tendency to use ':' for keys 
     * that can have a list of values, and used '=' for keys that can have only 
     * one value. Still, lists of one members will still require the ':' 
     * separator, so the type of separator cannot be inferred from the value 
     * type.
     * Moreover, the `set` directive works without separator. We handle this 
     * madness here.
     */
    private String getKeyAndSeparator(String dirName, String key)
    {
    	String s = key;
    	if (dirName.toUpperCase().equals("SET"))
    	{
    		s = s + " "; // No separator for `set`
    	} else {
	    	Set<String> l = XTBConstants.COLONSEPARATEDKEYS.get(
	    			dirName.toUpperCase());
	    	if (l !=null && l.contains(key.toUpperCase()))
	    	{
	    		s = s + ": ";
	    	} else {
	    		s = s + "=";
	    	}
    	}
    	return s;
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForFixBlock(Directive d)
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	if (d.getAllDirectiveDataBlocks().size() == 0)
    		return lines;

    	//WARNING! We assume there is only one data block, and that it is
    	// indeed a set of constraints.
    	ConstraintsSet cs = (ConstraintsSet) d.getAllDirectiveDataBlocks()
    			.get(0).getValue();
    	
    	ArrayList<String> unqIDsStr = new ArrayList<String>();
    	for (Constraint c : cs)
    	{
    		for (int i=0; i<c.getAtomIDs().length; i++)
            {
    			String idStr = c.getAtomIDs()[i]+"";
    			if (!unqIDsStr.contains(idStr) && !idStr.equals("-1"))
    			{
    				unqIDsStr.add(idStr);
    			}
            }
    	}
    	Collections.sort(unqIDsStr, new NumberAwareStringComparator());
    	String line = XTBConstants.INDENT + "atoms: " 
    			+ StringUtils.mergeListToString(unqIDsStr, ",", true);
    	lines.add(line);
    	return lines;
    }

//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForConstraintsBlock(Directive d)
    {
    	ArrayList<String> lines = new ArrayList<String>();

    	if (d.getAllDirectiveDataBlocks().size() == 0)
    		return lines;
    	
    	//WARNING! We assume there is only one data block, and that it is
    	// indeed a set of constraints.
    	ConstraintsSet cs = (ConstraintsSet) d.getAllDirectiveDataBlocks()
    			.get(0).getValue();
    	
    	
    	//WARNING: frozen atoms are defined in directive "FIX" for XTB
    	if (!cs.getConstrainsWithType(ConstraintType.FROZENATM).isEmpty())
    	{
    		//WARNING: frozen atoms are defined in directive "FIX" for XTB
    		// We should never come in here.
    	}
    	
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DISTANCE))
        {
        	String frozenBondsStr = XTBConstants.INDENT + "distance: ";
        	if (c.hasValue())
        	{
        		frozenBondsStr = frozenBondsStr + c.getAtomIDs()[0] + ", "
                		+ c.getAtomIDs()[1] + ", " + c.getValue();
        	} else {
                frozenBondsStr = frozenBondsStr + c.getAtomIDs()[0] + ", "
                		+ c.getAtomIDs()[1] 
                		+ ", auto";
        	}
        	lines.add(frozenBondsStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.ANGLE))
        {
            String frozenAngleStr = XTBConstants.INDENT + "angle: ";
            if (c.hasValue())
            {
                frozenAngleStr = frozenAngleStr + c.getAtomIDs()[0] + ", "
                        + c.getAtomIDs()[1] + ", " + c.getAtomIDs()[2] + ", " 
                		+ c.getValue();
            } else {
                frozenAngleStr = frozenAngleStr + c.getAtomIDs()[0] + ", "
                        + c.getAtomIDs()[1] + ", " + c.getAtomIDs()[2] 
                        + ", auto";
            }
            lines.add(frozenAngleStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DIHEDRAL))
        {
            String frozenTorsStr = XTBConstants.INDENT + "dihedral: ";
            if (c.hasValue())
            {
                frozenTorsStr = frozenTorsStr + c.getAtomIDs()[0] + ", "
                        + c.getAtomIDs()[1] + ", " + c.getAtomIDs()[2] + ", "
                        + c.getAtomIDs()[3] + ", " + c.getValue();
            } else {
                frozenTorsStr = frozenTorsStr + c.getAtomIDs()[0] + ", "
                        + c.getAtomIDs()[1] + ", " + c.getAtomIDs()[2] + ", "
                        + c.getAtomIDs()[3]
                        + ", auto";
            }
            lines.add(frozenTorsStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.UNDEFINED))
        {
            String frozenGroups = XTBConstants.INDENT + "atoms: ";
            for (int i=0; i<c.getAtomIDs().length; i++)
            {
            	frozenGroups = frozenGroups + c.getAtomIDs()[i];
            	if (i<(c.getAtomIDs().length-1))
            	{
            		frozenGroups = frozenGroups + ", ";
            	}
            }
            lines.add(frozenGroups);
        }
    	return lines;
    }
    
//------------------------------------------------------------------------------

}
