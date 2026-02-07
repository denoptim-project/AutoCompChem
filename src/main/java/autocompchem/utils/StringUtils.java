package autocompchem.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import java.util.regex.PatternSyntaxException;

/**
 * Toolbox for strings
 * 
 * @author Marco Foscato
 */

public class StringUtils
{
	
	protected static final Set<String> TRUE_VALUES = Set.of(
			"TRUE", "YES", "Y", "ON", "ENABLED", "ENABLE");
	    
    protected static final Set<String> FALSE_VALUES = Set.of(
    		"FALSE", "NO", "N", "OFF", "DISABLED", "DISABLE");

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
     * Converts a list of integers into a string representation
     * where consecutive integers are formatted as ranges and non-consecutive 
     * integers are listed separately.
     * @param sortedList the naturally sorted list of integers to format
     * @param idSeparator separator to use between non-consecutive items/ranges
     * @param rangeSeparator separator to use between the extremes of a range
     * @return formatted string with ranges (e.g., "1-3,6-7,9" for [1,3,2,9,6,7])
     */
    public static String formatIntegerListWithRanges(List<Integer> indexes, 
            String idSeparator, String rangeSeparator)
    {
        List<Integer> sortedList = new ArrayList<Integer>(indexes);
        Collections.sort(sortedList);
        if (sortedList == null || sortedList.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        int start = sortedList.get(0);
        int end = start;
        
        for (int i = 1; i < sortedList.size(); i++) {
            int current = sortedList.get(i);
            
            // Check if current number is consecutive to the previous one
            if (current == end + 1) {
                end = current;
            } else {
                // End of current range, append it to result
                if (sb.length() > 0) {
                    sb.append(idSeparator);
                }
                
                if (start == end) {
                    // Single number
                    sb.append(start);
                } else {
                    // Range
                    sb.append(start).append(rangeSeparator).append(end);
                }
                
                // Start new range
                start = current;
                end = current;
            }
        }
        
        // Append the last range
        if (sb.length() > 0) {
            sb.append(idSeparator);
        }
        
        if (start == end) {
            // Single number
            sb.append(start);
        } else {
            // Range
            sb.append(start).append(rangeSeparator).append(end);
        }
        
        return sb.toString();
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
    
    /**
     * Extract text in between parenthesis. Ignores nested parentheses.
     * @param input the text from which to extract the result.
     * @return the string contained in the first pair of parenthesis, or 
     * <code>null</code> if no parenthesis is found.
     */
    
    public static String getParenthesesContent(String input) 
    {
    	return getEnclosedContent(input, "(".charAt(0), ")".charAt(0));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Extract text in between two given characters. Ignores nested instances of
     *  the two characters.
     * @param input the text from which to extract the result.
     * @param openingChar the character that opens the substring to extract
     * @param closingChar the character that closes the substring to extract.
     * @return the string contained in the first pair of opening and closing 
     * characters, or <code>null</code> if no pair is found.
     */
    
    public static String getEnclosedContent(String input, char openingChar, 
    		char closingChar) 
    {
        int start = input.indexOf(openingChar);
        if (start == -1) return null;
        
        int count = 0;
        int end = start;
        
        boolean foundEnd = false;
        for (int i = start; i < input.length(); i++) 
        {
            if (input.charAt(i) == openingChar) count++;
            if (input.charAt(i) == closingChar) count--;
            if (count == 0) {
            	foundEnd = true;
                end = i;
                break;
            }
        }
        if (!foundEnd)
        {
        	return input.substring(start + 1);
        } else {
        	return input.substring(start + 1, end);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Parse a string into a boolean. Case insensitive, and understands Yes/True/Y
     * @param value the string to parse
     * @return the corresponding boolean or <code>null</code> if the given 
     * string is <code>null</code>.
     * @throws IllegalArgumentException if the string cannot be parsed to 
     * a boolean.
     */
    public static Boolean parseBoolean(String value) 
    {
    	return parseBoolean(value, false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Parse a string into a boolean. Case insensitive, and understands Yes/True/Y
     * @param value the string to parse
     * @param nullOrBlankToTrue use <code>true</code> to consider a 
     * <code>null</code> or a blank input string as <code>true</code>. Otherwise, 
     * <code>null</code>, returns <code>null</code>, while empty or blank 
     * strings trigger an exception.
     * @return the boolean or <code>null</code> if the given string is 
     * <code>null</code> and the <code>nullOrBlankToTrue</code> is 
     * <code>false</code>.
     * @throws IllegalArgumentException if the string cannot be parsed to 
     * a boolean.
     */
    public static Boolean parseBoolean(String value, boolean nullOrBlankToTrue) 
    {
    	if (value == null)
    	{
    		if (nullOrBlankToTrue)
        	{
        		return true;
        	} else {
        		return null;
        	}
    	}
    	
    	if (value.isBlank())
    	{
    		if (nullOrBlankToTrue)
        	{
        		return true;
        	}
    	}

        if (value.equals("null"))
        {
            if (nullOrBlankToTrue)
            {
                return true;
            }
        }
        
        String normalized = value.trim().toUpperCase();
        
        if (TRUE_VALUES.contains(normalized)) {
            return true;
        }
        
        if (FALSE_VALUES.contains(normalized)) {
            return false;
        }
        
        throw new IllegalArgumentException("Cannot parse boolean from: " + value);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given string is a valid REGEX
     * @param regex string to test
     * @return <code>true</code> if the string can be used as REGEX
     */
    public static boolean isValidRegex(String regex) 
    {
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Checks if the given string has the syntax of a command call with 
     * parentheses content, i.e., <code>command(...)</code>. 
     * For example, the string "getACCJobsData(arg,arg)" 
     * mathces the syntax od a command <code>getACCJobsData</code> with the 
     * parentheses content <code>arg,arg</code>.
     * @param txt the string to check
     * @param commandCall the command call to check for
     * @return <code>true</code> if the string has the syntax of a command call 
     * with parentheses content possibly with white spaces around the command call.
     */
    public static boolean hasSyntaxOfCommandCallWithParenthesesContent(
        String txt, String commandCall) 
    {
        String trimmed = txt.strip();
        String commandCallUpper = commandCall.toUpperCase();
        String trimmedUpper = trimmed.toUpperCase();
        if (trimmedUpper.startsWith(commandCallUpper))
        {
            int commandCallEnd = commandCall.length();
            String withoutCommand = trimmed.substring(commandCallEnd).strip();
            if (withoutCommand.isEmpty())
            {
                // No parentheses content
                return false;
            }
            String parenthesesContent = StringUtils.getParenthesesContent(
                withoutCommand);
            if (parenthesesContent == null)
            {
                return false;
            }
            // Find the position of the opening parenthesis
            int openParenPos = withoutCommand.indexOf('(');
            if (openParenPos == -1)
            {
                return false;
            }
            // Calculate what should remain after: opening paren + content + closing paren
            // After extracting content, we should have: ( + content + )
            int expectedEndPos = openParenPos + 1 + parenthesesContent.length() + 1;
            if (expectedEndPos > withoutCommand.length())
            {
                return false;
            }
            String withoutCommandAndParenthesis = withoutCommand.substring(
                expectedEndPos).strip();
            if (!withoutCommandAndParenthesis.isEmpty())
            {
                // There is something after the parentheses content
                return false;
            }
            return true;
        }
        return false;
    }
    
//------------------------------------------------------------------------------

}
