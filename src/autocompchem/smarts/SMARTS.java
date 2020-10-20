package autocompchem.smarts;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Object representing a single SMARTS query
 *
 * @author Marco Foscato
 */


public class SMARTS
{
    /**
     * String representation
     */
    private String smartsAsString = "";

    /**
     * Flag to record if this SMARTS is a single-atom SMARTS
     */
    private boolean isSingleAtom = false;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty SMARTS
     */

    public SMARTS()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a SMARTS from a string
     * @param str the string representation of the SMARTS 
     */

    public SMARTS(String str)
    {
        this.smartsAsString = str;
        this.isSingleAtom = isSingleAtomSMARTS(str);
    }

//------------------------------------------------------------------------------

    public String getString()
    {
    	return smartsAsString;
    }
    
//------------------------------------------------------------------------------

    /**
     * Evaluates if the SMARTS query is a single-atom SMARTS. A single-atom
     * SMARTS is a query that is meant to match groups of atoms not larger than
     * one atom. 
     * @param s the string to analyze
     * @return <code>true</code> if the string looks like a single atom SMARTS
     */

    public static boolean isSingleAtomSMARTS(String s)
    {
        boolean res = false;
        if (s.trim().startsWith("[") && s.trim().endsWith("]"))
        {
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

}
