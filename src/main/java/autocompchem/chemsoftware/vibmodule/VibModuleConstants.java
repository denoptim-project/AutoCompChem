package autocompchem.chemsoftware.vibmodule;

/*   
 *   Copyright (C) 2016  Marco Foscato 
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
 * Storage of predefined constants for VibModule related tools
 *
 * @author Marco Foscato
 */

public class VibModuleConstants
{
	/**
	 * Key of parameter providing SMARTS to identify the force field paramerters
	 * to extracts
	 */
	public final static String PARINTCOORDBYSMARTS = "INTCOORDBYSMARTS";
	
    /**
     * String defining normal termination in status file
     */
    public final static String NORMALCOMPLSTATUS = 
                                              " VibModule terminated normally.";

    /**
     * String defining the beginning of the force field parameters section
     */
    public final static String TITFFPARAMS = " Diagonal force field parameters";

    /**
     * String defining the beginning of the bond stretches section
     */
    public final static String TITSTRSEC = " bond stretches,";

    /**
     * String defining the beginning of the bond bendings section
     */
    public final static String TITBENDSEC = " bendings,";

    /**
     * String defining the beginning of the out-of-plane bendings section
     */
    public final static String TITOOPSEC = " out-of-plane bends";

    /**
     * String defining the beginning of the torsions section
     */
    public final static String TITTORSEC = " torsions,";

    /**
     * String defining the beginning of the frequencies section
     */
    public final static String TITFREQSEC = " Harmonic frequencies ";

    /**
     * Name of bond stretching type of internal coordinate
     */
    public final static String TYPSTR = "STR";

    /**
     * Name of angle bending type of internal coordinate
     */
    public final static String TYPBND = "BND";

    /**
     * Name of out-of-plane type of internal coordinate
     */
    public final static String TYPOOP = "OOP";

    /**
     * Name of bond torsion type of internal coordinate
     */
    public final static String TYPTOR = "TOR";

    /**
     * Name of parameter data
     */
    public final static String PARAMDATA = "FF-PARAM";

   /**
    * String representing a bond in VibModule output file
    */
   public final static String BNDSTR = "----";

}
