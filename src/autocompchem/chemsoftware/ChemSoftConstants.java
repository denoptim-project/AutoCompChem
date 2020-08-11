package autocompchem.chemsoftware;

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
 * Storage of predefined constants related to use of computational chemistry
 * software.
 *
 * @author Marco Foscato
 */

public class ChemSoftConstants
{
    /**
     * Label identifying commented out lines in job details files
     */
    public final static String JDCOMMENT = "#";
    
    /**
     * Separator for keyword:value pairs in job details files
     */
    public final static String JDKEYVALSEPARATOR = "=";

    /**
     * Separator for dataName:dataValue pairs in job details files
     */
    public final static String JDDATAVALSEPARATOR = "=";
    
    /**
     * Label for directive in job details files
     */
    public final static String JDLABDIRECTIVE = "$DIR_";

    /**
     * Label for task specific AutoCompChem parameters or tasks
     * defined in job details files
     */
    public final static String JDLABPARAMS = "$ACCPAR_";

    /**
     * Label for mute-key:value in job details files. 
     * Mute keys are keys that do not appear in
     * an input file of the comp.chem. software,
     *  only their value appears.
     */
    public final static String JDLABMUTEKEY = "$MT_";

    /** 
     * Label for loud-key:value in job details files. 
     * Loud keys are written in the input file 
     * of the comp.chem. software together with their value.
     */
    public final static String JDLABLOUDKEY = "$KV_";

    /**
     * Label for generic data in job details files
     */
    public final static String JDLABDATA = "$DATA_";

    /**
     * Label identifying the beginning of a multiline block
     * in job details files
     */
    public final static String JDLABOPENBLOCK = "$START";

    /**
     * Label identifying the end of a multiline block
     * in job details files
     */
    public final static String JDLABCLOSEBLOCK = "$END";
    
    /**
     * Lowest non-zero frequency (absolute value)
     */
    public final static double MINFREQ = 0.1;
    
    /**
     * Lowest value for non-zero frequencies
     */
    public final static double QHTHRSHFREQ = 0.0d;

    /**
     * Lowest value for imaginary modes
     */
    public final static double IMGFREQTHRSH = 0.0d;

    /**
     * Extension for computational chemistry input file
     */
	public static final String INPEXTENSION = ".inp";
	
    /**
     * Extension for computational chemistry job details file
     */
	public static final String JDEXTENSION = ".jd";

	/**
	 * Key for parameter defining the verbosity level
	 */
	public static final String PARVERBOSITY = "VERBOSITY";
	
	/**
	 * Key for parameter defining the pathname to molecular geometries
	 */
	public static final String PARGEOMFILE = "INPUTGEOMETRIESFILE";

	/**
	 * Key for parameter defining how to handle multiple geometries
	 */
	public static final String PARMULTIGEOMMODE = "MULTIGEOMMODE";
	
	/**
	 * Key for parameter defining the pathname to the file defining the details
	 * of the computational chemistry job.
	 */
	public static final String PARJOBDETAILS = "JOBDETAILS";

	/**
	 * Key for parameter defining the pathname's root for any output file (i.e.,
	 * the input for the computational chemistry software).
	 */
	public static final String PAROUTFILEROOT = "ROOTPATHNAME";
	
	/**
	 * Key for parameter defining the charge of the system.
	 */
	public static final String PARCHARGE = "CHARGE";
	
	/**
	 * Key for parameter defining the spin multiplicity of the system
	 */
	public static final String PARSPINMULT = "SPIN_MULTIPLICITY";
	
}
