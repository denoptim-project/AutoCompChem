package autocompchem.molecule.geometry;

import java.util.List;
import java.util.Objects;

import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.utils.NumberUtils;

/**
 * This class represents the concept of a geometric descriptor that is applied on a list
 * of atoms.
 * 
 * @author Marco Foscato
 */

public class GeomDescriptor extends AnnotatedAtomTuple 
	implements Comparable<GeomDescriptor>
{
	/**
	 * Name of this geometric descriptor
	 */
	private String name;
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a geometric descriptor of any kind offering the possibility to define
	 * a specific value and options to assign to the geometric descriptor.
	 * @param ids indexes (0-based) of centers (i.e., atoms) defining this 
	 * geometric descriptor
	 */
	public GeomDescriptor(int[] ids, String name)
	{
		super(ids);
		this.name = name;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a geometric descriptor from an annotated atom tuple.
	 * @param tuple the annotated atom tuple to parse.
	 */
	
	public GeomDescriptor(AnnotatedAtomTuple tuple)
	{
		super(tuple.getAtomIDs(), tuple.getAtmLabels(), 
				tuple.getValuelessAttribute(), 
				tuple.getValuedAttributes(), tuple.getNeighboringRelations(),
				tuple.getNumAtoms());
		this.name = tuple.getValueOfAttribute(GeomDescriptorDefinition.KEYNAME);
	}

//------------------------------------------------------------------------------

	/**
	 * Returns the name of this geometric descriptor
	 * @return the name of this geometric descriptor
	 */
	public String getName()
	{
		return name;
	}

//-----------------------------------------------------------------------------

	/**
	 * Returns the value of this geometric descriptor
	 * @return the value of this geometric descriptor
	 */
    public double getValue() 
    {
        return Double.parseDouble(getValueOfAttribute(
			AtomTupleConstants.KEYCURRENTVALUE));
    }

//------------------------------------------------------------------------------

	/**
	 * Returns the flag defining if this geometric descriptor is applied only 
	 * to intermolecular atoms.
	 * @return <code>true</code> if this geometric descriptor is applied only 
	 * to intermolecular atoms.
	 */
    public boolean limitToIntermolecular()
    {
        return hasValuelessAttribute(AtomTupleConstants.KEYONLYINTERMOLECULAR);
    }

//-----------------------------------------------------------------------------

	/**
	 * Returns the flag defining if this geometric descriptor is applied only to
	 * bonded atoms.
	 * @return <code>true</code> if this geometric descriptor is applied only to 
	 * bonded atoms.
	 */
    public boolean limitToBonded()
    {
        return hasValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    }
	
//-----------------------------------------------------------------------------

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("GeomDescriptor [Name: "+name+", IDs=[");
		List<Integer> ids = getAtomIDs();
		for (int i=0; i<ids.size(); i++)
		{
			sb.append(ids.get(i));
			if (i<(ids.size()-1))
				sb.append(",");
		}
		sb.append("]");
		if (getValueOfAttribute(AtomTupleConstants.KEYCURRENTVALUE)!=null)
		{
			sb.append(", Value: "+getValue());
		}
		if (limitToIntermolecular())
		{
			sb.append(", onlyIntermolecular");
		}
		if (limitToBonded())
		{
			sb.append(", onlyBonded");
		}
		sb.append("]");
		return sb.toString();
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public int compareTo(GeomDescriptor o) 
	{
		if (!this.name.equals(o.name))
			return this.name.compareTo(o.name);
		
		if (this.getNumberOfIDs()!=o.getNumberOfIDs())
			return Integer.compare(this.getNumberOfIDs(), o.getNumberOfIDs());
		
		return 0;
	}

//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
    	if (o== null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	    
		GeomDescriptor other = (GeomDescriptor) o;

		if (!this.name.equals(other.name))
			return false;
		
		if (!NumberUtils.closeEnough(this.getValue(), other.getValue()))
			return false;
	   	
	   	return super.equals(o);
	}
	
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(name, getAtomIDs(), getAtmLabels(), getValuelessAttribute(), 
		getValuedAttributes(), getNeighboringRelations(), getNumAtoms());
    }
	
//-----------------------------------------------------------------------------

	@Override
	public GeomDescriptor clone()
	{
		GeomDescriptor clone = new GeomDescriptor(super.clone());
		clone.name = this.name;
		return clone;
	}
	
//------------------------------------------------------------------------------

}
