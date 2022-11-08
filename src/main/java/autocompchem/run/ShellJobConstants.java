package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
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

/**
 * Storage of predefined constants for shell commands
 *
 * @author Marco Foscato
 */

public class ShellJobConstants
{
	/**
	 * Keyword for the parameter defining an entire command
	 */
	public final static String LABCOMMAND = "CMD";
	
    /**
     * Keyword for interpreter
     */
    public final static String LABINTERPRETER = "EXE";

    /**
     * Keyword for script
     */
    public final static String LABSCRIPT = "SCRIPT";

    /**
     * Keyword for arguments
     */
    public final static String LABARGS = "ARGS";

    /**
     * Keyword for requesting to run the command from a customised location
     */
	public static final String WORKDIR = "WORKDIR";

	/**
	 * Keyword requesting to copy some pathnames to the location from which the 
	 * command will be executed, which could be "." or anywhere according to the
	 * 
	 */
	public static final String COPYTOWORKDIR = "COPYTOWORKDIR";
	

}
