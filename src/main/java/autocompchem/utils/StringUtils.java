package autocompchem.utils;

import java.util.List;
import java.util.regex.Matcher;

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

import java.util.regex.Pattern;

/**
 * Toolbox for strings
 * 
 * @author Marco Foscato
 */

public class StringUtils
{
     private static boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Converts escaped characters into a string not containing any special 
     * character
     * @param str the string
     * @return the string with the escapable special characters converted
     */

    public static String escapeSpecialChars(String str)
    {
        String result = str;
        for (String key : StringUtilsConstants.ESCAPABLESIGNS.keySet())
        {
            if (str.contains("\\"+key))
            {
                String regex = "\\\\\\"+key;
                String replacement = StringUtilsConstants.ESCAPABLESIGNS.get(
                                                                           key);
                Pattern rgxPattern = Pattern.compile(regex);
                Matcher m = rgxPattern.matcher(result);
                result = m.replaceAll(replacement);
                if (debug)
                {
                    System.out.println("Replacing " + rgxPattern 
                                        + " with " + replacement);
                    System.out.println("After: "+result);
                }
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

    /**
     * Converts a string with previously escaped charters into a string 
     * containing the originally intended special
     * character
     * @param str the string
     * @return the string with the escapable special characters
     */

    public static String deescapeSpecialChars(String str)
    {
        String result = str;
        for (String key : StringUtilsConstants.ESCAPABLESIGNS.keySet())
        {
            String replacement = StringUtilsConstants.ESCAPABLESIGNS.get(key);
            if (str.contains(replacement))
            {
                String regex = replacement;
                Pattern rgxPattern = Pattern.compile(regex);
                Matcher m = rgxPattern.matcher(result);
                result = m.replaceAll("\\"+key);
                if (debug)
                {
                    System.out.println("de-Replacing " + rgxPattern
                                        + " with " + key);
                    System.out.println("After: "+result);
                }
            }
        }
        return result;
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends all entries of a list to obtain a single string that uses the 
     * given separator.
     * @param list the entries to append.
     * @param sep separator to use between entries.
     * @return the string <code>e_1+sep+e_2+sep+...+e_N</code>.
     */
    
    public static String mergeListToString(List<? extends Object> list, String sep)
    {
    	return mergeListToString(list,sep,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends all entries of a list to obtain a single string that uses the 
     * given separator.
     * @param list the entries to append.
     * @param sep separator to use between entries.
     * @param trim if <code>true</code> avoids to write separator after the
     * last entry.
     * @return the string <code>e_1+sep+e_2+sep+...+e_N</code>.
     */
    
    public static String mergeListToString(List<? extends Object> list, 
    		String sep, boolean trim)
    {
    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i<list.size(); i++)
    	{
    		sb.append(list.get(i).toString());
    		if (i<(list.size()-1))
    			sb.append(sep);
    		else
    			if (!trim)
    				sb.append(sep);
    	}
    	return sb.toString();
    }
    
//------------------------------------------------------------------------------

    public static int countMatches(String str, String regex)
    {
        Matcher m = Pattern.compile(regex).matcher(str);
    	int n=0;
    	while (m.find())
    		n++;
    	
    	return n;
    }
    
//------------------------------------------------------------------------------

}
