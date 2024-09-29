package autocompchem.run.jobediting;


import autocompchem.run.Job;
import autocompchem.wiro.chem.CompChemJob;


/**
 * Task that copies a parameter from a {@link Job} into another one.
 * Note the difference between
 * {@link Job}'s parameters and {@link CompChemJob}'s and their components.
 */

public class InheritJobParameter implements IJobSettingsInheritTask
{
	final JobEditType task = JobEditType.INHERIT_JOB_PARAMETER;
	
	/**
	 * The name of the parameter to inherit.
	 */
	final String paramName;
	
//------------------------------------------------------------------------------
	
	public InheritJobParameter(String paramName) 
	{
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
 	    
 	    InheritJobParameter other = (InheritJobParameter) o;
 	    
 	    return this.paramName.equals(other.paramName);
    }
	
//------------------------------------------------------------------------------

	@Override
	public void inheritSettings(Job source, Job destination)
			throws CloneNotSupportedException 
	{
		if (source.hasParameter(paramName))
		{
			destination.setParameter(source.getParameter(paramName).clone());
		}
	}
	
//------------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return task + " " + paramName;
	}
	
//------------------------------------------------------------------------------

}
