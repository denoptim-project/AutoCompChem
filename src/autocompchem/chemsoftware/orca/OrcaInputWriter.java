package autocompchem.chemsoftware.orca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.gaussian.GaussianOutputHandler;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BSMatchingRule;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.run.ACCJob;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;

/**
 * Writes input files for software ORCA.
 *
 * @author Marco Foscato
 */

//TODO: write doc

public class OrcaInputWriter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTORCA)));
    
    /**
     * Molecular geometries input file. One or more geometries depending on the
     * kind of computational chemistry job. 
     */
    private String inGeomFile;

    /**
     * Definition of how to use multiple geometries
     */
    private enum MultiGeomMode {SingleGeom,ReactProd,ReactTSProd,Path};
    
    /**
     * Chosen mode of handling multiple geometries.
     */
    private MultiGeomMode multiGeomMode = MultiGeomMode.SingleGeom;

    /**
     * Geometry names
     */
    private ArrayList<String> geomNames = new ArrayList<String>(
                                                     Arrays.asList("geometry"));

    /**
     * Input format identifier.
     */
    private String inFileFormat = "nd";

    /**
     * Output name (input for comp.chem. software).
     */
    private String outFileNameRoot;

    /**
     * Output job details name.
     */
    private String outJDFile;

    /**
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);

    /**
     * Storage of SMARTS queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Label used to identify single-atom smarts in the smarts reference name
     */
    private static final String SUBRULELAB = "_p";

    /**
     * Root of the smarts reference names
     */
    private static final String MSTRULEROOT = "smarts ";
    
    /**
     * Default value for integers
     */
    private final int def = -999999;

    /**
     * charge of the whole system
     */
    private int charge = def;
    
    /**
     * Spin multiplicity of the whole system
     */
    private int spinMult = def;
    
    /**
     * Object containing the details on the NWChem job
     */
    private OrcaJob oJob;

    /** 
     * Verbosity level
     */
    private int verbosity = 1;

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    @Override
    public void initialize()
    {
        if (params.contains(ChemSoftConstants.PARVERBOSITY))
        {
            String str = params.getParameter(
            		ChemSoftConstants.PARVERBOSITY).getValue().toString();
            this.verbosity = Integer.parseInt(str);

            if (verbosity > 0)
                System.out.println(" Adding parameters to NWChemInputWriter");
        }

        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
	        this.inGeomFile = params.getParameter(
	        		ChemSoftConstants.PARGEOMFILE).getValue().toString();
	        
	        //TODO: use automated detection of file type
	        
	        if (inGeomFile.endsWith(".sdf"))
	        {
	            inFileFormat = "SDF";
	        }
	        else if (inGeomFile.endsWith(".xyz"))
	        {
	            inFileFormat = "XYZ";
	        }
	        else if (inGeomFile.endsWith(".out"))
	        {
	        	//TODO: identify the kind of cc-software that produced that file
	        	Terminator.withMsgAndStatus("ERROR! Format of file '" + inGeomFile 
	        			+ "' not recognized!",-1);
	        }
	        else
	        {
	        	Terminator.withMsgAndStatus("ERROR! Format of file '" + inGeomFile 
	        			+ "' not recognized!",-1);
	        }
	        FileUtils.foundAndPermissions(this.inGeomFile,true,false,false);
        }

        if (params.contains(ChemSoftConstants.PARMULTIGEOMMODE))
        {
        	String value = 
                    params.getParameter(ChemSoftConstants.PARMULTIGEOMMODE
                    		).getValue().toString();
            this.multiGeomMode = MultiGeomMode.valueOf(value);
        }

        if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdFile = params.getParameter(
            		ChemSoftConstants.PARJOBDETAILS).getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from file '" + jdFile + "'.");
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.oJob = new OrcaJob(jdFile);
        } 
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                                  + " No 'JOBDETAILS' found in parameters.",-1);
        }

        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileNameRoot = params.getParameter(
            		ChemSoftConstants.PAROUTFILEROOT).getValue().toString();
            outJDFile = outFileNameRoot + ChemSoftConstants.JDEXTENSION;
        } else {
            String inputRoot = FileUtils.getRootOfFileName(inGeomFile);
            outFileNameRoot = inputRoot + ChemSoftConstants.INPEXTENSION;
            outJDFile = inputRoot + ChemSoftConstants.JDEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No '" + ChemSoftConstants.PAROUTFILEROOT
                		+ "' parameter found. "
                        + "Root of any output file name set to '" 
                		+ outFileNameRoot + "'.");
            }
        }


        if (params.contains(ChemSoftConstants.PARCHARGE))
        {
            charge = Integer.parseInt(params.getParameter(
            		ChemSoftConstants.PARCHARGE).getValue().toString());
        } 

        if (params.contains(ChemSoftConstants.PARSPINMULT))
        {
            spinMult = Integer.parseInt(params.getParameter(
            		ChemSoftConstants.PARSPINMULT).getValue().toString());
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @SuppressWarnings("incomplete-switch")
    @Override
    public void performTask()
    {
        switch (task)
          {
          case PREPAREINPUTORCA:
        	  writeInput();
              break;
          }

        if (exposedOutputCollector != null)
        {
/*
//TODO
            String refName = "";
            exposeOutputData(new NamedData(refName,
                  NamedDataType.DOUBLE, ));
*/
        }
    }

//------------------------------------------------------------------------------

    /**
     * get the sorted list of master names 
     */

    private ArrayList<String> getSortedSMARTSRefNames(
                                                      Map<String,String> smarts)
    {
        ArrayList<String> sortedMasterNames = new ArrayList<String>();
        for (String k : smarts.keySet())
        {
            String[] p = k.split(SUBRULELAB);
            if (!sortedMasterNames.contains(p[0]))
            {
                sortedMasterNames.add(p[0]);
            }
        }
        Collections.sort(sortedMasterNames, new NumberAwareStringComparator());
        return sortedMasterNames;
    }

//------------------------------------------------------------------------------

    /**
     * Write input file. This is empty in this class, as it is meant to be 
     * overwritten by subclasses.
     */

    public void writeInput()
    {
    	Terminator.withMsgAndStatus(" ERROR! Running "
    			+ "ChemSoftwareInputWriter.writeInput(), which should have been"
    			+ " overwritten by devellpers.",-1);
    }

//------------------------------------------------------------------------------

    /**
     * Return true if the charge or the spin are overwritten according to the
     * IAtomContainer properties "CHARGE" and "SPIN_MULTIPLICITY"
     * @param mol the molecule from which we get charge and spin
     */

    private void chargeOrSpinFromMol(IAtomContainer mol)
    {
        boolean res = false;

        String str = " Using molecular structure file to set charge and "
        		+ "spin multiplicity." + System.getProperty("line.separator")
        		+ " From c = " + charge + " and s.m. = " + spinMult;

        if (MolecularUtils.hasProperty(mol, ChemSoftConstants.PARCHARGE))
        {
            res = true;
            charge = Integer.parseInt(mol.getProperty(
            		ChemSoftConstants.PARCHARGE).toString());
        }

        if (MolecularUtils.hasProperty(mol, ChemSoftConstants.PARSPINMULT))
        {
            res = true;
            spinMult = Integer.parseInt(mol.getProperty(
            		ChemSoftConstants.PARSPINMULT).toString());
        }

        if (verbosity > 0)
        {
            if (res)
            {
                System.out.println(str + " to c = " + charge + " and s.m. = "
                                + spinMult);
            }
        }
    }

//------------------------------------------------------------------------------

}
