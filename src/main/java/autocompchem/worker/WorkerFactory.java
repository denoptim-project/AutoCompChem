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
import java.util.List;
import java.util.Set;

import autocompchem.chemsoftware.AspecificOutputAnalyzer;
import autocompchem.chemsoftware.gaussian.GaussianInputWriter;
import autocompchem.chemsoftware.gaussian.GaussianOutputAnalyzer;
import autocompchem.chemsoftware.gaussian.GaussianReStarter;
import autocompchem.chemsoftware.gaussian.legacy.GaussianJobDetailsConverter;
import autocompchem.chemsoftware.nwchem.NWChemInputWriter;
import autocompchem.chemsoftware.nwchem.NWChemOutputAnalyzer;
import autocompchem.chemsoftware.nwchem.NWChemOutputHandler;
import autocompchem.chemsoftware.nwchem.NWChemReStarter;
import autocompchem.chemsoftware.orca.OrcaInputWriter;
import autocompchem.chemsoftware.orca.OrcaOutputAnalyzer;
import autocompchem.chemsoftware.spartan.SpartanInputWriter;
import autocompchem.chemsoftware.spartan.SpartanOutputHandler;
import autocompchem.chemsoftware.vibmodule.VibModuleOutputHandler;
import autocompchem.chemsoftware.xtb.XTBInputWriter;
import autocompchem.chemsoftware.xtb.XTBOutputAnalyzer;
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
import autocompchem.run.ACCJob;
import autocompchem.run.Job;
import autocompchem.run.JobEvaluator;
import autocompchem.run.Terminator;

/**
 * Factory building workers. In this factory we chose the worker
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
     * Create a new worker that is meant to do the task in the given job. 
     * This method initializes the worker, i.e., 
     * it make the worker read the parameters and load the corresponding input
     * and configurations.
     * @param job the job to be done by the worker. We assume the parameters of 
     * this job include the {@link WorkerConstants#PARTASK}.
     * @return a suitable worker for the task.
     */ 

    private static Worker createWorker(Job job)
    {
    	return createWorker(job, true);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the task given in the 
     * parameters storage unit, which is expected to define an {@link ACCJob} 
     * to be embedded in the given main job. Effectively, we use the given
     * parameters to create a child job 
     * and we return a worker that can perform the task defined in the child 
     * job. Note that child jobs are not steps of the main job.
     * This method initializes the worker, i.e., 
     * it make the worker read the parameters and load the corresponding input
     * and configurations.
     * @param params the parameters that define the task and all related 
     * settings and input data of the child job.
     * @param mainJob the job that needs a task to be performed by a child job.
     * @return a suitable worker for the task.
     */ 

    private static Worker createWorker(ParameterStorage params, Job mainJob)
    {
    	Job subjob = new ACCJob(params);
    	if (mainJob!=null)
    	{
    		mainJob.addChild(subjob);
    		subjob.setParent(mainJob);
    	}
    	return createWorker(subjob, true);
    }

//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the task defined by the given 
     * job.
     * @param job the job that defines the task to be done by the worker.
     * @param initializeIt if <code>true</code> the worker is also initialized.
     * This requires that the parameters are consistent with the requirements
     * of the worker.
     * @return a suitable worker for the task defined by the job.
     */ 

    private static Worker createWorker(Job job, boolean initializeIt)
    {
    	String taskStr = job.getParameter(
    			WorkerConstants.PARTASK).getValueAsString();
    	TaskID taskID = TaskID.getFromString(taskStr);
    	
    	Worker worker = createWorker(taskID, job);
    	worker.setParameters(job.getParameters());
    	if (initializeIt)
    	{
    		worker.initialize();
    	}
    	
    	return worker;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Create a new worker capable of performing the given task, which is 
     * defined by by the given job. 
     * @param task the task to be performed by the worker.
     * @param job the job that is creating a worker to perform a task.
     * @return a suitable worker for the task or null.
     */ 

    private static Worker createWorker(TaskID task, Job job)
    {
    	// We first find out which kind of worker is meant to do the task.
    	List<WorkerID> suitableWorkerIDs = getWorkersCapableOfTask(task);
    	if (suitableWorkerIDs.size() > 1)
    	{
    		Terminator.withMsgAndStatus("ERROR! Multiple workers are "
    		 		+ "capable of task '"+task+"'. Unable to choose a "
    		 		+ "worker.", -1);
    	}
    	else if (suitableWorkerIDs.size() == 0)
    	{
    		 Terminator.withMsgAndStatus("ERROR! Unable to find a worker "
    		 		+ "capable of task '" + task + "'. A worker may exist but "
    		 		+ "has not been registered into " 
    		 		+ WorkerID.class.getName(), -1);
    	}
    	
    	// Now we make the actual worker
    	Worker worker = getNewWorkerInstance(suitableWorkerIDs.get(0));
    	worker.myJob = job;
    	
    	if (job != null)
    	{
    	    worker.setDataCollector(job.getOutputCollector());
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
    
    private static ArrayList<WorkerID> getWorkersCapableOfTask(TaskID task)
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
    
    private static Set<TaskID> getWorkerCapabilities(WorkerID wid)
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
        case GaussianJobDetailsConverter:
        	return GaussianJobDetailsConverter.capabilities;
        case DummyObjectsHandler:
            return DummyObjectsHandler.capabilities;
        case ForceFieldEditor:
            return ForceFieldEditor.capabilities;
		case GaussianInputWriter:
			return GaussianInputWriter.capabilities;
        case GaussianOutputAnalyzer:
            return GaussianOutputAnalyzer.capabilities;
        case GaussianReStarter:
            return GaussianReStarter.capabilities; 
        /*
        case GenericToolOutputHandler:
            return GenericToolOutputHandler.capabilities;
        */
        case AspecificOutputAnalyzer:
        	return AspecificOutputAnalyzer.capabilities;
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
			//TODO-gg del
        case NWChemOutputHandler:
            return NWChemOutputHandler.capabilities;
        case NWChemOutputAnalyzer:
            return NWChemOutputAnalyzer.capabilities;
        case NWChemReStarter:
            return NWChemReStarter.capabilities;
        case OrcaInputWriter:
        	return OrcaInputWriter.capabilities;
        case XTBInputWriter:
        	return XTBInputWriter.capabilities;
        case XTBOutputAnalyzer:
        	return XTBOutputAnalyzer.capabilities;
        case OrcaOutputAnalyzer:
        	return OrcaOutputAnalyzer.capabilities;
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
    
    private static Worker getNewWorkerInstance(WorkerID wid)
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
        case GaussianJobDetailsConverter:
        	return new GaussianJobDetailsConverter();
        case ConstraintsGenerator:
        	return new ConstraintsGenerator();
        case DummyObjectsHandler:
            return new DummyObjectsHandler();
        case ForceFieldEditor:
            return new ForceFieldEditor();
		case GaussianInputWriter:
			return new GaussianInputWriter();
        case GaussianOutputAnalyzer:
            return new GaussianOutputAnalyzer();
        case GaussianReStarter:
            return new GaussianReStarter();
        /*
        case GenericToolOutputHandler:
            return new GenericToolOutputHandler();
        */
        case AspecificOutputAnalyzer:
        	return new AspecificOutputAnalyzer();
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
			//TODO-gg del
        case NWChemOutputHandler:
            return new NWChemOutputHandler();
        case NWChemOutputAnalyzer:
            return new NWChemOutputAnalyzer();
        case NWChemReStarter:
            return new NWChemReStarter();
        case OrcaInputWriter:
        	return new OrcaInputWriter();
        case OrcaOutputAnalyzer:
        	return new OrcaOutputAnalyzer();
        case XTBInputWriter:
        	return new XTBInputWriter();
        case XTBOutputAnalyzer:
        	return new XTBOutputAnalyzer();
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

