package autocompchem.parameters;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Utilities for paramemters
 * 
 * @author Marco Foscato
 */

public class ParameterUtils
{
     

//------------------------------------------------------------------------------

    /**
     * Remove multiline start/end labels. 
     * This method replaces the labels, i.e., the strings
     * that define the beginning and the ned of a multiline block of text.
     * If such substitution generated an empty line, then that line is also 
     * removed
     * @param parValue the string-like value of the parameter
     * @return the purged string
     */

    public static String removeMultilineLabels(String parValue)
    {
        String[] parts = parValue.split(System.getProperty("line.separator"));
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<parts.length; i++)
        {
            String line = parts[i];
            boolean hasBegin = line.contains(ParameterConstants.STARTMULTILINE);
            line = line.replace(ParameterConstants.STARTMULTILINE,"");
            boolean hasEnd = line.contains(ParameterConstants.ENDMULTILINE);
            line = line.replace(ParameterConstants.ENDMULTILINE,"");
            if ((hasBegin || hasEnd) && line.matches("\\s+"))
            {
                continue;
            }
            else
            {
                if (!sb.toString().equals(""))
                {
                    sb.append(System.getProperty("line.separator"));
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------
}
