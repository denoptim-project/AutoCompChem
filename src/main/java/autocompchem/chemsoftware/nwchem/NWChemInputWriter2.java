package autocompchem.chemsoftware.nwchem;

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
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software NWChem .
 *
 * @author Marco Foscato
 */

public class NWChemInputWriter2 extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTNWCHEM2)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on NWChem conventions.
     */

    public NWChemInputWriter2() 
    {
		inpExtrension = NWChemConstants.NWCINPEXTENSION;
	}
    
//------------------------------------------------------------------------------
    
    @Deprecated
    protected void printInputForOneMol(IAtomContainer mol, 
    		String outFileName, String outFileNameRoot)
    {		
		//TODO-ggremove
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the method the encodes the syntax of the NWChem input file for a 
     * single job directive. Here we translate all chem.-software-agnostic 
     * components to NWChem-specific format.
     * @param d the directive to translate into text.
     * @param outmost set <code>true</code> if the directive is the outermost 
     * and, thus, must be decorated with the '%' character.
     * @return the list of lines for the input file
     */
    
    private ArrayList<String> getTextForInput(Directive d)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
    	
    	String dirName = d.getName();
        sb.append(dirName).append(" ");

        // keywords are appended in the same line as the directive's name
        Collections.sort(d.getAllKeywords(), new NWChemKeywordComparator2());
        int ik = 0;
        for (Keyword k : d.getAllKeywords())
        {
            ik++;
            String kStr = "";
            if (k.isLoud())
            {
            	kStr = k.getName() + " ";
            }
            
            //TODO-gg del
            //if (dirName.equals("SET") && k.getName().equals("geometry:actlist"))
            //	System.out.println("");
            
            switch (k.getType())
            {
            case STRING:
            	appendToKeywordLine(lines, dirName, sb, 
            			kStr + k.getValueAsString());
            	break;
            	
            case CONSTRAINTSSET:
            	// We expect this only as a way to specify atom lists
            	ConstraintsSet cs = (ConstraintsSet) k.getValue();
        		Set<Integer> ids = new HashSet<Integer>();
            	for (Constraint cns : cs)
                {
            		if (cns.getType()==ConstraintType.FROZENATM)
            		{
            			int[] arr = cns.getAtomIDs();
            			for (int i=0; i<arr.length; i++)
            				ids.add(arr[i]);
            		}
                }
            	List<String> ranges = StringUtils.makeStringForIndexes(
            			NumberUtils.getComplementaryIndexes(ids, 
            					cs.getNumAtoms()), ":", 
            			1); // From 0-based to 1-based
            	boolean first = true;
            	for (String range : ranges)
            	{
            		String str = "";
            		if (first)
            		{
                		str = kStr + " " + range;
                    	appendToKeywordLine(lines, dirName, sb, str);
                    	first = false;
            		} else {
	            		str = " " + range;
	                	appendToKeywordLine(lines, dirName, sb, str);
            		}
            	}
            	break;
            	
			default:
				break;
            }
           
            // TODO-gg verify this
            // Deal with inconsistent syntax of SET and UNSET directives
            // Yes, for some reason these two directives are written differently.
            if (ik < d.getAllKeywords().size()  &&
                (dirName.toUpperCase().equals("SET") || 
                		dirName.toUpperCase().equals("UNSET"))) 
            {
                sb.append(System.getProperty("line.separator"));
                sb.append(dirName).append(NWChemConstants.SUBDIRECTIVEINDENT);
            }
        }
        lines.add(sb.toString());

        //Check against maximum allowed
        if (lines.size() > NWChemConstants.MAXCONCATLINES)
        {
            //TODO: we can try to reduce length by replacing those parts of 
            // the title that are/were added by AutoCompChem with shorter
            // words or abbreviations
            Terminator.withMsgAndStatus("ERROR! Keyword section of directive "
                + dirName + " is more than " + NWChemConstants.MAXCONCATLINES 
                + " lines long, but shortening protocol is not "
                + "implemented in this version of autocompchem. You should use a "
                + "directive's data block ('" + NWChemConstants.LABDATA
                + "' in jobDetails file) rather than a "
                + "keyword.",-1);
        }

        //Then add the data sections
        for (DirectiveData data : d.getAllDirectiveDataBlocks())
        {
        	List<String> ddLines = new ArrayList<String>();
        	switch (data.getType())
        	{
			case BASISSET:
				BasisSet bs = (BasisSet) data.getValue();
				switch (d.getName().toUpperCase())
				{
				case "BASIS":
					ddLines.add(bs.toInputFileStringBS("NWChem"));
					break;
					
				case "ECP":
					ddLines.add(bs.toInputFileStringECP("NWChem"));
					break;
					
				default:
					Terminator.withMsgAndStatus("ERROR! Found basis set as "
							+ "value of a Directive data that is neither ECP "
							+ "nor BASIS, but is '" + d.getName() + "'.",-1);
				}
				break;
				
			case IATOMCONTAINER:
            	boolean useTags = false;
            	if (data.getTaskParams()!=null 
            			&& data.getTaskParams().contains(
            					ChemSoftConstants.PARUSEATMTAGS))
            	{
            		useTags = true;
            	}
				IAtomContainer mol = (IAtomContainer) data.getValue();
				for (IAtom atm : mol.atoms())
				{
					String atmId = atm.getSymbol();
					if (useTags)
					{
						// Convention is to use 1-based indexing here
						atmId = atmId + (mol.indexOf(atm)+1);
					}
					Point3d p3d = AtomUtils.getCoords3d(atm);
					ddLines.add(String.format(Locale.ENGLISH," %3s", atmId)
							+ String.format(Locale.ENGLISH," %10.6f",p3d.x)
							+ String.format(Locale.ENGLISH," %10.6f",p3d.y)
							+ String.format(Locale.ENGLISH," %10.6f",p3d.z));
				}
				break;
				
			case CONSTRAINTSSET:
				ConstraintsSet cSet = (ConstraintsSet) data.getValue();
				switch (d.getName().toUpperCase())
				{
				case NWChemConstants.ZCRDDIR:
					ddLines.addAll(getLinesForZCOORDData(cSet));
					break;
					
				default:
					Terminator.withMsgAndStatus("Found set of constraints"
							+ "in a ditective that is unexpected. Please, "
							+ "contact the authors and present your case.", -1); 
					break;
				}
				break;

        	default:
        		ddLines = data.getLines();
        		break;
        	}
        	
            for (String dataLine : ddLines)
            {
            	dataLine = dataLine.replace(System.getProperty("line.separator"),
            			System.getProperty("line.separator") 
            			+ NWChemConstants.SUBDIRECTIVEINDENT);
                lines.add(NWChemConstants.SUBDIRECTIVEINDENT + dataLine);
            }

            // Deal with inconsistent syntax of SET and UNSET directives
            if (dirName.toUpperCase().equals("SET") || 
                dirName.toUpperCase().equals("UNSET"))
            {
                Terminator.withMsgAndStatus("ERROR! Unexpected use of data "
                    + "block inside a '" + dirName + "' directive. "
                    + "Current NWChem does not support suck possibility.",-1);
            }
        }

        //Then add the sub-directives
        for (Directive subDir : d.getAllSubDirectives())
        {
        	List<String> subDirLines = getTextForInput(subDir);
            for (String dirLine : subDirLines)
            {
            	dirLine = dirLine.replace(System.getProperty("line.separator"),
            			System.getProperty("line.separator") 
            			+ NWChemConstants.SUBDIRECTIVEINDENT);
                lines.add(NWChemConstants.SUBDIRECTIVEINDENT + dirLine);
            }
            
            // Deal with inconsistent syntax of SET and UNSET directives
            if (dirName.toUpperCase().equals("SET") ||
            		dirName.toUpperCase().equals("UNSET"))
            {
                Terminator.withMsgAndStatus("ERROR! Unexpected subdirective "
                    + "inside a '" + dirName + "' directive. Current NWChem "
                    + "does not support such possibility.",-1);
            }
        }

        //Finally end the directive, unless its a SET/UNSET
        if (!dirName.toUpperCase().equals("SET") &&
                !dirName.toUpperCase().equals("UNSET"))
        {
            // NWChem bugs make NWChem input module expect the END label for 
            // DFT and GEOMETRY directives even when there are only keywords 
            if (d.getAllSubDirectives().size() > 0 
            	|| d.getAllDirectiveDataBlocks().size() > 0
                || dirName.toUpperCase().equals(NWChemConstants.DFTDIR)
                || dirName.toUpperCase().equals(NWChemConstants.GEOMDIR))
            {
                lines.add("END");
            }
        }
        return lines;
    }
    
//------------------------------------------------------------------------------
   
    private List<String> getLinesForZCOORDData(ConstraintsSet cs)
    {
    	// Now we prepare the actual lines of text
    	// NB: NWChem used 1-based indexing 
    	List<String> lines = new ArrayList<String>();
    	for (Constraint c : cs.getConstrainsWithType(ConstraintType.DISTANCE))
        {
    		int[] ids = c.getSortedAtomIDs();
        	String str = "BOND " + (ids[0]+1) + " " + (ids[1]+1);
        	if (c.hasValue())
        	{
        		str = str + " " + c.getValue();
        	}
        	if (c.hasOpt())
        	{
        		str = str + " " + c.getOpt();
        	}
        	lines.add(str);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.ANGLE))
        {
    		int[] ids = c.getSortedAtomIDs();
        	String str = "ANGLE " + (ids[0]+1) + " " + (ids[1]+1) + " " 
        			+ (ids[2]+1);
        	if (c.hasValue())
        	{
        		str = str + " " + c.getValue();
        	}
        	if (c.hasOpt())
        	{
        		str = str + " " + c.getOpt();
        	}
        	lines.add(str);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DIHEDRAL))
        {
    		int[] ids = c.getSortedAtomIDs();
        	String str = "TORSION " + (ids[0]+1) + " " + (ids[1]+1) + " " 
        			+ (ids[2]+1) + " " + (ids[3]+1);
        	if (c.hasValue())
        	{
        		str = str + " " + c.getValue();
        	}
        	if (c.hasOpt())
        	{
        		str = str + " " + c.getOpt();
        	}
        	lines.add(str);
        }
    	
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Method to append text for keywords so that the length does not 
     * grow above the max allowed value for NWChem.
     * @param lines the collector of lines that populate the keywords section
     * of a single directive
     * @param dirName the name of the directive for which we are printing the 
     * keywords.
     * @param sb the builder of the current line of keywords.
     * @param appendix the additional text we are trying to append.
     */
    private void appendToKeywordLine(ArrayList<String> lines, String dirName, 
    		StringBuilder sb, String appendix)
    {        
        // We must check that the line does not grow too long. When it gets 
        // too long use backslash to concatenate lines.
        int totalLength = sb.length() + appendix.length() + 1;
        if (totalLength > NWChemConstants.MAXLINELENGTH) 
        {
            // WARNING!
            // Here we assume the deepest possible directive is in layer
            // ten, but there is no actual limit to the number of layers.
            // The assumption is bases on the most common NWChem input files
            // not having more than 3-4 layers of directives, so a maximum
            // estimate of 10 should be safe.
            int singleKeyLenght = appendix.length() + 10; 
            if (singleKeyLenght > NWChemConstants.MAXLINELENGTH)
            {
                String[] words = appendix.split("\\s+");
                for (int i=0; i<words.length; i++)
                {
                    String word = words[i];
                    int expectedLength = sb.length() + word.length() + 1;
                    if (expectedLength > NWChemConstants.MAXLINELENGTH) 
                    {
                        // store the line up to this point
                        String arcLine = sb.toString() + "\\";
                        lines.add(arcLine);
                        // start a new line from scratch
                        sb.delete(0,sb.length());
                        // append indent due to this directive's name
                        for (int j=0; j<dirName.length(); j++)
                        {
                            sb.append(" ");
                        }
                    }
                    sb.append(word).append(" ");
                }
            } else {
                // store the line up to this point
                String arcLine = sb.toString() + "\\";
                lines.add(arcLine);
                // start a new line from scratch
                sb.delete(0,sb.length());
                // append indent due to this directive's name
                for (int i=0; i<dirName.length(); i++)
                {
                    sb.append(" ");
                }
                // append the whole keyword+value string 
                sb.append(appendix).append(" ");
            }
        } else {
            sb.append(appendix).append(" ");
        }
    }

//------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * 
     * This method is not doing anything in NWChem job's main input file. No usage
     * case requiring such functionality.
     */
	@Override
	protected void setSystemSpecificNames(CompChemJob ccj) 
	{}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * In NWChem's main input file the charge is defined in a {@link Keyword}
	 * of the
	 * {@value NWChemConstants#CHARGEDIR} {@link Directive}.
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
    			CompChemJob ccjStep = (CompChemJob)stepJob;
    			if (isPhythonStep(ccjStep))
    			{
    				continue;
    			}
    			ccjStep.setKeywordIfUnset(NWChemConstants.CHARGEDIR, 
    				"value", false, charge);
    		}
    	} else {
    		ccj.setKeywordIfUnset(NWChemConstants.CHARGEDIR, 
    				"value", false, charge);
    	}
	}
	
//------------------------------------------------------------------------------

	private boolean isPhythonStep(CompChemJob ccjStep) 
	{
		return ccjStep.getDirective(NWChemConstants.TASKDIR)!=null &&
				ccjStep.getDirective(NWChemConstants.TASKDIR).getKeyword(
				NWChemConstants.THEORYKW)!=null &&
						ccjStep.getDirective(NWChemConstants.TASKDIR)
						.getKeyword(NWChemConstants.THEORYKW)
						.getValueAsString().toUpperCase().equals("PYTHON");
	}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * In NWChem's main input file the spin multiplicity can be defined 
	 * in two places: as a sub{@link Directive} of the
	 * {@value NWChemConstants#SCFDIR} {@link Directive}, 
	 * or as a {@link Keyword} in the 
	 * {@value NWChemConstants#DFTDIR} {@link Directive}.
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
	
	private static void setSpinMultiplicity(CompChemJob ccjStep, String sm)
	{
		Directive dSCF = ccjStep.getDirective(NWChemConstants.SCFDIR);
		Directive dDFT = ccjStep.getDirective(NWChemConstants.DFTDIR);
		boolean useSCF = false;
		boolean useDFT = false;
		if (dSCF==null && dDFT==null)
		{
			Directive task = ccjStep.getDirective(NWChemConstants.TASKDIR);
			if (task == null)
			{
				throw new IllegalArgumentException("CompChemJob contians "
						+ "neither " + NWChemConstants.SCFDIR + " nor "
						+ NWChemConstants.DFTDIR + " and there is no "
					    + NWChemConstants.TASKDIR + "directive. "
					    + "Unable to guess where "
						+ "to define spin multiplicity.");
			}
			Keyword theory =  task.getKeyword(NWChemConstants.THEORYKW);
			if (theory == null)
			{
				throw new IllegalArgumentException(NWChemConstants.TASKDIR
						+ " does not contain keyword " 
						+ NWChemConstants.THEORYKW + ". Unable to guess where "
						+ "to define spin multiplicity.");
			}
			if (theory.getValueAsString().toUpperCase().startsWith("SCF"))
				useSCF = true;
			else if (theory.getValueAsString().toUpperCase().startsWith("DFT"))
				useDFT = true;
		} else if (dSCF!=null && dDFT==null)
		{
			useSCF = true;
		} else if (dSCF==null && dDFT!=null)
		{
			useDFT = true;
		}
		if (useDFT)
		{
			ccjStep.setKeywordIfUnset(NWChemConstants.DFTDIR, "mult", true, sm);	
		} else if (useSCF) 
		{
			Directive dirSCF = ccjStep.getDirective(NWChemConstants.SCFDIR);
			if (dirSCF==null)
			{
				dirSCF = new Directive(NWChemConstants.SCFDIR);
				if (Integer.parseInt(sm)-1 < 7)
				{
					// NB: here sm-1 is to get the index in the list!
					dirSCF.addSubDirective(new Directive(
							NWChemConstants.SCFSPINMULT.get(
									Integer.parseInt(sm)-1)));
				} else {
					// NB: here sm-1 because we need the number of singly occupied orbitals
					Directive nopenDir = new Directive(NWChemConstants.NOPENDIR);
					nopenDir.addKeyword(new Keyword("value", false, 
							Integer.parseInt(sm)-1));
					dirSCF.addSubDirective(nopenDir);
				}
				ccjStep.setDirective(dirSCF);
			} else {
				Directive smDir = null;
				for (String smStr : NWChemConstants.SCFSPINMULT)
				{
					smDir = dirSCF.getSubDirective(smStr);
					if (smDir!=null)
					{
						// We already have a spin multiplicity and this method
						// is not allowed to overwrite it
						return;
					}
				}
				dirSCF.addSubDirective(new Directive(
						NWChemConstants.SCFSPINMULT.get(
								Integer.parseInt(sm)-1)));
			}
		}
	}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * This method will add one or more geometries if there is no 
	 * {@value ChemSoftConstants#PARGEOMETRY)} task in the innermost and first
	 * {@value NWChemConstants#GEOMDIR} directive, and there is no 
	 * {@value NWChemConstants#RESTARTDIR} directive,
	 * i.e., if the seems to be no other specification of the geometry.
	 */
	@Override
	protected void setChemicalSystem(CompChemJob ccj, List<IAtomContainer> iacs) 
	{
		CompChemJob innermostJob = (CompChemJob) ccj.getInnermostFirstStep();
		if (innermostJob.getDirective(NWChemConstants.RESTARTDIR)!=null)
			return;
		
		Directive origiGeomDir = innermostJob.getDirective(
				NWChemConstants.GEOMDIR);
		if (origiGeomDir==null)
		{
			origiGeomDir = new Directive(NWChemConstants.GEOMDIR);
			innermostJob.addDirective(origiGeomDir);
		}
		boolean removeOriginalDir = false;
		boolean hasAddGeometryTask = false;
		for (int id=0; id<iacs.size(); id++)
		{
			Directive geomDir = null;
			if (iacs.size()==1)
			{
				geomDir = origiGeomDir;
			} else {
				removeOriginalDir = true;
				try {
					geomDir = origiGeomDir.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					Terminator.withMsgAndStatus("ERROR! Unable to clone "
							+ "directive: some value in not cloneable!", -1);
				}
			}
	
			DirectiveData dd = geomDir.getDirectiveData(NWChemConstants.GEOMDIR);
			if (dd==null)
			{
				dd = new DirectiveData();
				dd.setReference(NWChemConstants.GEOMDIR);
				geomDir.addDirectiveData(dd);
			}
			
			ParameterStorage taskParams = dd.getTaskParams();
			hasAddGeometryTask = taskParams!=null 
					&& taskParams.contains(ChemSoftConstants.JDACCTASK)
					&& taskParams.getParameterValue(ChemSoftConstants.JDACCTASK)
						.equals(ChemSoftConstants.PARGEOMETRY);
			if (dd.getValue()==null && !hasAddGeometryTask)
			{
				taskParams = new ParameterStorage();
				taskParams.setParameter(ChemSoftConstants.JDACCTASK, 
						ChemSoftConstants.PARGEOMETRY);
				taskParams.setParameter(ChemSoftConstants.PARUSEATMTAGS, null);
				taskParams.setParameter(ChemSoftConstants.PARMULTIGEOMID, id);
				dd.setTaskParams(taskParams);
				
				if (geomNames.size()==iacs.size())
				{
					geomDir.addKeyword(new Keyword(NWChemConstants.GEOMNAMEKW, 
							false, geomNames.get(id)));
				} else {
					geomDir.addKeyword(new Keyword(NWChemConstants.GEOMNAMEKW, 
							false, MolecularUtils.getNameOrID(iacs.get(id))));
				}
				innermostJob.addDirective(geomDir);
			}
		}
		if (removeOriginalDir)
		{
			// It is executed only if we had more than one geometry AND did
			// not have add_geometry task
			innermostJob.removeDirective(origiGeomDir);
		}
		
		if (hasAddGeometryTask)
			return;
		
		// Redo any multiplication of directive and specification of geometry
		// name in each step that follow the initial one.
		
		CompChemJob master = (CompChemJob) innermostJob.getParent();
		for (int iStep=1; iStep<master.getNumberOfSteps(); iStep++)
		{
			CompChemJob stepJob = (CompChemJob) master.getStep(iStep);
			if (isPhythonStep(stepJob))
			{
				continue;
			}
			Directive origiGeomDirStep = stepJob.getDirective(
					NWChemConstants.GEOMDIR);
			if (origiGeomDirStep==null)
			{
				origiGeomDirStep = new Directive(NWChemConstants.GEOMDIR);
				stepJob.addDirective(origiGeomDirStep);
			}
			removeOriginalDir = false;
			for (int id=0; id<iacs.size(); id++)
			{
				Directive geomDirStep = null;
				if (iacs.size()==1)
				{
					geomDirStep = origiGeomDirStep;
				} else {
					removeOriginalDir = true;
					try {
						geomDirStep = origiGeomDirStep.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
						Terminator.withMsgAndStatus("ERROR! Unable to clone "
								+ "directive: some value in not cloneable!",-1);
					}
				}
				
				if (geomNames.size()==iacs.size())
				{
					geomDirStep.addKeyword(new Keyword(
							NWChemConstants.GEOMNAMEKW, false, 
							geomNames.get(id)));
				} else {
					geomDirStep.addKeyword(new Keyword(
							NWChemConstants.GEOMNAMEKW, false, 
							MolecularUtils.getNameOrID(iacs.get(id))));
				}
				
				if (removeOriginalDir)
				{
					stepJob.addDirective(geomDirStep);
				}
			}
			if (removeOriginalDir)
			{
				stepJob.removeDirective(origiGeomDirStep);
			}
		}
	}

//------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * 
	 * Since NWChem job can have multiple steps, this method accepts 
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
				step.sortDirectivesBy(new NWChemDirectiveComparator2());
				Iterator<Directive> it = step.directiveIterator();
				while (it.hasNext())
				{
					Directive d = it.next();
					lines.addAll(getTextForInput(d));
				}
				if (i<(job.getNumberOfSteps()-1))
				{
					lines.add(NWChemConstants.TASKSEPARATORNW);
				}
			}
    	} else {
			job.sortDirectivesBy(new NWChemDirectiveComparator2());
    		Iterator<Directive> it = job.directiveIterator();
			while (it.hasNext())
			{
				Directive d = it.next();
				lines.addAll(getTextForInput(d));
			}
    	}
    	return lines;
	}
    
//------------------------------------------------------------------------------

}
