package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.errorhandling.ErrorManager;
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
 * Restarts a NWChem job that returned an error. The tool evaluates
 * the outcome of a NWChem job and, after detection and 
 * identification of the error message, generates a new input file
 * with settings that may allow the calculation to avoid/fix the
 * error and terminate properly. The errors and corresponding 
 * error fixing actions are defined in a database provided by the 
 * user. 
 * The execution of the this tool requires the following 
 * parameters: <br>
 * <ul>
 * <li>
 * <b>INFILE</b>: path or name of the output from NWChem 
 * (i.e. <code>name.out</code>).
 * </li>
 * <li>
 * <b>NEWNWFILE</b>:  path or name of the new input file (i.e, 
 * <code>name.nw</code>) to be generated for NWChem. The input will
 *  consider that a database file with the very same root name 
 * (i.e., <code>name.db</code>) is to be used. This means that a copy
 *  of the old database file has to be made and placed properly 
 * before the new <code>name.nw</code> is submitted to NWChem. 
 * </li>
 * <li>
 * <b>JOBDETAILSFILE</b>: formatted text file defining all the details 
 * of the NWChem job originally submitted to NWChem. In alternative use
 * <b>JOBDETAILS</b> to provide the job details in a nested block of text
 * </li> 
 * <li>
 * (optional)<b>NWCHEMERRORS</b> path to the folder storing NWChem known errors.
 * If this parameter is not defined, the software will only try to redo the
 * last task.
 * The format of the files defining errors described in 
 * {@link ErrorMessage}'s documentation. Acceptable error-fixing actions
 * are listed in the documentation of the {@link #restartJobWithErrorFix}
 * method.
 *  </li> 
 * <li>
 * <b>VERBOSITY</b> verbosity level.
 * </li>
 * </ul>
 * In addition, the following parameters are optional:
 * <ul>
 * <li>
 * (optional) <b>NWCDATABASE</b> name of NWChem's database file that hold
 * runtime settings and data of an NWChem job.
 * </li>
 * </ul>
 * These parameters can be provided as {@link ParameterStorage}. In
 *  alternative, the objects (name as a String, job details as 
 * {@link NWChemJob}, and errors as an array of {@link ErrorMessage})
 *  can be provided directly in the constructor of a new 
 * <code>NWChemReStarter</code>.
 *
 * @author Marco Foscato
 */


public class NWChemReStarter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.FIXANDRESTARTNWCHEM)));

    /**
     * Name of the .out file from NWChem (i.e., the input for this class)
     */
    private String inFile;

    /**
     * Name of the new .nw file (i.e., output of this class)
     */
    private String nwFile;

    /**
     * Name of the new jobDetails file
     */
    private String newJDFile;

    /**
     * Name of the database file
     */
    private String nwcDataBaseName;

    /**
     * Name of the jobdetails file
     */
    private NWChemJob nwcJob;

    /**
     * Name of the errors definition tree
     */
    @SuppressWarnings("unused")
        private List<ErrorMessage> errorDef;

    /**
     * Flag requiring to restart last task
     */
    private boolean justRestartLastTask = false;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

    //Storage for parameters used to construct this object
    private ParameterStorage paramsLoc;

//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public NWChemReStarter()
    {
        super("inputdefinition/todo.json");
    }

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
            System.out.println(" Adding parameters to NWChemReStarter");

        //Get and check the input file (which is an output from NWChem)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the output file (which is an input for NWChem)
        this.nwFile = params.getParameter("NEWNWFILE").getValue().toString();
        FileUtils.mustNotExist(this.nwFile);
        this.nwcDataBaseName = FileUtils.getRootOfFileName(this.nwFile) 
                                                                        + ".db";
        this.newJDFile = FileUtils.getRootOfFileName(this.nwFile) + ".jd";

        //Get and check the output file (which is an input for NWChem)
        if (params.contains("NWCDATABASE"))
        {
            this.nwcDataBaseName = params.getParameter(
                                           "NWCDATABASE").getValue().toString();
        }

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
            this.nwcJob = new NWChemJob(jdFile);
        }
        else if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdLines = params.getParameter(
            		ChemSoftConstants.PARJOBDETAILS).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from nested parameter block.");
            }
            List<String> lines = new ArrayList<String>(Arrays.asList(
            		jdLines.split("\\r?\\n")));
            this.nwcJob = new NWChemJob(lines);
        }
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
            		+ "Neither '" + ChemSoftConstants.PARJOBDETAILSFILE
            		+ "' nor '" + ChemSoftConstants.PARJOBDETAILS 
            		+ "'found in parameters.",-1);
        }
        
        //Get and check the list of known errors
        if (params.contains("NWCHEMERRORS"))
        {
            String errDefPath = 
                params.getParameter("NWCHEMERRORS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing known errors from " 
                                                                  + errDefPath);
            }
            FileUtils.foundAndPermissions(errDefPath,true,false,false);
            this.errorDef = ErrorManager.getAll(errDefPath);
        }
        else
        {
            this.justRestartLastTask = true;
            if (verbosity > 0)
            {
                System.out.println(" WARNING! No list of known errors.");
                System.out.println(" I can only restart the last NWChem task.");
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
          case FIXANDRESTARTNWCHEM:
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

    //TODO move to class doc
    /**
     * Creates a new input file for NWChem. The new input is generated 
     * according to the error-solving protocol defined in the 
     * {@link ErrorMessage}
     * object that represents an unsuccessful, but known, 
     * outcome of a NWChem job.
     * <p>
     * Follows the list of implemented <b>Error Fixing Actions</b>
     * that can be provided invoked from the definition of an
     *  {@link ErrorMessage}:
     * (TODO: update)
     * <ul>
     * <li> 
     * <b>REDO_STEP_ALTERING_DIRECTIVES</b> prepares a new input that aims to
     * redo the failing step, and all the following steps,
     * but with one or more edits in the list of directives.
     * Follows the list of details that can be provided
     * into an {@link ErrorMessage} when invoking this Error Fixing Actions:
     *    <ul>
     *    <li> 
     *    <i>IMPOSE_DIRECTIVES</i> use this keywords to provide a list of
     *    directives that will be added, or overwritten, in the first task 
     *    of the new job. Multiline blocks can be used to define 
     *    directives/keywords/data blocks. For this purpose use 
     *    {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABOPENBLOCK}
     *    and 
     *    {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABCLOSEBLOCK}.
     *    </li>
     *    <li>
     *    <i>REMOVE_DIRECTIVES</i> use this keywords to provide a list of
     *    directives that, if present in the failing step, must be removed
     *    from the first task of the job. Only the innermost directive is
     *    removed.
     *    Multiline blocks can be used to define multiple directives.
     *    For this purpose use
     *    {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABOPENBLOCK}
     *    and
     *    {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABCLOSEBLOCK}.
     *    </li>
     *    </ul>
     * </li>
     * <li> 
     * <b>REDO_STEP_AFTER_EXTRA_STEPS_KEEPING_SELECTED_OPTIONS</b> prepares
     * an input that aims to redo the failing step only after an additional
     * step has been performed and successfully completed. Part of the
     * settings of the failing step is used to define the
     * additional step (i.e., model chemistry and basis set).
     * Current geometry, wave function
     *  and other volatile information (i.e., not specifies in
     * the jobdetails file), but also other information stored in the NWChem 
     * database (i.e., the set of active atoms),
     * will be reused as from the provided database file
     * (i.e., name.db), which is expected to have the same name of the
     * new input file but different extension. 
     * This, unless settings are ovewrritten in the directives of the 
     * EXTRA_STEP.
     * Follows the list of details that can be provided
     * into an {@link ErrorMessage} when invoking this Error Fixing Actions:
     *    <ul>
     *    <li> 
     *    <i>EXTRA_STEP</i> use this keywords to provide the detail of the
     *                      additional step. Format of this multi line 
     *                      (labeled) block same as for {@link NWChemTask}.
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
     * and restart the {@link NWChemJob} from the next {@link NWChemTask}.
     * Current geometry, wave function, and other volatile information (i.e., 
     * which is not specified in the jobdetails file), 
     * but also other information stored in the NWChem
     * database (i.e., the set of active atoms), is taken 
     * from the database file which is expected to have the same name of the
     * new input file but different extension.
     * </li>
     * <!--
     * <li> 
     * <b></b> 
     * </li>
     * -->
     * </ul>
     */

    public void restartJobWithErrorFix()
    {
    	// We take most of the parameters of the present worker
    	ParameterStorage paramsForOutputHandler = paramsLoc.clone();
    	paramsForOutputHandler.setParameter(WorkerConstants.PARTASK,
    			"EVALUATENWCHEMOUTPUT");
    	
        //Gather information on the error job
    	Worker w = WorkerFactory.createWorker(paramsForOutputHandler, 
    			this.getMyJob());
    	NWChemOutputHandler oEval = (NWChemOutputHandler) w;
    	
                                                  
        oEval.evaluateOutputNWChem();
        if (!oEval.isErrorUnderstood() && !justRestartLastTask)
        {
            Terminator.withMsgAndStatus("ERROR! NWChem Error Message not "
                        + "understood.\nSorry, I cannot restart the job "
                        + "without understanding what the problem is.",-1);
        }
        ErrorMessage em = oEval.getErrorMessage();
        int failedStepID = oEval.getNumberOfSteps();

        //identify the type of error-solving protocol and get the details
        String typeOfAction = "";
        Map<String,String> efaDetails = new HashMap<String,String>();
        if (!justRestartLastTask)
        {
            typeOfAction = em.getErrorFixingAction();
            typeOfAction = typeOfAction.toUpperCase();
            efaDetails = em.getErrorFixingActionDetails();
        }
        //Check for consistency between reality and user's request
        if (typeOfAction.equals("NONE"))
        {
            Terminator.withMsgAndStatus("ERROR! Action specified in case of "
                                        + em.getName() + " is 'NONE'", -1);
        }
        if (justRestartLastTask)
        {
            typeOfAction = "REDO_STEP_ALTERING_DIRECTIVES";
        }
        if ((typeOfAction == null) || (typeOfAction.equals("")))
        {
            Terminator.withMsgAndStatus("ERROR! No error-fix action "
                       + "defined for error " + em.getName() +".",-1);
        }

        if (verbosity > 0)
        {
            System.out.println(" Action to fix the problem: "+typeOfAction);
        }

        //Identification of the failing step
        //
        // WARNING! Use failedStepID-1 because oEval.getNumberOfSteps()
        // works as a size() method and the number of steps as
        // from 1 to n (not from 0 to n-1)
        //
        NWChemTask failStep = nwcJob.getStep(failedStepID - 1);

        //define the new NWChemJob
        NWChemJob newNwcJob = new NWChemJob();
        switch (typeOfAction) {
            case "REDO_STEP_ALTERING_DIRECTIVES":
            {
                //Append the failing step and all the steps that never run
                boolean first = true;
                boolean wasStartDirImposed = false;
                boolean wasRestartDirImposed = false;
                for (int i=(failedStepID - 1); i<nwcJob.getNumberOfSteps(); i++)
                {
                    if (verbosity > 1)
                    {
                        System.out.println(" Setting step "+i+" of new job");
                    }

                    //Copy from nwcJob to newNwcJob
                    NWChemTask copyOfOldStep = new NWChemTask(
                                        nwcJob.getStep(i).toLinesJobDetails());


/*
                    if (verbosity > 2 && first)
                    {
                        System.out.println(" Directives from old jobdetails:");
                        for (NWChemDirective duD : copyOfOldStep.getAllDirectives())
                        {
                            System.out.println("  "+duD.getName());
                        }
                    }
*/

                    if (first)
                    {
                        //By default we replace the START with RESTART
                        //but this result can be overwritten by the
                        //remove/impose mechanism
                        copyOfOldStep.deleteDirective(
                              new ArrayList<String>(),NWChemConstants.STARTDIR);
                        NWChemDirective newRestartDir = new NWChemDirective(
                                                    NWChemConstants.RESTARTDIR);
                        copyOfOldStep.setDirective(
                          new ArrayList<String>(),newRestartDir,true,true,true);
                    }

                    //Modify directives of failed step, which is the first in
                    //the new NWChemJob
                    if (first 
                        && efaDetails.keySet().contains("IMPOSE_DIRECTIVES"))
                    {
                        //Get directives to impose
                        String dirsOneStr = efaDetails.get("IMPOSE_DIRECTIVES");
                        List<String> dirsAsLines = new ArrayList<String>(
                                         Arrays.asList(dirsOneStr.split("\n")));

                        NWChemTask dirsToAdd = new NWChemTask(dirsAsLines);
                        copyOfOldStep.impose(dirsToAdd);

                        if (verbosity > 2)
                        {
                            System.out.println(" Imposing directives:");
                            for (NWChemDirective duD : dirsToAdd.getAllDirectives())
                            {
                                System.out.println("  "+duD.getName());
                            }
                        }

                        //Deal with counters in title/comment
                        String title = copyOfOldStep.getTitle();
                        title = updateCounter(title,oEval);
                        copyOfOldStep.setTitle(title);

                        if (dirsToAdd.hasDirective(new ArrayList<String>(),
                                         new NWChemDirective(NWChemConstants.STARTDIR)))
                        {
                            wasStartDirImposed=true;
                        }
                        if (dirsToAdd.hasDirective(new ArrayList<String>(),
                                         new NWChemDirective(NWChemConstants.RESTARTDIR)))
                        {
                            wasRestartDirImposed=true;
                        }
                    }

                    if (first
                        && efaDetails.keySet().contains("REMOVE_DIRECTIVES"))
                    {
                        //Get directives to delete
                        String dirsOneStr = efaDetails.get("REMOVE_DIRECTIVES");
                        List<String> dirsAsLines = new ArrayList<String>(
                                         Arrays.asList(dirsOneStr.split("\n")));
                        NWChemTask dirsToRemove = new NWChemTask(dirsAsLines);
                        if (dirsToRemove.hasDirective(new ArrayList<String>(),
                                            new NWChemDirective(NWChemConstants.STARTDIR)))
                        {
                            wasStartDirImposed=true;
                        }
                        if (dirsToRemove.hasDirective(new ArrayList<String>(),
                                            new NWChemDirective(NWChemConstants.RESTARTDIR)))
                        {
                            wasRestartDirImposed=true;
                        }

                        if (verbosity > 2)
                        {
                            System.out.println(" Removing directives:");
                            for (NWChemDirective duD : dirsToRemove.getAllDirectives())
                            {
                                System.out.println("  "+duD.getName());
                            }
                        }
                        copyOfOldStep.delete(dirsToRemove);
                    }

                    if (first)
                    {
                        first = false;
                    }

                    //Store step
                    newNwcJob.addStep(copyOfOldStep);
                }

                //Add/Update RESTART directive
                if (!wasStartDirImposed)
                {
                    newNwcJob.getStep(0).deleteDirective(new ArrayList<String>(),
                                                      NWChemConstants.STARTDIR);
                }
                if (!wasRestartDirImposed && !wasStartDirImposed)
                {
                    NWChemDirective restartDir = new NWChemDirective(
                                                    NWChemConstants.RESTARTDIR);
                    String dbName = nwcDataBaseName;
                    if (nwcDataBaseName.endsWith(".db"))
                    {
                        dbName = dbName.substring(0,dbName.length()-3);
                    }
                    NWChemKeyword dbKey = new NWChemKeyword(
                                                       NWChemConstants.DBNAMEKW,
                                                                          false,
                                  new ArrayList<String>(Arrays.asList(dbName)));
                    restartDir.addKeyword(dbKey);
                    newNwcJob.getStep(0).setDirective(new ArrayList<String>(),
                                                     restartDir,true,true,true);
                }

                //Write the new input for NWChem
                IOtools.writeTXTAppend(nwFile,newNwcJob.toLinesInput(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newNwcJob.toLinesJobDetails(),
                                                                        false);

                break;
            }
            case "REDO_STEP_AFTER_EXTRA_STEPS_KEEPING_SELECTED_OPTIONS":
            {
                //convert the details in a NWChemJob
                if (!efaDetails.keySet().contains("EXTRA_STEP"))
                {
                    Terminator.withMsgAndStatus("ERROR! Option 'EXTRA_STEP' "
                        + "not found in " + em.getName()
                        + ".\nError Fixing Action '" 
                        + typeOfAction + "' requires the definition of an "
                        + "additional job under the option 'EXTRA_STEP'.",-1);
                }

                if (verbosity > 1)
                {
                    System.out.println(" Importing extra steps");
                }

                String extraNJobAsString = efaDetails.get("EXTRA_STEP");
                List<String> extraNJobAsLines = new ArrayList<String>(
                                  Arrays.asList(extraNJobAsString.split("\n")));
                NWChemJob extraNJob = new NWChemJob(extraNJobAsLines);

                //To modify the added steps, first get list of options to keep
                if (!efaDetails.keySet().contains("KEEP_OPTIONS"))
                {
                    Terminator.withMsgAndStatus("ERROR! Option 'KEEP_OPTIONS' "
                        + "not found in " + em.getName()
                        + ".\nError-fixing action '"
                        + typeOfAction + "' requires the definition of "
                        + "options (to be taken from the old, failing step) "
                        + "under the keyword 'KEEP_OPTIONS'. You usually want "
                        + "to keep least the type of task "
                        + "(i.e., scf, dft, etc.).",-1);
                }

                if (verbosity > 1)
                {
                    System.out.println(" Importing settings from old job");
                }

                String otkAsString = efaDetails.get("KEEP_OPTIONS");
                List<String> otkAsLines = new ArrayList<String>(
                                        Arrays.asList(otkAsString.split("\n")));
                NWChemTask otkMask = new NWChemTask(otkAsLines);

                NWChemTask otkTsk = failStep.extract(otkMask);

//TODO:del
/*
System.out.println("\nOTK-MASK:");
for (String l : otkMask.toLinesInput())
    System.out.println("M:  "+l);
for (String l : otkMask.toLinesJobDetails())
    System.out.println("Mj:  "+l);
System.out.println("\nOTK-TASK:");
for (String l : otkTsk.toLinesInput())
    System.out.println("T:  "+l);
for (String l : otkTsk.toLinesJobDetails())
    System.out.println("Tj:  "+l);
*/

                //Build the new job starting from the extra steps
                for (int i=0; i<extraNJob.getNumberOfSteps(); i++)
                {
                    NWChemTask extraStep = new NWChemTask(
                                extraNJob.getStep(i).toLinesJobDetails());

                    //Deal with title
                    String title = extraStep.getTitle();
                    title = title + " Additional step " + i 
                                 + " in attempt to fix: " + failStep.getTitle();
                    title = "\"" + title.replaceAll("\"","") + "\"";
                    extraStep.setTitle(title);

                    newNwcJob.addStep(extraStep);
                }

                //Project Opts To Keep into the extra steps
                for (int i=0; i<extraNJob.getNumberOfSteps(); i++)
                {
                    if (verbosity > 1)
                    {
                        System.out.println(" Setting extra step "+i+" of the "
                                                                   + "new job");
                    }

                    newNwcJob.getStep(i).impose(otkTsk);
                    if (i==0)
                    {
                        //Add/Update RESTART directive
                        newNwcJob.getStep(i).deleteDirective(
                                                       new ArrayList<String>(),
                                                      NWChemConstants.STARTDIR);
                        NWChemDirective restartDir = new NWChemDirective(
                                                    NWChemConstants.RESTARTDIR);
                        String dbName = nwcDataBaseName;
                        if (nwcDataBaseName.endsWith(".db"))
                        {
                            dbName = dbName.substring(0,dbName.length()-3);
                        }
                        NWChemKeyword dbKey = new NWChemKeyword(
                                                       NWChemConstants.DBNAMEKW,
                                                                          false,
                                  new ArrayList<String>(Arrays.asList(dbName)));
                        restartDir.addKeyword(dbKey);
                        newNwcJob.getStep(i).setDirective(
                                                       new ArrayList<String>(),
                                                     restartDir,true,true,true);
                    }
                }

                //Clean restart/start restart directive in failing step
                failStep.deleteDirective(new ArrayList<String>(),
                                                      NWChemConstants.STARTDIR);
                failStep.deleteDirective(new ArrayList<String>(),
                                                    NWChemConstants.RESTARTDIR);

                //Append the failing step and all the steps that never run
                for (int i=(failedStepID - 1); i<nwcJob.getNumberOfSteps(); i++)
                {
                    if (verbosity > 1)
                    {    
                        System.out.println(" Importing trailing step " + i);
                    }

                    //Copy from nwcJob to newNwcJob
                    NWChemTask copyOfOldStep = new NWChemTask(
                                        nwcJob.getStep(i).toLinesJobDetails());
                    newNwcJob.addStep(copyOfOldStep);
                }

                //Write the new INP file
                IOtools.writeTXTAppend(nwFile,newNwcJob.toLinesInput(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newNwcJob.toLinesJobDetails(),
                                                                         false);
                break;
            }
            case "SKIP_STEP":
            {
                //Copy only the steps after the failing one
                //
                // WARNING! failedStepID goes from 1 to n (not from 0 to n-1)
                //
                for (int i=failedStepID; i<nwcJob.getNumberOfSteps(); i++)
                {
                    if (verbosity > 1)
                    {
                        System.out.println(" Importing trailing step " + i);
                    }

                    //Copy from nwcJob to newNwcJob
                    NWChemTask copyOfOldStep = new NWChemTask(
                                        nwcJob.getStep(i).toLinesJobDetails());
                    if (i == failedStepID)
                    {
                        //Add/Update RESTART directive
                        copyOfOldStep.deleteDirective(new ArrayList<String>(),
                                                      NWChemConstants.STARTDIR);
                        NWChemDirective restartDir = new NWChemDirective(
                                                    NWChemConstants.RESTARTDIR);
                        String dbName = nwcDataBaseName;
                        if (nwcDataBaseName.endsWith(".db"))
                        {
                            dbName = dbName.substring(0,dbName.length()-3);
                        }
                        NWChemKeyword dbKey = new NWChemKeyword(
                                                       NWChemConstants.DBNAMEKW,
                                                                          false,
                                  new ArrayList<String>(Arrays.asList(dbName)));
                        restartDir.addKeyword(dbKey);
                        copyOfOldStep.setDirective(new ArrayList<String>(),
                                                     restartDir,true,true,true);
                    }
                    newNwcJob.addStep(copyOfOldStep);
                }

                //Write the new INP file
                IOtools.writeTXTAppend(nwFile,newNwcJob.toLinesInput(),false);

                //Write the new jobDetails file
                IOtools.writeTXTAppend(newJDFile,newNwcJob.toLinesJobDetails(),
                                                                        false);
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
                 Terminator.withMsgAndStatus("ERROR! Error Fixing Action '" 
                        + typeOfAction + "' not recognized.", -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Update the value of a counter stored in the title of a nwchem job.
     * The value of the counter is set according to the information
     * retrieved from the previous NWChem output.
     * @param title the outdated version of the title.
     * @param oEval the output handler retrieving the information about counters
     * @return the updated title
     */

    private String updateCounter(String title, NWChemOutputHandler oEval) 
    {
        Map<String,Integer> counters = oEval.getCounters();
        for (String counterName : counters.keySet())
        {
            //Prepare the new title with the new value of the counter
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
