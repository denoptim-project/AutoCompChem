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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.chemsoftware.gaussian.GaussianConstants;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.CenterBasisSet;
import autocompchem.modeling.basisset.ECPShell;
import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.basisset.Shell;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.conformation.ConformationalCoordinate;
import autocompchem.molecule.conformation.ConformationalSpace;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixAtom;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software Spartan.
 *
 * @author Marco Foscato
 */

public class SpartanInputWriter2 extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTSPARTAN2)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Spartan conventions.
     */

    public SpartanInputWriter2() 
    {
    	inpExtrension = "";
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
    	setDirectiveDataIfNotAlreadyThere(ccj, SpartanConstants.DIRCART, 
    			dd.getName(), dd);
    	
    	setKeywordIfNotAlreadyThere(ccj, SpartanConstants.DIRTITLE, "title", 
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
    			if (k.isLoud())
    				sbKw.append(k.getName()).append("=").append(
    						k.getValueAsString()).append(" ");
    			else
    				sbKw.append(k.getValueAsString()).append(" ");
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
    			if (k.isLoud())
    				sbKw.append(k.getName()).append("=").append(
    						k.getValueAsString()).append(" ");
    			else
    				sbKw.append(k.getValueAsString()).append(" ");
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
    		sbKw.append(csDir.getKeyword(
    				SpartanConstants.KWCHARGE).getValueAsString());
    		sbKw.append(" ").append(csDir.getKeyword(
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
    		ConstraintsSet cs = (ConstraintsSet) frzDir
    				.getAllDirectiveDataBlocks().get(0).getValue();

            StringBuilder sbFrz = new StringBuilder();
            sbFrz.append(SpartanConstants.INDENT);
            int indexCounter = 0;
    		for (Constraint c : cs.getConstrainsWithType(
    				ConstraintType.FROZENATM))
            {
    			indexCounter++;
    			if (indexCounter>12)
    			{
    				indexCounter = 0;
    				sbFrz.append(NL).append(SpartanConstants.INDENT);
    			}
    			sbFrz.append(String.format(Locale.ENGLISH, "%5d",
    					c.getAtomIDs()[0]+1));
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
    		for (ConformationalCoordinate coord : cs.coords())
            {
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
    			int[] ids = cstr.getAtomIDs();
    			
    			// Frozen atoms are specified in a dedicated directive.
    			if (ids.length<2)
    				continue;
    			
    			StringBuilder sbLine = new StringBuilder();
    			String[] cstrType = {"BOND ", "ANGL ", "TORS "};
    			sbLine.append(SpartanConstants.INDENT);
    			sbLine.append(cstrType[cstr.getNumberOfIDs()-2]);
    			for (int i=0; i<4; i++)
    			{
    				if (i<cstr.getNumberOfIDs())
    					sbLine.append(formatConstrainIds(ids[i]+1));
    				else
    					break;
    			}
    			sbLine.append(formatConstrainValue(cstr.getValue()));
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
    			int[] ids = cstr.getAtomIDs();
    			
    			// Here we get rid of redundant dynamic constraints
    			if (!isNewDynamicConstraint(ids, unique))
    				continue;
    			
    			StringBuilder sbLine = new StringBuilder();
    			sbLine.append(SpartanConstants.INDENT);
    			for (int i=0; i<4; i++)
    			{
    				if (i<cstr.getNumberOfIDs())
    					sbLine.append(formatConstrainIds(ids[i]+1));
    				else
    					sbLine.append(formatConstrainIds(0));
    			}
    			if (cstr.hasOpt())
    			{
    				sbLine.append(" ").append(cstr.getOpt());
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
    
    private boolean isNewDynamicConstraint(int[] ids, Set<List<Integer>> unique) 
    {
    	List<Integer> candidate = new ArrayList<Integer>();
    	switch (ids.length)
    	{
    		case 1:
    			candidate.add(ids[0]);
    			break;
    			
	    	case 2:
	        	candidate.add(ids[0]);
	        	candidate.add(ids[1]);
	        	Collections.sort(candidate);
	    		break;
	
	    	case 3:
	    		if (ids[0]>ids[2])
	    		{
	            	candidate.add(ids[2]);
	            	candidate.add(ids[1]);
	            	candidate.add(ids[0]);
	    		} else {
	            	candidate.add(ids[0]);
	            	candidate.add(ids[1]);
	            	candidate.add(ids[2]);
	    		}
	    		break;
	    		
	    	case 4:
	    		// NB: the identity of the first and last index is irrelevant
	    		// Therefore, we set first and last index to -1.
	    		if (ids[1]>ids[2])
	    		{
	            	candidate.add(-1);
	            	candidate.add(ids[2]);
	            	candidate.add(ids[1]);
	            	candidate.add(-1);
	    		} else {
	            	candidate.add(-1);
	            	candidate.add(ids[2]);
	            	candidate.add(ids[1]);
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

}