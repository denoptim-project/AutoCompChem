package autocompchem.molecule.vibrations;

/*
 *   Copyright (C) 2014  Marco Foscato
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class representing an ordered list of normal modes.
 * 
 * @author Marco Foscato
 */

public class NormalModeSet extends ArrayList<NormalMode> implements Cloneable
{
	/**
	 * Version ID
	 */
	private static final long serialVersionUID = -7082667876844386725L;
	
//------------------------------------------------------------------------------

	/**
     * Construct an empty list
     */

    public NormalModeSet()
    {
    	super();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a list of modes from any collection.
     * @params c the collection
     */

    public NormalModeSet(Collection<NormalMode> c)
    {
    	super(c);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets a specific component for a specific atom index in a specific mode.
     * @param modeId the index of the more to edit (0-N). If the given value 
     * is larger than the current
     * list, then an appropriate number of empty modes is added to this set.
     * @param atmId the index of the atom (0-N). If the given value is larger 
     * than the current
     * list, then an appropriate number of empty components is added.
     * @param componentId set to 0 for X, 1 for Y, and 2 for Z
     * @param value the numerical value to impose.
     */
    public void setComponent(int modeId, int atmId, int componentId, 
    		Double value)
    {
    	if (modeId>=this.size())
    	{
    		for (int i=this.size(); i<(modeId+1); i++)
    		{
    			this.add(new NormalMode());
    		}
    	}
    	this.get(modeId).setComponent(atmId, componentId, value);
    }
    
//------------------------------------------------------------------------------

    /**
     * Append a value 
     * @param nm the text to append
     */

    public void append(NormalMode nm)
    {
        super.add(nm);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a deep copy of this set
     */
    
    @Override
    public NormalModeSet clone()
    {
    	NormalModeSet cnms = new NormalModeSet();
    	for (NormalMode nm : this)
    	{
    		cnms.add(nm.clone());
    	}
    	return cnms;
    }

//------------------------------------------------------------------------------

    /**
     * @returns a text for human readers
     */
    
    public String toLines()
    {
    	StringBuilder sb = new StringBuilder();
    	boolean first = true;
    	for (NormalMode nm : this)
    	{
    		if (first)
    		{
    			first = false;
    		} else {
    		    sb.append(System.getProperty("line.separator"));
    		}
    		sb.append(nm.toLines());
    	}
    	return sb.toString();
    }
    
//------------------------------------------------------------------------------

}
