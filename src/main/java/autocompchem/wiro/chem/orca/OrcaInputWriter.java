package autocompchem.wiro.chem.orca;

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
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.CenterBasisSet;
import autocompchem.modeling.basisset.ECPShell;
import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.basisset.Shell;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixAtom;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveComponentType;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.IDirectiveComponent;
import autocompchem.wiro.chem.Keyword;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Writes input files for software ORCA.
 *
 * @author Marco Foscato
 */

public class OrcaInputWriter extends ChemSoftInputWriter
{
	/**
	 * Known basis-set defining directive names (or "keywords" using Orca user's
	 * manual language) that can be used to give basis set-related data.
	 */
	public enum BSKEYWORDS {NEWGTO, NEWAUX, 
		NEWECP, 
		NEWAUXJGTO, ADDAUXJGTO, 
		NEWAUXCGTO, ADDAUXCGTO, 
		NEWAUXJKGTO, ADDAUXJKGTO, 
		NEWCABSGTO, ADDCABSGTO
	}
	
	/**
	 * Property name to store the list of basis set components for an atom.
	 */
	private final String ATMSPECBSPROP = "ATMSPECBSPROPDATA";
	
    /**
     * String defining the task of preparing input files for Orca
     */
    public static final String PREPAREINPUTORCATASKNAME = "prepareInputOrca";

    /**
     * Task about preparing input files for Orca
     */
    public static final Task PREPAREINPUTORCATASK;
    static {
    	PREPAREINPUTORCATASK = Task.make(PREPAREINPUTORCATASKNAME);
    }
    
	/**
	 * Flag requesting use of deprecated "new_job" separator instead of using
	 * the compound's job syntax for defining multiple steps in a single 
	 * Orca input file.
	 */
	public static boolean useNewJobSyntax = false;
	
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Orca conventions.
     */

    public OrcaInputWriter() 
    {
		inpExtrension = OrcaConstants.INPEXTENSION;
	}
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	if (params.contains(OrcaConstants.PARNEWJOBSYNTAX))
        {
    		useNewJobSyntax = true;
        }
    }
  
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTORCATASK)));
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
    
    private List<String> getTextForInput(Directive d, boolean outmost)
    {	
    	List<String> lines = new ArrayList<String>();
    	
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
			
			case (OrcaConstants.STARDIRNAME):
			{
				lines.addAll(getTextForCoordsBlock(d,true));
				break;
			}
			
			case (OrcaConstants.COORDSDIRNAME): 
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
			
			case (OrcaConstants.CONSTRAINTSDIRNAME):
			{
				lines.addAll(getTextForConstraintsBlock(d));
				break;
			}
			
			case (OrcaConstants.BASISSETDIRNAME):
			{
			    // NB: here we deal with any basis set information that is not 
			    // specific to a center identified by index in the list of atoms.
			    // Those are projected into the COORDS directive by the 
			    // preProcessingJob() method. See also getTextForCoordsBlock().
				lines.addAll(getTextForBasisSetDirective(d));
				break;
			}
			
			default:
			{
				boolean needEnd = false;
				String line = "";
				if (outmost)
				{
					line = "%";
				}
				line = line + dirName;
				
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
    
    private List<String> getTextForConstraintsBlock(Directive d)
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
    
    private List<String> getTextForCoordsBlock(Directive d, 
    		boolean useStar)
    {
    	List<String> lines = new ArrayList<String>();
		String line = "";
		if (useStar)
		{
			line = "*";
		} else {
			line = "coords";
		}
		
		// NB: while we write the also detect which format of internal coords to
		// use, is not Cartesian
		boolean foundGZMT = false;
		boolean foundInt = false;
		
		d.sortKeywordsBy(new CoordsKeywordsComparator());
		for (Keyword k : d.getAllKeywords())
		{
			if ("INTERNAL".equals(k.getValueAsString().toUpperCase())
					|| "INT".equals(k.getValueAsString().toUpperCase()))
			{
				foundInt = true;
			} else if ("GZMT".equals(k.getValueAsString().toUpperCase())) {
				foundGZMT = true;
			}
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
				case IATOMCONTAINER:
				{
					IAtomContainer mol = (IAtomContainer) o;
					for (IAtom atm : mol.atoms())
					{
						Point3d p3d = AtomUtils.getCoords3d(atm);
						lines.add(OrcaConstants.INDENT 
								+ String.format(Locale.ENGLISH, " %3s", 
										atm.getSymbol())
								+ String.format(Locale.ENGLISH, " " 
										+ formatCartCoord, p3d.x)
								+ String.format(Locale.ENGLISH, " " 
										+ formatCartCoord, p3d.y)
								+ String.format(Locale.ENGLISH, " " 
										+ formatCartCoord, p3d.z));
						Object bsProps = atm.getProperty(ATMSPECBSPROP);
						if (bsProps != null)
						{
							@SuppressWarnings("unchecked")
							List<Directive> bsDirs = (List<Directive>) bsProps;
							for (Directive bsDir : bsDirs)
							{
								for (String bsl : getTextForInput(bsDir, false))
									lines.add(OrcaConstants.INDENT 
											+ OrcaConstants.INDENT + bsl);
							}
						}
					}
					break;
				}
				
				case ZMATRIX:
				{	
					ZMatrix zmat = (ZMatrix) o;
					String errMsg = "";
					if (!foundInt && !foundGZMT) 
					{
						errMsg = "Neither 'int' nor 'gzmt'";
					} else if (foundInt && foundGZMT) 
					{
						errMsg = "Both 'int' and 'gzmt'";
					}
					if (!errMsg.isEmpty()) 
					{
						throw new Error("WARNING! " + errMsg
								+ "were found among the keywords, "
								+ "but Z-Matrix found as a way to "
								+ "define geometry in internal coordinates. "
								+ "This is inconsistent, check your input.");
					} 
					String format = " " + formatIC + " ";
					if (foundInt)
					{
				        for (int i=0; i<zmat.getZAtomCount(); i++)
				        {
				        	ZMatrixAtom atm = zmat.getZAtom(i);
				        	StringBuilder sbAtom = new StringBuilder();
				        	sbAtom.append(atm.getName()).append(" ");
				            int idI = 0;
				            int idJ = 0;
				            int idK = 0;
				            String vI = "0.0";
				            String vJ = "0.0";
				            String vK = "0.0";
				            if (atm.getIdRef(0) != -1)
				            {
				            	//NB: Orca here uses 1-based indexing breaking 
				            	// Orca's own conventions!
				            	idI = atm.getIdRef(0) + 1;
				            	vI = String.format(Locale.ENGLISH, format,
				            			atm.getIC(0).getValue());
				            }
				            if (atm.getIdRef(1) != -1)
				            {
				            	idJ = atm.getIdRef(1) + 1;
				            	vJ = String.format(Locale.ENGLISH, format,
				            			atm.getIC(1).getValue());
				            }
				            if (atm.getIdRef(2) != -1)
				            {
				            	idK = atm.getIdRef(2) + 1;
				            	vK = String.format(Locale.ENGLISH, format,
				            			atm.getIC(2).getValue());
				            }
				            lines.add(OrcaConstants.INDENT
				            		+ atm.getName() + " " 
				            		+ idI + " " 
				            		+ idJ + " " 
				            		+ idK + " "
				            		+ vI + " "
				            		+ vJ + " "
				            		+ vK);    
						}
					}
					if (foundGZMT) 
					{
				        for (int i=0; i<zmat.getZAtomCount(); i++)
				        {
				        	ZMatrixAtom atm = zmat.getZAtom(i);
				        	StringBuilder sbAtom = new StringBuilder();
				        	sbAtom.append(atm.getName()).append(" ");
				            int idI = atm.getIdRef(0);
				            int idJ = atm.getIdRef(1);
				            int idK = atm.getIdRef(2);
				            InternalCoord icI = atm.getIC(0);
				            InternalCoord icJ = atm.getIC(1);
				            InternalCoord icK = atm.getIC(2);
				            if (atm.getIdRef(0) != -1)
				            {
				            	//NB: Orca here uses 1-based indexing breaking 
				            	// Orca's own conventions!
				            	sbAtom.append(idI + 1).append(" ");
				            	sbAtom.append(String.format(Locale.ENGLISH, 
		                    			" " + formatIC + " ", icI.getValue()));
				                if (idJ != -1)
				                {
				                	sbAtom.append(idJ + 1).append(" ");
				                	sbAtom.append(String.format(Locale.ENGLISH, 
			                    			" " + formatIC + " ", icJ.getValue()));
				                    if (idK != -1)
				                    {
				                    	sbAtom.append(idK + 1).append(" ");
				                    	sbAtom.append(String.format(Locale.ENGLISH, 
				                    			" " + formatIC, icK.getValue()));
				                    }
				                }
				            }
				            lines.add(sbAtom.toString());    
						}
					}
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
    
    private List<String> getTextForBasisSetDirective(Directive d)
    {
    	List<String> lines = new ArrayList<String>();
		String header = "%basis";
		
		for (Keyword k : d.getAllKeywords())
		{
			header = header + " " + k.toString(" ");
		}
		
		for (Directive sd1 : d.getAllSubDirectives())
		{
			for (String innerLine : getTextForInput(sd1,false))
			{
				lines.add(OrcaConstants.INDENT + innerLine);
			}
		}
		
    	for (DirectiveData dd : d.getAllDirectiveDataBlocks())
    	{
    		if (dd.getType().equals(NamedDataType.BASISSET))
    		{
    			// We skip this as we have converted the BS into directives
    			// during the preProcessing of the job
    		} else {
				for (String innerLine : dd.getLines())
				{
					lines.add(OrcaConstants.INDENT + innerLine);
				}
    		}
    	}
    	
    	// It is possible to have "%basis" that do not produce any content 
    	// because their content has been moved into the %coords directive. That
    	// is the case for atom-specific basis set.
    	// Here we avoid to print empty "%basis" directives
    	if (lines.size()<1)
    	{
    		if (d.getAllKeywords().size()!=0)
    		{
				lines.add(0, header);
    		}
    	} else {
    		lines.add(0, header);
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
		
		setCharge(ccj, charge);
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Sets the charge controlling directives if not previously present.
	 * @param ccj
	 * @param charge
	 */
	private static void setCharge(CompChemJob ccj, String charge)
	{
		List<Directive> coordDirs = ccj.getDirectives(
				OrcaConstants.COORDSDIRNAME, true);
		int numCoordDirs = coordDirs.size();
		List<Directive> starDirs = ccj.getDirectives(
				OrcaConstants.STARDIRNAME, true);
		int numStarDirs = starDirs.size();
		if (numCoordDirs==0 && numStarDirs==0) {
			DirComponentAddress adrs = new DirComponentAddress();
	    	adrs.addStep(OrcaConstants.COORDSDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	adrs.addStep(OrcaConstants.COORDSCHARGEDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	// NB: this adds at any level of embedding in nested jobs, but does
	    	// not overwrite existing components.
	    	addNewValueContainer(ccj, adrs, new Keyword(
	    			ChemSoftConstants.PARCHARGE, false, charge));
		}
		for (Directive starDir : starDirs)
		{
			if (!starDir.hasComponent(ChemSoftConstants.PARCHARGE, 
					DirectiveComponentType.KEYWORD))
			{
				starDir.setKeyword(new Keyword(ChemSoftConstants.PARCHARGE, 
						false, charge));
			}
		}
		for (Directive coordDir : coordDirs)
		{
			if (!coordDir.hasComponent(OrcaConstants.COORDSCHARGEDIRNAME, 
					DirectiveComponentType.DIRECTIVE))
			{
				Directive chargeDir = new Directive(
						OrcaConstants.COORDSCHARGEDIRNAME);
				chargeDir.addKeyword(new Keyword(
	    			ChemSoftConstants.PARCHARGE, false, charge));
				coordDir.addSubDirective(chargeDir);
			}
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
		
		setSpinMultiplicity(ccj, sm);
	}
	
//------------------------------------------------------------------------------

	private static void setSpinMultiplicity(CompChemJob ccj, String sm)
	{	
		List<Directive> coordDirs = ccj.getDirectives(
				OrcaConstants.COORDSDIRNAME, true);
		int numCoordDirs = coordDirs.size();
		List<Directive> starDirs = ccj.getDirectives(
				OrcaConstants.STARDIRNAME, true);
		int numStarDirs = starDirs.size();
		if (numCoordDirs==0 && numStarDirs==0) {
			DirComponentAddress adrs = new DirComponentAddress();
	    	adrs.addStep(OrcaConstants.COORDSDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	adrs.addStep(OrcaConstants.COORDSMULTDIRNAME, 
	    			DirectiveComponentType.DIRECTIVE);
	    	// NB: this adds at any level of embedding in nested jobs, but does
	    	// not overwrite existing components.
	    	addNewValueContainer(ccj, adrs, new Keyword(
	    			ChemSoftConstants.PARSPINMULT, false, sm));
		}
		for (Directive starDir : starDirs)
		{
			if (!starDir.hasComponent(ChemSoftConstants.PARSPINMULT, 
					DirectiveComponentType.KEYWORD))
			{
				starDir.setKeyword(new Keyword(ChemSoftConstants.PARSPINMULT, 
						false, sm));
			}
		}
		for (Directive coordDir : coordDirs)
		{
			if (!coordDir.hasComponent(OrcaConstants.COORDSMULTDIRNAME, 
					DirectiveComponentType.DIRECTIVE))
			{
				Directive chargeDir = new Directive(
						OrcaConstants.COORDSMULTDIRNAME);
				chargeDir.addKeyword( new Keyword(
	    			ChemSoftConstants.PARSPINMULT, false, sm));
				coordDir.addSubDirective(chargeDir);
			}
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
	public StringBuilder getTextForInput(CompChemJob job) 
	{
    	StringBuilder sb = new StringBuilder();
    	if (job.getNumberOfSteps()>1)
    	{
    		if (useNewJobSyntax) {
		    	for (int i=0; i<job.getNumberOfSteps(); i++)
				{
					CompChemJob step = (CompChemJob) job.getStep(i);
					
		    		preProcessingJob(step);
		    		
					Iterator<Directive> it = step.directiveIterator();
					while (it.hasNext())
					{
						Directive d = it.next();
						sb.append(StringUtils.mergeListToString(
								getTextForInput(d, true), NL));
					}
					if (i<(job.getNumberOfSteps()-1))
					{
						sb.append(OrcaConstants.JOBSEPARATOR).append(NL);
					}
				}
    		} else {
    			preProcessingJob(job);
        		Iterator<Directive> itOut = job.directiveIterator();
    			while (itOut.hasNext())
    			{
    				Directive d = itOut.next();
    				sb.append(StringUtils.mergeListToString(
    						getTextForInput(d, true), NL));
    			}
    			sb.append("%" + OrcaConstants.COMPOUNDDIRNAME).append(NL);
    			for (int i=0; i<job.getNumberOfSteps(); i++)
				{
        			sb.append(OrcaConstants.INDENT ).append(
        					OrcaConstants.COMPOUNDSTEPSTART).append(NL);
					CompChemJob step = (CompChemJob) job.getStep(i);
					
		    		preProcessingJob(step);
		    		
					Iterator<Directive> it = step.directiveIterator();
					while (it.hasNext())
					{
						Directive d = it.next();
						for (String line : getTextForInput(d, true))
						{
							sb.append(OrcaConstants.INDENT).append(line).append(NL);
						}
					}
	    			sb.append(OrcaConstants.INDENT).append(
        					OrcaConstants.COMPOUNDSTEPEND).append(NL);
				}
    			sb.append(OrcaConstants.COMPOUNDEND).append(NL);
    		}
    	} else if (job.getNumberOfSteps()==1) {
    		logger.warn("WARNING! Found a multistep job with only "
    				+ "one step. I assume you meant to prepare the input for "
    				+ "a single step job.");
    		CompChemJob step = (CompChemJob) job.getStep(0);
    		preProcessingJob(step);
    		Iterator<Directive> it = step.directiveIterator();
			while (it.hasNext())
			{
				Directive d = it.next();
				sb.append(StringUtils.mergeListToString(
						getTextForInput(d, true), NL));
			}
    	} else {
    		preProcessingJob(job);
    		Iterator<Directive> it = job.directiveIterator();
			while (it.hasNext())
			{
				Directive d = it.next();
				sb.append(StringUtils.mergeListToString(
						getTextForInput(d, true), NL));
			}
    	}
    	return sb;
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
	 * Does any pre-processing of a single job step. This is meant for any task
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
		
		// move info on basis set, if needed
		DirComponentAddress address = new DirComponentAddress();
		address.addStep(OrcaConstants.BASISSETDIRNAME, 
				DirectiveComponentType.DIRECTIVE);
		for (IDirectiveComponent dc : step.getDirectiveComponents(address))
		{
			if (!dc.getComponentType().equals(DirectiveComponentType.DIRECTIVE))
				continue;
			Directive basisSetDir = (Directive) dc;
			if (basisSetDir != null) 
			{
				for (DirectiveData dd : basisSetDir.getAllDirectiveDataBlocks())
				{
					
					if (dd.getType() != NamedDataType.BASISSET)
						continue;
					
					BasisSet bs = (BasisSet) dd.getValue();
					
					String bsName = "UNDEFINED: use any string among these "
							+ "as "
							+ "directive name (case-insensitive): " 
							+ StringUtils.mergeListToString(Arrays.asList(
									BSKEYWORDS.values()), ",");
					boolean notAnyBSKey = true;
					if (isBSKeyword(dd.getName()))
					{
						bsName = dd.getName();
						notAnyBSKey = false;
					}
					
					// NB: this is a peculiarity of ORCA: any atom specific basis 
					// set must be defined in the %coords directive. Here we 
					// deal with this.
					// Other, basis set info that is not specific to one or more
					// centers is given in the %basis directive. And this is
					// done in the usual getTextForBasisSetDirective 
					List<Directive> systemDefSubDirs = new ArrayList<Directive>();
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
							return; // should not be reached, but satisfies linter
						}
						
						logger.warn("WARNING: "
									+ "An index-specific basis set found in this "
									+ "job. Altering the '" + sysDefDir.getName() 
									+ "' directive, which, we assume, contains a "
									+ "sub-directive named '" 
									+ OrcaConstants.COORDSDIRNAME + "' with the "
									+ "list of atoms/centers to which we associate "
									+ "a basis set.");
						systemDefSubDirs = sysDefDir.getDirectives(
								OrcaConstants.COORDSDIRNAME);
						if (systemDefSubDirs.size()==0)
						{
							Terminator.withMsgAndStatus("WARNING: "
									+ "An index-specific basis set found in "
									+ "this "
									+ "job, but no directive named '" 
									+ OrcaConstants.COORDSDIRNAME + "' is found "
									+ "within the main '"
									+ OrcaConstants.COORDSDIRNAME + "' or '"
									+ OrcaConstants.STARDIRNAME + "' directive.", 
									-1);
						}
					}
						
					for (CenterBasisSet cbs : bs.getAllCenterBSs())
					{
						int idx = -1;
						String elSymbol = "";
						boolean atomSpecific = false;
						if (cbs.getCenterIndex()!=null)
						{
							idx = cbs.getCenterIndex();
							atomSpecific = true;
						} else {
							elSymbol = cbs.getElement();
						}
						
						if (cbs.getShells().size() > 0)
						{
							String dirName = bsName;
							if (!atomSpecific && notAnyBSKey)
							{
								dirName = "NewGTO";
							}
							Directive bsComponentDir = new Directive(dirName);
							bsComponentDir.addDirectiveData(
									new DirectiveData("shells", 
											getBSShellsLines(cbs)));
							if (atomSpecific)
							{
								appendAtomSpecBS(bsComponentDir, idx, 
										systemDefSubDirs);
							} else {
								bsComponentDir.setName(dirName + " " + elSymbol);
								basisSetDir.addSubDirective(bsComponentDir);
							}
						}
						
						if (cbs.getNamedComponents().size() > 0)
						{
							if (cbs.getNamedComponents().size()>1)
							{
								Terminator.withMsgAndStatus("Unable to "
										+ "deal "
										+ "with multiple basis set "
										+ "components "
										+ "for a single center. If this is "
										+ "really what you need, please "
										+ "contact the authors.", -1);
							}

							String dirName = bsName;
							if (!atomSpecific && notAnyBSKey)
							{
								dirName = "NewGTO";
							}
							
							Directive bsComponentDir = new Directive(dirName);
							bsComponentDir.addDirectiveData(
									new DirectiveData("shells", 
											Arrays.asList(
											"\"" 
											+ cbs.getNamedComponents().get(0) 
											+ "\"")));
							if (atomSpecific)
							{
								appendAtomSpecBS(bsComponentDir, idx, 
										systemDefSubDirs);
							} else {
								bsComponentDir.setName(dirName + " " + elSymbol);
								basisSetDir.addSubDirective(bsComponentDir);
							}
						}
						
						if (cbs.getECPShells().size() > 0)
						{
							// Unless we meant to do something specific with ECP
							// we add a newECP so that we do not inherit undesired
							// ECP from elsewhere.
							String dirName = bsName;
							if (!atomSpecific && notAnyBSKey)
							{
								dirName = "NewECP";
							}
							if (!bsName.toUpperCase().contains("ECP"))
							{
								
								dirName = "NewECP";
								String center = "";
								if (atomSpecific)
								{
									center = "center " + idx;
								} else {
									center = "element " + elSymbol;
								}
								
								logger.warn("WARNING: found ECP shells "
										+ "in basis set for " + center 
										+ " (" + dd.getClass().getSimpleName() 
										+ " named '" + dd.getName() + "'). "
										+ "Assuming this is meant as a "
										+ dirName + " section.");
							}
							Directive epcComponentDir = new Directive(dirName);
							epcComponentDir.addDirectiveData(
									new DirectiveData("shells", 
											getECPShellsLines(cbs)));
							if (atomSpecific)
							{
								appendAtomSpecBS(epcComponentDir, idx, 
										systemDefSubDirs);
							} else {
								epcComponentDir.setName(dirName + " " + elSymbol);
								basisSetDir.addSubDirective(epcComponentDir);
							}
						}
					}
				}
			}
		}
	}

//------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private void appendAtomSpecBS(Directive bsDir, int idx, 
			List<Directive> systemDefSubDirs) 
	{
		for (Directive systemDefSubDir : systemDefSubDirs)
		{
			DirectiveData ddSysDef = systemDefSubDir
					.getAllDirectiveDataBlocks().get(0);
			
			switch (ddSysDef.getType())
			{
				case IATOMCONTAINER:
				{
					IAtomContainer iac = (IAtomContainer) ddSysDef.getValue();
					IAtom atm = iac.getAtom(idx);
					if (atm.getProperty(ATMSPECBSPROP)==null)
						atm.setProperty(ATMSPECBSPROP, new ArrayList<Directive>());
					((List<Directive>) atm.getProperty(ATMSPECBSPROP)).add(bsDir);
					break;
				}
				
				case ZMATRIX:
				{
					ZMatrix iac = (ZMatrix) ddSysDef.getValue();
					ZMatrixAtom atm = iac.getZAtom(idx);
					if (atm.getProperty(ATMSPECBSPROP)==null)
						atm.setProperty(ATMSPECBSPROP, new ArrayList<Directive>());
					((List<Directive>) atm.getProperty(ATMSPECBSPROP)).add(bsDir);
					break;
				}
				
				default:
					throw new IllegalStateException("Attempt to alter the "
							+ "value of " 
							+ "a " + DirectiveData.class.getName() 
							+ " of type '" 
							+ ddSysDef.getType() + "' is not implemented. "
							+ "Report this to the authors");
			}
		}
		
	}

//------------------------------------------------------------------------------
	
	private boolean isBSKeyword(String s)
	{
	    for (BSKEYWORDS k : BSKEYWORDS.values()) {
	        if (k.name().equalsIgnoreCase(s)) {
	        	return true;
	        }
	    }
	    return false;
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
			logger.warn("WARNING! since Orca (5.0.4) uses letter 'j' "
					+ "as identifier for angular momentum with azimuthal "
					+ "number 7, and this is contrary to the convention of "
					+ "omitting letter 'j', you may be using the wrong angular "
					+ "momentum! "
					+ "Make sure this is indeed what you want to do.");
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
