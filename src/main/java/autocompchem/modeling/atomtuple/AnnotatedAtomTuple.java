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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.modeling.basisset.Primitive;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;

/**
 * This class represents the concept of a tuple of atom indexes decorated by 
 * attributes that can have either a boolean or String value.
 * <br>
 * Note: this class has a natural ordering that is inconsistent with equals.
 * This because attributes are not considered by natural ordering.
 * 
 * @author Marco Foscato
 */

public class AnnotatedAtomTuple implements Comparable<AnnotatedAtomTuple>
{	
	/**
	 * The 0-based atom indexes
	 */
	private List<Integer> atmIDs = new ArrayList<Integer>();
	
    /**
     * Map of keywords that take no value and are thus mapped 
     * to the flag reporting if they are found or not.
     */
    private Set<String> valuelessAttributes = new HashSet<String>();
    
    /**
     * Map of keywords that take values and are thus mapped to their
     * value.
     */
    private Map<String,String> valuedAttributes = 
    		new HashMap<String, String>();
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a tuple of atoms with decorating attributes.
	 * @param ids 0-based indexes defining the tuple of atoms. 
	 * @param valuelessAttributes value-less attributes.
	 * @param valuedAttributes map of attributes with their (String) value.
	 */
	public AnnotatedAtomTuple(int[] ids, 
			Set<String> valuelessAttributes, 
			Map<String, String> valuedAttributes)
	{
		for (int i=0; i<ids.length; i++)
		{
			this.atmIDs.add(ids[i]);
		}
		this.valuelessAttributes = valuelessAttributes;
		this.valuedAttributes = valuedAttributes;
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the list of atom indexes that defines this constraint (0-based).
	 */
	public List<Integer> getAtomIDs()
	{
		return atmIDs;
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the number of indexes that define this constraint. 
	 */
	public int getNumberOfIDs()
	{
		return atmIDs.size();
	}
    
//------------------------------------------------------------------------------

    /**
     * Sets a value-less attribute in this tuple.
     * @param key the name of the attribute to set.
     */

    public void setValuelessAttribute(String key)
    {
    	valuelessAttributes.add(key);
    }
    
//------------------------------------------------------------------------------

    /**
     * Remove a value-less attribute in this tuple.
     * @param key the name of the attribute to remove
     */

    public void removeValuelessAttribute(String key)
    {
    	valuelessAttributes.remove(key);
    }
    
//------------------------------------------------------------------------------

    /**
     * @param key the name of the attribute to get.
     * @return <code>true</code> if the keyword was found, <code>false</code> if
     * it was not found.
     */

    public boolean hasValuelessAttribute(String key)
    {
        return valuelessAttributes.contains(key);
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets all the value-less attribute in this tuple.
     */

    public Set<String> getValuelessAttribute()
    {
    	return valuelessAttributes;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the value associated to an attribute of this tuple.
     * @param key the name of the attribute to set.
     * @param value the value to set.
     */

    public void setValueOfAttribute(String key, String value)
    {
        valuedAttributes.put(key, value);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the value associated to an attribute of this tuple.
     * @param key the name of the attribute to get.
     * @return the value or <code>null</code> if the attribute was not found or 
     * it is not associated with any value (i.e., if it is boolean attribute,
     * then you can use {@link #isBooleanAttributeFound(String)}).
     */

    public String getValueOfAttribute(String key)
    {
        return valuedAttributes.get(key);
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets all the attribute that are associated with a value in this tuple.
     */

    public Set<String> getValuedAttribute()
    {
    	return valuedAttributes.keySet();
    }
	
//------------------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 * 
	 * <br><br>
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 * This because attributes are not considered by natural ordering.
	 */
	@Override
	public int compareTo(AnnotatedAtomTuple other) 
	{
		if (this.atmIDs.size() != other.atmIDs.size())
			return Integer.compare(this.atmIDs.size(), other.atmIDs.size());
		
		List<Integer> thisSorted = new ArrayList<Integer>();
		thisSorted.addAll(atmIDs);
		Collections.sort(thisSorted);
		
		List<Integer> otherSorted = new ArrayList<Integer>();
		otherSorted.addAll(other.atmIDs);
		Collections.sort(otherSorted);
		
		for (int i=0; i<atmIDs.size(); i++)
		{
			if (thisSorted.get(i)!=otherSorted.get(i))
				return Integer.compare(thisSorted.get(i), otherSorted.get(i));
		}
		
		//NB: attributes are not considered in natural ordering.
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
 	    
		AnnotatedAtomTuple other = (AnnotatedAtomTuple) o;
   	 
	   	if (this.atmIDs.size() != other.atmIDs.size())
	   		 return false;
	   				 
		for (int i=0; i<this.atmIDs.size(); i++)
		{
			if (this.atmIDs.get(i) != other.atmIDs.get(i))
				return false;
		}
		
		if (this.valuelessAttributes.size() 
				!= other.valuelessAttributes.size())
	   		 return false;
		
		for (String key : this.valuelessAttributes)
		{
			if (!other.valuelessAttributes.contains(key))
				return false;
		}
		
		if (this.valuedAttributes.keySet().size() 
				!= other.valuedAttributes.keySet().size())
	   		 return false;
		
		for (String key : this.valuedAttributes.keySet())
		{
			if (!other.valuedAttributes.containsKey(key))
				return false;
			if (!this.valuedAttributes.get(key).equals(
					other.valuedAttributes.get(key)))
				return false;
		}
		return true;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(" [atmIDs:[");
		sb.append(StringUtils.mergeListToString(atmIDs,","));
		sb.append("], valuelessAttributes:[").append(
				valuelessAttributes.toString());
		sb.append("], valuedAttributes:[").append(valuedAttributes.toString());
		sb.append("]]");
		return sb.toString();
	}
	
//------------------------------------------------------------------------------

}
