package autocompchem.run;

import java.util.Objects;


/**
 * A software name is effectively a case-insensitive string that is used to 
 * identify a third parties software package when having to state the software 
 * that produced some data or requires some input.
 */
public class SoftwareId {

	private String name;
	
//------------------------------------------------------------------------------

	public SoftwareId(String name)
	{
		this.name = name;
	}

//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(name.toUpperCase());
    }

//-----------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o) 
    {
    	if (o == null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	   
 	   SoftwareId other = (SoftwareId) o;
 	   
 	   return name.equalsIgnoreCase(other.name);
    }	
    
//------------------------------------------------------------------------------

    /**
     * String representation.
     * @return the string representation.
     */

    @Override
    public String toString()
    {
    	return name;
    }

//-----------------------------------------------------------------------------
  	
}