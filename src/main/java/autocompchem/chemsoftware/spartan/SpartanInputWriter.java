package autocompchem.chemsoftware.spartan;

/*   
 *   Copyright (C) 2016  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.conformation.ConformationalCoordinate;
import autocompchem.molecule.conformation.ConformationalMovesDefinition;
import autocompchem.molecule.conformation.ConformationalSpace;
import autocompchem.molecule.conformation.ConformationalSpaceBuilder;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for Spartan. 
 * The generated input consists in a folder
 * tree where a root folder contains a Spartan-only link
 * and a sub folder, which contains the real input file for Spartan14.
 * 
 * @author Marco Foscato
 */

public class SpartanInputWriter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTSPARTAN)));

    /**
     * Input filename (a structure file)
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
     * Keywords for Spartsn
     */
    private String keywords = ""; 

    /**
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);

    /**
     * Label used to identify single-atom smarts in the smarts reference name
     */
    private static final String SUBRULELAB = "_p";

    /**
     * Root of the smarts reference names for rotatable bonds
     */
    private static final String ROTOROOT = "sr ";

    /**
     * Root of the smarts reference names for constraints
     */
    private static final String CTRSROOT = "sc ";

    /**
     * Root of the smarts reference names for dynamic constraints
     */
    private static final String DYNCROOT = "dy ";

    /**
     * Root of the smarts reference names for constraints
     */
    private static final String ATMKEYROOT = "ak ";

    /**
     * Root of the smarts reference names for frozen atoms
     */
    private static final String FREEZEROOT = "fr ";

    /**
     * Default value for integers
     */
    private final int DEFNUM = -999999;

    /**
     * charge of the whole system
     */
    private int charge = DEFNUM;

    /**
     * Spin multiplicity of the whole system
     */
    private int spinMult = DEFNUM;

    /**
     * Storage of SMARTS queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Storage of details assiciated with SMARTS queries
     */
    private Map<String,ArrayList<String>> smartsOpts = 
                                        new HashMap<String,ArrayList<String>>();

    /**
     * Flag defining need to record rotatable bonds
     */
    private boolean defRotoBnds = false;

    /**
     * Flag defining need to record constraints
     */
    private boolean defConstraints = false;

    /**
     * Flag defining need to record dynamic constraints
     */
    private boolean defDynConstraints = false;

    /**
     * Flag defining need to record atom-specific keywords
     */
    private boolean defAtmSpecKeys = false;

    /**
     * Flag defining need to record frozen atoms
     */
    private boolean defFrozen = false;

    /** 
     * Verbosity level
     */
    private int verbosity = 1;

//------------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public SpartanInputWriter()
    {
        super("inputdefinition/todo.json");
    }

//-----------------------------------------------------------------------------

    //TODO move to class doc
    
    /**
     * Construct a new SpartanInputWriter using the 
     * {@link autocompchem.datacollections.Parameter}s 
     * taken from a
     * {@link ParameterStorage}.
     * <br>
     * <ul>
     * <li>
     * <b>INFILE</b>: name of the structure file (i.e. path/name.sdf).
     * </li>
     * <li>
     * <b>KEYWORDS</b>: the list of keywords for Spartan
     * </li>
     * <li>
     * (optional) <b>CHARGE</b> the charge of the chemical system
     * </li>
     * <li>
     * (optional) <b>SPIN_MULTIPLICITY</b> the spin multiplicity of the
     * chemical system
     * </li>
     * <li>
     * (optional) <b>VERBOSITY</b> verbosity level.
     * </li>
     * <li>
     * (optional) <b>OUTFILE</b> name of the output file (input for Spartan).
     * If this option is omitted the default name of the output is build from 
     * the root of the structure file name given in the INFILE parameter.
     * </li>
     * <li>
     * (optional) <b>ROTATABLEBONDS</b> the definition of the rotatable bonds.
     * can be a pathname or a single/multi line block
     * (see {@link autocompchem.datacollections.Parameter}) in which each line
     * contains a bond-matching SMARTS query and additional options/keywords
     * (for instance, the fold number to use for the bonds matched by the SMARTS
     * query). For Example:
     * <pre>
     * $STARTROTATABLEBONDS:
     * [C]~[#6] 3
     * [#7]!@~[#6] 3
     * [$([#6]1~[#7]~[*]~[*]~[*]~[*]1)] 2
     * $END
     * </pre>
     * </li>
     * <li>
     * (optional) <b>CONSTRAINTS</b> the definition of the constraints.
     * Single-atom SMARTS (i.e., must be enclosed in square brackets, eg. [C])
     * are used to define a single atom type. Combinations of 2, 3, or 4
     * whitespace-separated single-atom SMARTS are used to refer to stretching,
     * bending, or torsion types of ICs.
     * All n-tuple (n=2,3,4) of matching atoms that are also properly connected
     * are considered.
     * After the 2/3/4 single-atom SMARTS
     * the user can add one numerical value and/or text-style options
     * that will apply to all ICs matched by the combination of
     * single-atom SMARTS.
     * A multi line block is used to
     * define one set of SMARTS queries (plus additional numerical or text)
     * per each line.
     * <pre>
     * $STARTCONSTRAINTS:
     * [C] [$(C(~[N])~C)] 1.512
     * [*] [$(C(~[N])~C)] [*] 120.000 optionA option B
     * [*] [$(C(~[N])~C)] [C] [*]
     * $END
     * </pre>
     * </li>
     * <li>
     * (optional) <b>DYNAMICCONSTRAINT</b> the definition of dynamic 
     * constraints. This option uses the very same syntax of the CONSTAINTS 
     * option: first the n-tupla defining the type of dynamic constraint
     * and the atom specificity, then the numerical values defining constraint.
     * The numerical values should include: i) the initial (float) value of the 
     * dynamic constraint, ii) the value 0.00 (which, is not used now), iii)
     * the final (float) value of the dynamic constraint, and iv) the (integer)
     * number of steps to be taken from the initial to the final values.
     * </li>
     * <li>(optional) <b>ATOMSPECIFICKEYWORDS</b> definition of atom specific
     * keywords. SMARTS can be used to identify specific atoms and add the
     * atom specific keyword to the input file. One SMARTS-based rule per line.
     * Multi line block can be used.
     * To specify the keyword use
     * the following syntax:
     * <pre>&lt;SMARTS&gt; &lt;part1_K&gt; &lt;part2_K&gt; &lt;part3_K&gt;</pre>
     * where the <code>partN_K</code> are portions of the same keyword,
     * namely: <code>part1_K</code> goes before the atom label and usually
     * contains the Spartan keyword, and <code>partN_K</code> with N&gt;1
     * are appended after the atom label. For instance, this rule:
     * <pre>[#17] FFHINT= ~~10</pre>
     * generated the keyword <code>FFHINT=Cl1~~10</code> where "Cl1" is the atom
     * label identified by the SMARTS <code>[#17]</code>.
     * </li>
     * <li>(optional) <b>FREEZEATOMS</b> the definition of atoms to freeze.
     * SMARTS are used to identify atoms or groups to be frozen. One SMARTS 
     * query per line. Multi line block can be used.
     * </li>
     * </ul>
     * <p>
     *
     *  @param params object {@link ParameterStorage} containing all the
     * parameters needed
     */

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
                System.out.println(" Adding parameters to SpartanInputWriter");
        }

        //Get and check the input file
        this.inFile = params.getParameter("INFILE").getValue().toString();
        if (inFile.endsWith(".sdf"))
        {
            this.inFormat = "SDF";
        }
        else if (inFile.endsWith(".xyz"))
        {
            this.inFormat = "XYZ";
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! This version of "
                    + "SpartanInputWriter can handle only SDF or XYZ input",-1);
        }
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Spartan Keywords
        this.keywords = params.getParameter("KEYWORDS").getValue().toString();

        //CHARGE
        if (params.contains("CHARGE"))
        {
            charge = Integer.parseInt(
                        params.getParameter("CHARGE").getValue().toString());
            if (verbosity > 0)
            {
                System.out.println(" Found 'CHARGE' option. Setting charge to "
                                                                      + charge);
            }
        }

        //SPIN_MULTIPLICITY
        if (params.contains("SPIN_MULTIPLICITY"))
        {
            spinMult = Integer.parseInt(
                params.getParameter("SPIN_MULTIPLICITY").getValue().toString());
            if (verbosity > 0)
            {
                    System.out.println(" Found 'SPIN_MULTIPLICITY' option. "
                                              + "Setting value to " + spinMult);
            }
        }

        //Name of output
        if (params.contains("OUTFILE"))
        {
            this.outFile = params.getParameter("OUTFILE").getValue().toString();
            FileUtils.mustNotExist(outFile);
        } else {
            String inputRoot = FileUtils.getRootOfFileName(this.inFile);
            this.outFile = inputRoot + SpartanConstants.SPRTINPEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No 'OUTFILE' option found. "
                                + "Output name set to '" + this.outFile + "'.");
            }
            FileUtils.mustNotExist(this.outFile);
        }

        //SMARTS rules to identify rotatable bonds
        if (params.contains("ROTATABLEBONDS"))
        {
            this.defRotoBnds = true;
            String all = 
                    params.getParameter("ROTATABLEBONDS").getValue().toString();
            getNamedSmartsWithOpts(all,ROTOROOT,false);
        }

        //SMARTS rules to setup constraints
        if (params.contains("CONSTRAINTS"))
        {
            this.defConstraints = true;
            String all =
                    params.getParameter("CONSTRAINTS").getValue().toString();
            getNamedSmartsWithOpts(all,CTRSROOT,true);
        }

        //SMARTS rules to setup dynamic constraints
        if (params.contains("DYNAMICCONSTRAINT"))
        {
            this.defDynConstraints = true;
            String all =
                 params.getParameter("DYNAMICCONSTRAINT").getValue().toString();
            getNamedSmartsWithOpts(all,DYNCROOT,true);
        }

        //SMARTS rules to assign atom-specific keywords
        if (params.contains("ATOMSPECIFICKEYWORDS"))
        {
            this.defAtmSpecKeys = true;
            String all =
              params.getParameter("ATOMSPECIFICKEYWORDS").getValue().toString();
            getNamedSmartsWithOpts(all,ATMKEYROOT,false);
        }

        //SMARTS rules freezing atoms
        if (params.contains("FREEZEATOMS"))
        {
            this.defFrozen = true;
            String all =
                       params.getParameter("FREEZEATOMS").getValue().toString();
            getNamedSmartsWithOpts(all,FREEZEROOT,false);
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
          case PREPAREINPUTSPARTAN:
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
     * Read SMARTS queries and options associated with SMARTS queries.
     * This methos collects all SMARTS strings and related options found in a
     * given string. The string is
     * assumend to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 1 to 4 space-separated SMARTS.
     * All strings that do not begin or end with '[' and ']' are considered
     * to be options (i.e., not SMARTS).
     * All SMARTS and options are stored in the fields of this class.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @param masterRoot the root of the SMARTS query name. Use this to 
     * discriminate between SMARTS that are used for different purposes
     * @param smarts the map of previously red SMARTS queries for which this
     * method is collecting the options
     */
 
    private void getNamedSmartsWithOpts(String allLines, String masterRoot,
                                                               boolean noSingle)
    {
        if (verbosity > 1)
        {
            System.out.println(" Importing SMARTS with nameroot '" + masterRoot
                                                                        + "'.");
        }
        String[] lines = allLines.split("\\r?\\n");
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            int masterId = iNameSmarts.getAndIncrement();
            String masterName = masterRoot + masterId;

            String[] parts = lines[i].split("\\s+");
            int subId = -1;
            for (int j=0; j<parts.length; j++)
            {
                String word = parts[j].trim();

                // Ignore empty
                if (word.equals(""))
                {
                    continue;
                }

                // Discriminate SMARTS and OPTS
                if (SMARTS.isSingleAtomSMARTS(word))
                {
                    subId++;
                    String childName = masterName + SUBRULELAB + subId;
                    this.smarts.put(childName,word);
                }
                else
                {
                    ArrayList<String> opts = new ArrayList<String>();
                    for (int jj=j; jj<parts.length; jj++)
                    {
                        opts.add(parts[jj].trim());
                    }
                    this.smartsOpts.put(masterName,opts);
                    break;
                }

                if (subId > 3)
                {
                    Terminator.withMsgAndStatus("ERROR! More than 4 atomic "
                               + "SMARTS for IC-defining SMARTS rule "
                               + masterId + " (last SMARTS:" + word + "). "
                               + "These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check the input.",-1);
                }
            }
            if (subId < 1 && noSingle)
            {
                Terminator.withMsgAndStatus("ERROR! Less than 2 atomic "
                               + "SMARTS for IC-defining SMARTS rule "
                               + masterId 
                               + ". These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check input.",-1);
            }
            if (!this.smartsOpts.containsKey(masterName))
            {
                this.smartsOpts.put(masterName,new ArrayList<String>());
            }
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
     * Write Spartan input file according to the given parameters.
     * In case of multi entry structue files (i.e., SDF),
     * a single input will be generated per each molecule using the name
     * of the molecule (title in SDF properties) as root for the output.
     * Spartan job details derive from the current status of the
     * <code>SpartanJob</code> field in this SpartanInputWriter.
     */

    public void writeInputForEachMol()
    {
        //Get molecule/s
        int n = 0;
        try {
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
                    Terminator.withMsgAndStatus("ERROR! SpartanInputWriter"
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
                    outFile = molName + SpartanConstants.SPRTINPEXTENSION;
                }
                FileUtils.mustNotExist(outFile);

                //Write input for Spartan
                writeSpartanInputFile(mol);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                + "making Spartan input for molecule " + n + " from file " 
                + inFile + "\n" + t, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Prepares Spartan14's input for a single molecule according to the
     * parameters given to the constructor.
     * The generated input consists in a folder
     * tree where a root folder contains a Spartan-only link
     * and a subfolder, which contains the real input file for Spartan14.
     * @param mol the molecular representation
     */

    public void writeSpartanInputFile(IAtomContainer mol)
    {
        String molName = MolecularUtils.getNameOrID(mol); 
        String sep = System.getProperty("file.separator");

        if (verbosity > 0)
        {
            System.out.println(" Writing Spartan input file of molecule '"
                + molName + "' in folder '" + outFile + "'.");
        }

        //Create folder tree
        String molDirName = outFile + sep + molName;
        boolean allDone = (new File(molDirName)).mkdirs();
        if (!allDone)
        {
            Terminator.withMsgAndStatus("ERROR! Unable to create folder "
                                                 + molDirName + ".",-1);
        }

        //Create Spartan's flag files
        IOtools.writeTXTAppend(outFile+sep+SpartanConstants.ROOTFLGFILENAME,
                                        SpartanConstants.ROOTFLGFILEHEAD,false);
        IOtools. writeTXTAppend(molDirName+sep+SpartanConstants.MOLFLGFILENAME,
                                         SpartanConstants.MOLFLGFILEHEAD,false);

        //Create cell file
        IOtools.writeTXTAppend(molDirName+sep+SpartanConstants.CELLFILENAME,
                                                   getCellDirective(mol),false);

        //Create input
        IOtools.writeTXTAppend(molDirName+sep+SpartanConstants.INPUTFILENAME,
                                                      getInputLines(mol),false);

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
        else
        {
            
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

    @SuppressWarnings("unused")
        private void checkChargeSpinNotAtDefault()
    {
        if (charge == DEFNUM)
        {
            Terminator.withMsgAndStatus("ERROR! Property "
                + "<CHARGE> cannot be defined.",-1);
        }

        if (spinMult == DEFNUM)
        {
            Terminator.withMsgAndStatus("ERROR! Property "
                + "<SPIN_MULTIPLICITY> cannot be defined.", -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Collects all molecular properties and reports then in the form of a 
     * list of strings formatted for Spartan's CELL file.
     * @param mol the molecular representation containing the properties to
     * be formatted.
     */

    private ArrayList<String> getCellDirective(IAtomContainer mol)
    {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(SpartanConstants.CELLOPN);
        for (Map.Entry<Object, Object> p : mol.getProperties().entrySet())
        {
            lines.add(SpartanConstants.INDENT + p.getKey().toString() + "=" 
                                                     + p.getValue().toString());
        }
        lines.add(SpartanConstants.CELLEND);
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Prepare all the text for a Spartan input file.
     * @param mol the molecular representation
     */

    private ArrayList<String> getInputLines(IAtomContainer mol)
    {
        ArrayList<String> lines = new ArrayList<String>();

        //Match all SMARTS queries
        ManySMARTSQuery msq = null;
        if (defRotoBnds || defConstraints || defDynConstraints || defAtmSpecKeys
            || defFrozen)
        {
            msq = new ManySMARTSQuery(mol,smarts,verbosity);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                Terminator.withMsgAndStatus("ERROR! Cannot identify specific "
                          + "atoms with the given SMARTS queries. " + cause,-1);
            }
        }

        //Define atom label
        specifyAtomLabels(mol);

        //Keywords
        String finalKeys = keywords;
        if (defAtmSpecKeys)
        {
            String atmSpecKeys = getAtomSpecificKeywords(mol,msq);
            finalKeys = finalKeys + " " + atmSpecKeys;
        }
        lines.add(finalKeys);

        //Molecule name
        lines.add(MolecularUtils.getNameOrID(mol));
        
        //Set charge and spin multiplicity
        int oldChrg = this.charge;
        int oldSm = this.spinMult;
        chargeOrSpinFromIAC(mol);
        lines.add(this.charge + " " + this.spinMult);
        this.charge = oldChrg;
        this.spinMult = oldSm;

        //Carthesian coords (Angstrom)
        for (IAtom atm : mol.atoms())
        {
            double x = AtomUtils.getCoords3d(atm).x;
            double y = AtomUtils.getCoords3d(atm).y;
            double z = AtomUtils.getCoords3d(atm).z;
 
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.ENGLISH,"%3d",atm.getAtomicNumber())).append(" ");
            sb.append(String.format(Locale.ENGLISH," %13.8f",x)).append(" ");
            sb.append(String.format(Locale.ENGLISH," %13.8f",y)).append(" ");
            sb.append(String.format(Locale.ENGLISH," %13.8f",z));
            lines.add(sb.toString());
        }
        lines.add(SpartanConstants.XYZEND);
        
        //Topology
        lines.add(SpartanConstants.TOPOOPN);
        Iterator<IAtom> it = mol.atoms().iterator();
        while (it.hasNext())
        {
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<12; i++)
            {
                if (!it.hasNext())
                {
                    break;
                }
                IAtom atm = it.next();
                sb.append(
                         String.format(Locale.ENGLISH,"%5d",-mol.getConnectedBondsCount(atm)));
            }
            lines.add(sb.toString());
        }
        for (IBond bnd : mol.bonds())
        {
            int iA = mol.indexOf(bnd.getAtom(0));
            int iB = mol.indexOf(bnd.getAtom(1));
            int bo = -1;
            IBond.Order order = bnd.getOrder();
            if (order == IBond.Order.valueOf("SINGLE"))
            {
                bo = 1;
            } 
            else if (order == IBond.Order.valueOf("DOUBLE"))
            {
                bo = 2;
            } 
            else if (order == IBond.Order.valueOf("TRIPLE"))
            {
                bo = 3;
            } 
            else
            {
                if (verbosity > 0)
                {
                    System.out.println("WARNING! Unknown bond order between "
                        + MolecularUtils.getAtomRef(bnd.getAtom(0),mol)+" and "
                        + MolecularUtils.getAtomRef(bnd.getAtom(1),mol)
                        + " treated as single bond.");
                }
                bo = 1;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.ENGLISH,"%5d",iA+1)); //to 1-based ID
            sb.append(String.format(Locale.ENGLISH,"%5d",iB+1)); //to 1-based ID
            sb.append(String.format(Locale.ENGLISH,"%5d",bo));
            lines.add(sb.toString());
        }
        lines.add(SpartanConstants.TOPOEND);

        //Comments
        lines.add(SpartanConstants.COMMOPN);
        lines.add(SpartanConstants.INDENT + MolecularUtils.getNameOrID(mol));
        lines.add(SpartanConstants.COMMEND);

        //Properties
        lines.addAll(getCellDirective(mol));

        //Fozen atoms
        if (defFrozen)
        {
            ArrayList<String> part = getFrozenAtomLines(mol,msq);
            if (part.size() > 0)
            {
                if (!lines.get(0).toUpperCase().contains("PARTIAL"))
                {
                    lines.set(0,lines.get(0) + " PARTIAL");
                }
                lines.add(SpartanConstants.FREEZEOPN);
                lines.addAll(part);
                lines.add(SpartanConstants.FREEZEEND);
            }
        }

        //Conformations
        if (defRotoBnds)
        {
            ArrayList<String> part = getConformationalSpaceLines(mol,msq);
            if (part.size() > 0)
            {
                lines.add(SpartanConstants.CONFDIROPN);
                lines.addAll(part);
                lines.add(SpartanConstants.CONFDIREND);
            }
        }

        //Constraints
        if (defConstraints)
        {
            ArrayList<String> part = getConstraintsLines(mol,msq);
            if (part.size() > 0)
            {
                if (!lines.get(0).toUpperCase().contains("CONSTRAIN"))
                {
                    lines.set(0,lines.get(0) + " CONSTRAIN");
                }
                lines.add(SpartanConstants.CSTRDIROPN);
                lines.addAll(part);
                lines.add(SpartanConstants.CSTRDIREND);
            }
        }

        //Dynamic Constraints
        if (defDynConstraints)
        {
            ArrayList<String> part = getDynConstraintsLines(mol,msq);
            if (part.size() > 0)
            {
                lines.add(SpartanConstants.DYNCDIROPN);
                lines.addAll(part);
                lines.add(SpartanConstants.DYNCDIREND);
            }
        }

        //Atom labels
        lines.add(SpartanConstants.ATMLABELSOPN);
        for (IAtom atm : mol.atoms())
        {
            lines.add("\"" + atm.getProperty(
                           SpartanConstants.ATMLABELATMPROP).toString() + "\"");
        }
        lines.add(SpartanConstants.ATMLABELSEND);

        //Spartan cannot live without the PROPIN directive (even if empty)
        lines.add(SpartanConstants.PRODIROPN);
        lines.add(SpartanConstants.PRODIREND);

        return lines;
    }


//------------------------------------------------------------------------------

    /**
     * Append a property to all atoms with the atom label assigned according to
     * Spartan style (i.e., element symbol + number of atoms of such element 
     * including the current atom).
     * @param mol the molecule to edit
     */

    private void specifyAtomLabels(IAtomContainer mol)
    {
        Map<String,Integer> counters = new HashMap<String,Integer>();
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            IAtom atm = mol.getAtom(i);
            String el = atm.getSymbol();
            String label = "notSet";
            if (counters.keySet().contains(el))
            {
                int elCount = counters.get(el);
                label = el + (elCount+1);
                counters.put(el,elCount+1);
            }
            else
            {
                label = el + "1";
                counters.put(el,1);
            }
            atm.setProperty(SpartanConstants.ATMLABELATMPROP,label);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Prepare the list of atom specific keywords
     * @param mol the molecule
     * @param msq the collector of the results from SMARTS-based atom matches
     * @return a single string with all the atom specific keywords.
     */

    private String getAtomSpecificKeywords(IAtomContainer mol, 
                                                            ManySMARTSQuery msq)
    {
        StringBuilder sb = new StringBuilder();
        for (String atmSpecKeyRuleName : getSortedSMARTSRefNames(smarts))
        {
            if (!atmSpecKeyRuleName.toUpperCase().startsWith(
                                                      ATMKEYROOT.toUpperCase()))
            {
                continue;
            }

            for (String key : smarts.keySet())
            {
                // For now only one-SMARTS per atom specific keyword is expected
                if (key.startsWith(atmSpecKeyRuleName))
                {
                    ArrayList<String> keywordParts = smartsOpts.get(
                                                            atmSpecKeyRuleName);

                    if (msq.getNumMatchesOfQuery(key) == 0)
                    {
                        System.out.println("WARNING! No match for SMARTS query "
                                           + smarts.get(key) + " in molecule "
                                           + MolecularUtils.getNameOrID(mol) 
                                           + ". SMARTS-based "
                                           + "request will not take effect.");
                        continue;
                    }

                    List<List<Integer>> matches = msq.getMatchesOfSMARTS(key);
                    for (List<Integer> innerLst : matches)
                    {
                        for (Integer atmId : innerLst)
                        {
                            IAtom atm = mol.getAtom(atmId);
                            String lab = atm.getProperty(
                                   SpartanConstants.ATMLABELATMPROP).toString();

                            sb.append(" ").append(keywordParts.get(0));
                            sb.append(lab);
                            for (int j=1; j<keywordParts.size(); j++)
                            {
                                sb.append(keywordParts.get(j)).append(" ");
                            }
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Prepare the list of text lines for the definitions of frozen atoms
     * @return the list of lines
     */

    private ArrayList<String> getFrozenAtomLines(IAtomContainer mol,
                                                            ManySMARTSQuery msq)
    {
        // Get all matches
        Set<Integer> allMatches = new HashSet<Integer>();
        for (String key : smarts.keySet())
        {
            if (!key.toUpperCase().startsWith(FREEZEROOT.toUpperCase()))
            {
                continue;
            }
            
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
                System.out.println("WARNING! No match for SMARTS query "
                           + smarts.get(key) + " in molecule "
                           + MolecularUtils.getNameOrID(mol) + ". This SMARTS-"
                           + "based atom freezing rule will not take effect.");
                continue;
            }

            List<List<Integer>> matches = msq.getMatchesOfSMARTS(key);
            for (List<Integer> innerLst : matches)
            {
                allMatches.addAll(innerLst);
            }
        }
        ArrayList<Integer> sortedMatches = new ArrayList<Integer>();
        sortedMatches.addAll(allMatches);
        Collections.sort(sortedMatches);

        // Build text lines
        ArrayList<String> lines = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Integer atmId : sortedMatches)
        {
            i++;
            //NB: change from 0-based to 1-based atom index
            sb.append(String.format(Locale.ENGLISH,"%5d",atmId+1));
            if (i > 11)
            {
                i = 0;
                lines.add(sb.toString());
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0)
        {
            lines.add(sb.toString());
        }

        if (verbosity > 0)
        {
            System.out.println(" Number of frozen atoms: " + allMatches.size());
        }

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Prepare the list of text lines for the definition of the conformational
     * space.
     * @return the list of lines
     */

    private ArrayList<String> getConformationalSpaceLines(IAtomContainer mol,
                                                            ManySMARTSQuery msq)
    {
        // Extract definition of conformational space from input
        ConformationalMovesDefinition cmd = new ConformationalMovesDefinition();
        for (String confRuleName : getSortedSMARTSRefNames(smarts))
        {
            if (!confRuleName.toUpperCase().startsWith(ROTOROOT.toUpperCase()))
            {
                continue;
            }

            for (String key : smarts.keySet())
            {
                if (key.startsWith(confRuleName))
                {
                    cmd.addMoveDefinition(key,smarts.get(key),
                                                  smartsOpts.get(confRuleName));
                }
            }
        }

        // Define the conformational space
        ConformationalSpace confSpace = 
                ConformationalSpaceBuilder.makeConfSpace(mol,cmd,msq,verbosity);

        if (verbosity > 0)
        {
            System.out.println(" Number of conformers: "+ confSpace.getSize());
        }

        // Build text lines
        ArrayList<String> lines = new ArrayList<String>();
        for (ConformationalCoordinate cc : confSpace)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(cc.getAtomIDsAsString(true,"%5d"));
            sb.append(String.format(Locale.ENGLISH,"%5d",cc.getFold()));
            lines.add(sb.toString());
        }

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Prepare the list of text lines for the definition of contraints
     * @param mol the CDK representation of the system
     */

    private ArrayList<String> getConstraintsLines(IAtomContainer mol,
                                                            ManySMARTSQuery msq)
    {
        //TODO: should make this a method... 
        // Reorganize lists to group single-atom smarts that pertain to same IC
        Map<String,ArrayList<ArrayList<Integer>>> allIcRules =
                            new HashMap<String,ArrayList<ArrayList<Integer>>>();
        for (String icRuleName : getSortedSMARTSRefNames(smarts))
        {
            if (!icRuleName.toUpperCase().startsWith(CTRSROOT.toUpperCase()))
            {
                continue;
            }

            if (verbosity > 2)
            {
                System.out.println("Matching constraint rule '"+icRuleName+"'");
            }

            boolean skipRule =false;
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

                if (verbosity > 2)
                {
                   System.out.println("Recovering rule portion '" + key + "'");
                }

                int pos = Integer.parseInt(key.substring(preStr.length()));
                if (msq.getNumMatchesOfQuery(key) == 0)
                {
                    skipRule = true;
                    System.out.println("WARNING! No match for SMARTS query "
                         + smarts.get(key) + " in molecule "
                         + MolecularUtils.getNameOrID(mol) + ". SMARTS-based "
                         + "request will not take effect.");
                    break;
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
                if (verbosity > 2)
                {
                    System.out.println("Skipping rule '" + icRuleName + "'");
                }
                continue;
            }
            allIcRules.put(icRuleName,partsIcRule);
        }

        if (verbosity > 2)
        {
            System.out.println("Matching completed.");
            System.out.println("Rules with full set of matches: " 
                                                         + allIcRules.keySet());
        }

        // Get all constraints by comparing matched atoms with topology and
        // adding the options from user input
        ArrayList<ArrayList<String>> allCstrs = 
                                             new ArrayList<ArrayList<String>>();
        allCstrs.add(new ArrayList<String>()); //to collect bond-type of cstrs
        allCstrs.add(new ArrayList<String>()); //to collect angle-type
        allCstrs.add(new ArrayList<String>()); //to collect torsion-type
        for (String ruleKey : allIcRules.keySet())
        {
            if (verbosity > 2)
            {
                System.out.println("Applying constraints from rule '" 
                                                                + ruleKey+ "'");
            }
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
                                    processIDsNTuplaForConstraint(
                                        new ArrayList<Integer>(
                                                Arrays.asList(idA,idB,idC,idD)),
                                                        smartsOpts.get(ruleKey),
                                                                  allCstrs,mol);
                                }
                            }
                            else
                            {
                                processIDsNTuplaForConstraint(
                                    new ArrayList<Integer>(
                                                    Arrays.asList(idA,idB,idC)),
                                                        smartsOpts.get(ruleKey),
                                                                  allCstrs,mol);
                            }
                        }
                    }
                    else
                    {
                        processIDsNTuplaForConstraint(
                                 new ArrayList<Integer>(Arrays.asList(idA,idB)),
                                                        smartsOpts.get(ruleKey),
                                                                  allCstrs,mol);
                    }
                }
            }
        }

        if (verbosity > 0)
        {
            int tot = allCstrs.get(0).size() + allCstrs.get(1).size()
                                                       + allCstrs.get(2).size();
            System.out.println(" Total number of constraints: " + tot);
        }

        // Sort lists in to make it easy to read
        ArrayList<String> linesBnd = allCstrs.get(0);
        ArrayList<String> linesAng = allCstrs.get(1);
        ArrayList<String> linesTor = allCstrs.get(2);
        Collections.sort(linesBnd, new NumberAwareStringComparator());
        Collections.sort(linesAng, new NumberAwareStringComparator());
        Collections.sort(linesTor, new NumberAwareStringComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (String line : linesBnd)
        {
            lines.add(SpartanConstants.INDENT + line);
        }
        for (String line : linesAng)
        {
            lines.add(SpartanConstants.INDENT + line);
        }
        for (String line : linesTor)
        {
            lines.add(SpartanConstants.INDENT + line);
        }

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Process a single set of IDs that represent a constraint for a candidate 
     * internal coord.
     * For a list of atom IDs, we evaluate if the reordered list has been 
     * already used, prepare the proper string to be used in the 
     * directive, and add it to the proper block of strings depending
     * on whether is a bond, angle, or torsion
     * @param ids the list of atom indexes defining the internal coordinate
     * @param details the list of options/values/keywords to be appended to
     * the directive/instruction in the input file under construction
     * @param allCstrs the collector of the results: string with a format
     * suitable for ZCOORD directive.
     * @param mol the molecule under analysis
     */

    private void processIDsNTuplaForConstraint(ArrayList<Integer> ids,
                                                      ArrayList<String> details,
                                          ArrayList<ArrayList<String>> allCstrs,
                                                             IAtomContainer mol)
    {
        if (verbosity > 2)
        {
            System.out.println("Processing combination " +ids);
        }
        // Format the list of IDs
        int numIds = ids.size();
        String crdType = "";
        double currentValue = 0.0;
        switch (numIds)
        {
            case 2:
                crdType = "BOND";
                Collections.sort(ids);
                currentValue = MolecularUtils.calculateInteratomicDistance(
                                                       mol.getAtom(ids.get(0)),
                                                       mol.getAtom(ids.get(1)));
                break;

            case 3:
                crdType = "ANGL";
                if (ids.get(0) > ids.get(2))
                {
                    Collections.reverse(ids);
                }
                currentValue = MolecularUtils.calculateBondAngle(
                                                       mol.getAtom(ids.get(0)),
                                                       mol.getAtom(ids.get(1)),
                                                       mol.getAtom(ids.get(2)));
                break;

            case 4:
                crdType = "TORS";
                // NOTE: don't revert the 4-tupla: it could generate confusion
                // in case of improper torsions.
                // For instance, in the system D-C(-A)-B the ideal definition of
                // of the improper torsion is D,C,B,A, but upon inversion it
                // would become A,B,C,B, which looks weird because A and B are
                // not connected to each other.
                currentValue = MolecularUtils.calculateTorsionAngle(
                                                       mol.getAtom(ids.get(0)),
                                                       mol.getAtom(ids.get(1)),
                                                       mol.getAtom(ids.get(2)),
                                                       mol.getAtom(ids.get(3)));
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Something badly wrong. "
                    + "I got an internal coordinate with " + numIds
                    + " IDs. Please report this to the authors.",-1);
        }

        // Compare with existing ones 
        // TODO: this would be much more efficient if dealing with objects 
        // rather than with strings... No time for this now.
        // TOOD: should make a class that does all this
        for (String line : allCstrs.get(numIds-2)) 
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
                if (verbosity > 2)
                {
                    System.out.println("Ignoring previously defined or "
                                                          + "equivalent: "+ids);
                }
                // Exit without storing the new line
                return;
            }
        }

        // Build the string
        StringBuilder sb = new StringBuilder();
        sb.append(crdType).append(" ");
        for (Integer id : ids)
        {
            //WARNING: here we change from 0-based to 1-based
            sb.append(String.format(Locale.ENGLISH,"%5d",id+1)).append(" ");
        }
        if (details.size() == 0)
        {
            switch (numIds)
            {
                case 2:
                    sb.append(String.format(Locale.ENGLISH,"%10.6f",currentValue));
                    break;

                case 3:
                    sb.append(String.format(Locale.ENGLISH,"%10.6f",currentValue));
                    break;

                case 4:
                    sb.append(String.format(Locale.ENGLISH,"%10.6f",currentValue));
                    break;
            }
        }
        else
        {
            for (String s : details)
            {
                sb.append(s).append(" ");
            }
        }

        // Store 
        allCstrs.get(numIds-2).add(sb.toString());
    }    

//------------------------------------------------------------------------------

    /**
     * Prepare the lines of text lines for the definition of dynamic constraints
     * @param mol the CDK representation of the system
     */

    private ArrayList<String> getDynConstraintsLines(IAtomContainer mol,
                                                            ManySMARTSQuery msq)
    {
        //TODO: should make this a method...
        // Reorganize lists to group single-atom smarts that pertain to same IC
        Map<String,ArrayList<ArrayList<Integer>>> allIcRules =
                            new HashMap<String,ArrayList<ArrayList<Integer>>>();
        for (String icRuleName : getSortedSMARTSRefNames(smarts))
        {
            if (!icRuleName.toUpperCase().startsWith(DYNCROOT.toUpperCase()))
            {
                continue;
            }

            if (verbosity > 2)
            {
                System.out.println("Matching dynamic constraint rule '"
                                                                                                          +icRuleName+"'");
            }

            boolean skipRule =false;
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

                if (verbosity > 2)
                {
                   System.out.println("Recovering rule portion '" + key + "'");
                }

                int pos = Integer.parseInt(key.substring(preStr.length()));
                if (msq.getNumMatchesOfQuery(key) == 0)
                {
                    skipRule = true;
                    System.out.println("WARNING! No match for SMARTS query "
                         + smarts.get(key) + " in molecule "
                         + MolecularUtils.getNameOrID(mol) + ". SMARTS-based "
                         + "request will not take effect.");
                    break;
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
                if (verbosity > 2)
                {
                    System.out.println("Skipping rule '" + icRuleName + "'");
                }
                continue;
            }
            allIcRules.put(icRuleName,partsIcRule);
        }

        if (verbosity > 2)
        {
            System.out.println("Matching completed.");
            System.out.println("Rules with full set of matches: " 
                                                         + allIcRules.keySet());
        }

        // Get all dyn.constrs by comparing matched atoms with topology and
        // adding the options from user input
        ArrayList<ArrayList<String>> allDynCs = 
                                             new ArrayList<ArrayList<String>>();
        allDynCs.add(new ArrayList<String>()); //to collect bond-type
        allDynCs.add(new ArrayList<String>()); //to collect angle-type
        allDynCs.add(new ArrayList<String>()); //to collect torsion-type
        for (String ruleKey : allIcRules.keySet())
        {
            if (verbosity > 2)
            {
                System.out.println("Creating dynamic constraints from rule '" 
                                                                + ruleKey+ "'");
            }
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
                                    processIDsNTuplaForDynConstrs(
                                        new ArrayList<Integer>(
                                                Arrays.asList(idA,idB,idC,idD)),
                                                        smartsOpts.get(ruleKey),
                                                                  allDynCs,mol);
                                }
                            }
                            else
                            {
                                processIDsNTuplaForDynConstrs(
                                    new ArrayList<Integer>(
                                                    Arrays.asList(idA,idB,idC)),
                                                        smartsOpts.get(ruleKey),
                                                                  allDynCs,mol);
                            }
                        }
                    }
                    else
                    {
                        processIDsNTuplaForDynConstrs(
                                 new ArrayList<Integer>(Arrays.asList(idA,idB)),
                                                        smartsOpts.get(ruleKey),
                                                                  allDynCs,mol);
                    }
                }
            }
        }

        if (verbosity > 0)
        {
            int tot = allDynCs.get(0).size() + allDynCs.get(1).size()
                                                       + allDynCs.get(2).size();
            System.out.println(" Total number of dynamic constraints: " + tot);
        }

        // Sort list to make it easy to read
        ArrayList<String> linesBnd = allDynCs.get(0);
        ArrayList<String> linesAng = allDynCs.get(1);
        ArrayList<String> linesTor = allDynCs.get(2);
        Collections.sort(linesBnd, new NumberAwareStringComparator());
        Collections.sort(linesAng, new NumberAwareStringComparator());
        Collections.sort(linesTor, new NumberAwareStringComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (String line : linesBnd)
        {
            lines.add(SpartanConstants.INDENT + line);
        }
        for (String line : linesAng)
        {
            lines.add(SpartanConstants.INDENT + line);
        }
        for (String line : linesTor)
        {
            lines.add(SpartanConstants.INDENT + line);
        }

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Process a single set of IDs that represents a dynamic constraint for a  
     * candidate internal coord.
     * For a list of atom IDs, we evaluate if the reordered list has been 
     * already used, prepare the proper string to be used in the 
     * directive, and add it to the proper block of strings depending
     * on whether is a bond, angle, or torsion
     * @param ids the list of atom indexes defining the internal coordinate
     * @param details the list of options/values/keywords to be appended to
     * the directive/instruction in the input file under construction
     * @param allCstrs the collector of the results: string with a format
     * suitable for ZCOORD directive.
     * @param mol the molecule under analysis
     */

    private void processIDsNTuplaForDynConstrs(ArrayList<Integer> ids,
                                                      ArrayList<String> details,
                                          ArrayList<ArrayList<String>> allCstrs,
                                                             IAtomContainer mol)
    {
        if (verbosity > 2)
        {
            System.out.println("Processing combination " +ids);
        }
        // Format the list of IDs
        int numIds = ids.size();
        // The current value is not used now... but might turn out useful
/*        
        double currentValue = 0.0;
        switch (numIds)
        {
            case 2:
                Collections.sort(ids);
                currentValue = MolecularUtils.calculateInteratomicDistance(
                                                       mol.getAtom(ids.get(0)),
                                                       mol.getAtom(ids.get(1)));
                break;

            case 3:
                if (ids.get(0) > ids.get(2))
                {
                    Collections.reverse(ids);
                }
                currentValue = MolecularUtils.calculateBondAngle(
                                                       mol.getAtom(ids.get(0)),
                                                       mol.getAtom(ids.get(1)),
                                                       mol.getAtom(ids.get(2)));
                break;

            case 4:
                // NOTE: don't revert the 4-tupla: it could generate confusion
                // in case of improper torsions.
                // For instance, in the system D-C(-A)-B the ideal definition of
                // of the improper torsion is D,C,B,A, but upon inversion it
                // would become A,B,C,B, which looks weird because A and B are
                // not connected to each other.
                currentValue = MolecularUtils.calculateTorsionAngle(
                                                       mol.getAtom(ids.get(0)),
                                                       mol.getAtom(ids.get(1)),
                                                       mol.getAtom(ids.get(2)),
                                                       mol.getAtom(ids.get(3)));
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Something badly wrong. "
                    + "I got an internal coordinate with " + numIds
                    + " IDs. Please report this to the authors.",-1);
        }
*/

        // Compare with existing ones 
        // TODO: this would be much more efficient if dealing with objects 
        // rather than with strings... No time for this now.
        // TOOD: should make a class that does all this
        for (String line : allCstrs.get(numIds-2)) 
        {
            String[] parts = line.split("\\s+");
            int idAOnUsed = Integer.parseInt(parts[1]) - 1;
            int idBOnUsed = Integer.parseInt(parts[2]) - 1;
            int idCOnUsed = Integer.parseInt(parts[3]) - 1;
            boolean alreadyDefined = false;
            switch (numIds)
            {
                case 2:
                    if ((ids.get(0) == idAOnUsed && ids.get(1) == idBOnUsed) ||
                        (ids.get(1) == idAOnUsed && ids.get(0) == idBOnUsed))
                    {
                        alreadyDefined = true;
                    }
                    break;
                case 3:
                    if (ids.get(1) == idBOnUsed)
                    {
                        if ((ids.get(0) == idAOnUsed && ids.get(2) == idCOnUsed) ||
                            (ids.get(2) == idAOnUsed && ids.get(0) == idCOnUsed))
                        {
                            alreadyDefined = true;
                        }
                    }
                    break;
                case 4:
                    if ((ids.get(1) == idBOnUsed && ids.get(2) == idCOnUsed) ||
                        (ids.get(2) == idBOnUsed && ids.get(1) == idCOnUsed))
                    {
                        alreadyDefined = true;
                    }
                    break;
            }
            if (alreadyDefined)
            {
                if (verbosity > 2)
                {
                    System.out.println("Ignoring previously defined or "
                                                    + "equivalent to: "+ids);
                }
                // Exit without storing the new line
                return;
            }
        }

        // Build the string defining the dynamic constraint
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for (Integer id : ids)
        {
            //WARNING: here we change from 0-based to 1-based
            sb.append(String.format(Locale.ENGLISH,"%5d",id+1)).append(" ");
        }
            switch (numIds)
        {
            case 2:
                sb.append("    0     0 ");
                            break;

            case 3:
                sb.append("    0 ");
                break;
            }
        if (details.size() < 4)
        {
            Terminator.withMsgAndStatus("ERROR! Definition of a dynamic "
                    + "constraint needs at least 4 numerical values. For "
                    + "tupla '" + ids + "' there are only " + details.size()
                    + " arguments in the options. Check your input.", -1);
        }
        else
        {
            for (String s : details)
            {
                sb.append(s).append(" ");
            }
        }

        // Store 
        allCstrs.get(numIds-2).add(sb.toString());
    }

//------------------------------------------------------------------------------

}
