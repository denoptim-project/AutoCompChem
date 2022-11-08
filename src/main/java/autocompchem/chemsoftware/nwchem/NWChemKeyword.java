package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;

/*
 *   Copyright (C) 2016  Marco Foscato
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

import java.util.Arrays;

import autocompchem.run.Terminator;

/**
 * This object represents a keyword with an associated value to be used in
 * constructing {@link NWChemDirective}s. Note that the value can be an 
 * ordered sequence of items (string, numbers).
 *
 * @author Marco Foscato
 */

public class NWChemKeyword
{
    /**
     * Keyword name
     */
    private String name = "#nokeyword";

    /**
     * Keyword type (false=mute, true=load)
     */
    private boolean loudType = false;

    /**
     * Keyword value
     */
    private ArrayList<String> value;


//-----------------------------------------------------------------------------

    /**
     * Constructor for empty keyword
     */

    public NWChemKeyword()
    {
        value = new ArrayList<String>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a keyword from name, type, and value
     * @param name the name of the keyword (i.e., the actual keyword)
     * @param type use <code>false</code> to specify a mute-type of keyword
     * or <code>true</code> for loud type.
     * @param value the value of the keywords
     */

    public NWChemKeyword(String name, boolean type, ArrayList<String> value)
    {
        this.name = name;
        this.loudType = type;
        this.value = value;
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from formatted text (i.e., jobdetails line)
     * @param line the formatted text to be parsed
     */

    public NWChemKeyword(String line)
    {
        String upLine = line.toUpperCase();
        if (upLine.startsWith(NWChemConstants.LABLOUDKEY))
        {
            loudType = true;
            parseLine(line,NWChemConstants.LABLOUDKEY);
        }
        else if (upLine.startsWith(NWChemConstants.LABMUTEKEY))
        {
            loudType = false;
            parseLine(line,NWChemConstants.LABMUTEKEY);
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Expected '" 
                                        + NWChemConstants.LABLOUDKEY + "' or '"
                                        + NWChemConstants.LABMUTEKEY 
                                        + "' in fromt of keyword '" + line 
                                        + "'.",-1);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the name (i.e., the actual keyword) of this keyword
     * @return the name of this keyword
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the value associated to  this keyword
     * @return the value of the keywords
     */

    public ArrayList<String> getValue()
    {
        return value;
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the value corresponding to this keyword
     * @param value the value of the keywords
     */

    public void setValue(ArrayList<String> value)
    {
        this.value = value;
    }

//-----------------------------------------------------------------------------

    /**
     * Parse a jobdetails line and get keyword name and value
     * @param line the line to parse
     * @param label the label identifying the type of keyword
     */

    private void parseLine(String line, String label)
    {
        String[] p = line.split(NWChemConstants.KEYVALSEPARATOR,2);
        name = p[0].substring(label.length());
        if (p.length > 1)
        {
            value = new ArrayList<String>(Arrays.asList(p[1].split("\\s+")));
        }
        else
        {
            value = new ArrayList<String>(0);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted line according to NWChem's format for 
     * the keyword of a directive.
     * @return the formatted string ready to print this keyword in an 
     * input file for NWChem
     */

    public String toStringInput()
    {
        StringBuilder sb = new StringBuilder();
        if (loudType)
        {
            sb.append(name);
        }
        for (String v : value)
        {
            sb.append(" ").append(v);
        }
        return sb.toString();
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted line according to autocompchem's JobDetails format for
     * the keyword of an NWChem directive.
     * @return the formatted string ready to print the line setting this
     * keyword in a jobDetails file
     */

    public String toStringJobDetails()
    {
        StringBuilder sb = new StringBuilder();
        if (loudType)
        {
            sb.append(NWChemConstants.LABLOUDKEY);
        }
        else
        {
            sb.append(NWChemConstants.LABMUTEKEY);
        }
        sb.append(name).append(NWChemConstants.KEYVALSEPARATOR);
        for (String v : value)
        {
            sb.append(v).append(" ");
        }
        return sb.toString();
    }

//-----------------------------------------------------------------------------
 
}
