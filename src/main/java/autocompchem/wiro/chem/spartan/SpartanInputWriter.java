package autocompchem.wiro.chem.spartan;

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
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AnnotatedAtomTupleList;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.conformation.ConformationalCoordinate;
import autocompchem.molecule.conformation.ConformationalSpace;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveComponentType;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.Keyword;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Writes input files for software Spartan.
 *
 * @author Marco Foscato
 */

public class SpartanInputWriter extends ChemSoftInputWriter
{
    /**
     * String defining the task of preparing input files for Spartan
     */
    public static final String PREPAREINPUTSPARTANTASKNAME = "prepareInputSpartan";

    /**
     * Task about preparing input files for Spartan
     */
    public static final Task PREPAREINPUTSPARTANTASK;
    static {
    	PREPAREINPUTSPARTANTASK = Task.make(PREPAREINPUTSPARTANTASKNAME);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Spartan conventions.
     */
    public SpartanInputWriter() 
    {
    	inpExtrension = ".spardir";
    }

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTSPARTANTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new SpartanInputWriter();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * In Spartan the charge is defined in a {@link Keyword} named 
     * {@value SpartanConstants#KWCHARGE} of the 
     * {@value SpartanConstants#DIRCHARGEANDSPIN} {@link Directive}.
     * Moreover, the specification of the charge cannot be omitted, so the
     * <code>omitIfPossible</code> parameter does not take effect.
     */
    @Override
    protected void setChargeIfUnset(CompChemJob ccj, String charge, 
    		boolean omitIfPossible)
    {
    	setKeywordIfNotAlreadyThere(ccj, SpartanConstants.DIRCHARGEANDSPIN, 
    			SpartanConstants.KWCHARGE, charge);
    }
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * In Spartan the spin multiplicity is defined in a {@link Keyword} named 
     * {@value SpartanConstants#KWSPIN} of the 
     * {@value SpartanConstants#DIRCHARGEANDSPIN} {@link Directive}.
     * Moreover, the specification of the spin multiplicity cannot be omitted,
     * so the <code>omitIfPossible</code> parameter does not take effect.
     */
    @Override
    protected void setSpinMultiplicityIfUnset(CompChemJob ccj, String sm, 
    		boolean omitIfPossible)
    {
    	setKeywordIfNotAlreadyThere(ccj, SpartanConstants.DIRCHARGEANDSPIN, 
    			SpartanConstants.KWSPIN, sm);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * In Spartan, the chemical system is defined in the {@link DirectiveData} of
     * the {@value SpartanConstants#DIRCART} {@link Directive}. 
     * Here we also set the title of the system is {@link Directive}
     * {@value SpartanConstants#DIRTITLE}, as a keyword.
     * 
     * WARNING: so far it works with only one chemical system. If more than one 
     * system is present we take only the first.
     */
    protected void setChemicalSystem(CompChemJob ccj, List<IAtomContainer> iacs)
    {
    	IAtomContainer iac = iacs.get(0);

    	DirectiveData dd = new DirectiveData("coordinates");
    	dd.setValue(iac);
    	addNewDirectiveData(ccj, SpartanConstants.DIRCART, dd);
    	
    	addNewKeyword(ccj, SpartanConstants.DIRTITLE, "title", 
    			false, MolecularUtils.getNameOrID(iac));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Does nothing. Spartan does not consider more than one system.
     */
    protected void setSystemSpecificNames(CompChemJob ccj)
    {}
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * WARNING: considers only single-step jobs.
     */
    public StringBuilder getTextForInput(CompChemJob job)
    {
    	StringBuilder sb = new StringBuilder();
    	if (job.getNumberOfSteps()>0)
    		throw new IllegalArgumentException("Spartan jobs can not contain "
    				+ "steps! Found Job with " + job.getNumberOfSteps() 
    				+ " steps.");
        
        Directive kwDir = job.getDirective(SpartanConstants.DIRKEYWORDS);
    	if (kwDir != null)
    	{
    		//keywords are written in a single line
    		StringBuilder sbKw = new StringBuilder();
    		for (Keyword k : kwDir.getAllKeywords())
    		{
    			sbKw.append(k.toString("=")).append(" ");
    		}
    		sb.append(sbKw.toString()).append(NL);

            ensureNoDirectiveData(kwDir);
            ensureNoSubdirectives(kwDir);
    	}
    	
        Directive titDir = job.getDirective(SpartanConstants.DIRTITLE);
    	if (titDir != null)
    	{
    		//keywords are written in a single line
    		StringBuilder sbKw = new StringBuilder();
    		for (Keyword k : titDir.getAllKeywords())
    		{
    			sbKw.append(k.toString("=")).append(" ");
    		}
    		sb.append(sbKw.toString()).append(NL);
    		
            ensureNoDirectiveData(titDir);
            ensureNoSubdirectives(titDir);
    	}
    	
        Directive csDir = job.getDirective(SpartanConstants.DIRCHARGEANDSPIN);
    	if (csDir != null)
    	{
    		if (!csDir.hasComponent(SpartanConstants.KWCHARGE, 
    				DirectiveComponentType.KEYWORD))
    			throw new IllegalArgumentException("Directive for charge and "
    					+ "spin contains no keyword '" 
    					+ SpartanConstants.KWCHARGE + "'. Charge not defined!");
    		if (!csDir.hasComponent(SpartanConstants.KWSPIN, 
    				DirectiveComponentType.KEYWORD))
    			throw new IllegalArgumentException("Directive for charge and "
    					+ "spin contains no keyword '" 
    					+ SpartanConstants.KWSPIN + "'. Spin not defined!");
    		if (csDir.getAllKeywords().size()!=2)
    			throw new IllegalArgumentException("Directive for charge and "
    					+ "spin contains unexpected keywords.");
    		
    		StringBuilder sbKw = new StringBuilder();
    		//keywords must be in this order
    		sbKw.append(csDir.getFirstKeyword(
    				SpartanConstants.KWCHARGE).getValueAsString());
    		sbKw.append(" ").append(csDir.getFirstKeyword(
    				SpartanConstants.KWSPIN).getValueAsString());
    		sb.append(sbKw.toString()).append(NL);

            ensureNoDirectiveData(csDir);
            ensureNoSubdirectives(csDir);
    	} else {
    		throw new IllegalArgumentException("Directive '"
    				+ SpartanConstants.DIRCHARGEANDSPIN + "' not found.");
    	}
    	
    	Directive xyzDir = job.getDirective(SpartanConstants.DIRCART);
    	if (xyzDir != null)
    	{
    		ensureSingleDirectiveData(xyzDir);
    		
    		// First the atom numbers and coordinates
    		IAtomContainer mol = (IAtomContainer) xyzDir
    				.getAllDirectiveDataBlocks().get(0).getValue();
    		for (IAtom atm : mol.atoms())
            {
    			Point3d p3d = AtomUtils.getCoords3d(atm);
                StringBuilder sb2 = new StringBuilder();
                sb2.append(String.format(Locale.ENGLISH, "%3d", 
                		atm.getAtomicNumber())).append(" ");
                sb2.append(String.format(Locale.ENGLISH, " " 
						+ precision, p3d.x));
                sb2.append(String.format(Locale.ENGLISH, " " 
						+ precision, p3d.y));
                sb2.append(String.format(Locale.ENGLISH, " " 
						+ precision, p3d.z));
                sb.append(sb2.toString()).append(NL);
            }
            sb.append(SpartanConstants.XYZEND).append(NL);

            ensureNoKeywords(xyzDir);
            ensureNoSubdirectives(xyzDir);
    		
    		// Then the topology
            sb.append(SpartanConstants.TOPOOPN).append(NL);
            Iterator<IAtom> it = mol.atoms().iterator();
            while (it.hasNext())
            {
                StringBuilder sb2 = new StringBuilder();
                for (int i=0; i<12; i++)
                {
                    if (!it.hasNext())
                    {
                        break;
                    }
                    IAtom atm = it.next();
                    sb2.append(String.format(Locale.ENGLISH, "%5d", 
                    		-mol.getConnectedBondsCount(atm)));
                }
                sb.append(sb2.toString()).append(NL);
            }
            for (IBond bnd : mol.bonds())
            {
                int iA = mol.indexOf(bnd.getAtom(0));
                int iB = mol.indexOf(bnd.getAtom(1));
                int bo = -1;
                IBond.Order order = bnd.getOrder();
                if (order == IBond.Order.valueOf("SINGLE"))
                {
                    bo = 1;
                } 
                else if (order == IBond.Order.valueOf("DOUBLE"))
                {
                    bo = 2;
                } 
                else if (order == IBond.Order.valueOf("TRIPLE"))
                {
                    bo = 3;
                } 
                else
                {
                    logger.warn("WARNING! Unknown bond order between "
                            + MolecularUtils.getAtomRef(bnd.getAtom(0),mol)+" and "
                            + MolecularUtils.getAtomRef(bnd.getAtom(1),mol)
                            + " treated as single bond.");
                    bo = 1;
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append(String.format(Locale.ENGLISH, "%5d", iA+1)); //1-based ID
                sb2.append(String.format(Locale.ENGLISH, "%5d", iB+1)); //1-based ID
                sb2.append(String.format(Locale.ENGLISH, "%5d", bo));
                sb.append(sb2.toString()).append(NL);
            }
            sb.append(SpartanConstants.TOPOEND).append(NL);
    	} else {
    		throw new IllegalArgumentException("Directive '"
    				+ SpartanConstants.DIRCART + "' not found.");
    	}
    	
    	if (job.getDirective("HESSIAN")!=null)
    		throw new IllegalArgumentException("Directive 'HESSIAN' is not "
    				+ "generated from the atom container and should not be "
    				+ "present in the job.");
    	
    	Directive txtDir = job.getDirective(SpartanConstants.DIRCOMMENTS);
    	if (txtDir != null)
    	{
    		sb.append(SpartanConstants.COMMOPN).append(NL);
    		for (DirectiveData dd : txtDir.getAllDirectiveDataBlocks())
            {
    			sb.append(SpartanConstants.INDENT + dd.getValueAsString()).append(NL);
            }
            sb.append(SpartanConstants.COMMEND).append(NL);

            ensureNoKeywords(txtDir);
            ensureNoSubdirectives(txtDir);
    	}
    	
    	// WARNING: Omitting Cell definitions directive.
    	
    	Directive frzDir = job.getDirective(SpartanConstants.DIRFROZEN);
    	if (frzDir != null)
    	{
    		ensureSingleDirectiveData(frzDir);
    		
    		sb.append(SpartanConstants.FREEZEOPN).append(NL);

    		List<Integer> frozenIDs = new ArrayList<>();
    		DirectiveData dd = frzDir.getAllDirectiveDataBlocks().get(0);
    		if (dd.getType().equals(NamedDataType.ANNOTATEDATOMTUPLELIST))
    		{
	    		for (AnnotatedAtomTuple tuple : 
	    			(AnnotatedAtomTupleList) dd.getValue())
	            {
	    			for (Integer id : tuple.getAtomIDs())
	    				if (!frozenIDs.contains(id))
	    					frozenIDs.add(id);
	            }
    		} else if (dd.getType().equals(NamedDataType.CONSTRAINTSSET)) 
    		{
    			for (Constraint cns : ((ConstraintsSet) dd.getValue())
    					.getConstrainsWithType(ConstraintType.FROZENATM))
    			{
    				Integer id = cns.getAtomIDs().get(0);
    				if (!frozenIDs.contains(id))
	    					frozenIDs.add(id);
    			}
    					
    		}
    		Collections.sort(frozenIDs);

            StringBuilder sbFrz = new StringBuilder();
            sbFrz.append(SpartanConstants.INDENT);
            int indexCounter = 0;
    		for (Integer id : frozenIDs)
            {
    			indexCounter++;
    			if (indexCounter>12)
    			{
    				indexCounter = 0;
    				sbFrz.append(NL).append(SpartanConstants.INDENT);
    			}
    			// adapting to 1-based indexing
    			sbFrz.append(String.format(Locale.ENGLISH, "%5d", id+1));
            }
			sb.append(sbFrz.toString()).append(NL);
            sb.append(SpartanConstants.FREEZEEND).append(NL);

            ensureNoKeywords(frzDir);
            ensureNoSubdirectives(frzDir);
    	}
    	
    	Directive confDir = job.getDirective(SpartanConstants.DIRCONFORMER);
    	if (confDir != null)
    	{
    		ensureSingleDirectiveData(confDir);
    		
    		sb.append(SpartanConstants.CONFDIROPN).append(NL);
    		ConformationalSpace cs = (ConformationalSpace) confDir
    				.getAllDirectiveDataBlocks().get(0).getValue();
    		for (ConformationalCoordinate coord : cs)
            {
				List<Integer> ints = new ArrayList<Integer>();
				coord.getAtomIDs().stream().forEach(id -> ints.add(id+1));
				String intsStr = StringUtils.mergeListToString(ints, " ");
    			sb.append(SpartanConstants.INDENT)
    				.append(intsStr) // we expect a " " at the end
    				.append(String.format(Locale.ENGLISH, "%5d",
    						coord.getFold()))
    				.append(NL);
            }
            sb.append(SpartanConstants.CONFDIREND).append(NL);
            
            ensureNoKeywords(confDir);
            ensureNoSubdirectives(confDir);
    	}
    	
    	Directive cstrDir = job.getDirective(SpartanConstants.DIRCONSTRAINTS);
    	if (cstrDir != null)
    	{
    		ensureSingleDirectiveData(cstrDir);
    		
    		sb.append(SpartanConstants.CSTRDIROPN).append(NL);
    		
    		ConstraintsSet cs = (ConstraintsSet) cstrDir
    				.getAllDirectiveDataBlocks().get(0).getValue();
    		for (Iterator<Constraint> iter = cs.iterator(); iter.hasNext(); ) 
    		{
    			Constraint cstr = iter.next();
    			List<Integer> ids = cstr.getAtomIDs();
    			
    			// Frozen atoms are specified in a dedicated directive.
    			if (ids.size()<2)
    				continue;
    			
    			StringBuilder sbLine = new StringBuilder();
    			String[] cstrType = {"BOND ", "ANGL ", "TORS "};
    			sbLine.append(SpartanConstants.INDENT);
    			sbLine.append(cstrType[cstr.getNumberOfIDs()-2]);
    			for (int i=0; i<4; i++)
    			{
    				if (i<cstr.getNumberOfIDs())
    					sbLine.append(formatConstrainIds(ids.get(i)+1));
    				else
    					break;
    			}
    			if (cstr.hasValue())
    				sbLine.append(formatConstrainValue(cstr.getValue()));
    			else if (cstr.hasCurrentValue())
    				sbLine.append(formatConstrainValue(cstr.getCurrentValue()));
    			sb.append(sbLine.toString()).append(NL);
    		}
            sb.append(SpartanConstants.CSTRDIREND).append(NL);

            ensureNoKeywords(cstrDir);
            ensureNoSubdirectives(cstrDir);
    	}
    	
    	Directive dynDir = job.getDirective(SpartanConstants.DIRDYNONSTRAINTS);
    	if (dynDir != null)
    	{
    		ensureSingleDirectiveData(dynDir);
    		
    		sb.append(SpartanConstants.DYNCDIROPN).append(NL);
    		
    		ConstraintsSet cs = (ConstraintsSet) dynDir
    				.getAllDirectiveDataBlocks().get(0).getValue();

    		// We need to remove any attempt to add multiple dynamic constraints
    		// acting on the same internal coordinate. E.g., [1,2,3,4] and 
    		// [6,2,3,7] are both acting on bond 2-3. We keep only one of them.
    		Set<List<Integer>> unique = new HashSet<List<Integer>>();
    		for (Iterator<Constraint> iter = cs.iterator(); iter.hasNext(); ) 
    		{
    			Constraint cstr = iter.next();
    			List<Integer> ids = cstr.getAtomIDs();
    			
    			// Here we get rid of redundant dynamic constraints
    			if (!isNewDynamicConstraint(ids, unique))
    				continue;
    			
    			StringBuilder sbLine = new StringBuilder();
    			sbLine.append(SpartanConstants.INDENT);
    			for (int i=0; i<4; i++)
    			{
    				if (i<cstr.getNumberOfIDs())
    					sbLine.append(formatConstrainIds(ids.get(i)+1));
    				else
    					sbLine.append(formatConstrainIds(0));
    			}
    			if (!cstr.getSuffix().isBlank())
    			{
    				sbLine.append(" ").append(cstr.getSuffix());
    			}
    			sb.append(sbLine.toString()).append(NL);
    		}
            sb.append(SpartanConstants.DYNCDIREND).append(NL);

            ensureNoKeywords(dynDir);
            ensureNoSubdirectives(dynDir);
    	}
    	
    	Directive atmLabelDir = job.getDirective(SpartanConstants.DIRATOMLABELS);
    	if (atmLabelDir != null)
    	{
    		ensureSingleDirectiveData(atmLabelDir);
    		
    		sb.append(SpartanConstants.ATMLABELSOPN).append(NL);
    		
    		TextBlock labels = (TextBlock) atmLabelDir
    				.getAllDirectiveDataBlocks().get(0).getValue();
    		for (String label : labels)
    			sb.append(SpartanConstants.INDENT + "\"" + label + "\"").append(NL);
            sb.append(SpartanConstants.ATMLABELSEND).append(NL);

            ensureNoKeywords(atmLabelDir);
            ensureNoSubdirectives(atmLabelDir);
    	}
    	

    	// NB: some versions of Spartan require this directive, even if empty.
		sb.append(SpartanConstants.PRODIROPN).append(NL);
    	Directive propinDir = job.getDirective(SpartanConstants.DIRPROPIN);
    	if (propinDir != null)
    	{
    		for (DirectiveData dd : propinDir.getAllDirectiveDataBlocks())
    		{
    			if (dd.getType()!=NamedDataType.TEXTBLOCK)
    			{
    				throw new IllegalArgumentException("Unexpected type of "
    						+ "data in directive '" 
    						+ SpartanConstants.DIRPROPIN + "'. "
    						+ "Check directive data '" + dd.getName() + "'.");
    			}
    			TextBlock txt = (TextBlock) dd.getValue();
	    		sb.append(StringUtils.mergeListToString(txt,NL));
    		}
            ensureNoKeywords(atmLabelDir);
            ensureNoSubdirectives(atmLabelDir);
    	}
    	sb.append(SpartanConstants.PRODIREND).append(NL);
    	
        return sb;
    }

//------------------------------------------------------------------------------
    
    private boolean isNewDynamicConstraint(List<Integer> ids, 
    		Set<List<Integer>> unique) 
    {
    	List<Integer> candidate = new ArrayList<Integer>();
    	switch (ids.size())
    	{
    		case 1:
    			candidate.add(ids.get(0));
    			break;
    			
	    	case 2:
	        	candidate.add(ids.get(0));
	        	candidate.add(ids.get(1));
	        	Collections.sort(candidate);
	    		break;
	
	    	case 3:
	    		if (ids.get(0)>ids.get(2))
	    		{
	            	candidate.add(ids.get(2));
	            	candidate.add(ids.get(1));
	            	candidate.add(ids.get(0));
	    		} else {
	            	candidate.add(ids.get(0));
	            	candidate.add(ids.get(1));
	            	candidate.add(ids.get(2));
	    		}
	    		break;
	    		
	    	case 4:
	    		// NB: the identity of the first and last index is irrelevant
	    		// Therefore, we set first and last index to -1.
	    		if (ids.get(1)>ids.get(2))
	    		{
	            	candidate.add(-1);
	            	candidate.add(ids.get(2));
	            	candidate.add(ids.get(1));
	            	candidate.add(-1);
	    		} else {
	            	candidate.add(-1);
	            	candidate.add(ids.get(2));
	            	candidate.add(ids.get(1));
	            	candidate.add(-1);
	    		}
	    		break;
    	}
    	return unique.add(candidate);
	}

//------------------------------------------------------------------------------
    
	private Object formatConstrainIds(int i) 
    {
    	return String.format(Locale.ENGLISH, "%5d ", i);
	}
    
//------------------------------------------------------------------------------
    
    private Object formatConstrainValue(double v) 
    {
    	return String.format(Locale.ENGLISH, "%10.6f ", v);
	}

//------------------------------------------------------------------------------
    
	private void ensureNoKeywords(Directive d)
    {
    	if (d.getAllKeywords().size()>0)
		{
			throw new IllegalArgumentException("Directive '" 
					+ d.getName() + "' contains unexpected keywords. "
					+ "Check your input!");
		}
    }
    
//------------------------------------------------------------------------------
    
    private void ensureNoDirectiveData(Directive d)
    {
		if (d.getAllDirectiveDataBlocks().size()>0)
		{
			throw new IllegalArgumentException("Directive '" 
					+ d.getName() + "' contains unexpected "
					+ "data. Check your input!");
		}
    }
    
//------------------------------------------------------------------------------
    
    private void ensureNoSubdirectives(Directive d)
    {
		if (d.getAllSubDirectives().size()>0)
		{
			throw new IllegalArgumentException("Directive '" 
					+ d.getName() + "' contains unexpected "
					+ "subdirectives. Check your input!");
		}
    }
    
//------------------------------------------------------------------------------
    
    private void ensureSingleDirectiveData(Directive d)
    {
    	int sz = d.getAllDirectiveDataBlocks().size();
    	if (sz==0)
		{
			throw new IllegalArgumentException("Directive '" 
					+ d.getName() + "' contains no data. Check your input!");
		}
		if (sz>1)
		{
			throw new IllegalArgumentException("Directive '" 
					+ d.getName() + "' should contain one data collection, but "
					+ "contains " + sz + ". Check your input!");
		}
    }   
    
//------------------------------------------------------------------------------
	
  	/**
  	 * {@inheritDoc}
  	 * <br>
  	 * Spartan requires a folder structure containing "flag files" 
  	 * that define what the
  	 * folders are in Spartan convention, and "secondary input files" 
  	 * providing additional details
  	 * to the main input files (e.g., cell parameters, properties). This method
  	 * generates such folder structure, 
  	 * the "flag files", 
  	 * and the "secondary input files", but does
  	 *  not create the main input file, which is always named 
  	 *  <code>input</code>.
  	 *  <br>
  	 *  WARNING: although this method takes a list of structures, so far Spartan
  	 *  input can only pertain a single structure, so we will consider only the
  	 *  first of the structures provided.
  	 */
  	@Override
  	protected File manageOutputFileStructure(List<IAtomContainer> mols,
  			File output) 
  	{
  		IAtomContainer mol = mols.get(0);
  		
        String molName = MolecularUtils.getNameOrID(mol); 
        
        String sep = File.separator;
        
        // Create folder tree
        File jobFolder = output;
        File molSpecFolder = new File(jobFolder + sep + molName);
        if (!molSpecFolder.mkdirs())
        {
            Terminator.withMsgAndStatus("ERROR! Unable to create folder '"
            		+ molSpecFolder+ "'.",-1);
        }
        
        //Create Spartan's flag files
        IOtools.writeTXTAppend(new File(jobFolder + sep + 
        		SpartanConstants.ROOTFLGFILENAME), 
        		SpartanConstants.ROOTFLGFILEHEAD, false);
        IOtools. writeTXTAppend(new File(molSpecFolder + sep +
        		SpartanConstants.MOLFLGFILENAME),
        		SpartanConstants.MOLFLGFILEHEAD, false);
        
        //Create cell file
        IOtools.writeTXTAppend(new File(molSpecFolder + sep +
        		SpartanConstants.CELLFILENAME),
        		getCellDirective(mol), false);  		
        
        return new File(molSpecFolder + sep + SpartanConstants.INPUTFILENAME);
  	}
  	
//------------------------------------------------------------------------------

    /**
     * Collects all molecular properties and reports then in the form of a 
     * list of strings formatted for Spartan's CELL file.
     * @param mol the molecular representation containing the properties to
     * be formatted.
     */

    private ArrayList<String> getCellDirective(IAtomContainer mol)
    {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(SpartanConstants.CELLOPN);
        for (Map.Entry<Object, Object> p : mol.getProperties().entrySet())
        {
            lines.add(SpartanConstants.INDENT + p.getKey().toString() + "=" 
            		+ p.getValue().toString());
        }
        lines.add(SpartanConstants.CELLEND);
        return lines;
    }
    
//------------------------------------------------------------------------------

}
