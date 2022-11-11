package autocompchem.chemsoftware.qmmm;

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
 * Storage of predefined constants for QMMM (the software) and related tools
 *
 * @author Marco Foscato
 */

public class QMMMConstants
{
    /**
     * Extension of QMMM input files
     */
    public final static String QMMMINPEXTENSION = ".dat";

    /**
     * Extension of QMMM input coordinates file
     */
    public final static String QMMMCRDEXTENSION = ".crd";

    /**
     * Maximum length of text lines
     */
    public final static int MAXLINELENGTH = 80;

    /**
     * Maximum number of lines in title list
     */
    public final static int MAXTITLELINES = 5;

    /**
     * Keyword for section
     */
    public final static String LABSECTION = "$SEC_";

    /**
     * Keyword for mute-key:value (also known as 'switches' in QMMM manual). 
     * Mute keys are keys that do not appear in
     * a QMMM input file, only their value is printed.
     */
    public final static String LABMUTEKEY = "$MT_";

    /** 
     * Keyword for loud-key:value (also known as 'variables' in QMMM manual). 
     * Loud keys are written in the QMMM input 
     * file together with their value.
     */
    public final static String LABLOUDKEY = "$KV_";

    /**
     * Keyword for generic data
     */
    public final static String LABDATA = "$LIST_";

    /**
     * Separator for keyword:value pairs in jobdetails files
     */
    public final static String KEYVALSEPARATOR = "=";

    /**
     * Separator for dataName:dataValue pairs in jobdetails files
     */
    public final static String DATAVALSEPARATOR = "=";

    /**
     * Indentation for subsections
     */
    public final static String SUBSECTIONINDENT = "  ";

    /**
     * Keyword identifying the beginning of a multi line block
     */
    public final static String LABOPENBLOCK = "$OPENBLOCK";

    /**
     * Keyword identifying the end of a multi line block
     */
    public final static String LABCLOSEBLOCK = "$CLOSEBLOCK";

    /**
     * Name of main section MULTIGEN
     */
    public final static String MULTIGENSEC = "*MULTIGEN";

    /**
     * Name of main section MULTIOPT
     */
    public final static String MULTIOPTSEC = "*MULTIOPT";

    /**
     * Name of main section EXTOPT
     */
    public final static String EXTOPTSEC = "*EXTOPT";

    /**
     * Name of main section QMMM
     */
    public final static String QMMMSEC = "*QM/MM";

    /**
     * Name of main section TEST
     */
    public final static String TESTSEC = "*TEST";

    /**
     * Name of main section TESTMM
     */
    public final static String TESTMMSEC = "*TESTMM";

    /**
     * List of master sections
     */
    public final static Set<String> MASTERSECLST = new HashSet<String>(Arrays.asList(
                                                                    MULTIGENSEC,
                                                                    MULTIOPTSEC,
                                                                    EXTOPTSEC,
                                                                    QMMMSEC,
                                                                    TESTSEC,
                                                                    TESTMMSEC));

    /**
     * Name of list holding the coordinates
     */
    public final static String GEOMLIST = "GEOM";

    /**
     * Name of the list contaning the title
     */
    public final static String TITLELIST = "TITLE";

    /**
     * Name of keyword defining crd file format
     */
    public final static String COORDFORMATKEY = "MMVALEN";

    /**
     * Name of the keyword defining the theoretical model
     */
    public final static String MODELKEY = "CALMODEL";

    /**
     * Name of the keyword defining the number of atoms
     */
    public final static String NATOMSKEY = "NATOMS";

    /**
     * Name of the keyword defining the charge
     */
    public final static String CHARGEKEY = "CHARGE";

    /**
     * Name of the keyword defining the spin multiplicity
     */
    public final static String SPINMLTKEY = "MULTIPLICITY";

    /**
     * Name of atoms' property used to store the numerical representation of
     * atom type
     */
    public final static String NUMERICALATMTYPFIELD = "NUMFFTYPE";

    /**
     * Name of the keyword defining the type of embedding 
     * (mechanical or electric)
     */
    public final static String EMBEDDINGKEY = "EMBED"; 


}
