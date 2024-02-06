package autocompchem.chemsoftware.orca;

import java.io.File;

/*
 *   Copyright (C) 2016  Marco Foscato
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
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.CenterBasisSet;
import autocompchem.modeling.basisset.ECPShell;
import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.basisset.Shell;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Writes input files for software ORCA.
 *
 * @author Marco Foscato
 */

public class OrcaInputWriter extends ChemSoftInputWriter
{
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Orca conventions.
     */

    public OrcaInputWriter() 
    {
		inpExtrension = OrcaConstants.INPEXTENSION;
	}
  
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(Task.make("prepareInputOrca"))));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new OrcaInputWriter();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the method the encodes the syntax of the Orca input file for a 
     * single job directive. Here we translate all chem.software agnostic 
     * components to Orca-specific format.
     * @param d
     * @param outmost set <code>true</code> if the directive is the outermost 
     * and, thus, must be decorated with the '%' character.
     * @return the list of lines for the input file
     */
    
    private ArrayList<String> getTextForInput(Directive d, boolean outmost)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	String dirName = d.getName();
    	if (dirName.startsWith("#"))
    	{
			// This is a comment line for the Orca input.
			// There can be one or more such lines, each strictly filling
			// ONE single line.
			for (Keyword k : d.getAllKeywords())
			{
				lines.add("#"+k.toString(" "));
			}
			// Sub directives and DirectiveData are not suitable for Orca's
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
    	if (dirName.startsWith("%"))
    	{
    		//OK, I see what you want to do, but for further processing 
    		// we get rid of the '%'. We put it back anyway for all standard
    		// directives that are at the outermost level.
    		dirName = dirName.substring(1).trim();
    	} 
    	
    	// Here we translate all chem.software agnostic components 
    	// to Orca-specific formated text.
		switch (dirName.toUpperCase())
		{
			case ("!"):
			{
				// This is called "the keyword line" in ORCA manual.
				// There can be one or more such lines, each strictly filling
				// ONE single line.
				String line = "!";
				for (Keyword k : d.getAllKeywords())
				{
					line = line + " " + k.toString(" ");
				}
				// Sub directives and DirectiveData are not suitable for Orca's
				// "keyword line", so we do not expect them
				String errTail = "found in Orca's job details within the '!' "
						+ "keyword line. This is unexpected, "
						+ "but if you see an use for it, "
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
				lines.add(line);
				break;
			}
			
			case ("*"): // OrcaConstants.STARDIRNAME //TODO-gg
			{
				lines.addAll(getTextForCoordsBlock(d,true));
				break;
			}
			
			case ("COORDS"): // OrcaConstants.COORDSDIRNAME
			{
				String pre = "";
				if (outmost)
				{
					pre = "%";
				}
				List<String> dirLines = getTextForCoordsBlock(d,false);
				dirLines.set(0, pre+dirLines.get(0));
				lines.addAll(dirLines);
				break;
			}
			
			case ("CONSTRAINTS"):
			{
				lines.addAll(getTextForConstraintsBlock(d));
				break;
			}
			
			//TODO-gg: remove?!
			/*
			case ("BASIS"): // OrcaConstants.BASISSETDIRNAME
			{
			    // NB: here we deal with any basis set information that is not 
			    // specific to a center identified by index in the list of atoms.
			    // Those are projected into the COORDS directive by the 
			    // preProcessingJob() method. See also getTextForCoordsBlock().
				lines.addAll(getTextForBasisSetDirective(d));
				break;
			}
			*/
			
			default:
			{
				boolean needEnd = false;
				String line = "";
				if (outmost)
				{
					line = "%";
				}
				line = line + dirName + " ";
				
				for (Keyword k : d.getAllKeywords())
				{
					line = line + " " + k.toString(" ");
				}
				lines.add(line);
				
				for (Directive sd1 : d.getAllSubDirectives())
				{
					for (String innerLine : getTextForInput(sd1,false))
					{
						lines.add(OrcaConstants.INDENT + innerLine);
						needEnd = true;
					}
				}
				
				for (DirectiveData dd : d.getAllDirectiveDataBlocks())
				{
					for (String innerLine : dd.getLines())
					{
						lines.add(OrcaConstants.INDENT + innerLine);
						needEnd = true;
					}
				}
				if (needEnd)
				{
					lines.add("end");
				}
			}
		}
    	return lines;
    }

//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForConstraintsBlock(Directive d)
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.add(d.getName());
    	
    	//WARNING! We assume there is only one data block, and that is is
    	// indeed a set of constraints.
    	ConstraintsSet cs = (ConstraintsSet) d.getAllDirectiveDataBlocks()
    			.get(0).getValue();
    	
    	if (!cs.getConstrainsWithType(ConstraintType.FROZENATM).isEmpty())
    	{
	    	String frozenCentersStr = OrcaConstants.INDENT + "{ C";
	    	for (Constraint c : cs.getConstrainsWithType(
	    			ConstraintType.FROZENATM))
	    	{
	    		frozenCentersStr = frozenCentersStr + " " + c.getAtomIDs().get(0);
	    	}
	    	frozenCentersStr = frozenCentersStr + " C }";
	    	lines.add(frozenCentersStr);
    	}
    	
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DISTANCE))
        {
        	String frozenBondsStr = OrcaConstants.INDENT + "{ B";
        	if (c.hasValue())
        	{
        		frozenBondsStr = frozenBondsStr + " " + c.getAtomIDs().get(0) + " "
                		+ c.getAtomIDs().get(1) + " " + c.getValue();
        	} else if (c.hasCurrentValue())
        	{
        		frozenBondsStr = frozenBondsStr + " " + c.getAtomIDs().get(0) + " "
                		+ c.getAtomIDs().get(1) + " " + c.getCurrentValue();
        	} else {
                frozenBondsStr = frozenBondsStr + " " + c.getAtomIDs().get(0) + " "
                		+ c.getAtomIDs().get(1);
        	}
        	frozenBondsStr = frozenBondsStr + " C }";
        	lines.add(frozenBondsStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.ANGLE))
        {
            String frozenAngleStr = OrcaConstants.INDENT + "{ A";
            if (c.hasValue())
            {
                frozenAngleStr = frozenAngleStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " " 
                		+ c.getValue();
            } else if (c.hasCurrentValue())
            {
                frozenAngleStr = frozenAngleStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " " 
                		+ c.getCurrentValue();
            } else{
                frozenAngleStr = frozenAngleStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2);
            }
            frozenAngleStr = frozenAngleStr + " C }";
            lines.add(frozenAngleStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DIHEDRAL))
        {
            String frozenTorsStr = OrcaConstants.INDENT + "{ D";
            if (c.hasValue())
            {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " "
                        + c.getAtomIDs().get(3) + " " + c.getValue();
            } else if (c.hasCurrentValue())
            {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " "
                        + c.getAtomIDs().get(3) + " " + c.getCurrentValue();
            } else {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " "
                        + c.getAtomIDs().get(3);
            }
            frozenTorsStr = frozenTorsStr + " C }";
            lines.add(frozenTorsStr);
        }
        for (Constraint c : cs.getConstrainsWithType(
        		ConstraintType.IMPROPERTORSION))
        {
            String frozenTorsStr = OrcaConstants.INDENT + "{ D";
            if (c.hasValue())
            {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " "
                        + c.getAtomIDs().get(3) + " " + c.getValue();
            } else if (c.hasCurrentValue())
            {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " "
                        + c.getAtomIDs().get(3) + " " + c.getCurrentValue();
            } else{
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs().get(0) + " "
                        + c.getAtomIDs().get(1) + " " + c.getAtomIDs().get(2) + " "
                        + c.getAtomIDs().get(3);
            }
            frozenTorsStr = frozenTorsStr + " C }";
            lines.add(frozenTorsStr);
        }
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.UNDEFINED))
        {
            String cStr = OrcaConstants.INDENT;
            switch (c.getAtomIDs().size())
	            {
	            case 1:
	            	cStr = cStr + "{ C " + c.getAtomIDs().get(0);
	            	break;

	            case 2:
	            	cStr = cStr + "{ B " + c.getAtomIDs().get(0) 
	            			+ " "  + c.getAtomIDs().get(1);
	            	break;
	            	
	            case 3:
	            	cStr = cStr + "{ A " + c.getAtomIDs().get(0) 
	            			+ " "  + c.getAtomIDs().get(1)
	    	            	+ " "  + c.getAtomIDs().get(2);
	            	break;

	            case 4:
	            	cStr = cStr + "{ D " + c.getAtomIDs().get(0) 
	            			+ " "  + c.getAtomIDs().get(1)
	    	            	+ " "  + c.getAtomIDs().get(2)
	    	    	        + " "  + c.getAtomIDs().get(3);
	            	break;
            }
            
            if (c.hasValue())
            {
            	cStr = cStr + " " + c.getValue();
            } else if (c.hasCurrentValue())
            {
            	cStr = cStr + " " + c.getCurrentValue();
            }
            cStr = cStr + " C }";
            lines.add(cStr);
        }
        lines.add("end");
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForCoordsBlock(Directive d, 
    		boolean useStar)
    {
    	ArrayList<String> lines = new ArrayList<String>();
		String line = "";
		if (useStar)
		{
			line = "*";
		} else {
			line = "coords";
		}
		
		d.sortKeywordsBy(new CoordsKeywordsComparator());
		for (Keyword k : d.getAllKeywords())
		{
			line = line + " " + k.toString(" ");
		}
		lines.add(line);
		
		for (Directive sd1 : d.getAllSubDirectives())
		{
			for (String innerLine : getTextForInput(sd1,false))
			{
				lines.add(OrcaConstants.INDENT + innerLine);
			}
		}
		
		for (DirectiveData dd : d.getAllDirectiveDataBlocks())
		{
			Object o = dd.getValue();
			switch (dd.getType())
			{
			
				//TODO: you can save atom-specific basis set into the atom
			    // and retrieve then here. This because ORCA only accepts 
			    // atom specific information into the coords/xyz directive.
			
				case IATOMCONTAINER:
				{
					IAtomContainer mol = (IAtomContainer) o;
					for (IAtom atm : mol.atoms())
					{
						Point3d p3d = AtomUtils.getCoords3d(atm);
						lines.add(OrcaConstants.INDENT 
								+ String.format(Locale.ENGLISH," %3s",atm.getSymbol())
								+ String.format(Locale.ENGLISH," %10.5f",p3d.x)
								+ String.format(Locale.ENGLISH," %10.5f",p3d.y)
								+ String.format(Locale.ENGLISH," %10.5f",p3d.z));
					}
					break;
				}
				
				case ZMATRIX:
				{
					//TODO
					Terminator.withMsgAndStatus("ERROR! Writing of "
							+ "ZMatrix in Orca input file is not yet"
							+ "implemented... sorry!",-1);
					break;
				}
				
				default:
				{	
					for (String innerLine : dd.getLines())
					{
						lines.add(OrcaConstants.INDENT + innerLine);
					}
					break;
				}
			}
		}

		if (useStar)
		{
			if (lines.size()>1)
			{
				lines.add("*");
			}
		} else {
			lines.add("end");
		}
    	return lines;
    }

//------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * 
     * This method is not doing anything in Orca job's main input file. No usage
     * case requiring such functionality.
     */
	@Override
	protected void setSystemSpecificNames(CompChemJob ccj) 
	{}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * In Orca's main input file the charge is defined in the
	 * {@value ChemSoftConstants#PARCHARGE} {@link Keyword} of the
	 * {@value OrcaConstants#COORDSDIRNAME} {@link Directive}.
	 */
	@Override
	protected void setChargeIfUnset(CompChemJob ccj, String charge, 
			boolean omitIfPossible) 
	{
		if (omitIfPossible)
			return;
		
		if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
        		setCharge((CompChemJob) stepJob, charge);
    		}
    	} else {
    		setCharge(ccj, charge);
    	}
	}
	
//------------------------------------------------------------------------------
	
	private static void setCharge(CompChemJob ccj, String charge)
	{
		Directive dCoords = ccj.getDirective(OrcaConstants.COORDSDIRNAME);
		Directive dStar = ccj.getDirective(OrcaConstants.STARDIRNAME);
		if (dCoords==null && dStar==null)
		{
	    	DirComponentAddress adrs = new DirComponentAddress();
	    	adrs.addStep(OrcaConstants.COORDSDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	adrs.addStep(OrcaConstants.COORDSCHARGEDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	addNewValueContainer(ccj, adrs, new Keyword(
	    			ChemSoftConstants.PARCHARGE, false, charge));
		} else if (dCoords!=null && dStar==null)
		{
	    	DirComponentAddress adrs = new DirComponentAddress();
	    	adrs.addStep(OrcaConstants.COORDSDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	adrs.addStep(OrcaConstants.COORDSCHARGEDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	addNewValueContainer(ccj, adrs, new Keyword(
	    			ChemSoftConstants.PARCHARGE, false, charge));
		} else if (dCoords==null && dStar!=null)
		{
			addNewKeyword(ccj, OrcaConstants.STARDIRNAME, 
					ChemSoftConstants.PARCHARGE, false, charge);
		} else {
			Terminator.withMsgAndStatus("ERROR! Neither '"
				+ OrcaConstants.COORDSDIRNAME + "' nor '"
				+ OrcaConstants.STARDIRNAME + "' directive found. "
				+ "Cannot place charge info onto the"
				+ "definition of the system.", -1);
		}
	}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * In Orca's main input file the spin multiplicity is defined in the
	 * {@value ChemSoftConstants#PARSPINMULT} {@link Keyword} of the
	 * {@value OrcaConstants#COORDSDIRNAME} {@link Directive} 
	 */
	@Override
	protected void setSpinMultiplicityIfUnset(CompChemJob ccj, String sm,
			boolean omitIfPossible) 
	{
		if (omitIfPossible)
			return;
		
		if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
    			setSpinMultiplicity((CompChemJob) stepJob, sm);
    		}
    	} else {
    		setSpinMultiplicity(ccj, sm);
    	}
	}
	
//------------------------------------------------------------------------------

	public static void setSpinMultiplicity(CompChemJob ccj, String sm)
	{
		Directive dCoords = ccj.getDirective(OrcaConstants.COORDSDIRNAME);
		Directive dStar = ccj.getDirective(OrcaConstants.STARDIRNAME);
		if (dCoords==null && dStar==null)
		{
	    	DirComponentAddress adrs = new DirComponentAddress();
	    	adrs.addStep(OrcaConstants.COORDSDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	adrs.addStep(OrcaConstants.COORDSMULTDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	addNewValueContainer(ccj, adrs, new Keyword(
	    			ChemSoftConstants.PARSPINMULT, false, sm));
		} else if (dCoords!=null && dStar==null)
		{
	    	DirComponentAddress adrs = new DirComponentAddress();
	    	adrs.addStep(OrcaConstants.COORDSDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	adrs.addStep(OrcaConstants.COORDSMULTDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	addNewValueContainer(ccj, adrs, new Keyword(
	    			ChemSoftConstants.PARSPINMULT, false, sm));
		} else if (dCoords==null && dStar!=null)
		{
			addNewKeyword(ccj, OrcaConstants.STARDIRNAME, 
					ChemSoftConstants.PARSPINMULT, false, sm);
		} else {
			Terminator.withMsgAndStatus("ERROR! Neither '"
					+ OrcaConstants.COORDSDIRNAME + "' nor '"
					+ OrcaConstants.STARDIRNAME + "' directive found. "
					+ "Cannot place multiplicity info onto the"
					+ "definition of the system.", -1);
			}
	}
	
//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * For Orca jobs we exploit the ACC task resulting from 
	 * {@link ChemSoftConstants.PARGEOMETRY}.
	 */
	@Override
	protected void setChemicalSystem(CompChemJob ccj, List<IAtomContainer> iacs) 
	{}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * Since Orca job can have multiple steps, this method accepts 
	 * {@link CompChemJob}s containing multiple steps.
	 */
	@Override
	protected ArrayList<String> getTextForInput(CompChemJob job) 
	{
    	ArrayList<String> lines = new ArrayList<String>();
    	if (job.getNumberOfSteps()>1)
    	{
	    	for (int i=0; i<job.getNumberOfSteps(); i++)
			{
				CompChemJob step = (CompChemJob) job.getStep(i);
				
	    		preProcessingJob(step);
	    		
				Iterator<Directive> it = step.directiveIterator();
				while (it.hasNext())
				{
					Directive d = it.next();
					lines.addAll(getTextForInput(d, true));
				}
				if (i<(job.getNumberOfSteps()-1))
				{
					lines.add(OrcaConstants.JOBSEPARATOR);
				}
			}
    	} else if (job.getNumberOfSteps()==1) {
    		System.out.println(NL + "WARNING! Found a multistep job with only "
    				+ "one step. I assume you menat to prepare the input for"
    				+ "a single step job." + NL);
    		CompChemJob step = (CompChemJob) job.getStep(0);
    		preProcessingJob(step);
    		Iterator<Directive> it = step.directiveIterator();
			while (it.hasNext())
			{
				Directive d = it.next();
				lines.addAll(getTextForInput(d, true));
			}
    	} else {
    		preProcessingJob(job);
    		Iterator<Directive> it = job.directiveIterator();
			while (it.hasNext())
			{
				Directive d = it.next();
				lines.addAll(getTextForInput(d, true));
			}
    	}
    	return lines;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 * 
	 * No special file structure required for Orca. This method does nothing.
	 */
	@Override
  	protected File manageOutputFileStructure(List<IAtomContainer> mols,
  			File output) 
  	{
  		return output;
  	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Does nay pre-processing of a single job step. This is meant for any task
	 * that needs to be done within a job step as to alter the directive to 
	 * adhere to Orca's idiosyncrasies, such as the handling of atom-specific
	 * basis set inside the directive defining coordinates. 
	 */
	private void preProcessingJob(CompChemJob step)
	{
		Directive sysDefDir = step.getDirective(OrcaConstants.COORDSDIRNAME);
		if (sysDefDir == null) 
		{
			sysDefDir = step.getDirective(OrcaConstants.STARDIRNAME);
		}
		// NB: sysDefDir may still be null! Later we do check for null.
		
		// mode info on basis set, if needed
		Directive basisSetDir = step.getDirective(OrcaConstants.BASISSETDIRNAME);
		if (basisSetDir != null) 
		{
			Set<DirectiveData> ddsToRemove = new HashSet<DirectiveData>();
			for (DirectiveData dd : basisSetDir.getAllDirectiveDataBlocks())
			{
				
				if (dd.getType() != NamedDataType.BASISSET)
					continue;
				
				BasisSet bs = (BasisSet) dd.getValue();
				if (bs.hasIndexSpecificComponents())
				{
					if (sysDefDir == null)
					{
						Terminator.withMsgAndStatus("ERROR! Neither '"
							+ OrcaConstants.COORDSDIRNAME + "' nor '"
							+ OrcaConstants.STARDIRNAME + "' directive found. "
							+ "Cannot project atom specific info onto the"
							+ "definition of the system. Yet, the following "
							+ "contains atom-specific information: " + NL
							+ dd, -1);
					}
					
					//TODO-gg: place info into COORDS directive
					
				} else {
					// Convert basis set into directive for Orca
					// Note that there are more than NewGTO and NewECP, but 
					// their use is most probably not highly relevant for
					// the scope of ACC applications. 
					for (CenterBasisSet cbs : bs.centerBSs)
					{
						Directive bsComponentDir = new Directive("NewGTO " 
								+ cbs.getElement());
						bsComponentDir.addDirectiveData(
								new DirectiveData("shells", 
										getBSShellsLines(cbs)));
						basisSetDir.addSubDirective(bsComponentDir);
						
						if (cbs.getECPShells().size() > 0)
						{
							Directive epcComponentDir = new Directive("NewECP " 
									+ cbs.getElement());
							epcComponentDir.addDirectiveData(
									new DirectiveData("shells", 
											getECPShellsLines(cbs)));
							basisSetDir.addSubDirective(epcComponentDir);
						}
					}
				}
				ddsToRemove.add(dd);
			}
			// Now remove all directive data: they have been converted as to
			// move the info into either sub-dirs or into the COORDS directive.
			for (DirectiveData dd : ddsToRemove)
				basisSetDir.deleteComponent(dd);
		}
	}

//------------------------------------------------------------------------------
	
	private List<String> getBSShellsLines(CenterBasisSet cbs) 
	{
    	List<String> lines = new ArrayList<String>();
    	for (Shell s : cbs.getShells())
        {
    		// NB: no scaling factor. Use Orca's "SCALE X statement"
        	lines.add(String.format(Locale.ENGLISH, "%-3s %-3d",
        			s.getType(), s.getSize()));
        	int i = 0;
            for (Primitive p : s.getPrimitives())
            {
            	i++;
            	
                String eForm = " %" + (p.getExpPrecision()+7) + ".7f     ";
                String line = OrcaConstants.INDENT + i 
                		+ String.format(Locale.ENGLISH, eForm,p.getExp());
                
                String cForm = " %" + (p.getCoeffPrecision()) + ".7f     ";
                for (Double c : p.getCoeff())
                {
                	line = line + String.format(Locale.ENGLISH, cForm, c);
                }
                lines.add(line);
            }
        }
        return lines;
	}
	
//------------------------------------------------------------------------------
	
	private List<String> getECPShellsLines(CenterBasisSet cbs) 
	{
		List<String> lines = new ArrayList<String>();
		
    	lines.add("N_core " + String.format(Locale.ENGLISH, "%2d", 
    			cbs.getElectronsInECP()));
        lines.add("lmax " + convertAngMomentumToLetter(cbs.getECPMaxAngMom()));
        
        for (ECPShell s : cbs.getECPShells())
        {
        	lines.add(String.format(Locale.ENGLISH, "%-1s %2d", 
        			s.getType().toLowerCase().substring(0, 1), s.getSize()));
        	int i=0;
            for (Primitive p : s.getPrimitives())
            {
            	i++;
            	String line = OrcaConstants.INDENT + i;
                String eForm = " %" + (p.getExpPrecision()) + ".7f     ";
                line = line + String.format(Locale.ENGLISH, eForm, p.getExp());
                
                String cForm = " %" + (p.getCoeffPrecision()) + ".7f     ";
                // NB: in Orca we expect only one coefficient
                if (p.getCoeff().size()>1)
                {
                	Terminator.withMsgAndStatus("ERROR! In Orca we expect only "
                			+ "one coefficient, but I've found " 
                			+ p.getCoeff().size()
                			+ " in the basis set. Change format of basis set.",
                			-1);
                }
                line = line + String.format(Locale.ENGLISH, cForm, 
                		p.getCoeff().get(0));

            	// NB: rather  than ang. mom. is "radial power" in Orca.
                line = line + String.format(Locale.ENGLISH, " %-1d", 
                		p.getAngMmnt());
                lines.add(line);
            }
        }
    	return lines;
	}
    
//------------------------------------------------------------------------------
	
	/**
	 * Maps angular momentum identifiers. Note that Orca (contrary to 
	 * convention) includes the "j" letter. Empirical tests also show that 
	 * the lmax in the ECP definition does not go above k, even though the 
	 * Orca manusl (5.0.4) does mention "l" (angular momentum 9) in section 
	 * 9.5.3.
	 * Therefore, we use this Orca-specific
	 * mapping of Azimuthal numbers to historical letters.
	 */
    
	private String convertAngMomentumToLetter(int azimuthalNumber)
	{
		String l = null;
		if (azimuthalNumber>6)
		{
			System.out.println(NL + NL +
					"WARNING! since Orca (5.0.4) uses letter 'j' "
					+ "as identifier for angular momentum with azimuthal "
					+ "number 7, and this is contrary to the convention of "
					+ "omitting letter 'j', you may be using the wrong angular "
					+ "momentum! Make sure this is indeed what you want to do."
					+ NL + NL);
		}
		switch (azimuthalNumber)
		{
		case 0:
			l = "s";
			break;
		case 1:
			l = "p";
			break;
		case 2:
			l = "d";
			break;
		case 3:
			l = "f";
			break;
		case 4:
			l = "g";
			break;
		case 5:
			l = "h";
			break;
		case 6:
			l = "i";
			break;
		case 7:
			l = "j";
			break;
		case 8:
			l = "k";
			break;
		default:
			Terminator.withMsgAndStatus("ERROR! You requied to convert "
    				+ "angular momentom " + azimuthalNumber
    				+ " into historical letter, but Orca 5.0.4 declares "
    				+ "up to 'k' as maximum angular momentum. If this "
    				+ "convention has changed, please inform the authors of "
    				+ "AutoCompChem for an upgrade", -1);
		}
		return l;
	}
	
//------------------------------------------------------------------------------
}
