package autocompchem.api.controller;

import java.util.Map;

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
import autocompchem.worker.WorkerConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for computational chemistry software integrations.
 * Provides endpoints for generating input files and reading output files
 * for various quantum chemistry packages.
 *
 * @author Marco Foscato
 */
@RestController
@RequestMapping("/api/v1/compchem")
@Tag(name = "Computational Chemistry", description = "Integration with quantum chemistry software packages")
public class ComputationalChemistryController {

    @Autowired
    private AutoCompChemService autoCompChemService;

    /**
     * Get supported computational chemistry software.
     */
    @GetMapping("/software")
    @Operation(summary = "Get supported software", description = "Returns list of supported computational chemistry software packages")
    public ResponseEntity<Map<String, Object>> getSupportedSoftware() {
        Map<String, Object> software = Map.of(
            "inputWriters", Map.of(
                "gaussian", "prepareInputGaussian",
                "orca", "prepareInputOrca", 
                "nwchem", "prepareInputNWChem",
                "xtb", "prepareInputXTB",
                "spartan", "prepareInputSpartan"
            ),
            "outputReaders", Map.of(
                "gaussian", "readGaussianOutput",
                "orca", "readOrcaOutput",
                "nwchem", "readNWChemOutput", 
                "xtb", "readXTBOutput",
                "spartan", "readSpartanOutput"
            )
        );
        return ResponseEntity.ok(software);
    }

    /**
     * Generate input file for computational chemistry software.
     */
    @PostMapping("/software/{softwareName}/input")
    @Operation(summary = "Generate input file", description = "Generate input file for specified quantum chemistry software")
    public ResponseEntity<String> generateInputFile(
            @PathVariable String softwareName,
            @RequestBody Map<String, Object> parameters) {
        try {
            // Map software name to task
            String taskName = getInputTask(softwareName);
            if (taskName == null) {
                return ResponseEntity.badRequest().body("Unsupported software: " + softwareName);
            }

            // Convert map to ParameterStorage
            ParameterStorage params = new ParameterStorage();
            parameters.forEach((key, value) -> params.setParameter(key, value.toString()));
            params.setParameter(WorkerConstants.PARTASK, taskName);

            String result = autoCompChemService.executeTask(taskName, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating input: " + e.getMessage());
        }
    }

    /**
     * Read output file from computational chemistry software.
     */
    @PostMapping("/software/{softwareName}/output")
    @Operation(summary = "Read output file", description = "Parse output file from specified quantum chemistry software")
    public ResponseEntity<String> readOutputFile(
            @PathVariable String softwareName,
            @RequestBody Map<String, Object> parameters) {
        try {
            // Map software name to task
            String taskName = getOutputTask(softwareName);
            if (taskName == null) {
                return ResponseEntity.badRequest().body("Unsupported software: " + softwareName);
            }

            // Convert map to ParameterStorage
            ParameterStorage params = new ParameterStorage();
            parameters.forEach((key, value) -> params.setParameter(key, value.toString()));
            params.setParameter(WorkerConstants.PARTASK, taskName);

            String result = autoCompChemService.executeTask(taskName, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error reading output: " + e.getMessage());
        }
    }

    /**
     * Get job evaluation capabilities.
     */
    @GetMapping("/job/evaluation")
    @Operation(summary = "Get job evaluation info", description = "Returns information about job evaluation capabilities")
    public ResponseEntity<Map<String, Object>> getJobEvaluationInfo() {
        Map<String, Object> info = Map.of(
            "capabilities", Map.of(
                "evaluateJob", "Evaluate completed computational chemistry jobs",
                "cureJob", "Attempt to fix/heal problematic jobs"
            ),
            "description", "Job evaluation and healing capabilities"
        );
        return ResponseEntity.ok(info);
    }

    /**
     * Evaluate a computational chemistry job.
     */
    @PostMapping("/job/evaluate")
    @Operation(summary = "Evaluate job", description = "Evaluate the results of a computational chemistry job")
    public ResponseEntity<String> evaluateJob(@RequestBody Map<String, Object> parameters) {
        try {
            ParameterStorage params = new ParameterStorage();
            parameters.forEach((key, value) -> params.setParameter(key, value.toString()));
            
            String result = autoCompChemService.executeTask("evaluateJob", params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error evaluating job: " + e.getMessage());
        }
    }

    /**
     * Helper method to map software name to input task.
     */
    private String getInputTask(String softwareName) {
        return switch (softwareName.toLowerCase()) {
            case "gaussian" -> "prepareInputGaussian";
            case "orca" -> "prepareInputOrca";
            case "nwchem" -> "prepareInputNWChem";
            case "xtb" -> "prepareInputXTB";
            case "spartan" -> "prepareInputSpartan";
            default -> null;
        };
    }

    /**
     * Helper method to map software name to output task.
     */
    private String getOutputTask(String softwareName) {
        return switch (softwareName.toLowerCase()) {
            case "gaussian" -> "readGaussianOutput";
            case "orca" -> "readOrcaOutput";
            case "nwchem" -> "readNWChemOutput";
            case "xtb" -> "readXTBOutput";
            case "spartan" -> "readSpartanOutput";
            default -> null;
        };
    }
} 