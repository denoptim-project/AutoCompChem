package autocompchem.modeling.constraints;


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
 * An ordered collection of constraints.
 */
public class ConstraintsSet extends TreeSet<Constraint>
{

	/**
	 * total number of atoms in the system.
	 */
	private int numAtoms = 0;
	
//-----------------------------------------------------------------------------
	
	/**
	 * @return the number of atoms in the system from which these constraints
	 * are generated.
	 */
	public int getNumAtoms() 
	{
		return numAtoms;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Sets the number of atoms in the system from which these constraints
	 * are generated.
	 * @param numAtoms the number of atoms.
	 */
	protected void setNumAtoms(int numAtoms) 
	{
		this.numAtoms = numAtoms;
	}
	
//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ConstraintsSet))
			return false;
		ConstraintsSet other = (ConstraintsSet) o;
   	 
	   	if (this.numAtoms != other.numAtoms)
	   		 return false;
	   	
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

	/**
	 * Prints all the constraints into stdout.
	 */
	
	public void printAll() 
	{
		System.out.println("List of constraints: ");
		for (Constraint c : this)
		{
			System.out.println(" -> "+c);
		}
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

    public static class ConstraintsSetSerializer 
    implements JsonSerializer<ConstraintsSet>
    {
		@Override
		public JsonElement serialize(ConstraintsSet src, Type typeOfSrc, 
				JsonSerializationContext context) 
		{
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("numAtoms", src.numAtoms);
            jsonObject.add("constraints", context.serialize(src.toArray()));
            return jsonObject;
		}
    }
    
//-----------------------------------------------------------------------------

    public static class ConstraintsSetDeserializer 
      implements JsonDeserializer<ConstraintsSet>
    {
		@Override
		public ConstraintsSet deserialize(JsonElement json, Type typeOfT, 
				JsonDeserializationContext context)
				throws JsonParseException 
		{
			JsonObject jo = json.getAsJsonObject();
			ConstraintsSet cs = new ConstraintsSet();
			cs.setNumAtoms(Integer.parseInt(jo.get("numAtoms").getAsString()));
			for (JsonElement jel : jo.get("constraints").getAsJsonArray())
			{
				cs.add(context.deserialize(jel, Constraint.class));
			}
			return cs;
		}
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
	   		if (ic.compareIDs(iter.next().getAtomIDsList()))
	   			return true;
	   	}
		return false;
	}
    
//-----------------------------------------------------------------------------
	
}
