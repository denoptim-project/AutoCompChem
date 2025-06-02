package autocompchem.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import autocompchem.worker.WorkerFactory;

/**
 * Spring Boot main application class for AutoCompChem REST API.
 * This class initializes the web server and makes the computational chemistry
 * functionality available via REST endpoints.
 *
 * @author Marco Foscato
 */
@SpringBootApplication
@ComponentScan(basePackages = "autocompchem")
public class AutoCompChemApplication {

    public static void main(String[] args) {
        // Initialize the worker registry before starting Spring Boot
        WorkerFactory.getInstance();
        
        SpringApplication.run(AutoCompChemApplication.class, args);
    }
} 