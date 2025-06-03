package autocompchem.wiro.chem;

import java.io.File;
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

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.ACCFileType;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.wiro.ITextualInputWriter;
import autocompchem.wiro.InputWriter;
import autocompchem.wiro.WIROConstants;
import autocompchem.worker.WorkerConstants;

/**
 * Core components of any tool writing input files for software packages that
 * read some chemical systems as input. For software that does not read chemical
 * systems as input, see {@link InputWriter}.
 *
 * @author Marco Foscato
 */

public abstract class ChemSoftInputWriter extends AtomContainerInputProcessor 
	implements ITextualInputWriter
{
    /**
     * Flag requesting to overwrite geometry names
     */
    private boolean overwriteGeomNames = false;
    
    /**
     * Geometry names
     */
    protected List<String> geomNames = new ArrayList<String>();

    /**
     * Pathname root for output files (input for comp.chem. software).
     */
    protected String ccJobInputNameRoot;
    
    /**
     * Output name (input for comp.chem. software).
     */
    protected File ccJobInputFile;
    
    /**
     * Flag deciding if we write the specific job-details file or not.
     */
    private boolean writeJobSpecificJDOutput = true;

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
     * Default extension of the chem.soft. input file
     */
    protected String inpExtrension;

    /**
     * Default extension of the chem.soft output file
     */
    protected String outExtension;

    /**
     *  New line character
     */
    protected final String NL = System.getProperty("line.separator");
    
    /**
     * Format for writing Cartesian coordinates 
     */
    protected String formatCartCoord = "%10.8f";
    
    /**
     * Format for writing Cartesian coordinates 
     */
    protected String formatIC = "%4.4f";
    

//-----------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public ChemSoftInputWriter()
    {}
    
//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "inputdefinition/ChemSoftInputWriter.json";
	}
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    public void initialize()
    {
    	super.initialize();
        
        if (params.contains(ChemSoftConstants.PARCARTCOORDSFORMAT))
        {
        	formatCartCoord = params.getParameter(
                    ChemSoftConstants.PARCARTCOORDSFORMAT).getValueAsString();
        	try {
        		String.format(formatCartCoord, -123456.12345);
        	} catch (Throwable t) {
				t.printStackTrace();
				Terminator.withMsgAndStatus("ERROR! Could not use string '"
						+ formatCartCoord + "' as a floating point format "
						+ "definition. Please, check your input.", -1); 
        	}
        }
        
        if (params.contains(ChemSoftConstants.PARINTERNALCOORDSFORMAT))
        {
        	formatIC = params.getParameter(
                    ChemSoftConstants.PARINTERNALCOORDSFORMAT).getValueAsString();
        	try {
        		String.format(formatIC, -126.12345);
        	} catch (Throwable t) {
				t.printStackTrace();
				Terminator.withMsgAndStatus("ERROR! Could not use string '"
						+ formatIC + "' as a floating point format definition. "
						+ "Please, check your input.", -1); 
        	}
        }

        /*
        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
            String value = params.getParameter(
                    ChemSoftConstants.PARGEOMFILE).getValueAsString();
            String[] words = value.trim().split("\\s+");
            String pathname = words[0];
            FileUtils.foundAndPermissions(pathname,true,false,false);
            this.inGeomFile = new File(pathname);
            List<IAtomContainer> iacs = IOtools.readMultiMolFiles(inGeomFile);
            if (words.length > 1)
            {
            	for (int iw=1; iw<words.length; iw++)
            	{
            		String idStr = words[iw];
            		if (NumberUtils.isParsableToInt(idStr))
            		{
            			int id = Integer.parseInt(idStr);
            			if (id>-1 && id < iacs.size())
            			{
            				this.inpGeom.add(iacs.get(iacs.size()-1));
            			} else {
                        	Terminator.withMsgAndStatus("ERROR! Found request "
                        			+ "to take geometry " + id + " from '"
                        			+ pathname + "' but found "+ iacs.size() 
                        			+ " geometries. Check your input.",-1); 
            			}
            		} else if ("LAST".equals(idStr.toUpperCase())) {
            			this.inpGeom.add(iacs.get(iacs.size()-1));
            		} else {
            			Terminator.withMsgAndStatus("ERROR! Unable to "
                    			+ "understand option '" + idStr + "' for "
                    			+ ChemSoftConstants.PARGEOMFILE 
                    			+ ". Check your input.",-1); 
            		}
            	}
            } else {
            	this.inpGeom = iacs;
            }
        } 
        
        if (params.contains(ChemSoftConstants.PARGEOM))
        {
        	Object value = params.getParameter(ChemSoftConstants.PARGEOM)
        			.getValue();
            this.inpGeom = (List<IAtomContainer>) value;
        }

        if (params.contains(ChemSoftConstants.PARMULTIGEOMMODE))
        {
            String value = params.getParameter(
            		ChemSoftConstants.PARMULTIGEOMMODE).getValueAsString();
            this.multiGeomMode = EnumUtils.getEnumIgnoreCase(
            		MultiGeomMode.class, value);
        }
        */
        
        if (params.contains(ChemSoftConstants.PARGEOMNAMES))
        {
            this.overwriteGeomNames = true;
            this.geomNames.clear();
            String line = params.getParameter(
            		ChemSoftConstants.PARGEOMNAMES).getValueAsString();
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
        }

        if (params.contains(WIROConstants.PARJOBDETAILSFILE))
        {
            File jdFile = new File(params.getParameter(
                    WIROConstants.PARJOBDETAILSFILE).getValueAsString());
            logger.debug("Job details from JD file '" + jdFile + "'.");
            
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            if (FileAnalyzer.detectFileType(jdFile) ==
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
        else if (params.contains(WIROConstants.PARJOBDETAILSOBJ))
        {
        	this.ccJob = (CompChemJob) params.getParameter(
                    WIROConstants.PARJOBDETAILSOBJ).getValue();
        }
        else if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdLines = params.getParameter(
                    ChemSoftConstants.PARJOBDETAILS).getValueAsString();
            logger.info("Job details from nested parameter block.");
            List<String> lines = new ArrayList<String>(Arrays.asList(
                    jdLines.split("\\r?\\n")));
            this.ccJob = new CompChemJob(lines);
        }
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                    + "Neither '" + WIROConstants.PARJOBDETAILSFILE
                    + "' nor '" + ChemSoftConstants.PARJOBDETAILS 
                    + "'found in parameters.",-1);
        }

        if (params.contains(WIROConstants.PAROUTFILEROOT))
        {
            ccJobInputNameRoot = params.getParameter(
                    WIROConstants.PAROUTFILEROOT).getValueAsString();
            ccJobInputFile = new File(ccJobInputNameRoot + inpExtrension);
        } else if (params.contains(WIROConstants.PAROUTFILE))
        {
        	ccJobInputFile = new File(params.getParameter(
        			WIROConstants.PAROUTFILE).getValueAsString());
            ccJobInputNameRoot = FileUtils.getRootOfFileName(ccJobInputFile);
        } else {
        	ccJobInputNameRoot = decideRootPathName(inFile);
        	ccJobInputFile = new File(ccJobInputNameRoot + inpExtrension);
        }
        
        if (params.contains(WIROConstants.PARNOJSONOUTPUT))
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
    	if (params.contains(WIROConstants.PARJOBDETAILSOBJ))
    	{
    		// Here we do not read atom containers because we are given a fully 
    		// defined job details object, which is expected to contains all the
    		// details to make the input files.
    		// To this end, we set the list of input molecules to an empty list
    		inMols = new ArrayList<IAtomContainer>();
    		produceSingleJobInputFiles(inMols, ccJobInputFile, ccJobInputNameRoot);
    	} else {
    		// Here we read atom containers from whatever input we have, which
    		// could be a file to be read in, or a collection of objects stored 
    		// in the memory.
    		processInput();
    	}
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
		String fileRootName = ccJobInputNameRoot;
		if (inMols!=null && inMols.size()>1)
		{ 
			fileRootName = fileRootName + "-" + i;
		}
        if (overwriteGeomNames)
        {
            if (geomNames.size()!=inMols.size())
            {
            	Terminator.withMsgAndStatus("ERROR! Found " + inMols.size() 
            		+ " geometries, but " + geomNames.size() + " names. Check "
            		+ "your input.",-1); 
            }
        	String geomName = geomNames.get(i);
        	iac.setTitle(geomName);
        	fileRootName = ccJobInputNameRoot + "-" + geomName;
        }
        
        logger.debug("Writing input file for molecule #" 
                    + (i+1) + ": " 
            		+ MolecularUtils.getNameOrID(iac));
        
        List<IAtomContainer> set = new ArrayList<IAtomContainer>();
        set.add(iac);
        produceSingleJobInputFiles(set, 
        		new File(fileRootName+inpExtrension),
        		fileRootName);
        
        return iac;
	}
    
//------------------------------------------------------------------------------

	/**
	 * This worker is special in that the manipulation of multiple atom 
	 * containers may imply that all such atom containers contribute to the
	 * production of single output rather than producing an independent output
	 * each.
	 */
	
	@Override
	public List<IAtomContainer> processAllAtomContainer(List<IAtomContainer> iacs) 
	{
		// NB: do not use inMols, i.e., the field of the AtomContainer superclass
	    logger.info("Writing input file for " + iacs.size() + " geometries");
	    
		if (overwriteGeomNames)
	    {
			for (int molId = 0; molId<iacs.size(); molId++)
	        {
	        	String geomName = geomNames.get(molId);
	        	iacs.get(molId).setTitle(geomName);
	        }
	    }
	    
	    produceSingleJobInputFiles(iacs, 
	    		new File(ccJobInputNameRoot + inpExtrension), ccJobInputNameRoot);
    	
	    return iacs;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Prepare the set of input files of a single job, which may or may not 
     * consist of multiple steps to be performed by a computational chemistry 
     * software tool.
     * @param mols the set of geometries that pertain this single job. Note that
     * in the vast majority of cases there will be only one geometry, or zero, 
     * in which case we do not alter mol-specific settings. This
     * corresponds to multi-geometry mode {@value MultiGeomMode#INDEPENDENTJOBS}.
     * However, jobs do use multiple input geometries within the same job
     * (e.g., transition state searches that start from the geometries of the
     * reactant and product). This is the case of multi-geometry mode 
     * {@value MultiGeomMode#ALLINONEJOB}.
     * <br>
     * <b>WARNING</b>: Changes in the number of electrons or in spin 
     * multiplicity among the geometries are not supported (yet).
     * @param ccJobInput the job's main input file.
     * @param ccJobInputNameRoot the root of the 
     * pathname to any job input file that will be
     * produced. Extensions and suffixed are defined by software specific 
     * constants.
     */
    private void produceSingleJobInputFiles(List<IAtomContainer> mols, 
    		File ccJobInput,	String ccJobInputNameRoot)
    {
    	// We customize a copy of the master job
		CompChemJob molSpecJob = ccJob.clone();
		
		// Define the name's root for any input file created
		molSpecJob.setParameter(WIROConstants.PAROUTFILEROOT, 
				ccJobInputNameRoot, true);
		
		// Add atom coordinates to the so-far possibly molecule-agnostic job
		setChemicalSystem(molSpecJob, mols);
		
		// We keep a copy of the agnostic chemical system definition in the job 
		// parameters
		setChemicalSystemAsJobParam(molSpecJob, mols);
		
		// Add strings/pathnames that are molecular specific. e.g., pathnames or
		// links that are explicitly defined in any part of any input file.
		setSystemSpecificNames(molSpecJob);
		
		if (mols.size()>0)
		{
			// WARNING: changes of charge and spin in multiple geometries
			// must be reflected in the input of each geometry. Here we apply
			// the properties of the first, just in case they have not being set
			// otherwise.
			Integer chargeFromMol = getChargeFromMol(mols.get(0));
			if (chargeFromMol != null)
			{
				setChargeIfUnset(molSpecJob, chargeFromMol+"", omitCharge);
			}
			Integer smFromMol = getSpinMultiplicityFromMol(mols.get(0));
			if (smFromMol != null)
			{
				setSpinMultiplicityIfUnset(molSpecJob, smFromMol +"", omitSpinMult);
			}
    	}
		
		// This call takes care also of the sub-jobs/directives
		molSpecJob.processDirectives(mols, this.getMyJob());
		
		// Ensure a value of charge and spin has been defined
		setChargeIfUnset(molSpecJob, charge+"", omitCharge);
		setSpinMultiplicityIfUnset(molSpecJob, spinMult+"", omitSpinMult);
		
		// Manage output consisting of multiple files and/or folder trees
		ccJobInput = manageOutputFileStructure(mols, ccJobInput);
		
		// Produce the actual main input file
		FileUtils.mustNotExist(ccJobInput);
		IOtools.writeTXTAppend(ccJobInput, 
				getTextForInput(molSpecJob).toString(), false);
		
		// Produce a specific job-details file
		if (writeJobSpecificJDOutput)
		{
			CompChemJob cleanCCJ = molSpecJob.clone();
			cleanCCJ.removeACCTasks();
			File jdFileOut = new File(ccJobInputNameRoot 
					+ WIROConstants.JSONJDEXTENSION);
			FileUtils.mustNotExist(jdFileOut);
			Gson writer = ACCJson.getWriter();
			IOtools.writeTXTAppend(jdFileOut, writer.toJson(cleanCCJ), true);
		}
    }
    
//------------------------------------------------------------------------------
    
    private List<IAtomContainer> makeAtomContainersWithAtomTags(
    		List<IAtomContainer> mols)
    {
    	List<IAtomContainer> molsWithAtomTags = new ArrayList<IAtomContainer>();
    	for (IAtomContainer iac : mols)
    	{
    		molsWithAtomTags.add(MolecularUtils.makeSimpleCopyWithAtomTags(iac));
    	}
    	return molsWithAtomTags;
    }
    
//------------------------------------------------------------------------------
    
    private void setChemicalSystemAsJobParam(CompChemJob job, 
    		List<IAtomContainer> mols)
    {
    	if (mols==null || mols.size()==0)
    		return;
    	AtomContainerSet cSet = new AtomContainerSet();
    	for (IAtomContainer iac : mols)
    	{
    		cSet.addAtomContainer(iac);
    	}
    	job.setParameter(ChemSoftConstants.PARGEOM, cSet);
    }
 
//------------------------------------------------------------------------------
    
    protected static Integer getChargeFromMol(IAtomContainer mol)
    {
    	Integer charge = null;
    	Object pCharge = mol.getProperty(ChemSoftConstants.PARCHARGE);
		if (pCharge != null)
		{
			try {
				charge = Integer.valueOf(pCharge.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pCharge.toString() + "' as charge. Check "
						+ "value of property '" + ChemSoftConstants.PARCHARGE
						+ "'.", -1);
			}
		}
		return charge;
    }
    
//------------------------------------------------------------------------------
    
    protected static Integer getSpinMultiplicityFromMol(IAtomContainer mol)
    {
    	Integer sm = null;
    	Object pSM = mol.getProperty(ChemSoftConstants.PARSPINMULT);
		if (pSM != null)
		{
			try {
				sm = Integer.valueOf(pSM.toString());
			} catch (NumberFormatException e) {
				Terminator.withMsgAndStatus("ERROR! Could not interprete '" 
						+ pSM.toString() + "' as spin multiplicity. Check "
						+ "value of property '" + ChemSoftConstants.PARSPINMULT
						+ "'.", -1);
			}
		}
		return sm;
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
    protected abstract File manageOutputFileStructure(
    		List<IAtomContainer> mols, File output);

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
  	 * {@inheritDoc}
  	 */
  	@Override
  	public StringBuilder getTextForInput(Job job) 
  	{
  		return getTextForInput((CompChemJob)job);
  	}
  	
//------------------------------------------------------------------------------
    
    /**
     * Produced the text that will be printed in the job's main input file, 
     * the one defining what kind of setting the comp.chem. software should use
     * and what exactly to do. Other input needed by the comp.chem. software, 
     * such as the definition of the chemical system's geometry, may or may not 
     * be part of the main input file.
     */
    public abstract StringBuilder getTextForInput(CompChemJob job);
    
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
     * Adds a keyword in a directive with the given name to any step where it is
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
     * Adds data in a directive with the given name to any step where it is
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
     * Adds a value container at any level of embedding in a job directive's 
     * component structure, as long as no such 
     * container is already present on the given address.
     * @param ccj the job to customize.
     * @param address the location where to place the value container in the
     * directive's component structure. Any level of embedding is allowed. This
     * address should not include the component being added.
     * @param container the data container that is to be added.
     */
    public static void addNewValueContainer(CompChemJob ccj, 
    		DirComponentAddress address, IValueContainer container)
    {
    	if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
    			((CompChemJob) stepJob).addNewValueContainer(address, container);
    		}
    	} else {
    		ccj.addNewValueContainer(address, container);
    	}
    }
    
//------------------------------------------------------------------------------

    /**
     * Adds a value container at any level of embedding in a job directive's 
     * component structure. Ignores the potential existence of previously 
     * containers, i.e., does not overwrite existing containers.
     * @param ccj the job to customize.
     * @param address the location where to place the value container in the
     * directive's component structure. Any level of embedding is allowed.
     * @param container the data container that is to be added.
     */
    public static void appendValueContainer(CompChemJob ccj, 
    		DirComponentAddress address, IValueContainer container)
    {
    	if (ccj.getNumberOfSteps()>0)
    	{
    		for (Job stepJob : ccj.getSteps())
    		{
    			((CompChemJob) stepJob).appendValueContainer(address, container);
    		}
    	} else {
    		ccj.appendValueContainer(address, container);
    	}
    }
    
//------------------------------------------------------------------------------

}
