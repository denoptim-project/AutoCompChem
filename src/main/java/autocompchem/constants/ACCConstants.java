package autocompchem.constants;

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
 * Storage of predefined constants for general purposes.
 *
 * @author Marco Foscato
 */

public class ACCConstants
{   
    /**
     * Hartree to Joule conversion factor [J/Eh]. Source "
     *  J. Phys. Chem. Ref. Data 50, 033105 (2021); 
     *  https://doi.org/10.1063/5.0064853
     */
    public final static double HARTREETOJOULE = 4.3597447222071E-18;

    /**
     * Speed of light [cm/s]. Source "
     *  J. Phys. Chem. Ref. Data 50, 033105 (2021); 
     *  https://doi.org/10.1063/5.0064853
     */
    public final static double SPEEDOFLIGHT = 2.99792458E10;

    /**
     * Plank's constant 'h' [J/Hz]. Source "
     *  J. Phys. Chem. Ref. Data 50, 033105 (2021); 
     *  https://doi.org/10.1063/5.0064853
     */
    public final static double PLANKSK = 6.62607015E-34;

    /**
     * Avogadro's number [1/mol]. Source "
     *  J. Phys. Chem. Ref. Data 50, 033105 (2021); 
     *  https://doi.org/10.1063/5.0064853
     */
    public final static double NAVOGADRO = 6.02214076E23;

    /**
     * Boltzman's constant [J/Kelvin]. Source "
     *  J. Phys. Chem. Ref. Data 50, 033105 (2021); 
     *  https://doi.org/10.1063/5.0064853
     */
    public final static double BOLTZMANNSK = 1.380649E-23;
    
    /**
     * Hartree to kiloJ/mol conversion factor [J/(Eh*mol)].
     */
    public final static double HARTREETOJOULEPERMOLE = 
    		HARTREETOJOULE * NAVOGADRO;
   
    /**
     * Gas's constant [J/(Kelvin*mol)]
     */
    public final static double  GASR = NAVOGADRO * BOLTZMANNSK;
    
    /**
     * Aangstrom to Bohr conversion factor
     */
    public final static double ANGSTOMTOBOHR = 1.889725989;

    /**
     * Hartree to kilo calories per mole conversion factor.
     */
    @Deprecated
    public final static double HARTREETOKCALPERMOL = 627.5095;

    /**
     * Calories per mole to Joule per mole conversion factor.
     */
    @Deprecated
    public final static double JOULEPERMOLETOCALPERMOL = 4.184;

    /**
     * Atom property used to store atom tags
     */
    public final static String ATMTAGPROP = "ATMTAGPROP";

    /**
     * Atom property used to store index in atom list
     */
    public final static String ATMIDPROP = "ATMIDPROP";

    /**
     * Threshold for comparing {@link Double} numbers
     */
	public final static double DOUBLEPRECISION = 0.00000000000001;

	/**
	 * Logger charged to produce main log, which is always meant to STDOUT.
	 */
	public final static String MAINOUTPUTLOGGER = "MAINOUTPUTLOGGER";
	
}
