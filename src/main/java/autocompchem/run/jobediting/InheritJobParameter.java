package autocompchem.run.jobediting;


import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;


/**
 * Task that copies a parameter from a {@link Job} into another one.
 * Note the difference between
 * {@link Job}'s parameters and {@link CompChemJob}'s and their components.
 */

public class InheritJobParameter implements IJobSettingsInheritTask
{
	final TaskType task = TaskType.INHERIT_JOB_PARAMETER;
	
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

}
