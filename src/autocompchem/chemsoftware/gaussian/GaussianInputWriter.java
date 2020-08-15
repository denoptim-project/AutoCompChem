package autocompchem.chemsoftware.gaussian;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;

/**
 * Writes input files for Gaussian. Accepts both the direct definition of the
 * header (Link0 and Route sections of Gaussian input) and the use of 
 * a jobdetails formatted text file (see {@link GaussianJob}).<br>
 * Parameters:
 * <ul>
 * <li>
 * <b>INFILE</b>: name of the structure file (i.e. path/name.sdf).
 * </li>
 * <li>
 * <b>JOBDETAILS</b>: formatted text file defining all 
 * the details of a {@link GaussianJob}. Usually the jobdetails txt 
 * file is the file used to generate the input file (name
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#GAUINPEXTENSION}
 * ) for 
 * Gaussian (see {@link GaussianJob} for the format of jobdetails files).
 * In alternative, use
 * keyword 
 * <b>HEADER</b> with a labelled block of lines (i.e., a bunch of text 
 * starting with the $START label and finishing with the $END label).
 * The text in the labelled block is used as header of 
 * the Gaussian input file.
 * In this header only Gaussian's 'Link0' and 'Route' sections
 * should be included. For comments, charge and spin 
 * multiplicity, see below.<br>
 * <br>
 * <b>WARNING!</b> This header is treated as a pure string and 
 * no check
 * of its format will be performed. Thus if the header has wrong
 * format, so will have the output file.
 * Moreover, by using a fixed header no modification of such 
 * header can be performed by the GaussianInputWriter or by any
 * other tool (i.e. {@link GaussianReStarter}). For instance,
 * name of checkpoint file, memory, nodes, processors, and 
 * everything that is specified in the header CANNOT BE EDITED!<br>
 * <br>
 * </li>
 * <li>
 * (optional) <b>VERBOSITY</b> verbosity level.
 * </li>
 * <li>
 * (optional) <b>OUTNAME</b> name of the output file (
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#GAUINPEXTENSION}
 * for Gaussian)
 * </li>
 * </ul>
 * 
 * Optional parameters not needed if JOBDETAILS option is in use, but
 * that will overwrite JOBDETAILS specifications if both JOBDETAILS and
 * these options are specified in the {@link ParameterStorage}.
 * <ul>
 * <li>
 * (optional) <b>COMMENT</b> comment line for the output file
 * </li>
 * <li>
 * (optional) <b>CHARGE</b> the charge of the chemical system
 * </li>
 * <li>
 * (optional) <b>SPIN_MULTIPLICITY</b>  the spin multiplicity of the 
 * chemical system
 * </li>
 * </ul>
 *            
 * 
 * @author Marco Foscato
 */

public class GaussianInputWriter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTGAUSSIAN)));

    //Input filename
    private String inFile;

    //Input format identifier
    private String inFormat = "nd";

    /**
     *  Output name (input for Gaussian)
     */
    private String outFile;

    /**
     * Output job details name
     */
    private String outJDFile;

    //Keywords or job details
    private String header;
    private String comment;
    private final int def = -999999;
    private int charge = def;
    private int spinMult = def;
    private GaussianJob gaussJob;

    //Use header or job details from GaussianJob
    private boolean useHeader;

    //Verbosity level
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
            String vStr =params.getParameter("VERBOSITY").getValue().toString();
            this.verbosity = Integer.parseInt(vStr);

            if (verbosity > 0)
                System.out.println(" Adding parameters to GaussianInputWriter");
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
                  + "GaussianInputWriter can handle only SDF or XYZ input",-1);
        }
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Use GaussianJob or header specified by hand?
        useHeader = false;
        if (params.contains("JOBDETAILS"))
        {
            //Use GaussianJob
            String jdFile = 
                        params.getParameter("JOBDETAILS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Compound Gaussian job: details from "  
                         + jdFile);
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.gaussJob = new GaussianJob(jdFile);
        } else if (params.contains("HEADER")) {
            //Use header (only for single step jobs)
            useHeader = true;
            header = params.getParameter("HEADER").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Single step Gaussian job "
                        + "(details from header)\n" + header);
            }
        } else {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                + " No 'JOBDETAILS' or 'HEADER' found in parameters.",-1);
        }

        //Name of output
        if (params.contains("OUTNAME"))
        {
            outFile = params.getParameter("OUTNAME").getValue().toString();
            outJDFile = FileUtils.getRootOfFileName(outFile)
                                                + GaussianConstants.JDEXTENSION;
        } else {
            String inputRoot = FileUtils.getRootOfFileName(inFile);
            outFile = inputRoot + GaussianConstants.GAUINPEXTENSION;
            outJDFile = inputRoot + GaussianConstants.JDEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No 'OUTNAME' option found. "
                + "Output name set to '" + outFile + "'.");
            }
        }

        //COMMENT
        if (params.contains("COMMENT"))
        {
            comment = params.getParameter("COMMENT").getValue().toString();
            if (!useHeader)
            {
                this.gaussJob.setAllComments(comment);
                if (verbosity > 0)
                {
                    System.out.println(" Found 'COMMENT' option. Overwriting "
                              + "comments in all steps");
                }
            }
        } else if (useHeader) {
            comment = "Compound Job created from " + inFile
                            + " by AutoCompChem (with header)";
        }

        //CHARGE
        if (params.contains("CHARGE"))
        {
            charge = Integer.parseInt(
                        params.getParameter("CHARGE").getValue().toString());
            if (!useHeader)
            {
                this.gaussJob.setAllCharge(charge);
                if (verbosity > 0)
                {
                    System.out.println(" Found 'CHARGE' option. Overwriting "
                        + "charge in all steps using " + charge);
                }
            }
        } else {
            if (!useHeader)
            {
                charge = this.gaussJob.getStep(0).getMolSpec().getCharge();
            }
        }

        //SPIN_MULTIPLICITY
        if (params.contains("SPIN_MULTIPLICITY"))
        {
            spinMult = Integer.parseInt(
               params.getParameter("SPIN_MULTIPLICITY").getValue().toString());
            if (!useHeader)
            {
                this.gaussJob.setAllSpinMultiplicity(spinMult);
                if (verbosity > 0)
                {
                    System.out.println(" Found 'SPIN_MULTIPLICITY' option. "
                        + "Overwriting spin multiplicity in all steps "
                        + "using " + spinMult);
                }
            }
        } else {
            if (!useHeader)
            {
                spinMult = 
                    this.gaussJob.getStep(0).getMolSpec().getSpinMultiplicity();
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
          case PREPAREINPUTGAUSSIAN:
              writeInp();
              break;
          }

        if (exposedOutputCollector != null)
        {
/*
            String refName = "";
            exposeOutputData(new NamedData(refName,
                  NamedDataType.DOUBLE, ));
*/
        }
    }
//------------------------------------------------------------------------------

    /**
     * Write Gaussian input file according to the parameters given to the
     * constructor of this GaussianInputWriter. In case of multi entry  
     * files, a single input will be generated per each molecule using the name
     * of the molecule (title in SDF properties) as root for the output name
     * Gaussian job details derive from the current status of the
     * <code>GaussianJob</code> field in this GaussianInputWriter and the other
     * field defined by the constructor.
     * No argument is accepted by this method, so,
     * before running, make sure you provided the proper settings
     * during contruction of this GaussianInputWriter.
     */

    public void writeInp()
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

                default:
                    Terminator.withMsgAndStatus("ERROR! GaussianInputWriter"
                        + " does not accept file format other than SDF and"
                        + " XYZ. Make sure file '" + inFile +"' has proper "
                        + " format and extension.", -1);
            }

            for (IAtomContainer mol : mols)
            {
                n++;
                String molName = MolecularUtils.getNameOrID(mol);

                //Set name of the output file and checkpoint
                // If there is more than one molecule we cannot use the given
                // outFile for the output.
                if (mols.size() > 1)
                {
                    outFile = molName + GaussianConstants.GAUINPEXTENSION;
                    outJDFile = molName + GaussianConstants.JDEXTENSION;
                }

                FileUtils.mustNotExist(outFile);
                FileUtils.mustNotExist(outJDFile);
                String checkPointName = FileUtils.getRootOfFileName(outFile);

                //write INP using fixed header or JobDetails
                if (useHeader)
                {
                    writeSingleStepInp(mol,header,comment,outFile);
                } 
                else 
                {
                    //Update molecular representation
                    if (inFormat=="SDF")
                    {
                        if (chargeOrSpinFromIAC(mol))
                        {
                            gaussJob.setAllCharge(charge);
                            gaussJob.setAllSpinMultiplicity(spinMult);
                        }
                    }

                    //If by any chance one of the two was not defined before
                    checkChargeSpinNotAtDefault();

                    if (gaussJob.getStep(0).needsGeometry())
                    {
                        GaussianMolSpecification gMolSpec  = 
                              new GaussianMolSpecification(mol,charge,spinMult);
                        gaussJob.getStep(0).setMolSpecification(gMolSpec);
                    }

                    //Update molecule/atom-specific options
                    if (inFormat=="XYZ")
                    {
                        String msg = "WARNING! To ensure proper chemical  "
                        + "perception of atoms and groups use an input file "
                        + "that specified the connectivity and the bond orders "
                        + "(i.e., SDF). For now, only "
                        + "a poor chemical perception can be achieved from XYZ "
                        + "input files";
                        System.out.println(msg);
                        System.err.println(msg);
                    }
                    for (int istp=0; istp<gaussJob.getNumberOfSteps(); istp++)
                    {
                        GaussianStep gStep = gaussJob.getStep(istp);
                        GaussianOptionsSection gOpts = gStep.getOptionSection();

                        for (String oName : gOpts.getRefNames())
                        {
                            String value = gOpts.getValue(oName);
                            if (value.toUpperCase().startsWith(
                                                   GaussianConstants.LABPARAMS))
                            {
                                String aLn = value.substring(
                                          GaussianConstants.LABPARAMS.length());
                                String[] aLs = aLn.split(
                                          System.getProperty("line.separator"));
                                ArrayList<String> aAl = new ArrayList<String>(
                                                            Arrays.asList(aLs));
                                ParameterStorage aPs = new ParameterStorage();
                                aPs.importParametersFromLines("jobDetails",aAl);
                                String updatedOpt = getMoleculeSpecificOpts(mol,
                                                                     aPs,gStep);
                                gOpts.setPart(oName,updatedOpt);
                            }
                        }
                    }

                    //Update name of checkpoint file
                    gaussJob.setAllLinkSections("CHK",checkPointName);

                    //Write inp
                    writeGaussianInput();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                + "making Gaussin input for molecule " + n + " from file " 
                + inFile + "\n" + t, -1);
        }
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
     * Generated atom/molecule-specific options.
     * @param mol the specific molecule
     * @param params the details of the work to do. This object should contain
     * only one parameter.
     * @param gStep the step to which the options apply
     * @return a single string (likely containing newline character) with the
     * atom/molecule-specific options
     */

    public String getMoleculeSpecificOpts(IAtomContainer mol, 
                                    ParameterStorage params, GaussianStep gStep)
    {
        String result = "";
        String verbKey = ACCConstants.VERBOSITYPAR;
        GaussianRouteSection stepRoute = gStep.getRouteSection();

        for (String action : params.getRefNamesSet())
        {
            switch (action.toUpperCase()) 
            {
                case BasisSetConstants.ATMSPECBS:                	
                    ParameterStorage locPars = new ParameterStorage();
                    locPars.setParameter(params.getParameter(action));
                    locPars.setParameter(new Parameter(verbKey,
                    		NamedDataType.INTEGER, verbosity));
                    
                	// Get a worker to deal with the basis set generation task
                    locPars.setParameter(new Parameter("TASK",
                		NamedDataType.STRING, "GENERATEBASISSET"));
                	Worker w = WorkerFactory.createWorker(locPars);
                    BasisSetGenerator bsg = (BasisSetGenerator) w;
                    
                    bsg.setAtmIdxAsId(true);
                    BasisSet bs = bsg.assignBasisSet(mol);
                    String genBsKey = "GEN";
                    if (bs.hasECP())
                    {
                        genBsKey = "GENECP";
                    }
                    String bsKeyRef = GaussianConstants.SUBKEYMODELBASISET;
                    if (stepRoute.containsKey(bsKeyRef))
                    {
                        String oldBsKey = stepRoute.getValue(bsKeyRef);
                        String[] parts = oldBsKey.split("\\s+");
                        for (int j=1; j<parts.length;j++)
                        {
                            genBsKey = genBsKey + " " + parts[j];
                        }
                    }
                    stepRoute.put(bsKeyRef,genBsKey);
                    result = bs.toInputFileString("gaussian");
                    break;

                //TODO: add here other atom/molecule specific option

                default:
                    String msg = "WARNING! Action '" + action + "' is not a "
                           + "known task when updating atom/molecule-specific "
                           + "options in a Gaussian input file.";
                    if (verbosity > 0)
                    {
                        System.out.println(msg);
                    }
                    break;
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

    /**
     * Write Gaussian input file. Only a single step, no compound job.
     * Charge and Spin Multiplicity will be taken  
     * from the properties "SPIN_MULTIPLICITY" and "CHARGE" of the 
     * IAtomContainer. If these 
     * properties are not defined the execution will stop with an error.
     * @param mol molecule
     * @param kw keywords section including both 'link 0' and Route' sections
     * blank line termination is already included in the form.
     * @param comm text comment
     * @param outName name of the output file
     */

    public void writeSingleStepInp(IAtomContainer mol, String kw, String comm,
                                        String outName)
    {
        //This call also changes spin and charge
        if (chargeOrSpinFromIAC(mol))
        {
            //If by any chance one of the two was not defined from before
            checkChargeSpinNotAtDefault();
        } else {
            Terminator.withMsgAndStatus("ERROR! Properties "
                + "<SPIN_MULTIPLICITY> and <CHARGE> cannot be taken from "
                + "SDF properties.",-1);
        }

        //write the inp file for Gaussian
        comment = comm;
        writeSingleStepInp(mol, kw, comment, charge, spinMult, outName);
    }

//------------------------------------------------------------------------------

    /**
     * Write Gaussian input file. Only a single step, no compound job.
     * @param mol molecule
     * @param kw Keywords section including both 'link 0' and Route' sections
     * blank line termination is already included in the form.
     * @param comm text comment
     * blank line termination is already included in the form.
     * @param ch charge
     * @param sm spin multiplicity
     * @param outName name of the output file
     */

    public void writeSingleStepInp(IAtomContainer mol, String kw, String comm, 
                                        int ch, int sm, String outName)
    {
        //check file
        FileUtils.mustNotExist(outName);

        FileWriter writer = null;
        try
        {
            writer = new FileWriter(outName,true); 

            //write KEYWORDS - Link0 and Route sections
            writer.write(kw + "\n");
            writer.write("\n"); // Black line terminated section

            //write COMMENT - Title section
            writer.write(comm + "\n");
            writer.write("\n"); // Black line terminated section

            //write CHARGE and SPIM MULTIPLICITY - Molecule specification 1
            writer.write(ch + " " + sm + "\n");
            // write Z-matrix or coordinates - Molecule specification 2
            for (IAtom a : mol.atoms())
            {
                String symbol = a.getSymbol();
                Point3d p3d = a.getPoint3d();
                writer.write(symbol + "     " +
                              p3d.x + "     " +
                              p3d.y + "     " +
                              p3d.z + "     \n");
            }
            writer.write("\n"); // Black line terminated section

        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Failure in writing Gaussian "
                        + "input. Details " + t,-1);
        } finally {
             try {
                 if(writer != null)
                     writer.close();
             } catch (IOException ioe) {
                Terminator.withMsgAndStatus("ERROR! Failure in compliting "
                        + "writing process for Gaussian input." + ioe, -1);
             }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write multistep Gaussian input file, either Single- or multi-step job, 
     * using only the setting provided in constructing this GaussianInputWriter
     */

    public void writeGaussianInput()
    {
        if (verbosity > 0)
        {
             System.out.println(" Writing Gaussian input file: " + outFile);
        }

        IOtools.writeTXTAppend(outFile,gaussJob.toLinesInp(),false);
        IOtools.writeTXTAppend(outJDFile,gaussJob.toLinesJob(),false);
    }

//------------------------------------------------------------------------------

}
