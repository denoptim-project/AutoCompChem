package autocompchem.worker;

/*
 *   Copyright (C) 2020  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Set;

import autocompchem.chemsoftware.gaussian.GaussianInputWriter;
import autocompchem.chemsoftware.gaussian.GaussianOutputHandler;
import autocompchem.chemsoftware.gaussian.GaussianOutputHandler2;
import autocompchem.chemsoftware.gaussian.GaussianReStarter;
import autocompchem.chemsoftware.nwchem.NWChemInputWriter;
import autocompchem.chemsoftware.nwchem.NWChemOutputHandler;
import autocompchem.chemsoftware.nwchem.NWChemReStarter;
import autocompchem.chemsoftware.orca.OrcaInputWriter;
import autocompchem.chemsoftware.orca.OrcaOutputHandler;
import autocompchem.chemsoftware.spartan.SpartanInputWriter;
import autocompchem.chemsoftware.spartan.SpartanOutputHandler;
import autocompchem.chemsoftware.vibmodule.VibModuleOutputHandler;
import autocompchem.chemsoftware.xtb.XTBInputWriter;
import autocompchem.chemsoftware.xtb.XTBOutputHandler;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.AtomLabelsGenerator;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.basisset.BasisSetGenerator;
import autocompchem.modeling.constraints.ConstraintsGenerator;
import autocompchem.modeling.forcefield.AtomTypeMatcher;
import autocompchem.modeling.forcefield.ForceFieldEditor;
import autocompchem.molecule.MolecularComparator;
import autocompchem.molecule.MolecularMeter;
import autocompchem.molecule.MolecularMutator;
import autocompchem.molecule.MolecularPruner;
import autocompchem.molecule.MolecularReorderer;
import autocompchem.molecule.atomclashes.AtomClashAnalyzer;
import autocompchem.molecule.chelation.ChelateAnalyzer;
import autocompchem.molecule.conformation.ConformationalSpaceGenerator;
import autocompchem.molecule.connectivity.ConnectivityGenerator;
import autocompchem.molecule.dummyobjects.DummyObjectsHandler;
import autocompchem.molecule.geometry.MolecularGeometryEditor;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.molecule.sorting.MolecularSorter;
import autocompchem.run.Job;
import autocompchem.run.JobEvaluator;
import autocompchem.run.Terminator;

/**
 * Factory building AutoCompChem workers. In this factory we chose the worker
 * type based on the tasks that each worker type declares in its implementation.
 * As such declaration of suitable tasks is meant to live only in one place
 * (i.e., in the subclass implementing that capability), and we want to have no
 * registry of task-to-worker relations, we need to screen the
 * subclasses for those providing the functionality to carry on a specific task.
 * To reach this end and make sure only certain, specific values of 
 * registered task/worker IDs are used, we use {@link TaskID} and 
 * {@link WorkerID} enums. This allows to loop over subclasses of the 
 * {@link Worker} class (i.e., listed in the {@link WorkerID}), and expect to find one
 * of the registered {@link TaskID}, in the worker own declaration of its
 * capabilities. The down side is the long switch/case statements collected all
 * in this factory.
 * 
 * @author Marco Foscato
 */

public class WorkerFactory
{
//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the given task.
     * @param task the AutoCompChem task to be performed by the worker.
     * @return a suitable worker for the task.
     */ 

    public static Worker createWorker(TaskID taskID)
    {
    	return createWorker(taskID, null);
    }

//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the given task.
     * @param task the AutoCompChem task to be performed by the worker.
     * @return a suitable worker for the task.
     */ 

    public static Worker createWorker(String task)
    {
    	// Convert string-based task into enum
    	TaskID taskID = TaskID.getFromString(task);
    	
    	return createWorker(taskID);
    }
	
//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the given task for a master 
     * job.
     * @param task the AutoCompChem task to be performed by the worker.
     * @param masterJob the job that is creating a worker to perform a task.
     * @return a suitable worker for the task.
     */ 

    public static Worker createWorker(String task, Job masterJob)
    {
    	// Convert string-based task in to enum
    	TaskID taskID = TaskID.getFromString(task);
    	
    	return createWorker(taskID, masterJob);
    }

//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the task given in the 
     * parameters storage unit. This method initialised the worker, i.e., 
     * it make the worker read the parameters and load the corresponding input
     * and configurations.
     * @param params the parameters that define the task and all related 
     * settings and input data.
     * @return a suitable worker for the task.
     */ 

    public static Worker createWorker(ParameterStorage params)
    {    	
    	return createWorker(params,null);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the task given in the 
     * parameters storage unit. This method initialised the worker, i.e., 
     * it make the worker read the parameters and load the corresponding input
     * and configurations.
     * @param params the parameters that define the task and all related 
     * settings and input data.
     * @param masterJob the job that is creating a worker to perform a task.
     * @return a suitable worker for the task.
     */ 

    public static Worker createWorker(ParameterStorage params, Job masterJob)
    {
    	return createWorker(params, masterJob, true);
    }

//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the task given in the 
     * parameters storage unit. This method initialised the worker, i.e., 
     * it make the worker read the parameters and load the corresponding input
     * and configurations.
     * @param params the parameters that define the task and all related 
     * settings and input data.
     * @param masterJob the job that is creating a worker to perform a task.
     * @param initializeIt if <code>true</code> the worker is also initialized.
     * This requires that the parameters are consistent with the requirements
     * of the worker.
     * @return a suitable worker for the task.
     */ 

    public static Worker createWorker(ParameterStorage params, Job masterJob, 
    		boolean initializeIt)
    {
    	String taskStr = params.getParameter(
    			WorkerConstants.PARTASK).getValueAsString();
    	TaskID taskID = TaskID.getFromString(taskStr);
    	
    	Worker worker = createWorker(taskID, masterJob);
    	worker.setParameters(params);
    	if (initializeIt)
    	{
    		worker.initialize();
    	}
    	
    	return worker;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the given task for a master 
     * job.
     * @param task the AutoCompChem task to be performed by the worker.
     * @param masterJob the job that is creating a worker to perform a task.
     * @return a suitable worker for the task or null.
     */ 

    public static Worker createWorker(TaskID task, Job masterJob)
    {
    	// We first find out which worker is meant to take care of the given 
    	// task
    	ArrayList<WorkerID> suitableWorkerIDs = getWorkersCapableOfTask(task);
    	if (suitableWorkerIDs.size() > 1)
    	{
    		Terminator.withMsgAndStatus("ERROR! Multiple workers are "
    		 		+ "capable of task '"+task+"'. Unable to choose a "
    		 		+ "worker.",-1);
    	}
    	else if (suitableWorkerIDs.size() == 0)
    	{
    		 Terminator.withMsgAndStatus("ERROR! Unable to find a worker "
    		 		+ "capable of task '"+task+"'.",-1);
    	}
    	
    	// Now we make the actual worker
    	Worker worker = getNewWorkerInstance(suitableWorkerIDs.get(0));
    	
    	if (masterJob != null)
    	{
    	    worker.setDataCollector(masterJob.getOutputCollector());
    	}
    	
    	return worker;
    }
    
//----------------------------------------------------------------------------
    
    /**
     * Finds workers that have declared the capability to perform the given 
     * task.
     * @param task what suitable workers should be capable of.
     * @return the list of suitable workers (which can be empty!).
     */
    
    public static ArrayList<WorkerID> getWorkersCapableOfTask(TaskID task)
    {
    	ArrayList<WorkerID> suitableWorkerIDs = new ArrayList<WorkerID>();
    	for (WorkerID wid : WorkerID.values())
    	{	
    		Set<TaskID> workerCapabilities = getWorkerCapabilities(wid);
    		
			if (workerCapabilities == null)
			{
				Terminator.withMsgAndStatus("ERROR! Worker '" + wid + "' "
	    		 		+ "did not return capabilities. This is most "
						+ "likely a bug in '" + wid + "' or in "
						+ "the WorkerFactory. Make sure you added case '" + wid 
						+ "' in getWorkerCapabilities(). Please, report this "
						+ "to the authors.",-1);
			}
    		if (workerCapabilities.contains(task))
    		{
    			suitableWorkerIDs.add(wid);
    		}
    	}
    	return suitableWorkerIDs;
    }

//----------------------------------------------------------------------------
    
    /**
     * Return the set of tasks that a given worker identifier is capable of.
     * @param wid the worker identifier
     * @return the set of tasks that the given worker is capable of
     */
    
    public static Set<TaskID> getWorkerCapabilities(WorkerID wid)
	{
    	//TODO: evaluate registering the workers in other ways
		switch (wid)
		{
		case DummyWorker:
			return DummyWorker.capabilities;
        case AtomClashAnalyzer:
            return AtomClashAnalyzer.capabilities;
        case AtomTypeMatcher:
            return AtomTypeMatcher.capabilities;
        case AtomLabelsGenerator:
            return AtomLabelsGenerator.capabilities;
        case AtomTupleGenerator:
            return AtomTupleGenerator.capabilities;
        case BasisSetGenerator:
            return BasisSetGenerator.capabilities;
        case ChelateAnalyzer:
            return ChelateAnalyzer.capabilities;
        case ConnectivityGenerator:
            return ConnectivityGenerator.capabilities;
        case ConstraintsGenerator:
        	return ConstraintsGenerator.capabilities;
        case ConformationalSpaceGenerator:
        	return ConformationalSpaceGenerator.capabilities;
        case DummyObjectsHandler:
            return DummyObjectsHandler.capabilities;
        case ForceFieldEditor:
            return ForceFieldEditor.capabilities;
		case GaussianInputWriter:
			return GaussianInputWriter.capabilities;
        case GaussianOutputHandler:
            return GaussianOutputHandler.capabilities;
        case GaussianOutputHandler2:
            return GaussianOutputHandler2.capabilities;
        case GaussianReStarter:
            return GaussianReStarter.capabilities; 
        /*
        case GenericToolOutputHandler:
            return GenericToolOutputHandler.capabilities;
        */
        case JobEvaluator:
        	return JobEvaluator.capabilities;
        case MolecularComparator:
            return MolecularComparator.capabilities;
        case MolecularGeometryEditor:
            return MolecularGeometryEditor.capabilities;
        case MolecularMeter:
            return MolecularMeter.capabilities;
        case MolecularMutator:
            return MolecularMutator.capabilities;
        case MolecularPruner:
            return MolecularPruner.capabilities;
        case MolecularReorderer:
            return MolecularReorderer.capabilities;
        case MolecularSorter:
            return MolecularSorter.capabilities;
        case NWChemInputWriter:
			return NWChemInputWriter.capabilities; 
        case NWChemOutputHandler:
            return NWChemOutputHandler.capabilities;
        case NWChemReStarter:
            return NWChemReStarter.capabilities;
        case OrcaInputWriter:
        	return OrcaInputWriter.capabilities;
        case XTBInputWriter:
        	return XTBInputWriter.capabilities;
        case XTBOutputHandler:
        	return XTBOutputHandler.capabilities;
        case OrcaOutputHandler:
        	return OrcaOutputHandler.capabilities;
        case SpartanInputWriter:
            return SpartanInputWriter.capabilities;
        case SpartanOutputHandler:
            return SpartanOutputHandler.capabilities;
        case VibModuleOutputHandler:
            return VibModuleOutputHandler.capabilities;
        case ZMatrixHandler:
            return ZMatrixHandler.capabilities;

		//NB: add cases of new workers according to alphabetic order, please.
            
		}
		return null;
	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructs a new worker of a given type.
     * @param wid
     * @return
     */
    
    public static Worker getNewWorkerInstance(WorkerID wid)
    {
		switch (wid)
		{
		case DummyWorker:
			return new DummyWorker(); 
        case AtomClashAnalyzer:
            return new AtomClashAnalyzer();
        case AtomTypeMatcher:
            return new AtomTypeMatcher();
        case AtomLabelsGenerator:
            return new AtomLabelsGenerator();
        case AtomTupleGenerator:
            return new AtomTupleGenerator();
        case BasisSetGenerator:
            return new BasisSetGenerator();
        case ChelateAnalyzer:
            return new ChelateAnalyzer();
        case ConformationalSpaceGenerator:
        	return new ConformationalSpaceGenerator();
        case ConnectivityGenerator:
            return new ConnectivityGenerator();
        case ConstraintsGenerator:
        	return new ConstraintsGenerator();
        case DummyObjectsHandler:
            return new DummyObjectsHandler();
        case ForceFieldEditor:
            return new ForceFieldEditor();
		case GaussianInputWriter:
			return new GaussianInputWriter();
        case GaussianOutputHandler:
            return new GaussianOutputHandler();
        case GaussianOutputHandler2:
            return new GaussianOutputHandler2();
        case GaussianReStarter:
            return new GaussianReStarter();
        /*
        case GenericToolOutputHandler:
            return new GenericToolOutputHandler();
        */
        case JobEvaluator:
        	return new JobEvaluator();
        case MolecularComparator:
            return new MolecularComparator();
        case MolecularGeometryEditor:
            return new MolecularGeometryEditor();
        case MolecularMeter:
            return new MolecularMeter();
        case MolecularMutator:
            return new MolecularMutator();
        case MolecularPruner:
            return new MolecularPruner();
        case MolecularReorderer:
            return new MolecularReorderer();
        case MolecularSorter:
            return new MolecularSorter();
		case NWChemInputWriter:
			return new NWChemInputWriter();
        case NWChemOutputHandler:
            return new NWChemOutputHandler();
        case NWChemReStarter:
            return new NWChemReStarter();
        case OrcaInputWriter:
        	return new OrcaInputWriter();
        case OrcaOutputHandler:
        	return new OrcaOutputHandler();
        case XTBInputWriter:
        	return new XTBInputWriter();
        case XTBOutputHandler:
        	return new XTBOutputHandler();
        case SpartanInputWriter:
            return new SpartanInputWriter();
        case SpartanOutputHandler:
            return new SpartanOutputHandler();
        case VibModuleOutputHandler:
            return new VibModuleOutputHandler();
        case ZMatrixHandler:
            return new ZMatrixHandler();
            
		//NB: add cases of new workers according to alphabetic order, please
            
		}
		return null;
    }
    
//-----------------------------------------------------------------------------

}

