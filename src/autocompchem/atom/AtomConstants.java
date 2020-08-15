package autocompchem.atom;

import java.util.Arrays;
import java.util.HashSet;

/*   
 *   Copyright (C) 2017  Marco Foscato 
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

/**
 * Storage of predefined constants related to atoms
 *
 * @author Marco Foscato
 */

public class AtomConstants
{
    /**
     * Property identifying an atom-like object as a general purpose dummy atom
     */
    public final static String DUMMYATMLABEL = "Du";

    /**
     * Elemental symbols for dummy atoms in Molden
     */
    //TODO: rename and edit code that uses this
    public final static String DUMMYATMLABELMOLDEN = "X";

    /**
     * Property identifying an atom-like object as an attachment point
     */
    public final static String ATTACHMENTPOINTLABEL = "AP";

    /**
     * All known elemental symbols not referring to actual atoms (i.e., dummies)
     */
    public final static Set<String> ALLDUMMYLABELS = new HashSet<String>(
                                Arrays.asList(DUMMYATMLABEL,
                                              DUMMYATMLABELMOLDEN,
                                              ATTACHMENTPOINTLABEL));

}
