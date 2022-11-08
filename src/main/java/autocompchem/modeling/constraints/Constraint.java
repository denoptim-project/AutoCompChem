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

import java.util.ArrayList;

/**
 * This class represents the concept of a constraint that is applied on a list
 * of centers, which are represented by indexes (i.e., atom indexes in an atom list).
 * Notably, a constrain in itself might not define whether it is freezing 
 * an internal coordinate or applying some sort of potential to it. This aspect
 * is quite software-specific and therefore does not pertain this software-agnostic class.
 * Upon translating the constraints into software-specific input information, 
 * the distinction between freezing or applying a potential should be made clear
 * by the context in which the constraints are used.
 * 
 * @author Marco Foscato
 */

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
	
	/**
	 * Flag signaling this constrain uses a value
	 */
	private boolean hasValue = false;
	
	/**
	 * A given optional setting for this constraint. Examples are the options
	 * telling the comp. chem. software what to do with this constraints, i.e.,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint).
	 */
	private String options;
	
	/**
	 * Flag signaling this constrain uses options.
	 */
	private boolean hasOpt = false;

//------------------------------------------------------------------------------

	/**
	 * Builds an undefined constraint
	 */
	public Constraint()
	{}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for a frozen atom.
	 * @param i the index of the atom to freeze.
	 */
	public Constraint(int i)
	{
		this(i, -1, -1, -1, ConstraintType.FROZENATM, null, null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for a distance
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 */
	public Constraint(int i, int j)
	{
		this(i, j, -1, -1, ConstraintType.DISTANCE, null, null);
	}
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for an angle.
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 * @param k third index (0-based)
	 */
	public Constraint(int i, int j, int k)
	{
		this(i, j, k, -1, ConstraintType.ANGLE, null, null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for a dihedral.
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 * @param k third index (0-based)
	 * @param l forth index (0-based)
	 */
	public Constraint(int i, int j, int k, int l)
	{
		this(i, j, k, l, ConstraintType.DIHEDRAL, null, null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for a distance
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 * @param value a numerical value to be assigned to the constraint. Or null
	 * if no value is to be set.
	 */
	public Constraint(int i, int j, double value)
	{
		this(i, j, -1, -1, ConstraintType.DISTANCE, Double.valueOf(value),null);
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Constructs a constraint for an angle.
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 * @param k third index (0-based)
	 * @param value a numerical value to be assigned to the constraint. Or null
	 * if no value is to be set.
	 */
	public Constraint(int i, int j, int k, double value)
	{
		this(i, j, k, -1, ConstraintType.ANGLE, Double.valueOf(value), null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint of any kind offering the possibility to define
	 * a specific value and options to assign to the constraint.
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 * @param k third index (0-based)
	 * @param l forth index (0-based)
	 * @param value a numerical value to be assigned to the constraint. Or null
	 * if no value is to be set.
	 * @param type the type of constraint. This defines how many of the indexes
	 * are actually used.
	 * @param options additional string usually used to tell the comp. chem.
	 * software how to use the given information. For example,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint). Or null, if no option has to be given.
	 */
	public Constraint(int i, int j, int k, int l, 
			ConstraintType type, Double value, String options)
	{
		this.atmIDs[0] = i;
		this.atmIDs[1] = j;
		this.atmIDs[2] = k;
		this.atmIDs[3] = l;
		if (value != null)
		{
			this.value = value.doubleValue();
			this.hasValue = true;
		}
		if (options != null)
		{
			this.options = options;
			this.hasOpt = true;
		}
		this.type = type;
	}

//------------------------------------------------------------------------------

	/**
	 * Constructs a constrain.
	 * @param ids atom IDs. The number of value determines the type of 
	 * constraint.
	 * @throws Exception
	 */
	public static Constraint buildConstraint(ArrayList<Integer> ids) 
			throws Exception
	{
		return buildConstraint(ids, null, null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constrain.
	 * @param ids atom IDs. The number of value determines the type of 
	 * constraint.
	 * @param value a numerical value to be assigned to the constraint. Or null
	 * if no value is to be set.
	 * @param opts additional string usually used to tell the comp. chem.
	 * software how to use the given information. For example,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint). Or null, if no option has to be given.
	 * @return the constraint.
	 * @throws Exception
	 */
	public static Constraint buildConstraint(ArrayList<Integer> ids, 
			Double value, String opts) throws Exception
	{
		switch (ids.size())
		{
			case 1:
				return new Constraint(ids.get(0), -1, -1, -1,
						ConstraintType.FROZENATM, value, opts);
			case 2:
				return new Constraint(ids.get(0), ids.get(1), -1, -1,
						ConstraintType.DISTANCE, value, opts);
			case 3:
				return new Constraint(ids.get(0), ids.get(1), ids.get(2), -1,
						ConstraintType.ANGLE, value, opts);
			case 4:
				return new Constraint(ids.get(0), ids.get(1), ids.get(2),
						ids.get(3), ConstraintType.DIHEDRAL, value, opts);
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
	
	public boolean hasValue()
	{
		return hasValue;
	}
	
//------------------------------------------------------------------------------

	public double getValue()
	{
		return value;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Constraint [Type=");
		sb.append(type).append(", IDs=[");
		for (int i=0; i<atmIDs.length; i++)
		{
			sb.append(atmIDs[i]);
			if (i<(atmIDs.length-1))
				sb.append(",");
		}
		sb.append("], value=").append(value);
		sb.append("], options=").append(options);
		sb.append("] ");
		return sb.toString();
	}

//------------------------------------------------------------------------------

	public void setAtomIDs(int[] ids)
	{
		atmIDs = ids;
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
					} else if (this.getNumberOfIDs() == 4)
					{
						if (this.getAtomIDs()[0] == o.getAtomIDs()[3]
							&& this.getAtomIDs()[1] == o.getAtomIDs()[2]
							&& this.getAtomIDs()[3] == o.getAtomIDs()[1]
							&& this.getAtomIDs()[3] == o.getAtomIDs()[0])
						{
							return 0;
						} else {
							return Integer.compare(this.getAtomIDs()[2], 
									o.getAtomIDs()[2]);
						}
					} else {
						return Integer.compare(this.getAtomIDs()[2], 
								o.getAtomIDs()[2]);
					}
				} else if (this.getNumberOfIDs() == 3)
				{
					if (this.getAtomIDs()[0] == o.getAtomIDs()[2]
						&& this.getAtomIDs()[1] == o.getAtomIDs()[1]
						&& this.getAtomIDs()[2] == o.getAtomIDs()[0])
					{
						return 0;
					} else {
						return Integer.compare(this.getAtomIDs()[1], 
								o.getAtomIDs()[1]);
					}
				} else if (this.getNumberOfIDs() == 4)
				{
					if (this.getAtomIDs()[0] == o.getAtomIDs()[3]
						&& this.getAtomIDs()[1] == o.getAtomIDs()[2]
						&& this.getAtomIDs()[3] == o.getAtomIDs()[1]
						&& this.getAtomIDs()[3] == o.getAtomIDs()[0])
					{
						return 0;
					} else {
						return Integer.compare(this.getAtomIDs()[1], 
								o.getAtomIDs()[1]);
					}
				} else {
					return Integer.compare(this.getAtomIDs()[1], 
							o.getAtomIDs()[1]);
				}
			} else if (this.getNumberOfIDs() == 2)
			{
				if (this.getAtomIDs()[0] == o.getAtomIDs()[1]
					&& this.getAtomIDs()[1] == o.getAtomIDs()[0])
				{
					return 0;
				} else {
					return Integer.compare(this.getAtomIDs()[0], 
							o.getAtomIDs()[0]);
				}
			} else if (this.getNumberOfIDs() == 3)
			{
				if (this.getAtomIDs()[0] == o.getAtomIDs()[2]
					&& this.getAtomIDs()[1] == o.getAtomIDs()[1]
					&& this.getAtomIDs()[2] == o.getAtomIDs()[0])
				{
					return 0;
				} else {
					return Integer.compare(this.getAtomIDs()[0], 
							o.getAtomIDs()[0]);
				}
			} else if (this.getNumberOfIDs() == 4)
			{
				if (this.getAtomIDs()[0] == o.getAtomIDs()[3]
					&& this.getAtomIDs()[1] == o.getAtomIDs()[2]
					&& this.getAtomIDs()[3] == o.getAtomIDs()[1]
					&& this.getAtomIDs()[3] == o.getAtomIDs()[0])
				{
					return 0;
				} else {
					return Integer.compare(this.getAtomIDs()[0], 
							o.getAtomIDs()[0]);
				}
			} else {
				return Integer.compare(this.getAtomIDs()[0],o.getAtomIDs()[0]);
			}
		} else {
			return Integer.compare(this.getNumberOfIDs(), o.getNumberOfIDs());
		}
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return <code>true</code> is this constraint is associated with any 
	 * additional optional string.
	 */
	public boolean hasOpt() 
	{
		return hasOpt;
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the optional string associated with this constraint.
	 */
	public String getOpt() 
	{
		return options;
	}
	
//------------------------------------------------------------------------------

}
