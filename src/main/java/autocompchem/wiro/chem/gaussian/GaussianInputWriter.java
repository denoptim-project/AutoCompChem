package autocompchem.wiro.chem.gaussian;

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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomUtils;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.CenterBasisSet;
import autocompchem.modeling.basisset.ECPShell;
import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.basisset.Shell;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixAtom;
import autocompchem.run.Job;
import autocompchem.utils.StringUtils;
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
 * Writes input files for software Gaussian.
 *
 * @author Marco Foscato
 */

public class GaussianInputWriter extends ChemSoftInputWriter
{
    /**
     * String defining the task of preparing input files for Gaussian
     */
    public static final String PREPAREINPUTGAUSSIANTASKNAME = 
    		"prepareInputGaussian";

    /**
     * Task about preparing input files for Gaussian
     */
    public static final Task PREPAREINPUTGAUSSIANTASK;
    static {
    	PREPAREINPUTGAUSSIANTASK = Task.make(PREPAREINPUTGAUSSIANTASKNAME);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Gaussian conventions.
     */

    public GaussianInputWriter() 
    {
		inpExtrension = GaussianConstants.GAUINPEXTENSION;
	}
  
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PREPAREINPUTGAUSSIANTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new GaussianInputWriter();
    }

//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * In Gaussian the charge is defined in a {@link Keyword} named 
     * {@value GaussianConstants#MSCHARGEKEY} of the 
     * {@value GaussianConstants#DIRECTIVEMOLSPEC} {@link Directive}.
     * Moreover, the specification of the charge cannot be omitted, so the
     * <code>omitIfPossible</code> parameter does not take effect.
     */
    @Override
    protected void setChargeIfUnset(CompChemJob ccj, String charge, 
    		boolean omitIfPossible)
    {
    	setKeywordIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVEMOLSPEC, 
    			GaussianConstants.MSCHARGEKEY, charge);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * In Gaussian the spin multiplicity is defined in a {@link Keyword} named 
     * {@value GaussianConstants#MSSPINMLTKEY} of the 
     * {@value GaussianConstants#DIRECTIVEMOLSPEC} {@link Directive}.
     * Moreover, the specification of the spin cannot be omitted, so the
     * <code>omitIfPossible</code> parameter does not take effect.
     */
    @Override
    protected void setSpinMultiplicityIfUnset(CompChemJob ccj, String sm, 
    		boolean omitIfPossible)
    {
    	setKeywordIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVEMOLSPEC,
    			GaussianConstants.MSSPINMLTKEY, sm);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * 
     * In Gaussian, a chemical system is defined in the {@link DirectiveData} of
     * a {@value GaussianConstants#DIRECTIVEMOLSPEC} {@link Directive}. 
     * If multiple systems (i.e., more than one atom container) are given, they
     * are placed each in a dedicated 
     * {@value GaussianConstants#DIRECTIVEMOLSPEC} {@link Directive} in the 
     * same order as they are given.
     */
    protected void setChemicalSystem(CompChemJob ccj, List<IAtomContainer> iacs)
    {
    	if (!needsGeometry(ccj))
    		return;
    	
    	CompChemJob ccjToAlter = ccj;
    	if (ccj.getNumberOfSteps()>0)
    	{
    		ccjToAlter = (CompChemJob) ccj.getStep(0);
    	}
		if (iacs.size()==1)
		{
			DirectiveData dd = new DirectiveData(
					GaussianConstants.DIRECTIVEMOLSPEC);
	    	dd.setValue(iacs.get(0));
    		addNewDirectiveData(ccjToAlter, 
        			GaussianConstants.DIRECTIVEMOLSPEC, dd);
		} else {
			// Remove previously existing MolSpec (can have charge and spin)
	    	DirComponentAddress molDirAdrs = new DirComponentAddress();
	    	molDirAdrs.addStep(GaussianConstants.DIRECTIVEMOLSPEC, 
	    			DirectiveComponentType.DIRECTIVE);
			ccjToAlter.removeDirectiveComponent(molDirAdrs);

			// Remove previously existing Title (can have charge and spin)
	    	DirComponentAddress title = new DirComponentAddress();
	    	title.addStep(GaussianConstants.DIRECTIVETITLE, 
	    			DirectiveComponentType.DIRECTIVE);
			ccjToAlter.removeDirectiveComponent(title);
			
			// Generate directives for each geometry
    		for (IAtomContainer iac : iacs)
    		{
    	    	Directive titleDir = new Directive(
    	    			GaussianConstants.DIRECTIVETITLE);
    	    	titleDir.setKeyword(new Keyword("title", false, iac.getTitle()));
    	    	ccjToAlter.addDirective(titleDir);
    	    	
    			DirectiveData dd = new DirectiveData(
    					GaussianConstants.DIRECTIVEMOLSPEC);
    	    	dd.setValue(iac);
    	    	Directive molSpecDir = new Directive(
    	    			GaussianConstants.DIRECTIVEMOLSPEC);
    	    	molSpecDir.addDirectiveData(dd);
    			Integer charge = getChargeFromMol(iac);
    			if (charge != null)
    			{
    				molSpecDir.addKeyword(new Keyword(
    						GaussianConstants.MSCHARGEKEY, false, charge));
    			}
    			Integer sm = getSpinMultiplicityFromMol(iac);
    			if (sm != null)
    			{
    				molSpecDir.addKeyword(new Keyword(
    						GaussianConstants.MSSPINMLTKEY, false, sm));
    			}
    	    	ccjToAlter.addDirective(molSpecDir);
    		}
		}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if a job needs to specify the geometry of the chemical system in
     * its input file or if it does not because it either takes it from the 
     * checkpoint file or there is a task adding a geometry to the molecule
     * specification section. We assume that any {@link DirectiveData} found in
     * the {@value GaussianConstants#DIRECTIVEMOLSPEC} {@link Directive} is
     * about defining the geometry. Therefore, the existence of any such
     * {@link DirectiveData} is sufficient to make this method return 
     * <code>false</code>.
     * @param ccj the job to analyze
     * @return <code>true</code> if the geometry should be in the input file.
     */
    public static boolean needsGeometry(CompChemJob ccj)
    {
    	if (ccj.getNumberOfSteps()>0)
    	{
			return needsGeometry((CompChemJob) ccj.getStep(0));
    	} else {
    		Directive route = ccj.getDirective(GaussianConstants.DIRECTIVEROUTE);
    		if (route!=null && !needsGeometry(route))
    			return false;
    		Directive geom = ccj.getDirective(GaussianConstants.DIRECTIVEMOLSPEC);
    		//WARNING: we assume that any DirectiveData
    		if (geom!=null && geom.getAllDirectiveDataBlocks().size()>0)
    			return false;
    	}
    	return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Looks into the route section for keywords indicating the the geometry is 
     * taken from the checkpoint file, in one of the many ways, so no geometry
     * needs to be specified in the molecular section of the input.
     * @param routeDir
     * @return <code>true</code> if a geometry needs to be given in the 
     * molecular specification section.
     */
    private static boolean needsGeometry(Directive routeDir)
    {
    	if (routeDir!=null)
    	{
	    	Keyword geomKey = routeDir.getFirstKeyword(GaussianConstants.GAUKEYGEOM);
			if (geomKey!=null)
			{
				String value = geomKey.getValueAsString().toUpperCase();
				if (value.startsWith(GaussianConstants.GAUKEYGEOMCHK) ||
						value.startsWith(GaussianConstants.GAUKEYGEOMCHECK) ||
						value.startsWith(GaussianConstants.GAUKEYGEOMALLCHK) ||
						value.startsWith(GaussianConstants.GAUKEYGEOMSTEP) ||
						value.startsWith(GaussianConstants.GAUKEYGEOMNGEOM) ||
						value.startsWith(GaussianConstants.GAUKEYGEOMMOD))
		        {
					// Geometry will be taken from checkpoint file.
					return false;
		        }
			}
    	}
		return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     */
    protected void setSystemSpecificNames(CompChemJob ccj)
    {
    	File pathnameRoot = new File(ccJobInputNameRoot);
    	addNewKeyword(ccj, GaussianConstants.DIRECTIVELINK0,
    			"chk", true, pathnameRoot.getName());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     */
    @Override
	public StringBuilder getTextForInput(CompChemJob job)
    {
    	StringBuilder sb = new StringBuilder();
        if (job.getNumberOfSteps()>0)
        {
	        for (int step = 0; step<job.getNumberOfSteps(); step++)
	        {
	        	CompChemJob stepCcj = (CompChemJob)job.getStep(step);
	            if (step != 0)
	            {
	            	sb.append(GaussianConstants.STEPSEPARATOR).append(NL);
	            }
	            sb.append(getTextForStep(stepCcj));
	        }
        } else {
        	sb.append(getTextForStep(job));
        }
        return sb;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the method the encodes the syntax of the Gaussian input file for 
     * a single step job (i.e., the given {@link Job} is not expected to contain
     * embedded {@link Job}s). 
     * Here we translate all comp.chem.-software agnostic components to 
     * Gaussian-specific format.
     * @param step the Gaussian job step object.
     * @return the list of lines for the input file
     */
    
    private StringBuilder getTextForStep(CompChemJob step)
    {	
    	StringBuilder sb = new StringBuilder();
    	//
    	// Building lines of Link0 section
    	// 
    	
    	Directive lnkDir = step.getDirective(GaussianConstants.DIRECTIVELINK0);
    	if (lnkDir != null)
    	{
    		// We expect only keywords
    		for (Keyword k : lnkDir.getAllKeywords())
    		{
    			sb.append("%"+k.toString("=")).append(NL);
    		}
    		if (lnkDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Link0 section of Gaussian "
    					+ "job contains data blocks but only keywords are "
    					+ "expected. Check your input!");
    		}
    		if (lnkDir.getAllSubDirectives().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Link0 section of Gaussian "
    					+ "job contains subdirectives but only keywords are "
    					+ "expected. Check your input!");
    		}
    	} // No default link0 section
    	
    	//
    	// Building lines of Route section
    	//
    	Directive rouDir = step.getDirective(GaussianConstants.DIRECTIVEROUTE);
    	if (rouDir != null)
    	{
    		// First the special keywords
    		String firstLine = "";
    		Keyword pKey = rouDir.getFirstKeyword(GaussianConstants.KEYPRINT);
    		if (pKey!=null)
    		{
    			firstLine = "#" + pKey.getValueAsString();
    		}  else {
    			firstLine = "#P";
            }
    		Keyword modKey = rouDir.getFirstKeyword(GaussianConstants.KEYMODELMETHOD);
    		if (modKey!=null)
    		{
    			firstLine = firstLine + " " + modKey.getValueAsString() + "/";
    		} else {
    			firstLine = firstLine + " ";
    		}
    		Keyword bsKey = rouDir.getFirstKeyword(GaussianConstants.KEYMODELBASISET);
    		if (bsKey!=null)
    		{
    			// space has been already added, if needed
    			firstLine = firstLine + bsKey.getValueAsString();
    		}
    		sb.append(firstLine).append(NL);
    		Keyword jtKey = rouDir.getFirstKeyword(GaussianConstants.KEYJOBTYPE);
    		if (jtKey!=null)
    		{
    			sb.append("# " + jtKey.getValueAsString()).append(NL);
    		}
    		
    		// All other keywords
    		for (Keyword k : rouDir.getAllKeywords())
    		{
    			if (GaussianConstants.SPECIALKEYWORDS.contains(k.getName()))
    				continue;

    			sb.append("# "+k.toString("=")).append(NL);
    		}
    		for (Directive subDir : rouDir.getAllSubDirectives())
    		{
        		String directiveLine = "# ";
    			// Gaussian uses only one nesting level!
    			if (subDir.getAllSubDirectives().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Route "
        					+ "section of Gaussian job contains nested "
        					+ "sub-sub-directive/s. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			if (subDir.getAllDirectiveDataBlocks().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Route "
        					+ "section of Gaussian job contains directive "
        					+ "data blocks. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			directiveLine = directiveLine + subDir.getName() + "=(";
    			boolean first = true;
    			for (Keyword k : subDir.getAllKeywords())
    			{
    				String keyStr = k.toString("=");
    				if (first)
    				{
    					directiveLine = directiveLine + keyStr;
    				} else {
    					directiveLine = directiveLine + "," + keyStr;
    				}
    				first = false;
    			}
    			directiveLine = directiveLine + ")";
    			sb.append(directiveLine).append(NL);
    		}
    		
    		if (rouDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Route section of Gaussian "
    					+ "job contains data blocks but only keywords are "
    					+ "expected. Check your input!");
    		}
    	} else {
    		sb.append("#P").append(NL);
    	}
    	sb.append(NL); // Empty line terminating route section
		
		// Title and MolSpec and ModRedundant directive go hand in hand: 
		// if there are multiple geometries there can be also multiple titles
		// and multiple mod redundant sections
    	DirComponentAddress titleAddrs = new DirComponentAddress();
    	titleAddrs.addStep(GaussianConstants.DIRECTIVETITLE, 
    			DirectiveComponentType.DIRECTIVE);
    	List<IDirectiveComponent> titleDirs = step.getDirectiveComponents(
    			titleAddrs);
    	
    	DirComponentAddress molSpecAddrs = new DirComponentAddress();
    	molSpecAddrs.addStep(GaussianConstants.DIRECTIVEMOLSPEC, 
    			DirectiveComponentType.DIRECTIVE);
    	List<IDirectiveComponent> molSpecDirs = step.getDirectiveComponents(
    			molSpecAddrs);
    	
    	DirComponentAddress modRedAddrs = new DirComponentAddress();
    	modRedAddrs.addStep(GaussianConstants.DIRECTIVEOPTS, 
    			DirectiveComponentType.DIRECTIVE);
    	modRedAddrs.addStep(GaussianConstants.DDMODREDUNDANT, 
    			DirectiveComponentType.DIRECTIVEDATA);
    	List<IDirectiveComponent> modRedDirs = step.getDirectiveComponents(
    			modRedAddrs);
    	
    	int numBlocks = Math.max(titleAddrs.size(), molSpecDirs.size());
    	for (int blockId=0; blockId<numBlocks; blockId++)
    	{
			//
			// Building line of Title section (well, one line plus blank line)
			//
	    	if (blockId < titleDirs.size())
	    	{
	    		Directive titDir = (Directive) titleDirs.get(blockId);
	    		// We expect only ONE keywords
	    		if (titDir.getAllKeywords().size()>1)
	    		{
	    			throw new IllegalArgumentException(
	    					"Directive for title section of Gaussian "
	    					+ "job contains more than one keyword. "
	    					+ "Check your input!");
	    		}
	    		Keyword k = titDir.getAllKeywords().get(0);
	    		sb.append(k.getValueAsString()).append(NL);
	    		
	    		if (titDir.getAllDirectiveDataBlocks().size()>0)
	    		{
	    			throw new IllegalArgumentException(
	    					"Directive for title section of Gaussian "
	    					+ "job contains data blocks but only keywords are "
	    					+ "expected. Check your input!");
	    		}
	    		if (titDir.getAllSubDirectives().size()>0)
	    		{
	    			throw new IllegalArgumentException(
	    					"Directive for title section of Gaussian "
	    					+ "job contains subdirectives but only keywords are "
	    					+ "expected. Check your input!");
	    		}
	    	} else {
				sb.append("No title").append(NL);
	    	}
			sb.append("").append(NL); // Empty line terminating title section
		
			
			//
			// Building lines of Molecular Specification Section
			//
			if (blockId > molSpecDirs.size()-1)
	    	{
				throw new IllegalArgumentException(
    					"Found " + titleDirs.size() + " title directives but "
    					+ "only " + molSpecDirs.size() + " molecular "
    					+ "specification directives. Check your input!");
	    	}
			Directive molDir = (Directive) molSpecDirs.get(blockId);
	    	if (molDir != null)
	    	{
	    		// We expect TWO keywords
	    		if (molDir.getAllKeywords().size()!=2)
	    		{
	    			throw new IllegalArgumentException(
	    					"Directive for molecular specification "
	    					+ "section of Gaussian "
	    					+ "job contains N!=2 keywords. "
	    					+ "Check your input!");
	    		}
	    		Keyword kCharge = molDir.getFirstKeyword(
	    				GaussianConstants.MSCHARGEKEY);
		    	Keyword kSpinMult = molDir.getFirstKeyword(
		    			GaussianConstants.MSSPINMLTKEY);
	    		sb.append(kCharge.getValueAsString() + " " 
		    	+ kSpinMult.getValueAsString()).append(NL);
	    		
	    		if (molDir.getAllDirectiveDataBlocks().size()>0)
	    		{
	    			for (DirectiveData dd : molDir.getAllDirectiveDataBlocks())
	    			{
	    				switch (dd.getType())
	    	        	{
		    			case IATOMCONTAINER:
		    				IAtomContainer mol = (IAtomContainer) dd.getValue();
		    				for (IAtom atm : mol.atoms())
		    				{
		    					Point3d p3d = AtomUtils.getCoords3d(atm);
	
		    		    		String el = AtomUtils.getSymbolOrLabel(atm);
		    					sb.append(String.format(Locale.ENGLISH,"%s", el)

										+ String.format(Locale.ENGLISH, " " 
												+ formatCartCoord, p3d.x)
										+ String.format(Locale.ENGLISH, " " 
												+ formatCartCoord, p3d.y)
										+ String.format(Locale.ENGLISH, " " 
												+ formatCartCoord, p3d.z)).append(NL);
		    				}
		    				break;
		    			
		    			case ZMATRIX:
		    				ZMatrix zmat = (ZMatrix) dd.getValue();
		    				if (!zmat.hasConstants()) 
		    				{
		    					for (int i=0; i<zmat.getZAtomCount(); i++)
		    			        {
		    			        	ZMatrixAtom atm = zmat.getZAtom(i);
		    			        	StringBuilder sbAtom = new StringBuilder();
		    			        	sbAtom.append("  ");
		    			        	sbAtom.append(atm.getName()).append(" ");
		    			            int idI = atm.getIdRef(0);
		    			            int idJ = atm.getIdRef(1);
		    			            int idK = atm.getIdRef(2);
		    			            InternalCoord icI = atm.getIC(0);
		    			            InternalCoord icJ = atm.getIC(1);
		    			            InternalCoord icK = atm.getIC(2);
		    			            if (atm.getIdRef(0) != -1)
		    			            {
		    			            	sbAtom.append(idI + 1).append(" ");
		    			            	sbAtom.append(String.format(Locale.ENGLISH, 
		    			                		"%5.8f", 
		    			                		icI.getValue())).append(" ");
		    			                if (idJ != -1)
		    			                {
		    			                	sbAtom.append(idJ + 1).append(" ");
		    			                	sbAtom.append(String.format(
		    			                			Locale.ENGLISH,
		    			                    		"%5.8f", 
		    			                    		icJ.getValue())).append(" ");
		    			                    if (idK != -1)
		    			                    {
		    			                    	sbAtom.append(idK + 1).append(" ");
		    			                    	sbAtom.append(String.format(
		    			                        		Locale.ENGLISH,
		    			                        		"%5.8f", 
		    			                        		icK.getValue())).append(" ");
		    			                        if (!icK.getType().equals(
		    			                        		InternalCoord.NOTYPE))
		    			                        {
		    			                        	sbAtom.append(icK.getType());
		    			                        }
		    			                    }
		    			                }
		    			            }
		    			            sb.append(sbAtom.toString()).append(NL);
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
		    			                	sbAtom.append(
		    			                			icJ.getName()).append(" ");
		    			                    if (idK != -1)
		    			                    {
		    			                    	sbAtom.append(idK + 1).append(" ");
		    			                    	sbAtom.append(
		    			                    			icK.getName()).append(" ");
		    			                    	if (!icK.getType().equals(
		    			                        		InternalCoord.NOTYPE))
		    			                        {
		    			                    		sbAtom.append(icK.getType());
		    			                        }
		    			                    }
		    			                }
		    			            }
		    			            sb.append(sbAtom.toString()).append(NL);
		    			        }
		    			        // Then write the list of variables with initial value
		    			        sb.append("  Variables:").append(NL);
		    			        for (int i=0; i<zmat.getZAtomCount(); i++)
		    			        {
		    			        	ZMatrixAtom zatm = zmat.getZAtom(i);
		    			        	for (int iIC=0; iIC<zatm.getICsCount(); iIC++)
		    			        	{
		    			        		InternalCoord ic = zatm.getIC(iIC);
		    			        		if (ic.isConstant())
		    			        			continue;
		    			        		sb.append(ic.getName() + " " 
		    			        			+ String.format(Locale.ENGLISH, 
		    			        					" %5.8f", ic.getValue()))
		    			        		.append(NL);
		    			        	}
		    			        }
		    			        
		    			        // And finally write the list of constants
		    			        sb.append("  Constants:").append(NL);
		    			        for (int i=0; i<zmat.getZAtomCount(); i++)
		    			        {
		    			        	ZMatrixAtom zatm = zmat.getZAtom(i);
		    			        	for (int iIC=0; iIC<zatm.getICsCount(); iIC++)
		    			        	{
		    			        		InternalCoord ic = zatm.getIC(iIC);
		    			        		if (!ic.isConstant())
		    			        			continue;
		    			        		sb.append(ic.getName() + " " 
		    			        			+ String.format(Locale.ENGLISH,
		    			        					" %5.8f", ic.getValue()))
		    			        		.append(NL);
		    			        	}
		    			        }
		    				}
		    				break;
		    				
		    				default:
		    					throw new IllegalArgumentException("Unexpected "
		    							+ "type of DirectiveData. Check '"  
		    							+ dd.getName() + "' in directive '" 
		    							+ molDir.getName() + "'.");
	    	        	}
	    			}
	    		}
	    		if (molDir.getAllSubDirectives().size()>0)
	    		{
	    			throw new IllegalArgumentException(
	    					"Directive for molecular specification "
	    					+ "section of Gaussian "
	    					+ "job contains subdirectives but only keywords "
	    					+ "and ditrective data blocks are "
	    					+ "expected. Check your input!");
	    		}
	    	} // Since we always define charge and spin this directive is never null
			sb.append(NL); // Empty line terminating molecular specification section
			
			//
			// Build line for modredudant option
			//
			if (blockId < modRedDirs.size())
			{
				DirectiveData dd = (DirectiveData) modRedDirs.get(blockId);
				ConstraintsSet cs = (ConstraintsSet) dd.getValue();
	            for (Constraint cns : cs)
	            {
	            	String str = "";
	            	if (cns.getPrefix().isBlank())
	            	{	
	                	switch (cns.getType())
	                	{
							case ANGLE:
								str = "A ";
								break;
							case DIHEDRAL:
								str = "D ";
								break;
							case IMPROPERTORSION:
								str = "D ";
								break;
							case DISTANCE:
								str = "B ";
								break;
							case FROZENATM:
								str = "X ";
								break;
							case UNDEFINED:
								switch (cns.getAtomIDs().size())
								{
								case 1:
									str = "X ";
									break;
								case 2:
									str = "B ";
									break;
								case 3:
									str = "A ";
									break;
								case 4:
									str = "D ";
									break;
								}
								break;
							default:
								break;
	                	}
	            	}
	            	str = str + StringUtils.mergeListToString(
	            			cns.getAtomIDs(), " ", true, 1);
	            	
	            	if (!cns.getSuffix().isBlank())
	            	{
	            		str = str + " " + cns.getSuffix();
	            	}
	            	
	            	sb.append(str).append(NL);
	            }
				sb.append(NL); //empty line that terminates this part of option section
	    	}
    	}
			
		//
		// Building lines of (unique) Options section
		//
    	Directive optDir = step.getDirective(GaussianConstants.DIRECTIVEOPTS);
    	if (optDir != null)
    	{
    		// For the moment we expect only directive data in this section
    		Set<String> optNames = new HashSet<String>();
    		optDir.getAllDirectiveDataBlocks().stream().forEach(
    				dd -> optNames.add(dd.getName()));
    		List<String> sortedOptNames = sortOpts(optNames);
    		for (String ddName : sortedOptNames)
    		{
    			DirectiveData dd = optDir.getFirstDirectiveData(ddName);
    			// Some of the directivedata blocks need to be interpreted to
    			// convert the agnostic data into Gaussian slang
    			switch (ddName.toUpperCase())
    			{
	    			case GaussianConstants.DDBASISSET:
	    			{
	    				BasisSet bs = (BasisSet) dd.getValue();
	    				sb.append(StringUtils.mergeListToString(
	    						formatBasisSetLines(bs), NL));
	    				break;
	    			}
	    			
	    			case GaussianConstants.DDMODREDUNDANT:
	    			{
	    				// We have done this together with the title and molSpec
	    				/*
	    				ConstraintsSet cs = (ConstraintsSet) dd.getValue();
	                    for (Constraint cns : cs)
	                    {
	                    	String str = "";
	                    	if (cns.getPrefix().isBlank())
	                    	{	
		                    	switch (cns.getType())
		                    	{
									case ANGLE:
										str = "A ";
										break;
									case DIHEDRAL:
										str = "D ";
										break;
									case IMPROPERTORSION:
										str = "D ";
										break;
									case DISTANCE:
										str = "B ";
										break;
									case FROZENATM:
										str = "X ";
										break;
									case UNDEFINED:
										switch (cns.getAtomIDs().size())
										{
										case 1:
											str = "X ";
											break;
										case 2:
											str = "B ";
											break;
										case 3:
											str = "A ";
											break;
										case 4:
											str = "D ";
											break;
										}
										break;
									default:
										break;
		                    	}
	                    	}
	                    	str = str + StringUtils.mergeListToString(
	                    			cns.getAtomIDs(), " ", true, 1);
	                    	
	                    	if (!cns.getSuffix().isBlank())
	                    	{
	                    		str = str + " " + cns.getSuffix();
	                    	}
	                    	
	                    	sb.append(str).append(NL);
	                    }
	sb.append		lines.add(""); //empty line that terminates this part of option section
	        			*/
	    				break;
	    			}
	    		
	    			default:
	    			{
	    				sb.append(StringUtils.mergeListToString(
	    						optDir.getFirstDirectiveData(ddName).getLines(),
	    						NL));
	        			sb.append(NL); //empty line that terminates this part of option section
	    			}
    			}
    		}
    		
    		// Dealing with keywords even if we do not (yet) expect them to be
    		// present. They might result from the attempt to achieve special 
    		//results
    		for (Keyword k : optDir.getAllKeywords())
    		{
    			sb.append(k.toString("=")).append(NL);
    			sb.append("").append(NL); //empty line that terminates this part of option section
    		}
    		
    		// Dealing with subdirective even if we now do not expect them to be
    		// present. They might result from the attempt to achieve special 
    		//results
    		for (Directive subDir : optDir.getAllSubDirectives())
    		{
    			String directiveLine = "";
    			// Gaussian uses only one nesting level!
    			if (subDir.getAllSubDirectives().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Option "
        					+ "section of Gaussian job contains nested "
        					+ "sub-sub-directive/s. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			if (subDir.getAllDirectiveDataBlocks().size()>0)
    			{
    				throw new IllegalArgumentException(
        					"Subdirective " + subDir.getName() + " in Option "
        					+ "section of Gaussian job contains directive "
        					+ "data blocks. This is not compatible with "
        					+ "Gaussian input syntax. Check your input!");
    			}
    			directiveLine = directiveLine + subDir.getName() + "=(";
    			boolean first = true;
    			for (Keyword k : subDir.getAllKeywords())
    			{
    				String keyStr = k.toString("=");
    				sb.append("%"+k.toString("=")).append(NL);
    				if (first)
    				{
    					directiveLine = directiveLine + keyStr;
    				} else {
    					directiveLine = directiveLine + "," + keyStr;
    				}
    				first = false;
    			}
    			directiveLine = directiveLine + ")";
    			sb.append(directiveLine).append(NL);
    			sb.append("").append(NL); //empty line that terminates this part of option section
    		}
			sb.append("").append(NL); //empty line that terminates the option section
    	} // No default Option section
    	
    	return sb;
    }
    
//------------------------------------------------------------------------------
    
    private static String getCenterIdentifier(CenterBasisSet cbs)
    {
    	String atmStr = "";
		if (cbs.getCenterIndex()!=null)
		{
			atmStr = (cbs.getCenterIndex()+1) + "";
		} else {
			atmStr = Character.toUpperCase(cbs.getElement().charAt(0)) + "";
			if (cbs.getElement().length()>1)
				atmStr = atmStr + cbs.getElement().toLowerCase().substring(1);
		}
		return atmStr;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Prepares the lines of text hat are meant to define a basis set in 
     * Gaussian's input files, i.e., in the Gaussian Basis Set (gbs) format
     * used also for saving basis sets to files.
     * @param bs the basis set to format to a list of strings.
     * @return the list of strings where each string is a line (without new line
     * character at the end).
     */
    public static List<String> formatBasisSetLines(BasisSet bs) 
	{
    	List<String> lines = new ArrayList<String>();
    	for (CenterBasisSet cbs : bs.centerBSs)
    	{
	        String atmStr = getCenterIdentifier(cbs);
	        if (cbs.getNamedComponents().size() > 0)
	        {
	            for (String n : cbs.getNamedComponents())
	            {
	                lines.add(String.format(Locale.ENGLISH,"%-6s 0", atmStr));
	                lines.add(n);
	                lines.add("****");
	            } 
	        }
	        if (cbs.getShells().size() > 0)
	        {
	        	lines.add(String.format(Locale.ENGLISH,"%-6s 0", atmStr));
	            for (Shell s : cbs.getShells())
	            {
	            	lines.add(String.format(Locale.ENGLISH, "%-3s %-3d %-7.3f",
	            			s.getType(), s.getSize(), s.getScaleFact()));
	                for (Primitive p : s.getPrimitives())
	                {
	                    String eForm = "%" + (p.getExpPrecision() + 6) + "." 
	                    		+ (p.getExpPrecision()-1) + "E     ";
	                    String line = String.format(Locale.ENGLISH,
	                    		eForm,p.getExp());
	                    
	                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
	                    		+ (p.getCoeffPrecision()-1) + "E";
	                    for (Double c : p.getCoeff())
	                    {
	                    	line = line + String.format(Locale.ENGLISH,cForm,c);
	                    }
	                    lines.add(line);
	                }
	            }
	            lines.add("****");
	        }
    	}
    	
    	// This is where we add the empty line between basis set and ECP block
    	lines.add("");
    	
    	for (CenterBasisSet cbs : bs.centerBSs)
    	{
	        if (cbs.getECPShells().size() == 0)
	        {
	            continue;
	        }
	        
	        String atmStr = getCenterIdentifier(cbs);
	        
	        lines.add(String.format(Locale.ENGLISH, "%-6s 0", atmStr));
	        lines.add(String.format(Locale.ENGLISH, "%s %2d %3d", 
	        		cbs.getECPType(), cbs.getECPMaxAngMom(), 
	        		cbs.getElectronsInECP()));
            for (ECPShell s : cbs.getECPShells())
            {
            	lines.add(String.format(Locale.ENGLISH, "%-3s", s.getType()));
            	lines.add(String.format(Locale.ENGLISH, " %2d", s.getSize()));
                for (Primitive p : s.getPrimitives())
                {
                	String line = String.format(Locale.ENGLISH, "%-1d", 
                			p.getAngMmnt());
                    String eForm = "%" + (p.getExpPrecision() + 6) + "." 
                    		+ (p.getExpPrecision()-1) + "E     ";
                    line = line + String.format(Locale.ENGLISH,
                    		eForm, p.getExp());
                    
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
     * Order the keys of the option blocks according to the presumed 
     * expectations of Gaussian (i.e., basis set before PCM)
     * @param keySet the set of keys
     * @return the reordered list
     */

    private List<String> sortOpts(Set<String> keySet)
    {
        List<String> sortedKeys = new ArrayList<String>();
        if (keySet.contains(GaussianConstants.DDMODREDUNDANT))
        {
            sortedKeys.add(GaussianConstants.DDMODREDUNDANT);
        }
        if (keySet.contains(GaussianConstants.DDBASISSET))
        {
            sortedKeys.add(GaussianConstants.DDBASISSET);
        }
        if (keySet.contains(GaussianConstants.DDPCM))
        {
            sortedKeys.add(GaussianConstants.DDPCM);
        }
        for (String key : keySet)
        {
            if (key.equals(GaussianConstants.DDPCM) 
                || key.equals(GaussianConstants.DDBASISSET)
                || key.equals(GaussianConstants.DDMODREDUNDANT))
            {
                continue;
            }
            sortedKeys.add(key);
        }
        return sortedKeys;
    }
    
//------------------------------------------------------------------------------
	
  	/**
  	 * {@inheritDoc}
  	 * 
  	 * No special file structure required for Gaussian. This method does nothing.
  	 */
  	@Override
  	protected File manageOutputFileStructure(List<IAtomContainer> mols,
  			File output) 
  	{
  		return output;
  	}
    
//------------------------------------------------------------------------------

}
