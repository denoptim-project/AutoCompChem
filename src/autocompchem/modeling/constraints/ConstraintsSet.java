package autocompchem.modeling.constraints;

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
