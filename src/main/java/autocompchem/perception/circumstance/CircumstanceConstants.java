package autocompchem.perception.circumstance;

/*   
 *   Copyright (C) 2020  Marco Foscato 
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
 * Storage of predefined constants related to situations.
 *
 * @author Marco Foscato
 */

public class CircumstanceConstants
{
    /**
     * Identified for circumstance kind: a string to be matched
     */
    public final static String MATCHES = "MATCHES";
    
    /**
     * Identified for circumstance kind: a string NOT to be matched
     */
    public final static String NOMATCH = "NOMATCH";
    
    /**
     * Identified for circumstance kind: a loop counter based condition
     */
    public final static String MATCHESCOUNT = "MATCHESCOUNT";
    
    /**
     * Identified for circumstance kind: a loop counter based condition
     */
    public final static String LOOPCOUNTER = "LOOPCOUNTER";

}
