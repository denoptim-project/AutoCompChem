package autocompchem.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
            }
        }
        return result;
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends all string versions of the items in the list as to obtain a 
     * single string that uses the given separator and the given format for the
     * items.
     * @param list the items to append
     * @param the locale settings (e.g., use comma or point as decimal). See
     * {@link Locale}.
     * @param format the format to use when converting the each item to string.
     * We assume this format is compatible with the type of items.
     * @param sep separator to use between formatted entries.
     * @param trim if <code>true</code> avoids to write separator after the
     * last entry.
     * @param offset an integer to add to each item in the list.
     * @return the string <code>e_1+sep+e_2+sep+...+e_N</code>.
     */
    
    public static String mergeListToString(List<Integer> list, 
    		Locale loc, String format, String sep, boolean trim, int offset)
    {
    	List<String> modList = new ArrayList<String>();
    	list.stream().forEach(i -> modList.add(String.format(loc, format, 
    			i + offset)));
    	return mergeListToString(modList, sep, trim);
    }

//------------------------------------------------------------------------------

    /**
     * Appends all integers of a list to obtain a single string that uses the 
     * given separator. 
     * @param list the integers to append
     * @param sep separator to use between entries.
     * @param trim if <code>true</code> avoids to write separator after the
     * last entry.
     * @param offset an integer to add to each item in the list.
     * @return the string <code>e_1+sep+e_2+sep+...+e_N</code>.
     */
    
    public static String mergeListToString(List<Integer> list, String sep, 
    		boolean trim, int offset)
    {
    	return mergeListToString(list, Locale.ENGLISH, "%d", sep, trim, offset);
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends all entries of a list to obtain a single string that uses the 
     * given separator. Each object is converted into a string using the 
     * corresponding <code>toString()</code> method.
     * @param list the entries to append.
     * @param sep separator to use between entries.
     * @return the string <code>e_1+sep+e_2+sep+...+e_N+sep</code>.
     */
    
    public static String mergeListToString(List<? extends Object> list, String sep)
    {
    	return mergeListToString(list, sep, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends all entries of a list to obtain a single string that uses the 
     * given separator.  Each object is converted into a string using the 
     * corresponding <code>toString()</code> method.
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

    /**
     * Counts the number of occurrences of the given regex in the given string.
     * @param str the string to look into.
     * @param regex the reagex to find in the string.
     * @return the number of matches.
     */
    public static int countMatches(String str, String regex)
    {
        Matcher m = Pattern.compile(regex).matcher(str);
    	int n=0;
    	while (m.find())
    		n++;
    	
    	return n;
    }

//------------------------------------------------------------------------------
    
    /**
     * Condenses a list of indexes to define the shortest list of strings
     * where each string represents either a single index of a range of indexes.
     * For example for the list [1,2,3,5,6,7] this
     * method returns "1-3,5-7".
     * @param indexes the list of indexes. The list sill be sorted.
     * @param rangeSep the separator to use when writing a range. 
     * E.g., "-" in "5-7".
     */ 
    public static List<String> makeStringForIndexes(Collection<Integer> indexes, 
    		String rangeSep)
    {
    	return makeStringForIndexes(indexes, rangeSep, 0);
    }
//------------------------------------------------------------------------------
    
    /**
     * Condenses a list of indexes to define the shortest list of strings
     * where each string represents either a single index of a range of indexes.
     * For example for the list [1,2,3,5,6,7] this
     * method returns "1-3,5-7".
     * @param indexes the list of indexes. The list sill be sorted.
     * @param rangeSep the separator to use when writing a range. 
     * E.g., "-" in "5-7".
     * @param offSet use to add a constant to each index. This can be used to
     * change from 0-based to 1-based (using +1) or vice versa using (-1);
     */ 
    public static List<String> makeStringForIndexes(Collection<Integer> indexes, 
    		String rangeSep, int offSet)
    {
    	List<Integer> sortedIds = new ArrayList<Integer>();
    	sortedIds.addAll(indexes);
    	Collections.sort(sortedIds);
        List<String> list = new ArrayList<String>();
        int prevId = sortedIds.get(0);
        int rangeStart = prevId;
        for (int i=1; i<(sortedIds.size()+1); i++)
        {
            int currentId;
            if (i<sortedIds.size())
            {
                currentId = sortedIds.get(i);
            }
            else
            {
                currentId = prevId;
            }
            if (currentId != (prevId+1))
            {
            	String str = "";
                if (rangeStart != prevId)
                {	
                    str = (rangeStart+offSet) + rangeSep + (prevId+offSet);
                }
                else 
                {
                	str = (prevId+offSet)+"";
                }
                list.add(str);
                rangeStart = currentId;
            }
            prevId = currentId;
        }
        return list;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Parses a string to produce an array of doubles. This method assumes that
     * any word (i.e., string delimited by the given delimiting regex) can be
     * converted into a double. Ignores any blank string that is produced by the 
     * regex-based splitting.
     * @param txt the string to parse.
     * @param regex the delimiting regular expression. Note that the delimiter 
     * can be also any text. For instance, <code>"(?i)\\w+:"</code> identifies
     * as separator any case-insensitive text ending with ":".
     * @return the array of doubles in the order as they were found in the 
     * given string.
     */
    public static double[] parseArrayOfDoubles(String txt, String regex)
    {
    	String[] words = txt.trim().split(regex);
    	List<String> wordsList = new ArrayList<String>();
    	for (int i=0; i<words.length; i++)
        {
        	if (words[i].trim().isBlank())
        		continue;
        	wordsList.add(words[i].trim());
        }
        double[] values = new double[wordsList.size()];
        for (int i=0; i<wordsList.size(); i++)
        {
        	values[i] = Double.parseDouble(wordsList.get(i));
        }
        return values;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks is the given string can be an atom identifier of the form
     * "StringIndex" where the Index is an integer and the String is any
     * alphabetical string.
     * @param s
     * @return
     */
    public static boolean isAtomID(String s)
    {
    	return s.matches("[a-z,A-Z]+[0-9]+");
    }

//------------------------------------------------------------------------------

    /**
     * Splits a string into two parts where the first part contains all
     * characters up to and excluding the first character that can be converted 
     * into a number.
     * @param s string to parse
     * @return the pair of strings: first the one containing only alphabetical
     * characters, then the rest.
     */
    public static String[] splitCharactersAndNumber(String s)
    {
    	String[] result = new String[2];
    	result[0] = "";
    	result[1] = ""; 
    	boolean stillChars = true;
    	for (int i = 0; i < s.length(); i++) 
    	{
    		Character c = s.charAt(i);
    		if (NumberUtils.isNumber(c+""))
    			stillChars = false;
    		if (stillChars)
    		{
    			result[0] = result[0] + c;
    		} else {
    			result[1] = result[1] + c;
    		}
    	}
    	return result;
    }
    
//------------------------------------------------------------------------------

}
