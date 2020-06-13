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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Storage of predefined constants for shell commands
 *
 * @author Marco Foscato
 */

public class ShellJobConstants
{
    /**
     * Extension of shell input files
     */
    public final static String INPEXTENSION = ".sh";

    /**
     * Extension of shell job details file
     */
    public final static String JDEXTENSION = ".jd";

    /**
     * Step (i.e., task) separator for jobdetails files
     */
    public final static String TASKSEPARATORJD = "--NEW-TASK--";

    /**
     * Step (i.e., task) separator for input files
     */
    public final static String TASKSEPARATOR = "; ";

    /**
     * Separator for keyword:value pairs in jobdetails files
     */
    public final static String KEYVALSEPARATOR = "=";

    /**
     * Separator for dataName:dataValue pairs in jobdetails files
     */
    public final static String DATAVALSEPARATOR = "=";

    /**
     * Keyword for interpreter
     */
    public final static String LABINTERPRETER = "$EXE_";

    /**
     * Keyword for script
     */
    public final static String LABSCRIPT = "$SCRIPT_";

    /**
     * Keyword for arguments
     */
    public final static String LABARGS = "$ARGS_";

    /**
     * Keyword identifying the beginning of a multiline block
     */
    public final static String LABOPENBLOCK = "$OPENBLOCK";

    /**
     * Keyword identifying the end of a multiline block
     */
    public final static String LABCLOSEBLOCK = "$CLOSEBLOCK";

}
