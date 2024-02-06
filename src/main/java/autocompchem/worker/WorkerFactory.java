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

import java.util.HashMap;
import java.util.Map;

import autocompchem.chemsoftware.AspecificInputWriter;
import autocompchem.chemsoftware.AspecificOutputAnalyzer;
import autocompchem.chemsoftware.gaussian.GaussianInputWriter;
import autocompchem.chemsoftware.gaussian.GaussianOutputReader;
import autocompchem.chemsoftware.gaussian.legacy.GaussianJobDetailsConverter;
import autocompchem.chemsoftware.nwchem.NWChemInputWriter;
import autocompchem.chemsoftware.nwchem.NWChemOutputReader;
import autocompchem.chemsoftware.orca.OrcaInputWriter;
import autocompchem.chemsoftware.orca.OrcaOutputReader;
import autocompchem.chemsoftware.spartan.SpartanInputWriter;
import autocompchem.chemsoftware.spartan.SpartanOutputHandler;
import autocompchem.chemsoftware.vibmodule.VibModuleOutputHandler;
import autocompchem.chemsoftware.xtb.XTBInputWriter;
import autocompchem.chemsoftware.xtb.XTBOutputReader;
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

/**
 * Factory building {@link Worker}s. In this factory we chose the worker
 * type based on the tasks that each worker type declares in its implementation.
 * As such, declaration of suitable tasks is meant to live only in one place
 * (i.e., in the subclass implementing that capability). However, here we keep
 * a static registry of the {@link Task}-to-{@link Worker} relations, 
 * so we can easily find a {@link Worker} for a specific {@link Task}.
 * 
 * @author Marco Foscato
 */

public final class WorkerFactory
{
	/**
	 * The collection of registered types of {@link Worker} with the declared 
	 * task.
	 */
	private static Map<Task, Worker> knownWorkers = new HashMap<Task, Worker>();

	/**
	 * Singleton instance of this class
	 */
	private static WorkerFactory INSTANCE;

	
//-----------------------------------------------------------------------------
	
	private WorkerFactory() 
	{
		// Here we add all the workers those implemented in AudoCompChem      
        registerType(new AspecificInputWriter());      
        registerType(new AspecificOutputAnalyzer());
        registerType(new DummyWorker()); //This is only for tests
        registerType(new AtomClashAnalyzer());
        registerType(new AtomTypeMatcher());
        registerType(new AtomLabelsGenerator());
        registerType(new AtomTupleGenerator());
        registerType(new BasisSetGenerator());
        registerType(new ChelateAnalyzer());
        registerType(new ConformationalSpaceGenerator());
        registerType(new ConnectivityGenerator());
        registerType(new ConstraintsGenerator());
        registerType(new GaussianJobDetailsConverter());
        registerType(new DummyObjectsHandler());
        registerType(new ForceFieldEditor());
        registerType(new GaussianInputWriter());
        registerType(new GaussianOutputReader());
        registerType(new JobEvaluator());
        registerType(new MolecularComparator());
        registerType(new MolecularGeometryEditor());
        registerType(new MolecularMeter());
        registerType(new MolecularMutator());
        registerType(new MolecularPruner());
        registerType(new MolecularReorderer());
        registerType(new MolecularSorter());
        registerType(new NWChemInputWriter());
        registerType(new NWChemOutputReader());
        registerType(new OrcaInputWriter());
        registerType(new OrcaOutputReader());
        registerType(new XTBInputWriter());
        registerType(new XTBOutputReader());
        registerType(new SpartanInputWriter());
        registerType(new SpartanOutputHandler());
        registerType(new VibModuleOutputHandler());
        registerType(new ZMatrixHandler());
	}

//-----------------------------------------------------------------------------

	/**
	 * Returns the singleton instance of this class, i.e., the sole factory of
	 * {@link Worker}s that can be configured and used.
	 * @return the singleton instance.
	 */
	public synchronized static WorkerFactory getInstance()
	{
		if (INSTANCE==null)
			INSTANCE = new WorkerFactory();
		return INSTANCE;
	}

//-----------------------------------------------------------------------------

	/**
	 * Registers any type of {@link Worker} using a concrete implementation as 
	 * example. The given object will not be used for any task.
	 */
	public synchronized void registerType(Object object)
	{
		if (object instanceof Worker)
		{
			Worker worker = (Worker) object;
			for (Task task : worker.getCapabilities())
			{
				knownWorkers.put(task, (Worker) object);
			}
		} else {
			//TODO-gg: log warning
			System.err.println("Registration of " + Worker.class.getSimpleName() 
					+ " has failed because the given example object is not an "
					+ "instance of "
					+ Worker.class.getSimpleName() + ".");
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
     * Create a new {@link Worker} of a given class
     * @param className the simple name of the {@link Worker}'s class
     */
	public static Worker createWorker(Class<? extends Worker> clazz) 
			throws ClassNotFoundException
	{
		for (Worker exampleObj : knownWorkers.values())
		{
			if (exampleObj.getClass().equals(clazz))
			{
				return exampleObj.makeInstance(null);
			}
		}
		throw new ClassNotFoundException("No registered worker with type '" 
				+ clazz.getSimpleName() + "'.");
	}
	
//-----------------------------------------------------------------------------

    /**
     * Create a new worker that is meant to do the task in the given job. 
     * This method initializes the worker, i.e., 
     * it make the worker read the parameters and load the corresponding input
     * and configurations.
     * @param job the job to be done by the worker. We assume the parameters of 
     * this job include the {@link WorkerConstants#PARTASK}.
     * @return a suitable worker for the task.
     * @throws ClassNotFoundException if no suitable {@link Worker} has been 
     * registered, so we cannot make any instance of {@link Worker}.
     */ 

    public static Worker createWorker(Job job) throws ClassNotFoundException
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
     * @throws ClassNotFoundException if no suitable {@link Worker} has been 
     * registered, so we cannot make any instance of {@link Worker}.
     */ 

    public static Worker createWorker(ParameterStorage params, Job mainJob)
    		throws ClassNotFoundException
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
     * @throws ClassNotFoundException if no suitable {@link Worker} has been 
     * registered, so we cannot make any instance of {@link Worker}.
     */ 

    public static Worker createWorker(Job job, boolean initializeIt)
    		throws ClassNotFoundException
    {
    	String taskStr = job.getParameter(
    			WorkerConstants.PARTASK).getValueAsString();
    	Task task = Task.make(taskStr);
    	Worker worker = createWorker(task, job);
    	worker.setParameters(job.getParameters());
    	if (initializeIt)
    	{
    		worker.initialize();
    	}
    	return worker;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Create a new worker of a given type.
     * @param task the task to be performed by the worker.
     * @return a suitable worker for the task or null.
     * @throws ClassNotFoundException if no suitable {@link Worker} has been 
     * registered, so we cannot make any instance of {@link Worker}.
     */ 

    public static Worker createWorker(Task task) throws ClassNotFoundException
    {
    	return createWorker(task, null);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Create a new {@link Worker} capable of performing the given task, which i
     * is defined by the given {@link Job}. 
     * @param task the task to be performed by the {@link Worker}.
     * @param job the {@link Job} that has to be done by the {@link Worker}.
     * @return a suitable {@link Worker} for the task or <code>null</code>.
     * @throws ClassNotFoundException if no suitable {@link Worker} has been 
     * registered, so we cannot make any instance of {@link Worker}.
     */ 

    private synchronized static Worker createWorker(Task task, Job job) 
    		throws ClassNotFoundException
    {
    	if (INSTANCE==null)
    		getInstance();
    	
    	if (!knownWorkers.containsKey(task))
    	{
    		throw new ClassNotFoundException("Type of "
    				+ Worker.class.getSimpleName() + " has not been registered "
    				+ "in " + INSTANCE.getClass().getName() + " for task '"
    		 		+ task + "'.");
    	}
    	
    	Worker worker = knownWorkers.get(task).makeInstance(job);
    	if (worker!=null)
    	{	worker.myJob = job;
    		if (job != null)
	    	{
	    	    worker.setDataCollector(job.getOutputCollector());
	    	}
    	}
    	
    	return worker;
    }
    
//-----------------------------------------------------------------------------

}

