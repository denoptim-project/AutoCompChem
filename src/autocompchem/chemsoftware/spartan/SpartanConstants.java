package autocompchem.chemsoftware.spartan;

/*   
 *   Copyright (C) 2016  Marco Foscato 
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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Storage of predefined constants for Spartan related tools
 *
 * @author Marco Foscato
 */

public class SpartanConstants
{
    /**
     * Extension of Spartan input files
     */
    public final static String SPRTINPEXTENSION = ".spardir";

    /**
     * Name of Spartan flag file for root folder
     */
    public final static String ROOTFLGFILENAME = "_spartandir";

    /**
     * Header of Spartan flag file for root folder
     */
    public final static String ROOTFLGFILEHEAD = "spartan list direcory";

    /**
     * Name of Spartan flag file for molecule-specific folder
     */
    public final static String MOLFLGFILENAME = "_spartan";

    /**
     * Header of Spartan flag file for molecule-specific folder
     */
    public final static String MOLFLGFILEHEAD = "spartan directory";

    /**
     * Name of Spartan cell file (contains all properties from SDF)
     */
    public final static String CELLFILENAME = "cell";

    /**
     * Name of Spartan input file (contains the definition of the system)
     */
    public final static String INPUTFILENAME = "input";

    /**
     * Begin of cell directive
     */
    public final static String CELLOPN = "BEGIN_CELL_DEFINITIONS";

    /**
     * End of cell directive
     */
    public final static String CELLEND = "END_CELL_DEFINITIONS";

    /**
     * Begin of Cartesian coords directive
     */
    public final static String XYZOPN = "";

    /**
     * End of Cartesian coords directive
     */
    public final static String XYZEND = "ENDCART";

    /**
     * Begin of topology directive
     */
    public final static String TOPOOPN = "HESSIAN";

    /**
     * End of topology directive
     */
    public final static String TOPOEND = "^ENDHESS(.*)";

    /**
     * Begin of comment directive
     */
    public final static String COMMOPN = "BEGINCOMMENTS";

    /**
     * End of comment directive
     */
    public final static String COMMEND = "ENDCOMMENTS";

    /**
     * Begin of directive
     */
    public final static String ATMLABELSOPN = "ATOMLABELS";

    /**
     * End of directive
     */
    public final static String ATMLABELSEND = "ENDATOMLABELS";

    /**
     * Begin of constraints directive
     */
    public final static String CSTRDIROPN = "BEGINCONSTRAINTS";

    /**
     * End of constraints directive
     */
    public final static String CSTRDIREND = "ENDCONSTRAINTS";

    /**
     * Begin of dynamic constraints directive
     */
    public final static String DYNCDIROPN = "DYNCON";

    /**
     * End of dynamic constraints directive
     */
    public final static String DYNCDIREND = "ENDDYNCON";

    /**
     * Begin of constraints directive
     */
    public final static String FREEZEOPN = "FROZEN";

    /**
     * End of constraints directive
     */
    public final static String FREEZEEND = "ENDFROZEN";

    /**
     * Begin of conformer directive
     */
    public final static String CONFDIROPN = "CONFORMER";

    /**
     * End of conformer directive
     */
    public final static String CONFDIREND = "ENDCONFORMER";

    /**
     * Begin of "propin" directive
     */
    public final static String PRODIROPN = "BEGINPROPIN";

    /**
     * End of "propin" directive
     */
    public final static String PRODIREND = "ENDPROPIN";

    /**
     * Indentation in directives
     */
    public final static String INDENT = "  ";

    /**
     * Name of the output file 
     */
    public final static String OUTPUTFILENAME = "output";

    /**
     * Name of the status file
     */
    public final static String STATUSFILENAME = "status.html";

    /**
     * String defining notmal termination in status file
     */
    public final static String NORMALCOMPLSTATUS = "COMPLETED";

    /**
     * Name of the archive file
     */
    public final static String ARCHIVEFILENAME = "archive";

    /**
     * String identifying the beginning of XYZ block in archive file
     */
    public final static String ARCHSTARTXYZ = "^GEOMETRY.*";

    /**
     * String identifying the energy line in archive file
     */
    public final static String ARCHENERGYLAB = "^ENERGY *";

    /**
     * String identifying the basis set line in archive file
     */
    public final static String ARCHBASISLAB = "^BASIS *";

    /**
     * Atom property used to store atom's label according to Spartan format
     */
    public final static String ATMLABELATMPROP = "SPRT_ATM_LABEL";

}
