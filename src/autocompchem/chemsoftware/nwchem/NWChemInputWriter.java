package autocompchem.chemsoftware.nwchem;

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

import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
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
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;

/**
 * Writes input files for NWChem. Allows to define the directives for
 * NWChem in the form of 
 * a formatted text file that is referred to as the "jobDetails" file 
 * (see {@link NWChemJob} and {@link NWChemTask}).
 * Parameters used by this worker:
 * <ul>
 * <li>
 * <b>INFILE</b>: name of the structure file (i.e. path/name.sdf).
 * </li>
 * <li>
 * (optional) <b>MULTIGEOMNWCHEM</b>: use this parameter to provide names 
 * for the geometry/ies (all space-free names listed in one single line, 
 * space-separated). When this parameter is used all the geometries in the 
 * input molecular structure file (i.e., SDF or XYZ) are written into the 
 * first task of the NWChem job with unique atom tags. Basis set and 
 * constrains are also generated for all the geometries. 
 * This parameter
 * can also be specified in the first task of the job details file 
 * by prepending (i.e., add at the beginning) the
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABPARAMS}
 * label. 
 * </li>
 * <li>(optional) <b>USESAMETAGSFORMULTIGEOM</b>: imposes to use the same
 * atom tag through all geometries. We assume that all the 
 * geometries are consistent (i.e., same atom list).
 * This parameter
 * can also be specified in the first task of the job details file
 * by prepending (i.e., add at the beginning) the
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABPARAMS}
 * label.
 * </li>
 * <li>
 * <b>JOBDETAILS</b>: formatted text file defining all
 * the details of a {@link NWChemJob}.
 * The definition of the format of jobdetails files can be found in
 * {@link NWChemJob} documentation.
 * </li>
 * <li>
 * (optional) <b>VERBOSITY</b> verbosity level.
 * </li>
 * <li>
 * (optional) <b>OUTFILE</b> name of the output file (the input for NWChem).
 * If this option is omitted the default name of the output is build from 
 * the root of the structure file name given in the INFILE parameter.
 * </li>
 * </ul>
 * Optional parameters not needed if JOBDETAILS option is in use, but
 * that will overwrite JOBDETAILS specifications if both JOBDETAILS and
 * these options are specified in the {@link ParameterStorage}.
 * <ul>
 * <li>
 * (optional) <b>TITLE</b> title line for the output file
 * </li>
 * <li>
 * (optional) <b>CHARGE</b> the charge of the chemical system
 * </li>
 * <li>
 * (optional) <b>SPIN_MULTIPLICITY</b> the spin multiplicity of the
 * chemical system
 * </li>
 * <li>
 * (optional) <b>COORDINATESTYPE</b> the type of coordinates (Cartesian or
 * internal) to be used for the input file of NWChem (Default: Cartesian).
 * </li>
 * <li>
 * (optional) <b>FREEZEIC</b> define frozen internal coordinates 
 * (i.e., constants).
 * To identify the IC to freeze, SMARTS queries are used.
 * A multi line block (see {@link autocompchem.datacollections.Parameter}) 
 * can be used defining one 
 * SMARTS query per line.
 * </li>
 * <li>
 * (optional) <b>FREEZEATM</b> define frozen atoms when using Cartesian
 * coordinates.
 * SMARTS queries are used to identify atoms to freeze; one SMARTS per line.
 * </li>
 * <li>
 * (optional) <b>ZCOORD</b> can also be used to freeze redundant internal
 * coordinates and eventually release such constraints.
 * This only works if
 * the geometry is written with Cartesian coordinates and NWChem
 * uses the "autoz" module to generate the redundant
 * internal coordinated (NB: the "autoz" keyword is active by default)
 * SMARTS queries are used to identify the specific classes of internal
 * coordinates (IC) that will be listed in the ZCOORD directive.
 * Single-atom SMARTS (i.e., must be enclosed in square brackets, eg. [C])
 * are used to define a single atom type. Combinations of 2, 3, or 4
 * whitespace-separated single-atom SMARTS are used to refer to stretching,
 * bending, or torsion types of ICs.
 * All n-tuple (n=2,3,4) of matching atoms that are also properly connected
 * are considered.
 * After the 2/3/4 single-atom SMARTS
 * the user can add one numerical value and/or the <code>constant</code>
 * keyword that will apply to all ICs matched by the combination of
 * single-atom SMARTS.
 * A multi line block (see {@link autocompchem.datacollections.Parameter}) is used to
 * define one set of SMARTS queries (plus additional keywords-
 * related to that
 * class of IC) per each line.
 * <pre>
 * $STARTZCOORD:
 * [C] [$(C(~[N])~C)] 1.5 constant
 * [*] [$(C(~[N])~C)] [*] 120.000 constant
 * [*] [$(C(~[N])~C)] [C] [*]
 * $END
 * </pre>
 * </li>
 * </ul>
 * <p>
 * Note that parameters specified in the constructor by means of the 
 * {@link ParameterStorage} will take effect from
 * the first {@link NWChemTask} of the generated {@link NWChemJob} 
 * and will be effective until new instructions (i.e., keyword, directive) 
 * are given into the {@link NWChemJob}.
 * To specify task-specific parameters, i.e., parameters that are meant 
 * for a specific {@link NWChemTask} in the {@link NWChemJob}, use the
 * jobDetails file (see the use of label
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABPARAMS} 
 * in {@link NWChemTask}).
 * </p>
 * The
 * parameters are red and interpreted so to alter the features of single
 * NWChemTask (or step) in the NWChemJob. Currently supported task-
 * specific parameters are:
 * <ul>
 * <li>FREEZEIC</li>
 * <li>FREEZEATM</li>
 * <li>CHARGE</li>
 * <li>SPIN_MULTIPLICITY</li>
 * <li>ZCOORD</li>
 * </ul> 
 *
 * @author Marco Foscato
 */

public class NWChemInputWriter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTNWCHEM)));
    
    /**
     * Input filename (a structure file)
     */
    private String inFile;

    /**
     * Flag controlling handling of multiple geometries
     */
    private boolean multiGeom = false;

    /**
     * Flag controlling the use of the same atom tags in all multiple geometries
     */
    private boolean sameTagsInMultigeom = false;

    /**
     * Geometry names
     */
    private ArrayList<String> geomNames = new ArrayList<String>(
                                                     Arrays.asList("geometry"));

    /**
     * Input format identifier
     */
    private String inFormat = "nd";

    /**
     * Output name (input for NWChem)
     */
    private String outNWFile;

    /**
     * Output job details name 
     */
    private String outJDFile;

    /**
     * Type of coordinates
     */
    private String coordType = "CARTESIAN";

    /**
     * Flag imposing to use customized charges from input
     */
    private boolean customCharge = false;

    /**
     * Flag imposing to use customized atomic mass
     */
    private boolean customMass = false;

    /**
     * Flag detecting use of ZCOORD directive
     */
    private boolean zcoordInUse = false;

    /**
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);

    /**
     * Unique counter for atom tags
     */
    private AtomicInteger iAtomTag = new AtomicInteger(1);

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
     * Storage of details for ZCOORD classes
     */
    private Map<String,ArrayList<String>> zCrdDetails = 
                                        new HashMap<String,ArrayList<String>>();

    /**
     * Title of the job
     */
    private String title;

    /**
     * Default value for integers
     */
    private final int def = -999999;

    /**
     * charge of the whole system
     */
    private int charge = def;

    /**
     * Flag preventing the definition of charge
     */
    private boolean noCharge = false;
    
    /**
     * Spin multiplicity of the whole system
     */
    private int spinMult = def;

    /**
     * Flag preventing the definition of spin multiplicity
     */
    private boolean noSpinMult = false;

    /**
     * Object containing the details on the NWChem job
     */
    private NWChemJob nwcJob;

    /** 
     * Verbosity level
     */
    private int verbosity = 1;

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        //Define verbosity 
        if (params.contains("VERBOSITY"))
        {
            String str = params.getParameter("VERBOSITY").getValue().toString();
            this.verbosity = Integer.parseInt(str);

            if (verbosity > 0)
                System.out.println(" Adding parameters to NWChemInputWriter");
        }

        //Get and check the input file
        this.inFile = params.getParameter("INFILE").getValue().toString();
        if (inFile.endsWith(".sdf"))
        {
            inFormat = "SDF";
        }
        else if (inFile.endsWith(".xyz"))
        {
            inFormat = "XYZ";
        }
        else if (inFile.endsWith(".out"))
        {
            inFormat = "NWCOUT";
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! This version of "
                     + "NWChemInputWriter can handle only SDF or XYZ input",-1);
        }
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Deal with multiple geometries
        if (params.contains("MULTIGEOMNWCHEM"))
        {
            this.multiGeom = true;
            this.geomNames.clear();
            String line = 
                   params.getParameter("MULTIGEOMNWCHEM").getValue().toString();
            String[] parts = line.trim().split("\\s+");
            for (int i=0; i<parts.length; i++)
            {
                if (this.geomNames.contains(parts[i]))
                {
                    Terminator.withMsgAndStatus("ERROR! Geometry name '" 
                                        + parts[i] + "' is used more than once."
                                        + " Check line '" + line + "'.",-1);
                }
                this.geomNames.add(parts[i]);
            }
        }

        //Manage of tags
        if (params.contains("USESAMETAGSFORMULTIGEOM"))
        {
            this.sameTagsInMultigeom = true;
        }

        //Use NWChemJob; we do not accept headers in the form of plain text
        if (params.contains("JOBDETAILS"))
        {
            //Use NWChemJob
            String jdFile = 
                        params.getParameter("JOBDETAILS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Compound NWChem job: details from "  
                         + jdFile);
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.nwcJob = new NWChemJob(jdFile);

            //Deal with job details that affect the input writer initial setup
            if (nwcJob.getStep(0).hasACCParams())
            {
                ParameterStorage pp = nwcJob.getStep(0).getTaskSpecificParams();
                if (pp.contains("MULTIGEOMNWCHEM"))
                {
                    this.multiGeom = true;
                    this.geomNames.clear();
                    String line = pp.getParameter(
                                       "MULTIGEOMNWCHEM").getValue().toString();
                    String[] parts = line.trim().split("\\s+");
                    for (int i=0; i<parts.length; i++)
                    {
                        if (this.geomNames.contains(parts[i]))
                        {
                            Terminator.withMsgAndStatus("ERROR! Geometry name '"
                                        + parts[i] + "' is used more than once."
                                        + " Check line '" + line + "'.",-1);
                        }
                        this.geomNames.add(parts[i]);
                    }
                }
                if (pp.contains("USESAMETAGSFORMULTIGEOM"))
                {
                    this.sameTagsInMultigeom = true;
                }
            }
        } 
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                                  + " No 'JOBDETAILS' found in parameters.",-1);
        }

        //Name of output
        if (params.contains("OUTFILE"))
        {
            outNWFile = params.getParameter("OUTFILE").getValue().toString();
            outJDFile = FileUtils.getRootOfFileName(outNWFile) 
                                                  + NWChemConstants.JDEXTENSION;
        } else {
            String inputRoot = FileUtils.getRootOfFileName(inFile);
            outNWFile = inputRoot + NWChemConstants.NWCINPEXTENSION;
            outJDFile = inputRoot + NWChemConstants.JDEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No 'OUTFILE' option found. "
                                   + "Output name set to '" + outNWFile + "'.");
            }
        }

        //TITLE
        if (params.contains("TITLE"))
        {
            title = params.getParameter("TITLE").getValue().toString();
            this.nwcJob.setAllTitle(title);
            if (verbosity > 0)
            {
                System.out.println(" Found 'TITLE' option. Overwriting "
                                                        + "title in all steps");
            }
        }

        //CHARGE
        if (params.contains("CHARGE"))
        {
            charge = Integer.parseInt(
                        params.getParameter("CHARGE").getValue().toString());
            this.nwcJob.setAllCharge(charge);
            if (verbosity > 0)
            {
                System.out.println(" Found 'CHARGE' option. Overwriting "
                                + "charge in all steps. New value = " + charge);
            }
        } 
        if (params.contains("NOCHARGE"))
        {
            this.noCharge = true;
            if (verbosity > 0)
            {
                System.out.println(" Found 'NOCHARGE' option. No charge will "
                                     + "be specified in the NWChem input file");
            }
        }

        //SPIN_MULTIPLICITY
        if (params.contains("SPIN_MULTIPLICITY"))
        {
            spinMult = Integer.parseInt(
                params.getParameter("SPIN_MULTIPLICITY").getValue().toString());
            this.nwcJob.setAllSpinMultiplicity(spinMult);
            if (verbosity > 0)
            {
                    System.out.println(" Found 'SPIN_MULTIPLICITY' option. "
                        + "Overwriting spin multiplicity in all steps. New "
                        + "value = " + spinMult);
            }
        }
        if (params.contains("NOSPIN_MULTIPLICITY"))
        {
            this.noSpinMult = true;
            if (verbosity > 0)
            {
                System.out.println(" Found 'NOSPIN_MULTIPLICITY' option. "
                      + "No charge will be specified in the NWChem input file");
            }
        }

        //COORDINATES TYPE
        if (params.contains("COORDINATESTYPE"))
        {
            coordType = 
                   params.getParameter("COORDINATESTYPE").getValue().toString();
            if (verbosity > 0)
            {
                    System.out.println(" Found 'COORDINATESTYPE' option. "
                                       + "Coordinates will be written as '" 
                                       + coordType + "' coordinates.");
            }
        }

        //Check compatibility of FREEZE options
        checkFreezingOptions(params);
//TODO: check compatibility with FREEZE options in jobdetails

        //SMARTS rules to freeze ICs
        if (params.contains("FREEZEIC"))
        {
            String all = params.getParameter("FREEZEIC").getValue().toString();
            this.smarts.putAll(getNamedICSMARTS(all));
        }

        //SMARTS rules to identify internal coords for ZCOORD directove
        if (params.contains("ZCOORD"))
        {
            zcoordInUse = true;
            String all = params.getParameter("ZCOORD").getValue().toString();
            this.smarts = getNamedICSMARTS(all);
            this.zCrdDetails = getOptsForNamedICSMARTS(all,smarts);
        }

        //SMARTS rules to freeze atoms in Cartesian coordinates
        if (params.contains("FREEZEATM"))
        {
            String all = params.getParameter("FREEZEATM").getValue().toString();
            this.smarts.putAll(getNamedAtomSMARTS(all));
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
          case PREPAREINPUTNWCHEM:
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
     * Check compatibility of options related to freezing atoms/coordinates
     */

    private void checkFreezingOptions(ParameterStorage params)
    {
        if (params.contains("FREEZEIC") && params.contains("FREEZEATM"))
        {
            Terminator.withMsgAndStatus("ERROR! Options FREEZEIC and FREEZEATM "
                + "cannot be used simultaneously. Check input.",-1);
        }
        if (params.contains("FREEZEIC") && params.contains("ZCOORD"))
        {
            Terminator.withMsgAndStatus("ERROR! Options FREEZEIC and "
               + "ZCOORD cannot be used simultaneously. Check input.",-1);
        }
        if (params.contains("ZCOORD") && params.contains("FREEZEATM"))
        {
            Terminator.withMsgAndStatus("ERROR! Options ZCOORD and "
                + "FREEZEATM cannot be used simultaneously. Check input.",-1);
        }
        if (params.contains("ZCOORD") && 
            !coordType.toUpperCase().equals("CARTESIAN"))
        {
            Terminator.withMsgAndStatus("ERROR! Options ZCOORD can only "
                + "be used in association with Cartesian coordinates.",-1);
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
     * Read options and values associated with SMARTS queries. 
     * This methos collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumend to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @param smarts the map of previously red SMARTS queries for which this
     * method is collecting the options
     * @return the map of all named details. Naming is based on incremental and
     * unique indexing where an integer index is used to identify the list of 
     * strings red from the same line.
     */

    private Map<String,ArrayList<String>> getOptsForNamedICSMARTS(
                                     String allLines, Map<String,String> smarts)
    {
        ArrayList<String> sortedMasterNames = getSortedSMARTSRefNames(smarts);
        
        Map<String,ArrayList<String>> map = 
                                        new HashMap<String,ArrayList<String>>();
        if (verbosity > 1)
        {
            System.out.println(" Importing options for IC-identifying SMARTS");
        }
        String[] lines = allLines.split("\\r?\\n");
        int ii=-1;
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            ii++;
            String[] parts = lines[i].split("\\s+");
            ArrayList<String> lstDetails = new ArrayList<String>();
            for (int j=0; j<parts.length; j++)
            {
                String str = parts[j].trim();

                // Ignore single-atom SMARTS
                if (str.equals("") || SMARTS.isSingleAtomSMARTS(str))
                {
                    continue;
                }

                lstDetails.add(str);
            }
            map.put(sortedMasterNames.get(ii),lstDetails);
        }

        return map;
    }

//------------------------------------------------------------------------------

    /**
     * Reads SMARTS for defining internal coordinates. 
     * This methos collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumend to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @return the map of all named smarts. Naming is based on incremental and
     * unique indexing where a major index is used to identify sets of SMARTS
     * red from the same line, and anothe idex is used to identify the 
     * order of the SMARTS red in the same line.
     */

    private Map<String,String> getNamedICSMARTS(String allLines)
    {
        Map<String,String> map = new HashMap<String,String>();
        if (verbosity > 1)
        {
            System.out.println(" Importing SMARTS to identify ICs");
        }
        String[] lines = allLines.split("\\r?\\n");
        int ii = -1;
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            // This allows to retrace the exact order in which lines are
            // given, yet without using the line number as index and allowing
            // to store multiple blocks of SMARTS queries in the same map
            ii = iNameSmarts.getAndIncrement();
            String masterName = MSTRULEROOT + ii;

            String[] parts = lines[i].split("\\s+");
            int jj = -1;
            for (int j=0; j<parts.length; j++)
            {
                String singleSmarts = parts[j].trim();

                // Ignore any string that is not a single-atom SMARTS
                if (singleSmarts.equals("") ||
                    !SMARTS.isSingleAtomSMARTS(singleSmarts))
                {
                    continue;
                }

                if (jj > 3)
                {
                    Terminator.withMsgAndStatus("ERROR! More than 4 atomic "
                               + "SMARTS for IC-defining SMARTS rule "
                               + ii + " (last SMARTS:" + singleSmarts + "). "
                               + "These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check the input.",-1);
                }
                jj++;
                String childName = masterName + SUBRULELAB + jj;
                map.put(childName,singleSmarts);
            }
            if (jj < 1)
            {
                Terminator.withMsgAndStatus("ERROR! Less than 2 atomic "
                               + "SMARTS for IC-freezing SMARTS rule "
                               + ii + ". These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check input.",-1);
            }
        }
        return map;
    }

//------------------------------------------------------------------------------

    /**
     * Imports the SMARTS for freezing atoms in Cartesian coords
     * @param allLines the string collecting all lines and including newline 
     * characters; this is the format the SMARTS strings are stored in a
     * ParametersStorage object
     */

    private Map<String,String> getNamedAtomSMARTS(String allLines)
    {
        Map<String,String> map = new HashMap<String,String>();
        if (verbosity > 0)
        {
            System.out.println(" Importing SMARTS to freeze ATOMS");
        }
        String[] lines = allLines.split("\\r?\\n");
        for (int i=0; i<lines.length; i++)
        {
            String singleSmarts = lines[i].trim();
            if (singleSmarts.equals(""))
            {
                continue;
            }
            String singleSmartsName = "smarts" + iNameSmarts.getAndIncrement();
            map.put(singleSmartsName,singleSmarts);
        }
        return map;
    }

//------------------------------------------------------------------------------

    /**
     * Write NWChem input file according to the given parameters. 
     */

    public void writeInput()
    {
        if (this.multiGeom)
        {
            writeInputWithMultipleGeometry();
        }
        else
        {
            writeInputForEachMol();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write NWChem input file according to the given parameters.
     * In case of multi entry structure files (i.e., SDF), every entry will
     * be placed in the same NWChem file specifying different geometry names.
     * The geometry names can be customized using the MULTIGEOMNWCHEM parameter.
     * It is assumed that all entries have the same atom lists.
     */

    public void writeInputWithMultipleGeometry()
    {
        //Get geometries
        try {
            ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
            switch (inFormat) {

                case "SDF":
                    mols = IOtools.readSDF(inFile);
                    break;

                case "XYZ":
                    mols = IOtools.readXYZ(inFile);
                    break;

                default:
                    Terminator.withMsgAndStatus("ERROR! NWChemInputWriter "
                        + "can write multi-geometry input files for NWChem "
                        + "only when starting from XYZ of SDF files. Make "
                        + "sure file '" + inFile +"' has proper "
                        + "format and extension.", -1);
            }
            if (mols.size() != geomNames.size())
            {
                Terminator.withMsgAndStatus("ERROR! Number of molecules (" 
                        + mols.size() + ") differs from number of geometry "
                        + "names (" + geomNames.size() + "). Please provide a "
                        + "consistent list of geometries and names.",-1);
            }

            FileUtils.mustNotExist(outNWFile);

            //Get custom charges
            if (customCharge)
            {
                //TODO: collect custom charges from input
            }

            //Get custom atom mass
            if (customMass)
            {
                //TODO: collect isotopes from input
            }

            int n=-1;
            for (IAtomContainer mol : mols)
            {
                n++;

                //
                // WARNING! The directive name becomes "GEOMETRY geomName".
                // This is because GEOMETRY directive can appear more than is
                // a single NWChem task. To male it unique we could the
                // geometry name (i.e., the default is "geometry" to the the 
                // name of the directive (i.e., "GEOMETRY").
                //
                String geomDirName = NWChemConstants.GEOMDIR + " " 
                                                             + geomNames.get(n);

                ArrayList<String> geomDirPath = new ArrayList<String>(
                                                    Arrays.asList(geomDirName));

                //Set the molecular representation
                if (coordType.toUpperCase().equals("CARTESIAN"))
                {
                    //Set Cartesian coords in GEOMETRY directive
                    NWChemDirectiveData crdData = makeCartesianCoordsData(mol);
                    nwcJob.getStep(0).setDataDirective(geomDirPath,crdData);

                    //Set freezing directives
                    if (smarts.size() > 0 && !zcoordInUse)
                    {
                        //Set the list of active (i.e., not frozen) atoms
                        ArrayList<String> setDirPath = 
                                    new ArrayList<String>(Arrays.asList("SET"));
                        NWChemKeyword kw = makeActiveAtomsKeyword(mol,smarts);
                        nwcJob.getStep(0).setKeyword(setDirPath,kw);
                    }
                    else if (smarts.size() > 0 && zcoordInUse)
                    {
                        //Set ZCOORD directive
                        NWChemDirective dZCoord = makeZCoordDirective(mol,
                                                            smarts,zCrdDetails);
                        nwcJob.getStep(0).setDirective(geomDirPath,dZCoord,true,
                                                                     true,true);
                    }
                }
                else if (coordType.toUpperCase().equals("INTERNAL"))
                {
                    //ICs and definition of constants are in same directive
                    NWChemDirective dZMat = makeInternalCoordsDirective(mol,
                                                                        smarts);
                    nwcJob.getStep(0).setDirective(geomDirPath,dZMat,true,true,
                                                                          true);
                }
                else
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to understand "
                        + "coordination type '" + coordType + "'.",-1);
                }

                //Set charge and spin multiplicity
                if (inFormat=="SDF" && !noCharge && !noSpinMult)
                {
                    chargeOrSpinFromIAC(mol);
                }
                if (!noCharge && !noSpinMult)
                {
                    checkChargeSpinNotAtDefault();
                    nwcJob.setAllCharge(charge);
                    nwcJob.setAllSpinMultiplicity(spinMult);
                 }

                for (int i=0; i<nwcJob.getNumberOfSteps(); i++)
                {
                    //Process task-specific parameters
                    if (nwcJob.getStep(i).hasACCParams())
                    {
                        if (verbosity > 0)
                        {
                            System.out.println(" Processing task-specific ACC "
                                                  + "parameters (task="+i+") ");
                            if (verbosity > 1)
                            {
                                System.out.println(
                                     nwcJob.getStep(i).getTaskSpecificParams());
                            }
                        }
                        processTaskSpecificACCParams(nwcJob.getStep(i), mol,
                                                                 geomDirPath,n);
                    }

                    //Project setting from general GEOMETRY directive into
                    // geometry-specific directive        
                    NWChemDirective genGDir = nwcJob.getStep(i).getDirective(
                                                       NWChemConstants.GEOMDIR);
                    if (genGDir != null)
                    {
                        NWChemDirective locDir = nwcJob.getStep(i).getDirective(
                                      new ArrayList<String>(),geomDirName,true);
                        for (NWChemKeyword kw : genGDir.getAllKeywords())
                        {
                            locDir.addKeyword(kw);
                        }
                        for (NWChemDirective d : genGDir.getAllSubDirectives())
                        {
                            locDir.addSubDirective(d);
                        }
                        for (NWChemDirectiveData dd : 
                                            genGDir.getAllDirectiveDataBlocks())
                        {
                            locDir.addDirectiveData(dd);
                        }
                    }
                }
            }

            //Remove aspecific geometry directives
            for (int i=0; i<nwcJob.getNumberOfSteps(); i++)
            {
                nwcJob.getStep(i).deleteDirective(new ArrayList<String>(),
                                                       NWChemConstants.GEOMDIR);
            }

            //Print to file
            printInput();
        } 
        catch (Throwable t) 
        {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                + "making multi-geometry NWChem input file from file "
                + inFile + "\n" + t, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write NWChem input file according to the given parameters.
     * In case of multi entry structure files (i.e., SDF),
     * a single input will be generated per each molecule using the name
     * of the molecule (title in SDF properties) as root for the output.
     * NWChem job details derive from the current status of the
     * <code>NWChemJob</code> field in this NWChemInputWriter.
     */

    public void writeInputForEachMol()
    {
        //Get molecule/s
        int n = 0;
        try {
            ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
            switch (inFormat) {

                case "SDF":
                    mols = IOtools.readSDF(inFile);
                    break;

                case "XYZ":
                    mols = IOtools.readXYZ(inFile);
                    break;

                case "NWCOUT":
                    NWChemOutputHandler nwcOutHndlr = new NWChemOutputHandler();
                    mols.add(nwcOutHndlr.extractLastOutputGeometry(inFile));
                    break;

                default:
                    Terminator.withMsgAndStatus("ERROR! NWChemInputWriter"
                        + " does not accept file format other than SDF and"
                        + " XYZ. Make sure file '" + inFile +"' has proper "
                        + " format and extension.", -1);
            }

            for (IAtomContainer mol : mols)
            {
                n++;
                String molName = MolecularUtils.getNameOrID(mol);

                //Set name of output
                // If there is more than one molecule we cannot use the given
                // outNWFile for the output.
                if (mols.size() > 1)
                {
                    outNWFile = molName + NWChemConstants.NWCINPEXTENSION;
                    outJDFile = molName + NWChemConstants.JDEXTENSION;
                    nwcJob.setAllPrefix(molName);
                }

                FileUtils.mustNotExist(outNWFile);

                //Get custom charges
                if (customCharge)
                {
                    //TODO: collect custom charges from input
                }

                //Get custom atom mass
                if (customMass)
                {
                    //TODO: collect isotopes from input
                }

                //Set the molecular representation
                if (coordType.toUpperCase().equals("CARTESIAN"))
                {
                    //Set Cartesian coords in GEOMETRY directive
                    NWChemDirectiveData crdData = makeCartesianCoordsData(mol);
                    ArrayList<String> dPath = new ArrayList<String>(
                                        Arrays.asList(NWChemConstants.GEOMDIR));
                    nwcJob.getStep(0).setDataDirective(dPath,crdData);

                    //Set freezing directives
                    if (smarts.size() > 0 && !zcoordInUse)
                    {
                        //Set the list of active (i.e., not frozen) atoms
                        dPath = new ArrayList<String>(Arrays.asList("SET"));
                        NWChemKeyword kw = makeActiveAtomsKeyword(mol,smarts);
                        nwcJob.getStep(0).setKeyword(dPath,kw);
                    }
                    else if (smarts.size() > 0 && zcoordInUse)
                    {
                        //Set ZCOORD directive
                        NWChemDirective dZCoord = makeZCoordDirective(mol,
                                                            smarts,zCrdDetails);
                        ArrayList<String> dPath2 = new ArrayList<String>(
                                        Arrays.asList(NWChemConstants.GEOMDIR));
                        nwcJob.getStep(0).setDirective(dPath2,dZCoord,true,true,
                                                                          true);
                        //TODO add "adjust" if this is a restart job
                    }
                }
                else if (coordType.toUpperCase().equals("INTERNAL"))
                {
                    //ICs and definition of constants are in same directive
                    NWChemDirective dZMat = makeInternalCoordsDirective(mol,
                                                                        smarts);
                    ArrayList<String> dPath = new ArrayList<String>(
                                        Arrays.asList(NWChemConstants.GEOMDIR));
                    nwcJob.getStep(0).setDirective(dPath,dZMat,true,true,true);
                }
                else
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to understand "
                        + "coordination type '" + coordType + "'.",-1);
                }

                //Set charge and spin multiplicity
                if (inFormat=="SDF" && !noCharge && !noSpinMult)
                {
                    chargeOrSpinFromIAC(mol);
                }
                if (!noCharge && !noSpinMult)
                {
                    checkChargeSpinNotAtDefault();
                    nwcJob.setAllCharge(charge);
                    nwcJob.setAllSpinMultiplicity(spinMult);
                }

                //Process task-specific parameters
                for (int i=0; i<nwcJob.getNumberOfSteps(); i++)
                {
                    if (nwcJob.getStep(i).hasACCParams())
                    {
                        if (verbosity > 0)
                        {
                            System.out.println(" Processing task-specific ACC "
                                                  + "parameters (task="+i+") ");
                            if (verbosity > 1)
                            {
                                System.out.println(
                                     nwcJob.getStep(i).getTaskSpecificParams());
                            }
                        }
                        processTaskSpecificACCParams(nwcJob.getStep(i), mol, 
                                        new ArrayList<String>(Arrays.asList(
                                                   NWChemConstants.GEOMDIR)),0);
                    }
                }

                //Print to file
                printInput();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                + "making NWChem input for molecule " + n + " from file " 
                + inFile + "\n" + t, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Method to process all task specific parameters for AutoCompChem. 
     * @param task the specific task 
     * @param mol the molecule 
     * @param geomDirPath the path to the geometry directive. This is
     * constant unless the NWChem job contains more than one geometries, in 
     * which case there will be more than one "GEOMETRY" directive, and to make
     * their name be unique we consider the "extended" name, which includes the
     * name of the geometry (i.e., by default "geometry") to the name of the 
     * directive (i.e., "GEOMETRY" in the current versions on NWChem).
     * @param reprtition an integer indicating if this is a repetition due
     * to multi-geometry input. Use zero to remove any effect of this 
     * parameter. Non-zero values make the method avoid to add redundant info.
     */ 

    private void processTaskSpecificACCParams(NWChemTask task, 
                                                             IAtomContainer mol,
                                                  ArrayList<String> geomDirPath,
                                                                 int repetition)
    {
        ParameterStorage params = task.getTaskSpecificParams();

        //Check compatibility of FREEZE options
        checkFreezingOptions(params);

        //SMARTS rules to freeze ICs
        if (params.contains("FREEZEIC"))
        {
            //TODO
            Terminator.withMsgAndStatus("ERROR! The task-specific IC-freezing "
                                          + "option is not yet developed." ,-1);
/*
            String all = params.getParameter("FREEZEIC").getValue().toString();
            ... TODO ...
*/
        }

        if (params.contains("ZCOORD"))
        {
            zcoordInUse = true;
            String all = params.getParameter("ZCOORD").getValue().toString();
            Map<String,String> tsSmarts = getNamedICSMARTS(all);
            Map<String,ArrayList<String>> tsZCrdDets = 
                                          getOptsForNamedICSMARTS(all,tsSmarts);
            NWChemDirective dZCoord = makeZCoordDirective(mol,tsSmarts,
                                                                    tsZCrdDets);
            task.setDirective(geomDirPath,dZCoord,true,true,true);
        }

        //SMARTS rules to freeze atoms in Cartesian coordinates
        if (params.contains("FREEZEATM"))
        {
            String all = params.getParameter("FREEZEATM").getValue().toString();
            ArrayList<String> dp = new ArrayList<String>(Arrays.asList("SET"));
            NWChemKeyword kw = makeActiveAtomsKeyword(mol,
                                                       getNamedAtomSMARTS(all));
            task.setKeyword(dp,kw);
        }

        //CHARGE
        if (params.contains("CHARGE"))
        {
            charge = Integer.parseInt(
                        params.getParameter("CHARGE").getValue().toString());
            task.setCharge(charge);
            if (verbosity > 0)
            {
                System.out.println(" Found 'CHARGE' option. Overwriting "
                                + "charge in one step. New value = " + charge);
            }
        }

        //SPIN_MULTIPLICITY
        if (params.contains("SPIN_MULTIPLICITY"))
        {
            spinMult = Integer.parseInt(
                params.getParameter("SPIN_MULTIPLICITY").getValue().toString());
            task.setSpinMultiplicity(spinMult);
            if (verbosity > 0)
            {
                    System.out.println(" Found 'SPIN_MULTIPLICITY' option. "
                        + "Overwriting spin multiplicity in one step. New "
                        + "value = " + spinMult);
            }
        }

        //Atom specific or customised basis set
        if (params.contains(BasisSetConstants.ATMSPECBS))
        {
            boolean goon = true;

            //The locPars are the params that we'll use to create the basis set
            ParameterStorage locPars = new ParameterStorage();
            
            Parameter atmSpecBSParam = new Parameter(
                params.getParameter(BasisSetConstants.ATMSPECBS).getReference(),
                params.getParameter(BasisSetConstants.ATMSPECBS).getType(),
                params.getParameter(BasisSetConstants.ATMSPECBS).getValue());
            if (repetition > 0)
            {
                //Avoid writing the basis set when atom tags are reused.
                //Nevertheless we still match all rules to spot potential issues
            	//So, here we modify the basis set matching rules as to keep only
            	//those that are needed also in repetitions>0
                if (sameTagsInMultigeom)
                {
                    goon = false;
                }

                //Ignore elemental symbol-based rules: they apply to all 
                // geometries and must not be repeated
                BasisSetGenerator bsg = new BasisSetGenerator();
                bsg.setBSMatchingRules(params.getParameter(
                            BasisSetConstants.ATMSPECBS).getValue().toString());
                StringBuilder sb = new StringBuilder();
                for (String rRef : bsg.getBSMatchingRules().keySet())
                {
                    BSMatchingRule bsRule = bsg.getBSMatchingRules().get(rRef);
                    if (!bsRule.getType().toUpperCase().equals(
                                            BasisSetConstants.ATMMATCHBYSYMBOL))
                    {
                        sb.append(bsRule.toParsableString());
                        sb.append(System.getProperty("line.separator"));
                    }
                }
                if (sb.length() == 0)
                {
                    goon = false;
                }
                atmSpecBSParam.setValue(sb.toString());

                // Allow some partial assignation of basis set
                // this because we might have removed some redundant rule 
                // and this can lead to a partial match
                locPars.setParameter(new Parameter(
                		BasisSetConstants.ALLOWPARTIALMATCH,
                            		 NamedDataType.BOOLEAN, "true"));
            }
            locPars.setParameter(atmSpecBSParam);
            locPars.setParameter(new Parameter(ACCConstants.VERBOSITYPAR,
                          NamedDataType.INTEGER, verbosity));
            
            // Do it only if there is something to do...
            if (goon)
            {
            	// Get a worker to deal with the basis set generation task
            	ParameterStorage paramsForBasisSetGen = locPars.clone();
            	paramsForBasisSetGen.setParameter(new Parameter("TASK",
            		NamedDataType.STRING, "GENERATEBASISSET"));
            	Worker w = WorkerFactory.createWorker(paramsForBasisSetGen);
                BasisSetGenerator bsg = (BasisSetGenerator) w;
                
                //...and build the basis set
                BasisSet bs = bsg.assignBasisSet(mol);
    
                //Report the basis function section as NWChem directive
                String bsStr = bs.toInputFileStringBS("nwchem");
                ArrayList<String> bsLin = new ArrayList<String>(Arrays.asList(
                                                            bsStr.split("\n")));
                // Get rid of the directive name and tail
                bsLin.remove(0);
                bsLin.remove(bsLin.size()-1);
    
                NWChemDirective dBasis = new NWChemDirective(
                                                      NWChemConstants.BASISDIR);
                NWChemDirectiveData dBSData = 
                                    new NWChemDirectiveData("atomSpecBS",bsLin);
                dBasis.addDirectiveData(dBSData);
    
                //Store the directive into the task
                ArrayList<String> dPath = new ArrayList<String>();
                task.setDirective(dPath,dBasis,false,false,!multiGeom);
    
                if (bs.hasECP())
                {
                    String eStr = bs.toInputFileStringECP("nwchem");
                    ArrayList<String> eLin = new ArrayList<String>(
                                               Arrays.asList(eStr.split("\n")));
                    // Get rid of the directive name and tail
                    eLin.remove(0);
                    eLin.remove(eLin.size()-1);
                    NWChemDirective dECP = new NWChemDirective(
                                                        NWChemConstants.ECPDIR);
                    NWChemDirectiveData dECPData = new NWChemDirectiveData(
                                                            "atomSpecECP",eLin);
                    dECP.addDirectiveData(dECPData);
                    task.setDirective(dPath,dECP,false,false,!multiGeom);
                }
            }
        }

        //TODO: add other options that can be task specific

    }

//------------------------------------------------------------------------------

    /**
     * Return true if the charge or the spin are overwritten according to the
     * IAtomContainer properties "CHARGE" and "SPIN_MULTIPLICITY"
     * @param mol the molecule from which we get charge and spin
     */

    private boolean chargeOrSpinFromIAC(IAtomContainer mol)
    {
        boolean res = false;

        String str = " Using IAtomContainer to overwrite set charge and "
                        + "spin multiplicity.\n"
                        + " From c = " + charge + " and s.m. = "
                        + spinMult;

        //Deal with the charge
        if (MolecularUtils.hasProperty(mol, "CHARGE"))
        {
            res = true;
            charge = Integer.parseInt(mol.getProperty("CHARGE").toString());
        }

        //Deal with the spin multiplicity
        if (MolecularUtils.hasProperty(mol, "SPIN_MULTIPLICITY"))
        {
            res = true;
            spinMult = Integer.parseInt(
                        mol.getProperty("SPIN_MULTIPLICITY").toString());
        }

        if (verbosity > 0)
        {
            if (res)
            {
                System.out.println(str + " to c = " + charge + " and s.m. = "
                                + spinMult);
            }
        }

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Check whether charge or spin were defined. If one is not defined, 
     * terminates the program.
     */

    private void checkChargeSpinNotAtDefault()
    {
        if (charge == def)
        {
            Terminator.withMsgAndStatus("ERROR! Property "
                + "<CHARGE> cannot be defined.",-1);
        }

        if (spinMult == def)
        {
            Terminator.withMsgAndStatus("ERROR! Property "
                + "<SPIN_MULTIPLICITY> cannot be defined.", -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Creates a data block with the Cartesian coordinates of the given 
     * molecule.
     * Note that, contarily to what happens in case of internal coordinates,
     * the directive defining the Crtesian coordinates does not have to 
     * capability to define frozen atoms (i.e., atoms for which the set of
     * Cartesina coordinates will not change). In accordance to this,
     * frozen atom are identified and a proper directive is generated in the 
     * method {@link #makeActiveAtomsKeyword(IAtomContainer)}.
     * @param mol the CDK representation of the system
     */

    private NWChemDirectiveData makeCartesianCoordsData(IAtomContainer mol)
    {
        // Reset counter when using the same atom tags for multiple geoms
        if (sameTagsInMultigeom)
        {
            iAtomTag = new AtomicInteger(1);
        }

        // Preare the Cartesian coordinates data block
        ArrayList<String> lines = new ArrayList<String>();
        for (IAtom atm : mol.atoms())
        {
            StringBuilder sb = new StringBuilder(atm.getSymbol());
            if (nwcJob.requiresAtomTags())
            {
                sb.append(iAtomTag.getAndIncrement());
                atm.setProperty(ACCConstants.ATMTAGPROP,sb.toString());
            }
            sb.append(String.format(" %5.8f",
                                            MolecularUtils.getCoords3d(atm).x));
            sb.append(String.format(" %5.8f",
                                            MolecularUtils.getCoords3d(atm).y));
            sb.append(String.format(" %5.8f",
                                            MolecularUtils.getCoords3d(atm).z));
            if (customCharge)
            {
                sb.append(" ").append(atm.getCharge());
            }
            if (customMass)
            {
                sb.append(" ").append(atm.getExactMass());
            }
            lines.add(sb.toString());
        }
        NWChemDirectiveData coordsData = new NWChemDirectiveData(
                                              NWChemConstants.CARTCOORDS,lines);
        return coordsData;
    }

//------------------------------------------------------------------------------

    /**
     * Creates a keyword to specify the active atoms: atoms which Cartesian
     * coordinates are not frozen. 
     * @param mol the CDK representation of the system
     * @param freezingSmarts the map of SMARTS queries identifying the atoms to
     * freeze  
     */

    private NWChemKeyword makeActiveAtomsKeyword(IAtomContainer mol, 
                                              Map<String,String> freezingSmarts)
    {
        // make an initial list of active atoms including all
        ArrayList<Integer> activeIds = new ArrayList<Integer>();
        for (int i=-1; i<(mol.getAtomCount()-1); i++, activeIds.add(i));

        if (verbosity > 1)
        {
            System.out.println("Looking for atoms to freeze.");
        }

        // Identify atom to freeze
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,freezingSmarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " +cause,-1);
        }
        for (String key : freezingSmarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
                 System.out.println("WARNING! No match for SMARTS query "
                                 + freezingSmarts.get(key) + " in molecule "
                                 + MolecularUtils.getNameOrID(mol));
                continue;
            }
            List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(key);
            String reportFrozen = "";
            boolean reportedMultiple = false;
            for (List<Integer> atmIDsLst : allMatches)
            {
                if (atmIDsLst.size() > 1)
                {
                    if (verbosity > 0 && reportedMultiple)
                    {
                        reportedMultiple = true;
                        System.out.println("WARNING! Atom-freezing SMARTS '" 
                        + freezingSmarts.get(key) +"' matched a group of "
                        + atmIDsLst.size() + " atoms that will all be frozen.");
                    }
                }
                for (Integer frozenAtmId : atmIDsLst)
                {
                    activeIds.remove(Integer.valueOf(frozenAtmId));
                    reportFrozen = reportFrozen + frozenAtmId + " ";
                }
            }
            if (verbosity > 1)
            {
                System.out.println("Atom-freezing SMARTS matched by atom IDs "
                                                                + reportFrozen);
            }
        }

        // Prepare the keyword
        ArrayList<String> list = new ArrayList<String>();
        int prevId = activeIds.get(0);
        int rangeStart = prevId;
        for (int i=1; i<(activeIds.size()+1); i++)
        {
            int currentId;
            if (i<activeIds.size())
            {
                currentId = activeIds.get(i);
            }
            else
            {
                currentId = prevId;
            }
            if (currentId != (prevId+1))
            {
                if (rangeStart != prevId)
                {
                    String range = (rangeStart+1) + ":" + (prevId+1) + " ";
                    list.add(range);
                }
                else 
                {
                    String single = (prevId+1) + " ";
                    list.add(single);
                }
                rangeStart = currentId;
            }
            prevId = currentId;
        }
        
        NWChemKeyword kw = new NWChemKeyword(NWChemConstants.ACTIVEATOMS,true,
                                                                          list);
        return kw;
    }

//------------------------------------------------------------------------------

    /**
     * Creates a ZCOORD directive to define internal coordinates with/without
     * a numerical value and to freeze/unfreeze them.
     * @param mol the CDK representation of the system
     * @param smarts the map of SMARTS queries identifying the internal coords
     * to include in the ZCOORD directive
     * @param zCrdDets the further detail per each class of internal coordinate
     * to include in the ZCOORD directive (i.e., the initial value, and/or the
     * <code>constant</code> keyword).
     * @return the ZCOORD directive
     */

    private NWChemDirective makeZCoordDirective(IAtomContainer mol,
              Map<String,String> smarts, Map<String,ArrayList<String>> zCrdDets)
    {
        // Look for matches to define the IC to work with
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! Cannot identify internal "
                               + "coordinates for ZCOORD directive" + cause,-1);
        }

        //TODO: should make this a method... 
        // Reorganize lists to group single-atom smarts that pertain to same IC
        Map<String,ArrayList<ArrayList<Integer>>> allIcRules =
                            new HashMap<String,ArrayList<ArrayList<Integer>>>();
        boolean skipRule =false;
        for (String icRuleName : getSortedSMARTSRefNames(smarts))
        {
            String preStr = icRuleName + SUBRULELAB;
            ArrayList<ArrayList<Integer>> partsIcRule =
                                       new ArrayList<ArrayList<Integer>>(4);
            for (int i=0; i<4; i++)
            {
                partsIcRule.add(null);
            }
            for (String key : smarts.keySet())
            {
                if (!key.startsWith(preStr))
                {
                    continue;
                }
                int pos = Integer.parseInt(key.substring(preStr.length()));
                if (msq.getNumMatchesOfQuery(key) == 0)
                {
                    skipRule = true;
                    System.out.println("WARNING! No match for SMARTS query "
                         + smarts.get(key) + " in molecule "
                         + MolecularUtils.getNameOrID(mol) + ". Skipping "
                         + "freezing rule '" + icRuleName + "'.");
                    continue;
                }

                List<List<Integer>> matches = msq.getMatchesOfSMARTS(key);

                ArrayList<Integer> allAtmsIds = new ArrayList<Integer>();
                for (List<Integer> innerLst : matches)
                {
                    allAtmsIds.addAll(innerLst);
                }
                partsIcRule.set(pos,allAtmsIds);
            }
            if (skipRule)
            {
                continue;
            }
            allIcRules.put(icRuleName,partsIcRule);
        }

        // Get zcoords by comparing matched atoms with topology
        ArrayList<ArrayList<String>> linesDataBlock = 
                                             new ArrayList<ArrayList<String>>();
        linesDataBlock.add(new ArrayList<String>());
        linesDataBlock.add(new ArrayList<String>());
        linesDataBlock.add(new ArrayList<String>());
        for (String ruleKey : allIcRules.keySet())
        {
            ArrayList<ArrayList<Integer>> partsIcRule = allIcRules.get(ruleKey);
            for (Integer idA : partsIcRule.get(0))
            {
                IAtom atmA = mol.getAtom(idA);
                for (Integer idB : partsIcRule.get(1))
                {
                    IAtom atmB = mol.getAtom(idB);
                    if (!mol.getConnectedAtomsList(atmA).contains(atmB)
                        || atmA == atmB)
                    {
                        continue;
                    }
                    
                    if (partsIcRule.get(2) != null)
                    {
                        for (Integer idC : partsIcRule.get(2))
                        {
                            IAtom atmC = mol.getAtom(idC);
                            if (!mol.getConnectedAtomsList(atmB).contains(atmC)
                                || atmA == atmC || atmB == atmC)
                            {
                                continue;
                            }
                            if (partsIcRule.get(3) != null)
                            {
                                for (Integer idD : partsIcRule.get(3))
                                {
                                    IAtom atmD = mol.getAtom(idD);
                                    if (atmA == atmD || atmB == atmD 
                                         || atmC == atmD)
                                    {
                                        continue;
                                    }
                                    
                                    if(!mol.getConnectedAtomsList(
                                                           atmB).contains(atmD)
                                        && !mol.getConnectedAtomsList(
                                                         atmC).contains(atmD))
                                    {
                                        continue;
                                    }
                                    processSingleCombinationOfIDs(
                                        new ArrayList<Integer>(
                                                Arrays.asList(idA,idB,idC,idD)),
                                                          zCrdDets.get(ruleKey),
                                                                linesDataBlock);
                                }
                            }
                            else
                            {
                                processSingleCombinationOfIDs(
                                    new ArrayList<Integer>(
                                                    Arrays.asList(idA,idB,idC)),
                                                          zCrdDets.get(ruleKey),
                                                                linesDataBlock);
                            }
                        }
                    }
                    else
                    {
                        processSingleCombinationOfIDs(
                                 new ArrayList<Integer>(Arrays.asList(idA,idB)),
                                                          zCrdDets.get(ruleKey),
                                                                linesDataBlock);
                    }
                }
            }
        }

        if (verbosity > 0)
        {
            System.out.println(" Total internal coords. in ZCOORD directive: " 
                                               +(linesDataBlock.get(0).size() 
                                               + linesDataBlock.get(1).size() 
                                               + linesDataBlock.get(2).size()));
        }

        // Sort lists so it's easy to read
        ArrayList<String> linesBnd = linesDataBlock.get(0);
        ArrayList<String> linesAng = linesDataBlock.get(1);
        ArrayList<String> linesTor = linesDataBlock.get(2);
        Collections.sort(linesBnd, new NumberAwareStringComparator());
        Collections.sort(linesAng, new NumberAwareStringComparator());
        Collections.sort(linesTor, new NumberAwareStringComparator());
        ArrayList<String> lines = new ArrayList<String>();
        lines.addAll(linesBnd);
        lines.addAll(linesAng);
        lines.addAll(linesTor);

        // Return it as a directive
        NWChemDirective zcrdDir = new NWChemDirective(NWChemConstants.ZCRDDIR);
        zcrdDir.addDirectiveData(new NWChemDirectiveData("zcoord",lines));
        return zcrdDir;
    }

//------------------------------------------------------------------------------

    /**
     * Process a single set of IDs that represent a candidate internal coord.
     * For a list of atom IDs, we evaluate if the reordered list has been 
     * already defined, and prepara the proper string to be used in the ZCOORD
     * directive, and add it to the proper block of strings depending
     * on whether is a bond, angle, or torsion
     * @param ids the list of atom indeces defining the zcoord
     * @param details the list of options/values/keywords to be appended to
     * the zcoord line
     * @param linesDataBlock the collector of the results: string with a format
     * suitable for ZCOORD directive.
     */

    private void processSingleCombinationOfIDs(ArrayList<Integer> ids,
                                                      ArrayList<String> details,
                                    ArrayList<ArrayList<String>> linesDataBlock)
    {
        // Format the list of IDs
        int numIds = ids.size();
        String crdType = "";
        switch (numIds)
        {
            case 2:
                crdType = "BOND";
                Collections.sort(ids);
                break;

            case 3:
                crdType = "ANGLE";
                if (ids.get(0) > ids.get(2))
                {
                    Collections.reverse(ids);
                }
                break;

            case 4:
                crdType = "TORSION";
                // NOTE: don't revert the 4-tupla: it could generate confusion
                // in case of inproper torsions.
                // For instance, in the system D-C(-A)-B the ideal definition of
                // of the inptoper torsion is D,C,B,A, but upon inversion it
                // would become A,B,C,B, which looks weird because A and B are
                // not connected to each other.
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Something badly wrong. "
                    + "I got an internal coordinate with " + numIds
                    + " IDs. Please report this to the authors.",-1);
        }

        // Compare with existing ones 
        // TODO: this whould be much more efficient if dealing with objects 
        // rather than with strings... No time for this now.
        for (String line : linesDataBlock.get(numIds-2)) 
        {
            String[] parts = line.split("\\s+");
            boolean alreadyDefined = true;
            for (int j=1; j<(numIds+1); j++)
            {
                // WARNING: 1-based IDs are used for storage
                int idOnExistingLine = Integer.parseInt(parts[j]) - 1;
                if (idOnExistingLine == ids.get(j-1))
                {
                    continue;
                }
                else
                {
                    alreadyDefined = false;
                    if (j == 3 && numIds == 4) //only torsions enter this block
                    {
                        int nextIdOnExisting = Integer.parseInt(parts[j+1]) - 1;
                        if (idOnExistingLine == ids.get(3) &&
                            nextIdOnExisting == ids.get(2))
                        {
                            alreadyDefined = true;
                        }
                    }
                    break;
                }
            }
            if (alreadyDefined)
            {
                // Exit without storing the new line
                return;
            }
        }

        // Build the string
        StringBuilder sb = new StringBuilder();
        sb.append(crdType).append(" ");
        for (Integer id : ids)
        {
            //WARNING: here we chenge from 0-based to 1-based
            sb.append(id + 1).append(" ");
        }
        for (String s : details)
        {
            sb.append(s).append(" ");
        }

        // Store 
        linesDataBlock.get(numIds-2).add(sb.toString());
    }    

//------------------------------------------------------------------------------

    /**   
     * Creates a directive to define the geometry by means of internal 
     * coordinates.
     * @param mol the CDK representation of the system
     * @param freezingSmarts the map of SMARTS queries identifying the atoms to
     * freeze
     */

    private NWChemDirective makeInternalCoordsDirective(IAtomContainer mol,
                                              Map<String,String> freezingSmarts)
    {
//TODO
        if (nwcJob.requiresAtomTags())
        {
            Terminator.withMsgAndStatus("ERROR! Some task has required the use "
                + "of atom tags, but this is not yet implemented for interal "
                + "coordinates input. Use Cartesian coordiantes input or avoid "
                + "tasks that require atom tags (... or implement the use of "
                + "atoms tags). Exiting!",-1);
/*
            sb.append(iAtomTag.getAndIncrement());
            atm.setProperty(ACCConstants.ATMTAGPROP,sb.toString());
*/
        }

        //Build the Z-Matrix from cartesian coordinates and connectivity
        ParameterStorage locPar = new ParameterStorage();
        locPar.setParameter(new Parameter("TASK",NamedDataType.STRING,
        		"PRINTZMATRIX"));
        locPar.setParameter(params.getParameter("VERBOSITY"));
        locPar.setParameter(new Parameter("MOL",
        		NamedDataType.IATOMCONTAINER,mol));
        Worker w = WorkerFactory.createWorker(locPar);
        ZMatrixHandler zmh = (ZMatrixHandler) w;
        
        ZMatrix zmat = zmh.makeZMatrix();        
        
        NWChemDirective zmatDir = new NWChemDirective(NWChemConstants.ZMATDIR);

        if (freezingSmarts.size() > 0)
        {
            //Look for IC to freeze
            Set<InternalCoord> variables = new HashSet<InternalCoord>();
            Set<InternalCoord> constants = new HashSet<InternalCoord>();
            if (verbosity > 1)
            {
                System.out.println("Looking for the ICs to freeze");
            }
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,freezingSmarts,
                                                                     verbosity);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                Terminator.withMsgAndStatus("ERROR! " +cause,-1);
            }

            // Reorganize lists to group single-atom smarts of the same IC
            Map<String,ArrayList<ArrayList<Integer>>> allIcFreezingRules =
                            new HashMap<String,ArrayList<ArrayList<Integer>>>();
            boolean skipRule =false;
            for (String icRuleName : getSortedSMARTSRefNames(freezingSmarts))
            {
                String preStr = icRuleName + SUBRULELAB;
                ArrayList<ArrayList<Integer>> componentsIcRule = 
                                           new ArrayList<ArrayList<Integer>>(4);
                for (int i=0; i<4; i++)
                {
                    componentsIcRule.add(null);
                }
                for (String key : freezingSmarts.keySet())
                {
                    if (!key.startsWith(preStr))
                    {
                        continue;
                    }
                    int pos = Integer.parseInt(key.substring(preStr.length()));
                    if (msq.getNumMatchesOfQuery(key) == 0)
                    {
                        skipRule = true;
                        System.out.println("WARNING! No match for SMARTS query "
                             + freezingSmarts.get(key) + " in molecule "
                             + MolecularUtils.getNameOrID(mol) + ". Skipping "
                             + "freezing rule '" + icRuleName + "'.");
                        continue;
                    }

                    List<List<Integer>> matches = msq.getMatchesOfSMARTS(key);

                    ArrayList<Integer> allAtmsIds = new ArrayList<Integer>();
                    for (List<Integer> innerLst : matches)
                    {
                        allAtmsIds.addAll(innerLst);
                    }
                    componentsIcRule.set(pos,allAtmsIds);
                }
                if (skipRule)
                {
                    continue;
                }
                allIcFreezingRules.put(icRuleName,componentsIcRule);
            }

            // Compare matched tuples with IC in z-matrix => the constants
            for (String ruleKey : allIcFreezingRules.keySet())
            {
                ArrayList<ArrayList<Integer>> componentsIcRule = 
                                                allIcFreezingRules.get(ruleKey);
                for (Integer idA : componentsIcRule.get(0))
                {
                    for (Integer idB : componentsIcRule.get(1))
                    {
                        if (componentsIcRule.get(2) != null)
                        {
                            for (Integer idC : componentsIcRule.get(2))
                            {
                                if (componentsIcRule.get(3) != null)
                                {
                                    for (Integer idD : componentsIcRule.get(3))
                                    {
                                        checkTuplaVsZCoords(ruleKey, constants,
                                                    zmh, new ArrayList<Integer>(
                                               Arrays.asList(idA,idB,idC,idD)));
                                    }
                                }
                                else
                                {
                                    checkTuplaVsZCoords(ruleKey, constants, zmh,
                                                         new ArrayList<Integer>(
                                                   Arrays.asList(idA,idB,idC)));
                                }
                            }
                        }
                        else
                        {
                            checkTuplaVsZCoords(ruleKey, constants, zmh,
                                                         new ArrayList<Integer>(
                                                       Arrays.asList(idA,idB)));
                        }
                    }
                }
            }

            if (verbosity > 0)
            {
                System.out.println(" Total frozen ICs: " + constants.size() 
                                             + "/" + zmh.getIntCoords().size());
            }

            //make list of active atoms (complementary of 'constants')
            variables.addAll(zmh.getIntCoords());
            variables.removeAll(constants);

            //Report the zmatrix with symbolic variables
            zmatDir.addDirectiveData(new NWChemDirectiveData("zmatrix",
                                               zmat.toLinesOfText(true,false)));
            
            //Report variables as directive data
            ArrayList<String> varLines = new ArrayList<String>();
            varLines.add(NWChemConstants.VARIABLEICBLOCK);
            for (InternalCoord zCrd : variables)
            {
                varLines.add(" " + zCrd.getVariabeDefLine(" "));
            }
            NWChemDirectiveData varData = new NWChemDirectiveData(
                                                NWChemConstants.VARIABLEICBLOCK,
                                                                      varLines);
            zmatDir.addDirectiveData(varData);

            //Report constants as directive data
            ArrayList<String> constLines = new ArrayList<String>();
            varLines.add(NWChemConstants.CONSTANTICBLOCK);
            for (InternalCoord zCrd : constants)
            {
                constLines.add(" " + zCrd.getVariabeDefLine(" "));
            }
            NWChemDirectiveData constData = new NWChemDirectiveData(
                                                NWChemConstants.CONSTANTICBLOCK,
                                                                    constLines);
            zmatDir.addDirectiveData(constData);
        }
        else
        {
            zmatDir.addDirectiveData(new NWChemDirectiveData("zmatrix",
                                              zmat.toLinesOfText(false,false)));
        }

        return zmatDir;
    }

//------------------------------------------------------------------------------
 
    /**
     * Check if a tupla of indexes is used by any of the internal coordinates
     * defined by a ZMatrixHandler
     * @param rule name of the IC-freezing rule
     * @param constants list of pointers to frozed coordinates
     * @param zmh the z-matrix handler
     * @param tupla the tupla
     */

    private void checkTuplaVsZCoords(String name, Set<InternalCoord> constants, 
                                   ZMatrixHandler zmh, ArrayList<Integer> tupla)
    {
        for (InternalCoord zCrd : zmh.getIntCoords())
        {
            if (zCrd.compareIDs(tupla))
            {
                if (verbosity > 1)
                {
                    System.out.println(" IC defined by "
                          + tupla.size() + "-tupla " + tupla
                          + " " + zCrd.getName() + " (matched "
                          + "SMARTS: " + name + ")");
                }
                constants.add(zCrd);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write NWChem input file, either single- or multi-step job, according
     * to the currently loaded NWChemJob.
     */

    public void printInput()
    {
        if (verbosity > 0)
        {
             System.out.println(" Writing NWChem input file: " + outNWFile);
        }

        IOtools.writeTXTAppend(outNWFile,nwcJob.toLinesInput(),false);
        IOtools.writeTXTAppend(outJDFile,nwcJob.toLinesJobDetails(),false);
    }

//------------------------------------------------------------------------------

}
