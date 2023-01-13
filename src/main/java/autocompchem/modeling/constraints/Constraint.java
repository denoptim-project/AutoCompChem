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
import java.util.List;

import javax.print.attribute.SetOfIntegerSyntax;

import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
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

public class Constraint extends AnnotatedAtomTuple implements Comparable<Constraint>
{
	/**
	 * Classes of the constraints according to the corresponding internal 
	 * coordinate possibly represented.
	 */
	public enum ConstraintType {FROZENATM, DISTANCE, ANGLE, DIHEDRAL, 
		IMPROPERTORSION, UNDEFINED}
	
	/**
	 * The type of this constraint
	 */
	private ConstraintType type = ConstraintType.UNDEFINED;
	
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for a frozen atom.
	 * @param i the index of the atom to freeze.
	 */
	public Constraint(int i)
	{
		this(new int[] {i}, ConstraintType.FROZENATM, null, null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint for a distance
	 * @param i first index (0-based)
	 * @param j second index (0-based)
	 */
	public Constraint(int i, int j)
	{
		this(new int[] {i, j}, ConstraintType.DISTANCE, null, null);
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
		this(new int[] {i, j, k}, ConstraintType.ANGLE, null, null);
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
		this(new int[] {i, j, k, l}, null, null, null);
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
		this(new int[] {i, j}, ConstraintType.DISTANCE, Double.valueOf(value),
				null);
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
		this(new int[] {i, j, k}, ConstraintType.ANGLE, Double.valueOf(value), 
				null);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint of any kind offering the possibility to define
	 * a specific value and options to assign to the constraint.
	 * @param ids indexes (0-based) of centers (i.e., atoms) defining this 
	 * constraint
	 * @param value a numerical value to be assigned to the constraint. Or null
	 * if no value is to be set.
	 * @param type the type of constraint. This defines how many of the indexes
	 * are actually used.
	 * @param options additional string usually used to tell the comp. chem.
	 * software how to use the given information. For example,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint). Or null, if no option has to be given.
	 */
	public Constraint(int[] ids,
			ConstraintType type, Double value, String options)
	{
		super(ids);
		this.type = type;
		if (value != null)
		{
			setValue(value);
		}
		if (options != null)
		{
			setOpts(options);
		}
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a constraint of any kind offering the possibility to define
	 * a specific value and options to assign to the constraint.
	 * @param ids indexes (0-based) of centers (i.e., atoms) defining this 
	 * constraint
	 * @param value a numerical value to be assigned to the constraint. Or null
	 * if no value is to be set.
	 * @param type the type of constraint. This defines how many of the indexes
	 * are actually used.
	 * @param options additional string usually used to tell the comp. chem.
	 * software how to use the given information. For example,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint). Or null, if no option has to be given.
	 */
	
	public Constraint(AnnotatedAtomTuple tuple)
	{
		super(tuple.getAtomIDs(), tuple.getValuelessAttribute(), 
				tuple.getValuedAttributes());
		if (!hasValuelessAttribute(ConstrainDefinition.KEYNOINTCOORD))
		{
			switch (getNumberOfIDs())
			{
				case 1:
					type = ConstraintType.FROZENATM;
					break;
					
				case 2:
					type = ConstraintType.DISTANCE;
					break;
					
				case 3:
					type = ConstraintType.ANGLE;
					break;
					
				case 4:
					//TODO-gg test me!
					if (hasValuelessAttribute(AtomTupleConstants.KEYONLYBONDED))
					{
						type = ConstraintType.DIHEDRAL;
					} else {
						//TODO-gg use connectivity matrix instead of 
						// areLinearlyConnected so that we can distinguish the
						// improper torsion from undefined 4-tuples.
						// Also, add option to ignore the typing of the constraint.
						type = ConstraintType.IMPROPERTORSION;
					}
					break;
					
				default:
					throw new IllegalArgumentException("Unexpected number of "
							+ "atom IDs (" + getNumberOfIDs() + "). "
							+ "Cannot construct a Constraint.");
			}
		}
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
		return buildConstraint(ids, null, null, areLinearlyConnected, null, null);
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
	 * @param prefix a string associated with the constraint and flagged as 
	 * prefix.
	 * @param suffix a string associated with the constraint and flagged as 
	 * suffix.
	 * @return the constraint.
	 * @throws Exception
	 */
	public static Constraint buildConstraint(ArrayList<Integer> ids, 
			Double value, String opts, boolean areLinearlyConnected,
			String prefix, String suffix) throws Exception
	{
		return buildConstraint(ids, value, opts, areLinearlyConnected,
				 prefix, suffix, true);
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
	 * @param prefix a string associated with the constraint and flagged as 
	 * prefix.
	 * @param suffix a string associated with the constraint and flagged as 
	 * suffix.
	 * @param notAnIC use <code>true</code> to prevent processing this 
	 * constraint as an internal coordinate.
	 * @return the constraint.
	 * @throws Exception
	 */
	public static Constraint buildConstraint(ArrayList<Integer> ids, 
			Double value, String opts, boolean areLinearlyConnected,
			String prefix, String suffix, boolean notAnIC) throws Exception
	{
		ConstraintType type = ConstraintType.UNDEFINED;
		if (!notAnIC)
		{
			switch (ids.size())
			{
				case 1:
					type = ConstraintType.FROZENATM;
					break;
					
				case 2:
					type = ConstraintType.DISTANCE;
					break;
					
				case 3:
					type = ConstraintType.ANGLE;
					break;
					
				case 4:
					if (areLinearlyConnected)
					{
						type = ConstraintType.DIHEDRAL;
					} else {
						//TODO-gg use connectivity matrix instead of 
						// areLinearlyConnected so that we can distinguish the
						// improper torsion from undefined 4-tuples.
						// Also, add option to ignore the typing of the constraint.
						type = ConstraintType.IMPROPERTORSION;
					}
					break;
					
				default:
					throw new Exception("Unexpected number of atom IDs (" 
							+ ids.size() + "). Cannot construct a Constraint.");
			}
		} 

		int[] idsArr = new int[ids.size()];
		for (int i=0; i<ids.size(); i++)
			idsArr[i] = ids.get(i);
	
		Constraint cstr = new Constraint(idsArr, type, value, opts);
		cstr.setPrefix(prefix);
		cstr.setSuffix(suffix);
		return cstr;
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
     * Returns the value associated to constraints defined by this rule
     * @return the value or null.
     */

    public double getValue()
    {
    	return hasValue() ? Double.parseDouble(getValueOfAttribute(
    			ConstrainDefinition.KEYVALUES)) : null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the value associated to constraints defined by this rule and
     * corresponding to the current value in the geometry used to define the 
     * constraint.
     * @return the value or null.
     */

    public double getCurrentValue()
    {
    	return hasCurrentValue() ? Double.parseDouble(getValueOfAttribute(
    		    			AtomTupleConstants.KEYCURRENTVALUE)) : null;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting options or null.
     */

    public String getOpts()
    {
        return getValueOfAttribute(ConstrainDefinition.KEYOPTIONS);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting prefix or null.
     */

    public String getPrefix()
    {
        return getValueOfAttribute(ConstrainDefinition.KEYPREFIX);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting suffix or null.
     */

    public String getSuffix()
    {
        return getValueOfAttribute(ConstrainDefinition.KEYSUFFIX);
    }
    
//------------------------------------------------------------------------------

  	/**
  	 * Changes the value associated with this constraint.
  	 * @param value the new value.
  	 */
  	public void setOpts(String opts) 
  	{
  		setValueOfAttribute(ConstrainDefinition.KEYOPTIONS, opts);
  	}
    	
//------------------------------------------------------------------------------
    
  	/**
  	 * Set any optional string marked to be a prefix.
  	 * @param prefix the string to use as prefix
  	 */
  	public void setPrefix(String prefix) 
  	{
  		setValueOfAttribute(ConstrainDefinition.KEYPREFIX, prefix);
  	}
  	
//------------------------------------------------------------------------------

  	/**
  	 * Set any optional string marked to be a suffix.
  	 * @param suffix the string to use as suffix
  	 */
  	public void setSuffix(String suffix) 
  	{
  		setValueOfAttribute(ConstrainDefinition.KEYSUFFIX, suffix);
  	}
	
//------------------------------------------------------------------------------

	/**
	 * Changes the value associated with this constraint.
	 * @param value the new value.
	 */
	public void setValue(double value) 
	{
		setValueOfAttribute(ConstrainDefinition.KEYVALUES, value+"");
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return <code>true</code> is this constraint is associated with any 
	 * additional optional string.
	 */
	public boolean hasOpt() 
	{
		return getValueOfAttribute(ConstrainDefinition.KEYOPTIONS)!=null;
	}
		
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule makes use of the a value that may
     * not be the current value found in the geometry.
     * @return <code>true</code> if this constraints defined by this rule use
     * a value.
     */
  	public boolean hasValue()
  	{
  		return getValueOfAttribute(ConstrainDefinition.KEYVALUES)!=null;
  	}
  	
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule makes use of the the current value
     * found in the geometry.
     * @return <code>true</code> if this constraints defined by this rule use
     * the current value found in the geometry.
     */
  	public boolean hasCurrentValue()
  	{
  		return getValueOfAttribute(AtomTupleConstants.KEYCURRENTVALUE)!=null;
  	}
	
//------------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Constraint [Type=");
		sb.append(type).append(", IDs=[");
		List<Integer> ids = getAtomIDs();
		for (int i=0; i<ids.size(); i++)
		{
			sb.append(ids.get(i));
			if (i<(ids.size()-1))
				sb.append(",");
		}
		sb.append("], value=").append(hasValue() ? getValue() : "null");
		sb.append("], options=").append(getOpts());
		sb.append("], prefix=").append(getPrefix());
		sb.append("], suffix=").append(getSuffix());
		sb.append("] ");
		return sb.toString();
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
		List<Integer> ids = getAtomIDs();
		int[] sorted = new int[ids.size()];
        switch (type)
        {
            case DISTANCE:
                if (ids.get(0) > ids.get(1))
                {
                	sorted = new int[2];
                	sorted[0] = ids.get(1);
                	sorted[1] = ids.get(0);
                } else {
                	sorted = new int[2];
                	sorted[0] = ids.get(0);
                	sorted[1] = ids.get(1);
                }
                break;

            case ANGLE:
                if (ids.get(0) > ids.get(2))
                {
                	sorted = new int[3];
                	sorted[0] = ids.get(2);
                	sorted[1] = ids.get(1);
                	sorted[2] = ids.get(0);
                } else {
                	sorted = new int[3];
                	sorted[0] = ids.get(0);
                	sorted[1] = ids.get(1);
                	sorted[2] = ids.get(2);
                }
                break;

            case DIHEDRAL:
            	if (ids.get(1) > ids.get(2))
                {
                	sorted = new int[4];
                	sorted[0] = ids.get(3);
                	sorted[1] = ids.get(2);
                	sorted[2] = ids.get(1);
                	sorted[3] = ids.get(0);
                } else {
                	sorted = new int[4];
                	sorted[0] = ids.get(0);
                	sorted[1] = ids.get(1);
                	sorted[2] = ids.get(2);
                	sorted[3] = ids.get(3);
                }
                break;
			default:
				for (int i=0; i<ids.size(); i++)
					sorted[i] = ids.get(i);
				break;
        }
		return sorted;
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
		
		if (this.getNumberOfIDs()!=o.getNumberOfIDs())
			return Integer.compare(this.getNumberOfIDs(), o.getNumberOfIDs());
		
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
				// At this stage we know this and o have the same number of ids
				for (int i=0; i<this.getNumberOfIDs(); i++)
				{
					if (ids1[i]!=ids2[i])
						return Integer.compare(ids1[i], ids2[i]);
				}
			}
		}
		
		// Type and IDs are equivalent
		return 0;
	}

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
 	    
		Constraint other = (Constraint) o;
		
		if ((this.hasValue() && !other.hasValue())
				|| (!this.hasValue() && other.hasValue()))
			return false;
		
		if (this.hasValue() && other.hasValue() 
				&& !NumberUtils.closeEnough(this.getValue(), other.getValue()))
	   		 return false;
		
		if ((this.hasOpt() && !other.hasOpt())
				|| (!this.hasOpt() && other.hasOpt()))
			return false;
		
		if (this.getOpts()!=null && other.getOpts()!=null
				&& !this.getOpts().equals(other.getOpts()))
			return false;
		
		if ((this.getPrefix()!=null && other.getPrefix()==null)
				|| (this.getPrefix()==null && other.getPrefix()!=null))
			return false;
		
		if (this.getPrefix()!=null && other.getPrefix()!=null
				&& !this.getPrefix().equals(other.getPrefix()))
			return false;
	
		if ((this.getSuffix()!=null && other.getSuffix()==null)
				|| (this.getSuffix()==null && other.getSuffix()!=null))
			return false;
		
		if (this.getSuffix()!=null && other.getSuffix()!=null
				&& !this.getSuffix().equals(other.getSuffix()))
			return false;
	   	 
	   	if (this.type!=other.type)
	   		return false;
	   	
	   	return super.equals(o);
	}
	
//------------------------------------------------------------------------------

}
