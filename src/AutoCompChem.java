
/*
 *   AutoCompChem
 *   Copyright (C) 2014 Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Date;

import java.lang.Throwable;

import uibkvant.parameters.ParameterStorage;
import uibkvant.chemsoftware.generic.GenericToolOutputHandler;
import uibkvant.chemsoftware.gaussian.GaussianOutputHandler;
import uibkvant.chemsoftware.gaussian.GaussianReStarter;
import uibkvant.chemsoftware.gaussian.GaussianInputWriter;
import uibkvant.chemsoftware.nwchem.NWChemOutputHandler;
import uibkvant.chemsoftware.nwchem.NWChemReStarter;
import uibkvant.chemsoftware.nwchem.NWChemInputWriter;
import uibkvant.chemsoftware.qmmm.QMMMInputWriter;
import uibkvant.chemsoftware.spartan.SpartanInputWriter;
import uibkvant.chemsoftware.spartan.SpartanOutputHandler;
import uibkvant.chemsoftware.vibmodule.VibModuleOutputHandler;
import uibkvant.molecule.MolecularComparator;
import uibkvant.molecule.MolecularMeter;
import uibkvant.molecule.MolecularMutator;
import uibkvant.molecule.MolecularPruner;
import uibkvant.molecule.MolecularReorderer;
import uibkvant.molecule.MolecularGeometryEditor;
import uibkvant.molecule.connectivity.ConnectivityGenerator;
import uibkvant.run.Terminator;
import uibkvant.modeling.forcefield.AtomTypeMatcher;
import uibkvant.modeling.forcefield.ForceFieldEditor;
import uibkvant.modeling.basisset.BasisSetGenerator;
import uibkvant.molecule.atomclashes.AtomClashAnalyzer;
import uibkvant.molecule.sorting.MolecularSorter;
import uibkvant.molecule.chelation.ChelateAnalyzer;
import uibkvant.molecule.dummyobjects.DummyObjectsHandler;
import uibkvant.molecule.intcoords.zmatrix.ZMatrixHandler;
import uibkvant.perception.PerceptronTest;
import uibkvant.files.FilesAnalyzer;

/**
 * AtomCompChem (Automated Computational Chemist) is a tool meant for 
 * preparation of input files, analysis of outputs, and interaction
 * with computational chemistry-related softwares calculations.
 *
 * @version 25 Nov 2013
 * @author Marco Foscato (University of Bergen)
 */

public class AutoCompChem
{
    //Software version number
    private static final String version = "1.0";

//------------------------------------------------------------------------------
    public static void main(String[] args)
    {
        // Welcome and check of the correct usage
        printInit();
        if (args.length != 1)
        {
            printUsage();
            Terminator.withMsgAndStatus("ERROR! AutoCompChem requires "
			+ "one (and only one) argument.",1);
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
     * @param params a <code>uibkvant.parameters.ParameterStorage</code> 
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

            case "TESTPERCEPTRON":
            {
		PerceptronTest.runTestLogParse();
                break;
            }

	    case "TESTTXTEXTRACTOR":
	    {
		FilesAnalyzer.testExtractMapOfTxtBlocksWithDelimiters();
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
