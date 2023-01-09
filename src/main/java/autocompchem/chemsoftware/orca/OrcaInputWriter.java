package autocompchem.chemsoftware.orca;

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
import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.io.IOtools;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software ORCA.
 *
 * @author Marco Foscato
 */

public class OrcaInputWriter extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTORCA)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Orca conventions.
     */

    public OrcaInputWriter() 
    {
		inpExtrension = OrcaConstants.INPEXTENSION;
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
				if (k.isLoud())
				{
					lines.add("#" + k.getName() + " " + k.getValueAsString());
				} else
				{
					lines.add("#" + k.getValueAsString());	
				}
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
					if (k.isLoud())
					{
						line = line + " " + k.getName() + " " 
								+ k.getValueAsString();
					} else
					{
						line = line + " " + k.getValueAsString();
					}
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
			
			case ("*"):
			{
				lines.addAll(getTextForCoordsBlock(d,true));
				break;
			}
			
			case ("COORDS"):
			{
				lines.addAll(getTextForCoordsBlock(d,false));
				break;
			}
			
			case ("CONSTRAINTS"):
			{
				lines.addAll(getTextForConstraintsBlock(d));
				break;
			}
			
			//TODO: add handling of customized basis set
			// see https://www.researchgate.net/post/How_to_create_ORCA_input_and_insert_the_basis_sets_manually
			/*
			case ("BASIS"):
			{
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
					if (k.isLoud())
					{
						line = line + " " + k.getName() + " "
								+ k.getValueAsString();
					} else {
						line = line + " " + k.getValueAsString();
					}
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
	    		frozenCentersStr = frozenCentersStr + " " + c.getAtomIDs()[0];
	    	}
	    	frozenCentersStr = frozenCentersStr + " C }";
	    	lines.add(frozenCentersStr);
    	}
    	
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DISTANCE))
        {
        	String frozenBondsStr = OrcaConstants.INDENT + "{ B";
        	if (c.hasValue())
        	{
        		frozenBondsStr = frozenBondsStr + " " + c.getAtomIDs()[0] + " "
                		+ c.getAtomIDs()[1] + " " + c.getValue();
        	} else {
                frozenBondsStr = frozenBondsStr + " " + c.getAtomIDs()[0] + " "
                		+ c.getAtomIDs()[1];
        	}
        	frozenBondsStr = frozenBondsStr + " C }";
        	lines.add(frozenBondsStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.ANGLE))
        {
            String frozenAngleStr = OrcaConstants.INDENT + "{ A";
            if (c.hasValue())
            {
                frozenAngleStr = frozenAngleStr + " " + c.getAtomIDs()[0] + " "
                        + c.getAtomIDs()[1] + " " + c.getAtomIDs()[2] + " " 
                		+ c.getValue();
            } else {
                frozenAngleStr = frozenAngleStr + " " + c.getAtomIDs()[0] + " "
                        + c.getAtomIDs()[1] + " " + c.getAtomIDs()[2];
            }
            frozenAngleStr = frozenAngleStr + " C }";
            lines.add(frozenAngleStr);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DIHEDRAL))
        {
            String frozenTorsStr = OrcaConstants.INDENT + "{ D";
            if (c.hasValue())
            {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs()[0] + " "
                        + c.getAtomIDs()[1] + " " + c.getAtomIDs()[2] + " "
                        + c.getAtomIDs()[3] + " " + c.getValue();
            } else {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs()[0] + " "
                        + c.getAtomIDs()[1] + " " + c.getAtomIDs()[2] + " "
                        + c.getAtomIDs()[3];
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
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs()[0] + " "
                        + c.getAtomIDs()[1] + " " + c.getAtomIDs()[2] + " "
                        + c.getAtomIDs()[3] + " " + c.getValue();
            } else {
                frozenTorsStr = frozenTorsStr + " " + c.getAtomIDs()[0] + " "
                        + c.getAtomIDs()[1] + " " + c.getAtomIDs()[2] + " "
                        + c.getAtomIDs()[3];
            }
            frozenTorsStr = frozenTorsStr + " C }";
            lines.add(frozenTorsStr);
        }
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.UNDEFINED))
        {
            String cStr = OrcaConstants.INDENT;
            switch (c.getAtomIDs().length)
	            {
	            case 1:
	            	cStr = cStr + "{ C " + c.getAtomIDs()[0];
	            	break;

	            case 2:
	            	cStr = cStr + "{ B " + c.getAtomIDs()[0] 
	            			+ " "  + c.getAtomIDs()[1];
	            	break;
	            	
	            case 3:
	            	cStr = cStr + "{ A " + c.getAtomIDs()[0] 
	            			+ " "  + c.getAtomIDs()[1]
	    	            	+ " "  + c.getAtomIDs()[2];
	            	break;

	            case 4:
	            	cStr = cStr + "{ D " + c.getAtomIDs()[0] 
	            			+ " "  + c.getAtomIDs()[1]
	    	            	+ " "  + c.getAtomIDs()[2]
	    	    	        + " "  + c.getAtomIDs()[3];
	            	break;
            }
            
            if (c.hasValue())
            {
            	cStr = cStr + " " + c.getValue();
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
			line = "%coords";
		}
		
		// Ensure we have the right keyword 
		if (d.hasComponent(ChemSoftConstants.DIRDATAGEOMETRY, 
				DirectiveComponentType.DIRECTIVEDATA))
		{
			DirectiveData dd = d.getDirectiveData(
					ChemSoftConstants.DIRDATAGEOMETRY);
			switch (dd.getType())
			{
				//TODO:
				/*
				case ZMATRIX:
				{
					break;
				}
				*/
				case IATOMCONTAINER:
				default:
				{
					d.setKeyword(new Keyword(ChemSoftConstants.PARCOORDTYPE,
			        		false, CoordsType.XYZ.toString()));
					break;
				}
			}
		}
		
		d.sortKeywordsBy(new CoordsKeywordsComparator());
		for (Keyword k : d.getAllKeywords())
		{
			//TODO-gg this is what the toString() method of Keyword should do!
			if (k.isLoud())
			{
				line = line + " " + k.getName() + " " + k.getValueAsString();
			} else {
				line = line + " " + k.getValueAsString();
			}
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
			if (dd.getName().toUpperCase().equals(
					ChemSoftConstants.DIRDATAGEOMETRY))
			{
				Object o = dd.getValue();
				switch (dd.getType())
				{
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
						Terminator.withMsgAndStatus("ERROR! Unable to "
								+ "understand type of geometry '" + 
								dd.getType() + "' in OrcaInputWriter.",-1);
						break;
					}
				}
				
			} else {
				for (String innerLine : dd.getLines())
				{
					lines.add(OrcaConstants.INDENT + innerLine);
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
			ccj.setKeywordIfUnset(OrcaConstants.COORDSDIRNAME, 
			ChemSoftConstants.PARCHARGE, false, charge);
		} else if (dCoords!=null && dStar==null)
		{
			ccj.setKeywordIfUnset(OrcaConstants.COORDSDIRNAME, 
			ChemSoftConstants.PARCHARGE, false, charge);
		} else if (dCoords==null && dStar!=null)
		{
			ccj.setKeywordIfUnset(OrcaConstants.STARDIRNAME, 
			ChemSoftConstants.PARCHARGE, false, charge);
		}
		//One or the other directive must be present!
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
			ccj.setKeywordIfUnset(OrcaConstants.COORDSDIRNAME, 
			ChemSoftConstants.PARSPINMULT, false, sm);
		} else if (dCoords!=null && dStar==null)
		{
			ccj.setKeywordIfUnset(OrcaConstants.COORDSDIRNAME, 
			ChemSoftConstants.PARSPINMULT, false, sm);
		} else if (dCoords==null && dStar!=null)
		{
			ccj.setKeywordIfUnset(OrcaConstants.STARDIRNAME, 
			ChemSoftConstants.PARSPINMULT, false, sm);
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
    	} else {
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

}
