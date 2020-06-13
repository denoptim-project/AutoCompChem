package autocompchem.run;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Date;

/**
 * <code>Terminator</code> has the power to kill the execution of a running job
 * usually returning an error message for the user and an exit status for the
 * machine.
 * @author Marco Foscato
 */

public class Terminator
{

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
        String line = "\n\n**********************************************"
                + "*****************************"
                + "\n " + date.toString()
                + "\n\n Termination status: " + exitStatus
                + "\n Final message: " + message 
                + "\n\n Thanks for using Foscato's scripts and software."
                + "\n Mandi! ;) \n";

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

