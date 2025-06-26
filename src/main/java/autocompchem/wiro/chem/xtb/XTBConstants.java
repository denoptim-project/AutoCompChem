package autocompchem.wiro.chem.xtb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*   
 *   Copyright (C) 2021  Marco Foscato 
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
 * Storage of predefined constants related to the use of XTB
 *
 * @author Marco Foscato
 */

public class XTBConstants
{	
	/**
	 * Indentation used for nested input blocks
	 */
	public static final String INDENT = "  ";

	/**
	 * Extension for XTB input files
	 */
	public static final String INPEXTENSION = ".xcontrol";

	/**
	 * String identifying the normal termination of a single/multi job run. 
	 * In case of multi job runs, this in only found at the end of the last job.
	 */
	public static final String LOGNORMALTERM = 
			"\\* finished run on ";

	/**
	 * String identifying the single point energy at the end of a SP job
	 */
	public static final String LOGFINALSPENERGY = "\\| TOTAL ENERGY";

	/**
	 * String identifying a converged SCF optimisation
	 */
	public static final String LOGSCFSUCCESS = 
			"\\*\\*\\* convergence criteria satisfied after";

	/**
	 * The list of keywords requiring ':' instead of '=' as separator
	 */
	public static final Map<String,Set<String>> COLONSEPARATEDKEYS = 
			new HashMap<String,Set<String>>();
	
	static {
		COLONSEPARATEDKEYS.put("CONSTRAIN", new HashSet<String>(Arrays.asList(
				"ELEMENTS",
        		"ATOMS",
        		"DISTANCE",
        		"ANGLE",
        		"DIHEDRAL",
        		"CENTER",
        		"Z")));

		COLONSEPARATEDKEYS.put("FIX", new HashSet<String>(Arrays.asList(
				"ELEMENTS",
        		"ATOMS",
        		"FREEZE",
        		"SHAKE")));
		COLONSEPARATEDKEYS.put("HESS", new HashSet<String>(Arrays.asList(
				"ELEMENT MASS",
        		"ISOTOPE",
        		"MODIFY MASS",
        		"SCALE MASS")));
		COLONSEPARATEDKEYS.put("METADYN", new HashSet<String>(Arrays.asList(
				"ATOMS",
        		"RMSD",
        		"BIAS ATOMS",
        		"BIAS ELEMENTS")));
		COLONSEPARATEDKEYS.put("SCAN", new HashSet<String>(Arrays.asList(
				"INT",
        		"NAME")));
		COLONSEPARATEDKEYS.put("SPLIT", new HashSet<String>(Arrays.asList(
				"FRAGMENT1",
        		"FRAGMENT2",
        		"FRAGMENT")));
		COLONSEPARATEDKEYS.put("WALL", new HashSet<String>(Arrays.asList(
				"SPHERE",
        		"ELLIPSOID")));
	}

	/**
	 * String identifying the beginning of a single job step in the XTB log.
	 */
	public static final String LOGJOBSTEPSTART = " \\* started run on ";

	/**
	 * String identifying a converged geometry optimisation in the XTB log.
	 */
	public static final String LOGGEOMOPTCONVERGED = "\\*\\*\\* GEOMETRY "
			+ "OPTIMIZATION CONVERGED AFTER .* ITERATIONS \\*\\*\\*";
	
	/**
	 * String identifying the declaration of where is the optimized geometry
	 * in the XTB log
	 */
	public static final String LOGOPTGEOMFILENAME = 
			"optimized geometry written to";
	
	/**
	 * String identifying the declaration of where is the geometry trajectory
	 * of geometry optimization run is. This from the XTB log
	 */
	public static final String LOGOPTTRJFILENAME = 
			"Optimization log is written to";

	/**
	 * String identifying the list of vibrational frequencies
	 */
	public static final String LOGVIBFREQ = "\\| * Frequency Printout *\\|";
	
	/**
	 * String identifying the Gibbs free energy including 
	 * thermochemical corrections
	 */
	public static final String LOGGIBBSFREEENERGY = "\\| TOTAL FREE ENERGY";
	
	
	
	
	
}
