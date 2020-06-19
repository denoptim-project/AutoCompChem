package autocompchem.utils;

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

import java.text.NumberFormat;
import java.text.ParsePosition;

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
        NumberFormat format = NumberFormat.getInstance();
        ParsePosition position = new ParsePosition(0);
        format.parse(str, position);
        if (str.length() == position.getIndex())
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
}
