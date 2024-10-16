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

import java.util.Date;

/**
 * Terminator has the power to kill the execution of a running job
 * usually returning an error message for the user and an exit status for the
 * machine.
 */

public class Terminator
{
	private static final String NL = System.getProperty("line.separator");
	
//------------------------------------------------------------------------------

    /**
     * Terminate execution with error message and specify exit status
     * @param message message to be printed in <code>stdout</code> and 
     * <code>stderr</code>
     * @param exitStatus exit status
     */

    public static void withMsgAndStatus(String message, int exitStatus)
    {
        Date date = new Date();
        String line = NL + NL + "**********************************************"
                + "*****************************"
                + NL + " " + date.toString() + NL + NL
                + " Termination status: " + exitStatus + NL
                + " Final message: " + message + NL + NL
                + " Thanks for using AutoCompChem." + NL
                + " Mandi! ;) " + NL;

        System.out.println(line);
        
//TODO add check if stdout and stderr are not the same
        if (exitStatus != 0)
        {
            System.err.println(line);
        }

        System.exit(exitStatus);
    }

//------------------------------------------------------------------------------

}

