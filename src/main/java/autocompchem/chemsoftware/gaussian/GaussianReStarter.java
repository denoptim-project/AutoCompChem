package autocompchem.chemsoftware.gaussian;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.errorhandling.ErrorMessage;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * Restarts a Gaussian job that returned an error. The tool evaluates
 *  the outcome of a Gaussian job and, after detection and 
 * identification of the error message, generates a new input file
 *  with settings that may allow the calculation to avoid/fix the
 *  error and terminate properly. The errors and corresponding 
 * error fixing actions are defined in a database provided by the 
 * user. The execution of the this tool requires the following 
 * parameters: <br>
 * <ul>
 * <li>
 * <b>INFILE</b>: path or name of the output from Gaussian 
 * (i.e. name.out).
 * </li>
 * <li>
 * <b>NEWINPFILE</b>:  path or name of the new input file (i.e, 
 * <code>name.inp</code>) to be generated for Gaussian. The input will
 *  consider that a checkpoint file with the very same root name 
 * (i.e., <code>name.chk</code>) is to be used. This means that a copy
 *  of the old checkpoint file has to be made and placed properly 
 * before the new <code>inp</code> is submitted to Gaussian. 
 * </li>
 * <li>
 * <b>JOBDETAILSFILE</b>:  text file defining all the details 
 * of  the Gaussian job originally submitted to Gaussian. In alternative,
 * use <b>JOBDETAILS</b>:
 * </li> 
 * <li>
 * <b>GAUSSIANERRORS</b> path to the folder storing Gaussian known errors.
 *  </li> 
 * <li>
 * <b>VERBOSITY</b> verbosity level.
 * </li>
 * </ul>
 * These parameters can be provided as {@link ParameterStorage}. In
 *  alternative, the objects (name as a String, job details as 
 * {@link GaussianJob}, and errors as an array of {@link ErrorMessage})
 *  can be provided directly in the constructor of a new 
 * <code>GaussianReStarter</code>.
 *
 * @author Marco Foscato
 */


public class GaussianReStarter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
                    Collections.unmodifiableSet(new HashSet<TaskID>(
                                    Arrays.asList(TaskID.FIXANDRESTARTGAUSSIAN)));
	
    /**
     * Name of the Gaussian output file (the input for this class)
     */
    private String inFile;

    /**
     * Name of the new Gaussian input file (the output of this class)
     */
    private String inpFile;

    /**
     * Name of the new jobDetails file
     */
    private String newJDFile;

    /**
     * Name of the checkpoint file
     */
    private String checkPointName;

    /**
     * Specification of the gaussian job
     */
    private GaussianJob gaussJob;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

    /**
     * Storage for parameters used to construct this object
     */
    private ParameterStorage paramsLoc;


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty restarter
     */
/*
    public GaussianReStarter() 
    {
    }
*/
    

    /**
     * Constructor.
     */
    public GaussianReStarter()
    {
        super("inputdefinition/todo.json");
    }

//------------------------------------------------------------------------------

    /**
     * Constructor specifying the all parameters in a single 
     * <code>ParameterStorage</code>. The following parameters
     * are required:
     * <ul>
     * <li>
     * <b>INFILE</b>: path or name of the output from Gaussian 
     * (i.e. name.out).
     * </li>
     * <li>
     * <b>NEWINPFILE</b>:  path or name of the new input file (i.e, 
     * <code>name.inp</code>) to be generated for Gaussian. The input will
     *  consider that a checkpoint file with the very same root name 
     * (i.e., <code>name.chk</code>) is to be used. This means that a copy
     *  of the old checkpoint file has to be made and placed properly 
     * before the new <code>inp</code> is submitted to Gaussian. 
     * </li>
     * <li>
     * <b>JOBDETAILS</b>:  f text file defining all the details 
     * of  the Gaussian job originally submitted to Gaussian.
     * </li> 
     * <li>
     * <b>GAUSSIANERRORS</b> path to the folder storing Gaussian known errors.
     *  </li> 
     * <li>
     * <b>VERBOSITY</b> verbosity level.
     * </li>
     * </ul>
     * 
     * @param params object <code>ParameterStorage</code> containing all the
     * parameters needed
     */
/*
    public GaussianReStarter(ParameterStorage params) 
    {
    	*/
//-----------------------------------------------------------------------------

	/**
	 * Initialise the worker according to the parameters loaded by constructor.
	 */

    @Override
    public void initialize()
    {
        //Keep track of the parameter's object
        paramsLoc = params;

        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to GaussianReStarter");

        //Get and check the input file (which is an output from G03/09)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the output file (which is an input from G03/09)
        this.inpFile = params.getParameter("NEWINPFILE").getValue().toString();
        FileUtils.mustNotExist(this.inpFile);
        checkPointName = FileUtils.getRootOfFileName(this.inpFile);
        newJDFile = FileUtils.getRootOfFileName(this.inpFile) + ".jd";

        if (params.contains(ChemSoftConstants.PARJOBDETAILSFILE))
        {
            String jdFile = params.getParameter(
            		ChemSoftConstants.PARJOBDETAILSFILE).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from JD file '" 
                		+ jdFile + "'.");
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.gaussJob = new GaussianJob(jdFile);
        }
        else if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdLines = params.getParameter(
            		ChemSoftConstants.PARJOBDETAILS).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from nested parameter block.");
            }
            ArrayList<String> lines = new ArrayList<String>(Arrays.asList(
            		jdLines.split("\\r?\\n")));
            this.gaussJob = new GaussianJob(lines);
        }
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
            		+ "Neither '" + ChemSoftConstants.PARJOBDETAILSFILE
            		+ "' nor '" + ChemSoftConstants.PARJOBDETAILS 
            		+ "'found in parameters.",-1);
        }
    }
    
//------------------------------------------------------------------------------
    
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
        case FIXANDRESTARTGAUSSIAN:
        	restartJobWithErrorFix();
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
     * Creates a new input file for Gaussian. The new input is generated 
     * according to the error-solving protocol defined in the 
     * {@link ErrorMessage}
     * object that represents an unsuccessful, but known, 
     * outcome of a Gaussian job.
     * <p>
     * Follows the list of implemented <b>Error Fixing Actions</b>
     * that can be provided invoked from the definition of an
     *  {@link ErrorMessage}:
     * <ul>
     * <li> 
     * <b>REDO_STEP_ALTERING_LINK_0_SECTION</b> prepares an input that aims to
     * redo the failing step, and all the following steps,
     * but with a modified the Link 0 Section (see
     * <a href="http://www.gaussian.com/g_tech/g_ur/k_link0.htm">Link0</a>).
     * In case the failing step was the very first one, the original input 
     * geometry, that is the one provided to the job that did not terminate
     * successfully, is used to prepare the new input for Gaussian. Otherwise
     * checkpoint file is used. Follows the list of details that can be provided
     * into an {@link ErrorMessage} when invoking this Error Fixing Actions:
     *    <ul>
     *    <li> 
     *    <i>REDUCE_MEMORY_DEMADS</i> use this keywords with value like 1000MB
     *                                or 1GB (in general [integer][units]) to
     *                                require the reduction of the memory 
     *                                allocated by Gaussian (%mem in Gaussian 
     *                                input file). This will set the amount of 
     *                                dynamic memory of all steps to a value
     *                                that is the original reduced by the
     *                                reduction specified with this keyword.
     *    </li>
     *    </ul>
     * </li>
     * <li> 
     * <b>REDO_STEP_ALTERING_ROUTE_OPTION</b> prepares an input that aims to
     * redo the failing step but with a modified Route Section (see
     * <a href="http://www.gaussian.com/g_tech/g_ur/m_input.htm">Route</a>).
     * All following steps of a multi step {@link GaussianJob} will not be 
     * altered. The geometry and wave function of the restarting job are taken
     * from the checkpoint file which is expected to have the same name of the
     * new input file but different extension. 
     * Follows the list of details that can be provided
     * into an {@link ErrorMessage} when invoking this Error Fixing Actions:
     *    <ul>
     *    <li> 
     *    <i>IMPOSE_ROUTE_PARAMS</i> use this keywords to provide list of
     *                               keyword:value pairs that have to be 
     *                               added or changes (if already present) in
     *                               the Route Section of the new first step.
     *                               Multiline labeled blocks are suitable
     *                               for providing more than one keyword:value 
     *                               pair.
     *    </li>
     *    </ul>
     * </li>
     * <li> 
     * <b>REDO_STEP_AFTER_EXTRA_STEPS_KEEPING_SELECTED_OPTIONS</b> prepares
     * an input that aims to redo the failing step only after an additional
     * step has been performed and successfully completed. Part of the
     * Route Section of the failing step is used to define the Route Section
     * of the additional step (i.e., model chemistry and basis set).
     * Geometry and wave function of the restarting job are taken
     * from the checkpoint file which is expected to have the same name of the
     * new input file but different extension.
     * Follows the list of details that can be provided
     * into an {@link ErrorMessage} when invoking this Error Fixing Actions:
     *    <ul>
     *    <li> 
     *    <i>EXTRA_STEP</i> use this keywords to provide the detail of the
     *                      additional step. Format of this multiline 
     *                      (labeled) block same as for {@link GaussianStep}.
     *    </li>
     *    <li> 
     *    <i>KEEP_OPTIONS</i> use this keywords to specify which of the 
     *                      parameters set in the original Route Section
     *                      has to be overwritten, or added if missing, in the
     *                      additional step.
     *    </li>
     *    </ul>
     * </li>
     * <li> 
     * <b>SKIP_STEP</b> prepares an input that aims to skip the failing step
     * and restart the {@link GaussianJob} from the next {@link GaussianStep}.
     * Geometry and wave function of the restarting job are taken
     * from the checkpoint file which is expected to have the same name of the
     * new input file but different extension.
     * </li>
     * </ul>
     */

    public void restartJobWithErrorFix()
    {
    	// We take most of the parameters of the present worker
    	ParameterStorage paramsForOutputHandler = paramsLoc.clone();
    	paramsForOutputHandler.setParameter(
    			WorkerConstants.PARTASK, "EVALUATEGAUSSIANOUTPUT");
    	
        //Gather information on the error job
    	Worker w = WorkerFactory.createWorker(paramsForOutputHandler, 
    			this.getMyJob());
        GaussianOutputHandler oEval = (GaussianOutputHandler) w;
                                                  
        oEval.evaluateGaussianOutput();
        if (!oEval.isErrorUnderstood())
        {
            Terminator.withMsgAndStatus("ERROR! Gaussian Error Message not "
                        + "understood.\nSorry, I cannot restart the job "
                        + "without understanding what the problemi is.",-1);
        }
        ErrorMessage em = oEval.getErrorMessage();
        int failedStepID = oEval.getNumberOfSteps();

        //identify the type of error-solving protocol and get the details
        String typeOfAction = em.getErrorFixingAction();
        typeOfAction = typeOfAction.toUpperCase();
        Map<String,String> details = em.getErrorFixingActionDetails();
        //Check for consistency between reality and user's request
        if (typeOfAction.equals("NONE"))
        {
            Terminator.withMsgAndStatus("ERROR! Action specified in case of "
                                        + em.getName() + " is 'NONE'", -1);
        }
        if ((typeOfAction == null) || (typeOfAction.equals("")))
        {
            Terminator.withMsgAndStatus("ERROR! No error-fix action defined "
                                        + "for error " + em.getName() +".",-1);
        }

        if (verbosity > 0)
            System.out.println(" Action to fix the problem: "+typeOfAction);        
        //Identify the failing step
        //WARNING! Use failedStepID -1 because oEval.getNumberOfSteps()
        //works as a size() method and returns the number of steps as
        // from 1 to n (not from 0 to n-1)

        GaussianStep failStep = gaussJob.getStep(failedStepID - 1);

        //define the new GaussianJob
        GaussianJob newGJob = new GaussianJob();
        switch (typeOfAction) {
            case "REDO_STEP_ALTERING_LINK_0_SECTION":
            {
                //Append the failing step and all the steps that never run
                for (int i=(failedStepID - 1); i<gaussJob.getNumberOfSteps(); 
                                                                            i++)
                {
                    //Copy from gaussJob to newGJob
                    GaussianStep copyOfOldStep = new GaussianStep(
                                        gaussJob.getStep(i).toLinesJob());
                    //Store step
                    newGJob.addStep(copyOfOldStep);
                }

                //Update Name of Checkpoint file in all steps
                newGJob.setAllLinkSections("CHK",checkPointName);

                //Modify all Link 0 Commands sections

                // 1: Reduce memory requirements
                if (details.keySet().contains("REDUCE_MEMORY_DEMADS"))
                {
                    String mRedStr = details.get("REDUCE_MEMORY_DEMADS");
                    mRedStr = mRedStr.trim();
                    mRedStr = mRedStr.toUpperCase();
                    double mRedVal = Double.parseDouble(
                                        mRedStr.replaceAll("[A-Z]+",""));
                    String mRedUnt = mRedStr.replaceAll("[0-9]+","");
                    switch (mRedUnt) {
                        case "MB":
                        {
                            break;
                        }
                        case "GB":
                        {
                            mRedVal = mRedVal * 1000;
                            break;
                        }
                        default:
                             Terminator.withMsgAndStatus("ERROR! Memory must be"
                            + " reported in MB or GB. Unable to understand '"
                            + mRedUnt + "'.",-1);
                    }

                    String mOldStr = "16000MB";
                    if (failStep.getLinkCommand().hasKey("MEM"))
                    {
                        mOldStr = failStep.getLinkCommand().getValue("MEM");
                    }
                    mOldStr = mOldStr.trim();
                    mOldStr = mOldStr.toUpperCase();
                    double mOldVal = Double.parseDouble(
                                        mOldStr.replaceAll("[a-zA-Z]+",""));
                    String mOldUnt = mOldStr.replaceAll("[0-9]+","");
                    switch (mOldUnt) {
                        case "MB":
                        {
                            break;
                        }       
                        case "GB":
                        {
                            mOldVal = mOldVal * 1000;
                            break;
                        }
                        default:
                            Terminator.withMsgAndStatus("ERROR! Memory must be"
                            + " reported in MB or GB. Unable to understand '"
                            + mOldUnt + "'.",-1);
                    }

                    double mNewVal = mOldVal - mRedVal;
                    String mNewStr = String.format(Locale.ENGLISH,
                    		"%.0f",mNewVal) + "MB";
                    newGJob.setAllLinkSections("MEM",mNewStr);
                }

                // 2: TODO add other modifications to Link 0 section here

                //Update Molecular specification
                GaussianStep firstStep = newGJob.getStep(0);
                GaussianMolSpecification firstMolSpec = firstStep.getMolSpec();

                //Update Charge and spin in all steps
                int charge = firstMolSpec.getCharge();
                int spinMult = firstMolSpec.getSpinMultiplicity();
                newGJob.setAllCharge(charge);
                newGJob.setAllSpinMultiplicity(spinMult);

                //Get Molecular representation section (chk or out)
                if (!firstStep.getRouteSection().keySet().contains(
                        GaussianConstants.LABLOUDKEY + "GEOM"))
                {
                    IAtomContainer initGeom = oEval.getInitialGeometry();
                    for (int i=0; i<initGeom.getAtomCount(); i++)
                    {
                        IAtom atm = initGeom.getAtom(i);
                        String line = atm.getSymbol()    + "   " 
                                    + atm.getPoint3d().x + "   " 
                                    + atm.getPoint3d().y + "   "
                                    + atm.getPoint3d().z + "   ";
                        firstMolSpec.addPart(line);
                    }
                }

                //Write the new INP file
                IOtools.writeTXTAppend(inpFile,newGJob.toLinesInp(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newGJob.toLinesJob(),false);

                break;
            }
            case "REDO_STEP_ALTERING_ROUTE_OPTION":
            {
                //Append the failing step and all the steps that never run
                boolean first = true;
                for (int i=(failedStepID - 1); i<gaussJob.getNumberOfSteps(); 
                                                                            i++)
                {
                    //Copy from gaussJob to newGJob
                    GaussianStep copyOfOldStep = new GaussianStep(
                                        gaussJob.getStep(i).toLinesJob());
                    //Modify keywords and opts in route section of failed step
                    if (first && details.keySet().contains(
                                                         "IMPOSE_ROUTE_PARAMS"))
                    {
                        //Get the route section of this step
                        GaussianRouteSection route = 
                                                copyOfOldStep.getRouteSection();

                        //Get opts to add to first step
                        String optsAsString = details.get(
                                                         "IMPOSE_ROUTE_PARAMS");
                        String[] partsOfString = optsAsString.split("\n");
                        for (int ii=0; ii<partsOfString.length; ii++)
                        {
                            String line = partsOfString[ii];
                            line = line.toUpperCase();
                            if (line.startsWith(GaussianConstants.KEYTITLESEC))
                            {
                                String title = line.substring(
                                        GaussianConstants.KEYTITLESEC.length());

                                copyOfOldStep.setComment(title);
                            } 
                            else if (line.startsWith(
                                    GaussianConstants.KEYROUTESEC))
                            {

                                //Decode the content of the line
                                ArrayList<String> keyValue = 
                                        route.lineToKeyValuePair(line);

                                //Store
                                route.put(keyValue.get(0),keyValue.get(1));
                            }
                        }

                        //Deal with counters in title/comment
                        String comment = copyOfOldStep.getComment();
                        comment = updateCounter(comment,oEval);
                        copyOfOldStep.setComment(comment);

                        first = false;
                    }

                    //Store step
                    newGJob.addStep(copyOfOldStep);
                }

                //Update Name of Checkpoint file in all steps
                newGJob.setAllLinkSections("CHK",checkPointName);

                //Update Charge and spin in all steps
                GaussianStep firstStep = newGJob.getStep(0);
                int charge = firstStep.getMolSpec().getCharge();
                int spinMult = firstStep.getMolSpec().getSpinMultiplicity();
                newGJob.setAllCharge(charge);
                newGJob.setAllSpinMultiplicity(spinMult);

                //Write the new INP file
                IOtools.writeTXTAppend(inpFile,newGJob.toLinesInp(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newGJob.toLinesJob(),false);

                break;
            }
            case "REDO_STEP_AFTER_EXTRA_STEPS_KEEPING_SELECTED_OPTIONS":
            {

                //convert the details in a GaussianJob
                ArrayList<String> extraGJobAsLines = new ArrayList<String>();
                if (!details.keySet().contains("EXTRA_STEP"))
                {
                    Terminator.withMsgAndStatus("ERROR! Option 'EXTRA_STEP' "
                        + "not found in " + em.getName()
                        + ".\nError-fixing action '" 
                        + typeOfAction + "' requires the definition of an "
                        + "additional job under the option 'EXTRA_STEP'.",-1);
                }
                String extraGJobAsString = details.get("EXTRA_STEP");
                String[] partsOfString = extraGJobAsString.split("\n");
                for (int i=0; i<partsOfString.length; i++)
                {
                    extraGJobAsLines.add(partsOfString[i]);
                }
                GaussianJob extraGJob = new GaussianJob(extraGJobAsLines);

                //Start building the new steps from the details specified in the
                // definition of the error
                for (int i=0; i<extraGJob.getNumberOfSteps(); i++)
                {

                    //Start building the extra step
                    GaussianStep extraStep = new GaussianStep(
                                extraGJob.getStep(i).toLinesJob());

                    //Store steo in the new gaussian gob
                    newGJob.addStep(extraStep);
                }

                //To modify the added steps, first get list of options to keep
                if (!details.keySet().contains("KEEP_OPTIONS"))
                {
                    Terminator.withMsgAndStatus("ERROR! Option 'KEEP_OPTIONS' "
                        + "not found in " + em.getName()
                        + ".\nError-fixing action '"
                        + typeOfAction + "' requires the definition of "
                        + "options (to be taken from the old, failing step) "
                        + "under the keyword 'KEEP_OPTIONS'.",-1);
                }
                String optsToKeepAsString = details.get("KEEP_OPTIONS");
                String[] partsOfOpts = optsToKeepAsString.split("\n");
                for (int i=0; i<partsOfOpts.length; i++)
                {
                    String line = partsOfOpts[i];
                    line = line.toUpperCase();
                    if (line.startsWith(GaussianConstants.KEYLINKSEC))
                    {
                        //Keep portions of the Link 0 section
                        String keyToKeep = line.substring(
                                         GaussianConstants.KEYLINKSEC.length());
                        GaussianLinkCommandsSection oriLnk = 
                                                      failStep.getLinkCommand();
                        //Check that we actually have this parameter
                        if (oriLnk.keySet().contains(keyToKeep))
                        {
                            //Apply on all additional steps
                            for (int j=0; j<extraGJob.getNumberOfSteps(); j++)
                            {
                                GaussianStep es = newGJob.getStep(j);
                                GaussianLinkCommandsSection esLnk =
                                                            es.getLinkCommand();
                                esLnk.setValue(keyToKeep,oriLnk.getValue(
                                                                    keyToKeep));
                            }
                        } else {
                            System.out.println("WARNING! Cannot keep "
                                + "option '" + keyToKeep + "' from Link 0 "
                                + "Section. Option is not found in previous"
                                + "job detail's file and will not be added!");
                        }

                        //Apply on all additional steps
                        for (int j=0; j<extraGJob.getNumberOfSteps(); j++)
                        {
                            GaussianStep es = newGJob.getStep(j);
                            GaussianLinkCommandsSection esLnk = 
                                                            es.getLinkCommand();
                            esLnk.setValue(keyToKeep,oriLnk.getValue(
                                                                    keyToKeep));
                        }

                    } else if (line.startsWith(GaussianConstants.KEYROUTESEC)) {
                        //Keep portions of the Route Section
                        String keyToKeep = line.substring(
                                        GaussianConstants.KEYROUTESEC.length());
                        GaussianRouteSection oriRoute = 
                                                     failStep.getRouteSection();
                        //Check that we actually have this parameter
                        if (oriRoute.keySet().contains(keyToKeep))
                        {
                            //Apply on all additional steps
                            for (int j=0; j<extraGJob.getNumberOfSteps(); j++)
                            {
                                GaussianStep es = newGJob.getStep(j);
                                GaussianRouteSection esRoute = 
                                                           es.getRouteSection();
                                esRoute.put(keyToKeep,oriRoute.getValue(
                                                                    keyToKeep));
                            }

                        } else {
                            System.out.println("WARNING! Cannot keep "
                                + "option '" + keyToKeep + "' from Route "
                                + "Section. Option is not found in previous"
                                + "job detail's file and will not be added!");
                        }

                    } else if (line.startsWith(GaussianConstants.KEYMOLSEC)) {
                        //Keep portions of the Molecular Specification
                        String keyToKeep = line.substring(
                                         GaussianConstants.KEYMOLSEC.length());
                        GaussianMolSpecification oriMol = failStep.getMolSpec();
                        
                        //No need to check: MolSpec doesn't work with keys
                        
                        //Apply on all additional steps
                        for (int j=0; j<extraGJob.getNumberOfSteps(); j++)
                        {
                            GaussianStep es = newGJob.getStep(j);
                            GaussianMolSpecification esMol = es.getMolSpec();
                            if (keyToKeep.toUpperCase().equals("CHARGE"))
                            {
                                esMol.setCharge(oriMol.getCharge());
                            } else if (keyToKeep.toUpperCase().equals("SPIN")) {
                                esMol.setSpinMultiplicity(
                                        oriMol.getSpinMultiplicity());
                            }
                        }
                        
                    } else if (line.startsWith(GaussianConstants.KEYOPTSSEC)) {
                        //Keep portions of an Option Section
                        Terminator.withMsgAndStatus("ERROR! Treatment of Option"
                                + "Sections in connection with " + typeOfAction
                                + "' not implemented yet.",-1);
                    } else {
                        Terminator.withMsgAndStatus("ERROR! Unclear statement "
                                + "in the file defining error " + em.getName()
                                + ".\nI don't know "
                                + "what to do with line: " + line, -1);
                    }
                } //End of loop over parts of "KEEP_OPTIONS"

                //Append the failing step and all the steps that never run
                for (int i=(failedStepID - 1); i<gaussJob.getNumberOfSteps(); 
                                                                            i++)
                {
                    //Copy from gaussJob to newGJob
                    GaussianStep copyOfOldStep = new GaussianStep(
                                        gaussJob.getStep(i).toLinesJob());
                    newGJob.addStep(copyOfOldStep);
                }

                //Update Name of Checkpoint file in all steps
                newGJob.setAllLinkSections("CHK",checkPointName);

                //Update Charge and spin in all steps
                GaussianStep firstStep = newGJob.getStep(0);
                int charge = firstStep.getMolSpec().getCharge();
                int spinMult = firstStep.getMolSpec().getSpinMultiplicity();
                newGJob.setAllCharge(charge);
                newGJob.setAllSpinMultiplicity(spinMult);

                //Write the new INP file
                IOtools.writeTXTAppend(inpFile,newGJob.toLinesInp(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newGJob.toLinesJob(),false);
               
                break;
            }
            case "SKIP_STEP":
            {
                
                //Copy only the steps after the failing one
                //WARNING! failedStepID goes from 1 to n (not from 0 to n-1)
                for (int i=failedStepID; i<gaussJob.getNumberOfSteps(); i++)
                {
                    //Copy from gaussJob to newGJob
                    GaussianStep copyOfOldStep = new GaussianStep(
                                        gaussJob.getStep(i).toLinesJob());
                    newGJob.addStep(copyOfOldStep);
                }

                //Update Name of Checkpoint file in all steps
                newGJob.setAllLinkSections("CHK",checkPointName);

                //Update Charge and spin in all steps
                GaussianStep firstStep = newGJob.getStep(0);
                int charge = firstStep.getMolSpec().getCharge();
                int spinMult = firstStep.getMolSpec().getSpinMultiplicity();
                newGJob.setAllCharge(charge);
                newGJob.setAllSpinMultiplicity(spinMult);

                //Write the new INP file
                IOtools.writeTXTAppend(inpFile,newGJob.toLinesInp(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newGJob.toLinesJob(),false);

                break;
            }

/*
            case "":
            {
                e
                System.out.println("");

                break;
            }
*/

            default:
                 Terminator.withMsgAndStatus("ERROR! Type of action " 
                        + typeOfAction + " not recosnized.", -1);
        }
                

        //prepare the new input file

    }

//------------------------------------------------------------------------------

    /**
     * Update the value of a counter in the comment of a gaussian job using the
     * value given as an argument
     * @return the new title with the updated value of the counter
     */

    private String updateCounter(String title, GaussianOutputHandler oEval) 
    {
        Map<String,Integer> counters = oEval.getCounters();
        for (String counterName : counters.keySet())
        {
            //Prepare the new title/comment with the new value of the counter
            int newValue = counters.get(counterName) + 1;
            String newTitle = counterName +"="+ newValue +" ";

            //Add all the rest of the title section
            String[] words = title.split("\\s+");
            for (int iw=0; iw<words.length; iw++)
            {
                String w = words[iw];
                if (!w.contains(counterName))
                {
                    newTitle = newTitle + w + " ";
                }
            }
        
            title = newTitle;
        }
        return title;
    }

//------------------------------------------------------------------------------

}
