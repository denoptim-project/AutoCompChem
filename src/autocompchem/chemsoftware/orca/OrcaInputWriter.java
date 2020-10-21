package autocompchem.chemsoftware.orca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * Writes input files for software ORCA.
 *
 * @author Marco Foscato
 */

//TODO: write doc

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
		outExtension = OrcaConstants.INPEXTENSION;
	}
    
//------------------------------------------------------------------------------
    
    protected void printInputForOneMol(IAtomContainer mol, 
    		String outFileName, String outFileNameRoot)
    {		
		CompChemJob molSpecJob = ccJob.clone();

		Parameter pathnamePar = new Parameter(
				ChemSoftConstants.PAROUTFILEROOT,outFileNameRoot);
		molSpecJob.setParameter(pathnamePar);
		
		for (Job subJob : molSpecJob.getSteps())
		{
			subJob.setParameter(pathnamePar);
		}
		
		// These calls take care also of the sub-jobs/directives
		molSpecJob.processDirectives(mol);
		molSpecJob.sortDirectivesBy(new OrcaDirectiveComparator());
		
    	// WARNING: for now we are not considering the possibility of having
    	// both directives AND sub jobs. So it's either one or the other
		
		ArrayList<String> lines = new ArrayList<String>();
		if (molSpecJob.getNumberOfSteps()>0)
		{
			for (int i=0; i<molSpecJob.getNumberOfSteps(); i++)
			{
				CompChemJob step = (CompChemJob) molSpecJob.getStep(i);
				lines.addAll(getTextForInput(step));
				if (i<(molSpecJob.getNumberOfSteps()-1))
				{
					lines.add(OrcaConstants.JOBSEPARATOR);
				}
			}
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
					lines.add("#" + k.getName() + " "
						+ StringUtils.mergeListToString(k.getValue()," "));
				} else
				{
					lines.add("#" + StringUtils.mergeListToString(
							k.getValue()," "));	
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
							+ StringUtils.mergeListToString(k.getValue()," ");
					} else
					{
						line = line + " " + StringUtils.mergeListToString(
								k.getValue()," ");
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
							+ StringUtils.mergeListToString(k.getValue()," ");
					} else {
						line = line + " "
							+ StringUtils.mergeListToString(k.getValue()," ");
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
    	
    	String frozenCentersStr = OrcaConstants.INDENT + "{ C";
    	for (Constraint c : cs.getConstrainsWithType(ConstraintType.FROZENATM))
    	{
    		frozenCentersStr = frozenCentersStr + " " + c.getAtomIDs()[0];
    	}
    	frozenCentersStr = frozenCentersStr + " C }";
    	lines.add(frozenCentersStr);
    	
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
			if (k.isLoud())
			{
				line = line + " " + k.getName() + " "
					+ StringUtils.mergeListToString(k.getValue()," ");
			} else {
				line = line + " "
					+ StringUtils.mergeListToString(k.getValue()," ");
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
			if (dd.getName().equals(ChemSoftConstants.DIRDATAGEOMETRY))
			{
				Object o = dd.getValue();
				switch (dd.getType())
				{
					case IATOMCONTAINER:
					{
						IAtomContainer mol = (IAtomContainer) o;
						for (IAtom atm : mol.atoms())
						{
							Point3d p3d = MolecularUtils.getCoords3d(atm);
							lines.add(OrcaConstants.INDENT 
									+ String.format(" %3s",atm.getSymbol())
									+ String.format(" %10.5f",p3d.x)
									+ String.format(" %10.5f",p3d.y)
									+ String.format(" %10.5f",p3d.z));
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

}
