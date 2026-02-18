package autocompchem.wiro.chem.xtb;

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
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.connectivity.NearestNeighborMap;
import autocompchem.run.Job;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.Keyword;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Writes input files for software XTB.
 *
 * @author Marco Foscato
 */

public class XTBInputWriter extends ChemSoftInputWriter
{
    /**
     * String defining the task of preparing input files for XTB
     */
    public static final String PREPAREINPUTXTBTASKNAME = "prepareInputXTB";

    /**
     * Task about preparing input files for XTB
     */
    public static final Task PREPAREINPUTXTBTASK;
    static {
    	PREPAREINPUTXTBTASK = Task.make(PREPAREINPUTXTBTASKNAME);
    }
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on XTB conventions.
     */

    public XTBInputWriter() 
    {
		inpExtrension = XTBConstants.INPEXTENSION;
	}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTXTBTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new XTBInputWriter();
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
				lines.add("#" + k.toString(" "));
			}
			// Sub directives and DirectiveData are not suitable for XTB's
			// "keyword line", so we do not expect them
			String errTail = "found within a comment line. This "
					+ "is unexpected, but if you see an use for it, "
					+ "then, please, implement the conversion to "
					+ "input file text lines.";
			if (d.getAllSubDirectives().size()>0)
			{
				throw new IllegalStateException("Unexpected sub "
						+ "directives "+errTail);
			}
			if (d.getAllDirectiveDataBlocks().size()>0)
			{
				throw new IllegalStateException("Unexpected directive "
						+ "data blocks "+errTail);
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
					lines.add("$chrg " 
							+ d.getAllKeywords().get(0).getValueAsString());
				} else {
					throw new IllegalArgumentException("Expecting one keyword "
							+ "in $chrg/$charge directive.");
				}
				break;
			}
			
			case ("SPIN"):
			{
				if (d.getAllKeywords().size() == 1)
				{
					lines.add("$spin " 
							+ d.getAllKeywords().get(0).getValueAsString());
				} else {
					throw new IllegalArgumentException("Expecting one keyword "
							+ "in $spin directive.");
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
					String v = StringUtils.mergeListToString(k.getValueAsLines(),
							",",true);
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
				
				for (DirectiveData dd : d.getAllDirectiveDataBlocks())
				{
					if (dd.getValue() instanceof ConstraintsSet)
					{
						ConstraintsSet cs = (ConstraintsSet) dd.getValue();
						lines.addAll(getTextForConstraintsBlock(cs));
					} else {
						for (String innerLine : dd.getLines())
						{
							lines.add(XTBConstants.INDENT + innerLine);
						}
					}
				}
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
					String v = StringUtils.mergeListToString(k.getValueAsLines(),
							",",true);
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
				
				// The only data block should be the fixes (i.e., constraints)
				// generated by ACC
				lines.addAll(getTextForFixBlock(d));
				lines.add("$end");
				break;
			}
			
			case ("FFNB"):
			{
				String line = "";
				if (outmost)
				{
					line = "$";
				}
				line = line + dirName;
				lines.add(line);
				
				// No keywords needed by 2025 version of xtb, but perhaps future
				// version will require handling of keywords here
				for (Keyword k : d.getAllKeywords())
				{
					String l = XTBConstants.INDENT ;
					if (k.isLoud())
					{
						l = l + getKeyAndSeparator(dirName, k.getName());
					}
					String v = StringUtils.mergeListToString(k.getValueAsLines(),
							",",true);
					while (v.contains(ParameterConstants.STARTMULTILINE))
					{
						v = v.replace(ParameterConstants.STARTMULTILINE, "");
						v = v.replace(ParameterConstants.ENDMULTILINE, "");
					}
					lines.add(l + v );
				}

				// No keywords needed by 2025 version of xtb, but perhaps future
				// version will require handling of keywords here
				for (Directive sd1 : d.getAllSubDirectives())
				{
					for (String innerLine : getTextForInput(sd1,false))
					{
						lines.add(XTBConstants.INDENT + innerLine);
					}
				}
				
				// The only data block should be the map of nearest neighbors
				// generated by ACC
				lines.addAll(getTextForFFNBBlock(d));
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
					String v = StringUtils.mergeListToString(k.getValueAsLines(),
							",",true);
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
    /*
     * Example of intended result:
     * $ffnb
     *   nb = 1:5, 24, 27, 28, 41
     *   nb = 2:3,  9, 10
     *   nb = 8:7, 20, 21
     * $end
     */
    
    private List<String> getTextForFFNBBlock(Directive d)
    {
    	List<String> lines = new ArrayList<String>();
    	
    	if (d.getAllDirectiveDataBlocks().size() == 0)
    		return lines;

    	//WARNING! We assume there is only one data block, and that it is
    	// the nearest neighbor map
    	NearestNeighborMap nnm = (NearestNeighborMap) d.getAllDirectiveDataBlocks()
    			.get(0).getValue();
    	
    	for (Integer srcIdx : nnm.getSortedKeys())
    	{
    		//NB: 1-based indexes in xTB input files
    		StringBuilder sb = new StringBuilder();
    		sb.append(XTBConstants.INDENT).append("nb = ")
    			.append(srcIdx+1)
    			.append(":")
    			.append(StringUtils.mergeListToString(
    					nnm.getNbrsId(srcIdx, false), ", ", true));
    		lines.add(sb.toString());
    	}
    	return lines;
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
    		for (Integer idx : c.getAtomIDs())
            {
    			String idStr = (idx+1)+"";
    			if (!unqIDsStr.contains(idStr) && !(idx == -1))
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
    
    private List<String> getTextForConstraintsBlock(ConstraintsSet cs)
    {
    	List<String> lines = new ArrayList<String>();
    	
    	//NB: xtb used 1-based indexes for atoms
    	
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DISTANCE))
        {
        	String frozenBondsStr = XTBConstants.INDENT + "distance: ";
        	if (c.hasValue())
        	{
        		frozenBondsStr = frozenBondsStr + (c.getAtomIDs().get(0)+1) + ", "
                		+ (c.getAtomIDs().get(1)+1) + ", " + c.getValue();
        	} else if (c.hasCurrentValue())
        	{
        		frozenBondsStr = frozenBondsStr + (c.getAtomIDs().get(0)+1) + ", "
                		+ (c.getAtomIDs().get(1)+1) + ", " + c.getCurrentValue();
        	} else {
                frozenBondsStr = frozenBondsStr + (c.getAtomIDs().get(0)+1) + ", "
                		+ (c.getAtomIDs().get(1)+1) 
                		+ ", auto";
        	}
        	lines.add(frozenBondsStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.ANGLE))
        {
            String frozenAngleStr = XTBConstants.INDENT + "angle: ";
            if (c.hasValue())
            {
                frozenAngleStr = frozenAngleStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", " 
                		+ c.getValue();
            } else if (c.hasCurrentValue())
            {
                frozenAngleStr = frozenAngleStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", " 
                		+ c.getCurrentValue();
            } else {
                frozenAngleStr = frozenAngleStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " + (c.getAtomIDs().get(2)+1) 
                        + ", auto";
            }
            lines.add(frozenAngleStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DIHEDRAL))
        {
            String frozenTorsStr = XTBConstants.INDENT + "dihedral: ";
            if (c.hasValue())
            {
                frozenTorsStr = frozenTorsStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", "
                        + (c.getAtomIDs().get(3)+1) + ", " + c.getValue();
            } else if (c.hasCurrentValue())
            {
                frozenTorsStr = frozenTorsStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", "
                        + (c.getAtomIDs().get(3)+1) + ", " + c.getCurrentValue();
            } else {
                frozenTorsStr = frozenTorsStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", "
                        + (c.getAtomIDs().get(3)+1)
                        + ", auto";
            }
            lines.add(frozenTorsStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(
        		ConstraintType.IMPROPERTORSION))
        {
            String frozenTorsStr = XTBConstants.INDENT + "dihedral: ";
            if (c.hasValue())
            {
                frozenTorsStr = frozenTorsStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", "
                        + (c.getAtomIDs().get(3)+1) + ", " + c.getValue();
            } else if (c.hasCurrentValue())
            {
                frozenTorsStr = frozenTorsStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", "
                        + (c.getAtomIDs().get(3)+1) + ", " + c.getCurrentValue();
            } else {
                frozenTorsStr = frozenTorsStr + (c.getAtomIDs().get(0)+1) + ", "
                        + (c.getAtomIDs().get(1)+1) + ", " 
                		+ (c.getAtomIDs().get(2)+1) + ", "
                        + (c.getAtomIDs().get(3)+1)
                        + ", auto";
            }
            lines.add(frozenTorsStr);
        }
        
    	//WARNING: frozen atoms are defined in directive "FIX" for XTB, but
    	// there is a way to define "atoms" in the list of constraints
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.FROZENATM))
        {
            String frozenGroups = XTBConstants.INDENT + "atoms: ";
            boolean first = true;
            for (Integer idx : c.getAtomIDs())
            {
            	if (idx<0)
            		continue;
            	if (first)
            	{
            		first = false;
            		frozenGroups = frozenGroups + (idx+1);
            	} else {
            		frozenGroups = frozenGroups + ", " + (idx+1);
            	}
            }
            lines.add(frozenGroups);
        }
        
    	//WARNING: undefined constraints are translated into "atom" constraints
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.UNDEFINED))
        {
            String frozenGroups = XTBConstants.INDENT + "atoms: ";
            boolean first = true;
            for (Integer idx : c.getAtomIDs())
            {
            	if (idx<0)
            		continue;
            	if (first)
            	{
            		first = false;
            		frozenGroups = frozenGroups + (idx+1);
            	} else {
            		frozenGroups = frozenGroups + ", " + (idx+1);
            	}
            }
            lines.add(frozenGroups);
        }
    	return lines;
    }

//-----------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * 
     * This method is not doing anything in XTB job's main input file. No usage
     * case requiring such functionality.
     */
	@Override
	protected void setSystemSpecificNames(CompChemJob ccj) 
	{}

//-----------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * In XTB's main input file the charge is defined in a
	 *  {@link Keyword} of the
	 * 'charge' {@link Directive}.
	 */
	@Override
	protected void setChargeIfUnset(CompChemJob ccj, String charge,
			boolean omitIfPossible) 
	{
		if (omitIfPossible)
			return;
		
		addNewKeyword(ccj, "charge", "value", false, charge);
	}

//-----------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * In XTB's main input file the spin multiplicity is defined in a 
	 * {@link Keyword} of the
	 * 'spin' {@link Directive} as number of unpaired electrons.
	 */
	@Override
	protected void setSpinMultiplicityIfUnset(CompChemJob ccj, String sm,
			boolean omitIfPossible) 
	{
		if (omitIfPossible)
			return;
		
		int numUnpairedEls = Integer.parseInt(sm) - 1;
		addNewKeyword(ccj, "spin", "value", false, numUnpairedEls + "");
	}

//-----------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * XTB allows for reading the chemical system in an external file. This
	 * method is, therefore, not writing any molecular specification to the
	 * job's main input file.
	 */
	@Override
	protected void setChemicalSystem(CompChemJob ccj, List<IAtomContainer> iacs) 
	{
    	//TODO: check for consistency between the two methods for setting the geometry
    	// - one is this method
    	// - the other is using the "ADD_GEOMETRY" task in a directive
    	
	}

//-----------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * At this time, XTB does not run multi-step jobs. Therefore, an exception is
	 * triggered if you try to feed a multi-step job as argument to this method.
	 */
	@Override
	public StringBuilder getTextForInput(CompChemJob job) 
	{
    	StringBuilder sb = new StringBuilder();
    	CompChemJob actualJob = job;
		if (job.getNumberOfSteps()>1)
		{
			throw new IllegalArgumentException("XTB does not run "
					+ "multi-step jobs, but your input contains more than one "
					+ "step.");
		} else if (job.getNumberOfSteps()==1) {
			// Ignore the container job be cause it contains a single job
			actualJob = (CompChemJob) job.getStep(0);
		}
		
		Iterator<Directive> it = actualJob.directiveIterator();
		while (it.hasNext())
		{
			Directive d = it.next();
			sb.append(StringUtils.mergeListToString(getTextForInput(d,true), NL));
		}
    	return sb;
	}

//------------------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 * 
	 * No special file structure required for XTB. This method does nothing.
	 */
	@Override
	protected File manageOutputFileStructure(List<IAtomContainer> mols,
  			File output) 
  	{
  		return output;
  	}
    
//------------------------------------------------------------------------------

}
