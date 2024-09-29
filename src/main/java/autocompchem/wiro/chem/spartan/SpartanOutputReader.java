package autocompchem.wiro.chem.spartan;

import java.io.File;
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
import autocompchem.run.Terminator;
import autocompchem.wiro.ITextualInputWriter;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftInputWriter;
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
     * The identifier of the molecule/model to focus on, ignoring the rest.
     */
    private String selectedMolID;
    
    /**
     * The list of model identifiers
     */
    private List<String> modelIDs;
    
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
    	
    	if (params.contains(ChemSoftConstants.PARMODELID))
        {
    		selectedMolID = params.getParameter(
            		ChemSoftConstants.PARMODELID).getValueAsString();
        }
    	
    	if (!inFile.isDirectory())
    	{
    		Terminator.withMsgAndStatus("ERROR! " 
    				+ this.getClass().getSimpleName() + " requires the "
    				+ "value of " + ChemSoftConstants.PARJOBOUTPUTFILE 
    				+ " to be a pathname to a Spartan directory, but '"
    				+ inFile + "' is not.", -1);
    	}
    	
    	if (selectedMolID==null)
    	{
	    	List<File> allFIles = FileUtils.findByGlob(inFile, "*", true);
			modelIDs = new ArrayList<String>(); 
			allFIles.stream()
				.filter(f -> f.isDirectory())
				.forEach(d -> modelIDs.add(d.getName()));
			if (modelIDs.size()==1)
			{
				selectedMolID = modelIDs.get(0);
			}
    	}
    }

//------------------------------------------------------------------------------

      @Override
      public String getKnownInputDefinition() {
          return "inputdefinition/SpartanOutputReader.json";
      }
    
//------------------------------------------------------------------------------

    /**
     * We override how to define the file to read and interpret as log.
     * This because in Spartan the log contains little information on geometries
     * @return the log file
     */
    public File getLogPathName()
    {
    	if (inFile.isDirectory())
    	{
    		// Search for Spartan model/molecule-specific data
    		List<File> allFiles = FileUtils.findByGlob(inFile, "*", true);
    		List<File> allDirectories = allFiles.stream()
    			.filter(f -> f.isDirectory())
    			.collect(Collectors.toList());
    		if (allDirectories.size()==1)
    		{
    	    	return new File(inFile.getAbsoluteFile() + File.separator 
    	    			+ allDirectories.get(0).getName() + File.separator 
    	    			+ SpartanConstants.OUTPUTFILENAME);
    		}
    		logger.info("Sprtan ouput file '" + inFile + "' is a "
            			+ "directory containing multiple models.");
	    	return null;
    	} else {
    		return inFile;
    	}
    }    
    
//------------------------------------------------------------------------------

    /**
     * We override the behavior on missing log file because Spartan produced
     * structured data that one may call "output", but that does not contain
     * any log to read. 
     * @return the log file
     */
    protected void reactToMissingLogFile(File logFile)
    {
		if (logFile!=null)
			logger.info("Log file '" + logFile + "' not found. "
					+ "Interpreting '" + inFile + "' as a data folder.");
    	
    	parseSpartanDir();
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
    	// Take the overall "normal termination" flag from the status file
    	String line = null;
    	while ((line = reader.readLine()) != null)
        {
        	if (line.matches(".*" + SpartanConstants.NORMALCOMPLSTATUS + ".*"))
        	{
        		normalTerminated = true;
        	}
        }
    	
    	parseSpartanDir();
    }
   
//-----------------------------------------------------------------------------
    
    private void parseSpartanDir()
    {   
    	if (selectedMolID!=null)
    	{
    		parseModelDir(selectedMolID);
    	} else {
    		 Terminator.withMsgAndStatus("ERROR! Handling of Spartan output "
    		 		+ "including multiple models is not yet implemented.", -1);
    		 
    		//TODO: we could do this only if we make the analysis tasks work on
    		// any NamedData that starts with the expected constant but also 
    		// include in the key the molID
    		/*
    		// Search for Spartan model/molecule-specific data
    		List<File> allFIles = FileUtils.find(inFile, "", true);
    		List<File> allDirectories = allFIles.stream()
    			.filter(f -> f.isDirectory())
    			.collect(Collectors.toList());
    		
    		// We parse all molecules/models
    		for (File dir : allDirectories)
    		{
    			parseSpartanDir(dir.getName());
    		}
    		*/
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Reads all data that we know how to parse from a model/molecule specific
     * Spartan directory. 
     * @param molID the identifier of the molecule/model, i.e., the name of the 
     * Spartan directory to read.
     */
    
    private void parseModelDir(String molID)
    {
        String sprtArchFile = inFile + File.separator + molID + File.separator
        		+ SpartanConstants.ARCHIVEFILENAME;
        String sprtInpFile = inFile + File.separator + molID + File.separator
        		+ SpartanConstants.INPUTFILENAME;
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
	        Terminator.withMsgAndStatus("ERROR! Unable to find number of atoms "
	                        + "in archive '" + sprtArchFile + "'.",-1);
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
	        Terminator.withMsgAndStatus("ERROR! Unable to deal with Spartan "
	                  + SpartanConstants.INPUTFILENAME + " file that has "
	                  + "none or more than one '" + SpartanConstants.TOPOOPN 
	                  + "' block. Check file '" + sprtInpFile + "'.",-1);
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
	    		//TODO-gg this is a possibility to use only when the analysis 
	    		// tasks have been adapted to process multi-model output
				//ChemSoftConstants.JOBDATAGEOMETRIES + "_" + molID, molList));
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
                Terminator.withMsgAndStatus("ERROR! Unable to convert line '"
                        + lines.get(i) + "' into an atom while reading Spartan "
                        + "file.",-1);
            }
        }
        return iac;
    }

//------------------------------------------------------------------------------

	@Override
	protected Set<FileFingerprint> getOutputFingerprint() 
	{
		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
		conditions.add(new FileFingerprint("./*/_spartan", 1, 
				"^spartan directory$"));
		return conditions;
	}

//------------------------------------------------------------------------------

	@Override
	public String getSoftwareID() {
		return "Spartan";
	}

//------------------------------------------------------------------------------

	@Override
	protected ITextualInputWriter getSoftInputWriter() {
		return new SpartanInputWriter();
	}
		
//-----------------------------------------------------------------------------
    
}
