package autocompchem.wiro.acc;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.io.IOtools;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.wiro.InputWriter;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.wiro.chem.ChemSoftOutputReader;
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
     * Method that parses the given log file from AutoCompChem and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    protected void readLogFile(LogReader buffRead) throws Exception
    {
        //TODO-gg
    	/*
		// Store data of last job, which ended with the end of the file
		stepEndLineNum = lineNum-1;
		storeDataOfOneStep(stepId, stepData, stepInitLineNum, 
				stepEndLineNum, stepScfConvSteps, stepScfConvEnergies, 
				stepGeoms);
		*/
    }
    
//-----------------------------------------------------------------------------
    
    //TODO-gg adjust
    private void storeDataOfOneStep(int stepId, NamedDataCollector stepData) 
    		throws CloneNotSupportedException
    {
    	/*
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAINITLINE,
				stepInitLineNum));
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAENDLINE,
				stepEndLineNum));
		*/
		
		stepsData.put(stepId,stepData.clone());
    }

//------------------------------------------------------------------------------

	@Override
	public Set<FileFingerprint> getOutputFingerprint() 
	{
		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
		conditions.add(new FileFingerprint(".", 10, "REGEX_TODO-gg"));
			//TODO-gg
				//"^\\s*\\* O   R   C   A \\*\\s*$"));
	
		return conditions;
	}

//------------------------------------------------------------------------------

	@Override
	public String getSoftwareID() {
		return "ACC";
	}

//------------------------------------------------------------------------------

	@Override
	public InputWriter getSoftInputWriter() {
		return new ACCInputWriter();
	}
		
//-----------------------------------------------------------------------------
    
}
