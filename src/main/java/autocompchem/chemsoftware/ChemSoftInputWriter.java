package autocompchem.chemsoftware;

import java.io.IOException;

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
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;

import autocompchem.files.ACCFileType;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Worker;

/**
 * Core components of any tool writing input files for software packages.
 *
 * @author Marco Foscato
 */

public abstract class ChemSoftInputWriter extends Worker
{
    /**
     * Molecular geometries input file. One or more geometries depending on the
     * kind of computational chemistry job. 
     */
    private String inGeomFile;
    
    /**
     * List of molecular systems considered as input. This can either be
     * the list of molecules for which we want to make the input, or the list
     * of geometries used to make a multi-geometry input file.
     */
    private List<IAtomContainer> inpGeom = new ArrayList<IAtomContainer>();

    /**
     * Definition of how to use multiple geometries
     */
    private enum MultiGeomMode {INDEPENDENT, PATH};
    
    /**
     * Chosen mode of handling multiple geometries.
     */
    private MultiGeomMode multiGeomMode = MultiGeomMode.INDEPENDENT;

    /**
     * Flaq requesting to overwrite geometry names
     */
    private boolean overwriteGeomNames = false;
    
    /**
     * Geometry names
     */
    protected List<String> geomNames = new ArrayList<String>(
    		Arrays.asList("geometry"));

    /**
     * Input format identifier.
     */
    private String inFileFormat = "nd";

    //TODO-gg make these be File
    /**
     * Pathname root for output files (input for comp.chem. software).
     */
    protected String outFileNameRoot;
    
    /**
     * Output name (input for comp.chem. software).
     */
    protected String outFileName;
    
    /**
     * Flag deciding if we write the specific job-details file or not.
     */
    private boolean writeJobSpecificJDOutput = true;

    /**
     * Output job details name.
     */
    private String outJDFile;

    /**
     * Charge of the whole system.
     */
    private int charge = 0;
    
    /**
     * Spin multiplicity of the whole system.
     */
    private int spinMult = 1;
    
    /**
     * Flag requiring to omit charge specification.
     */
    private boolean omitCharge = false;
    
    /**
     * Flag requiring to omit spin multiplicity specification.
     */
    private boolean omitSpinMult = false;
    
    /**
     * The computational chemistry job we want to prepare the input for.
     */
    protected CompChemJob ccJob;

    /** 
     * Verbosity level
     */
    protected int verbosity = 0;
    
    /**
     * Default extension of the chem.soft. input file
     */
    protected String inpExtrension;

    /**
     * Default extension of the chem.soft output file
     */
    protected String outExtension;

    protected final String NL = System.getProperty("line.separator");
    

//-----------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public ChemSoftInputWriter()
    {
    	super("inputdefinition/ChemSoftInputWriter.json");
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    @Override
    public void initialize()
    {
        if (params.contains(ChemSoftConstants.PARVERBOSITY))
        {
            String str = params.getParameter(
                    ChemSoftConstants.PARVERBOSITY).getValueAsString();
            this.verbosity = Integer.parseInt(str);

            if (verbosity > 0)
                System.out.println(" Adding parameters to chemical software "
                		+ "input writer.");
        }

        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
            this.inGeomFile = params.getParameter(
                    ChemSoftConstants.PARGEOMFILE).getValueAsString();
            
            //TODO: use automated detection of file type
            
            if (inGeomFile.endsWith(".sdf"))
            {
                inFileFormat = "SDF";
            }
            else if (inGeomFile.endsWith(".xyz"))
            {
                inFileFormat = "XYZ";
            }
            else if (inGeomFile.endsWith(".out"))
            {
                //TODO: identify the kind of cc-software that produced that file
                Terminator.withMsgAndStatus("ERROR! Format of file '"
                        + inGeomFile + "' not recognized!",-1);
            }
            else
            {
                Terminator.withMsgAndStatus("ERROR! Format of file '" 
                        + inGeomFile + "' not recognized!",-1);
            }
            FileUtils.foundAndPermissions(this.inGeomFile,true,false,false);
            
            //TODO: make and use a general molecular structure reader
            switch (inFileFormat) 
            {
            case "SDF":
                inpGeom = IOtools.readSDF(inGeomFile);
                break;

            case "XYZ":
                inpGeom = IOtools.readXYZ(inGeomFile);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! " 
                    + this.getClass().getName()
                    + " can read multi-geometry input files "
                    + "only when starting from XYZ of SDF files. Make "
                    + "sure file '" + inGeomFile + "' has proper "
                    + "format and extension.", -1);
            }
        } 
        if (params.contains(ChemSoftConstants.PARGEOM))
        {
            this.inpGeom = (List<IAtomContainer>) params.getParameter(
                    ChemSoftConstants.PARGEOM).getValue();
        }
        //TODO: deal with stream-like input
        if (inpGeom.size()==0)
        {
            Terminator.withMsgAndStatus("ERROR! No geometry provided. "
            		+ "Nothing to do!",-1);
        }

        if (params.contains(ChemSoftConstants.PARMULTIGEOMMODE))
        {
            String value = params.getParameter(
            		ChemSoftConstants.PARMULTIGEOMMODE).getValueAsString();
            this.multiGeomMode = MultiGeomMode.valueOf(value.toUpperCase());
        }
        
        if (params.contains(ChemSoftConstants.PARGEOMNAMES))
        {
            this.overwriteGeomNames = true;
            this.geomNames.clear();
            String line = params.getParameter(
            		ChemSoftConstants.PARGEOMNAMES).getValue().toString();
            String[] parts = line.trim().split("\\s+");
            for (int i=0; i<parts.length; i++)
            {
                if (this.geomNames.contains(parts[i]))
                {
                    Terminator.withMsgAndStatus("ERROR! Geometry name '" 
                                        + parts[i] + "' is used more than once."
                                        + " Check line '" + line + "'.",-1);
                }
                this.geomNames.add(parts[i]);
            }
            if (geomNames.size()!=inpGeom.size())
            {
            	Terminator.withMsgAndStatus("ERROR! Found " + inpGeom.size() 
            		+ " geometries, but " + geomNames.size() + " names. Check "
            		+ "yout input.",-1); 
            }
        }

        if (params.contains(ChemSoftConstants.PARJOBDETAILSFILE))
        {
            String jdFile = params.getParameter(
                    ChemSoftConstants.PARJOBDETAILSFILE).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from JD file '" 
                        + jdFile + "'.");
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            if (FileAnalyzer.getFileTypeByProbeContentType(jdFile) ==
            		ACCFileType.JSON)
            {
            	try {
					this.ccJob = CompChemJob.fromJSONFile(jdFile);
				} catch (IOException e) {
					e.printStackTrace();
					Terminator.withMsgAndStatus("ERROR! Could not construct "
							+ "job from file '" + jdFile + "'.",-1); 
				}	
            } else {
            	this.ccJob = new CompChemJob(jdFile);
            }
        }
        else if (params.contains(ChemSoftConstants.PARJOBDETAILSOBJ))
        {
        	this.ccJob = (CompChemJob) params.getParameter(
                    ChemSoftConstants.PARJOBDETAILSOBJ).getValue();
        }
        else if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdLines = params.getParameter(
                    ChemSoftConstants.PARJOBDETAILS).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from nested parameter block.");
            }
            ArrayList<String> lines = new ArrayList<String>(Arrays.asList(
                    jdLines.split("\\r?\\n")));
            this.ccJob = new CompChemJob(lines);
        }
        else if (params.contains(ChemSoftConstants.PARHEADER))
        {
        	this.ccJob = new CompChemJob();
        	Directive header = new Directive(ChemSoftConstants.PARHEADER);
        	DirectiveData ddHeader = new DirectiveData(
        			ChemSoftConstants.PARHEADER);
        	ddHeader.setValue(params.getParameter(
                    ChemSoftConstants.PARHEADER).getValueAsString());
        	header.addDirectiveData(ddHeader);
        	this.ccJob.addDirective(header);
        }
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                    + "Neither '" + ChemSoftConstants.PARJOBDETAILSFILE
                    + "' nor '" + ChemSoftConstants.PARJOBDETAILS 
                    + "'found in parameters.",-1);
        }

        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileNameRoot = params.getParameter(
                    ChemSoftConstants.PAROUTFILEROOT).getValueAsString();
            outFileName = outFileNameRoot + inpExtrension;
            outJDFile = outFileNameRoot + ChemSoftConstants.JSONJDEXTENSION;
        } else if (params.contains(ChemSoftConstants.PAROUTFILE))
        {
        	outFileName = params.getParameter(
        			ChemSoftConstants.PAROUTFILE).getValueAsString();
            outFileNameRoot = FileUtils.getRootOfFileName(outFileName);
            outJDFile = outFileNameRoot + ChemSoftConstants.JSONJDEXTENSION;
        } else {
        	if (inGeomFile==null)
        	{
        		outFileNameRoot = "accOutput";
                if (verbosity > 0)
                {
	        		System.out.println(" Neither '" 
	        				 + ChemSoftConstants.PAROUTFILE + "' nor '" 
	        				 + ChemSoftConstants.PAROUTFILEROOT + "' found and no '"
	        				 + ChemSoftConstants.PARGEOMFILE + "' found. " + NL
	                         + " Root of any output file name set to '" 
	                         + outFileNameRoot + "'.");
                }
        	} else {
        		outFileNameRoot = FileUtils.getRootOfFileName(inGeomFile);
                if (verbosity > 0)
                {
                    System.out.println(" Neither '" 
                    		+ ChemSoftConstants.PAROUTFILEROOT + "' nor '"
                    		+ ChemSoftConstants.PAROUTFILE 
                    		+ "' parameter found. " + NL
                            + " Root of any output file name set to '" 
                            + outFileNameRoot + "'.");
                }
        	}
            outFileName = outFileNameRoot + inpExtrension;
            outJDFile = outFileNameRoot + ChemSoftConstants.JSONJDEXTENSION;
        }
        
        if (params.contains(ChemSoftConstants.PARNOJSONOUTPUT))
        {
        	writeJobSpecificJDOutput = false;
        }

        if (params.contains(ChemSoftConstants.PARCHARGE))
        {
            charge = Integer.parseInt(params.getParameter(
                    ChemSoftConstants.PARCHARGE).getValueAsString());
        } 

        if (params.contains(ChemSoftConstants.PARSPINMULT))
        {
            spinMult = Integer.parseInt(params.getParameter(
                    ChemSoftConstants.PARSPINMULT).getValueAsString());
        }
        
        if (params.contains(ChemSoftConstants.PARNOCHARGE))
        {
            omitCharge = true;
        }
        
        if (params.contains(ChemSoftConstants.PARNOSPIN))
        {
            omitSpinMult = true;
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialized.
     */

    @Override
    public void performTask()
    {
    	if (inpGeom.size() == 1)
    	{
    		produceSingleJobInputFiles(inpGeom, outFileName, outFileNameRoot);
    	} else {
    		
    		switch (multiGeomMode)
    		{
			case INDEPENDENT:
				// We simply repeat the same operation for each geometry.
				for (int molId = 0; molId<inpGeom.size(); molId++)
		        {
		            IAtomContainer mol = inpGeom.get(molId);
		            
		            String fileRootName = outFileNameRoot + "-" + molId;
		            if (overwriteGeomNames)
		            {
		            	String geomName = geomNames.get(molId);
		            	mol.setTitle(geomName);
		            	fileRootName = outFileNameRoot + "-" + geomName;
		            }
		            
		            if (verbosity > 0)
		            {
		                System.out.println(" Writing input file for molecule #" 
		                        + (molId+1) + ": " 
		                		+ MolecularUtils.getNameOrID(mol));
		            }
		            
		            List<IAtomContainer> set = new ArrayList<IAtomContainer>();
		            set.add(mol);
		            produceSingleJobInputFiles(set, fileRootName+inpExtrension,
		            		fileRootName);
		        }
				break;
				
			case PATH:
				// All geometries are included in a single input
	            if (verbosity > 0)
	            {
	                System.out.println(" Writing input file for " 
	                		+ inpGeom.size() + " geometries");
	            }
	            
				if (overwriteGeomNames)
	            {
					for (int molId = 0; molId<inpGeom.size(); molId++)
			        {
		            	String geomName = geomNames.get(molId);
		            	inpGeom.get(molId).setTitle(geomName);
			        }
	            }
	            
	            produceSingleJobInputFiles(inpGeom, outFileNameRoot 
	            		+ inpExtrension, outFileNameRoot);
				break;
				
			default:
				Terminator.withMsgAndStatus("ERROR! Multigeometry "
						+ "mode '" + multiGeomMode + "' is not implemented. "
						+ "Please, contact the authors.", -1);
				break;
    		}
    	}
    	
        if (exposedOutputCollector != null)
        {
/*
//TODO
            String refName = "";
            exposeOutputData(new NamedData(refName,
                  NamedDataType.DOUBLE, ));
*/
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Prepare the set of input files of a single job, which may or may not 
     * consist of multiple steps to be performed by a computational chemistry 
     * software tool.
     * @param mols the set of geometries that pertain this single job. Note that
     * in the vast majority of cases there will be only one geometry. This
     * corresponds to multi-geometry mode {@value MultiGeomMode#INDEPENDENT}.
     * However, jobs do use multiple input geometries within the same job
     * (e.g., transition state searches that start from the geometries of the
     * reactant and product). This is the case of multi-geometry mode 
     * {@value MultiGeomMode#PATH}.
     * <br>
     * <b>WARNING</b>: Changes in the number of electrons or in spin 
     * multiplicity among the geometries are not supported (yet).
     * @param outFileName the pathname of the job's main input file.
     * @param outFileNameRoot the root of the 
     * pathname to any job input file that will be
     * produced. Extensions and suffixed are defined by software specific 
     * constants.
     */
    private void produceSingleJobInputFiles(List<IAtomContainer> mols, 
    		String outFileName,	String outFileNameRoot)
    {
    	// We customize a copy of the master job
		CompChemJob molSpecJob = ccJob.clone();
		
		// Define the name's root for any input file created
		molSpecJob.setParameter(ChemSoftConstants.PAROUTFILEROOT, 
				outFileNameRoot, true);
		
		// Add atom coordinates to the so-far molecule-agnostic job
		setChemicalSystem(molSpecJob, mols);
		
		// Add strings/pathnames that are molecular specific. e.g., pathnames or
		// links that are explicitly defined in any part of any input file.
		setSystemSpecificNames(molSpecJob);
		
		// WARNING: for now we do not expect any change of #electrons or spin
		Object pCharge = mols.get(0).getProperty(ChemSoftConstants.PARCHARGE);
		if (pCharge != null)
		{
			try {
				Integer.valueOf(pCharge.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pCharge.toString() + "' as charge. Check "
						+ "value of property '" + ChemSoftConstants.PARCHARGE
						+ "'.", -1);
			}
			setChargeIfUnset(molSpecJob, pCharge.toString(), omitCharge);
		}
		
		Object pSpin = mols.get(0).getProperty(ChemSoftConstants.PARSPINMULT);
		if (pSpin != null)
		{
			try {
				Integer.valueOf(pSpin.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pSpin.toString() + "' as spin multiplicity. Check "
						+ "value of property '" + ChemSoftConstants.PARSPINMULT
						+ "'.", -1);
			}
			setSpinMultiplicityIfUnset(molSpecJob, pSpin.toString(), omitSpinMult);
		}
		
		// These calls take care also of the sub-jobs/directives
		molSpecJob.processDirectives(mols, this.getMyJob());
		
		// Ensure a value of charge and spin has been defined
		setChargeIfUnset(molSpecJob, charge+"", omitCharge);
		setSpinMultiplicityIfUnset(molSpecJob, spinMult+"", omitSpinMult);
		
		// Manage output consisting of multiple files and/or folder trees
		outFileName = manageOutputFileStructure(mols, outFileName);
		
		// Produce the actual main input file
		FileUtils.mustNotExist(outFileName);
		IOtools.writeTXTAppend(outFileName, getTextForInput(molSpecJob), false);
		
		// Produce a specific job-details file
		if (writeJobSpecificJDOutput)
		{
			CompChemJob cleanCCJ = molSpecJob.clone();
			cleanCCJ.removeACCTasks();
			FileUtils.mustNotExist(outFileNameRoot 
					+ ChemSoftConstants.JSONJDEXTENSION);
			Gson writer = ACCJson.getWriter();
			IOtools.writeTXTAppend(outFileNameRoot 
					+ ChemSoftConstants.JSONJDEXTENSION, 
					writer.toJson(cleanCCJ), true);
		}
    }

//------------------------------------------------------------------------------
    
    /**
     * Generates additional output files and folder tree, if needed. Some 
     * software requires input organized in specific folder trees and 
     * accompanied by accessory files. This method deals with this demands.
     * @param mols the structures to work with.
     * @param outputFileName the original pathname of the main input file. This 
     * may be modified as a result of the creation of the a structure-specific 
     * folder tree.
     * @return the pathname of the main input file.
     */
    protected abstract String manageOutputFileStructure(
    		List<IAtomContainer> mols, String outputFileName);

//------------------------------------------------------------------------------
      
	/**
     * Sets the strings that the given job needs to become specific.
     */
    protected abstract void setSystemSpecificNames(CompChemJob ccj);
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the charge definition to any step where it is not already defined. 
     * This means that this method must not overwrite existing charge settings.
     * Moreover, this method must deal with the requests to omit charge 
     * specification.
     * @param ccj the job to customize.
     * @param charge the value of the charge to specify.
     * @param omitIfPossible if <code>true</code> we are asked to omit the 
     * specification and leave the job use pre-existing information
     * coming from previous steps or from some sort
     * of input external to the job's main input file, e.g., checkpoint files)
     */
    protected abstract void setChargeIfUnset(CompChemJob ccj, String charge, 
    		boolean omitIfPossible);
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the spin multiplicity definition to any step where it is not already 
     * defined. 
     * This means that this method must not overwrite existing settings.
     * Moreover, this method must deal with the requests to omit spin 
     * specification.
     * @param ccj the job to customize.
     * @param sm the value of the spin multiplicity to specify.
     * @param omitIfPossible if <code>true</code> we are asked to omit the 
     * specification and leave the job use pre-existing information
     * coming from previous steps or from some sort
     * of input external to the job's main input file, e.g., checkpoint files)
     */
    protected abstract void setSpinMultiplicityIfUnset(CompChemJob ccj, 
    		String sm, boolean omitIfPossible);
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the definitions of the chemical system/s is needed. This method
     * handles multiple geometries according to the settings given upon 
     * initializing this instance.
     * Note that we here only consider the definition of the atoms and their 
     * position, not the charge of the spin multiplicity, for which there are 
     * dedicated methods.
     * @param ccj the job to customize.
     * @param iacs the atom containers to translate into one or more 
     * chemical system representation suitable for the comp.chem. software.
     */
    protected abstract void setChemicalSystem(CompChemJob ccj, 
    		List<IAtomContainer> iacs);

//------------------------------------------------------------------------------
    
    /**
     * Produced the text that will be printed in the job's main input file, 
     * the one defining what kind of setting the comp.chem. software should use
     * and what exactly to do. Other input needed by the comp.chem. software, 
     * such as the definition of the chemical system's geometry, may or may not 
     * be part of the main input file.
     */
    //TODO: make this work with a StringBuilder
    protected abstract ArrayList<String> getTextForInput(CompChemJob job);
    
//------------------------------------------------------------------------------
    
    /**
     * Sets a keyword in a directive with the given name to any step where it is
     * not already defined. 
     * This means that this method does not overwrite existing charge settings
     * @param ccj the job to customize.
     * @param dirName the name of the directive
     * @param keyName the name of the keywords
     * @param value the value of the keyword to specify.
     */
    public static void setKeywordIfNotAlreadyThere(CompChemJob ccj, 
    		String dirName, String keyName, String value)
    {
    	addNewKeyword(ccj, dirName, keyName, false, value);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets a keyword in a directive with the given name to any step where it is
     * not already defined. 
     * This means that this method does not overwrite existing keywords.
     * This method can alter only the given job and its steps, but does not 
     * consider the possibility of having further levels of nested jobs.
     * This method cannot add keywords that are more deeply embedded. Use 
     * {@link #addNewValueContainer(CompChemJob, DirComponentAddress, IValueContainer}
     * for that.
     * @param ccj the job to customize.
     * @param dirName the name of the directive that should hold the keyword.
     * @param keyName the name of the keywords
     * @param isLoud use <code>true</code> if the keyword should be set to be
     * a loud keyword, meaning that conversion to text used the syntax 
     * <code>key|separator|value</code> (for loud keywords) instead of just
     * <code>value</code> (for non-loud, or silent keywords).
     * @param value the value of the keyword to specify.
     */
    public static void addNewKeyword(CompChemJob ccj, 
    		String dirName, String keyName, boolean isLoud, String value)
    {
    	Keyword key = new Keyword(keyName, isLoud, value);
    	DirComponentAddress adrs = new DirComponentAddress();
    	adrs.addStep(dirName, DirectiveComponentType.DIRECTIVE);
    	addNewValueContainer(ccj, adrs, key);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets data in a directive with the given name to any step where it is
     * not already defined. 
     * This means that this method does not overwrite existing data.
     * This method can alter only the given job and its steps, but does not 
     * consider the possibility of having further levels of nested jobs.
     * This method cannot add data that are more deeply embedded. Use 
     * {@link #addNewValueContainer(CompChemJob, DirComponentAddress, IValueContainer}
     * for that.
     * @param ccj the job to customize.
     * @param dirName the directive that should hold the data to add.
     * @param dd the data to add.
     */
    public static void addNewDirectiveData(CompChemJob ccj, 
    		String dirName, DirectiveData dd)
    {
    	DirComponentAddress adrs = new DirComponentAddress();
    	adrs.addStep(dirName, DirectiveComponentType.DIRECTIVE);
    	addNewValueContainer(ccj, adrs, dd);
    }
    
//------------------------------------------------------------------------------

    /**
     * Adds a value container at any lever of embedding in a job directive's 
     * component structure, as long as no such 
     * container is already present on the given address.
     * @param ccj the job to customize.
     * @param address the location where to place the value container in the
     * directive's component structure. Any level of embedding is allowed.
     * @param component the data container that is to be added.
     */
    public static void addNewValueContainer(CompChemJob ccj, 
    		DirComponentAddress address, IValueContainer component)
    {
    	if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
    			((CompChemJob) stepJob).addNewValueContainer(address, component);
    		}
    	} else {
    		ccj.addNewValueContainer(address, component);
    	}
    }
    
//------------------------------------------------------------------------------

}
