package autocompchem.run;


/*
 *   Copyright (C) 2014  Marco Foscato
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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.utils.TimeUtils;

/**
 * Terminator has the power to kill the execution of a running job
 * usually returning an error message for the user and an exit status for the
 * machine. In web service contexts, it throws exceptions instead of 
 * terminating the entire JVM.
 */

public class Terminator
{
	private static final String NL = System.getProperty("line.separator");
    
    /*
     * The logger of this class
     */
    private static Logger logger = LogManager.getLogger(
    		"autocompchem.run.Terminator");

    /**
     * Custom exception for web service contexts to avoid System.exit()
     */
    public static class TaskTerminationException extends RuntimeException {
        private final int exitStatus;
        
        public TaskTerminationException(String message, int exitStatus) {
            super(message);
            this.exitStatus = exitStatus;
        }
        
        public TaskTerminationException(String message, int exitStatus, Throwable cause) {
            super(message, cause);
            this.exitStatus = exitStatus;
        }
        
        public int getExitStatus() {
            return exitStatus;
        }
    }
    
    /**
     * Check if we're running in a web service context (Spring Boot)
     * @return true if running as a web service, false if CLI
     */
    private static boolean isWebServiceContext() {
        try {
            // Check if Spring application context is available
            Class.forName("org.springframework.boot.SpringApplication");
            
            // Check if we're in a web environment by looking for servlet context
            Thread currentThread = Thread.currentThread();
            String threadName = currentThread.getName();
            
            // Spring Boot uses threads like "http-nio-8080-exec-1" for web requests
            if (threadName.contains("http-nio") || threadName.contains("tomcat") 
                || threadName.contains("jetty") || threadName.contains("reactor")) {
                return true;
            }
            
            // Also check stack trace for Spring web components
            StackTraceElement[] stackTrace = currentThread.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (className.contains("org.springframework.web") 
                    || className.contains("org.springframework.boot.web")
                    || className.contains("javax.servlet")
                    || className.contains("jakarta.servlet")
                    || className.contains("autocompchem.api")) {
                    return true;
                }
            }
            
            return false;
        } catch (ClassNotFoundException e) {
            // Spring not available, definitely CLI context
            return false;
        }
    }
	
 //------------------------------------------------------------------------------

    /**
     * Terminate execution with error message and specify exit status.
     * @param message the final message to be printed when closing the log.
     * @param exitStatus exit status
     * @param cause a cause for which we print stack trace.
     */

    public static void withMsgAndStatus(String message, int exitStatus,
    		Throwable cause)
    {
    	cause.printStackTrace();
    	withMsgAndStatus(message, exitStatus);
    }
    
//------------------------------------------------------------------------------

    /**
     * Terminate execution with error message and specify exit status.
     * In web service contexts, throws TaskTerminationException instead of System.exit().
     * @param message the final message to be printed when closing the log.
     * @param exitStatus exit status
     */

    public static void withMsgAndStatus(String message, int exitStatus)
    {
        String msg = TimeUtils.getTimestampLine() + NL
                + "Termination status: " + exitStatus + NL
                + "Final message: " + message + NL 
                + "Thanks for using AutoCompChem." + NL
                + "Mandi! ;) ";
        
        if (isWebServiceContext()) {
            // In web service context, log and throw exception instead of System.exit()
            if (exitStatus != 0) {
                logger.error(msg);
                throw new TaskTerminationException(message, exitStatus);
            } else {
                logger.info(msg);
                // For successful termination in web context, we don't throw exception
                // just log the completion message
            }
        } else {
            // In CLI context, use original behavior
            if (exitStatus != 0) {
                logger.fatal(msg);
            } else {
                logger.info(msg);
            }
            System.exit(exitStatus);
        }
    }

//------------------------------------------------------------------------------

}

