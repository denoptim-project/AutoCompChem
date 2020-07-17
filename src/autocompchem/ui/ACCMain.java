package autocompchem.ui;

import java.util.Date;

import autocompchem.chemsoftware.gaussian.GaussianInputWriter;
import autocompchem.chemsoftware.gaussian.GaussianOutputHandler;
import autocompchem.chemsoftware.gaussian.GaussianReStarter;
import autocompchem.chemsoftware.generic.GenericToolOutputHandler;
import autocompchem.chemsoftware.nwchem.NWChemInputWriter;
import autocompchem.chemsoftware.nwchem.NWChemOutputHandler;
import autocompchem.chemsoftware.nwchem.NWChemReStarter;
import autocompchem.chemsoftware.qmmm.QMMMInputWriter;
import autocompchem.chemsoftware.spartan.SpartanInputWriter;
import autocompchem.chemsoftware.spartan.SpartanOutputHandler;
import autocompchem.chemsoftware.vibmodule.VibModuleOutputHandler;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.modeling.forcefield.AtomTypeMatcher;
import autocompchem.modeling.forcefield.ForceFieldEditor;
import autocompchem.molecule.MolecularComparator;
import autocompchem.molecule.MolecularMeter;
import autocompchem.molecule.MolecularMutator;
import autocompchem.molecule.MolecularPruner;
import autocompchem.molecule.MolecularReorderer;
import autocompchem.molecule.atomclashes.AtomClashAnalyzer;
import autocompchem.molecule.chelation.ChelateAnalyzer;
import autocompchem.molecule.connectivity.ConnectivityGenerator;
import autocompchem.molecule.dummyobjects.DummyObjectsHandler;
import autocompchem.molecule.geometry.MolecularGeometryEditor;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.molecule.sorting.MolecularSorter;
import autocompchem.run.Terminator;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;

/**
 * Main for AtomCompChem (Automated Computational Chemist). The entry point
 * for both cli and gui.
 *
 * @version 25 Nov 2013
 * @author Marco Foscato
 */

public class ACCMain
{
    //Software version number //TODO: move to logging class
    private static final String version = "1.0";
    
    // System.spec line separator
    private static final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------
    public static void main(String[] args)
    {
        // Welcome and check of the correct usage
        printInit();
        if (args.length < 1)
        {
//TODO more than one args can be used to give params for more than one task
            printUsage();
            Terminator.withMsgAndStatus("ERROR! No input or command line "
                + "argument given. " + System.getProperty("line.separator")
                + "AutoCompChem requires one (and only one) argument.",1);
        }

        // Update options and environment according to User Input
        ParameterStorage ACCParameters = new ParameterStorage();
        ACCParameters.setDefault();
        try {
            ACCParameters.importParameters(args[0]);
        } catch (Throwable t) {
            String msg = "Exception returned while reading Parameters.";
            t.printStackTrace();
            Terminator.withMsgAndStatus(msg,-1);
        }

        // Do the task
        try {
//TODO sequence of tasks
//TODO            //do all the tasks in the list
//TODO            for (int i=0; i<taskList.size(); i++)
//TODO            {

                String task = ACCParameters.getParameter("TASK").getValue().toString();
                Date date = new Date();
                System.out.println(" " + date.toString());
                System.out.println(" AutoCompChedm is initiating the task '" 
                                + task + "'. ");

                runTask(task,ACCParameters);

//TODO            } //end of loop over tasks
        } catch (Throwable t) {
            t.printStackTrace();

            String msg = t.getMessage();

            if (msg == null)
            {
                Terminator.withMsgAndStatus("Exception occurred! But 'null' "
                        + "message returned. Please "
                        + "report this to the author.", -1);
            }

            if (msg.startsWith("ERROR!"))
            {
                Terminator.withMsgAndStatus(t.getMessage(),-1);
            } else {
                t.printStackTrace();
                Terminator.withMsgAndStatus("Exception occurred! Please "
                        + "report this to the author.", -1);
            }
        }
        
        // Exit
        Terminator.withMsgAndStatus("Normal Termination",0);
    }

//------------------------------------------------------------------------------

    /**
     * Run a specific task
     * @param task the string identifying the type of task
     * @param params a <code>autocompchem.parameters.ParameterStorage</code> 
     * passing all the necessary parameters to the tool executing the task.
     */
    private static void runTask(String task, ParameterStorage params) 
                                                                throws Throwable
    {
    	
        Worker worker = WorkerFactory.createWorker(task);
        worker.setParameters(params);
        worker.initialize();
        worker.performTask();
    }

//------------------------------------------------------------------------------

    /**
     * Write the initial message
     */
    private static void printInit()
    {
    	
        System.out.println(NL + NL 
        		+ "**********************************************"
                + "*****************************"
                + NL + "                              AutoCompChem"
                + NL + "                              Version: "+version
                + NL + "**********************************************"
                + "*****************************" + NL);
    }

//------------------------------------------------------------------------------
    /**
     * Write the manual/usage information
     */
    private static void printUsage()
    {
        System.out.println(NL + " Usage: "
        		+ NL + " java -jar AutoCompChem.jar <parameters_file>" + NL);
    }

//------------------------------------------------------------------------------
}
