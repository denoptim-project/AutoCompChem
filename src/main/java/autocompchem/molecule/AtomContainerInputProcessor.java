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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.IOtools.IACOutFormat;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


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
     * The input molecules
     */
    protected List<IAtomContainer> inMols;
    
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

    public void initialize()
    {
    	super.initialize();
        if (params.contains(ChemSoftConstants.PARINFILE))
        {
        	String value = params.getParameter(ChemSoftConstants.PARINFILE)
            		.getValueAsString();
        	processInputFileParameter(value);
        }
        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
        	String value = params.getParameter(ChemSoftConstants.PARGEOMFILE)
            		.getValueAsString();
        	processInputFileParameter(value);
        }
        if (params.contains(ChemSoftConstants.PARGEOM))
        {
            inMols = new ArrayList<IAtomContainer>();
            ((AtomContainerSet) params.getParameter(ChemSoftConstants.PARGEOM)
            		.getValue()).atomContainers().forEach(i -> inMols.add(i));
           
            if (params.contains("INFILE"))
            {
            	logger.warn("WARNING: found both "
            			+ ChemSoftConstants.PARINFILE + " and "
            			+ ChemSoftConstants.PARGEOM + ". Using geometries from "
            			+ ChemSoftConstants.PARGEOM + " as input for "
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
        	chosenGeomIdx = Integer.parseInt(params.getParameter(
        			ChemSoftConstants.PARMULTIGEOMID).getValueAsString());
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
        
        if (params.contains(ChemSoftConstants.PAROUTFILE))
        {
	        this.outFile = new File(params.getParameter(
	        		ChemSoftConstants.PAROUTFILE).getValueAsString());
	        FileUtils.mustNotExist(this.outFile);
	        String ext = FileUtils.getFileExtension(outFile)
	        		.replaceFirst("\\.","");
	        if (ext != null && EnumUtils.isValidEnum(IACOutFormat.class, 
	        		ext.toUpperCase()))
	        {
	        	this.outFormat = ext.toUpperCase();
	        }
        } else {
        	logger.debug("WARNING: No " + ChemSoftConstants.PAROUTFILE 
        			+ " parameter given. "
        			+ "Results will be kept in the memory for further use.");
        }
        
        if (params.contains(ChemSoftConstants.PAROUTFORMAT))
        {
        	this.outFormat = params.getParameter(
        			ChemSoftConstants.PAROUTFORMAT).getValueAsString();
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
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * NB: if we are reading a huge file, this code will cause problems, but
	 * we can assume no huge file will be read here.
	 */
	protected void processInputFileParameter(String value)
	{
        String[] words = value.trim().split("\\s+");
        String pathname = words[0];
        FileUtils.foundAndPermissions(pathname,true,false,false);
        inFile = new File(pathname);
        
        List<IAtomContainer> iacs = IOtools.readMultiMolFiles(inFile);
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
                    	Terminator.withMsgAndStatus("ERROR! Found request "
                    			+ "to take geometry " + id + " from '"
                    			+ pathname + "' but found "+ iacs.size() 
                    			+ " geometries. Check your input.",-1); 
        			}
        		} else if ("LAST".equals(idStr.toUpperCase())) {
        			this.inMols.add(iacs.get(iacs.size()-1));
        		} else {
        			Terminator.withMsgAndStatus("ERROR! Unable to "
                			+ "understand option '" + idStr + "' for "
                			+ ChemSoftConstants.PARGEOMFILE 
                			+ ". Check your input.",-1); 
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
     * @param outputStorage the collector of output data
     */
    protected void processInput()
    {
    	// We must ensure inMols != null when inFile is null
        if (inFile==null && inMols==null)
        {
            Terminator.withMsgAndStatus("ERROR! Missing parameter defining the "
            		+ "input geometries (" + ChemSoftConstants.PARGEOM + ") or "
            		+ "an input file to read geometries from. "
            		+ "Cannot perform task " + task + " from "
            		+ this.getClass().getSimpleName() + ".", -1);
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
	                if (exposedOutputCollector != null)
	                {
	    	        	exposeAtomContainer(result);
	                }
	                if (outFile!=null && !outFileAlreadyUsed)
	                {
	        			IOtools.writeAtomContainerToFile(outFile, result, outFormat, true);
	                }
	                
	            	if (breakAfterThis)
	            		break;
	            }
			}
			break;
			
		case ALLINONEJOB:
			{
				AtomContainerSet results = new AtomContainerSet();
				List<IAtomContainer> iacs = processAllAtomContainer(inMols);
				iacs.forEach(iac -> results.addAtomContainer(iac));
	            if (exposedOutputCollector != null)
	            {
		        	exposeAtomContainers(results);
	            }
                if (outFile!=null && !outFileAlreadyUsed)
                {
        			IOtools.writeAtomContainerSetToFile(outFile, iacs, outFormat, true);
                }
			}
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
