package autocompchem.chemsoftware.nwchem;

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
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AnnotatedAtomTupleList;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.CenterBasisSet;
import autocompchem.modeling.basisset.ECPShell;
import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.basisset.Shell;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.geometry.MolecularGeometryHandler;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixAtom;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Writes input files for software NWChem .
 *
 * @author Marco Foscato
 */

public class NWChemInputWriter extends ChemSoftInputWriter
{
	/**
	 * Flag controlling whether we write upper or lower case directive names.
	 */
    private boolean dirnamesInUppercase = true;
    
    /**
     * String defining the task of preparing input files for NWChem
     */
    public static final String PREPAREINPUTNWCHEMTASKNAME = 
    		"prepareInputNWChem";

    /**
     * Task about preparing input files for NWChem
     */
    public static final Task PREPAREINPUTNWCHEMTASK;
    static {
    	PREPAREINPUTNWCHEMTASK = Task.make(PREPAREINPUTNWCHEMTASKNAME);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on NWChem conventions.
     */

    public NWChemInputWriter() 
    {
		inpExtrension = NWChemConstants.NWCINPEXTENSION;
	}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTNWCHEMTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new NWChemInputWriter();
    }
    
//------------------------------------------------------------------------------
    
    private void configureToUppercase(CompChemJob ccj)
    {
    	// We set the default format for directive names: upper-/lower-case
    	int numUppercase = 0;
    	int numTotal = 0;
    	Iterator<Directive> iter = ccj.directiveIterator();
    	while (iter.hasNext())
    	{
    		Directive d = iter.next();
    		numTotal++;
    		if (d.getName().equals(d.getName().toUpperCase()))
    			numUppercase++;
    	}
    	for (Job step : ccj.getSteps())
    	{
    		CompChemJob ccStep = (CompChemJob) step;
    		Iterator<Directive> iter2 = ccStep.directiveIterator();
        	while (iter2.hasNext())
        	{
        		Directive d = iter2.next();
        		numTotal++;
        		if (d.getName().equals(d.getName().toUpperCase()))
        			numUppercase++;
        	}
    	}
    	this.dirnamesInUppercase = numUppercase > (numTotal/2);
    }
    
//------------------------------------------------------------------------------
    
    private String formatCase(String directiveName)
    {
    	if (dirnamesInUppercase)
    		return directiveName.toUpperCase();
    	else
    		return directiveName.toLowerCase();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the method the encodes the syntax of the NWChem input file for a 
     * single job directive. Here we translate all chem.-software-agnostic 
     * components to NWChem-specific format.
     * @param d the directive to translate into text.
     * @return the list of lines for the input file
     */
    
    private ArrayList<String> getTextForInput(Directive d)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
    	
    	String dirName = d.getName();
    	boolean dirNameIsUpperCase = dirName.equals(dirName.toUpperCase());
        sb.append(dirName).append(" ");

        // keywords are appended in the same line as the directive's name
        Collections.sort(d.getAllKeywords(), new NWChemKeywordComparator());
        int ik = 0;
        for (Keyword k : d.getAllKeywords())
        {
            ik++;
            String kStr = "";
            if (k.isLoud())
            {
            	kStr = k.getName() + " ";
            }
            
            switch (k.getType())
            {
            case STRING:
            	appendToKeywordLine(lines, dirName, sb, 
            			kStr + k.getValueAsString());
            	break;
            	
            case ANNOTATEDATOMTUPLELIST:
            	AnnotatedAtomTupleList tuples = 
            		(AnnotatedAtomTupleList) k.getValue();
        		Set<Integer> ids = new HashSet<Integer>();
            	for (AnnotatedAtomTuple tuple : tuples)
                {
            		ids.addAll(tuple.getAtomIDs());
                }
            	List<String> ranges = StringUtils.makeStringForIndexes(ids, ":", 
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
           
            // Deal with inconsistent syntax of SET and UNSET directives
            // Yes, for some reason these two directives are written differently.
            // YEt, we now can have multiple directives with the same name ,so 
            // there can be multiple SET directives.
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
					ddLines.addAll(formatBasisSetLines(bs));
					break;
					
				case "ECP":
					ddLines.addAll(formatECPLines(bs));
					break;
					
				default:
					Terminator.withMsgAndStatus("ERROR! Found basis set as "
							+ "value of a Directive data that is neither ECP "
							+ "nor BASIS, but is '" + d.getName() + "'.",-1);
				}
				break;
				
			case IATOMCONTAINER:
				IAtomContainer mol = (IAtomContainer) data.getValue();
				for (IAtom atm : mol.atoms())
				{
					String atmId = AtomUtils.getSymbolOrLabel(atm);
					Point3d p3d = AtomUtils.getCoords3d(atm);
					ddLines.add(String.format(Locale.ENGLISH," %3s", atmId)
							+ String.format(Locale.ENGLISH, " " 
									+ precision, p3d.x)
							+ String.format(Locale.ENGLISH, " " 
									+ precision, p3d.y)
							+ String.format(Locale.ENGLISH, " " 
									+ precision, p3d.z));
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
							+ "contact the authors and present your use "
							+ "case.", -1); 
					break;
				}
				break;
				
			case ZMATRIX:
				if (!d.getName().toUpperCase().equals("ZMATRIX"))
					ddLines.add(formatCase("ZMATRIX"));
				
				ZMatrix zmat = (ZMatrix) data.getValue();
				
				if (!zmat.hasConstants()) 
				{
					for (int i=0; i<zmat.getZAtomCount(); i++)
			        {
			        	ZMatrixAtom atm = zmat.getZAtom(i);
			        	StringBuilder sb2 = new StringBuilder();
			        	sb2.append("  ");
			        	sb2.append(atm.getName()).append(" ");
			            int idI = atm.getIdRef(0);
			            int idJ = atm.getIdRef(1);
			            int idK = atm.getIdRef(2);
			            InternalCoord icI = atm.getIC(0);
			            InternalCoord icJ = atm.getIC(1);
			            InternalCoord icK = atm.getIC(2);
			            if (atm.getIdRef(0) != -1)
			            {
			                sb2.append(idI + 1).append(" ");
			                sb2.append(String.format(Locale.ENGLISH, "%5.8f", 
			                		icI.getValue())).append(" ");
			                if (idJ != -1)
			                {
			                    sb2.append(idJ + 1).append(" ");
			                    sb2.append(String.format(Locale.ENGLISH,
			                    		"%5.8f", icJ.getValue())).append(" ");
			                    if (idK != -1)
			                    {
			                        sb2.append(idK + 1).append(" ");
			                        sb2.append(String.format(Locale.ENGLISH,
			                        		"%5.8f", icK.getValue()))
			                        .append(" ");
			                        if (!icK.getType().equals(
			                        		InternalCoord.NOTYPE))
			                        {
			                        	sb2.append(icK.getType());
			                        }
			                    }
			                }
			            }
			            ddLines.add(sb2.toString());
			        }
				} else {
					// First write the ZMatrix itself (with variable names)
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
			            	//NB: 1-based indexing!
			            	sbAtom.append(idI + 1).append(" ");
			            	sbAtom.append(icI.getName()).append(" ");
			                if (idJ != -1)
			                {
			                	sbAtom.append(idJ + 1).append(" ");
			                	sbAtom.append(icJ.getName()).append(" ");
			                    if (idK != -1)
			                    {
			                    	sbAtom.append(idK + 1).append(" ");
			                    	sbAtom.append(icK.getName()).append(" ");
			                    	if (!icK.getType().equals(
			                        		InternalCoord.NOTYPE))
			                        {
			                    		sbAtom.append(icK.getType());
			                        }
			                    }
			                }
			            }
			            ddLines.add(sbAtom.toString());
			        }
			        // Then write the list of variables with initial value
			        ddLines.add(formatCase("VARIABLES"));
			        for (int i=0; i<zmat.getZAtomCount(); i++)
			        {
			        	ZMatrixAtom zatm = zmat.getZAtom(i);
			        	for (int iIC=0; iIC<zatm.getICsCount(); iIC++)
			        	{
			        		InternalCoord ic = zatm.getIC(iIC);
			        		if (ic.isConstant())
			        			continue;
			        		ddLines.add(ic.getName() + " " + String.format(
			        				Locale.ENGLISH," %5.8f", ic.getValue()));
			        	}
			        }
			        
			        // And finally write the list of constants
			        ddLines.add(formatCase("CONSTANTS"));
			        for (int i=0; i<zmat.getZAtomCount(); i++)
			        {
			        	ZMatrixAtom zatm = zmat.getZAtom(i);
			        	for (int iIC=0; iIC<zatm.getICsCount(); iIC++)
			        	{
			        		InternalCoord ic = zatm.getIC(iIC);
			        		if (!ic.isConstant())
			        			continue;
			        		ddLines.add(ic.getName() + " " + String.format(
			        				Locale.ENGLISH," %5.8f", ic.getValue()));
			        	}
			        }
				}
				if (!d.getName().toUpperCase().equals(formatCase("ZMATRIX")))
					ddLines.add(formatCase("END"));
				break;

			case ANNOTATEDATOMTUPLELIST:
            	AnnotatedAtomTupleList tuples = 
            		(AnnotatedAtomTupleList) data.getValue();
        		Set<String> includedLines = new HashSet<String>();
            	for (AnnotatedAtomTuple tuple : tuples)
                {
            		String line = tuple.getPrefix() + " ";
                	boolean first = true;
            		for (String idOrRange : StringUtils.makeStringForIndexes(
            				tuple.getAtomIDs(), ":", 1)) // From 0-based to 1-based)
    				{
            			if (first)
            			{
            				line = line + idOrRange;
            			    first = false;
            			} else {
            				line = line + " " + idOrRange;
            			}
    			
    				}
                	line = line + " " + tuple.getSuffix();
                	if (!includedLines.contains(line))
                	{
                		ddLines.add(line);
                		includedLines.add(line);
                	}
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
            	if (dirNameIsUpperCase)
            		lines.add("END");
            	else
            		lines.add("end");
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
        	String str = "BOND " + c.getPrefix() + (ids[0]+1) + " " + (ids[1]+1);

        	if (c.hasValue() && c.hasCurrentValue())
        	{
        		throw new IllegalArgumentException("Ambiguity! It is not clear "
        				+ "which value to report in NWChem input since "
        				+ "constraint contains both current "
        				+ "and assigned value. "
        				+ "Please, check definition of constraint " + c);
        	}
        	if (c.hasValue())
        	{
        		str = str + " " + c.getValue();
        	}
        	if (c.hasCurrentValue())
        	{
        		str = str + " " + c.getCurrentValue();
        	}
        	if (!c.getSuffix().isBlank())
        	{
        		str = str + " " + c.getSuffix();
        	}
        	lines.add(str);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.ANGLE))
        {
    		int[] ids = c.getSortedAtomIDs();
        	String str = "ANGLE " + c.getPrefix()
        			+ (ids[0]+1) + " " + (ids[1]+1) + " " 
        			+ (ids[2]+1);

        	if (c.hasValue() && c.hasCurrentValue())
        	{
        		throw new IllegalArgumentException("Ambiguity! It is not clear "
        				+ "which value to report in NWChem input since "
        				+ "constraint contains both current "
        				+ "and assigned value. "
        				+ "Please, check definition of constraint " + c);
        	}
        	if (c.hasValue())
        	{
        		str = str + " " + c.getValue();
        	}
        	if (c.hasCurrentValue())
        	{
        		str = str + " " + c.getCurrentValue();
        	}
        	if (!c.getSuffix().isBlank())
        	{
        		str = str + " " + c.getSuffix();
        	}
        	lines.add(str);
        }
        
        for (Constraint c : cs.getConstrainsWithType(ConstraintType.DIHEDRAL))
        {
    		int[] ids = c.getSortedAtomIDs();
        	String str = "TORSION " + c.getPrefix()
        			+ (ids[0]+1) + " " + (ids[1]+1) + " " 
        			+ (ids[2]+1) + " " + (ids[3]+1);

        	if (c.hasValue() && c.hasCurrentValue())
        	{
        		throw new IllegalArgumentException("Ambiguity! It is not clear "
        				+ "which value to report in NWChem input since "
        				+ "constraint contains both current "
        				+ "and assigned value. "
        				+ "Please, check definition of constraint " + c);
        	}
        	if (c.hasValue())
        	{
        		str = str + " " + c.getValue();
        	}
        	if (c.hasCurrentValue())
        	{
        		str = str + " " + c.getCurrentValue();
        	}
        	if (!c.getSuffix().isBlank())
        	{
        		str = str + " " + c.getSuffix();
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
    			addNewKeyword(ccjStep, formatCase(NWChemConstants.CHARGEDIR), 
    				"value", false, charge);
    		}
    	} else {
    		addNewKeyword(ccj, formatCase(NWChemConstants.CHARGEDIR), 
    				"value", false, charge);
    	}
	}
	
//------------------------------------------------------------------------------

	private boolean isPhythonStep(CompChemJob ccjStep) 
	{
		return ccjStep.getDirective(NWChemConstants.TASKDIR)!=null &&
				ccjStep.getDirective(NWChemConstants.TASKDIR).getFirstKeyword(
				NWChemConstants.THEORYKW)!=null &&
						ccjStep.getDirective(NWChemConstants.TASKDIR)
						.getFirstKeyword(NWChemConstants.THEORYKW)
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
	
	private void setSpinMultiplicity(CompChemJob ccjStep, String sm)
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
			Keyword theory = task.getFirstKeyword(NWChemConstants.THEORYKW);
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
			addNewKeyword(ccjStep, formatCase(NWChemConstants.DFTDIR), "mult", 
					true, sm);	
		} else if (useSCF) 
		{
			Directive dirSCF = ccjStep.getDirective(formatCase(
					NWChemConstants.SCFDIR));
			if (dirSCF==null)
			{
				dirSCF = new Directive(formatCase(NWChemConstants.SCFDIR));
				if (Integer.parseInt(sm)-1 < 7)
				{
					// NB: here sm-1 is to get the index in the list!
					dirSCF.addSubDirective(new Directive(formatCase(
							NWChemConstants.SCFSPINMULT.get(
									Integer.parseInt(sm)-1))));
				} else {
					// NB: here sm-1 because we need the number of singly occupied orbitals
					Directive nopenDir = new Directive(
							formatCase(NWChemConstants.NOPENDIR));
					nopenDir.addKeyword(new Keyword("value", false, 
							Integer.parseInt(sm)-1));
					dirSCF.addSubDirective(nopenDir);
				}
				ccjStep.setDirective(dirSCF);
			} else {
				Directive smDir = null;
				for (String smStr : NWChemConstants.SCFSPINMULT)
				{
					smDir = dirSCF.getFirstDirective(smStr);
					if (smDir!=null)
					{
						// We already have a spin multiplicity and this method
						// is not allowed to overwrite it
						return;
					}
				}
				dirSCF.addSubDirective(new Directive(formatCase(
						NWChemConstants.SCFSPINMULT.get(
								Integer.parseInt(sm)-1))));
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
		// Just to try to be as consistent as possible wrt upper/lower case
		configureToUppercase(ccj);
		
		CompChemJob innermostJob = (CompChemJob) ccj.getInnermostFirstStep();
		if (innermostJob.getDirective(NWChemConstants.RESTARTDIR)!=null)
			return;
		
		DirComponentAddress addressToGeomDir = new DirComponentAddress();
		addressToGeomDir.addStep(NWChemConstants.GEOMDIR,
				DirectiveComponentType.DIRECTIVE);
		
		List<IDirectiveComponent> allGeomDirs = 
				innermostJob.getDirectiveComponents(addressToGeomDir);
		if (allGeomDirs.size()>0)
		{
			// We have geometry directives. In it there are many alternative 
			// ways to define a geometry, so we assume that any such way is 
			// present and we do not do anything. 
			return;
		} else {
			// No directive 'geometry'. Make as many as needed to host the input
			// structures.
			for (int i=0; i<iacs.size(); i++)
			{
				IAtomContainer mol = iacs.get(i);
				Directive newGeomDir = new Directive(formatCase(
						NWChemConstants.GEOMDIR));
				DirectiveData dd = new DirectiveData(formatCase(
						NWChemConstants.GEOMDIR));
				dd.setValue(mol);
				newGeomDir.addDirectiveData(dd);

				String geomName = MolecularUtils.getNameIDOrNull(mol);
				if (geomName!=null)
				{
					newGeomDir.addKeyword(new Keyword(
							NWChemConstants.GEOMNAMEKW, false, geomName));
				}
				
				innermostJob.addDirective(newGeomDir);
			}
		}
		
		
		// Old approach tries to do too much and gets in clash with the 
		// flexibility of the job details:
		// - if we have multiple geometry directives in the job details,
		// then this will take one and remove the others.
		// - it does not consider the multiple alternative ways to define 
		// geometries (Cartesian, Load, ZMAtrix), so it risks to add a geometry
		// where there already is one defined in a nother way.
		
		/*
		Directive origiGeomDir = innermostJob.getDirective(
				NWChemConstants.GEOMDIR);
		if (origiGeomDir==null)
		{
			origiGeomDir = new Directive(formatCase(NWChemConstants.GEOMDIR));
			innermostJob.addDirective(origiGeomDir);
		}
		
		boolean removeOriginalDir = false;
		boolean hasAddGeometryTask = false;
		boolean usingOriginalDir = false;
		for (int id=0; id<iacs.size(); id++)
		{
			Directive geomDir = null;
			if (iacs.size()==1)
			{
				geomDir = origiGeomDir;
				usingOriginalDir = true;
			} else {
				removeOriginalDir = true;
				try {
					geomDir = origiGeomDir.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					Terminator.withMsgAndStatus("ERROR! Unable to clone "
							+ "directive: some value is not cloneable!", -1);
				}
			}
	
			DirectiveData dd = geomDir.getFirstDirectiveData(
					NWChemConstants.GEOMDIR);
			if (dd==null)
			{
				dd = new DirectiveData(formatCase(NWChemConstants.GEOMDIR));
				geomDir.addDirectiveData(dd);
			}
			
			ParameterStorage taskParams = dd.getTaskParams();
			// TODO-gg why is this done only for NWChem?
			hasAddGeometryTask = taskParams!=null 
					&& taskParams.contains(ChemSoftConstants.JDACCTASK)
					&& Task.make(taskParams.getParameterValue(
							ChemSoftConstants.JDACCTASK))
					.equals(MolecularGeometryHandler.GETMOLECULARGEOMETRYTASK);
			if (dd.getValue()==null && !hasAddGeometryTask)
			{
				taskParams = new ParameterStorage();
				//TODO-gg should probably make Task behave in jsonable parameter storage.
				taskParams.setParameter(ChemSoftConstants.JDACCTASK, 
						MolecularGeometryHandler.GETMOLECULARGEOMETRYTASK.ID);
				if (useAtomTags)
					taskParams.setParameter(ChemSoftConstants.PARUSEATMTAGS, null);
				taskParams.setParameter(ChemSoftConstants.PARMULTIGEOMID, 
						NamedDataType.INTEGER, id);
				dd.setTaskParams(taskParams);
				
				if (geomNames.size()==iacs.size())
				{
					geomDir.addKeyword(new Keyword(NWChemConstants.GEOMNAMEKW, 
							false, geomNames.get(id)));
				} else {
					String name = MolecularUtils.getNameIDOrNull(iacs.get(id));
					if (name!=null)
						geomDir.addKeyword(new Keyword(
								NWChemConstants.GEOMNAMEKW, false, name));
				}
				if (!usingOriginalDir)
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
		*/
		
		if (innermostJob==ccj)
			// Need this to handle simple jobs with single step that is the 
			// outermost job. In that case there is no parent and no steps
			return;
		
		// Still, part of the old, too-specific approach
		/*
		
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
				continue;
				// Do not add empty 'geometry' directives
				//
				//origiGeomDirStep = new Directive(formatCase(
				//		NWChemConstants.GEOMDIR));
				//stepJob.addDirective(origiGeomDirStep);
				//
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
		*/
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
    	if (job.getNumberOfSteps()>0)
    	{
	    	for (int i=0; i<job.getNumberOfSteps(); i++)
			{
				CompChemJob step = (CompChemJob) job.getStep(i);
				step.sortDirectivesBy(new NWChemDirectiveComparator());
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
			job.sortDirectivesBy(new NWChemDirectiveComparator());
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
    
    /**
     * Prepares the lines of text that are meant to define a basis set in 
     * NWChem's input files.
     * @param bs the basis set to format to a list of strings.
     * @return the list of strings where each string is a line (without new line
     * character at the end).
     */
    public static List<String> formatBasisSetLines(BasisSet bs) 
	{
    	List<String> lines = new ArrayList<String>();
    	for (CenterBasisSet cbs : bs.centerBSs)
    	{
    		String atmStr = "";
    		if (cbs.getCenterTag()!=null)
    		{
    			atmStr = cbs.getCenterTag();
    		} else if (cbs.getElement()!=null) {
    			atmStr = Character.toUpperCase(cbs.getElement().charAt(0))+"";
    			if (cbs.getElement().length()>1)
    				atmStr = atmStr + cbs.getElement().toLowerCase().substring(1);
		        if (cbs.getCenterIndex()!=null)
		        	atmStr = atmStr + (cbs.getCenterIndex()+1);
    		}
            for (String n : cbs.getNamedComponents())
            {
                if (n.contains(" "))
                {
                	lines.add(String.format(Locale.ENGLISH,
                    		"  %s library \"%s\"", atmStr, n));
                } else {
                	lines.add(String.format(Locale.ENGLISH, 
                    		"  %s library %s", atmStr, n));
                }
            }
            for (Shell s : cbs.getShells())
            {
            	lines.add(String.format(Locale.ENGLISH, "  %s %s", atmStr, 
            			s.getType()));
                for (Primitive p : s.getPrimitives())
                {
                	String line = "";
                    String eForm = "%" + (p.getExpPrecision() + 6) + "."
                    		+ (p.getExpPrecision()-1) + "E      ";
                    line = String.format(Locale.ENGLISH, eForm, p.getExp());
                    
                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
                            + (p.getCoeffPrecision()-1) + "E";
					for (Double c : p.getCoeff())
					{
						line = line + String.format(Locale.ENGLISH, cForm, c);
					}
					lines.add(line);
                }
            }
    	}
    	return lines;
	}
	
//------------------------------------------------------------------------------
      
    /**
     * Prepares the lines of text that are meant to define the ECPs in 
     * NWChem's input files.
     * @param bs the basis set containing the ECPs to format to a list of 
     * strings.
     * @return the list of strings where each string is a line (without new line
     * character at the end).
     */
    public static List<String> formatECPLines(BasisSet bs) 
  	{
      	List<String> lines = new ArrayList<String>();
      	for (CenterBasisSet cbs : bs.getAllCenterBSs())
        {
    		if (cbs.getECPShells().size()==0)
    			continue;
    		
      		String atmStr = "";
    		if (cbs.getCenterTag()!=null)
    		{
    			atmStr = cbs.getCenterTag();
    		} else if (cbs.getElement()!=null) {
    			atmStr = Character.toUpperCase(cbs.getElement().charAt(0))+"";
    			if (cbs.getElement().length()>1)
    				atmStr = atmStr + cbs.getElement().toLowerCase().substring(1);
		        if (cbs.getCenterIndex()!=null)
		        	atmStr = atmStr + (cbs.getCenterIndex()+1);
    		}
    	
  			lines.add(String.format(Locale.ENGLISH,
            		"  %s nelec %s", atmStr, cbs.getElectronsInECP()));
  			
  			boolean first = true;
      		for (ECPShell s : cbs.getECPShells())
      		{
                String ecpsType = s.getType();
                if (first && ecpsType.substring(0,1).toUpperCase().equals(
                            BasisSetConstants.ANGMOMINTTOSTR.get(
                            		cbs.getECPMaxAngMom())))
                {
                    ecpsType = "ul";
                } else {
                    String[] parts = ecpsType.split("-");
                    ecpsType = parts[0]; 
                }
                if (first)
                	first = false;
                lines.add(String.format(Locale.ENGLISH, "  %s %s", atmStr, 
                		ecpsType));
                for (Primitive p : s.getPrimitives())
                {
                	String line = String.format(Locale.ENGLISH, "  %d ", 
                			p.getAngMmnt());
                    
                	String eForm = "%" + (p.getExpPrecision() + 6) + "."
                                           + (p.getExpPrecision()-1) + "E     ";
                    line = line + String.format(Locale.ENGLISH,
                    		eForm,p.getExp());

                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
                            + (p.getCoeffPrecision()-1) + "E";
					for (Double c : p.getCoeff())
					{
						line = line + String.format(Locale.ENGLISH, cForm, c);
					}
					lines.add(line);
                }
      		}
        }
      	
      	return lines;
  	}
    
//------------------------------------------------------------------------------
	
  	/**
  	 * {@inheritDoc}
  	 * 
  	 * No special file structure required for NWChem. This method does nothing.
  	 */
  	@Override
  	protected File manageOutputFileStructure(List<IAtomContainer> mols,
  			File output) 
  	{
  		return output;
  	}
  	
//------------------------------------------------------------------------------

}
