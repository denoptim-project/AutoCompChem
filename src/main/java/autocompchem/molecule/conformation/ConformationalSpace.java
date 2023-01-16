package autocompchem.molecule.conformation;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.ConstraintsSet;


/**
 * The conformational space as the combination of a list of conformational
 * changes (i.e., the conformational coordinates).
 * 
 * @author Marco Foscato 
 */

public class ConformationalSpace extends TreeSet<ConformationalCoordinate> 
	implements Cloneable
{
    /**
     * Unique counter for coordinates names
     */
    private final AtomicInteger CRDID = new AtomicInteger(0);

//------------------------------------------------------------------------------

    /**
     * Get unique name for coord
     * @return a string unique within this ConformationalSpace
     */

    @Deprecated
    public String getUnqCoordName()
    {
        return ConformationalCoordDefinition.BASENAME + CRDID.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Get number of combinations. The number of combinations represent the
     * size of this ConformationalSpace.
     * @return the number of combinations, or the largest calculated number 
     * multiplied by -1 if the actual result is to big 
     * to be calculated.
     */

    public int getSize()
    {
        int sz = 1;
        for (ConformationalCoordinate cc : this)
        {
            sz = sz * cc.getFold();
            int diff = Integer.MAX_VALUE - sz;
            if (diff < 10000)
            {
                sz = -sz;
                break;
            }
        }
        return sz;
    }
	
//------------------------------------------------------------------------------

  	@Override
  	public boolean equals(Object o)
  	{
  		if (!(o instanceof ConformationalSpace))
  			return false;
  		
  		ConformationalSpace other = (ConformationalSpace) o;
     	 
  	   	if (this.size() != other.size())
  	   		 return false;
  	   	
  	   	Iterator<ConformationalCoordinate> thisIter = this.iterator();
  	   	Iterator<ConformationalCoordinate> otherIter = other.iterator();
  	   	while (thisIter.hasNext())
  	   	{
   	        if (!thisIter.next().equals(otherIter.next()))
   	            return false;
  	   	}
  	   	return true;
  	}

//-----------------------------------------------------------------------------

  	/**
  	 * Prints all the conformational coordinates into stdout.
  	 */
  	
  	public void printAll() 
  	{
  		System.out.println("Conformational space is defined by: ");
  		for (ConformationalCoordinate c : this)
  		{
  			System.out.println(" -> "+c);
  		}
  	}

//------------------------------------------------------------------------------
    
}
