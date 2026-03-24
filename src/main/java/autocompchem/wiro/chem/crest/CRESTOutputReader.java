package autocompchem.wiro.chem.crest;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.SoftwareId;
import autocompchem.wiro.ITextualInputWriter;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for CREST output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of CREST. The rest of the functionality is in the superclass
 * {@link ChemSoftOutputReader}.
 * 
 * @author Marco Foscato
 */
public class CRESTOutputReader extends ChemSoftOutputReader
{
	/**
	 * Case insensitive software identifier
	 */
	public static final SoftwareId SOFTWAREID = new SoftwareId("CREST");
	
    /**
     * String defining the task of analyzing CREST output files
     */
    public static final String ANALYSECRESTOUTPUTTASKNAME = "analyseCRESTOutput";

    /**
     * Task about analyzing CREST output files
     */
    public static final Task ANALYSECRESTOUTPUTTASK;
    static {
    	ANALYSECRESTOUTPUTTASK = Task.make(ANALYSECRESTOUTPUTTASKNAME);
    }
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYSECRESTOUTPUTTASK)));
    }

//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "inputdefinition/CRESTOutputReader.json";
	}

//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job) {
		return new CRESTOutputReader();
	}

//------------------------------------------------------------------------------
    
    /**
     * Method that parses the given log file from CREST and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    protected void readLogFile(LogReader reader) throws Exception
    {		
        String line = null;
        while ((line = reader.readLine()) != null)
        {
        	if (line.matches(".*" + CRESTConstants.LOGNORMALTERM+ ".*"))
        	{
        		normalTerminated = true;
        	}
		}
		
		// Extract the pathname of the log file: we use it to find data 
    	//printed on other files
		String path = "";
		if (inFile.getParent() != null)
		{
        	path = inFile.getParent() + System.getProperty(
        			"file.separator");
		}

		// Resulting conformates are writtes in a file wiht hard-coded name
		File conformersXYZ = getNewFile(path + "crest_conformers.xyz");
        AtomContainerSet conformers = new AtomContainerSet();
		if (conformersXYZ.exists())
		{
			List<IAtomContainer> mols = IOtools.readXYZ(conformersXYZ);
		
			for (IAtomContainer mol : mols)
			{
				conformers.addAtomContainer(mol);
			}

			logger.info("Found " + mols.size() + " conformers");
		} else {
			logger.warn("No conformers found (missing file: " 
			    + conformersXYZ.getAbsolutePath() + ")");
		}

		NamedDataCollector results = new NamedDataCollector();
		results.putNamedData(new NamedData(
			ChemSoftConstants.JOBDATAGEOMETRIES,
			conformers));
		
		stepsData.put(0, results);
	}
    
//------------------------------------------------------------------------------

  	@Override
  	protected Set<FileFingerprint> getOutputFingerprint() 
  	{
  		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
  		conditions.add(new FileFingerprint(".", 10, 
  				"^\\s*║\\s*Conformer-Rotamer Ensemble Sampling Tool\\s*║\\s*$"));
  		// NB: we allow to consider the working directory as output as CREST 
  		// creates many useful files in the work space and with conventional 
  		// names.
  		conditions.add(new FileFingerprint("./*", 10, 
  				"^\\s*║\\s*Conformer-Rotamer Ensemble Sampling Tool\\s*║\\s*$"));
  		return conditions;
  	}

//------------------------------------------------------------------------------

  	@Override
	public SoftwareId getSoftwareID() {
		return SOFTWAREID;
  	}

//------------------------------------------------------------------------------

  	@Override
  	protected ITextualInputWriter getSoftInputWriter() {
  		return new CRESTInputWriter();
  	}
  	
//-----------------------------------------------------------------------------
    
}
