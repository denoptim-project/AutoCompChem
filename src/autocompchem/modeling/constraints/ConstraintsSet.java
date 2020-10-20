package autocompchem.modeling.constraints;

import java.util.TreeSet;

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
	
	//TODO add get by type
}
