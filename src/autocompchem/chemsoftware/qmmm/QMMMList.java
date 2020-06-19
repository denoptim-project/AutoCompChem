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
 * This object represents a block of formatted data pertaining to a QMMM 
 * input section (i.e., a 'list' in QMMM users manual).
 *
 * @author Marco Foscato
 */

public class QMMMList
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

    public QMMMList()
    {
        lines = new ArrayList<String>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from formatted text (i.e., jobdetails line)
     * @param line the text to parse
     */

    public QMMMList(String line)
    {
        if (line.toUpperCase().startsWith(QMMMConstants.LABDATA))
        {
            line = line.substring(QMMMConstants.LABDATA.length());
        }
        String[] parts = line.split(QMMMConstants.DATAVALSEPARATOR,2);
        name = parts[0];
        if (parts.length < 2)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot discriminate between "
                                       + "reference name and content of the "
                                       + "list in '" + line 
				       + "'. Check jobdetails file.",-1);
        }
        String block = parts[1];
        if (block.toUpperCase().startsWith(QMMMConstants.LABOPENBLOCK))
        {
            block = block.substring(QMMMConstants.LABOPENBLOCK.length());
        }
        String[] dataLines = block.split(System.getProperty("line.separator"));
        lines = new ArrayList<String>(Arrays.asList(dataLines));
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from content
     * @param name the name of this list
     * @param lines the list of lines representing the content of the list
     */

    public QMMMList(String name, ArrayList<String> lines)
    {
        this.name = name;
        this.lines = lines;        
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name of this list
     * @return the name of this list
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------

    /**
     * Return the content of this list
     * @return the content of this list
     */

    public ArrayList<String> getContent()
    {
        return lines;
    }

//-----------------------------------------------------------------------------

    /** 
     * Set the content of this list
     * @param lines the content to impose
     */

    public void setContent(ArrayList<String> lines)
    {
	this.lines = lines;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of QMMM input sections.
     * @return the list of lines for a QMMM inout file
     */

    public ArrayList<String> toLinesInput()
    {
	ArrayList<String> inpFileLines = new ArrayList<String>();
	inpFileLines.add(name.toUpperCase());
	for (String line : lines)
	{
	    inpFileLines.add(QMMMConstants.SUBSECTIONINDENT + line);
	}
	inpFileLines.add("END");
        return inpFileLines;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of QMMM input sections.
     * @return the list of lines for a QMMM inout file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        if (lines.size() > 1 
	    && !lines.get(0).startsWith(QMMMConstants.LABOPENBLOCK))
        {
            lines.set(0,QMMMConstants.LABOPENBLOCK + lines.get(0));
            lines.add(QMMMConstants.LABCLOSEBLOCK);
        }
        return lines;
    }

//-----------------------------------------------------------------------------

}
