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
       

        date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Done with ACC job (" + task	+ ")");
        }
    }

//------------------------------------------------------------------------------

}
