package autocompchem.molecule;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.IOtools.IACOutFormat;
import autocompchem.run.Job;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;


/**
 * General {@link Worker} that processes atom containers definitions as input. 
 * The nature of the processing is defined in subclasses of this class, but 
 * this class ensures that the containers resulting from the processing, if
 * any processing is done, are exposed so that they become usable outside this 
 * class. See {@link Worker#exposeOutputData(NamedData)}.
 * 
 * @author Marco Foscato
 */

public class AtomContainerInputProcessor extends Worker
{
    
    /**
     * The input file, i.e., any molecular structure file to read from.
     */
    protected File inFile;
   
    /**
     * The output file, i.e., any molecular structure file to wrote into.
     */
    protected File outFile;
    
    /**
     * Flag indicated whether the main output file has already been used for 
     * something else and should not be used for reporting the processed atom
     * container.
     */
    protected boolean outFileAlreadyUsed = false;
    
    /**
     * Format of the output file
     */
    protected String outFormat = "SDF";

	/**
	 * Flag to limit to the last output when sets of output structures are available.
	 */
	protected boolean onlyLastOne = false;

    /**
     * The input molecules
     */
    protected List<IAtomContainer> inMols;
    
    /**
     * List of properties to set on the processed atom containers.
     * Since these properties may change during the execution of this worker's 
     * job, we set them twice: first upon reading in the input atom containers, 
     * and then when saving the results.
     */
    protected Map<String,String> iacPropertiesToAdd = new HashMap<String,String>();
    
    /**
     * The list of resulting data
     */
    protected List<Object> output = new ArrayList<Object>();
    
    /**
     * The index of the chosen geometry to work with when input consists of 
     * multiple geometries.
     */
    protected Integer chosenGeomIdx;
    
    /**
     * Definition of how to use multiple geometries
     */
    public enum MultiGeomMode {INDEPENDENTJOBS, ALLINONEJOB};
    
    /**
     * Chosen mode of handling multiple geometries.
     */
    protected MultiGeomMode multiGeomMode = MultiGeomMode.INDEPENDENTJOBS;

    /**
     * String defining the task of generating tuples of atoms
     */
    public static final String READIACSTASKNAME = "readAtomContainers";

    /**
     * Task about importing a container of atoms
     */
    public static final Task READIACSTASK;
    static {
    	READIACSTASK = Task.make(READIACSTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(READIACSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomContainerInputProcessor.json";
    }
    
//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job) {
		return new AtomContainerInputProcessor();
	}
    
//------------------------------------------------------------------------------
	
    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

	@Override
    public void initialize()
    {
    	super.initialize();

        if (params.contains(ChemSoftConstants.PARGEOM))
        {
            inMols = new ArrayList<IAtomContainer>();
            Object geomValue = params.getParameter(ChemSoftConstants.PARGEOM).getValue();
            
            if (geomValue instanceof AtomContainerSet)
            {
                ((AtomContainerSet) geomValue).atomContainers().forEach(i -> inMols.add(i));
            }
            else if (geomValue instanceof IAtomContainer)
            {
                inMols.add((IAtomContainer) geomValue);
            }
            else
            {
                throw new IllegalArgumentException("Parameter " + ChemSoftConstants.PARGEOM 
                        + " must be either IAtomContainer or AtomContainerSet, but got: " 
                        + geomValue.getClass().getName());
            }
           
            if (params.contains(WorkerConstants.PARINFILE) || params.contains(ChemSoftConstants.PARGEOMFILE))
            {
				String existing = "";
				if (params.contains(WorkerConstants.PARINFILE))
				{
					existing = WorkerConstants.PARINFILE;
				}
				if (params.contains(ChemSoftConstants.PARGEOMFILE))
				{
					existing = ChemSoftConstants.PARGEOMFILE;
				}

            	logger.warn("WARNING: found both "
            			+ existing + " and "
            			+ ChemSoftConstants.PARGEOM + ". Using geometries from "
            			+ existing + " as input for "
            			+ this.getClass().getSimpleName() + ".");
            	this.inFile = null;
            }
        }

        if (params.contains(WorkerConstants.PARINFILE))
		{
			String value = params.getParameter(WorkerConstants.PARINFILE)
					.getValueAsString();
			String format = null;
			if (params.contains(WorkerConstants.PARINFORMAT))
			{	
				format = params.getParameter(WorkerConstants.PARINFORMAT)
						.getValueAsString();
			}
			processInputFileParameter(value, format);
		}

		if (params.contains(ChemSoftConstants.PARGEOMFILE))
		{
			String value = params.getParameter(ChemSoftConstants.PARGEOMFILE)
					.getValueAsString();
			String format = null;
			if (params.contains(WorkerConstants.PARINFORMAT))
			{	
				format = params.getParameter(WorkerConstants.PARINFORMAT)
						.getValueAsString();
			}
			processInputFileParameter(value, format);

			if (params.contains(WorkerConstants.PARINFILE))
			{
				logger.warn("WARNING: found both "
						+ WorkerConstants.PARINFILE + " and "
						+ ChemSoftConstants.PARGEOMFILE + ". Using geometries from "
						+ ChemSoftConstants.PARGEOMFILE + " as input for "
						+ this.getClass().getSimpleName() + ".");
				this.inFile = null;
			}
		}
        
        if (params.contains(ChemSoftConstants.PARMULTIGEOMMODE))
        {
            String value = params.getParameter(
            		ChemSoftConstants.PARMULTIGEOMMODE).getValueAsString();
            multiGeomMode = EnumUtils.getEnumIgnoreCase(
            		MultiGeomMode.class, value);
        }
        
        if (params.contains(ChemSoftConstants.PARMULTIGEOMID))
        {
			String value = params.getParameter(
					ChemSoftConstants.PARMULTIGEOMID).getValueAsString();
			if (NumberUtils.isParsableToInt(value))
			{
				chosenGeomIdx = Integer.parseInt(value);
			} else if(value.toUpperCase().equals("LAST")) {
				chosenGeomIdx = inMols.size() - 1;
			} else {
				throw new IllegalArgumentException("Unable to "
						+ "understand option '" + value + "' for "
						+ ChemSoftConstants.PARMULTIGEOMID + ". Check your input."); 
			}

        	if (multiGeomMode!=MultiGeomMode.INDEPENDENTJOBS)
        	{
	        	multiGeomMode = MultiGeomMode.INDEPENDENTJOBS;
	        	logger.warn("WARNING: found parameter "
	        			+ ChemSoftConstants.PARMULTIGEOMID + ". Ignoring any "
	        			+ "value given for "
	        			+ ChemSoftConstants.PARMULTIGEOMMODE + " and setting "
	        			+ "multiGeomMode to " + multiGeomMode + ".");
        	}
        }
        
        if (params.contains(ChemSoftConstants.PARSETIACPROPERTIES))
        {
        	String text = params.getParameter(
        			ChemSoftConstants.PARSETIACPROPERTIES).getValueAsString();
        	parsePropertiesToSet(text);
        }
        
        if (params.contains(WorkerConstants.PAROUTFILE))
        {
	        this.outFile = getNewFile(params.getParameter(
	        		WorkerConstants.PAROUTFILE).getValueAsString());
	        FileUtils.mustNotExist(this.outFile);
	        String ext = FileUtils.getFileExtension(outFile);
	        if (ext != null)
	        {
	        	ext = ext.replaceFirst("\\.","");
		        if (EnumUtils.isValidEnum(IACOutFormat.class, 
		        		ext.toUpperCase()))
		        {
		        	this.outFormat = ext.toUpperCase();
		        }
	        }
        } else {
        	if (!params.contains(WorkerConstants.PARNOOUTFILEMODE))
        	{
	        	logger.debug("WARNING: No " + WorkerConstants.PAROUTFILE 
	        			+ " parameter given to " 
	        			+ this.getClass().getSimpleName() + ". "
	        			+ "Results will be kept in the memory for further use.");
        	}
        }
        
        if (params.contains(WorkerConstants.PAROUTFORMAT))
        {
        	this.outFormat = params.getParameter(
        			WorkerConstants.PAROUTFORMAT).getValueAsString();
        }

		if (params.contains(WorkerConstants.PARONLYLASTSTRUCTURE))
		{
			this.onlyLastOne = true;
		}
        
        // If no other input channel is used, then get input from memory of 
        // previous step
        if (inMols==null)
        {
        	inMols = new ArrayList<IAtomContainer>();
        	NamedData dataFromPrevStep = getOutputOfPrevStep(
        			ChemSoftConstants.JOBDATAGEOMETRIES, true);
        	if (dataFromPrevStep != null)
        	{
        		AtomContainerSet prevData = (AtomContainerSet) 
        				dataFromPrevStep.getValue();
            	prevData.atomContainers().forEach(i -> inMols.add(i));
        	} else {
        		logger.debug("No previous data to process. Running '" 
        				+ this.getClass().getSimpleName() 
        				+ "' without any input atom container.");
        	}
        }
        
        // We set properties here upon initialization, but also when writing 
        // the output to allow for properties computed within the job of this 
        // worker to be added after the worker has done its job.
        if (inMols!=null)
        {
        	for (IAtomContainer iac : inMols)
        	{
        		for (Entry<String, String> e : iacPropertiesToAdd.entrySet())
        		{
        			iac.setProperty(e.getKey(), e.getValue());
        		}
        	}
        }
    }
	
//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining what properties to set to the 
     * atom containers.
     * @param text the text (i.e., multiple lines) to be parsed.
     */

    protected void parsePropertiesToSet(String text)
    {
    	// NB: the REGEX makes this compatible with either new-line character
        String[] arr = text.split("\\r?\\n|\\r");
        parsePropertiesToSet(new ArrayList<String>(Arrays.asList(arr)));
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining what properties to set to the 
     * atom containers.
     * @param lines the lines of text to be parsed.
     */

    protected void parsePropertiesToSet(List<String> lines)
    {
        for (String line : lines)
        {
        	String[] nameValueParts = line.split(":", 2);
    		String propertyName = line.stripLeading().stripTrailing();
    		String propertyValue = "";
        	if (nameValueParts.length>0)
        	{
        		propertyName = nameValueParts[0].stripLeading().stripTrailing();
        		if (nameValueParts.length>1)
            	{
        			propertyValue = nameValueParts[1].stripLeading().stripTrailing();
            	}
        	}
        	iacPropertiesToAdd.put(propertyName, propertyValue);
        }
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Processes the input file parameter reading chemical structures from the 
	 * file. NB: if we are reading a huge file, this code will cause problems, 
	 * but we can assume no huge file will be read here.
	 * @param value the value of the parameter.
	 */

	protected void processInputFileParameter(String value)
	{
		processInputFileParameter(value, null);
	}

//-----------------------------------------------------------------------------
    
	/**
	 * Processes the input file parameter reading chemical structures from the 
	 * file. NB: if we are reading a huge file, this code will cause problems, 
	 * but we can assume no huge file will be read here.
	 * @param value the value of the parameter.
	 * @param format the format of the file to be read. If <code>null</code>, 
	 * we infer it from the extension.
	 */
	protected void processInputFileParameter(String value, String format)
	{
        String[] words = value.trim().split("\\s+");
        String pathname = words[0];
        inFile = getNewFile(pathname);
        FileUtils.foundAndPermissions(inFile,true,false,false);
        
        List<IAtomContainer> iacs = IOtools.readMultiMolFiles(inFile, format);
        inMols = new ArrayList<IAtomContainer>();
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
        				this.inMols.add(iacs.get(iacs.size()-1));
        			} else {
                    	throw new IllegalArgumentException("Found request "
                    			+ "to take geometry " + id + " from '"
                    			+ pathname + "' but found "+ iacs.size() 
                    			+ " geometries. Check your input."); 
        			}
        		} else if ("LAST".equals(idStr.toUpperCase())) {
        			this.inMols.add(iacs.get(iacs.size()-1));
        		} else {
        			throw new IllegalArgumentException("Unable to "
                			+ "understand option '" + idStr + "' for "
                			+ ChemSoftConstants.PARGEOMFILE 
                			+ ". Check your input."); 
        		}
        	}
        } else {
        	this.inMols = iacs;
        }
	}
    
//-----------------------------------------------------------------------------

	@Override
	public void performTask() {
		processInput();
	}
   
//------------------------------------------------------------------------------

    /**
     * Goes through the input according to the settings available to this
     * instance.
     */
    protected void processInput()
    {
    	// We must ensure inMols != null when inFile is null
        if (inFile==null && (inMols==null || inMols.size()==0))
        {
            throw new IllegalArgumentException("Missing parameter defining the "
            		+ "input geometries (" + ChemSoftConstants.PARGEOM + ") or "
            		+ "an input file to read geometries from. "
            		+ "No input to task " + task + " from "
            		+ this.getClass().getSimpleName() + ".");
        }

        switch (multiGeomMode)
		{
		case INDEPENDENTJOBS:
			{
		        boolean breakAfterThis = false;
	        	for (int i=0; i<inMols.size(); i++)
	            {
	            	if (chosenGeomIdx!=null)
		            {
	            		if (i==chosenGeomIdx)
	            		{
			        		breakAfterThis = true;
	            		} else {
	            			continue;
	            		}
		            }
	            	IAtomContainer iac = inMols.get(i);
	      			if (inMols.size()>1)
	            	{
	      				logger.info("#" + i + " " + MolecularUtils.getNameOrID(
	      						iac));
	            	}
	            	IAtomContainer result = processOneAtomContainer(iac, i);
	            	
	            	// Finalize output
	            	setPropertiesToOutgoingIAC(result);
	                if (exposedOutputCollector != null)
	                {
	    	        	exposeAtomContainer(result);
	                }
	                if (!outFileAlreadyUsed)
	                	tryWritingToOutfile(result);
	                
	            	if (breakAfterThis)
	            		break;
	            }
			}
			break;
			
		case ALLINONEJOB:
			{
				AtomContainerSet results = new AtomContainerSet();
				List<IAtomContainer> iacs = processAllAtomContainer(inMols);
				iacs.forEach(iac -> setPropertiesToOutgoingIAC(iac));
				iacs.forEach(iac -> results.addAtomContainer(iac));
	            if (exposedOutputCollector != null)
	            {
		        	exposeAtomContainers(results);
	            }
                if (!outFileAlreadyUsed)
                	tryWritingToOutfile(iacs);
			}
		}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to write to the file defined by the OUTFILE parameter, if defined
     * and if not already used.
     * @param iac the atom container to write.
     */
    public void tryWritingToOutfile(IAtomContainer iac)
    {
        if (outFile!=null)
        {
			IOtools.writeAtomContainerToFile(outFile, iac, outFormat, true);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to write to the file defined by the OUTFILE parameter, if defined
     * and if not already used.
     * @param iacs the atom containers to write.
     */
    public void tryWritingToOutfile(IAtomContainerSet iacs)
    {
        if (outFile!=null)
        {
			if (onlyLastOne && iacs.getAtomContainerCount()>0)
			{
				IOtools.writeAtomContainerToFile(outFile, iacs.getAtomContainer(
					iacs.getAtomContainerCount()-1), outFormat, true);
			} else {
				IOtools.writeAtomContainerSetToFile(outFile, iacs, outFormat, true);
			}
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to write to the file defined by the OUTFILE parameter, if defined
     * and if not already used.
     * @param iacs the atom containers to write.
     */
    public void tryWritingToOutfile(List<IAtomContainer> iacs)
    {
        if (outFile!=null)
        {
			if (onlyLastOne && iacs.size()>0)
			{
				IOtools.writeAtomContainerToFile(outFile, iacs.get(iacs.size()-1), outFormat, true);
			} else {
				IOtools.writeAtomContainerSetToFile(outFile, iacs, outFormat, true);
			}
        }
    }
    
//------------------------------------------------------------------------------
    
    private void setPropertiesToOutgoingIAC(IAtomContainer iac)
    {
    	// We may need to update the values, if any of the properties requires
    	// values from the job of this worker
    	if (myJob==null)
    	{
    		return;
    	}
    	
    	// We work only on the parameters that need updating
    	NamedDataCollector dataToUpdate = new NamedDataCollector();
    	for (Entry<String, String> e : iacPropertiesToAdd.entrySet())
		{
    		if (e.getValue().toUpperCase().contains(Job.GETACCJOBSRESULTS))
    		{
    			dataToUpdate.putNamedData(new NamedData(e.getKey(),e.getValue()));
    		}
		}

    	myJob.updateValuesFromJobsTree(dataToUpdate, Job.GETACCJOBSRESULTS);

		for (Entry<String, NamedData> e : dataToUpdate.getAllNamedData().entrySet())
		{
			iac.setProperty(e.getKey(), e.getValue().getValueAsString());
		}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Takes the given {@link AtomContainer} and does nothing else to it than
     * returning it.
     * Subclasses should overwrite this method to do what they want with the 
     * given {@link AtomContainer} and produce what they like. Any output data 
     * should be placed placed in the {@link Worker#exposedOutputCollector} by
     * using {@link Worker#exposeOutputData(NamedData)}.
     * @param iac the atom container to be processed
     * @param i the index of this atom container. This is usually used for 
     * logging purposes and creation of labels specific to the atom container.
     */
    public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i)
    {
    	exposeOutputData(new NamedData(READIACSTASK.ID+i,iac));
    	return iac;
    }
    
//------------------------------------------------------------------------------

  	/**
  	 * Takes the given list of {@link AtomContainer}s and processes one item at
  	 * the time independently by sending it to
  	 * {@link #processOneAtomContainer(IAtomContainer, int)}.
     * Subclasses should overwrite this method to do what they want with the 
     * given list and produce what they like as output. Any output data 
     * should be placed placed in the {@link Worker#exposedOutputCollector} by
     * using {@link Worker#exposeOutputData(NamedData)}.
     * @param iacs the list of atom containers to be processed.
  	 */
    
  	public List<IAtomContainer> processAllAtomContainer(
  			List<IAtomContainer> iacs) 
  	{   
  		List<IAtomContainer>  outputList = new ArrayList<IAtomContainer>();
  		for (int molId = 0; molId<iacs.size(); molId++)
        {
  			IAtomContainer iac = iacs.get(molId);
  			if (iacs.size()>1)
  			{
  				logger.info("#" + molId + " " + MolecularUtils.getNameOrID(iac));
  			}
  			outputList.add(processOneAtomContainer(iac, molId));
  		}
  		return outputList;
    }
  	
//------------------------------------------------------------------------------
  	
  	/**
  	 * Put the given atom container in the exposed output named, by internal 
  	 * convention, according to {@link ChemSoftConstants#JOBDATAGEOMETRIES}
  	 * 
  	 * @param iacs the collection of atom containers to expose
  	 */
  	protected void exposeAtomContainer(IAtomContainer iac)
  	{
    	AtomContainerSet iacs = new AtomContainerSet();
    	iacs.addAtomContainer(iac);
    	exposeAtomContainers(iacs);
  	}
  	
//------------------------------------------------------------------------------
    
  	/**
  	 * Put the given atom containers in the exposed output named, by internal 
  	 * convention, according to {@link ChemSoftConstants#JOBDATAGEOMETRIES}
  	 * 
  	 * @param iacs the collection of atom containers to expose
  	 */
  	protected void exposeAtomContainers(AtomContainerSet iacs)
  	{
        exposeOutputData(new NamedData(ChemSoftConstants.JOBDATAGEOMETRIES,
        		iacs));
  	}
  	
//------------------------------------------------------------------------------

}
