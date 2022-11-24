package autocompchem.chemsoftware.gaussian;

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
 * Storage of predefined constants for Gaussian related tools
 *
 * @author Marco Foscato
 */

public class GaussianConstants
{
    /**
     * Extension of Gaussian input files
     */
    public final static String GAUINPEXTENSION = ".inp";
    
    /**
     * Extension for Gaussian output files (i.e., log files from Gaussian)
     */
	public static final String OUTEXTENSION = ".out";

    /**
     * Extension of Gaussian job details file
     */
    public final static String JDEXTENSION = ".jd";

    //Keyword for lines of Link 0 section
    public final static String KEYLINKSEC = "LNKSECTION# ";

    //Keyword for lines of Route section
    public final static String KEYROUTESEC = "RUTSECTION# ";

    //Keyword for lines of title/comment
    public final static String KEYTITLESEC = "TITSECTION# ";

    //Keyword for lines of Molecular Specification section
    public final static String KEYMOLSEC = "MOLSECTION# ";

    //Root for keywords of lines of Options section
    public final static String KEYOPTSSEC = "OPTSECTION#";

    //Set with the keywords used in job details' definition
    public final static Set<String> JOBDETAILSKEYWORDS = new HashSet<String>(
                                Arrays.asList(
                                        KEYLINKSEC,
                                        KEYROUTESEC,
                                        KEYTITLESEC,
                                        KEYMOLSEC,
                                        KEYOPTSSEC));

    //subKeyword for print settings in Route Section
    public final static String SUBKEYPRINT = "$MTPRINT";

    //subKeyword for method in Route Section
    public final static String SUBKEYMODELMETHOD = "$MTMODEL_METHOD";
    public final static String SUBKEYMODELBASISET = "$MTMODEL_BASISSET";

    //subKeyword for job type in Route Section
    public final static String SUBKEYJOBTYPE = "$MTJOBTYPE";

/*
    //subKeyword for in Route Section
    public final static String = "";
*/

    //Step separator
    public final static String STEPSEPARATOR = "--Link1--";

    //Keyword for mute-key:value
    public final static String LABMUTEKEY = "$MT";

    //Keyword for loud-key:value
    public final static String LABLOUDKEY = "$KV";

    //Keyword for free text
    public final static String LABFREE = "$FR";

    /**
     * Keyword for task specific AutoCompChem parameters
     */
    public final static String LABPARAMS = "$ACCPAR_";

    //Number of gaussian option sections
//TODO this number should be set according to Gaussian manual
    public final static int MAXNUMOPTSECTIONS = 20;

    /**
     * Gaussian keyword controlling the source of the geometry
     */
    public final static String GAUKEYGEOM = "GEOM";

    /**
     * Gaussian keyword's option controlling the source of the geometry
     */
    public final static String GAUKEYGEOMCHECK = "CHECKPOINT";

    /**
     * Gaussian keyword's option controlling the source of the geometry
     */
    public final static String GAUKEYGEOMCHK = "CHECK";

    /**
     * Gaussian keyword's option controlling the source of the geometry
     */
    public final static String GAUKEYGEOMALLCHK = "ALLCHECK";

    /**
     * Gaussian keyword's option controlling the source of the geometry
     */
    public final static String GAUKEYGEOMSTEP = "STEP";

    /**
     * Gaussian keyword's option controlling the source of the geometry
     */
    public final static String GAUKEYGEOMNGEOM = "NGEOM";

    /**
     * Gaussian keyword's option controlling the source of the geometry
     */
    public final static String GAUKEYGEOMMOD = "MODIFY";

    /**
     * JobDetails keyword for recording the charge
     */
    public final static String CHARGEKEY = LABMUTEKEY + "CHARGE";

    /**
     * JobDetails keyword for recording the spin multiplicity
     */
    public final static String SPINMLTKEY = LABMUTEKEY + "SPINMLT";

    /**
     * JobDetails keyword for PCM options section
     */
    public final static String PCMOPTKEY = LABMUTEKEY + "PCM";

    /**
     * JobDetails keyword for basis set option section
     */
    public final static String BASISOPTKEY = LABMUTEKEY + "BASIS";
    
    /**
     * JobDetails keyword for modRedundant section
     */
    public final static String MODREDUNDANTKEY = LABMUTEKEY + "MODREDUNDANT";

    /**
     * String identifying the beginning of a geometry block.
     */
    public final static String OUTSTARTXYZ = 
                     "^ Center \\s* Atomic \\s* Atomic \\s*  Coordinates (.*)";

    /**
     * String identifying the end of a geometry block.
     */
    public final static String OUTENDXYZ = "^ Rotational constants(.*)";

    /**
     * String identifying end of a geometry optimization step. Either
     * converged or not converged
     */
    public final static String OUTENDGEOMOPTSTEP = "^\\s* Item \\s* Value \\s* Threshold  Converged?(.*)";

    /**
     * String identifying convergence of geometry optimization
     */
    public final static String OUTENDCONVGEOMOPTSTEP = "^ Optimization completed(.*)";

    /**
     * String identifying total DFT energy
     */
    public final static String OUTTOTDFTENERGY = "^\\s* SCF Done:  E(.*) =(.*)";

    /**
     * String identifying thermal correction to enthalpy
     */
    public final static String OUTCORRH = "^ Thermal correction to Enthalpy(.*)";

    /**
     * String identifying total entropy
     */
    public final static String OUTTOTS = "^ Total     (.*)";

    /**
     * String identifying translational entropy
     */
    public final static String OUTTRAS = "^ Translational      (.*)";

    /**
     * String identifying rotational entropy
     */
    public final static String OUTROTS = "^ Rotational       (.*)";

    /**
     * String identifying vibrational entropy
     */
    public final static String OUTVIBS = "^ Vibrational       (.*)";

    /**
     * String identifying temperature
     */
    public final static String OUTTEMP = "^ Temperature  (.*) Kelvin. (.*)";

    /**
     * String identifying projected frequencies
     */
    public final static String OUTPROJFREQ = "^ Frequencies -- (.*)";

    /**
     * Lowest non-zero frequency (absolute value)
     */
    public final static double MINFREQ = 0.1;

}
