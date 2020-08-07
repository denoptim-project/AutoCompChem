package autocompchem.run;

/*   
 *   Copyright (C) 2020  Marco Foscato 
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
 * Storage of predefined constants related to Actions.
 *
 * @author Marco Foscato
 */

public class ActionConstants
{
    /**
     * Label defining a commented-out line
     */
    public final static String COMMENTLINE = "#";

    /**
     * Separator between key and value of a parameter
     */
    public final static String SEPARATOR = ":";

    /**
     * Label defining the beginning of a multiline block
     */
    public final static String STARTMULTILINE = "$START";

    /**
     * Label defining the end of a multiline block
     */
    public final static String ENDMULTILINE = "$END";

    /**
     * Label of line containing the type of action
     */
    public final static String TYPEKEY = "ACTIONTYPE";
    
    /**
     * Label of line containing the object of the action
     */
    public final static String OBJECTKEY = "ACTIONOBJ";
    
    /**
     * Label of line containing the action details
     */
    public final static String DETAILSKEY = "ACTIONDETAILS";

}
