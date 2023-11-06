package autocompchem.chemsoftware;

import java.util.ArrayList;
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

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftOutputAnalyzer;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Aspecific reader for log/output data files. This is a wrapper that includes
 * both detection of the type of data to read and creation of a suitable worker
 * to read and analyze that data.
 * 
 * @author Marco Foscato
 */
public class AspecificOutputAnalyzer extends ChemSoftOutputAnalyzer
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
                    Collections.unmodifiableSet(new HashSet<TaskID>(
                                    Arrays.asList(TaskID.ANALYSEOUTPUT)));
    

//-----------------------------------------------------------------------------

    public AspecificOutputAnalyzer()
    {
    }
    
//-----------------------------------------------------------------------------
    

    //TODO-gg remove after creation of IWorkerCreator
    
    @Override
    protected void readLogFile(LogReader buffRead) throws Exception
    {	
    }
    
//------------------------------------------------------------------------------

    //TODO-gg remove after creation of IWorkerCreator
    
//------------------------------------------------------------------------------

  	@Override
  	protected Set<FileFingerprint> getOutputFingerprint() 
  	{
  		return new HashSet<FileFingerprint>();
  	}

//------------------------------------------------------------------------------

  	@Override
	public Set<TaskID> getCapabilities() {
		return Collections.unmodifiableSet(new HashSet<TaskID>(
                        Arrays.asList(TaskID.ANALYSEOUTPUT)));
	}

//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "";
	}

//------------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Object... args) {
		return new AspecificOutputAnalyzer();
	}

//-----------------------------------------------------------------------------
    
}
