package autocompchem.wiro.chem;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ParameterStorage;

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
    public final static String JDOPENBLOCK = "$START";

    /**
     * Label identifying the end of a multiline block
     * in job details files
     */
    public final static String JDCLOSEBLOCK = "$END";

	/**
	 * Label identifying the separation end of a comp.chem. software step
	 * and the beginning of another. Note that these steps are meant to be
	 * taken within the comp.chem. software.
	 */
	public static final String JDLABSTEPSEPARATOR = "----NEW-STEP----";
	
	/**
	 * Label identifying a definition of a task for ACC in job details files
	 */
	public static final String JDLABACCTASK = "$ACCTASK";
	
	/**
	 * Name of parameter defining a task  for ACC.
	 */
	public static final String JDACCTASK = "TASK";
    
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
	 * Key for parameter defining the pathname to molecular geometries
	 */
	public static final String PARGEOMFILE = "INPUTGEOMETRIESFILE";
	
	/**
	 * Key for parameter defining the molecular geometries as 
	 * {@link List} of {@link IAtomContainer}s.
	 */
	public static final String PARGEOM = "INPUTGEOMETRIES";

	/**
	 * Key for parameter defining how to handle multiple geometries
	 */
	public static final String PARMULTIGEOMMODE = "MULTIGEOMMODE";
	
	/**
	 * Key for parameter defining the names of all geometries given as input.
	 */
	public static final String PARGEOMNAMES = "MULTIGEOMNAMES";
	
	/**
	 * Key of parameter defining the format for reporting Cartesian coordinates.
	 */
	public static final String PARCARTCOORDSFORMAT = "CARTESIANCOORDSFORMAT";
	
	/**
	 * Key of parameter defining the format for reporting Internal coordinates.
	 */
	public static final String PARINTERNALCOORDSFORMAT = "INTERNALCOORDSFORMAT";
	
	/**
	 * Key for parameter defining the details
	 * of the computational chemistry job in terms of {@link ParameterStorage}.
	 */
	public static final String PARJOBDETAILS = "JOBDETAILS";
	
	/**
	 * Key for parameter defining the charge of the system.
	 */
	public static final String PARCHARGE = "CHARGE";
	
	/**
	 * Key for parameter defining the spin multiplicity of the system
	 */
	public static final String PARSPINMULT = "SPIN_MULTIPLICITY";
	
	/**
	 * Value-less parameter requiring to omit charge specification.
	 */
	public static final String PARNOCHARGE = "NOCHARGE";
	
	/**
	 * Value-less parameter requiring to omit spin multiplicity.
	 */
	public static final String PARNOSPIN = "NOSPIN";
	
	/**
	 * Possible values for how a molecular geometry can be reported
	 */
	public static enum CoordsType {XYZ, ZMAT};
	
	/**
	 * Key for the parameter defining which geometry to use when dealing with
	 * multiple geometries.
	 */
	public static final String PARMULTIGEOMID = "USEGEOMATINDEX";
	
	/**
	 * Key for the parameter reflecting the user's intention to focus on a 
	 * single molecular (sub) model to use when dealing 
	 * collections of multiple models.
	 */
	public static final String PARMODELID = "MODELID";
	
	/**
	 *  Key for the parameter requiring model specific behavior. This implies
	 *  things like altering output filenames to make them model-specific.
	 */
	public static final String PARMODELSPECIFIC = "MODELSPECIFIC";
	
	/**
	 * Parameter specifying which type of atom label to use. 
	 */
	public static final String PARATMLABELTYPE = "LABELTYPE";

	/**
	 * Key for parameter defining if and what suffix append when
	 * reporting the complete pathname in tasks {@link PARGETFILENAMEROOT}
	 */
	public static final String PARGETFILENAMEROOTSUFFIX = "SUFFIX";
	
	/**
	 * Key for parameter defining if and what quotation mark to use for
	 * reporting the complete pathname in tasks {@link PARGETFILENAMEROOT}
	 */
	public static final String PARGETFILENAMEROOTQUOTE = "QUOTATION";
	
	/**
	 * Key for parameter defining the reference name of a keyword generated in
	 * atom-specific fashion.
	 */
	public static final String KEYWORDNAME = "KEYWORDNAME";
	
	/**
	 * Key for parameter defining if a keyword generated in atom-specific 
	 * fashion is "loud" (i.e., is supposed to be reported using the pair 
	 * name:value) or not (i.e., only the value will be reported, but a name may
	 *  still be non-blank).
	 */
	public static final String KWISLOUD = "KWISLOUD";

	/**
	 * Key for parameter defining which type of pointer to use. Either "index" 
	 * or "label".
	 */
	public static final String ATMPOINTER = "ATMPOINTER";
	
	/**
	 * Key for parameter defining the type of coordinates used to define the
	 * chemical system.
	 */
	public static final String PARCOORDTYPE = "COORDTYPE";
	
	/**
	 * Name of directive meant to contain the geometry and related information.
	 */
	public static final String DIRGEOMETRY = "GEOMETRY";

	/**
	 * Key for parameter collecting analysis tasks for comp.chem. output files.
	 */
	public static final String PARANALYSISTASKS = "ANALYSISTASKS";

	/**
	 * Key for parameter requesting to print the last geometry from a
	 * comp.chem job
	 */
	public static final String PARPRINTLASTGEOM = "SAVELASTGEOMETRY";

	/**
	 * Key for parameter requesting to print the last geometry from each step of
	 * a comp.chem job
	 */
	public static final String PARPRINTLASTGEOMEACH = 
			"SAVELASTGEOMETRYOFEACHSTEP";

	/**
	 * Key for parameter requesting to print the last geometry from a
	 * comp.chem job
	 */
	public static final String PARPRINTALLGEOM = "SAVEALLGEOMETRIES";

	/**
	 * Key for parameter requesting to print vibrational modes
	 */
	public static final String PARPRINTVIBMODES = "SAVEVIBMODES";

	/**
	 * Key for parameter providing a file to be used as template connectivity
	 */
	public static final String PARTEMPLATECONNECTIVITY = 
			"TEMPLATECONNECTIVITY";
	
	/**
	 * Key for parameter defining the tolerance value
	 */
	public static final String PARBONDLENGTHTOLETANCE = "TOLERANCE";
	
	/**
	 * Key for parameter requesting to extract energy, the nature of which may 
	 * depend on the actual file (e.g., SCF, Strain).
	 */
	public static final String PARGETENERGY = "GETENERGY";

	/**
	 * Key for parameter requesting to calculate free energy
	 */
	public static final String PARGETFREEENERGY = "GETFREEENERGY";

	/**
	 * Key for parameter requesting analysis of the kind of critical point
	 * found on a potential energy surface (minimum vs. saddle point)
	 */
	public static final String PARCRITICALPOINTKIND = 
			"DETECTKINDOFCRITICALPOINT";
	
	/**
	 * Name for data storing the line number where a job begins.
	 */
	public static final String JOBDATAINITLINE = "INITLINE";
	
	/**
	 * Name for data storing the line number where a job ends
	 */
	public static final String JOBDATAENDLINE = "ENDLINE";

	/**
	 * Name of data flagging the successful end of a SCF optimisation
	 */
	public static final String JOBDATASCFCONVERGED = "SCFCONVERGENCE";

	/**
	 * Name of data storing the optimised SCF energies
	 */
	public static final String JOBDATASCFENERGIES = "SCFCONVENERGIES";

	/**
	 * Name of data storing the last SCF energies
	 */
	public static final String JOBDATAFINALSCFENERGY = "FINALSCFENERGY";

	/**
	 * Name of data storing the number of  SCF steps
	 */
	public static final String JOBDATASCFSTEPS = "SCFSTEPS";

	/**
	 * Name of data indicating that the geometry optimisation has converged
	 */
	public static final String JOBGEOMOPTCONVERGED = "GEOMOPTCONVERGED";
	
	/**
	 * Name of data storing the kind of critical point, if detected
	 */
	public static final String JOBDATACRITICALPOINTKIND = "CRITICALPOINTKIND";
	
	/**
	 * Name of data storing the vibrational frequencies
	 */
	public static final String JOBDATAVIBFREQ = "VIBFREQ";

	/**
	 * Name of data storing the normal modes
	 */
	public static final String JOBDATAVIBMODES = "VIBMOFES";

	/**
	 * Name of data collecting molecular geometries
	 */
	public static final String JOBDATAGEOMETRIES = "GEOMETRIES";

	/**
	 * Name of data containing Gibbs free energy already containing all
	 * the thermochemical corrections (a.u.).
	 */
	public static final String JOBDATAGIBBSFREEENERGY = "GIBBSFREEENERGY";
	
	/**
	 * Name of data containing quasi-harmonic recomputed Gibbs free energy 
	 * already containing all the thermochemical corrections (a.u.).
	 */
	public static final String JOBDATAQHGIBBSFREEENERGY = "QHGIBBSFREEENERGY";
	
	/**
	 * Name of data containing the temperature for thermochemical corrections.
	 */
	public static final String JOBDATTHERMOCHEM_TEMP = "THERMOCHEM_TEMP";

	/**
	 * Name of data containing the total entropy
	 */
	public static final String JOBDATTHERMOCHEM_S_TOT = "ENTROPY_TOT";
	
	/**
	 * Name of data containing the electronic entropy
	 */
	public static final String JOBDATTHERMOCHEM_S_ELECTR = "ENTROPY_ELE";

	/**
	 * Name of data containing the vibrational entropy
	 */
	//NB: the extra space in the string is needed!
	public static final String JOBDATTHERMOCHEM_S_VIB = "ENTROPY_VIB";

	/**
	 * Name of data containing the translational entropy
	 */
	//NB: the extra space in the string is needed!
	public static final String JOBDATTHERMOCHEM_S_TRANS = "ENTROPY_TRN";

	/**
	 * Name of data containing the rotational entropy
	 */
	public static final String JOBDATTHERMOCHEM_S_ROT = "ENTROPY_ROT";

	/**
	 * Name of data containing the total enthalpy
	 */
	public static final String JOBDATTHERMOCHEM_H = "ENTHALPY";

	/**
	 * Name of data containing the thermal correction to the enthalpy
	 */
	public static final String JOBDATTHERMOCHEM_H_CORR = "ENTHALPY_CORR";

	/**
	 * Name of data containing the zero point energy
	 */
	public static final String JOBDATTHERMOCHEM_ZPE = "ZPE";

	/**
	 * Name of data containing the vibrational correction to thermal energy
	 */
	public static final String JOBDATTHERMOCHEM_UCORR_VIB = "UCORR_VIB";

	/**
	 * Name of data containing the rotational correction to thermal energy
	 */
	public static final String JOBDATTHERMOCHEM_UCORR_ROT = "UCORR_ROT";

	/**
	 * Name of data containing the translational correction to thermal energy
	 */
	public static final String JOBDATTHERMOCHEM_UCORR_TRANS = "UCORR_TRN";
	
	/**
	 * Key of parameter generally used to provide specification of format.
	 * This keyword is given without any context, so it can be used anywhere.
	 */
	public static final String GENERALFORMAT = "FORMAT";
	
	/**
	 * Key of parameter generally used to provide specification of file name.
	 * This keyword is given without any context, so it can be used anywhere.
	 */
	public static final String GENERALFILENAME = "FILENAME";
	
	/**
	 * Key of parameter generally used to provide specification of one or more
	 * indexes.
	 * This keyword is given without any context, so it can be used anywhere.
	 */
	public static final String GENERALINDEXES = "INDEXES";

	/**
	 * Key of parameter specifying the threshold used for quasiharmonic
	 * calculation of thermochemical corrections.
	 */
	public static final String QHARMTHRSLD = "QHTHRESHOLD";
	
	/**
	 * Key of parameter specifying the threshold below which the we consider 
	 * any imaginary mode a read one. If |iv|&lt;val then iv becomes v
	 * when calculating quasi-harmonic vibrational component of entropy.
	 */
	public static final String QHARMTOREAL = "QHARMTOREAL";
	
	/**
	 * Key of parameters specifying the threshold below which we ignore 
	 * frequencies when calculating the quasi-harmonic vibrational entropy.
	 * 
	 */
	public static final String QHARMIGNORE = "QHARMIGNORE";
	
	/**
	 * Key of parameter specifying the smallest frequency considered to be
	 * non-zero.
	 */
	public static final String SMALLESTFREQ = "LOWESTFREQ";

	/**
	 * Key of flag signaling that the output of a job is scattered over
	 * multiple files.
	 */
	public static final String JOBOUTPUTONMULTIPLEFILES = "MULTIPLEOUTPUTFILES";

	/**
	 * Name of data containing the pathnames of additional files that contain
	 * the geometries.
	 */
	public static final String EXTERNALFILEGEOMETRIES = "EXTERNALGEOMSFILE";
	
}
