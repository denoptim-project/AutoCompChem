package autocompchem.chemsoftware;

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
 * This object represents a block of text-based data.
 *
 * @author Marco Foscato
 */

public class DirectiveData
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

    public DirectiveData()
    {
        lines = new ArrayList<String>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from formatted text (i.e., job details line)
     * @param line the text to parse
     */

    public DirectiveData(String line)
    {
        if (line.toUpperCase().startsWith(ChemSoftConstants.JDLABDATA))
        {
            line = line.substring(ChemSoftConstants.JDLABDATA.length());
        }
        String[] parts = line.split(ChemSoftConstants.JDDATAVALSEPARATOR,2);
        name = parts[0];
        if (parts.length < 2)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot discriminate between "
                                       + "reference name and block of data in '"
                                       + line + "'. Check jobdetails file.",-1);
        }
        String block = parts[1];
        if (block.toUpperCase().startsWith(ChemSoftConstants.JDLABOPENBLOCK))
        {
            block = block.substring(ChemSoftConstants.JDLABOPENBLOCK.length());
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

    public DirectiveData(String name, ArrayList<String> lines)
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
     * @return the list of lines for a NWChem input file
     */

    public ArrayList<String> toLinesJobDetails()
    {
      	ArrayList<String> toJD = new ArrayList<String>();
    	
        if (lines.size() > 1)
        {
        	toJD.add(ChemSoftConstants.JDLABDATA + name 
            		+ ChemSoftConstants.JDDATAVALSEPARATOR
            		+ ChemSoftConstants.JDLABOPENBLOCK + lines.get(0));
        	for (int i=1; i<lines.size(); i++)
        	{
        		toJD.add(lines.get(i));
        	}
            toJD.add(ChemSoftConstants.JDLABCLOSEBLOCK);
        } else
        {
        	toJD.add(ChemSoftConstants.JDLABDATA + name 
            		+ ChemSoftConstants.JDDATAVALSEPARATOR + lines.get(0));
        }
        
        return toJD;
    }

//-----------------------------------------------------------------------------

}
