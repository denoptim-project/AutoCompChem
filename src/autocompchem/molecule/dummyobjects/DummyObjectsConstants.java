package autocompchem.molecule.dummyobjects;

/*   
 *   Copyright (C) 2017  Marco Foscato 
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
 * Storage of predefined constants for dummy objects-related code
 *
 * @author Marco Foscato
 */

public class DummyObjectsConstants
{
    /**
     * Angle threshold above which an angle is considered as sufficiently 
     * close to linear as to require special attention. For instance, use 
     * of linearity-braking dummy atom.
     */
    public final static double LINEARANGLE = 176.00;

    /**
     * Dummy bond length
     */
    public final static double DUBNDLENGTH = 1.00;

}
