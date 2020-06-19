package autocompchem.chemsoftware.qmmm;

import java.util.ArrayList;

/*
 *   Copyright (C) 2016  Marco Foscato
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

import java.util.Arrays;

import autocompchem.run.Terminator;

/**
 * This object represents a keyword with an associated value to be used when
 * constructing {@link QMMMSection}s. Note that the value can be an 
 * ordered sequence of items (e.g. string, numbers) and is always printed in the
 * QMMM input file. On the contrary, the keyword can also be
 * printed in the input file for QMMM, i.e., a so-called 'loud' keyword, or 
 * can be used only in this data structure and not printed in QMMM input files,
 * which instead is referred as to a 'mute' keyword.
 *
 * @author Marco Foscato
 */

public class QMMMKeyword
{
    /**
     * Keyword name
     */
    private String name = "#unnnamedkey";

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

    public QMMMKeyword()
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

    public QMMMKeyword(String name, boolean type, ArrayList<String> value)
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

    public QMMMKeyword(String line)
    {
        String upLine = line.toUpperCase();
        if (upLine.startsWith(QMMMConstants.LABLOUDKEY))
        {
            loudType = true;
            parseLine(line,QMMMConstants.LABLOUDKEY);
        }
        else if (upLine.startsWith(QMMMConstants.LABMUTEKEY))
        {
            loudType = false;
            parseLine(line,QMMMConstants.LABMUTEKEY);
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Expected '" 
                                        + QMMMConstants.LABLOUDKEY + "' or '"
                                        + QMMMConstants.LABMUTEKEY 
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
     * Returns the value associated to this keyword
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
        String[] p = line.split(QMMMConstants.KEYVALSEPARATOR,2);
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
     * Produces a formatted line according to QMMM's format for 
     * the keyword of a section.
     * @return the formatter string
     */

    public String toStringInput()
    {
        StringBuilder sb = new StringBuilder();
        if (loudType)
        {
            sb.append(name.toUpperCase()).append(" ");
        }
        for (String v : value)
        {
            sb.append(v).append(" ");
        }
        return sb.toString();
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted line according to autocompchem's JobDetails format for
     * the keyword of a QMMM section.
     * @return the formatter string
     */

    public String toStringJobDetails()
    {
        StringBuilder sb = new StringBuilder();
        if (loudType)
        {
            sb.append(QMMMConstants.LABLOUDKEY);
        }
        else
        {
            sb.append(QMMMConstants.LABLOUDKEY);
        }
        sb.append(QMMMConstants.KEYVALSEPARATOR);
        for (String v : value)
        {
            sb.append(" ").append(v);
        }
        return sb.toString();
    }

//-----------------------------------------------------------------------------
 
}
