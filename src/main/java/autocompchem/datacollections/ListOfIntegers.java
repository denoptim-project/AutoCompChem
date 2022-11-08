package autocompchem.datacollections;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class representing an ordered list of integers.
 * 
 * @author Marco Foscato
 */

public class ListOfIntegers extends ArrayList<Integer>
{
	/**
	 * Version ID
	 */
	private static final long serialVersionUID = -7082667876844386725L;
	
//------------------------------------------------------------------------------

	/**
     * Construct an empty TextBlock
     */

    public ListOfIntegers()
    {
    	super();
    }

//------------------------------------------------------------------------------

    /**
     * Construct an empty TextBlock
     */

    public ListOfIntegers(Collection<Integer> c)
    {
    	super(c);
    }
      
//------------------------------------------------------------------------------

    /**
     * Append a value 
     * @param value the text to append
     */

    public void appendText(Integer value)
    {
        super.add(value);
    }

//------------------------------------------------------------------------------

}
