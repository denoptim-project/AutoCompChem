package autocompchem.api.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData;
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
     * @return list of available task names (only non-test tasks)
     */
    public List<String> getAvailableTasks() {
        // Ensure WorkerFactory is initialized
        WorkerFactory.getInstance();
        
        List<Task> allTasks = Task.getRegisteredTasks();
        logger.debug("Found {} registered tasks total", allTasks.size());
        
        // Filter to only include tasks with testOnly=false and extract casedID
        List<String> availableTaskNames = allTasks.stream()
                .filter(task -> !task.testOnly)
                .map(task -> task.casedID)
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} non-test tasks", availableTaskNames.size());
        for (String taskName : availableTaskNames) {
            logger.debug("Available task: {}", taskName);
        }
        
        return availableTaskNames;
    }

    /**
     * Execute a task with given parameters.
     * @param taskName the name of the task to execute
     * @param parameters the parameters for the task
     * @return the result of the task execution including exposed output data
     * @throws Exception if task execution fails
     */
    public Map<String, Object> executeTask(String taskName, ParameterStorage parameters) throws Exception {
        // Add the task to parameters if not present
        if (!parameters.contains(WorkerConstants.PARTASK)) {
            parameters.setParameter(WorkerConstants.PARTASK, taskName);
        }

        // Create and run the job
        Job job = new ACCJob(parameters);
        job.run();

        // Capture the result
        Map<String, Object> result = new HashMap<>();
        result.put("status", "Task '" + taskName + "' completed successfully");
        
        // Get exposed output data
        Collection<NamedData> exposedData = job.getOutputDataSet();
        if (exposedData != null && !exposedData.isEmpty()) {
            Map<String, Object> outputData = new HashMap<>();
            logger.debug("Found {} exposed data items", exposedData.size());
            
            for (NamedData data : exposedData) {
                String key = data.getReference();
                Object value = data.getValue();
                outputData.put(key, value);
                logger.debug("Exposed data: {} = {}", key, value);
            }
            
            result.put("exposedData", outputData);
            result.put("exposedDataCount", exposedData.size());
        } else {
            logger.debug("No exposed data found");
            result.put("exposedData", new HashMap<>());
            result.put("exposedDataCount", 0);
        }

        return result;
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
     * @return set of all available task names (only non-test tasks)
     */
    public Set<String> getAllCapabilities() {
        return Task.getRegisteredTasks().stream()
                .filter(task -> !task.testOnly)
                .map(task -> task.casedID)
                .collect(java.util.stream.Collectors.toSet());
    }
} 