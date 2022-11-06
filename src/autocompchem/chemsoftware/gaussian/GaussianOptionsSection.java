package autocompchem.chemsoftware.gaussian;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterUtils;
import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;

/**
 * Object representing "Options Section" of Gaussian input files
 * 
 * @author Marco Foscato
 */

public class GaussianOptionsSection
{

    /**
     * Portions of the Options Section
     */
    private Map<String,String> parts = new HashMap<String,String>();


//------------------------------------------------------------------------------
    
    /**
     * Constructor for an empty GaussianOptionsSection object
     */

    public GaussianOptionsSection()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty GaussianOptionsSection object from a list of
     * formatted lines
     * @param lines the blick of lines to be put in this options section
     */

    public GaussianOptionsSection(ArrayList<String> lines)
    {
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            if (!line.toUpperCase().startsWith(GaussianConstants.KEYOPTSSEC))
                continue;

            //Decode the content of the line
            ArrayList<String> keyValue = lineToKeyValuePair(line);

            //Store
            parts.put(keyValue.get(0),keyValue.get(1));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add or update a portion to this Options Section. 
     * If the <code>refName</code> is 
     * already in use, the content is overwritten.
     * @param refName the regerence name of the part of options to add
     * @param value the content of the part of options to add
     */

    public void setPart(String refName, String value)
    {
        parts.put(refName, value);
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of reference names for all the options contained in this
     * section.
     * @return the list o reference names
     */

    public Set<String> getRefNames()
    {
        return parts.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Return one of the option
     * @param refName the reference name 
     * @return the required option
     */

    public String getValue(String refName)
    {
        return parts.get(refName);
    }

//------------------------------------------------------------------------------

    /**
     * Return a pair of strings representing the key:value pair defined in the
     * line of text
     * @param line text to be transformed in a key:value pair
     * @return a <code>key:value</code> pair in the form of a 2-elements
     * vector (i.e., ArrayList) of strings.
     */

    private ArrayList<String> lineToKeyValuePair(String line)
    {
        String actualLine = line.substring(
                                    GaussianConstants.KEYOPTSSEC.length());
        String key = "";
        String value = "";
        if (actualLine.contains("="))
        {
            key = actualLine.substring(0,actualLine.indexOf("="));
            value = actualLine.substring(actualLine.indexOf("=") + 1);
        } else {
            String msg = "ERROR! Attempt to create a GaussianOptionsSection "
                        + "without specifying a reference name for the option "
                        + "(i.e., '=' not found). Check the following line in "
                        + "your input: " + line;
            Terminator.withMsgAndStatus(msg,-1);
        }

        ArrayList<String> keyValuePair = new ArrayList<String>();
        keyValuePair.add(key.toUpperCase().trim());
        keyValuePair.add(value);

        return keyValuePair;
    }

//------------------------------------------------------------------------------

    /**
     * Order the keys of the option blocks according to the presumed 
     * expectations of Gaussian (i.e., basis set before PCM)
     * @param keySet the set of keys
     * @return the reordered list
     */

    private ArrayList<String> sortOpts(Set<String> keySet)
    {
        ArrayList<String> sortedKeys = new ArrayList<String>();
        if (keySet.contains(GaussianConstants.MODREDUNDANTKEY))
        {
            sortedKeys.add(GaussianConstants.MODREDUNDANTKEY);
        }
        if (keySet.contains(GaussianConstants.BASISOPTKEY))
        {
            sortedKeys.add(GaussianConstants.BASISOPTKEY);
        }
        if (keySet.contains(GaussianConstants.PCMOPTKEY))
        {
            sortedKeys.add(GaussianConstants.PCMOPTKEY);
        }
        for (String key : keySet)
        {
            if (key.equals(GaussianConstants.PCMOPTKEY) 
                || key.equals(GaussianConstants.BASISOPTKEY)
                || key.equals(GaussianConstants.MODREDUNDANTKEY))
            {
                continue;
            }
            sortedKeys.add(key);
        }
        return sortedKeys;
    }

//------------------------------------------------------------------------------

    /**
     * Return an array of strings without new line characters.
     * Suitable to print this Options Section in a Gaussian input file.
     * @return the lines of text ready for a Gaussian input file.
     */

    public ArrayList<String> toLinesInp()
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (String key : sortOpts(parts.keySet()))
        {
            lines.add(StringUtils.deescapeSpecialChars(
                         ParameterUtils.removeMultilineLabels(parts.get(key))));
            lines.add("");
        }
        // An extra blank line seems not necessary in Gaussian16
        lines.add("");

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Return an array of strings without new line characters
     * Suitable to print this Options Section as autocompchem jobDetails format
     * @return the lines of text ready for a jobDetails file
     */

    public ArrayList<String> toLinesJob()
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (String key : parts.keySet())
        {
            String line = "";
            String val = parts.get(key);
            if (val.contains(System.getProperty("line.separator"))
                && !val.contains(ParameterConstants.STARTMULTILINE))
            {
                line = GaussianConstants.KEYOPTSSEC + " " + key + "=" 
                       + ParameterConstants.STARTMULTILINE + val
                       + System.getProperty("line.separator") 
                       + ParameterConstants.ENDMULTILINE;
            }
            else
            {
                line = GaussianConstants.KEYOPTSSEC + " " + key + "=" + val;
            }
            lines.add(line);
        }

        //No need of blackline because this in not Gaussian input

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Return a string representation with new line characters
     * @return the human readable string-like representation of this object
     */

    public String toString()
    {
        String str = "GaussianOptionsSection [";
        for (String key : parts.keySet())
        {
            str = str + key + ":" + parts.get(key) + ", ";
        }
        str = str + "] ";
        return str;
    }

//------------------------------------------------------------------------------

}
