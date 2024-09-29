package autocompchem.wiro.chem.gaussian.legacy;

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

import autocompchem.wiro.chem.gaussian.GaussianConstants;

/**
 * Object representing "Route Section" section of Gaussian input files
 * 
 * @author Marco Foscato
 */

public class GaussianRouteSection
{

    //Portions of the Link 0 Section
    private Map<String,String> parts = new HashMap<String,String>();

    //Ordered list of keys
    private ArrayList<String> order = new ArrayList<String>();

    //Number of free parts not related to any key, or better related only
    // to the internal key
    private int numFree = 0;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty Route section
     */

    public GaussianRouteSection()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a new Route Section from the list of formatted lines. Lines
     * not pertaining to this section (as from their first keyword) are 
     * ignored.
     * @param lines list of lines to be read
     */

    public GaussianRouteSection(ArrayList<String> lines)
    {
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            line = line.toUpperCase();

            if (!line.startsWith(GaussianConstants.KEYROUTESEC))
                continue;

            //Decode the content of the line
            ArrayList<String> keyValue = lineToKeyValuePair(line);
            
            //Store
            put(keyValue.get(0),keyValue.get(1));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return a pair of strings representing the key:value pair defined in the
     * line of text
     * @param line text to be transformed in a key:value pair
     * @return a <code>key:value</code> pair in the form of a 2-elements
     * vector (i.e., ArrayList) of strings.
     */

    public ArrayList<String> lineToKeyValuePair(String line)
    {
        String actualLine = line.substring(
                                    GaussianConstants.KEYROUTESEC.length());
        String key = "";
        String value = "";
        if (actualLine.contains("="))
        {
            key = actualLine.substring(0,actualLine.indexOf("="));
            value = actualLine.substring(actualLine.indexOf("=") + 1);
            if (!key.startsWith(GaussianConstants.LABLOUDKEY)
                    && !key.startsWith(GaussianConstants.LABMUTEKEY))
            {
                key = GaussianConstants.LABFREE + Integer.toString(numFree);
            }
        } else {
            if (actualLine.startsWith(GaussianConstants.LABFREE))
            {
                value = actualLine.substring(
                                    GaussianConstants.LABFREE.length());
            } else {
                value = actualLine;
            }
            numFree++;
            key = GaussianConstants.LABFREE + Integer.toString(numFree);
        }

        ArrayList<String> keyValuePair = new ArrayList<String>();
        keyValuePair.add(key);
        keyValuePair.add(value);

        return keyValuePair;        
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of keys in the ensemble of key:value fields in this
     * Route section
     * @return the list of key strings
     */

    public Set<String> keySet()
    {
        return parts.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Checks if a key is ontained in this route section. Must be used with
     * the proper prefix for the key, namely
     * {@value autocompchem.wiro.chem.gaussian.GaussianConstants#LABFREE},
     * {@value autocompchem.wiro.chem.gaussian.GaussianConstants#LABLOUDKEY},
     * or {@value autocompchem.wiro.chem.gaussian.GaussianConstants#LABMUTEKEY}.
     * @param key the key to look for
     * @return <code>true</code> if this section contains the specified key
     */

    public boolean containsKey(String key)
    {
        return parts.keySet().contains(key);
    }

//------------------------------------------------------------------------------

    /**
     * Return the complete key:value map
     * @return the map of key:value entries in this section
     */

    public Map<String,String> getKeyValueMap()
    {
        return parts;
    }

//------------------------------------------------------------------------------

    /**
     * Add a key:value pair to this Route Section. The key will be reported in
     * Gaussian input file as 'key=value'. 
     * Label for defining a loud keyword is added by this method.
     * @param key the key of the pair key:value
     * @param value the value corresponding to <code>key</code>
     */

    public void addLoudKeyValue(String key, String value)
    {
        key = GaussianConstants.LABLOUDKEY + key;
        put(key,value);
    }

//------------------------------------------------------------------------------

    /**
     * Add a MUTE-key:value pair to this Route Section. MUTE-key is used only 
     * to identify the value unambiguously, but will not be written in 
     * Gaussian input file.
     * Label for defining a mute keyword is added by this method.
     * @param key the key of the pair key:value
     * @param value the value corresponding to <code>key</code>
     */

    public void addMuteKeyValue(String key, String value)
    {
        key = GaussianConstants.LABMUTEKEY + key;
        put(key,value);
    }

//------------------------------------------------------------------------------

    /**
     * Add a free value to this Route Section. Free values cannot be accessed 
     * or modified directly. These values are treated as simple lines of text
     * when writing this Route Section in an input file for Gaussian.
     * Note that a free part is not related to a key, and thus not traceable by
     * the GaussianRouteSection. Every free part is thus not modifiable.
     * @param value the free value to be stored
     */

    public void addFreeValue(String value)
    {
        numFree++;
        String key = GaussianConstants.LABFREE + Integer.toString(numFree);
        put(key,value);
    }

//------------------------------------------------------------------------------

    /**
     * Put a key-value pair in this Route Section
     * assuming that labels for mute-key, loud-key, and free text
     * are already in the argument provided.
     * If not, it appends the prefix of free text.
     * Note that a free text is not related to a key, and thus not traceable by
     * the GaussianRouteSection. Every free part is thus not modifiable.
     * @param key the key of the pair key:value. The label for mute-key, 
     * loud-key, or free text is already in the argument provided.
     * @param value the value corresponding to <code>key</code>
     */

    public void put(String key, String value)
    {
        if (!key.startsWith(GaussianConstants.LABLOUDKEY) 
                        && !key.startsWith(GaussianConstants.LABMUTEKEY) 
                        && !key.startsWith(GaussianConstants.LABFREE))
        {
            numFree++;
            key = GaussianConstants.LABFREE + Integer.toString(numFree);
        }

        key = key.toUpperCase();
        value = value.toUpperCase();

        parts.put(key,value);

        if (!order.contains(key))
            order.add(key);
    }

//------------------------------------------------------------------------------

    /**
     * Return line referring to a key. Labels for mute-key, loud-key, and free
     * text must be in the argument of this method
     * @param key the string used as key
     * @return the value corresponding to the given key
     */

    public String getValue(String key)
    {
        return parts.get(key);
    }

//------------------------------------------------------------------------------

    /**
     * Return an array of string without new line characters
     * Suitable to print this Route Section for Inp files read by Gaussian.
     * @return the list of lines ready to print a Gaussin input file
     */

    public ArrayList<String> toLinesInp()
    {
        ArrayList<String> lines = new ArrayList<String>();
        Map<String,String> tmp = new HashMap<String,String>();

        //First of all the printing option
        if (parts.keySet().contains(GaussianConstants.SUBKEYPRINT))
        {
            lines.add("#" + parts.get(GaussianConstants.SUBKEYPRINT));
        } else {
            lines.add("#P");
        }

        //Then model chemistry
        String mod = "";
        if (parts.keySet().contains(GaussianConstants.SUBKEYMODELMETHOD))
        {
            mod = parts.get(GaussianConstants.SUBKEYMODELMETHOD);
        } 
        if (parts.keySet().contains(GaussianConstants.SUBKEYMODELBASISET))
        {
            if (!mod.equals(""))
                 mod = mod + "/";
            
            mod = mod + parts.get(GaussianConstants.SUBKEYMODELBASISET);
        }
        lines.add("# " + mod);

        //Now all the rest
        for (int i=0; i<order.size(); i++)
        {
            String key = order.get(i);

            //Printing and method already done
            if (key.equals(GaussianConstants.SUBKEYPRINT) 
                        || key.equals(GaussianConstants.SUBKEYMODELMETHOD) 
                        || key.equals(GaussianConstants.SUBKEYMODELBASISET))
            {
                continue;
            }

            //Avoid double definition of job type
            if (key.equals(GaussianConstants.SUBKEYJOBTYPE))
            {
                continue;
            }

            //For KeyValue report both the key and the value
            if (key.startsWith(GaussianConstants.LABLOUDKEY))
            {
                key = key.substring(3);
                //in case of suboption
                if (key.contains("_$"))
                {
                    String subkey = key.substring(key.indexOf("_$") + 1);
                    key = key.substring(0,key.indexOf("_$"));
                    //What should I print?
                    String toPrint ="";
                    if (subkey.startsWith(GaussianConstants.LABLOUDKEY))
                    {
                        toPrint = subkey.substring(3) + "=" + parts.get(
                                                                  order.get(i));
                    } 
                    else if (subkey.startsWith(GaussianConstants.LABMUTEKEY)) 
                    {
                        toPrint = parts.get(order.get(i));
                    }

                    //since this is a suboption store it in tmp
                    if (tmp.keySet().contains(key))
                    {
                        String old = tmp.get(key);
                        tmp.put(key,old + "," + toPrint);
                    } else {
                        tmp.put(key,key + "=(" + toPrint);
                    }
                } else {
                    lines.add("# " + key + "=" + parts.get(order.get(i)));
                }
            } else if (key.startsWith(GaussianConstants.LABMUTEKEY)) {
                lines.add("# " + parts.get(order.get(i)));
            } else if (key.startsWith(GaussianConstants.LABFREE)) {
                lines.add("# " + parts.get(order.get(i)));
            }
        }

        //Now add also all the options in tmp
        for (String key : tmp.keySet())
        {
            lines.add("# " + tmp.get(key) + ") ");
        }

        //Add job type if not already there
        if (parts.keySet().contains(GaussianConstants.SUBKEYJOBTYPE))
        {
            String jobType = parts.get(GaussianConstants.SUBKEYJOBTYPE);
            if (!tmp.keySet().contains(jobType))
            {
               lines.add("# " + jobType);
            }
        }

        //This section is black line terminated
        lines.add("");

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Return an array of string without newline character
     * Suitable to print this Route Section in autocompchem's JobDetails format.
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJob()
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (int i=0; i<order.size(); i++)
        {
            lines.add(GaussianConstants.KEYROUTESEC + order.get(i) + "=" 
                                                +  parts.get(order.get(i)));
        }
        return lines;
    }

//------------------------------------------------------------------------------

}
