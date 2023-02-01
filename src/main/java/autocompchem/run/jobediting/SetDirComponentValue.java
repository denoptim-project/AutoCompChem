package autocompchem.run.jobediting;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.run.Job;

/**
 * Class for tasks that add or change {@link IDirectiveComponent}s of a 
 * {@link CompChemJob}. Any other job is not affected by this kind of task.
 */
public class SetDirComponentValue extends DirComponentEditTask
{
	/**
	 * The value to set for the parameter.
	 */
	final Object newValue;

//------------------------------------------------------------------------------

	/**
	 * Constructor
	 * @param address the location of the {@link IDirectiveComponent}s to create 
	 * or change. 
	 * @param newValue the value to assign to the parameter.
	 */
	public SetDirComponentValue(DirComponentAddress address, 
			Object newValue) 
	{
		super(address, TaskType.SET);
		this.newValue = newValue;
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
 	    
 	   SetDirComponentValue other = (SetDirComponentValue) o;
 	    
 	    if (!this.newValue.equals(other.newValue))
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
