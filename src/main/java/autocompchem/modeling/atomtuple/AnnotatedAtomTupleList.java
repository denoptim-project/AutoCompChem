package autocompchem.modeling.atomtuple;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * An ordered set of atom tuples.
 */
public class AnnotatedAtomTupleList extends ArrayList<AnnotatedAtomTuple>
{

	/**
	 * Version identifier for serialization
	 */
	private static final long serialVersionUID = -3515665116095772408L;

//------------------------------------------------------------------------------
	
	/**
	 * Constructor for an empty list.
	 */
	public AnnotatedAtomTupleList()
	{}
		
//------------------------------------------------------------------------------

	/**
	 * Constructor from a list of tuples.
	 * @param tuples tuples to add to this collection.
	 */
	public AnnotatedAtomTupleList(List<AnnotatedAtomTuple> tuples) 
	{
		this.addAll(tuples);
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
 	    
 	    AnnotatedAtomTupleList other = (AnnotatedAtomTupleList) o;
 	   
	   	if (this.size() != other.size())
	   		 return false;
	   	
	   	Iterator<AnnotatedAtomTuple> thisIter = this.iterator();
	   	Iterator<AnnotatedAtomTuple> otherIter = other.iterator();
	   	while (thisIter.hasNext())
	   	{
	   		if (!thisIter.next().equals(otherIter.next()))
	   			return false;
	   	}
	   	
	   	return true;
	}
	
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(super.hashCode());
    }
	
//-----------------------------------------------------------------------------
	
	@Override
	public AnnotatedAtomTupleList clone()
	{
		AnnotatedAtomTupleList clone = new AnnotatedAtomTupleList();
		for(AnnotatedAtomTuple tuple : this)
	   	{
	   		clone.add(tuple.clone());
	   	}
		return clone;
	}
    
//-----------------------------------------------------------------------------
	
}
