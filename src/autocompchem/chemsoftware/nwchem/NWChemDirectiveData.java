package autocompchem.chemsoftware.nwchem;

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
 * This object represents a block of formatted data pertaining to an NWChem 
 * input directive.
 *
 * @author Marco Foscato
 */

public class NWChemDirectiveData
{

    /**
     * Name of this block of data
     */
    private String name = "#noname";

    /**
     * Data collected as an ordered list of lines of text
     */
    private ArrayList<String> lines;

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty data block
     */

    public NWChemDirectiveData()
    {
        lines = new ArrayList<String>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from formatted text (i.e., jobdetails line)
     * @param line the text to parse
     */

    public NWChemDirectiveData(String line)
    {
        if (line.toUpperCase().startsWith(NWChemConstants.LABDATA))
        {
            line = line.substring(NWChemConstants.LABDATA.length());
        }
        String[] parts = line.split(NWChemConstants.DATAVALSEPARATOR,2);
        name = parts[0];
        if (parts.length < 2)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot discriminate between "
                                       + "reference name and block of data in '"
                                       + line + "'. Check jobdetails file.",-1);
        }
        String block = parts[1];
        if (block.toUpperCase().startsWith(NWChemConstants.LABOPENBLOCK))
        {
            block = block.substring(NWChemConstants.LABOPENBLOCK.length());
        }
        if (block.toUpperCase().startsWith(System.getProperty(
                                                             "line.separator")))
        {
            block = block.substring(System.getProperty(
                                                    "line.separator").length());
        }
        String[] dataLines = block.split(System.getProperty("line.separator"));
        lines = new ArrayList<String>(Arrays.asList(dataLines));
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from content
     * @param name the name of this block of data
     * @param lines the list of lines representing the contained data
     */

    public NWChemDirectiveData(String name, ArrayList<String> lines)
    {
        this.name = name;
        this.lines = lines;        
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name of this block of data
     * @return the name of this block of data
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------

    /**
     * Return the content of this block of data
     * @return the content of this block of data
     */

    public ArrayList<String> getContent()
    {
        return lines;
    }

//-----------------------------------------------------------------------------

    /** 
     * Set the content of this data clock
     * @param lines the content to impose
     */

    public void setContent(ArrayList<String> lines)
    {
        this.lines = lines;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of NWChem input directives.
     * @return the list of lines for a NWChem inout file
     */

    public ArrayList<String> toLinesInput()
    {
        return lines;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of NWChem input directives.
     * @return the list of lines for a NWChem inout file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        if (lines.size() > 1)
        {
            lines.set(0,NWChemConstants.LABOPENBLOCK + lines.get(0));
            lines.add(NWChemConstants.LABCLOSEBLOCK);
        }
        lines.set(0, NWChemConstants.LABDATA + name 
                             + NWChemConstants.DATAVALSEPARATOR + lines.get(0));
        return lines;
    }

//-----------------------------------------------------------------------------

}
