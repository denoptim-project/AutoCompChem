package autocompchem.molecule.conformation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The conformational space as the ordered list of non-redundant conformational
 * changes (i.e., the {@link ConformationalCoordinate}). The entries are kept 
 * non-redundant
 * by the {@link Comparator} given upon construction, which also controls the
 * ordering of the coordinates returned by the iterator. 
 * The default comparator is {@link ConfCoordComparator}.
 * 
 * @author Marco Foscato 
 */

public class ConformationalSpace extends TreeSet<ConformationalCoordinate> 
	implements Cloneable
{
    /**
     * Unique counter for coordinates names
     */
    private final AtomicInteger CRDID = new AtomicInteger(0);  
    
//------------------------------------------------------------------------------

    /**
     * Constructor using default comparator
     */
    public ConformationalSpace()
    {
    	super(new ConfCoordComparator());
    }
    
//------------------------------------------------------------------------------

    /**
     * Get unique name for coord
     * @return a string unique within this ConformationalSpace
     */

    @Deprecated
    public String getUnqCoordName()
    {
        return ConformationalCoordDefinition.BASENAME + CRDID.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Get number of combinations. The number of combinations represent the
     * size of this ConformationalSpace.
     * @return the number of combinations, or the largest calculated number 
     * multiplied by -1 if the actual result is to big 
     * to be calculated.
     */

    public int getSize()
    {
        int sz = 1;
        for (ConformationalCoordinate cc : this)
        {
            sz = sz * cc.getFold();
            int diff = Integer.MAX_VALUE - sz;
            if (diff < 10000)
            {
                sz = -sz;
                break;
            }
        }
        return sz;
    }
	
//------------------------------------------------------------------------------

  	@Override
  	public boolean equals(Object o)
  	{
  		if (!(o instanceof ConformationalSpace))
  			return false;
  		
  		ConformationalSpace other = (ConformationalSpace) o;
     	 
  	   	if (this.size() != other.size())
  	   		 return false;
  	   	
  	   	Iterator<ConformationalCoordinate> thisIter = this.iterator();
  	   	Iterator<ConformationalCoordinate> otherIter = other.iterator();
  	   	while (thisIter.hasNext())
  	   	{
   	        if (!thisIter.next().equals(otherIter.next()))
   	            return false;
  	   	}
  	   	return true;
  	}

//-----------------------------------------------------------------------------

  	/**
  	 * Prints all the conformational coordinates into stdout.
  	 */
  	
  	public void printAll() 
  	{
  		System.out.println("Conformational space is defined by: ");
  		for (ConformationalCoordinate c : this)
  		{
  			System.out.println(" -> "+c);
  		}
  	}

//------------------------------------------------------------------------------
    
}
