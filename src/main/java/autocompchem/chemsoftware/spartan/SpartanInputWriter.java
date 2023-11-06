package autocompchem.chemsoftware.spartan;

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
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
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
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software Spartan.
 *
 * @author Marco Foscato
 */

public class SpartanInputWriter extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTSPARTAN)));
    
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
    public Set<TaskID> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<TaskID>(
             Arrays.asList(TaskID.PREPAREINPUTSPARTAN)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Object... args) {
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
    protected ArrayList<String> getTextForInput(CompChemJob job)
    {
    	if (job.getNumberOfSteps()>0)
    		throw new IllegalArgumentException("Spartan jobs can not contain "
    				+ "steps! Found Job with " + job.getNumberOfSteps() 
    				+ " steps.");
        ArrayList<String> lines= new ArrayList<String>();
        
        Directive kwDir = job.getDirective(SpartanConstants.DIRKEYWORDS);
    	if (kwDir != null)
    	{
    		//keywords are written in a single line
    		StringBuilder sbKw = new StringBuilder();
    		for (Keyword k : kwDir.getAllKeywords())
    		{
    			sbKw.append(k.toString("=")).append(" ");
    		}
    		lines.add(sbKw.toString());

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
    		lines.add(sbKw.toString());
    		
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
    		lines.add(sbKw.toString());

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
                StringBuilder sb = new StringBuilder();
                sb.append(String.format(Locale.ENGLISH, "%3d", 
                		atm.getAtomicNumber())).append(" ");
                sb.append(String.format(Locale.ENGLISH, " %13.8f", p3d.x));
                sb.append(" ");
                sb.append(String.format(Locale.ENGLISH, " %13.8f", p3d.y));
                sb.append(" ");
                sb.append(String.format(Locale.ENGLISH, " %13.8f", p3d.z));
                lines.add(sb.toString());
            }
            lines.add(SpartanConstants.XYZEND);

            ensureNoKeywords(xyzDir);
            ensureNoSubdirectives(xyzDir);
    		
    		// Then the topology
            lines.add(SpartanConstants.TOPOOPN);
            Iterator<IAtom> it = mol.atoms().iterator();
            while (it.hasNext())
            {
                StringBuilder sb = new StringBuilder();
                for (int i=0; i<12; i++)
                {
                    if (!it.hasNext())
                    {
                        break;
                    }
                    IAtom atm = it.next();
                    sb.append(String.format(Locale.ENGLISH, "%5d", 
                    		-mol.getConnectedBondsCount(atm)));
                }
                lines.add(sb.toString());
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
                    if (verbosity > 0)
                    {
                        System.out.println("WARNING! Unknown bond order between "
                            + MolecularUtils.getAtomRef(bnd.getAtom(0),mol)+" and "
                            + MolecularUtils.getAtomRef(bnd.getAtom(1),mol)
                            + " treated as single bond.");
                    }
                    bo = 1;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(String.format(Locale.ENGLISH, "%5d", iA+1)); //1-based ID
                sb.append(String.format(Locale.ENGLISH, "%5d", iB+1)); //1-based ID
                sb.append(String.format(Locale.ENGLISH, "%5d", bo));
                lines.add(sb.toString());
            }
            lines.add(SpartanConstants.TOPOEND);
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
    		lines.add(SpartanConstants.COMMOPN);
    		for (DirectiveData dd : txtDir.getAllDirectiveDataBlocks())
            {
    			lines.add(SpartanConstants.INDENT + dd.getValueAsString());
            }
            lines.add(SpartanConstants.COMMEND);

            ensureNoKeywords(txtDir);
            ensureNoSubdirectives(txtDir);
    	}
    	
    	// WARNING: Omitting Cell definitions directive.
    	
    	Directive frzDir = job.getDirective(SpartanConstants.DIRFROZEN);
    	if (frzDir != null)
    	{
    		ensureSingleDirectiveData(frzDir);
    		
    		lines.add(SpartanConstants.FREEZEOPN);

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
			lines.add(sbFrz.toString());
            lines.add(SpartanConstants.FREEZEEND);

            ensureNoKeywords(frzDir);
            ensureNoSubdirectives(frzDir);
    	}
    	
    	Directive confDir = job.getDirective(SpartanConstants.DIRCONFORMER);
    	if (confDir != null)
    	{
    		ensureSingleDirectiveData(confDir);
    		
    		lines.add(SpartanConstants.CONFDIROPN);
    		ConformationalSpace cs = (ConformationalSpace) confDir
    				.getAllDirectiveDataBlocks().get(0).getValue();
    		for (ConformationalCoordinate coord : cs)
            {
    			//TODO-gg fixme
    			lines.add(SpartanConstants.INDENT 
    					+ coord.getAtomIDsAsString(true, "%5d")
    					+ String.format(Locale.ENGLISH, "%5d", coord.getFold()));
            }
            lines.add(SpartanConstants.CONFDIREND);
            
            ensureNoKeywords(confDir);
            ensureNoSubdirectives(confDir);
    	}
    	
    	Directive cstrDir = job.getDirective(SpartanConstants.DIRCONSTRAINTS);
    	if (cstrDir != null)
    	{
    		ensureSingleDirectiveData(cstrDir);
    		
    		lines.add(SpartanConstants.CSTRDIROPN);
    		
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
    			lines.add(sbLine.toString());
    		}
            lines.add(SpartanConstants.CSTRDIREND);

            ensureNoKeywords(cstrDir);
            ensureNoSubdirectives(cstrDir);
    	}
    	
    	Directive dynDir = job.getDirective(SpartanConstants.DIRDYNONSTRAINTS);
    	if (dynDir != null)
    	{
    		ensureSingleDirectiveData(dynDir);
    		
    		lines.add(SpartanConstants.DYNCDIROPN);
    		
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
    			lines.add(sbLine.toString());
    		}
            lines.add(SpartanConstants.DYNCDIREND);

            ensureNoKeywords(dynDir);
            ensureNoSubdirectives(dynDir);
    	}
    	
    	Directive atmLabelDir = job.getDirective(SpartanConstants.DIRATOMLABELS);
    	if (atmLabelDir != null)
    	{
    		ensureSingleDirectiveData(atmLabelDir);
    		
    		lines.add(SpartanConstants.ATMLABELSOPN);
    		
    		TextBlock labels = (TextBlock) atmLabelDir
    				.getAllDirectiveDataBlocks().get(0).getValue();
    		for (String label : labels)
    			lines.add(SpartanConstants.INDENT + "\"" + label + "\"");
            lines.add(SpartanConstants.ATMLABELSEND);

            ensureNoKeywords(atmLabelDir);
            ensureNoSubdirectives(atmLabelDir);
    	}
    	

    	// NB: some versions of Spartan require this directive, even if empty.
		lines.add(SpartanConstants.PRODIROPN);
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
	    		lines.addAll(txt);
    		}
            ensureNoKeywords(atmLabelDir);
            ensureNoSubdirectives(atmLabelDir);
    	}
    	lines.add(SpartanConstants.PRODIREND);
    	
        return lines;
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
