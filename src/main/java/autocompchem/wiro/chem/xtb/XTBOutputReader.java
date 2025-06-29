package autocompchem.wiro.chem.xtb;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.SoftwareId;
import autocompchem.run.Terminator;
import autocompchem.wiro.ITextualInputWriter;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for XTB output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of XTB. The rest of the functionality is in the superclass
 * {@link ChemSoftOutputReader}.
 * 
 * @author Marco Foscato
 */
public class XTBOutputReader extends ChemSoftOutputReader
{
	/**
	 * Case insensitive software identifier
	 */
	public static final SoftwareId SOFTWAREID = new SoftwareId("xTB");
	
    /**
     * String defining the task of analyzing XTB output files
     */
    public static final String ANALYSEXTBOUTPUTTASKNAME = "analyseXTBOutput";

    /**
     * Task about analyzing XTB output files
     */
    public static final Task ANALYSEXTBOUTPUTTASK;
    static {
    	ANALYSEXTBOUTPUTTASK = Task.make(ANALYSEXTBOUTPUTTASKNAME);
    }
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYSEXTBOUTPUTTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new XTBOutputReader();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Method that parses the given log file from XTB and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    protected void readLogFile(LogReader reader) throws Exception
    {	
    	// Extract the pathname of the log file: we'll use it to find output 
    	// that is printed on other files
		String path = "";
		if (inFile.getParent() != null)
		{
        	path = inFile.getParent() + System.getProperty(
        			"file.separator");
		}
		
        String line = null;
        NamedDataCollector stepData = new NamedDataCollector();
        ListOfIntegers stepScfConvSteps = new ListOfIntegers();
        ListOfDoubles stepScfConvEnergies = new ListOfDoubles();
        AtomContainerSet stepGeoms = new AtomContainerSet();
        
        boolean foundOptTrajectory = false;
        File xtbopt = null;
        
    	int stepInitLineNum = 0;
    	int stepEndLineNum = Integer.MAX_VALUE;
        int stepId = 0;
        boolean first = true;
        int lineNum = -1;
        while ((line = reader.readLine()) != null)
        {
        	lineNum++;
        	
        	if (line.matches(".*" + XTBConstants.LOGJOBSTEPSTART+ ".*"))
        	{
        		if (first)
        		{
        			first = false;
        		} else {
        			stepEndLineNum = lineNum-1;
        			
        			if (!foundOptTrajectory)
        			{
                		File xyzOpt = new File(path + "xtbopt.log");
                		if (xyzOpt.exists())
                		{
                			List<IAtomContainer> mols = IOtools.readXYZ(xyzOpt);
                		
                			for (IAtomContainer mol : mols)
                			{
                				stepGeoms.addAtomContainer(mol);
                			}
                        }
        			}
        			
        			storeDataOfOneStep(stepId, 
        					stepData, stepInitLineNum, 
        					stepEndLineNum, stepScfConvSteps, 
        					stepScfConvEnergies, stepGeoms);
        			
        			// ...clear local storage...
        			stepData = new NamedDataCollector();
        			stepScfConvSteps = new ListOfIntegers();
                    stepScfConvEnergies = new ListOfDoubles();
                    stepGeoms = new AtomContainerSet();
                    
                	//...and move on to next step.
        			stepId++;
        			stepInitLineNum = lineNum;
        		}
        	}
        	
        	if (line.matches(".*" + XTBConstants.LOGNORMALTERM+ ".*"))
        	{
        		normalTerminated = true;
        	}
        	
        	if (line.matches(".*" + XTBConstants.LOGFINALSPENERGY+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		stepScfConvEnergies.add(Double.parseDouble(p[3]));
        	}
        	
        	if (line.matches(".*" + XTBConstants.LOGSCFSUCCESS + ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		stepScfConvSteps.add(Integer.parseInt(p[5]));
        		stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATASCFCONVERGED,true));
        	}
        	
        	if (line.matches(".*" + 
        			XTBConstants.LOGGEOMOPTCONVERGED+ ".*"))
        	{
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBGEOMOPTCONVERGED,true));
        	}

        	if (line.matches(".*" + XTBConstants.LOGVIBFREQ+ ".*"))
        	{
        		ListOfDoubles list = new ListOfDoubles();
        		int skipped = 0;
        		while ((line = reader.readLine()) != null)
                {
                	lineNum++;
                	skipped++;
                	if (skipped<3)
                	{
                		continue;
                	}
                	if (line.contains("Molecule has no symmetry elements") 
                			|| line.contains("It seems to be the"))
                	{
                		continue;
                	}
                	if (line.contains("reduced masses"))
                	{
                		break;
                	}
            		String[] p = line.trim().split("\\s+");
            		if (p.length < 3)
            		{
            			Terminator.withMsgAndStatus("ERROR! Cannot "
            					+ "read vibrationsl frequencies from line '" 
            					+ line + "'.",-1);
            		}
            		for (int i=2; i<p.length; i++)
            		{
            			list.add(Double.parseDouble(p[i]));
            		}
                }
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATAVIBFREQ,list));
        	}
        	
        	if (line.matches(".*" + XTBConstants.LOGGIBBSFREEENERGY+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATAGIBBSFREEENERGY,
    					Double.parseDouble(p[4])));
        	}
        	
        	
        	/*
            if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_ELECTR
                    + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_ELECTR,
                        val));
            }

            if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_VIB
                + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_VIB,
                        val));
            }

            if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_TRANS
                + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_TRANS,
                        val));
            }

            if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_ROT
                + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_ROT,
                        val));
            }

            if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_H
                + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_H,
                        val));
            }

            if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_ZPE
                + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[4]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_ZPE,
                        val));
            }

            if (line.matches(".*" 
            		+ XTBConstants.LOGTHERMOCHEM_UCORR_VIB + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[4]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_VIB,
                        val));
            }

            if (line.matches(".*" 
            		+ XTBConstants.LOGTHERMOCHEM_UCORR_ROT + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[4]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_ROT,
                        val));
            }

            if (line.matches(".*" 
            		+ XTBConstants.LOGTHERMOCHEM_UCORR_TRANS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[4]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_TRANS,
                        val));
            }
        	*/
        	
        	
        	if (line.matches(".*" + XTBConstants.LOGOPTTRJFILENAME + ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		String optGeomFileName = p[5];
        		optGeomFileName = optGeomFileName.replace("\'","");
        		optGeomFileName = optGeomFileName.replace("\'","");
        		
        		File xyzOpt = new File(path + optGeomFileName);
        		if (!xyzOpt.exists())
        		{
        			logger.warn("WARNING! Found redirection"
            					+ " to additional output file, " 
            					+ "but '"
            					+ xyzOpt.getAbsolutePath() + "' could "
            					+ "be found! " + System.getProperty(
            							"line.separator") 
            					+ "I cannot find geometries for step "
						+  (stepId+1) + "!");
        		} else {
        			List<IAtomContainer> mols = IOtools.readXYZ(xyzOpt);
        			for (IAtomContainer mol : mols)
        			{
        				stepGeoms.addAtomContainer(mol);
        			}
                }
        		foundOptTrajectory = true;
        	}
        	
        	if (line.matches(".*" + XTBConstants.LOGOPTGEOMFILENAME+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		String optGeomFileName = p[4];
        		optGeomFileName = optGeomFileName.replace("\'","");
        		optGeomFileName = optGeomFileName.replace("\'","");
        		
        		xtbopt = new File(path + optGeomFileName);
        	}
    	
        	// There is plenty of other data in the XTB log file. 
        	// So, this list of parsed data will grow as needed...
        	// Here is a template of code to be added to parse some data
        	
        	/*

        	if (line.matches(".*" + XTBConstants.____+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		//TODO: write code that parses data
        		 
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.____,
    					__data__));
        	}
        	 */
        }
        
        if (!foundOptTrajectory)
		{
    		File xyzOpt = new File(path + "xtbopt.log");
    		if (xyzOpt.exists())
    		{
    			List<IAtomContainer> mols = IOtools.readXYZ(xyzOpt);
    		
    			for (IAtomContainer mol : mols)
    			{
    				stepGeoms.addAtomContainer(mol);
    			}
            }
		}
        
        // From xTB 6.7.1 the trajectory is not written in the log and it is not
        // written to file if there are no steps taken (i.e., the input is
        // already converged). In such case, the only source of geometry is the
        // file where the optimized geometry is written
        if (!foundOptTrajectory)
        {
        	// We look for the files that XTB creates with the final geometry, 
        	// but this practice is often inconsistent among versions
        	if (xtbopt==null)
        	{
        		// The xtbopt file is created also by Hessian jobs in cTB v6.7
        		xtbopt = new File(path + "xtbopt.sdf");
        		if (!xtbopt.exists())
        		{	
        			xtbopt = new File(path + "xtbopt.xyz");
	        		if (!xtbopt.exists())
	        			xtbopt = null;
        		}
        	}
        	if (xtbopt!=null)
        	{
	    		if (!xtbopt.exists())
	    		{
	    			logger.warn("WARNING! Found redirection"
	        					+ " to additional output file, " 
	        					+ "but '"
	        					+ xtbopt.getAbsolutePath() + "' could "
	        					+ "be found! " + System.getProperty(
	        							"line.separator") 
	        					+ "I cannot find optimized geometry for step "
						+  (stepId+1) + "!");
	    		} else {
	    			// There should be only one geometry!
	    			List<IAtomContainer> mols = IOtools.readMultiMolFiles(xtbopt);
	    			for (IAtomContainer mol : mols)
	    			{
	    				stepGeoms.addAtomContainer(mol);
	    			}
	            }
        	}
        }
        
        //TOTO read vib modes from g98.out (a.k.a. the fake output)
        /*
	    stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAVIBMODES,nms));
	    */
        
		// Store data of last job, which ended with the end of the file
		stepEndLineNum = lineNum-1;
		storeDataOfOneStep(stepId, stepData, stepInitLineNum, 
				stepEndLineNum, stepScfConvSteps, stepScfConvEnergies, 
				stepGeoms);
    }
    
//-----------------------------------------------------------------------------
    
    private void storeDataOfOneStep(
    		int stepId, NamedDataCollector stepData, 
    		int stepInitLineNum, int stepEndLineNum, 
    		ListOfIntegers stepScfConvSteps, ListOfDoubles stepScfConvEnergies,
    		AtomContainerSet stepGeoms) throws CloneNotSupportedException
    {
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAINITLINE,
				stepInitLineNum));
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAENDLINE,
				stepEndLineNum));
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATASCFENERGIES,
				stepScfConvEnergies));
		if (!stepScfConvSteps.isEmpty())
		{
    		stepData.putNamedData(new NamedData(
    				ChemSoftConstants.JOBDATASCFSTEPS,
    				stepScfConvSteps));
		}
		if (!stepGeoms.isEmpty())
		{
			stepData.putNamedData(new NamedData(
					ChemSoftConstants.JOBDATAGEOMETRIES,
					stepGeoms.clone()));
		}
		
		stepsData.put(stepId, stepData.clone());
    }
    
//------------------------------------------------------------------------------

  	@Override
  	protected Set<FileFingerprint> getOutputFingerprint() 
  	{
  		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
  		conditions.add(new FileFingerprint(".", 10, 
  				"^\\s*\\|\\s*x T B\\s*\\|\\s*$"));
  		// NB: we allow to consider the working directory as output as xTB 
  		// creates many useful files in the work space and with conventional 
  		// names.
  		conditions.add(new FileFingerprint("./*", 10, 
  				"^\\s*\\|\\s*x T B\\s*\\|\\s*$"));
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
  		return new XTBInputWriter();
  	}
  	
//-----------------------------------------------------------------------------
    
}
