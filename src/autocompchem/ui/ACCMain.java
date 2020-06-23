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
import autocompchem.parameters.ParameterStorage;
import autocompchem.run.Terminator;

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

//------------------------------------------------------------------------------
    public static void main(String[] args)
    {
        // Welcome and check of the correct usage
        printInit();
        if (args.length < 1)
        {
//TODO more than one ars can be used to give params for more than one task
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
     * @param task the string indentifying the type of task
     * @param params a <code>autocompchem.parameters.ParameterStorage</code> 
     * passing all the necessary parameters to the tool executing the task.
     */
    private static void runTask(String task, ParameterStorage params) 
                                                                throws Throwable
    {
        task = task.toUpperCase();
        switch (task) {

            case "EVALUATEGAUSSIANOUTPUT":
            {
                GaussianOutputHandler gauOEval = 
                                              new GaussianOutputHandler(params);
                gauOEval.performAnalysis();
                break;
            }

            case "FIXANDRESTARTGAUSSIAN":
            {
                GaussianReStarter gauReStart = new GaussianReStarter(params);
                gauReStart.restartJobWithErrorFix();
                break;
            }

            case "PREPAREINPUTGAUSSIAN":
            {
                GaussianInputWriter gauInpWriter = new GaussianInputWriter(
                                                                        params);
                gauInpWriter.writeInp();
                break;
            }

            case "EVALUATENWCHEMOUTPUT":
            {
                NWChemOutputHandler nwcOEval = new NWChemOutputHandler(params);
                nwcOEval.performAnalysis();
                break;
            }

            case "EXTRACTOPTGEOMSFROMNWCHEMOUTPUT":
            {
                NWChemOutputHandler nwcOEval = new NWChemOutputHandler(params);
                nwcOEval.printOptTrajectory();
                break;
            }

            case "EXTRACTTRAJECTORYFROMNWCHEMOUTPUT":
            {
                NWChemOutputHandler nwcOEval = new NWChemOutputHandler(params);
                nwcOEval.printTrajectory();
                break;
            }

            case "EXTRACTLASTGEOMETRYFROMNWCHEMOUTPUT":
            {
                NWChemOutputHandler nwcOEval = new NWChemOutputHandler(params);
                nwcOEval.printLastOutputGeometry();
                break;
            }

            case "FIXANDRESTARTNWCHEM":
            {
                NWChemReStarter nwcReStart = new NWChemReStarter(params);
                nwcReStart.restartJobWithErrorFix();
                break;
            }

            case "PREPAREINPUTNWCHEM":
            {
                NWChemInputWriter nwcInpWriter = new NWChemInputWriter(params);
                nwcInpWriter.writeInput();
                break;
            }

            case "PREPAREINPUTQMMM":
            {
                QMMMInputWriter qmmmInpWriter = new QMMMInputWriter(params);
                qmmmInpWriter.writeInputForEachMol();
                break;
            }

            case "COMPARETWOMOLECULES":
            {
                MolecularComparator mc = new MolecularComparator(params);
                mc.runComparisonOfMoleculesBySuperposition();
                break;
            }

            case "COMPARETWOGEOMETRIES":
            {
                MolecularComparator mcg = new MolecularComparator(params);
                mcg.compareTwoGeometries();
                break;
            }

            case "COMPARETWOCONNECTIVITIES":
            {
                MolecularComparator mcc = new MolecularComparator(params);
                mcc.compareTwoConnectivities();
                break;
            }

            case "RICALCULATECONNECTIVITY":
            {
                //TODO: to test 
                ConnectivityGenerator cg = new ConnectivityGenerator(params);
                cg.ricalculateConnectivity();
                break;
            }

            case "ADDBONDSFORSINGLEELEMENT":
            {
                ConnectivityGenerator cgse = new ConnectivityGenerator(params);
                cgse.addBondsOnSingleElement();
                break;
            }

            case "ASSIGNATOMTYPES":
            {
                AtomTypeMatcher atm = new AtomTypeMatcher(params);
                atm.assignAtomTypesToAll();
                break;
            }

            case "PARAMETRIZEFORCEFIELD":
            {
                ForceFieldEditor ffEdit = new ForceFieldEditor(params);
                ffEdit.includeFFParamsFromVibModule();
                break;
            }

            case "MEASUREGEOMDESCRIPTORS":
            {
                MolecularMeter mtr = new MolecularMeter(params);
                mtr.measureAllQuantities();
                break;
            }

            case "ANALYZEVDWCLASHES":
            {
                AtomClashAnalyzer avdsc = new AtomClashAnalyzer(params);
                avdsc.runStandalone();
                break;
            }

            case "SORTSDFMOLECULES":
            {
                MolecularSorter ms = new MolecularSorter(params);
                ms.writeSortedSDF();
                break;
            }

            case "ANALYSISCHELATES":
            {
                ChelateAnalyzer ca = new ChelateAnalyzer(params);
                ca.runStandalone();
                break;
            }

            case "ADDDUMMYATOMS":
            {
                DummyObjectsHandler doh = new DummyObjectsHandler(params);
                doh.addDummyAtoms();
                break;
            }

            case "REMOVEDUMMYATOMS":
            {
                DummyObjectsHandler doh = new DummyObjectsHandler(params);
                doh.removeDummyAtoms();
                break;
            }

            case "REORDERATOMLIST":
            {
                MolecularReorderer mReor = new MolecularReorderer(params);
                mReor.reorderAll();
                break;
            }

            case "MUTATEATOMS":
            {
                MolecularMutator mMut = new MolecularMutator(params);
                mMut.mutateAll();
                break;
            }

            case "EVALUATEGENERICOUTPUT":
            {
                GenericToolOutputHandler gtOH = 
                                          new GenericToolOutputHandler(params);
                gtOH.performAnalysis();
                break;
            }

            case "MODIFYGEOMETRY":
            {
                MolecularGeometryEditor mge = 
                                            new MolecularGeometryEditor(params);
                mge.applyMove();
                break;
            }

/*
TODO
            case "FIXANDRESTARTGENERIC":
            {
                GenericReStarter genReStart = new GenericReStarter(params);
                genReStart.restartJobWithErrorFix();
                break;
            }
*/

            case "PRUNEMOLECULES":
            {
                MolecularPruner molPruner = new MolecularPruner(params);
                molPruner.pruneAll();
                break;
            }

            case "PREPAREINPUTSPARTAN":
            {
                SpartanInputWriter sprtIn = new SpartanInputWriter(params);
                sprtIn.writeInputForEachMol();
                break;
            }

            case "EXTRACTGEOMETRIESFROMSPARTANTREE":
            {
                SpartanOutputHandler sprtOut = new SpartanOutputHandler(params);
                sprtOut.printTrajectory();
                break;
            }

            case "EXTRACTLASTGEOMETRYFROMSPARTANTREE":
            {
                SpartanOutputHandler sprtOut2 =new SpartanOutputHandler(params);
                sprtOut2.printLastOutputGeometry();
                break;
            }

            case "GENERATEBASISSET":
            {
                BasisSetGenerator bsGen = new BasisSetGenerator(params);
                bsGen.assignBasisSetToAllMolsInFile();
                break;
            }

            case "EXTRACTVIBMODULEFORCECONSTANTS":
            {
                VibModuleOutputHandler vmOEval = 
                                             new VibModuleOutputHandler(params);
                vmOEval.extractForceFieldParameters();
                break;
            }

            case "PRINTZMATRIX":
            {
                ZMatrixHandler zmh = new ZMatrixHandler(params);
                zmh.printZMatrix();
                break;
            }

            case "CONVERTZMATRIXTOSDF":
            {
                ZMatrixHandler zmh = new ZMatrixHandler(params);
                zmh.convertZMatrixToSDF();
                break;
            }

            case "SUBTRACTZMATRICES":
            {
                ZMatrixHandler zmh = new ZMatrixHandler(params);
                zmh.subtractZMatrices();
                break;
            }

            case "ALIGNATOMLISTS":
            {
                MolecularReorderer mReor = new MolecularReorderer(params);
                mReor.alignAtomList();
                break;
            }

/*
            case "":
            {
                //
                System.out.println("");

                break;
            }
*/

            default:
            {
                Terminator.withMsgAndStatus("ERROR! Task '" + task  + "'"
                                + " not known! Check the input.",-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write the initial message
     */
    private static void printInit()
    {
        System.out.println("\n\n**********************************************"
                + "*****************************"
                + "\n                              AutoCompChem"
                +"\n                              Version: "+version
                +"\n**********************************************"
                + "*****************************\n");
    }

//------------------------------------------------------------------------------
    /**
     * Write the manual/usage information
     */
    private static void printUsage()
    {
        System.out.println("\n Usage: \n java -jar AutoCompChem.jar "
                                +"<parameters_file>\n");
    }

//------------------------------------------------------------------------------
}
