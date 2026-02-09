package autocompchem.worker;

/*   
 *   Copyright (C) 2020  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Storage of predefined constants related to Actions.
 *
 * @author Marco Foscato
 */

public class WorkerConstants
{
 
    /**
     * Key for parameter defining the task of the worker 
     */
    public static final String PARTASK = "TASK";
    
	/**
	 * Key for parameter defining the pathname to any input file.
	 */
	public static final String PARINFILE = "INFILE";

	/**
	 * Key for parameter defining the format of the input file.
	 * Usially not needed, but gives the change to specify the format of the input file
	 * irrespectivly on its extension.
	 */
	public static final String PARINFORMAT = "INFORMAT";
	
	/**
	 * Key for parameter defining the pathname to the main output file. If more
	 * output files are needed these are dealt with by the specific 
	 * {@link Worker} implementations.
	 */
	public static final String PAROUTFILE = "OUTFILE";
	
	/**
	 * Flag indicating that a worker should not warn the user when missing 
	 * the PAROUTFILE parameter
	 */
	public static final String PARNOOUTFILEMODE = "NOOUTFILEMODE";
	
	/**
	 * Key for parameter defining the pathname to an output file with raw data,
	 * typically not a molecular structure file. If more
	 * output files are needed these are dealt with by the specific 
	 * {@link Worker} implementations.
	 */
	public static final String PAROUTDATAFILE = "OUTDATAFILE";
	
	/**
	 * Key of parameter defining the format to be used for writing molecular 
	 * structure representations of processes atom containers.
	 */
	public static final String PAROUTFORMAT = "OUTFORMAT";

	/**
	 * Key of parameter defining to write to the output file only the last output 
	 * structure when sets of output structures are available.
	 */
	public static final String PARONLYLASTSTRUCTURE = "ONLYLASTSTRUCTURE";

}
