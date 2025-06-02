package autocompchem.api.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import autocompchem.api.service.AutoCompChemService;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.worker.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for AutoCompChem main functionality.
 * Provides endpoints for task management and execution.
 *
 * @author Marco Foscato
 */
@RestController
@RequestMapping("/api/v1/autocompchem")
@Tag(name = "AutoCompChem", description = "Main AutoCompChem computational chemistry operations")
public class AutoCompChemController {

    @Autowired
    private AutoCompChemService autoCompChemService;

    /**
     * Get information about the API.
     */
    @GetMapping("/info")
    @Operation(summary = "Get API information", description = "Returns basic information about the AutoCompChem API")
    public ResponseEntity<Map<String, String>> getInfo() {
        Map<String, String> info = Map.of(
            "name", "AutoCompChem REST API",
            "version", "3.0.0",
            "description", "RESTful interface for computational chemistry task automation"
        );
        return ResponseEntity.ok(info);
    }

    /**
     * Get all available tasks.
     */
    @GetMapping("/tasks")
    @Operation(summary = "Get available tasks", description = "Returns a list of all available computational tasks")
    public ResponseEntity<List<Task>> getAvailableTasks() {
        List<Task> tasks = autoCompChemService.getAvailableTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get help for a specific task.
     */
    @GetMapping("/tasks/{taskName}/help")
    @Operation(summary = "Get task help", description = "Returns help information for a specific task")
    public ResponseEntity<String> getTaskHelp(@PathVariable("taskName") String taskName) {
        try {
            String help = autoCompChemService.getTaskHelp(taskName);
            return ResponseEntity.ok(help);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting help for task: " + e.getMessage());
        }
    }

    /**
     * Execute a computational task.
     */
    @PostMapping("/tasks/{taskName}/execute")
    @Operation(summary = "Execute task", description = "Executes a computational chemistry task with given parameters")
    public ResponseEntity<String> executeTask(
            @PathVariable("taskName") String taskName,
            @RequestBody Map<String, Object> parameters) {
        try {
            // Convert map to ParameterStorage
            ParameterStorage params = new ParameterStorage();
            parameters.forEach((key, value) -> params.setParameter(key, value.toString()));
            
            String result = autoCompChemService.executeTask(taskName, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error executing task: " + e.getMessage());
        }
    }

    /**
     * Get all worker capabilities.
     */
    @GetMapping("/capabilities")
    @Operation(summary = "Get worker capabilities", description = "Returns all capabilities of registered workers")
    public ResponseEntity<Set<Task>> getAllCapabilities() {
        Set<Task> capabilities = autoCompChemService.getAllCapabilities();
        return ResponseEntity.ok(capabilities);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the service")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = Map.of(
            "status", "UP",
            "service", "AutoCompChem API"
        );
        return ResponseEntity.ok(health);
    }
} 