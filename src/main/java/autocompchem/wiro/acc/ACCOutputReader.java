package autocompchem.wiro.acc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.run.Job;
import autocompchem.run.SoftwareId;
import autocompchem.wiro.InputWriter;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for AutoCompChem log files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of an AutoCompChem job. 
 * The rest of the functionality is in the superclass
 * {@link OutputReader}.
 * 
 * @author Marco Foscato
 */
public class ACCOutputReader extends OutputReader
{
	/**
	 * Case insensitive software identifier
	 */
	public static final SoftwareId SOFTWAREID = new SoftwareId("ACC");
	
    /**
     * String defining the task of analyzing ACC output files
     */
    public static final String ANALYSEACCOUTPUTTASKNAME = "analyseACCOutput";

    /**
     * Task about analyzing ACC output files
     */
    public static final Task ANALYSEACCOUTPUTTASK;
    static {
    	ANALYSEACCOUTPUTTASK = Task.make(ANALYSEACCOUTPUTTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYSEACCOUTPUTTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ACCOutputReader();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * No parsing, but this does see beginning of job's steps and normal 
     * termination flag.
     */
    
    @Override
    protected void readLogFile(LogReader reader) throws Exception
    {
        String line = null;
        NamedDataCollector stepData = new NamedDataCollector();
    	int stepInitLineNum = 0;
        int stepId = 0;
        boolean first = true;
        int lineNum = -1;
        while ((line = reader.readLine()) != null)
        {
        	lineNum++;
        	// This is defined in Job.run()
        	if (line.matches("^Initiating " + SoftwareId.ACC + " Job #.*") 
        			|| line.matches("^Initiating " + SoftwareId.SHELL 
        					+ " Job #.*"))
        	{
        		normalTerminated = false;
        		if (first)
        		{
        			first = false;
        		} else {
        			// NB: we do this here because we do not want this to 
        			// be dependent on the "Normal termination" line.
        			
        			storeDataOfOneStep(stepId, stepData, stepInitLineNum, 
        					lineNum-1);
        			
        			// ...clear local storage...
        			stepData = new NamedDataCollector();
                    
        			stepId++;
        			stepInitLineNum = lineNum;
        		}
        	} 
        	// This is defined in ACCMain.main()
        	else if (line.matches("^Final message: Normal Termination$"))
        	{
        		normalTerminated = true;
        	}
        	 
        	// NB: we do not yet parse anything from ACC logs because we do not
        	// yet see any usage case where such parsing would be needed.
        	
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void storeDataOfOneStep(int stepId, NamedDataCollector stepData, 
    		int stepInitLineNum, int stepEndLineNum) 
    		throws CloneNotSupportedException
    {
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAINITLINE,
				stepInitLineNum));
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAENDLINE,
				stepEndLineNum));
		stepsData.put(stepId, stepData.clone());
    }

//------------------------------------------------------------------------------

	@Override
	public Set<FileFingerprint> getOutputFingerprint() 
	{
		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
		conditions.add(new FileFingerprint(".", 2,"^\\s*AutoCompChem\\s*$"));
		conditions.add(new FileFingerprint(".", 3,"^\\s*Version: .*$"));
		conditions.add(new FileFingerprint(".", 4,"^\\**$"));
		conditions.add(new FileFingerprint(".", 8, "^Initiating .* Job .*$"));
	
		return conditions;
	}

//------------------------------------------------------------------------------

	@Override
	public SoftwareId getSoftwareID() {
		return SOFTWAREID;
	}

//------------------------------------------------------------------------------

	@Override
	public InputWriter getSoftInputWriter() {
		return new ACCInputWriter();
	}
		
//-----------------------------------------------------------------------------
    
}
