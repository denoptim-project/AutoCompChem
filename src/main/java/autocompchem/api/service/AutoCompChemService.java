package autocompchem.api.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.run.ACCJob;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * Service class that provides a high-level interface to AutoCompChem functionality
 * for REST API endpoints.
 *
 * @author Marco Foscato
 */
@Service
public class AutoCompChemService {

    private static final Logger logger = LogManager.getLogger(AutoCompChemService.class);

    /**
     * Get all available tasks that can be performed by workers.
     * @return list of available tasks
     */
    public List<Task> getAvailableTasks() {
        // Ensure WorkerFactory is initialized
        WorkerFactory.getInstance();
        
        List<Task> tasks = Task.getRegisteredTasks();
        logger.debug("Found {} registered tasks", tasks.size());
        for (Task task : tasks) {
            logger.debug("Registered task: {}", task.casedID);
        }
        return tasks;
    }

    /**
     * Execute a task with given parameters.
     * @param taskName the name of the task to execute
     * @param parameters the parameters for the task
     * @return the result of the task execution
     * @throws Exception if task execution fails
     */
    public String executeTask(String taskName, ParameterStorage parameters) throws Exception {
        // Add the task to parameters if not present
        if (!parameters.contains(WorkerConstants.PARTASK)) {
            parameters.setParameter(WorkerConstants.PARTASK, taskName);
        }

        // Create and run the job
        Job job = new ACCJob(parameters);
        job.run();

        return "Task '" + taskName + "' completed successfully";
    }

    /**
     * Read molecular structures from a file.
     * @param file the file to read
     * @return list of molecular structures
     */
    public List<IAtomContainer> readMolecularStructures(File file) {
        return IOtools.readMultiMolFiles(file);
    }

    /**
     * Write molecular structures to a file.
     * @param molecules the molecular structures to write
     * @param file the output file
     * @param format the output format (SDF, XYZ, etc.)
     * @param append whether to append to existing file
     */
    public void writeMolecularStructures(List<IAtomContainer> molecules, File file, 
                                         String format, boolean append) {
        IOtools.writeAtomContainerSetToFile(file, molecules, format, append);
    }

    /**
     * Get help information for a specific task.
     * @param taskName the name of the task
     * @return help text for the task
     * @throws Exception if task is not found
     */
    public String getTaskHelp(String taskName) throws Exception {
        Task task = Task.make(taskName);
        Worker worker = WorkerFactory.createWorker(task);
        
        if (worker != null) {
            return worker.getTaskSpecificHelp();
        } else {
            return "No help available for task: " + taskName;
        }
    }

    /**
     * Create a job from a parameter file.
     * @param parameterFile the parameter file
     * @return the created job
     * @throws Exception if job creation fails
     */
    public Job createJobFromFile(File parameterFile) throws Exception {
        return JobFactory.buildFromFile(parameterFile);
    }

    /**
     * Get the capabilities of all registered workers.
     * @return set of all available tasks
     */
    public Set<Task> getAllCapabilities() {
        return Task.getRegisteredTasks().stream()
                .collect(java.util.stream.Collectors.toSet());
    }
} 