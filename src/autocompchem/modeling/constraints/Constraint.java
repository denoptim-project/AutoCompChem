package autocompchem.modeling.constraints;

import java.util.ArrayList;

public class Constraint implements Comparable<Constraint>
{
	public enum ConstraintType {FROZENATM, DISTANCE, ANGLE, DIHEDRAL, UNDEFINED}
	
	/**
	 * The type of this constraint
	 */
	private ConstraintType type = ConstraintType.UNDEFINED;
	
	/**
	 * The 0-based atom ids
	 */
	private int[] atmIDs = new int[]{-1,-1,-1,-1};
	
	/**
	 * A given value for this constraint
	 */
	private double value;
	
//------------------------------------------------------------------------------
	
	public Constraint(int i)
	{
		this.type = ConstraintType.FROZENATM;
		this.atmIDs[0] = i;
	}
	
//------------------------------------------------------------------------------

	public Constraint(int i, int j)
	{
		this.type = ConstraintType.DISTANCE;
		this.atmIDs[0] = i;
		this.atmIDs[1] = j;
	}
//------------------------------------------------------------------------------

	public Constraint(int i, int j, int k)
	{
		this.type = ConstraintType.ANGLE;
		this.atmIDs[0] = i;
		this.atmIDs[1] = j;
		this.atmIDs[2] = k;
	}
	
//------------------------------------------------------------------------------

	public Constraint(int i, int j, int k, int l)
	{
		this.type = ConstraintType.DIHEDRAL;
		this.atmIDs[0] = i;
		this.atmIDs[1] = j;
		this.atmIDs[2] = k;
		this.atmIDs[3] = l;
	}
	
//------------------------------------------------------------------------------

	public Constraint(int i, int j, double value)
	{
		this(i,j);
		this.value = value;
	}
	
//------------------------------------------------------------------------------

	public Constraint(int i, int j, int k, double value)
	{
		this(i,j,k);
		this.value = value;
	}
	
//------------------------------------------------------------------------------

	public Constraint(int i, int j, int k, int l, double value)
	{
		this(i,j,k,l);
		this.value = value;
	}

//------------------------------------------------------------------------------

	public static Constraint buildConstraint(ArrayList<Integer> ids) 
			throws Exception
	{
		switch (ids.size())
		{
			case 1:
				return new Constraint(ids.get(0));
			case 2:
				return new Constraint(ids.get(0),ids.get(1));
			case 3:
				return new Constraint(ids.get(0),ids.get(1),ids.get(2));
			case 4:
				return new Constraint(ids.get(0),ids.get(1),ids.get(2),
						ids.get(3));
			default:
				throw new Exception("Unexpected number of atom IDs (" 
						+ ids.size() + "). Cannot construct a Constraint.");
		}
	}
	
//------------------------------------------------------------------------------

	public static Constraint buildConstraint(ArrayList<Integer> ids, 
			double value) throws Exception
	{
		switch (ids.size())
		{
			case 2:
				return new Constraint(ids.get(0),ids.get(1),value);
			case 3:
				return new Constraint(ids.get(0),ids.get(1),ids.get(2),value);
			case 4:
				return new Constraint(ids.get(0),ids.get(1),ids.get(2),
						ids.get(3),value);
			default:
				throw new Exception("Unexpected number of atom IDs (" 
						+ ids.size() + "). Cannot construct a Constraint.");
		}
	}
	
//------------------------------------------------------------------------------
	
	public ConstraintType getType()
	{
		return type;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Constraint [Type=");
		sb.append(type).append(", IDs=[").append(atmIDs[0]).append(",");
		sb.append(atmIDs[1]).append(",").append(atmIDs[2]).append(",");
		sb.append(atmIDs[3]).append("], value=").append(value);
		sb.append("] ");
		return sb.toString();
	}

//------------------------------------------------------------------------------

	public int[] getAtomIDs()
	{
		return atmIDs;
	}
	
//------------------------------------------------------------------------------

	private int getNumberOfIDs()
	{
		int n = 0;
		for (int i : atmIDs)
		{
			if (i>-1)
				n++;
		}
		return n;
	}
//------------------------------------------------------------------------------
	
	@Override
	public int compareTo(Constraint o) 
	{	
		if (this.getNumberOfIDs() == o.getNumberOfIDs())
		{
			if (this.getAtomIDs()[0] == o.getAtomIDs()[0]
					&& this.getNumberOfIDs() > 1)
			{
				if (this.getAtomIDs()[1] == o.getAtomIDs()[1]
						&& this.getNumberOfIDs() > 2)
				{
					if (this.getAtomIDs()[2] == o.getAtomIDs()[2]
							&& this.getNumberOfIDs() > 3)
					{
						return Integer.compare(this.getAtomIDs()[3],
								o.getAtomIDs()[3]);
					} else {
						return Integer.compare(this.getAtomIDs()[2],
								o.getAtomIDs()[2]);
					}
				} else {
					return Integer.compare(this.getAtomIDs()[1],
							o.getAtomIDs()[1]);
				}
			} else {
				return Integer.compare(this.getAtomIDs()[0], o.getAtomIDs()[0]);
			}
		} else {
			return Integer.compare(this.getNumberOfIDs(), o.getNumberOfIDs());
		}
	}
	
//------------------------------------------------------------------------------

}
