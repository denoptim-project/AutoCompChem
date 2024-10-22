package autocompchem.wiro;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.SoftwareId;
import autocompchem.run.Terminator;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;

/**
 * Core components of any worker writing input files for any software 
 * packages that does not read a chemical system as input. For the latter,
 * see {@link ChemSoftInputWriter}.
 * This class is not meant to ever be chosen as the worker charged to perform a
 * task. It only provides general purpose functionality to purpose specific 
 * implementations.
 *
 * @author Marco Foscato
 */

public class InputWriter extends Worker implements ITextualInputWriter
{
    /**
     * Pathname root for output files (i.e., the input for the other software).
     */
    protected String outFileNameRoot;
    
    /**
     * Output name (input for the other software).
     */
    protected File outFile;
    
    /**
     * Flag deciding if we write the specific job-details file or not.
     */
    private boolean writeJobSpecificJDOutput = true;
    
    /**
     * The job we want to prepare the input for.
     */
    protected Job jobToInput;
    
    /**
     * Default extension of the software input file
     */
    protected String inpExtrension;

    /**
     * Default extension of the software output file
     */
    protected String outExtension;

    /**
     * String defining the task of preparing input
     */
    public static final String PREPAREINPUTTASKNAME = "prepareInput";

    /**
     * Task about preparing input files
     */
    public static final Task PREPAREINPUTTASK;
    static {
    	PREPAREINPUTTASK = Task.make(PREPAREINPUTTASKNAME);
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public InputWriter()
    {}
    
//------------------------------------------------------------------------------

    @Override
	public String getKnownInputDefinition() {
		return "inputdefinition/InputWriter.json";
	}
	
//------------------------------------------------------------------------------

    /**
     * Returns <code>null</code> as this implementation is not meant to ever be 
     * chosen as the worker charged to perform a task. 
     * It only provides general purpose functionality to purpose specific 
     * implementations.
     */
   	@Override
  	public Set<Task> getCapabilities() {
		return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(PREPAREINPUTTASK)));
  	}

//-----------------------------------------------------------------------------

 	@Override
  	public Worker makeInstance(Job job) 
 	{
		if (job==null)
		{
			// This happens when requesting the generation of help message
			return new InputWriter();
		}
		
		if (!job.hasParameter(WorkerConstants.PARTASK))
		{
			String taskStr = job.getParameter(WorkerConstants.PARTASK)
					.getValueAsString();
	    	if (Task.make(taskStr) != PREPAREINPUTTASK)
	    	{
				logger.warn("WARNING: attempt to make a " 
						+ ChemSoftInputWriter.class.getSimpleName() 
						+ " for task '" + taskStr + "', but this is not allowed.");
				return new InputWriter();
	    	}
		}
    	
    	if (!job.hasParameter(WIROConstants.SOFTWAREID))
		{
			logger.warn("WARNING: cannot detect the type of "
					+ "software for which to prepare an input. "
					+ "Make sure the parameter '" 
					+ WIROConstants.SOFTWAREID + "' is given.");
			return new InputWriter();
		}    	
    	SoftwareId softwareID = new SoftwareId(job.getParameter(
    			WIROConstants.SOFTWAREID).getValueAsString());
    	
		ReaderWriterFactory builder = 
				ReaderWriterFactory.getInstance();
		return builder.makeInstanceInputWriter(softwareID);
  	}
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    public void initialize()
    {
    	super.initialize();
       
    	if (params.contains(WIROConstants.PARJOBDETAILSFILE))
        {
            File jdFile = new File(params.getParameter(
                    WIROConstants.PARJOBDETAILSFILE).getValueAsString());
            logger.debug("Job details from JD file '" + jdFile + "'.");
            
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.jobToInput = JobFactory.buildFromFile(jdFile);
        } else if (params.contains(WIROConstants.PARJOBDETAILSOBJ)) {
        	this.jobToInput = (Job) params.getParameter(
                    WIROConstants.PARJOBDETAILSOBJ).getValue();
        } else {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                    + "Neither '" + WIROConstants.PARJOBDETAILSFILE
                    + "' nor '" + WIROConstants.PARJOBDETAILSOBJ 
                    + "'found in parameters.",-1);
        }

        if (params.contains(WIROConstants.PAROUTFILEROOT))
        {
            outFileNameRoot = params.getParameter(
                    WIROConstants.PAROUTFILEROOT).getValueAsString();
            outFile = new File(outFileNameRoot + inpExtrension);
        } else if (params.contains(WIROConstants.PAROUTFILE))
        {
        	outFile = new File(params.getParameter(
        			WIROConstants.PAROUTFILE).getValueAsString());
            outFileNameRoot = FileUtils.getRootOfFileName(outFile);
            if (outFile.getParentFile()!=null)
            {
            	outFileNameRoot = outFile.getParentFile().getAbsolutePath()
            			+ File.separator + outFileNameRoot;
            }
        } else {
    		outFileNameRoot = "softinput";
            logger.debug("Neither '" 
        				 + WIROConstants.PAROUTFILE + "' nor '" 
        				 + WIROConstants.PAROUTFILEROOT + "' found. " + NL
                         + "Root of any output file name set to '" 
                         + outFileNameRoot + "'.");
        	outFile = new File(outFileNameRoot + inpExtrension);
        }
        
        if (params.contains(WIROConstants.PARNOJSONOUTPUT))
        {
        	writeJobSpecificJDOutput = false;
        }
    }
 
//------------------------------------------------------------------------------
    
    /**
     * Produced the text that will be printed in the job's main input file, 
     * the one defining what kind of task and setting the software should use
     * to do its job.
     */
    @Override
    public StringBuilder getTextForInput(Job job)
    {
		StringBuilder sb = new StringBuilder();
		Gson writer = ACCJson.getWriter();
    	return sb.append(writer.toJson(job));
    }
    
//------------------------------------------------------------------------------

	@Override
	public void performTask() {
		IOtools.writeTXTAppend(outFile, getTextForInput(jobToInput).toString(),
				false);
		if (writeJobSpecificJDOutput)
		{
			IOtools.writeJobToJSON(jobToInput, new File(
					outFileNameRoot + WIROConstants.JSONJDEXTENSION));
		}
	}
	
//------------------------------------------------------------------------------

}
