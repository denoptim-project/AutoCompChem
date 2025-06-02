package autocompchem.ui;

import org.springframework.boot.SpringApplication;

/**
 * Main launcher for AutoCompChem that can run in either CLI mode or Spring Boot server mode.
 * 
 * Usage:
 * - java -jar autocompchem.jar [normal CLI args] -> runs CLI mode via ACCMain
 * - java -jar autocompchem.jar --server -> runs Spring Boot web server
 * - java -jar autocompchem.jar --server --port=9090 -> runs server on port 9090
 * 
 * @author AutoCompChem Team
 */
public class AutoCompChemLauncher {
    
    public static void main(String[] args) {
        // Check if this is a server mode request
        if (args.length > 0 && isServerMode(args)) {
            // Remove --server from args and pass the rest to Spring Boot
            String[] springArgs = filterServerArgs(args);
            SpringApplication.run(AutoCompChemApplication.class, springArgs);
        } else {
            // Run in traditional CLI mode
            ACCMain.main(args);
        }
    }
    
    /**
     * Check if any argument indicates server mode
     */
    private static boolean isServerMode(String[] args) {
        for (String arg : args) {
            if ("--server".equals(arg) || "--web".equals(arg) || 
                "--spring-boot".equals(arg) || "--api".equals(arg)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Filter out server-specific arguments that shouldn't be passed to Spring Boot
     */
    private static String[] filterServerArgs(String[] args) {
        java.util.List<String> filteredArgs = new java.util.ArrayList<>();
        
        for (String arg : args) {
            // Skip our custom server mode flags
            if (!"--server".equals(arg) && !"--web".equals(arg) && 
                !"--spring-boot".equals(arg) && !"--api".equals(arg)) {
                filteredArgs.add(arg);
            }
        }
        
        return filteredArgs.toArray(new String[0]);
    }
} 