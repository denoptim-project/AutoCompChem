package autocompchem.chemsoftware.orca;

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
 * Storage of predefined constants related to the use of Orca
 *
 * @author Marco Foscato
 */

public class OrcaConstants
{

	/**
	 * The separator used in Orca input files to identify the end of the input
	 * of a single job, and the beginning of the input of the next job.
	 */
	public static final String JOBSEPARATOR = "$new_job";
	
	/**
	 * Indentation used for nested input blocks
	 */
	public static final String INDENT = "  ";

	/**
	 * Extension for Orca input files
	 */
	public static final String INPEXTENSION = ".inp";
	
}
