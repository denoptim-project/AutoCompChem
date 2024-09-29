package autocompchem.wiro.chem;

/**
 * The pair of two bits of info: the type and the reference name of a directive
 * component. This class does not contain any reference to an actual instance of
 * directive component, but it represents the possibility to have a directive 
 * component with the specified name and type. Note that such component might
 * not exist.
 */
public class DirComponentTypeAndName 
{
	/**
	 * The type of the component
	 */
	public final DirectiveComponentType type;
	
	/**
	 * The reference name of the component
	 */
	public final String name;

//------------------------------------------------------------------------------
	
	public DirComponentTypeAndName(String name, DirectiveComponentType type)
	{
		this.type = type;
		this.name = name;
	}
	
//------------------------------------------------------------------------------	

	@Override
	public boolean equals(Object o)
	{
		if (o == null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	    
 	    DirComponentTypeAndName other = (DirComponentTypeAndName) o;
 	   
 	    return this.name.equals(other.name) 
 	    		&& this.type.equals(other.type);
	}
	
//------------------------------------------------------------------------------
	
}
