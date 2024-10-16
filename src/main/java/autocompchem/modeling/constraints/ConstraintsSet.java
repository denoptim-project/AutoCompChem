package autocompchem.modeling.constraints;


import java.util.Iterator;
import java.util.Objects;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.TreeSet;

import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.molecule.intcoords.InternalCoord;

/**
 * An ordered collection of constraints.
 */
public class ConstraintsSet extends TreeSet<Constraint> implements Cloneable
{
	/**
	 * Version ID
	 */
	private static final long serialVersionUID = 1L;

//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ConstraintsSet))
			return false;
		
		ConstraintsSet other = (ConstraintsSet) o;
	   	
	   	if (this.size() != other.size())
	   		 return false;
	   	
	   	Iterator<Constraint> thisIter = this.iterator();
	   	Iterator<Constraint> otherIter = other.iterator();
	   	while (thisIter.hasNext())
	   	{
	   	        if (!thisIter.next().equals(otherIter.next()))
	   	                return false;
	   	}
	   	return true;
	}
	
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(super.hashCode());
    }
	
//-----------------------------------------------------------------------------

	@Override
	public ConstraintsSet clone()
	{
		ConstraintsSet clone = new ConstraintsSet();
		for(Constraint c : this)
	   	{
	   		clone.add(c.clone());
	   	}
		return clone;
	}

//-----------------------------------------------------------------------------

	/**
	 * Prints all the constraints into stdout.
	 */
	
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		String NL = System.getProperty("line.separator");
		sb.append("List of constraints: ").append(NL);
		for (Constraint c : this)
		{
			sb.append(" -> "+c+NL);
		}
		return sb.toString();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Extracts only the constraints of the given type.
	 * @param type the type of constraints to return.
	 * @return the list of constraints with the given type. If the type is
	 * not included in this set, then we return an empty list.
	 */
	
	public ConstraintsSet getConstrainsWithType(ConstraintType type)
	{
		ConstraintsSet subset = new ConstraintsSet();
		for (Constraint c : this)
		{
			if (type.equals(c.getType()))
				subset.add(c);
		}
		return subset;
	}

//-----------------------------------------------------------------------------

    /**
     * Searches for the tuple defining the given {@link InternalCoord}inate. 
     * Ignores the type of internal coordinate (e.g., does not distinguish
     * between proper or improper torsion), but considers different and
     * equivalent ordering of the indexes (e.g., does not distinguish between 
     * ABC and CBA).
     * @param ic the internal coordinate to search for.
     * @return <code>true</code> if the tuple of indexes defining the given
     * internal coordinate is found in any constrain present in this set.
     */
    public boolean containsInternalCoord(InternalCoord ic) 
    {
    	Iterator<Constraint> iter = this.iterator();
	   	while (iter.hasNext())
	   	{
	   		if (ic.compareIDs(iter.next().getAtomIDs()))
	   			return true;
	   	}
		return false;
	}
    
//-----------------------------------------------------------------------------
	
}
