package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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

import java.util.Set;

/**
 * Storage of predefined constants for NWChem related tools
 *
 * @author Marco Foscato
 */

public class NWChemConstants
{
    /**
     * Extension of NWChem input files
     */
    public final static String NWCINPEXTENSION = ".nw";

    /**
     * Extension of NWChem job details file
     */
    public final static String JDEXTENSION = ".jd";

    /**
     * Step (i.e., task) separator for jobdetails files
     */
    public final static String TASKSEPARATORJD = "----END-OF-TASK----";

    /**
     * Step (i.e., task) separator for NWChem input files
     */
    public final static String TASKSEPARATORNW = "############";

    /**
     * Separator for keyword:value pairs in jobdetails files
     */
    public final static String KEYVALSEPARATOR = "=";

    /**
     * Separator for dataName:dataValue pairs in jobdetails files
     */
    public final static String DATAVALSEPARATOR = "=";

    /**
     * Indentation for subdirectives
     */
    public final static String SUBDIRECTIVEINDENT = "  ";
    
    /**
     * Keyword for directive
     */
    public final static String LABDIRECTIVE = "$DIR_";

    /**
     * Keyword for task specific AutoCompChem parameters
     */
    public final static String LABPARAMS = "$ACCPAR_";

    /**
     * Keyword for mute-key:value. Mute keys are keys that do not appear in
     * an NWChem input file, only their value does appear.
     */
    public final static String LABMUTEKEY = "$MT_";

    /** 
     * Keyword for loud-key:value. Loud keys are written in the NWChem input 
     * file together with their value.
     */
    public final static String LABLOUDKEY = "$KV_";

    /**
     * Keyword for generic data
     */
    public final static String LABDATA = "$DATA_";

    /**
     * Keyword identifying the beginning of a multiline block
     */
    public final static String LABOPENBLOCK = "$OPENBLOCK";

    /**
     * Keyword identifying the end of a multiline block
     */
    public final static String LABCLOSEBLOCK = "$CLOSEBLOCK";

    /**
     * String replacing space within directive's name in jobdetails
     */
    public final static String SPACEINDIRNAME = "@SPACE@";

    /**
     * Name of charge directive
     */
    public final static String CHARGEDIR = "CHARGE";

    /**
     * Name of directive holding the coordinates 
     */
    public final static String GEOMDIR = "GEOMETRY";

    /**
     * Name of directive holding the internal coordinates (Z-matrix)
     */
    public final static String ZMATDIR = "ZMATRIX";

    /**
     * Name of directive holding the user-defined internal coordinates (Z-coord)
     */
    public final static String ZCRDDIR = "ZCOORD";

    /**
     * Name of directive holding the basis set information
     */
    public final static String BASISDIR = "BASIS";

    /**
     * Name of directive holding the effective core potential information
     */
    public final static String ECPDIR = "ECP";

    /**
     * Name of directive controlling SCF calculations
     */
    public final static String SCFDIR = "SCF";
    
    /**
     * Name of directive controlling DFT calculations
     */
    public final static String DFTDIR = "DFT";
    
    /**
     * Directive defining the number of singly occupied orbitals (i.e., the 
     * number of unpaird electrons);
     */
	public static final String NOPENDIR = "NOPEN";

    /**
     * Name and header of the data block with variable internal coordinates
     */
    public final static String VARIABLEICBLOCK = "VARIABLES";

    /**
     * Name and header of the data block with constants internal coordinates
     */
    public final static String CONSTANTICBLOCK = "CONSTANTS";

    /**
     * Name of task directive 
     */
    public final static String TASKDIR = "TASK";

    /**
     * Name of title directive
     */
    public final static String TITLEDIR = "TITLE";

    /**
     * Name of start directive
     */
    public final static String STARTDIR = "START";

    /**
     * Name of start directive
     */
    public final static String RESTARTDIR = "RESTART";

    /**
     * Name of the keyword belonging to the GEOMETRY directive and that 
     * defines the name of the geometry.
     */
    public final static String GEOMNAMEKW = "geomname";

    /**
     * Name of keyword defining the name of the database file (i.e., the
     * equivalent of the checkpoint file). The keyword can belong to the
     * START and RESTART directives.
     */
    public final static String DBNAMEKW = "DBNAME";

    /**
     * Name of the keyword belonging to the TASK directive and that defines the 
     * theory in use (i.e., SCF and DFT), but can also be 'python' indicating 
     * an embedded python jobb.
     */
    public final static String THEORYKW = "theory";

    /** 
     * Name of the keyword belonging to the TASK directive and that defined the
     * type of task or operation (i.e., energy, gradient, hessian, optimize, 
     * saddle, frequencies, vscf, property, dynamics, thermodynamics)
     */
    public final static String OPERATIONKW = "tasktype";

    /**
     * Name of data block containing Cartesian coordinates
     */
    public final static String CARTCOORDS = "coords";

    /**
     * Name of the keyword defining the list of active atoms
     */
    public final static String ACTIVEATOMS = "geometry:actlist";

    /**
     * List of start-up directives that are placed at the beginning of a task
     */
    public final static Set<String> STARTUPTASKS = new HashSet<String>(
                                                  Arrays.asList("START",
                                                                "RESTART",
                                                                "SCRATCH_DIR",
                                                                "PERMANENT_DIR",
                                                                "MEMORY",
                                                                "ECHO"));

    /**
     * Max length of lines in NWChem input file. The actual limit declared by 
     * NWChem is 1023, but such a long line becomes difficult to read.
     */
    public final static int MAXLINELENGTH = 80;

    /**
     * Max number of concatenated lines  in NWChem input file
     */
    public final static int MAXCONCATLINES = 5;

    /**
     * String identifying the beginning of the output for a single task
     */
    public final static String OUTINITSTR = "NWChem Input Module";

    /**
     * String identifying the normal termination of a single task
     */
    public final static String OUTNORMALENDSTR = 
                          "Please cite the following reference when publishing";

    /**
     * String identifying the beginning of the initial geometry
     */
    public final static String OUTSTARTINITXYZ = "XYZ format geometry";

    /**
     * String identifying the end of the initial geometry
     */
    public final static String OUTENDINITXYZ = 
            "^ ===============================================================";

    /**
     * String identifying the beginning of a geometry block. This string
     * is used in NWChem as header of XYZ coordinates table for initial, 
     * intermediate, and final geometries, for instance, in the output of
     * a geometry optimization job. WARNING! Frequency-only jobs use a different
     * syntax.
     */
    public final static String OUTSTARTXYZ = 
                     "(.*) No\\. (.*) Tag (.*) Charge (.*) X (.*) Y (.*) Z(.*)";

    /**
     * String identifying the end of a geometry block. This string
     * is used in NWChem to terminate an XYZ coordinates table for initial,
     * intermediate, and final geometries, for instance, in the output of
     * a geometry optimization job. WARNING! Frequency-only jobs use a different
     * syntax.
     */
    public final static String OUTENDXYZ = "^\\s*$";

    /**
     * String identifying the beginning of a geometry block in the output of
     * a frequency/Hessian task. WARNING! Geometry optimization tasks use a 
     * different syntax.
     */
    public final static String OUTHESSTARTXYZ = 
                                 "(.*) atom (.*) X (.*) Y (.*) Z (.*) mass(.*)";

    /**
     * String identifying the end of a geometry block in the output of
     * a frequency/Hessian task. WARNING! Geometry optimization tasks use a
     * different syntax.
     */
    public final static String OUTHESENDXYZ = "^\\s*$";

    /**
     * String identifying end of a geometry optimization step. Both
     * converged and not converged
     */
    public final static String OUTENDGEOMOPTSTEP = "^\\s* Step       Energy      Delta E   Gmax     Grms     Xrms     Xmax   Walltime";

    /**
     * String identifying convergence of geometry optimization
     */
    public final static String OUTENDCONVGEOMOPTSTEP = "^\\s* Optimization converged";

    /**
     * String identifying total DFT energy
     */
    public final static String OUTTOTDFTENERGY = "^\\s* Total DFT energy =(.*)";

    /**
     * String identifying thermal correction to enthalpy
     */
    public final static String OUTCORRH = "^ Thermal correction to Enthalpy \\s*=(.*)";

    /**
     * String identifying total entropy
     */
    public final static String OUTTOTS = "^\\s* Total Entropy \\s*=(.*)";

    /**
     * String identifying translational entropy
     */
    public final static String OUTTRAS = "^\\s* - Translational                =(.*)";

    /**
     * String identifying rotational entropy
     */
    public final static String OUTROTS = "^\\s* - Rotational                   =(.*)";

    /**
     * String identifying vibrational entropy
     */
    public final static String OUTVIBS = "^\\s* - Vibrational                  =(.*)";

    /**
     * String identifying temperature
     */
    public final static String OUTTEMP = "^ Temperature \\s* =(.*)";

    /**
     * String identifying projected frequencies
     */
    public final static String OUTPROJFREQ = "^ P\\.Frequency (.*)";

    /**
     * Spin multiplicity as string for SCF directive
     */
    public final static ArrayList<String> SCFSPINMULT = new ArrayList<String>(
                                                        Arrays.asList("SINGLET",
                                                                      "DOUBLET",
                                                                      "TRIPLET",
                                                                      "QUARTET",
                                                                      "QUINTET",
                                                                       "SEXTET",
                                                                       "SEPTET",
                                                                      "OCTET"));

    /**
     * Lowest non-zero frequency (absolute value)
     */
    public final static double MINFREQ = 0.1;

    
}
