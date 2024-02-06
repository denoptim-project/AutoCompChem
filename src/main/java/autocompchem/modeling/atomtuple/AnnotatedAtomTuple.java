package autocompchem.modeling.atomtuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.molecule.connectivity.NearestNeighborMap;
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
     * reflect any changes occurring afterwards. It does not consider the kind
     * of connection between the atoms.
     */
	private NearestNeighborMap connectionTable;

	/**
	 * Total number of atoms in the system. This is NOT the number of items in
	 * the tuple, but the number of items in the container from which the tuple
	 * has been taken.
	 */
	private int numAtoms = 0;
	
//------------------------------------------------------------------------------

  	/**
  	 * Constructs a tuple of atoms with no decorating attributes.
  	 * @param ids 0-based indexes defining the tuple of atoms.
  	 */
  	public AnnotatedAtomTuple(int[] ids)
  	{
  		this(Arrays.stream(ids).boxed().collect(Collectors.toList()), 
  				new HashSet<String>(), new HashMap<String, String>(), 
  				null, 0);
  	}
  	
//------------------------------------------------------------------------------

  	/**
  	 * Constructs a tuple of atoms with decorating attributes.
  	 * @param ids 0-based indexes defining the tuple of atoms.
  	 * @param valuelessAttributes value-less attributes.
  	 * @param valuedAttributes map of attributes with their (String) value.
  	 * @param ct defines the neighboring relation between atoms in the tuple.
  	 * @param numAtoms the number of atoms in the container from which the tuple
  	 * is extracted.
  	 */
  	public AnnotatedAtomTuple(int[] ids, 
  			Set<String> valuelessAttributes, 
  			Map<String, String> valuedAttributes,
  			NearestNeighborMap ct, int numAtoms)
  	{
  		this(Arrays.stream(ids).boxed().collect(Collectors.toList()), 
  				valuelessAttributes, valuedAttributes, ct, numAtoms);
  	}
  	
//------------------------------------------------------------------------------

	/**
	 * Constructs a tuple of atoms with decorating attributes.
	 * @param ids 0-based indexes defining the tuple of atoms.
	 * @param valuelessAttributes value-less attributes.
	 * @param valuedAttributes map of attributes with their (String) value.
  	 * @param ct defines the neighboring relation between atoms in the tuple.
  	 * @param numAtoms the number of atoms in the container from which the tuple
  	 * is extracted.
	 */
	public AnnotatedAtomTuple(List<Integer> ids, 
			Set<String> valuelessAttributes, 
			Map<String, String> valuedAttributes,
			NearestNeighborMap ct, int numAtoms)
	{
		this.atmIDs = ids;
		this.valuelessAttributes = valuelessAttributes;
		this.valuedAttributes = valuedAttributes;
		this.connectionTable = ct;
		this.numAtoms = numAtoms;
	}

//------------------------------------------------------------------------------

	/**
	 * Constructs a tuple of atoms without decorating attributes, but does
	 * infer the {@link NearestNeighborMap} information.
	 * @param atoms ordered list of atoms from which to build the tuple.
	 * @param mol the container collecting the atoms.
	 */
	public AnnotatedAtomTuple(List<IAtom> atoms, IAtomContainer mol)
	{
  		this(atoms.stream()
  					.map(a -> mol.indexOf(a))
  					.collect(Collectors.toList()), 
  				new HashSet<String>(), new HashMap<String, String>(),
  				new NearestNeighborMap(atoms, mol),
  				mol.getAtomCount());
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a tuple of atoms with decorating attributes.
	 * @param atoms ordered list of atoms from which to build the tuple.
	 * @param mol the container collecting the atoms.
	 * @param valuelessAttributes value-less attributes.
	 * @param valuedAttributes map of attributes with their (String) value.
  	 * @param ct defines the neighboring relation between atoms in the tuple.
	 */
	public AnnotatedAtomTuple(List<IAtom> atoms, IAtomContainer mol, 
			Set<String> valuelessAttributes, 
			Map<String, String> valuedAttributes)
	{
  		this(atoms.stream()
  					.map(a -> mol.indexOf(a))
  					.collect(Collectors.toList()), 
  				valuelessAttributes, valuedAttributes,
  				new NearestNeighborMap(atoms, mol),
  				mol.getAtomCount());
	}

//------------------------------------------------------------------------------

	/**
	 * Sets the value of the index at the given position. 
	 * <br>
	 * <b>WARNING!</b> does not update the neighboring relations.
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
	
	
//-----------------------------------------------------------------------------
	
	/**
	 * @return the number of atoms in the system from which these tuples
	 * are generated.
	 */
	public int getNumAtoms() 
	{
		return numAtoms;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Sets the number of atoms in the system from which these tuples
	 * are generated.
	 * @param numAtoms the number of atoms.
	 */
	public void setNumAtoms(int numAtoms) 
	{
		this.numAtoms = numAtoms;
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
     *@return all the keys of attribute that are associated with a value in this 
     * tuple.
     */

    public Set<String> getValuedAttributeKeys()
    {
    	return valuedAttributes.keySet();
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the attribute that are associated with a value in this tuple.
     */

    public Map<String,String> getValuedAttributes()
    {
    	return valuedAttributes;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting prefix or an empty string is no prefix
     * is available.
     */

    public String getPrefix()
    {
    	String value = getValueOfAttribute(AtomTupleConstants.KEYPREFIX);
    	if (value != null)
    		return value;
    	else
    		return "";
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting suffix or an empty string is no suffix
     * is available.
     */

    public String getSuffix()
    {
    	String value = getValueOfAttribute(AtomTupleConstants.KEYSUFFIX);
    	if (value != null)
    		return value;
    	else
    		return "";
    }
    	
//------------------------------------------------------------------------------
    
  	/**
  	 * Set any optional string marked to be a prefix.
  	 * @param prefix the string to use as prefix
  	 */
  	public void setPrefix(String prefix) 
  	{
  		setValueOfAttribute(AtomTupleConstants.KEYPREFIX, prefix);
  	}
  	
//------------------------------------------------------------------------------

  	/**
  	 * Set any optional string marked to be a suffix.
  	 * @param suffix the string to use as suffix
  	 */
  	public void setSuffix(String suffix) 
  	{
  		setValueOfAttribute(AtomTupleConstants.KEYSUFFIX, suffix);
  	}
    
//------------------------------------------------------------------------------

    /**
     * @return the table defining the which atom is connected to which other one
     * in terms of their atom indexes.
     */

    public NearestNeighborMap getNeighboringRelations()
    {
    	return connectionTable;
    }
    
//------------------------------------------------------------------------------
	
    /**
     * Checks if the atoms represented by the index in the given indexes are
     * neighbors, i.e., were connected to each other when this tuple was 
     * generated.
     * @param idxA index of one of the atom indexes. NB: this is not 
     * the atom index itself, but is its index in the list of indexes.
     * @param idxB index of one of the atom indexes. NB: this is not 
     * the atom index itself, but is its index in the list of indexes.
     * @return <code>true</code> if the to atoms were connected to each other
     * when this tuple was generated.
     */
    public boolean areNeighbors(int idxA, int idxB)
    {
    	return connectionTable.areNeighbors(getAtomIDs().get(idxA), 
    			getAtomIDs().get(idxB));
    }
    
//------------------------------------------------------------------------------
	
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
  		
  		NearestNeighborMap ct = null;
  		if (connectionTable!=null)
  			ct = connectionTable.clone();
  		
  		return new AnnotatedAtomTuple(ids, 
  				clonedValuelessAtts, clonedValuedAtts, ct, numAtoms);
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

 	    if (this.numAtoms != other.numAtoms)
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
		
		if (connectionTable!=null)
			if (!this.connectionTable.equals(other.connectionTable))
				return false;
		
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
		String otherFields = generateStringForSubClassFields();
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
	protected String generateStringForSubClassFields() {
		return "";
	}
	
//------------------------------------------------------------------------------

}
