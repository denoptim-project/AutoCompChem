package autocompchem.constants;

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

/**
 * Storage of predefined constants for general purposes
 *
 * @author Marco Foscato
 */

public class ACCConstants
{
    /**
     * Aangstroom to Bohr conversion factor
     */
    public final static double ANGSTOMTOBOHR = 1.889725989;

    /**
     * Hartree to Kilo calories per mole conversion factor
     */
    public final static double HARTREETOKCALPERMOL = 627.5095;

    /**
     * Calories per mole to Joule per mole conversion factor
     */
    public final static double JOULEPERMOLETOCALPERMOL = 4.184;

    /**
     * Speed of light [cm/s]
     */
    public final static double SPEEDOFLIGHT = 2.99792458E10;

    /**
     * Boltzman's constant [J/Kelvin]
     */
    public final static double BOLTZMANNSK = 1.3806488E-23;

    /**
     * Plank's contant [J*s]
     */
    public final static double  PLANKSK = 6.62606957E-34;

    /**
     * Avogadro's number [1/mol]
     */
    public final static double  NAVOGADRO = 6.02214129E23;

    /**
     * Gas's constant [J/(Kelvin*mol)]
     */
    public final static double  GASR = NAVOGADRO * BOLTZMANNSK;

    /**
     * Reference name for verbosity level parameter
     */
    public final static String VERBOSITYPAR = "VERBOSITY";

    /**
     * Atom property used to store atom tags
     */
    public final static String ATMTAGPROP = "ATMTAGPROP";

    /**
     * Atom property used to store index in atom list
     */
    public final static String ATMIDPROP = "ATMIDPROP";

}
