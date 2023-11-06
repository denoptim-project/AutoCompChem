package autocompchem.chemsoftware.gaussian.legacy; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

import java.util.Set;

import autocompchem.chemsoftware.gaussian.GaussianConstants;

/**
 * Object representing "Link 0 command" section of Gaussian input files
 * 
 * @author Marco Foscato
 */

public class GaussianLinkCommandsSection
{
    /**
     * Portions of the Link 0 Section
     */
    private Map<String,String> parts = new HashMap<String,String>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty link0 section
     */

    public GaussianLinkCommandsSection()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a Link 0 section from the array of formatted lines. If lines
     * not pertaining to this section ere ignored.
     * @param lines list of lines to be read
     */

    public GaussianLinkCommandsSection(ArrayList<String> lines)
    {
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            String lineUp = line.toUpperCase();

            if (!lineUp.startsWith(GaussianConstants.KEYLINKSEC))
            {
                continue;
            }

            //Decode form
            String actualLine = line.substring(
                                         GaussianConstants.KEYLINKSEC.length());
            String key = actualLine.substring(0,actualLine.indexOf("="));
            String value = actualLine.substring(actualLine.indexOf("=") + 1);
            setValue(key,value);            
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return The list of keys
     * @return the list of keys
     */

    public Set<String> keySet()
    {
        return parts.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Check whether there is a specific key 
     * @param key the key to look for
     * @return <code>true</code> is the key is contained in this objects
     */

    public boolean hasKey(String key)
    {
        boolean res = false;
        if (parts.keySet().contains(key))
        {
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Return line referring to a key
     * @param key the string used as key
     * @return the string value referring to the given key
     */

    public String getValue(String key)
    {
        return parts.get(key);
    }

//------------------------------------------------------------------------------

    /**
     * Set the value of a field by <code>key</code>. If this pair of key:value
     * does not exist it's created.
     * @param key the string used as key
     * @param value the new value
     */

    public void setValue(String key, String value)
    {
        parts.put(key,value);
    }

//------------------------------------------------------------------------------

    /**
     * Return a string representation with new line characters
     * Suitable to print this Link 0 Section
     * @return the string representation of this link0 section
     */

    public String toString()
    {
        String str = "";
        for (String key : parts.keySet())
        {
            str = str + "%" + key + "=" + parts.get(key) + "\n";
        }

        return str;
    }

//------------------------------------------------------------------------------

    /**
     * Return an array of strings without new line characters
     * Suitable to print this Link 0 Section
     * @return the line of text ready to print a Gaussian input file
     */

    public ArrayList<String> toLinesInp()
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (String key : parts.keySet())
        {
            lines.add("%" + key + "=" + parts.get(key));
        }

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Return an array of strings without new line characters
     * Suitable to print this Link 0 Section
     * @return the line of text ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJob()
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (String key : parts.keySet())
        {
            lines.add(GaussianConstants.KEYLINKSEC 
                        + key + "=" + parts.get(key));
        }

        return lines;
    }

//------------------------------------------------------------------------------

}
