package autocompchem.run.jobediting;

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.run.Job;

public class SetDirComponentParameter extends SetDirComponentValue 
{
	/**
	 * The name of the parameter to set in the specified 
	 * {@link IDirectiveComponent}.
	 */
	final String paramName;

//------------------------------------------------------------------------------

	/**
	 * Constructor
	 * @param address the location of the {@link IDirectiveComponent}s to create 
	 * or change and that will contain the parameter we add or change.
	 * @param paramName the name of the parameter to set.
	 * @param newValue the value to assign to the parameter.
	 */
	
	public SetDirComponentParameter(DirComponentAddress address, 
			String paramName, Object newValue) 
	{
		super(address, newValue);
		this.paramName = paramName;
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
 	    
 	    SetDirComponentParameter other = (SetDirComponentParameter) o;
 	    
 	    if (!this.paramName.equals(other.paramName))
 	    	return false;
 	    
 	    return super.equals(o);
    }
	
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job) {
		// TODO Auto-generated method stub
		
	}
	
//------------------------------------------------------------------------------

}
