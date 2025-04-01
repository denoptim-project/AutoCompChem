package autocompchem.molecule.conformation;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import autocompchem.molecule.conformation.ConformationalCoordinate.ConformationalCoordType;


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
	 * Version ID
	 */
	private static final long serialVersionUID = 1L;
	
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
    
    /**
     * Checks if all {@link ConformationalCoordinate}s contained here 
     * are of type {@link ConformationalCoordType#TORSION}.
     * @return <code>true</code> if all the {@link ConformationalCoordinate}s
     * are of type {@link ConformationalCoordType#TORSION}.
     */

	public boolean containsOnlyTorsions() 
	{
		for (ConformationalCoordinate cc : this)
	    {
			if (cc.getType() != ConformationalCoordType.TORSION)
				return false;
	    }
		return true;
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
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(CRDID, super.hashCode());
    }
  	
//-----------------------------------------------------------------------------

  	@Override
  	public ConformationalSpace clone()
  	{
  		ConformationalSpace clone = new ConformationalSpace();
  		for(ConformationalCoordinate coord : this)
  	   	{
  	   		clone.add(coord.clone());
  	   	}
  		clone.CRDID.set(this.CRDID.get());
  		return clone;
  	}

//-----------------------------------------------------------------------------

  	/**
  	 * Prepares an {@value Level.INFO} message with all the conformational 
  	 * coordinates defining this conformational space and sends it to the given 
  	 * logger.
  	 */
  	
  	public void printAll(Logger logger) 
  	{
  		logger.info(toPrintableString());
  	}
  	
//-----------------------------------------------------------------------------

  	/**
  	 * Prepares an {@value Level.INFO} message with all the conformational 
  	 * coordinates defining this conformational space and sends it to the given 
  	 * logger.
  	 */
  	
  	public String toPrintableString() 
  	{
  		String NL = System.getProperty("line.separator");
  		String numConformers = "is TOO LARGE TO COMPUTE!";
  		int size = getSize(); 
  		if (size>0)
  			numConformers = "= " + size;
  		String msg = "Conformational space is defined by " + this.size()
  				+ " coordinates (#Conformers " + numConformers + "):" + NL;
  		for (ConformationalCoordinate c : this)
  		{
  			 msg = msg + " -> " + c + NL;
  		}
  		return msg;
  	}
  	
//-----------------------------------------------------------------------------

  	/**
  	 * Changes all the atom identifiers as to reflect the given atom list 
  	 * reordering map.
  	 * @param atomReorderingMap mapping of OLD index (key) to NEW index (value).
  	 */
  	
	public void applyReorderingMap(Map<Integer, Integer> atomReorderingMap) {
		for(ConformationalCoordinate coord : this)
  	   	{
  	   		coord.applyReorderingMap(atomReorderingMap);
  	   	}
	}

//------------------------------------------------------------------------------
    
}
