package autocompchem.chemsoftware.gaussian;

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
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
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
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software Gaussian.
 *
 * @author Marco Foscato
 */

public class GaussianInputWriter extends ChemSoftInputWriter
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTGAUSSIAN)));
    
//-----------------------------------------------------------------------------

    /**
     * Constructor sets the parameters that depend on Gaussian conventions.
     */

    public GaussianInputWriter() 
    {
		inpExtrension = GaussianConstants.GAUINPEXTENSION;
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
     * the {@value GaussianConstants#DIRECTIVEMOLSPEC} {@link Directive}.
     * 
     * WARNING: so far it works with only one molecule.
     */
    protected void setChemicalSystem(CompChemJob ccj, List<IAtomContainer> iacs)
    {
    	if (!needsGeometry(ccj))
    		return;
    	
    	//TODO-gg fixme!
    	//WARNING so far works with only one chemical system
    	IAtomContainer iac = iacs.get(0);

    	DirectiveData dd = new DirectiveData("coordinates");
    	dd.setValue(iac);
    	if (ccj.getNumberOfSteps()>0)
    	{
        	setDirectiveDataIfNotAlreadyThere((CompChemJob) ccj.getStep(0), 
        			GaussianConstants.DIRECTIVEMOLSPEC, "coordinates", dd);
    	} else {
        	setDirectiveDataIfNotAlreadyThere(ccj, 
        			GaussianConstants.DIRECTIVEMOLSPEC, "coordinates", dd);
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
	    	Keyword geomKey = routeDir.getKeyword(GaussianConstants.GAUKEYGEOM);
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
    	File pathnameRoot = new File(outFileNameRoot);
    	setKeywordIfNotAlreadyThere(ccj, GaussianConstants.DIRECTIVELINK0,
    			"chk", true, pathnameRoot.getName());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     */
    protected ArrayList<String> getTextForInput(CompChemJob job)
    {
        ArrayList<String> lines= new ArrayList<String>();
        if (job.getNumberOfSteps()>1)
        {
	        for (int step = 0; step<job.getNumberOfSteps(); step++)
	        {
	        	CompChemJob stepCcj = (CompChemJob)job.getStep(step);
	            if (step != 0)
	            {
	                lines.add(GaussianConstants.STEPSEPARATOR);
	            }
	            lines.addAll(getTextForStep(stepCcj));
	        }
        } else {
        	lines.addAll(getTextForStep(job));
        }
        return lines;
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
    
    private ArrayList<String> getTextForStep(CompChemJob step)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	//
    	// Building lines of Link0 section
    	// 
    	
    	Directive lnkDir = step.getDirective(GaussianConstants.DIRECTIVELINK0);
    	if (lnkDir != null)
    	{
    		// We expect only keywords
    		for (Keyword k : lnkDir.getAllKeywords())
    		{
    			if (k.isLoud())
    				lines.add("%" + k.getName() + "=" + k.getValueAsString());
    			else
        			lines.add("%" + k.getValueAsString());
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
    	// Convert header into route section
    	//
    	Directive textHeader = step.getDirective(ChemSoftConstants.PARHEADER);
    	if (textHeader!=null)
    	{
    		lines.add(textHeader.getDirectiveData(ChemSoftConstants.PARHEADER)
    				.getValueAsString());
    	}
    	
    	//
    	// Building lines of Route section
    	//
    	Directive rouDir = step.getDirective(GaussianConstants.DIRECTIVEROUTE);
    	if (rouDir != null)
    	{
    		// First the special keywords
    		String firstLine = "";
    		Keyword pKey = rouDir.getKeyword(GaussianConstants.KEYPRINT);
    		if (pKey!=null)
    		{
    			firstLine = "#"+pKey.getValueAsString();
    		}  else {
    			firstLine = "#P";
            }
    		Keyword modKey = rouDir.getKeyword(GaussianConstants.KEYMODELMETHOD);
    		if (modKey!=null)
    		{
    			firstLine = firstLine + " " + modKey.getValueAsString() + "/";
    		} else {
    			firstLine = firstLine + " ";
    		}
    		Keyword bsKey = rouDir.getKeyword(GaussianConstants.KEYMODELBASISET);
    		if (bsKey!=null)
    		{
    			// space has been already added, if needed
    			firstLine = firstLine + bsKey.getValueAsString();
    		}
    		lines.add(firstLine);
    		Keyword jtKey = rouDir.getKeyword(GaussianConstants.KEYJOBTYPE);
    		if (jtKey!=null)
    		{
    			lines.add("# " + jtKey.getValueAsString());
    		}
    		
    		// All other keywords
    		for (Keyword k : rouDir.getAllKeywords())
    		{
    			if (GaussianConstants.SPECIALKEYWORDS.contains(k.getName()))
    				continue;
    			if (k.isLoud())
    			{
    				lines.add("# " + k.getName() + "=" + k.getValueAsString());
    			} else {
        			lines.add("# " + k.getValueAsString());
    			}
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
    				String keyStr = "";
    				if (k.isLoud())
    				{
    					keyStr = k.getName() + "=" + k.getValueAsString();
    				} else {
    					keyStr = k.getValueAsString();
    				}
    				if (first)
    				{
    					directiveLine = directiveLine + keyStr;
    				} else {
    					directiveLine = directiveLine + "," + keyStr;
    				}
    				first = false;
    			}
    			directiveLine = directiveLine + ")";
        		lines.add(directiveLine);
    		}
    		
    		if (rouDir.getAllDirectiveDataBlocks().size()>0)
    		{
    			throw new IllegalArgumentException(
    					"Directive for Route section of Gaussian "
    					+ "job contains data blocks but only keywords are "
    					+ "expected. Check your input!");
    		}
    	} else {
    		if (textHeader==null)
    			lines.add("#P");
    	}
		lines.add(""); // Empty line terminating route section
    	
		//
		// Building line of Title section (well, one line plus blank line)
		//
    	Directive titDir = step.getDirective(GaussianConstants.DIRECTIVETITLE);
    	if (titDir != null)
    	{
    		// We expect only ONE keywords
    		if (titDir.getAllKeywords().size()>1)
    		{
    			throw new IllegalArgumentException(
    					"Directive for title section of Gaussian "
    					+ "job contains more than one keyword. "
    					+ "Check your input!");
    		}
    		Keyword k = titDir.getAllKeywords().get(0);
    		lines.add(k.getValueAsString());
    		
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
			lines.add("No title");
    	}
		lines.add(""); // Empty line terminating title section
		
		//
		// Building lines of Molecular Specification Section
		//
		Directive molDir = step.getDirective(GaussianConstants.DIRECTIVEMOLSPEC);
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
    		Keyword kCharge = molDir.getKeyword(GaussianConstants.MSCHARGEKEY);
	    	Keyword kSpinMult = molDir.getKeyword(GaussianConstants.MSSPINMLTKEY);
    		lines.add(kCharge.getValueAsString() + " " + kSpinMult.getValueAsString());
    		
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
	    					lines.add(String.format(Locale.ENGLISH,"%s", el)
	    							+ String.format(Locale.ENGLISH," %17.12f",
	    									p3d.x)
	    							+ String.format(Locale.ENGLISH," %17.12f",
	    									p3d.y)
	    							+ String.format(Locale.ENGLISH," %17.12f",
	    									p3d.z));
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
	    			            lines.add(sbAtom.toString());
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
	    			            lines.add(sbAtom.toString());
	    			        }
	    			        // Then write the list of variables with initial value
	    			        lines.add("  Variables:");
	    			        for (int i=0; i<zmat.getZAtomCount(); i++)
	    			        {
	    			        	ZMatrixAtom zatm = zmat.getZAtom(i);
	    			        	for (int iIC=0; iIC<zatm.getICsCount(); iIC++)
	    			        	{
	    			        		InternalCoord ic = zatm.getIC(iIC);
	    			        		if (ic.isConstant())
	    			        			continue;
	    			        		lines.add(ic.getName() + " " 
	    			        			+ String.format(Locale.ENGLISH, 
	    			        					" %5.8f", ic.getValue()));
	    			        	}
	    			        }
	    			        
	    			        // And finally write the list of constants
	    			        lines.add("  Constants:");
	    			        for (int i=0; i<zmat.getZAtomCount(); i++)
	    			        {
	    			        	ZMatrixAtom zatm = zmat.getZAtom(i);
	    			        	for (int iIC=0; iIC<zatm.getICsCount(); iIC++)
	    			        	{
	    			        		InternalCoord ic = zatm.getIC(iIC);
	    			        		if (!ic.isConstant())
	    			        			continue;
	    			        		lines.add(ic.getName() + " " 
	    			        			+ String.format(Locale.ENGLISH,
	    			        					" %5.8f", ic.getValue()));
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
		lines.add(""); // Empty line terminating molecular specification section
    	
		//
		// Building lines of Options section
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
    			DirectiveData dd = optDir.getDirectiveData(ddName);
    			// Some of the directivedata blocks need to be interpreted to
    			// convert the agnostic data into Gaussian slang
    			switch (ddName.toUpperCase())
    			{
	    			case GaussianConstants.DDBASISSET:
	    			{
	    				BasisSet bs = (BasisSet) dd.getValue();
	    				lines.addAll(formatBasisSetLines(bs));
	    				break;
	    			}
	    			
	    			case GaussianConstants.DDMODREDUNDANT:
	    			{
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
	                    	
	                    	lines.add(str);
	                    }
	        			lines.add(""); //empty line that terminates this part of option section
	    				break;
	    			}
	    		
	    			default:
	    			{
	    				lines.addAll(optDir.getDirectiveData(ddName).getLines());
	        			lines.add(""); //empty line that terminates this part of option section
	    			}
    			}
    		}
    		
    		// Dealing with keywords even if we do not (yet) expect them to be
    		// present. They might result from the attempt to achieve special 
    		//results
    		for (Keyword k : optDir.getAllKeywords())
    		{
    			if (k.isLoud())
    				lines.add(k.getName() + "=" + k.getValueAsString());
    			else
        			lines.add(k.getValueAsString());
    			lines.add(""); //empty line that terminates this part of option section
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
    				String keyStr = "";
    				if (k.isLoud())
    				{
    					keyStr = k.getName() + "=" + k.getValueAsString();
    				} else {
    					keyStr = k.getValueAsString();
    				}
    				if (first)
    				{
    					directiveLine = directiveLine + keyStr;
    				} else {
    					directiveLine = directiveLine + "," + keyStr;
    				}
    				first = false;
    			}
    			directiveLine = directiveLine + ")";
    			lines.add(directiveLine);
    			lines.add(""); //empty line that terminates this part of option section
    		}
			lines.add(""); //empty line that terminates the option section
    	} // No default Option section
    	
    	return lines;
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
  	protected String manageOutputFileStructure(List<IAtomContainer> mols,
  			String outputFileName) 
  	{
  		return outputFileName;
  	}
    
//------------------------------------------------------------------------------

}
