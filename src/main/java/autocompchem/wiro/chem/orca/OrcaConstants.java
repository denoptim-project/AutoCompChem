package autocompchem.wiro.chem.orca;

import autocompchem.wiro.chem.Directive;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
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
	 * Key for parameter requesting to use the old "new_job" syntax 
	 * (declared deprecated from version Orca 6) for
	 * reporting multiple steps in a single Orca input file.
	 */
	public static final String PARNEWJOBSYNTAX = "USENEWJOBSYNTAX";
	
	/**
	 * The old (declared deprecated from version Orca 6) separator used in Orca 
	 * input files to identify the end of the input
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
	
	/**
	 * String identifying the total number of jobs processed in one Orca run
	 */
	public static final String LOGTOTJOBSTEPS = 
			" JOBS TO BE PROCESSED THIS RUN ";

	/**
	 * String identifying the beginning of a single Orca job in a run that
	 * includes more than one job.
	 */
	public static final String LOGJOBSTEPSTART = "  JOB NUMBER ";

	/**
	 * String identifying the normal termination of a single/multi job run. 
	 * In case of multi job runs, this in only found at the end of the last job.
	 */
	public static final String LOGNORMALTERM = 
			"\\*\\*\\*\\*ORCA TERMINATED NORMALLY\\*\\*\\*\\*";

	/**
	 * String identifying the single point energy at the end of a SP job
	 */
	public static final String LOGFINALSPENERGY = "FINAL SINGLE POINT ENERGY";

	/**
	 * String identifying a converged SCF optimisation
	 */
	public static final String LOGSCFSUCCESS = "SCF CONVERGED AFTER";

	/**
	 * String identifying the beginning of the current geometry
	 */
	public static final String JOBLOGCURRENTGEOMETRY = 
			"CARTESIAN COORDINATES \\(ANGSTROEM\\)";
	
	/**
	 * String identifying a converged geometry optimisation
	 */
	public static final String LOGGEOMOPTCONVERGED = 
			"OPTIMIZATION HAS CONVERGED";
	
	/**
	 * String identifying the list of vibrational frequencies
	 */
	public static final String LOGVIBFREQ = "VIBRATIONAL FREQUENCIES";

	/**
	 * String identifying the list of normal modes
	 */
	public static final String LOGVIBMODES = "NORMAL MODES";

	/**
	 * String identifying the Gibbs free energy including 
	 * thermochemical corrections
	 */
	public static final String LOGGIBBSFREEENERGY = "Final Gibbs free energy ";
	
	/**
	 * String identifying the electronic entropy 
	 */
	public static final String LOGTHERMOCHEM_S_ELECTR = "Electronic entropy ";
	
	/**
	 * String identifying the vibrational entropy
	 */
	//NB: the extra space in the string is needed!
	public static final String LOGTHERMOCHEM_S_VIB = "Vibrational entropy  ";
	
	/**
	 * String identifying the translational entropy
	 */
	//NB: the extra space in the string is needed!
	public static final String LOGTHERMOCHEM_S_TRANS = "Rotational entropy  ";
	
	/**
	 * String identifying the rotational entropy
	 */
	public static final String LOGTHERMOCHEM_S_ROT = "Translational entropy ";
	
	/**
	 * String identifying the total enthalpy
	 */
	public static final String LOGTHERMOCHEM_H = "Total Enthalpy ";
	
	/**
	 * String identifying the zero point energy
	 */
	public static final String LOGTHERMOCHEM_ZPE = "Zero point energy ";
	
	/**
	 * String identifying the vibrational correction to thermal energy
	 */
	public static final String LOGTHERMOCHEM_UCORR_VIB = 
			"Thermal vibrational correction ";
	
	/**
	 * String identifying the rotational correction to thermal energy
	 */
	public static final String LOGTHERMOCHEM_UCORR_ROT = 
			"Thermal rotational correction ";
	
	/**
	 * String identifying the translational correction to thermal energy
	 */
	public static final String LOGTHERMOCHEM_UCORR_TRANS = 
			"Thermal translational correction ";

	/**
	 * String identifying the line defining the namespace for Orca-xTB output
	 * WARNING: this is controlled by Grimme's xTB software, not by Orca!
	 */
	public static final String LOGJOBEXTNAMESPACE = 
			"calculation namespace ";
	
	/**
	 * Name of the 'compound' {@link Directive} defining the steps of a 
	 * compound job.
	 */
	public static final String COMPOUNDDIRNAME = "Compound";	
	
	/**
	 * Tail of the 'compound' {@link Directive} defining the steps of a 
	 * compound job.
	 */
	public static final String COMPOUNDEND = "end";
	
	/**
	 * Header (practically a directive name) of the step defining block in a 
	 * compound job.
	 */
	public static final String COMPOUNDSTEPSTART = "New_Step";
	
	/**
	 * Tail of the step defining block in a compound job.
	 */
	public static final String COMPOUNDSTEPEND = "Step_end";
	
	/**
	 * Name of the 'coords' {@link Directive} defining the chemical system.
	 */
	public static final String COORDSDIRNAME = "COORDS";
	
	/**
	 * Name of the sub{@link Directive} of 'coords' defining the type of 
	 * coordinates defining the chemical system.
	 */
	public static final String COORDSCTYPDIRNAME = "CTYP";
	

	/**
	 * Name of the {@link Directive} defining the constraints applied to the 
	 * chemical system.
	 */
	public static final String CONSTRAINTSDIRNAME = "CONSTRAINTS";
	
	/**
	 * Name of the sub{@link Directive} of 'coords' defining the charge of
	 * the chemical system.
	 */
	public static final String COORDSCHARGEDIRNAME = "CHARGE";
	
	/**
	 * Name of the sub{@link Directive} of 'coords' defining the spin
	 * multiplicity of
	 * the chemical system.
	 */
	public static final String COORDSMULTDIRNAME = "MULT";
	

	/**
	 * Name of the '*' {@link Directive} defining the chemical system.
	 */
	public static final String STARDIRNAME = "*";

	/**
	 * Name of the {@link Directive} defining the basis set.
	 */
	public static final String BASISSETDIRNAME = "BASIS";	

}
