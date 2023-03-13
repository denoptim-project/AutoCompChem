package autocompchem.run.jobediting;


import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;


/**
 * Task that sets a parameter in a {@link Job}. Note the difference between
 * {@link Job}'s parameters and {@link CompChemJob}'s and their components. This
 * task aims at editing only a single parameter of any job.
 */

public class SetJobParameter implements IJobEditingTask
{
	final JobEditType task = JobEditType.SET_JOB_PARAMETER;
	
	/**
	 * The value to set for the parameter.
	 */
	final NamedData parameter;

	
//------------------------------------------------------------------------------
	
	public SetJobParameter(NamedData parameter) 
	{
		this.parameter = parameter;
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
 	    
 	    return this.parameter.equals(other.parameter);
    }
	
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job) 
	{
		job.setParameter(parameter);
	}
	
//------------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return task + " " + parameter;
	}
	
//------------------------------------------------------------------------------

}
