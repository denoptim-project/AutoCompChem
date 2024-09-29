package autocompchem.wiro.chem.gaussian;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Job;
import autocompchem.wiro.ITextualInputWriter;
import autocompchem.wiro.InputWriter;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for Gaussian output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of Gaussian jobs. 
 * The rest of the functionality is in the superclass
 * {@link ChemSoftOutputReader}.
 * 
 * @author Marco Foscato
 */
public class GaussianOutputReader extends ChemSoftOutputReader
{   
	/**
     * String defining the task of analyzing Gaussian output files
     */
    public static final String ANALYSEGAUSSIANOUTPUTTASKNAME = 
    		"analyseGaussianOutput";

    /**
     * Task about analyzing Gaussian output files
     */
    public static final Task ANALYSEGAUSSIANOUTPUTTASK;
    static {
    	ANALYSEGAUSSIANOUTPUTTASK = Task.make(ANALYSEGAUSSIANOUTPUTTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYSEGAUSSIANOUTPUTTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new GaussianOutputReader();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Method that parses the given log file from Gaussian and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    protected void readLogFile(LogReader reader) throws Exception
    {
        String line = null;
        NamedDataCollector stepData = new NamedDataCollector();
        ListOfIntegers stepScfConvSteps = new ListOfIntegers();
        ListOfDoubles stepScfConvEnergies = new ListOfDoubles();
        AtomContainerSet stepGeoms = new AtomContainerSet();
        
    	int stepInitLineNum = 0;
        int stepId = 0;
        boolean first = true;
        int lineNum = -1;
        while ((line = reader.readLine()) != null)
        {
        	lineNum++;
        	if (line.matches(".*" +GaussianConstants.LOGJOBSTEPSTART+ ".*"))
        	{
        		normalTerminated = false;
        		if (first)
        		{
        			first = false;
        		} else {
        			// NB: we do this here because we do not want this to 
        			// be dependent on the "Normal termination" line.
        			
        			storeDataOfOneStep(stepId, 
        					stepData, stepInitLineNum, 
        					lineNum-1, stepScfConvSteps, 
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
        	} else if (line.matches(".*" + GaussianConstants.LOGJOBSTEPEND 
        			+ ".*"))
        	{
        		normalTerminated = true;
        	} else if (line.matches(".*" + GaussianConstants.OUTSTARTXYZ 
        			+ ".*")) 
        	{
        		IAtomContainer iac = new AtomContainer();
        		int skipped = 0;
        		while ((line = reader.readLine()) != null)
                {
            		lineNum++;
            		if (skipped<2)
            		{
                		skipped++;
                		continue;
            		}
                	if (line.matches("^ -----------------.*$"))
                	{
                		// NB: the first such lines was skipped above
                		break;
                	}
                    String[] parts = line.trim().split("\\s+");
                    String el = AtomUtils.getElementalSymbol(
                    		Integer.parseInt(parts[1]));
                    Point3d p3d = new Point3d(Double.parseDouble(parts[3]),
                                              Double.parseDouble(parts[4]),
                                              Double.parseDouble(parts[5]));
                    IAtom atm = new Atom(el, p3d);
                    iac.addAtom(atm);
                }
        		stepGeoms.addAtomContainer(iac);
        	} else if (line.matches(".*" + GaussianConstants.OUTSCFENERGY
        			+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		stepScfConvEnergies.add(Double.parseDouble(p[4]));
        		stepScfConvSteps.add(Integer.parseInt(p[7]));
        		stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATASCFCONVERGED,true));
        	} else if (line.matches(".*" + 
        			GaussianConstants.OUTENDCONVGEOMOPTSTEP + ".*"))
        	{
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBGEOMOPTCONVERGED, true));
        	} else if (line.matches(GaussianConstants.OUTFREQHEADER + ".*"))
        	{
        		NormalModeSet listNormModes = 
        				GaussianUtils.parseNormalModesFromLog(reader);

        		ListOfDoubles listFreqs = new ListOfDoubles();
        		for (NormalMode nm : listNormModes)
        		{
        			listFreqs.add(nm.getFrequency());
        		}
        		
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATAVIBFREQ, listFreqs));
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATAVIBMODES, listNormModes));
        	} else if (line.matches(".*" 
                        + GaussianConstants.OUTGIBBSFREEENERGY + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[7]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATAGIBBSFREEENERGY,
                        val));
            } else if (line.matches(".*" + GaussianConstants.OUTCORRH+ ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[4]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_H_CORR,
                        val));
            } else if (line.matches(".*" + GaussianConstants.OUTTEMP + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[1]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_TEMP,
                        val));
            } else if (line.matches(".*" + GaussianConstants.OUTTOTS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]); // Cal/Mol-Kelvin
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_TOT,
                        val));
            } else if (line.matches(".*" + GaussianConstants.OUTTRAS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_TRANS,
                        val));
            } else if (line.matches(".*" + GaussianConstants.OUTROTS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_ROT,
                        val));
            } else if (line.matches(".*" + GaussianConstants.OUTVIBS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_VIB,
                        val));
            }
        	
        	
        	// There is plenty of other data in the Gaussian log file. 
        	// So, this list of parsed data will grow as needed...
        	// Here is a template of code to be added to parse some data
        	
        	/*

        	  else if (line.matches(".*" + GaussianConstants.____+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		//TODO: write code that parses data
        		 
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.____,
    					__data__));
        	}
        	 */
        }
        
		// Store data of last job, which ended with the end of the file
		storeDataOfOneStep(stepId, stepData, stepInitLineNum, 
				lineNum-1, stepScfConvSteps, stepScfConvEnergies, 
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
				"^ Entering Gaussian System, Link 0=.*$"));
		return conditions;
	}

//------------------------------------------------------------------------------

	@Override
	public String getSoftwareID() {
		return "Gaussian";
	}

//------------------------------------------------------------------------------

	@Override
	protected ITextualInputWriter getSoftInputWriter() {
		return new GaussianInputWriter();
	}

//------------------------------------------------------------------------------
    
}
