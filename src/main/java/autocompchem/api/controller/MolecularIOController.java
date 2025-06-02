package autocompchem.api.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import autocompchem.api.service.AutoCompChemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for molecular file I/O operations.
 * Handles reading, writing, and converting molecular structure files.
 *
 * @author Marco Foscato
 */
@RestController
@RequestMapping("/api/v1/molecules")
@Tag(name = "Molecular I/O", description = "Operations for reading, writing, and converting molecular structure files")
public class MolecularIOController {

    @Autowired
    private AutoCompChemService autoCompChemService;

    private final String tempDir = System.getProperty("java.io.tmpdir");

    /**
     * Upload and parse a molecular structure file.
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload molecular file", description = "Upload and parse a molecular structure file (SDF, XYZ, etc.)")
    public ResponseEntity<Map<String, Object>> uploadMolecularFile(
            @RequestParam("file") MultipartFile file) {
        try {
            // Save uploaded file temporarily
            Path tempFilePath = Paths.get(tempDir, file.getOriginalFilename());
            Files.write(tempFilePath, file.getBytes());
            File tempFile = tempFilePath.toFile();

            // Read molecular structures
            List<IAtomContainer> molecules = autoCompChemService.readMolecularStructures(tempFile);

            // Clean up temp file
            tempFile.delete();

            Map<String, Object> response = Map.of(
                "filename", file.getOriginalFilename(),
                "moleculeCount", molecules.size(),
                "message", "File processed successfully"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "error", "Failed to process file: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Convert molecular file format.
     */
    @PostMapping("/convert")
    @Operation(summary = "Convert file format", description = "Convert molecular structure file from one format to another")
    public ResponseEntity<Resource> convertMolecularFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("outputFormat") String outputFormat) {
        try {
            // Save uploaded file temporarily
            Path tempInputPath = Paths.get(tempDir, file.getOriginalFilename());
            Files.write(tempInputPath, file.getBytes());
            File tempInputFile = tempInputPath.toFile();

            // Read molecular structures
            List<IAtomContainer> molecules = autoCompChemService.readMolecularStructures(tempInputFile);

            // Create output file
            String outputFileName = getBaseName(file.getOriginalFilename()) + "." + outputFormat.toLowerCase();
            Path tempOutputPath = Paths.get(tempDir, outputFileName);
            File tempOutputFile = tempOutputPath.toFile();

            // Write in new format
            autoCompChemService.writeMolecularStructures(molecules, tempOutputFile, outputFormat, false);

            // Clean up input file
            tempInputFile.delete();

            // Return converted file
            Resource resource = new UrlResource(tempOutputPath.toUri());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get supported file formats.
     */
    @GetMapping("/formats")
    @Operation(summary = "Get supported formats", description = "Returns list of supported molecular file formats")
    public ResponseEntity<Map<String, Object>> getSupportedFormats() {
        Map<String, Object> formats = Map.of(
            "input", List.of("sdf", "xyz", "mol", "pdb"),
            "output", List.of("sdf", "xyz", "orcatrajectory"),
            "description", "Supported molecular file formats for input and output"
        );
        return ResponseEntity.ok(formats);
    }

    /**
     * Get molecule count from uploaded file.
     */
    @PostMapping("/count")
    @Operation(summary = "Count molecules", description = "Count the number of molecules in an uploaded file")
    public ResponseEntity<Map<String, Object>> countMolecules(
            @RequestParam("file") MultipartFile file) {
        try {
            // Save uploaded file temporarily
            Path tempFilePath = Paths.get(tempDir, file.getOriginalFilename());
            Files.write(tempFilePath, file.getBytes());
            File tempFile = tempFilePath.toFile();

            // Read molecular structures
            List<IAtomContainer> molecules = autoCompChemService.readMolecularStructures(tempFile);

            // Clean up temp file
            tempFile.delete();

            Map<String, Object> response = Map.of(
                "filename", file.getOriginalFilename(),
                "moleculeCount", molecules.size(),
                "fileSize", file.getSize()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "error", "Failed to count molecules: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Helper method to get base name of a file (without extension).
     */
    private String getBaseName(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
} 