package autocompchem.chemsoftware;

/*
 *   Copyright (C) 2020  Marco Foscato
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

import java.util.ArrayList;
import java.util.Arrays;

import autocompchem.run.Terminator;

/**
 * This object represents a keyword with an associated value to be used in
 * constructing {@link Directive}s for computational chemistry software. 
 * Note that the value can be an ordered sequence of items (string, numbers).
 * Keyword can be either "mute" or "loud". A "loud" keyword is written in 
 * the software's input file as <code>KEY&lt;SEPARATOR&gt;VALUE</code> while 
 * a "mute" keyword is given to the designated software using only its 
 * <code>VALUE</code>. In other words,
 * the <code>KEY</code> part (i.e., the name) of a "mute" keyword is omitted 
 * when preparing the input file for the designated software.
 *
 * @author Marco Foscato
 */

public class Keyword implements IDirectiveComponent
{
    /**
     * Keyword name.
     */
    private String name = "#nokeyword";

    /**
     * Keyword type: either "mute" (false), or "loud" (true).
     */
    private boolean isLoud = false;

    /**
     * Keyword value.
     */
    private ArrayList<String> value;


//-----------------------------------------------------------------------------

    /**
     * Constructor for empty keyword
     */

    public Keyword()
    {
        value = new ArrayList<String>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a keyword from name, type, and value.
     * @param name the name of the keyword (i.e., the actual keyword).
     * @param isLoud use <code>false</code> to specify a mute-type of keyword
     * or <code>true</code> for loud type.
     * @param value the value of the keywords.
     */

    public Keyword(String name, boolean isLoud, ArrayList<String> value)
    {
        this.name = name;
        this.isLoud = isLoud;
        this.value = value;
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from formatted text (i.e., job details line).
     * @param line the formatted text to be parsed.
     */

    public Keyword(String line)
    {
        String upLine = line.toUpperCase();
        if (upLine.startsWith(ChemSoftConstants.JDLABLOUDKEY))
        {
            isLoud = true;
            parseLine(line,ChemSoftConstants.JDLABLOUDKEY);
        }
        else if (upLine.startsWith(ChemSoftConstants.JDLABMUTEKEY))
        {
            isLoud = false;
            parseLine(line,ChemSoftConstants.JDLABMUTEKEY);
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Expected '" 
            		+ ChemSoftConstants.JDLABLOUDKEY + "' or '" 
            		+ ChemSoftConstants.JDLABMUTEKEY + "' in front of key"
            		+ " name '" + line + "'.",-1);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the name (i.e., the actual keyword) of this keyword.
     * @return the name of this keyword.
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------
    
    /**
     * @return the kind of directive component this is.
     */
    
	public DirectiveComponent getComponentType() 
	{
		return DirectiveComponent.KEYWORD;
	}
	
//-----------------------------------------------------------------------------

    /**
     * Returns the value associated to  this keyword.
     * @return the value of the keywords.
     */

    public ArrayList<String> getValue()
    {
        return value;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if this keyword is loud, meaning that it is 
     * meant to be written as a <code>KEY&lt;SEPARATOR&gt;VALUE</code> pair
     */
    public boolean isLoud()
    {
    	return isLoud;
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the value corresponding to this keyword.
     * @param value the value of the keywords.
     */

    public void setValue(ArrayList<String> value)
    {
        this.value = value;
    }

//-----------------------------------------------------------------------------

    /**
     * Parse a job details line and get keyword name and value.
     * @param line the line to parse.
     * @param label the label identifying the type of keyword.
     */

    private void parseLine(String line, String label)
    {
        String[] p = line.split(ChemSoftConstants.JDKEYVALSEPARATOR,2);
        name = p[0].substring(label.length()).trim();
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
     * Checks if there is any ACC task definition within this directive.
     * @return <code>true</code> if there is at least one ACC task definition.
     */
    
    public boolean hasACCTask()
    {
    	for (String l : value)
    	{
    		if (l.contains(ChemSoftConstants.JDLABACCTASK))
    		{
    			return true;
    		}
    	}
    	return false;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted line according to autocompchem's job details format.
     * @return the formatted string ready to print the line setting this
     * keyword in a job details file
     */

    public String toStringJobDetails()
    {
        StringBuilder sb = new StringBuilder();
        if (isLoud)
        {
            sb.append(ChemSoftConstants.JDLABLOUDKEY);
        }
        else
        {
            sb.append(ChemSoftConstants.JDLABMUTEKEY);
        }
        sb.append(name).append(ChemSoftConstants.JDKEYVALSEPARATOR);
        for (String v : value)
        {
            sb.append(v).append(" ");
        }
        return sb.toString();
    }

//-----------------------------------------------------------------------------
 
}
