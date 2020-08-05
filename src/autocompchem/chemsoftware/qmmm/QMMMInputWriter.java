package autocompchem.chemsoftware.qmmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.tinker.TinkerXYZReader;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.connectivity.ConnectivityTable;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for QMMM.
 * 
 * @author Marco Foscato
 */

public class QMMMInputWriter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTQMMM)));

    /**
     * Input filename
     */
    private String inFile;

    /**
     * Input format identifier
     */
    private String inFormat = "nd";

    /**
     * Output name
     */
    private String outFile;

    /**
     * Coordinates file (used only if MM or QM/MM calculations)
     */
    private String crdFile;

    /**
     * Name of QM software
     */
    private String qmsoftware = "GAUSSIAN-09";

    /**
     * Name of MM software
     */
    private String mmsoftware = "TINKER-6.3.3";

    /**
     * Name of force field
     */
    private String forcefield = "MMFF94";

    /**
     * External source of atom types
     */
    private String atmTypFile = "";

    /**
     * Format of external source of atom types
     */
    private String atmTypFormat = "";

    /**
     * SMARTS for freezing atoms
     */
    private Map<String,String> frozenSmarts = new HashMap<String,String>();

    /**
     * SMARTS for defining QM atoms
     */
    private Map<String,String> qmAtmSmarts = new HashMap<String,String>();

    /**
     * SMARTS for defining cap atoms (collected by SMARTS string name)
     */
    private Map<String,String> capAtmSmarts = new HashMap<String,String>(); 

    /**
     * Cap atom specifications (collected by SMARTS string name)
     */
    private Map<String,String> capAtmSpecs = new HashMap<String,String>();

    /**
     * Atom indexes of QM atoms (1-based).
     */
    private ArrayList<Integer> qmAtmsIds = new ArrayList<Integer>();

    /**
     * Title of the job
     */ 
    private String title;

    /**
     * Default value for undefined parameters (non-sense value)
     */
    private final int def = -999999;

    /**
     * Value of the total charge
     */
    private int charge = def;

    /**
     * Value of the spin multiplicity of the whole system
     */
    private int spinMult = def;

    /**
     * Object defining the detail of the job
     */
    private QMMMJob qmmmJob;

    /**
     * Verbosity level
     */
    private int verbosity = 1;


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty QMMMInputWriter
     */

    public QMMMInputWriter()
    {
    }

//------------------------------------------------------------------------------

    //TODO: move to class doc
    /**
     * Construct a new QMMMInputWriter using the parameters taken from a
     * {@link autocompchem.datacollections.ParameterStorage}.<br>
     * The parameters for preparing the input for QMMM
     * have to be defines in the form of
     * a jobdetails formatted text file (see {@link QMMMJob}).<br>
     * Parameters required:
     * <ul>
     * <li>
     * <b>INFILE</b>: name of the structure file (i.e. path/name.sdf).
     * </li>
     * <li>
     * <b>JOBDETAILS</b>: formatted text file defining all
     * the details of a {@link QMMMJob}.
     * The definition of the format of jobdetails files can be found in
     * {@link QMMMJob} documentation.
     * </li>
     * <li>
     * (optional) <b>VERBOSITY</b> verbosity level.
     * </li>
     * <li>
     * (optional) <b>OUTFILE</b> name of the output file (i.e., input for QMMM).
     * If this option is omitted the default name of the output is build from 
     * the root of the structure file name.
     * </li>
     * </ul>
     * Optional parameters not needed if JOBDETAILS option is in use, but
     * that will overwrite JOBDETAILS specifications if both JOBDETAILS and
     * these options are specified in the
     * {@link autocompchem.datacollections.ParameterStorage}.
     * <ul>
     * <li>
     * (optional) <b>TITLE</b> title line for the output file
     * </li>
     * <li>
     * (optional) <b>CHARGE</b> the charge of the chemical system
     * </li>
     * <li>
     * (optional) <b>SPIN_MULTIPLICITY</b>  the spin multiplicity of the
     * chemical system
     * </li>
     * <li>
     * (optional) <b>QM_SOFTWARE</b> defined the software that will perform
     * quantum molecular mechanics
     * calculations (with version ID). Default is GAUSSIAN-09.
     * </li>
     * <li>
     * (optional) <b>MM_SOFTWARE</b> defined the software that will perform
     * classical molecular mechanics
     * calculations (with version ID. Default is TINKER-6.3.3.
     * </li>
     * <li>
     * (optional) <b>QMATOMS_ID</b> define atoms belonging to QM region by
     * listing atom ID 
     * (1-based atom number in the input molecular representation)
     * </li>
     * <li>
     * (optional) <b>QMATOMS_SMARTS</b> define atoms belonging to QM region
     * using MARTS queries.
     * A multi line block (see
     * {@link autocompchem.datacollections.Parameter}) can be used defining one SMARTS
     * query per line.
     * </li>
     * <li>
     * (optional) <b>CAPATOMS_SMARTS</b> define cap atoms using SMARTS queries
     * and cap atom specification.
     * A multi line block (see
     * {@link autocompchem.datacollections.Parameter}) can be used defining each line
     * one SMARTS and the cap atom properties (name, atom type, scale factor) 
     * used to define the cap atom that replaces the atom matched by the SMARTS 
     * query.
     * The syntax for each lice is as follows: [SMARTSquery] [cap atom specs]
     * </li>
     * <li>
     * (optional) <b>FREEZEATM</b> define frozen atoms.
     * SMARTS queries are used to identify atoms to freeze.
     * A multi line block (see
     * {@link autocompchem.datacollections.Parameter}) can be used defining one SMARTS
     * query per line.
     * </li>
     * <li>
     * (optional) <b>ATOMTYPESFILE</b> provides a file from wich atom types 
     * are read
     * and specifies the format for reading such file.
     * The syntax for this option is as follows: [filename] [format]
     * </li>
     * </ul>
     *          
     * @param params object 
     * {@link autocompchem.datacollections.ParameterStorage} containing all the
     * parameters needed
     */
/*
    public QMMMInputWriter(ParameterStorage params)
    {
    
    */
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
                System.out.println(" Adding parameters to QMMMInputWriter");
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
        else
        {
            Terminator.withMsgAndStatus("ERROR! This version of "
                     + "QMMMInputWriter can handle only SDF or XYZ input",-1);
        }
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Use QMMMJob; we do not accept headers in the form of plain text
        if (params.contains("JOBDETAILS"))
        {
            //Use QMMMJob
            String jdFile = 
                        params.getParameter("JOBDETAILS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Compound QMMM job: details from "  
                         + jdFile);
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.qmmmJob = new QMMMJob(jdFile);
        } 
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                                  + " No 'JOBDETAILS' found in parameters.",-1);
        }

        //Name of output
        if (params.contains("OUTFILE"))
        {
            outFile = params.getParameter("OUTFILE").getValue().toString();
            crdFile = FileUtils.getRootOfFileName(outFile) 
                                               + QMMMConstants.QMMMCRDEXTENSION;
        } else {
            String inputRoot = FileUtils.getRootOfFileName(inFile);
            outFile = inputRoot + QMMMConstants.QMMMINPEXTENSION;
            crdFile = inputRoot + QMMMConstants.QMMMCRDEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No 'OUTFILE' option found. "
                                     + "Output name set to '" + outFile + "'.");
            }
        }

        //TITLE
        if (params.contains("TITLE"))
        {
            title = params.getParameter("TITLE").getValue().toString();
            this.qmmmJob.setTitle(title);
            if (verbosity > 0)
            {
                System.out.println(" Found 'TITLE' option. Overwriting title");
            }
        }

        //CHARGE
        if (params.contains("CHARGE"))
        {
            charge = Integer.parseInt(
                        params.getParameter("CHARGE").getValue().toString());
            this.qmmmJob.setCharge(charge);
            if (verbosity > 0)
            {
                System.out.println(" Found 'CHARGE' option. Overwriting "
                                             + "charge. New value = " + charge);
            }
        } 

        //SPIN_MULTIPLICITY
        if (params.contains("SPIN_MULTIPLICITY"))
        {
            spinMult = Integer.parseInt(
                params.getParameter("SPIN_MULTIPLICITY").getValue().toString());
            this.qmmmJob.setSpinMultiplicity(spinMult);
            if (verbosity > 0)
            {
                    System.out.println(" Found 'SPIN_MULTIPLICITY' option. "
                        + "Overwriting spin multiplicity. New "
                        + "value = " + spinMult);
            }
        }

        //Softwares
        if (params.contains("QM_SOFTWARE"))
        {
            qmsoftware = 
                       params.getParameter("QM_SOFTWARE").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Setting 'QM_SOFTWARE' to " + qmsoftware);
            }
        }
        if (params.contains("MM_SOFTWARE"))
        {
            mmsoftware = 
                       params.getParameter("MM_SOFTWARE").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Setting 'MM_SOFTWARE' to " + mmsoftware);
            }
        }

        //Atom indexes for QM atoms
        if (params.contains("QMATOMS_ID"))
        {
            String s = params.getParameter("QMATOMS_ID").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing QM atoms IDs");
            }
            String[] ids = s.split("\\s+");
            for (int i=0; i<ids.length; i++)
            {
                String strId = ids[i].trim();
                if (strId.equals(""))
                {
                    continue;
                }
                this.qmAtmsIds.add(Integer.parseInt(strId));
            }
        }

        //SMARTS rules to define QM atoms
        if (params.contains("QMATOMS_SMARTS"))
        {
            String all = params.getParameter(
                                        "QMATOMS_SMARTS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing SMARTS defining QM atoms");
            }
            String[] lines = all.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                String singleSmarts = lines[i].trim();
                if (singleSmarts.equals(""))
                {
                    continue;
                }
                String singleSmartsName = "smarts"+i;
                this.qmAtmSmarts.put(singleSmartsName,singleSmarts);
            }
        }

        //SMARTS rules to freeze atoms in Cartesian coordinates
        if (params.contains("FREEZEATM"))
        {
            String all = params.getParameter("FREEZEATM").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing SMARTS to freeze ATOMS");
            }
            String[] lines = all.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                String singleSmarts = lines[i].trim();
                if (singleSmarts.equals(""))
                {
                    continue;
                }
                String singleSmartsName = "smarts"+i;
                this.frozenSmarts.put(singleSmartsName,singleSmarts);
            }
        }

        //SMARTS rules to define cap atoms
        if (params.contains("CAPATOMS_SMARTS"))
        {
            String all = params.getParameter(
                                       "CAPATOMS_SMARTS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing SMARTS defining CAP atoms");
            }
            String[] lines = all.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                String smartsAndCaps = lines[i].trim();
                if (smartsAndCaps.equals(""))
                {
                    continue;
                }
                String[] parts = smartsAndCaps.split("\\s+",2);
                if (parts.length < 2)
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to understand "
                                + "cap atom definition '" + smartsAndCaps +"'. "
                                + "Expected format [SMARTS] [CAP atom "
                                + "definition].",-1);
                }
                String singleSmartsName = "smarts"+i;
                String singleSmarts = parts[0];
                String capAtmDef = parts[1];
                this.capAtmSmarts.put(singleSmartsName,singleSmarts);
                this.capAtmSpecs.put(singleSmartsName,capAtmDef);
            }
        }

        //Atom types from external tool
        if (params.contains("ATOMTYPESFILE"))
        {
            String all = 
                     params.getParameter("ATOMTYPESFILE").getValue().toString();
            String[] parts = all.split("\\s+");
            if (parts.length != 2)
            {
                Terminator.withMsgAndStatus("ERROR! Cannot understand line " 
                        + all + "'. Correct sintax is [filename] [format]", -1);
            }
            atmTypFile = parts[0];
            atmTypFormat = parts[1];
            if (verbosity > 0)
            {
                    System.out.println(" Atom types will be taken from file '"
                                                           + atmTypFile + "'.");
            }
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
          case PREPAREINPUTQMMM:
        	  writeInputForEachMol();
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
     * Write QMMM input file according to the given parameters.
     * In case of multi entry structure files (i.e., SDF),
     * a single input will be generated per each molecule using the name
     * of the molecule (title in SDF properties) as root for the output.
     * QMMM job details derive from the current status of the
     * <code>QMMMJob</code> field in this QMMMInputWriter.
     */

    public void writeInputForEachMol()
    {
        //Get molecule/s
        int n = 0;
        try 
        {
            ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
            switch (inFormat) 
            {
                case "SDF":
                    mols = IOtools.readSDF(inFile);
                    break;

                case "XYZ":
                    mols = IOtools.readXYZ(inFile);
                    break;

                default:
                    Terminator.withMsgAndStatus("ERROR! QMMMInputWriter"
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
                // outFile for the output.
                if (mols.size() > 1)
                {
                    outFile = molName + QMMMConstants.QMMMINPEXTENSION;
                    crdFile = molName + QMMMConstants.QMMMCRDEXTENSION;
                }

                FileUtils.mustNotExist(outFile);
                FileUtils.mustNotExist(crdFile);

                //Define Cartesian coords according to the molecular model.
                if (qmmmJob.getModel().equals("QM"))
                {
                    QMMMList geomLst = makeCartesianCoordsList(mol);
                    ArrayList<String> secPath = new ArrayList<String>(
                                      Arrays.asList(QMMMConstants.MULTIGENSEC));
                    qmmmJob.setList(secPath,geomLst);
                }
                else if (qmmmJob.getModel().equals("MM")
                         || qmmmJob.getModel().equals("QM/MM"))
                {
                    ArrayList<String> secPath = new ArrayList<String>(
                                      Arrays.asList(QMMMConstants.MULTIGENSEC));
                    QMMMKeyword key = makeCartesianCoordsFile(mol,mmsoftware,
                                                                    forcefield);
                    qmmmJob.setKeyword(secPath,key);
                }
                else
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to understand "
                        + " calculation model '" + qmmmJob.getModel() 
                        + "'. Known values are 'QM', 'MM', and 'QM/MM'.",-1);
                }

                //Set the list of QM atoms
                ArrayList<Integer> locQmAtmsIds = new ArrayList<Integer>();
                locQmAtmsIds.addAll(qmAtmsIds);
                if (qmAtmSmarts.size()>0 && qmmmJob.getModel().equals("QM/MM"))
                {
                    makeQMAtmsLstFromSMARTS(mol,locQmAtmsIds);
                }
                if (locQmAtmsIds.size()>0 && qmmmJob.getModel().equals("QM/MM"))
                {
                    ArrayList<String> dPath = new ArrayList<String>(
                                          Arrays.asList(QMMMConstants.QMMMSEC));
                    Collections.sort(locQmAtmsIds);
                    ArrayList<String> qmAtmsAsStr = new ArrayList<String>();
                    for (Integer id : locQmAtmsIds)
                    {
                        qmAtmsAsStr.add(Integer.toString(id));
                    }

                    QMMMList lst = new QMMMList("QMATOM",qmAtmsAsStr);
                    qmmmJob.setList(dPath,lst);
                }

                //Set cap atoms
                if (capAtmSmarts.size()>0 && qmmmJob.getModel().equals("QM/MM"))
                {
                    ArrayList<String> dPath = new ArrayList<String>(
                                          Arrays.asList(QMMMConstants.QMMMSEC));
                    QMMMList lst = makeCapAtomList(mol);
                    qmmmJob.setList(dPath,lst);
                }

                //Set total number of atoms
                qmmmJob.setNatoms(mol.getAtomCount());

                //Set charge and spin multiplicity
                if (inFormat=="SDF")
                {
                    chargeOrSpinFromIAC(mol);
                }
                checkChargeSpinNotAtDefault();
                qmmmJob.setCharge(charge);
                qmmmJob.setSpinMultiplicity(spinMult);

                //Set the list of active (i.e., not frozen) atoms
                if (frozenSmarts.size() > 0)
                {
                    ArrayList<String> dPath = new ArrayList<String>(
                                      Arrays.asList(QMMMConstants.MULTIOPTSEC));
                    QMMMList lst = makeFrozenAtomsList(mol);
                    qmmmJob.setList(dPath,lst);
                }

                //Write inp
                writeInput();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                + "making QMMM input file for molecule " + n + " from file " 
                + inFile + "\n" + t, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Create the list of cap atoms
     * @param mol the molecular object to work with
     * @return the list of cap atoms
     */

    private QMMMList makeCapAtomList(IAtomContainer mol)
    {
        ArrayList<String> capAtmsLst = new ArrayList<String>();
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,capAtmSmarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " + cause,-1);
        }
        ArrayList<Integer> alreadyMatched = new ArrayList<Integer>();
        for (String key : capAtmSmarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
                 System.out.println("WARNING! No match for SMARTS query '"
                                 + capAtmSmarts.get(key) + "' in molecule "
                                 + MolecularUtils.getNameOrID(mol));
                continue;
            }
            List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(key);
            for (List<Integer> atmIDsLst : allMatches)
            {
                if (atmIDsLst.size() > 1)
                {
                    Terminator.withMsgAndStatus("ERROR! Cap-defining SMARTS '"
                        + capAtmSmarts.get(key) +"' matched a group of "
                        + atmIDsLst.size() + " atoms. Only single atoms should "                        + "be matches by cap-defining SMARTS queries.",-1);
                }
                if (alreadyMatched.contains(atmIDsLst.get(0)))
                {
                    Terminator.withMsgAndStatus("ERROR! Cap-defining SMARTS '"
                        + capAtmSmarts.get(key) +"' matched an atom that has "
                        + "already been used to define a cap atom.",-1);
                }
                String capLine = atmIDsLst.get(0) + 1 // 1-based atom ID
                                 + " " +capAtmSpecs.get(key); 
                alreadyMatched.add(atmIDsLst.get(0));
                capAtmsLst.add(capLine);
                if (verbosity > 1)
                {
                    System.out.println("Defining CAP atom: " + capLine);
                }
            }
        }
        Collections.sort(capAtmsLst);
        
        QMMMList lst = new QMMMList("CAPATOM",capAtmsLst);
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Add atoms ID to the list of QM atoms according to the SMARTS queries
     * given by the user.
     * @param mol the molecular object to work with
     * @param locQmAtmsIds current list of QM atom by IDs 
     */

    private void makeQMAtmsLstFromSMARTS(IAtomContainer mol, 
                                                ArrayList<Integer> locQmAtmsIds)
    {
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,qmAtmSmarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " +cause,-1);
        }
        for (String key : qmAtmSmarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
                 System.out.println("WARNING! No match for SMARTS query "
                                 + qmAtmSmarts.get(key) + " in molecule "
                                 + MolecularUtils.getNameOrID(mol));
                continue;
            }
            List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(key);
            String reportQMAtm = "";
            for (List<Integer> atmIDsLst : allMatches)
            {
                if (atmIDsLst.size() > 1)
                {
                    if (verbosity > 0)
                    {
                        System.out.println("WARNING! QM atom-defining SMARTS '"
                        + qmAtmSmarts.get(key) +"' matched a group of "
                        + atmIDsLst.size() + " atoms that will all be qmAtm.");
                    }
                }
                for (Integer qmAtmId : atmIDsLst)
                {
                    if (!locQmAtmsIds.contains(qmAtmId + 1))
                    {
                        //NOTE: we return 1-based IDs
                        locQmAtmsIds.add(qmAtmId + 1);
                    }
                    reportQMAtm = reportQMAtm + qmAtmId + " ";
                }
            }
            if (verbosity > 1 && !reportQMAtm.equals(""))
            {
                System.out.println("QM atom-defining SMARTS '"
                                   + qmAtmSmarts.get(key)
                                   + "' matched by atom IDs " + reportQMAtm);
            }
        }
    }

//------------------------------------------------------------------------------
   
    /**
     * Returns the list of frozed atoms as 1-based QMMMList of atom IDs
     * @param mol the molecular object to work with
     */

    private QMMMList makeFrozenAtomsList(IAtomContainer mol)
    {
        ArrayList<String> lstAsString = new ArrayList<String>();
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,frozenSmarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " +cause,-1);
        }
        for (String key : frozenSmarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
                 System.out.println("WARNING! No match for SMARTS query "
                                 + frozenSmarts.get(key) + " in molecule "
                                 + MolecularUtils.getNameOrID(mol));
                continue;
            }
            List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(key);
            String reportFrozen = "";
            for (List<Integer> atmIDsLst : allMatches)
            {
                if (atmIDsLst.size() > 1)
                {
                    if (verbosity > 0)
                    {
                        System.out.println("WARNING! Atom-freezing SMARTS '"
                        + frozenSmarts.get(key) +"' matched a group of "
                        + atmIDsLst.size() + " atoms that will all be frozen.");
                    }
                }
                for (Integer frozenAtmId : atmIDsLst)
                {
                    //NOT: we return 1-based IDs
                    String idAsString = Integer.toString(frozenAtmId + 1);
                    if (!lstAsString.contains(idAsString))
                    {
                        lstAsString.add(idAsString);
                        reportFrozen = reportFrozen + frozenAtmId + " ";
                    }
                }
            }
            if (verbosity > 1 && !reportFrozen.equals(""))
            {
                System.out.println("Atom-freezing SMARTS '" 
                                   + frozenSmarts.get(key) 
                                   + "' matched by atom IDs " + reportFrozen);
            }
        }

        QMMMList lst = new QMMMList("FROZEATM",lstAsString);
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Return true if the charge or the spin are overwritten according to the
     * IAtomContainer properties "CHARGE" and "SPIN_MULTIPLICITY"
     * @param mol the molecule from which we get charge and spin
     * @return <code>true</code> if the charge or the spin multiplicity are
     * changes due to <code>IAtomContainer</code> properties
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

        if (verbosity > 1)
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
     * Creates a list with the Cartesian coordinates of the given 
     * molecule.
     * @param mol the CDK representation of the system
     * @return the list of coordinated
     */

    private QMMMList makeCartesianCoordsList(IAtomContainer mol)
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (IAtom atm : mol.atoms())
        {
            StringBuilder sb = new StringBuilder(atm.getSymbol());
            sb.append(String.format(" %5.8f",atm.getPoint3d().x));
            sb.append(String.format(" %5.8f",atm.getPoint3d().y));
            sb.append(String.format(" %5.8f",atm.getPoint3d().z));
            lines.add(sb.toString());
        }
        QMMMList geomLst = new QMMMList(QMMMConstants.GEOMLIST,lines);
        return geomLst;
    }

//------------------------------------------------------------------------------

    /**
     * Creates a list with the Cartesian coordinates of the given
     * molecule and writes the crd file.
     * @param mol the CDK representation of the system
     * @param software the format of the coordinate file, as the name of the
     * software meant to read such file (i.e., tinker, namd).
     * @return the keyword for the dat file
     */

    private QMMMKeyword makeCartesianCoordsFile(IAtomContainer mol, 
                                             String software, String forcefield)
    {
        // Check
        FileUtils.mustNotExist(crdFile);

        // Get connectivity table
        ConnectivityTable ct = new ConnectivityTable(mol);

        // Get atom types
        IAtomContainer ffmol = getIACWithAtomTypes();
        for (int i=0; i<ffmol.getAtomCount(); i++)
        {
            IAtom ffatm = ffmol.getAtom(i);
            IAtom atm = mol.getAtom(i);
            atm.setProperty(QMMMConstants.NUMERICALATMTYPFIELD,
                         ffatm.getProperty(QMMMConstants.NUMERICALATMTYPFIELD));
            atm.setAtomTypeName(ffatm.getAtomTypeName());
        }

        // Make coordinates file
        ArrayList<String> lines = new ArrayList<String>();
        if (software.toUpperCase().contains("TINKER"))
        {
            // WARNING 
            // WARNING the format only resembles the one used by Tinker!
            // WARNING

            lines.add(mol.getAtomCount()+" "+MolecularUtils.getNameOrID(mol));

            int atmId = 0; 
            for (IAtom atm : mol.atoms())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%1$4s",atmId + 1)); //1-based ID
                sb.append(String.format(" %1$2s",atm.getSymbol()));
                sb.append(String.format(" %1$3s",atm.getAtomTypeName()));
                sb.append(String.format(" %5.8f",atm.getPoint3d().x));
                sb.append(String.format(" %5.8f",atm.getPoint3d().y));
                sb.append(String.format(" %5.8f",atm.getPoint3d().z));
                sb.append(String.format(" %1$4s",atm.getProperty(
                        QMMMConstants.NUMERICALATMTYPFIELD).toString()));
                sb.append(String.format(" %1$4s",ct.getNbrsIdAsString(atmId,
                                                                   false," ")));
                lines.add(sb.toString());
                atmId++;
            }

            lines.add("END");
        }
/*
//TODO
        else if (software.toUpperCase().contains("NMDO"))
        {
        }
*/
        else
        {
            Terminator.withMsgAndStatus("ERROR! Unknown software for MM: '" 
                                                          + software + "'.",-1);
        }

        // Write file
        IOtools.writeTXTAppend(crdFile,lines,false);

        // Make keyword
        QMMMKeyword key = new QMMMKeyword(QMMMConstants.COORDFORMATKEY,true,
                                new ArrayList<String>(Arrays.asList(software)));

        return key;
    }

//------------------------------------------------------------------------------

    /**
     * Read external source of atom types. For now atom types have to be defined
     * in an external file. 
     * TODO: need to include possibiity
     * to get atom types on the fly with CDK atom typer.
     * @return a cdk representation with atom types in the atom properties
     */

    private IAtomContainer getIACWithAtomTypes()
    {
        if (atmTypFile.equals("") || atmTypFormat.equals(""))
        {
            Terminator.withMsgAndStatus("ERROR! Attempt to recover atom types "
                            + "from file requires the ATOMTYPESFILE option.",-1);
        }
        IAtomContainer iac = new AtomContainer();
        switch (atmTypFormat.toUpperCase())
        {
            case "TINKER":
                iac = TinkerXYZReader.readTinkerXYZFIle(atmTypFile);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + atmTypFormat 
                      + "' not supported for entering atom typed molecule.",-1);
        }

        return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Write QMMM input file  
     * using only the setting provided in constructing this QMMMInputWriter.
     */

    public void writeInput()
    {
        IOtools.writeTXTAppend(outFile,qmmmJob.toLinesInput(),false);
    }

//------------------------------------------------------------------------------

}
