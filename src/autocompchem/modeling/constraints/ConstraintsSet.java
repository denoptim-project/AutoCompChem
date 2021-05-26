package autocompchem.modeling.constraints;

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

import java.util.TreeSet;

import autocompchem.modeling.constraints.Constraint.ConstraintType;

public class ConstraintsSet extends TreeSet<Constraint>
{

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

}
