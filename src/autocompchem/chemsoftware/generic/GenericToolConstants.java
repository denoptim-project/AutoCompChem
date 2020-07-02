package autocompchem.chemsoftware.generic;

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

/**
 * Storage of predefined constants for GenericTool related tools.
 *
 * @author Marco Foscato
 */

public class GenericToolConstants
{
   /**
    * Keyword for the parameter specifying the verbosity level
    */
   public final static String VERBOSITYKEY = "VERBOSITY";

   /**
    * Keyword for the parameter specifying the main input file
    */
   public final static String INPUTFILENAMEKEY = "INFILE";

   /**
    * Keyword for the parameter specifying the root of the known errors' tree
    */
   public final static String ERRORTREEROOTKEY = "GENERICERRORSROOT";

    /**
     * Keyword for customising the string used to identify the beginning of 
     * a single task
     */
    public final static String DEFINITMSGKEY = "DEFINITMSG";

    /**
     * Default value for message indicating the beginning of a step
     */
    public final static String DEFINITMSG = "BEGINNING OF TASK";

    /**
     * Keyword for customising the string used to identify the normal completion
     * of a single task
     */
    public final static String DEFNORMENDMSGKEY = "DEFNORMENDMSG";

    /**
     * Default value for message indicating the normal termination of a task
     */
    public final static String DEFNORMENDMSG = "TASK TERMINATED NORMALLY";
 
    /**
     * Step (i.e., task) separator for jobdetails files
     */
    public final static String TASKSEPARATORJD = "----END-OF-TASK----";

    /**
     * Keyword for mute-key:value. Mute keys are keys that do not appear in
     * an GenericTool input file, only their value does appear.
     */
    public final static String LABMUTEKEY = "$MT_";

    /**
     * Keyword for loud-key:value. 
     * Loud keys are written in the GenericTool input
     * file together with their value.
     */
    public final static String LABLOUDKEY = "$KV_";

    /**
     * Keyword identifying the beginning of a multi line block
     */
    public final static String LABOPENBLOCK = "$OPENBLOCK";

    /**
     * Keyword identifying the end of a multi line block
     */
    public final static String LABCLOSEBLOCK = "$CLOSEBLOCK";

}
