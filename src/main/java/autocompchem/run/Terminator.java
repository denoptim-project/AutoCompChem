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
 * machine.
 */

public class Terminator
{
	private static final String NL = System.getProperty("line.separator");
    
    /*
     * The logger of this class
     */
    private static Logger logger = LogManager.getLogger(
    		"autocompchem.run.Terminator");
	
 //------------------------------------------------------------------------------

    /**
     * Terminate execution with error message and specify exit status.
     * @param message the final message to be printed when closing the log.
     * @param exitStatus exit status
     * @param cuase a cause for which we print stack trace.
     */

    public static void withMsgAndStatus(String message, int exitStatus,
    		Throwable cuase)
    {
    	cuase.printStackTrace();
    	withMsgAndStatus(message, exitStatus);
    }
    
//------------------------------------------------------------------------------

    /**
     * Terminate execution with error message and specify exit status.
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
        if (exitStatus != 0)
        {
        	logger.fatal(msg);
        } else {
        	logger.info(msg);
        }
        System.exit(exitStatus);
    }

//------------------------------------------------------------------------------

}

