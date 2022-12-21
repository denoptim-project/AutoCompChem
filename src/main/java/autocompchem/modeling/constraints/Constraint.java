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
import java.util.Arrays;
import java.util.Comparator;

import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.utils.NumberUtils;

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
	public enum ConstraintType {FROZENATM, DISTANCE, ANGLE, DIHEDRAL, 
		IMPROPERTORSION, UNDEFINED}
	
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
	 * @param isDihedral use <code>true<code> to indicate that this 4-tupla 
	 * indicates a proper dihedral where the connectivity is i-j-k-l.
	 */
	public Constraint(int i, int j, int k, int l, boolean isDihedral)
	{
		this(i, j, k, l, null, null, null);
		if (isDihedral)
			type = ConstraintType.DIHEDRAL;
		else
			type = ConstraintType.IMPROPERTORSION;
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
	 * @param areLinearlyConnected use <code>true</code> is the given IDs 
	 * represent a set of centers that are connected in the order given, e.g.
	 * 1-j-k-l.
	 * @throws Exception
	 */
	public static Constraint buildConstraint(ArrayList<Integer> ids, 
			boolean areLinearlyConnected) throws Exception
	{
		return buildConstraint(ids, null, null, areLinearlyConnected);
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
	 * @param areLinearlyConnected use <code>true</code> is the given IDs 
	 * represent a set of centers that are connected in the order given, e.g.
	 * 1-j-k-l.
	 * @return the constraint.
	 * @throws Exception
	 */
	public static Constraint buildConstraint(ArrayList<Integer> ids, 
			Double value, String opts, boolean areLinearlyConnected) 
					throws Exception
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
				if (areLinearlyConnected)
					return new Constraint(ids.get(0), ids.get(1), ids.get(2),
						ids.get(3), ConstraintType.DIHEDRAL, value, opts);
				else
					return new Constraint(ids.get(0), ids.get(1), ids.get(2),
							ids.get(3), ConstraintType.IMPROPERTORSION, value, 
							opts);
				
			default:
				throw new Exception("Unexpected number of atom IDs (" 
						+ ids.size() + "). Cannot construct a Constraint.");
		}
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * @return the type of this constraint.
	 */
	public ConstraintType getType()
	{
		return type;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * @return <code>true</code> if this constraint has an associated value.
	 */
	public boolean hasValue()
	{
		return hasValue;
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the value associated with this constraint.
	 */
	public double getValue()
	{
		return value;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Changes the value associated with this constraint.
	 * @param value the new value.
	 */
	public void setValue(double value) 
	{
		this.hasValue = true;
		this.value = value;
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

	/**
	 * Defines the atom indexes that define this constraint.
	 * @param ids the list of 0-based indexes.
	 */
	public void setAtomIDs(int[] ids)
	{
		atmIDs = ids;
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the list of atom indexes that defines this constraint (0-based).
	 */
	public int[] getAtomIDs()
	{
		return atmIDs;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Returns a copy of the list of indexes in which the indexes are sorted
	 * as follows:
	 * <ul>
	 * <li>{@link ConstraintType#DISTANCE}: smallest ID first</li>
	 * <li>{@link ConstraintType#ANGLE}: the smallest between first and last 
	 * index is placed first.</li>
	 * <li>{@link ConstraintType#DIHEDRAL}: rearranges as to get the smallest 
	 * between second and third index in the second position.</li>
	 * </ul>
	 * No change for other types.
	 * @return the sorted list of IDS.
	 */
	public int[] getSortedAtomIDs()
	{
		int[] sorted = atmIDs;
        switch (type)
        {
            case DISTANCE:
                if (atmIDs[0] > atmIDs[1])
                {
                	sorted = new int[2];
                	sorted[0] = atmIDs[1];
                	sorted[1] = atmIDs[0];
                } else {
                	sorted = new int[2];
                	sorted[0] = atmIDs[0];
                	sorted[1] = atmIDs[1];
                }
                break;

            case ANGLE:
                if (atmIDs[0] > atmIDs[2])
                {
                	sorted = new int[3];
                	sorted[0] = atmIDs[2];
                	sorted[1] = atmIDs[1];
                	sorted[2] = atmIDs[0];
                } else {
                	sorted = new int[3];
                	sorted[0] = atmIDs[0];
                	sorted[1] = atmIDs[1];
                	sorted[2] = atmIDs[2];
                }
                break;

            case DIHEDRAL:
            	if (atmIDs[1] > atmIDs[2])
                {
                	sorted = new int[4];
                	sorted[0] = atmIDs[3];
                	sorted[1] = atmIDs[2];
                	sorted[2] = atmIDs[1];
                	sorted[3] = atmIDs[0];
                } else {
                	sorted = new int[4];
                	sorted[0] = atmIDs[0];
                	sorted[1] = atmIDs[1];
                	sorted[2] = atmIDs[2];
                	sorted[3] = atmIDs[3];
                }
                break;
			default:
				//Nothing.
				break;
        }
		return sorted;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Usually, 
	 * one index is used to define a frozen atom, two for distances, 
	 * three for angles, and four for dihedral of improper torsions. Yet, upon
	 * demands from the user any number of indexes can be associated with an
	 * {@value ConstraintType#UNDEFINED} type. So the number of indexes should
	 * not be used as a replacement of the defined type. 
	 * See {@link Constraint#getType()}
	 * @return the number of indexes that define this constraint. 
	 */
	public int getNumberOfIDs()
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
		// Type takes priority
		if (this.getType()!=o.getType())
		{
			return this.getType().compareTo(o.getType());
		}
		
		// In case of same type then we look at the main indexes
		int[] ids1 = this.getSortedAtomIDs();
		int[] ids2 = o.getSortedAtomIDs();
		switch (this.getType())
		{
			case DISTANCE:
			{
				if (ids1[0]!=ids2[0])
					return Integer.compare(ids1[0], ids2[0]);
				else if (ids1[1]!=ids2[1])
					return Integer.compare(ids1[1], ids2[1]);
				break;
			}
			case ANGLE:
			{
				if (ids1[1]!=ids2[1])
					return Integer.compare(ids1[1], ids2[1]);
				else if (ids1[0]!=ids2[0])
					return Integer.compare(ids1[0], ids2[0]);
				else if (ids1[2]!=ids2[2])
					return Integer.compare(ids1[2], ids2[2]);
				break;
			}
			case DIHEDRAL:
			{
				if (ids1[1]!=ids2[1])
					return Integer.compare(ids1[1], ids2[1]);
				else
					if (ids1[2]!=ids2[2])
						return Integer.compare(ids1[2], ids2[2]);
					else if (ids1[0]!=ids2[0])
						return Integer.compare(ids1[0], ids2[0]);
					else if (ids1[3]!=ids2[3])
						return Integer.compare(ids1[3], ids2[3]);
				break;
			}
			case FROZENATM:
			{
				return Integer.compare(ids1[0], ids2[0]);
			}
			default:
			{
				if (ids1[0]!=ids2[0])
					return Integer.compare(ids1[0], ids2[0]);
				else if (ids1[1]!=ids2[1])
					return Integer.compare(ids1[1], ids2[1]);
				else if (ids1[2]!=ids2[2])
					return Integer.compare(ids1[2], ids2[2]);
				else if (ids1[3]!=ids2[3])
					return Integer.compare(ids1[3], ids2[3]);
				break;
			}
		}
		
		// Type and IDs are equivalent
		return 0;
	}

//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Constraint))
			return false;
		Constraint other = (Constraint) o;
   	 
	   	if (this.atmIDs.length != other.atmIDs.length)
	   		 return false;
	   				 
		for (int i=0; i<this.atmIDs.length; i++)
		{
			if (this.atmIDs[i] != other.atmIDs[i])
				return false;
		}
		
		if (!NumberUtils.closeEnough(this.value, other.value))
	   		 return false;
		
		if (this.options!=null && other.options!=null
				&& !this.options.equals(other.options))
			return false;
	   	 
	   	return this.type == other.type
	   			&& this.hasValue == other.hasValue 
	   			&& this.hasOpt == other.hasOpt;
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
