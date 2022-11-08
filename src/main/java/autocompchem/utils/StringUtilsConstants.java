package autocompchem.utils;

import java.util.HashMap;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import java.util.Map;

/**
 * Storage of predefined constants related to string handling tools
 *
 * @author Marco Foscato
 */

public class StringUtilsConstants
{
    /**
     * Map of escaped characters
     */
    public final static Map<String,String> ESCAPABLESIGNS = 
                                                   new HashMap<String,String>()
    {
        {
            put("$","ACCEscapedDollar");
        };
    };

}
