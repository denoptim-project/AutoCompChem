package autocompchem.modeling.atomtuple;


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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.molecule.intcoords.InternalCoord;

/**
 * An ordered set of atom tuples.
 */
public class AnnotatedAtomTupleList extends ArrayList<AnnotatedAtomTuple> 
	implements Cloneable
{
	
//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
    	if ( o== null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	    
 	   AnnotatedAtomTupleList other = (AnnotatedAtomTupleList) o;
   	   	
	   	if (this.size() != other.size())
	   		 return false;
	   	
	   	Iterator<AnnotatedAtomTuple> thisIter = this.iterator();
	   	Iterator<AnnotatedAtomTuple> otherIter = other.iterator();
	   	while (thisIter.hasNext())
	   	{
	   		if (!thisIter.next().equals(otherIter.next()))
	   			return false;
	   	}
	   	
	   	return true;
	}

//-----------------------------------------------------------------------------

	/**
	 * Prints all the constraints into stdout.
	 */
	
	public void printAll() 
	{
		for (AnnotatedAtomTuple c : this)
		{
			System.out.println(" -> " + c);
		}
	}
	
//-----------------------------------------------------------------------------
	
	@Override
	public AnnotatedAtomTupleList clone()
	{
		AnnotatedAtomTupleList clone = new AnnotatedAtomTupleList();
		for(AnnotatedAtomTuple tuple : this)
	   	{
	   		clone.add(tuple.clone());
	   	}
		return clone;
	}
    
//-----------------------------------------------------------------------------
	
}
