package autocompchem.utils;

import java.text.DecimalFormat;

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

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Toolbox for numbers
 * 
 * @author Marco Foscato
 */

public class NumberUtils
{

//------------------------------------------------------------------------------

    /**
     * Checks if a string is a representation of a number. This method considers
     * Fortran scientific notation as a number, although you'll have to convert
     * the "D" into an "E" before parsing to a number.
     * @param str the string
     * @return <code>true</code> if the string can be converted into a number
     */

    public static boolean isNumber(String str)
    {
        str = formatScientificNotation(str);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        ParsePosition position = new ParsePosition(0);
        Number num = format.parse(str, position);
        if (str.length() == position.getIndex() || num!=null)
        {
            return true;
        }
        return false;
    }


//------------------------------------------------------------------------------

    /**
     * Ensures that a string representation of a Fortran double/single 
     * precision numbers is reported in a Java compatible way. In practice,
     * we replace "D" with "E".
     * @param str the string to modify
     * @return the modified string with "E" in stead of "D"
     */

    public static String formatScientificNotation(String str)
    {
        String outStr = str;
        if (outStr.toUpperCase().contains("D"))
        {
            outStr = outStr.toUpperCase().replace("D","E");
        }
        if (outStr.toUpperCase().contains("E+00"))
        {
            outStr = outStr.toUpperCase().replace("E+00","");
        }
        if (outStr.toUpperCase().contains("E-00"))
        {
            outStr = outStr.toUpperCase().replace("E-00","");
        }
        if (outStr.toUpperCase().contains("E+"))
        {
            outStr = outStr.toUpperCase().replace("E+","E");
        }
        return outStr;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given string is parsable to an integer.
     * @param s the candidate string.
     * @return <code>true</code> if the string can be parsed into an integer.
     */
    public static boolean isParsableToInt(String s)
    {	
    	final String Digits     = "(\\p{Digit}+)";
    	final String iRegex    =
    	      ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
    	       "[+-]?" +
    	       Digits +
    	       "[\\x00-\\x20]*");// Optional trailing "whitespace"

    	return Pattern.matches(iRegex, s);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given string is parsable to a double
     * @param s the candidate string
     * @return <code>true</code> if the string can be parsed into a double
     */
    public static boolean isParsableToDouble(String s)
    {
    	// As suggested in JavaDoc
    	
    	final String Digits     = "(\\p{Digit}+)";
    	final String HexDigits  = "(\\p{XDigit}+)";
    	// an exponent is 'e' or 'E' followed by an optionally
    	// signed decimal integer.
    	final String Exp        = "[eE][+-]?"+Digits;
    	final String fpRegex    =
    	      ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
    	       "[+-]?(" + // Optional sign character
    	       "NaN|" +           // "NaN" string
    	       "Infinity|" +      // "Infinity" string

    	       // A decimal floating-point string representing a finite positive
    	       // number without a leading sign has at most five basic pieces:
    	       // Digits . Digits ExponentPart FloatTypeSuffix
    	       //
    	       // Since this method allows integer-only strings as input
    	       // in addition to strings of floating-point literals, the
    	       // two sub-patterns below are simplifications of the grammar
    	       // productions from section 3.10.2 of
    	       // The Java Language Specification.

    	       // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
    	       "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

    	       // . Digits ExponentPart_opt FloatTypeSuffix_opt
    	       "(\\.("+Digits+")("+Exp+")?)|"+

    	       // Hexadecimal strings
    	       "((" +
    	        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
    	        "(0[xX]" + HexDigits + "(\\.)?)|" +

    	        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
    	        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

    	        ")[pP][+-]?" + Digits + "))" +
    	       "[fFdD]?))" +
    	       "[\\x00-\\x20]*");// Optional trailing "whitespace"

    	return Pattern.matches(fpRegex, s);
    }

//------------------------------------------------------------------------------

    /**
     * Detects the precision of a float number reported as a string.
     * @param str the string representing the number
     * @return the number of significant digits
     */

    public static int getPrecision(String str)
    {
        int p = 0;
        boolean count = false;
        for (int i = 0; i<str.length(); i++)
        {
            char c = str.charAt(i);
            // Ignore leading chars (including 0)
            if (!count && "123456789".indexOf(c) == -1)
            {
                continue;
            }
            // Stop when anything else than a digit, comma, or point is found
            if (count && "0123456789.,".indexOf(c) == -1)
            {
                break;
            }
            // Count digits (including tailing 0)
            count = true;
            if ("0123456789".indexOf(c) != -1)
            {
                p++;
            }
        }
        // Deal with zeros (i.e., 0.000 vs 0.000000)
        if (p==0 && str.indexOf('0')!=-1)
        {
            p = 1;
            for (int i = 0; i<str.length(); i++)
            {
                char c = str.charAt(i);
                // Ignore leading chars (including 0)
                if (!count && ".".indexOf(c) == -1)
                {
                    continue;
                }
                // Stop on anything else than a digit, comma, or point
                if (count && "0.".indexOf(c) == -1)
                {
                    break;
                }
                // Count digits (including tailing 0)
                count = true;
                if ("0".indexOf(c) != -1)
                {
                    p++;
                }
            }
        }
        return p;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Formats a decimal number using the given pattern but with English format
     * as for separators.
     * @param pattern the pattern to use. Example "###.#"
     * @param decimals minimum number of decimal digits to print. Overwrites the
     * specific defined by the pattern.
     * @param value the value to format
     * @return the formatted string
     */
    public static String getEnglishFormattedDecimal(String pattern, 
            int decimals, double value)
    {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern(pattern);
        df.setMinimumFractionDigits(decimals);
        return df.format(value);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Formats a decimal number using the given pattern but with English format
     * as for separators. Imposes 4 as the minimum number of fractional digits.
     * @param pattern the pattern to use. Example "###.####"
     * @param value the value to format
     * @return the formatted string
     */
    public static String getEnglishFormattedDecimal(String pattern, double value)
    {
        return getEnglishFormattedDecimal(pattern,4,value);
    }

//------------------------------------------------------------------------------     
}
