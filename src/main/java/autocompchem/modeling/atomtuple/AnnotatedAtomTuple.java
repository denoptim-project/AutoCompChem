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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import autocompchem.utils.StringUtils;

/**
 * This class represents the concept of a tuple of atom indexes decorated by 
 * attributes that can have either a boolean or String value.
 * 
 * @author Marco Foscato
 */

public class AnnotatedAtomTuple implements Cloneable
{	
	/**
	 * The 0-based atom indexes
	 */
	private List<Integer> atmIDs;
	
    /**
     * Map of keywords that take no value and are thus mapped 
     * to the flag reporting if they are found or not.
     */
    private Set<String> valuelessAttributes;
    
    /**
     * Map of keywords that take values and are thus mapped to their
     * value.
     */
    private Map<String,String> valuedAttributes;
    
    /**
     * Connectivity map for the given indexes. This is a snapshot of the
     * connectivity as it is the moment this tuple is created. It does not 
     * reflect any changes occurring afterwards. It does not report 
     */
	private Map<Integer,Integer> connections;
	
//------------------------------------------------------------------------------

  	/**
  	 * Constructs a tuple of atoms with no decorating attributes.
  	 * @param ids 0-based indexes defining the tuple of atoms.
  	 */
  	public AnnotatedAtomTuple(int[] ids)
  	{
  		this.atmIDs = new ArrayList<Integer>();
  		for (int i=0; i<ids.length; i++)
  		{
  			this.atmIDs.add(ids[i]);
  		}
  		this.valuelessAttributes = new HashSet<String>();
  		this.valuedAttributes = new HashMap<String, String>();
  	}
  	
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
  		this(Arrays.stream(ids).boxed().collect(Collectors.toList()), 
  				valuelessAttributes, valuedAttributes);
  	}
  	
//------------------------------------------------------------------------------

	/**
	 * Constructs a tuple of atoms with decorating attributes.
	 * @param ids 0-based indexes defining the tuple of atoms.
	 * @param valuelessAttributes value-less attributes.
	 * @param valuedAttributes map of attributes with their (String) value.
	 */
	public AnnotatedAtomTuple(List<Integer> ids, 
			Set<String> valuelessAttributes, 
			Map<String, String> valuedAttributes)
	{
		this.atmIDs = ids;
		this.valuelessAttributes = valuelessAttributes;
		this.valuedAttributes = valuedAttributes;
	}

//------------------------------------------------------------------------------

	/**
	 * Sets the value of the index at the given positiion.
	 */
	public void setIndexAt(int position, int value)
	{
		atmIDs.set(position, value);
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the list of atom indexes that defines this tuple (0-based).
	 */
	public List<Integer> getAtomIDs()
	{
		return atmIDs;
	}
	
//------------------------------------------------------------------------------

	/**
	 * @return the number of indexes that define this tuple. 
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
    	valuelessAttributes.add(key.toUpperCase());
    }
    
//------------------------------------------------------------------------------

    /**
     * Remove a value-less attribute in this tuple. Case insensitive.
     * @param key the name of the attribute to remove
     */

    public void removeValuelessAttribute(String key)
    {
    	valuelessAttributes.remove(key.toUpperCase());
    }
    
//------------------------------------------------------------------------------

    /**
     * @param key the name of the attribute to get. Case insensitive.
     * @return <code>true</code> if the keyword was found, <code>false</code> if
     * it was not found.
     */

    public boolean hasValuelessAttribute(String key)
    {
        return valuelessAttributes.contains(key.toUpperCase());
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
     * @param key the name of the attribute to get. Case insensitive.
     * @return <code>true</code> if the keyword was found, <code>false</code> if
     * it was not found.
     */

    public boolean hasValueledAttribute(String key)
    {
        return valuedAttributes.containsKey(key.toUpperCase());
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the value associated to an attribute of this tuple.
     * @param key the name of the attribute to set. Case insensitive.
     * @param value the value to set.
     */

    public void setValueOfAttribute(String key, String value)
    {
        valuedAttributes.put(key.toUpperCase(), value);
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
        return valuedAttributes.get(key.toUpperCase());
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets all the keys of attribute that are associated with a value in this 
     * tuple.
     */

    public Set<String> getValuedAttributeKeys()
    {
    	return valuedAttributes.keySet();
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets all the attribute that are associated with a value in this tuple.
     */

    public Map<String,String> getValuedAttributes()
    {
    	return valuedAttributes;
    }
	
//-----------------------------------------------------------------------------
	
  	@Override
  	public AnnotatedAtomTuple clone()
  	{
  		int[] ids = new int[this.atmIDs.size()];
  		for (int i=0; i<atmIDs.size(); i++)
  		{
  			ids[i] = atmIDs.get(i).intValue();
  		}
  		
  		Set<String> clonedValuelessAtts = new HashSet<String>();
  		for (String key : valuelessAttributes)
  			clonedValuelessAtts.add(key);
  		
  		Map<String,String> clonedValuedAtts = new HashMap<String, String>();
  		for (String key : valuedAttributes.keySet())
  			clonedValuedAtts.put(key, valuedAttributes.get(key));
  		
  		return new AnnotatedAtomTuple(ids, 
  				clonedValuelessAtts, clonedValuedAtts);
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
		sb.append(StringUtils.mergeListToString(atmIDs, ",", true));
		sb.append("], ");
		String otherFields = gerToStringOfFields();
		if (!otherFields.isBlank())
			sb.append(otherFields);
		sb.append("valuelessAttributes:[").append(
				valuelessAttributes.toString());
		sb.append("], valuedAttributes:[").append(valuedAttributes.toString());
		sb.append("]]");
		return sb.toString();
	}

//------------------------------------------------------------------------------
	
	/**
	 * Overwrite this method to include fields of subclasses in the string
	 * representation of a subclass without having to overwrite the toString() 
	 * method. The returned string must end with ", ".
	 */
	protected String gerToStringOfFields() {
		return "";
	}
	
//------------------------------------------------------------------------------

}
