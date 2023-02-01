package autocompchem.run.jobediting;

import autocompchem.run.Job;

public class SetJobParameter extends JobParameterEditTask
{
	/**
	 * The value to set for the parameter.
	 */
	final Object newValue;
	
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param name the name of the job parameter to create or change.
	 * @param newValue the value to assign to the parameter.
	 */
	public SetJobParameter(String name, Object newValue) 
	{
		super(name, TaskType.SET);
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
 	    
 	    SetJobParameter other = (SetJobParameter) o;
 	    
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
