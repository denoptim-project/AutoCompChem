package autocompchem.modeling.forcefield;

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
 * Storage of predefined constants related to molecular force fields
 *
 * @author Marco Foscato
 */

public class ForceFieldConstants
{

    /**
     * Keyword used to specify atom type
     */
    public final static String ATMTYPSTRMAP = "TYPE";

    /**
     * Keyword used to specify atom class
     */
    public final static String ATMCLSSTRMAP = "CLASS";

    /**
     * Property used to store atom type's numerical index 
     * in <code>AtomType</code>
     */
    public final static String ATMTYPINT = "ATMTYPINT";

    /**
     * Property used to store atom class's generical string representation
     * in <code>AtomType</code>
     */
    public final static String ATMCLSSTR = "ATMCLSSTR";

    /**
     * Property used to store atom class's numerical index
     * in <code>AtomType</code>
     */
    public final static String ATMCLSINT = "ATMCLSINT";

    /**
     * Property used to store atom type identification string
     * in <code>AtomType</code>
     */
    public final static String ATMTYPSTR = "ATMTYPSTR";

    /**
     * Property used to store atom type-related comment
     * in <code>AtomType</code>
     */
    public final static String ATMTYPTXT = "ATMTYPTXT";

    /**
     * Property used to store SMARTS matching atom types 
     * in <code>AtomType</code>
     */
    public final static String SMARTSQUERYATMTYP = "SMARTSQUERYATMTYP";

    /**
     * Vibrational analysis format name for VibModule
     */
    public final static String VIBANLVIBMODULE = "VIBMODULE";

    /**
     * Keyword for Tinker force field file format
     */
    public final static String FFFILETNKFORMAT = "FFFILETNKFORMAT";

}
