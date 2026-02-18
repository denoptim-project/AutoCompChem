package autocompchem.wiro.chem.spartan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond.Order;

import autocompchem.atom.AtomUtils;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileFingerprint;
import autocompchem.files.FileUtils;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.SoftwareId;
import autocompchem.wiro.ITextualInputWriter;
import autocompchem.wiro.WIROConstants;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for Spartan output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of Spartan. 
 * The rest of the functionality is in the superclass
 * {@link ChemSoftOutputReader}.
 * 
 * @author Marco Foscato
 */
public class SpartanOutputReader extends ChemSoftOutputReader
{
	/**
	 * Case insensitive software identifier
	 */
	public static final SoftwareId SOFTWAREID = new SoftwareId("Spartan");
	
    /**
     * String defining the task of analyzing Spartan output files, which are 
     * actually folders.
     */
    public static final String ANALYSESPARTANOUTPUTTASKNAME = 
    		"analyseSpartanOutput";

    /**
     * Task about analyzing Spartan output files/folders
     */
    public static final Task ANALYSESPARTANOUTPUTTASK;
    static {
    	ANALYSESPARTANOUTPUTTASK = Task.make(ANALYSESPARTANOUTPUTTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYSESPARTANOUTPUTTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new SpartanOutputReader();
    }

//------------------------------------------------------------------------------

    /**
     * This override is to process  the input to detect the model name
     * when there is only one model to analyze.
     */

    public void initialize()
    {
    	super.initialize();
    	
    	if (!inFile.isDirectory())
    	{
    		throw new IllegalArgumentException(
    				this.getClass().getSimpleName() + " requires the "
    				+ "value of " + WIROConstants.PARJOBOUTPUTFILE 
    				+ " to be a pathname to a Spartan directory, but '"
    				+ inFile + "' is not.");
    	}
    	
    	List<File> allModelDirs = FileUtils.findByGlob(inFile, "*", true)
    			.stream()
    			.filter(f -> f.isDirectory())
    			.collect(Collectors.toList());
    	if (selectedMolID==null)
    	{	
	    	if (allModelDirs.size() == 1)
			{
				inFile = allModelDirs.get(0);
			}
    	} else {
    		// This condition is to avoid adding the problems of a redundant 
    		// info on model identifier both as parameter AND in the pathname 
    		// leading to a model-specific folder (which does NOT contain any 
    		// subfolfers)
    		if (allModelDirs.size()>1)
			{
				inFile = new File(inFile + File.separator + selectedMolID);
			}
    	}
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/SpartanOutputReader.json";
    }
      
//------------------------------------------------------------------------------
  	
    @Override
    protected List<File> getSubModelOutputFiles()
  	{
    	List<File> sumModels = new ArrayList<File>();
  		if (inFile.isDirectory())
    	{
  			List<File> allDirectories = getSpartanSubModelDirectories();
  			if (allDirectories.size()==1)
  			{
  				// This is a multi-model container that contains only one model
  				// so, we fall back into the single model scenario by changing 
  				// the pathname to the file accordingly.
  				inFile =allDirectories.get(0);
  				// but we still return an empty list, as not to trigger the 
  				// launching of model-specific output readers
  			} else if (allDirectories.size()>1)
    		{
    			sumModels = allDirectories;
    		}
    	}
  		return sumModels;
  	}
    
//------------------------------------------------------------------------------
    
  	private List<File> getSpartanSubModelDirectories()
  	{
		List<File> allFiles = FileUtils.findByGlob(inFile, "*", true);
		return allFiles.stream()
			.filter(f -> f.isDirectory())
			.collect(Collectors.toList());
  	}
    
//------------------------------------------------------------------------------

    @Override
    public File getLogPathName()
    {
    	if (inFile.isDirectory())
    	{
    		return new File(inFile.getAbsoluteFile() + File.separator 
	    			+ SpartanConstants.OUTPUTFILENAME);
    		/*
    		List<File> allDirectories = getSpartanSubModelDirectories();
    		if (allDirectories.size()==0)
    		{
    			// inFile is a submodel folder that might have an output of its
    			// own, but might also have no output
    			return new File(inFile.getAbsoluteFile() + File.separator 
    	    			+ SpartanConstants.OUTPUTFILENAME);
    		} else if (allDirectories.size()==1)
    		{
    			// We see a single model so we ignore the structure that could 
    			// hold multiple models
    	    	return new File(inFile.getAbsoluteFile() + File.separator 
    	    			+ allDirectories.get(0).getName() + File.separator 
    	    			+ SpartanConstants.OUTPUTFILENAME);
    		} else
    		{
    			// We have a multi-model Spartan output
    			if (selectedMolID!=null)
    			{
    				inFile = new File(inFile.getAbsoluteFile() + File.separator 
        	    			+ selectedMolID);
        	    	return new File(inFile.getAbsoluteFile() + File.separator 
        	    			+ SpartanConstants.OUTPUTFILENAME);
    			}
    			throw new Error("Spartan output file '" + inFile + "' is a "
	            			+ "directory containing multiple models, but there "
	            			+ "is also no request to focus on a model and "
	            			+ "we anded up ignoring the molti-model nature. "
	            			+ "Please, report this to the authors");
    		}
    		*/
    	} else {
    		return inFile;
    	}
    }    
    
//------------------------------------------------------------------------------

    /**
     * We override the behavior on missing log file because Spartan produced
     * structured data that one may call "output", but that does not contain
     * any log to read. Effectively we ignore the lack of a log file but we do
     * parse the data that we can find.
     * @return the log file
     */
    protected void reactToMissingLogFile(File logFile)
    {
		if (logFile!=null)
			logger.info("Log file '" + logFile + "' not found. "
					+ "Interpreting '" + inFile + "' as a data folder.");
    	
		try {
			parseModelDir(inFile);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse model directory: " 
					+ e.getMessage(), e);
		}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * We parse the Spartan directories in a customized way because the 
     * structure of the data is that of a nested tree with multiple output files
     * rather than a single log/output to read as expected by the original 
     * method in the superclass. 
     * In reality, given the structure of Spartan output data,
     * this method does not really read the log via the LogReader.
     * It reads all the relevant files
     * that are produced by Spartan to report its output.
     */
    
    @Override
    protected void readLogFile(LogReader reader) throws Exception
    {
    	String line = null;
    	boolean foundNormalEndInLog = false;
    	while ((line = reader.readLine()) != null)
        {
        	if (line.matches(".*" + SpartanConstants.LOGNORMALTERM + ".*"))
        	{
        		foundNormalEndInLog = true;
        	}
        }
    	
    	// Make sure the status output is consistent with the log, as there are 
    	// cases of log files reporting "Successful completion" while
        // the job has actually failed, as reported in the status file
    	// See <a href="https://github.com/denoptim-project/AutoCompChem/issues/21">issue #21</a>
    	String statusFile = inFile.getAbsoluteFile() + File.separator + ".."
    			+ File.separator + SpartanConstants.STATUSFILENAME;
    	// But the status file is not written for ouput files that are created
    	// by a job (e.g., the DYN constrained conformational search). Missing
    	// status file does not mean there has been an error.
    	boolean foundNormalEndStatus = true;
    	if ((new File(statusFile).exists()))
    	{
    		foundNormalEndStatus = FileAnalyzer.grep(statusFile, 
    			SpartanConstants.NORMALCOMPLSTATUS).size() > 0;
    	}
    	
    	if (foundNormalEndInLog && foundNormalEndStatus)
    		normalTerminated = true;
    	
    	parseModelDir(inFile);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Reads all data that we know how to parse from a model/molecule specific
     * Spartan directory that contains an 
     * {@value SpartanConstants#INPUTFILENAME} file and an
     * {@value SpartanConstants#ARCHIVEFILENAME} file
     * @param the folder that contains the files to parse, which also 
     * corresponds to the identified of the model.
     */
    
    private void parseModelDir(File moleculeDirectory) throws IOException
    {
    	String molID = moleculeDirectory.getName();
        String sprtArchFile = moleculeDirectory.getAbsolutePath() 
        		+ File.separator + SpartanConstants.ARCHIVEFILENAME;
        String sprtInpFile = moleculeDirectory.getAbsolutePath() 
        		+ File.separator + SpartanConstants.INPUTFILENAME;
        FileUtils.foundAndPermissions(sprtArchFile,true,false,false);
        FileUtils.foundAndPermissions(sprtInpFile,true,false,false);
        
        List<List<String>> firstGeom =
        		FileAnalyzer.extractMultiTxtBlocksWithDelimiters(sprtArchFile,
        				SpartanConstants.ARCHSTARTXYZ,
        				SpartanConstants.ARCHENERGYLAB + "|" 
        						+ SpartanConstants.ARCHBASISLAB,
        				false,
        				false);

	    int nAtms = firstGeom.get(0).size();
	    if (nAtms < 1)
	    {
	        throw new IllegalStateException("Unable to find number of atoms "
	                        + "in archive '" + sprtArchFile + "'.");
	    }

	    // Read in molecular definition blocks
	    List<List<String>> blocks = 
	    		FileAnalyzer.extractMultiTxtBlocksWithDelimiterAndSize(
	    				sprtArchFile,
	    				SpartanConstants.ARCHSTARTXYZ,
	    				nAtms,
	    				false);
	    
	    // Make molecular objects from text
	    List<IAtomContainer> molList = new ArrayList<IAtomContainer>();
	    for (List<String> singleBlock : blocks)
	    {
	        molList.add(getIAtomContainerFromXYZblock(singleBlock, molID));
	    }

	    // Get connectivity block
	    blocks = FileAnalyzer.extractMultiTxtBlocksWithDelimiters(sprtInpFile,
	    		"^" + SpartanConstants.TOPOOPN,
	    		SpartanConstants.TOPOEND+"(.*)",
	    		true,
	    		false);

	    // Get connectivity
	    if (blocks.size() == 1)
	    {
	        List<int[]> cnTab = new ArrayList<int[]>();
	        int nAtmTypesFound = 0;
	        for (String line : blocks.get(0))
	        {
	            // Need to check for keyword abbreviation
	            if (line.toUpperCase().startsWith("^"+SpartanConstants.TOPOOPN)
	                || line.toUpperCase().startsWith(SpartanConstants.TOPOEND
	                		+ "(.*)"))
	            {
	                continue;
	            }
	
	            String[] parts = line.trim().split("\\s+");
	
	            //read (and for now ignore) the atom type section
	            if (nAtmTypesFound < nAtms)
	            {
	               nAtmTypesFound = nAtmTypesFound +  parts.length;
	               continue;
	            }
	  
	            try
	            {
	                cnTab.add(new int[] {Integer.parseInt(parts[0]), 
	                                     Integer.parseInt(parts[1]),
	                                     Integer.parseInt(parts[2])});
	            }
	            catch (Throwable t)
	            {
	                logger.warn("WARNING! Could not read connectivity "
	                                          + "from line '" + line + "'. ");
	                break;
	            }
	        }
	        // WARNING! Assuming that all geometries have the same connectivity
	        for (int[] ids : cnTab)
	        {
	            Order o = MolecularUtils.intToBondOrder(ids[2]); 
	            for (IAtomContainer mol : molList)
	            {
	                mol.addBond(ids[0]-1,ids[1]-1,o);
	            }
	        }
	    } else {
	        throw new IllegalStateException("Unable to deal with Spartan "
	                  + SpartanConstants.INPUTFILENAME + " file that has "
	                  + "none or more than one '" + SpartanConstants.TOPOOPN 
	                  + "' block. Check file '" + sprtInpFile + "'.");
	    }
    	
	    //TODO: plenty of other data we could parse, but we do not do it yet...
	    

	    // Store data of this "step". We consider one Spartan job as a single 
	    // step because no multistep job is possible, yet.
	    
	    AtomContainerSet acs = new AtomContainerSet();
	    for (IAtomContainer iac : molList)
	    	acs.addAtomContainer(iac);
	    
	    if (!stepsData.containsKey(0))
	    {
	    	stepsData.put(0, new NamedDataCollector());
	    }
	    
	    stepsData.get(0).putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAGEOMETRIES, acs));
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a Cartesian coordinates table, in the format from Spartan output 
     * file (including header) and return the corresponding molecular 
     * representation (without connectivity). It converts the coordinates
     * from Bohr (units in Spartan files) to Angstrom (units in XYZ).
     * @param lines the list of lines corresponding to the XYZ table
     * @return the molecule obtained
     */

    public static IAtomContainer getIAtomContainerFromXYZblock(
    		List<String> lines, String molName)
    {
        double b2a = 1.0 / ACCConstants.ANGSTOMTOBOHR;
        IAtomContainer iac = new AtomContainer();
        iac.setProperty(CDKConstants.TITLE, molName);
        for (int i=0; i<lines.size(); i++)
        {
            String[] parts = lines.get(i).trim().split("\\s+");
            try
            {
                String el = AtomUtils.getElementalSymbol(
                		Integer.parseInt(parts[0]));
                Point3d p3d = new Point3d(Double.parseDouble(parts[1]) * b2a,
                                          Double.parseDouble(parts[2]) * b2a,
                                          Double.parseDouble(parts[3]) * b2a);
                IAtom atm = new Atom(el,p3d);
                iac.addAtom(atm);
            }
            catch (Throwable t)
            {
                throw new IllegalArgumentException("Unable to convert line '"
                        + lines.get(i) + "' into an atom while reading Spartan "
                        + "file.");
            }
        }
        return iac;
    }

//------------------------------------------------------------------------------

	@Override
	protected Set<FileFingerprint> getOutputFingerprint() 
	{
		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
		
		//These are meant for folder that may contain multiple models
		conditions.add(new FileFingerprint("./*/_spartan", 1, 
				"^\\s*[sS]partan .*[dD]irectory$"));
		conditions.add(new FileFingerprint("./*/_spartandir", 1, 
				"^$"));
		
		// This is meant for submodel directories
		conditions.add(new FileFingerprint("./_spartan", 1, 
				"^\\s*[sS]partan .*[dD]irectory$"));
		
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
		return new SpartanInputWriter();
	}
		
//-----------------------------------------------------------------------------
    
}
