package autocompchem.perception.situation;

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

public class SituationConstants
{
	/**
	 * File extension of text-based situation files
	 */
	public final static String SITUATIONTXTFILEEXT = ".json";
	
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
     * Label of line containing the type of situation
     */
    public final static String SITUATIONTYPE = "SITUATIONTYPE";
    
    /**
     * Label of line containing the reference name of the situation
     */
    public final static String REFERENCENAMELINE = "REFERENCENAME";
 
    /**
     * Label of line defining a circumstance
     */
    public final static String CIRCUMSTANCE = "CIRCUMSTANCE";
    
    /**
     * Label of line defining an action/impulse triggered by the occurrence of 
     * a situation
     */
    public final static String ACTION = "ACTION";

}
