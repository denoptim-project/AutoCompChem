package autocompchem.run;

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
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;

/**
 * A job class that represents work to be done by AutoCompChem itself
 *
 * @author Marco Foscato
 */

public class ACCJob extends Job 
{
 
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ACCJob()
    {
        super();
        this.appID = RunnableAppID.ACC;
    }

//------------------------------------------------------------------------------

    /**
     * Runs the given task
     */

    @Override
    public void runThisJobSubClassSpecific()
    {   
    	// Check for any ACC task...
    	if (!this.params.contains("TASK"))
    	{
    		// ...if none, then this job is just a container for other jobs
    		if (getVerbosity() > 0)
            {
                System.out.println("Running job container " + this.toString());
            }
    		return;
    	}

        // Here we are sure the params include this keyword
        String task = this.params.getParameter("TASK").getValue().toString();
        
        Date date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println(" AutoCompChedm is initiating the task '" 
                            + task + "'. ");
        }
        
        Worker worker = WorkerFactory.createWorker(params,this);
        worker.performTask();
        
        //TODO del
        //OLD WAY
        /*
        task = task.toUpperCase();
        switch (task)
        {
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

            default:
            {
                Terminator.withMsgAndStatus("ERROR! Task '" + task  + "'"
                                + " not known! Check the input.",-1);
            }
        }
        */
        

        date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Done with ACC job (" + task	+ ")");
        }
    }

//------------------------------------------------------------------------------

}
