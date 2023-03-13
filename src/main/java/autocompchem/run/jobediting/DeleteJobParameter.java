package autocompchem.run.jobediting;

import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;

public class DeleteJobParameter implements IJobEditingTask
{	
	final TaskType task = TaskType.REMOVE_JOB_PARAMETER;
	
	/**
	 * The name of the parameter to delete from the job. Case Insensitive!
	 */
	final String paramName;

	
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param name the name of the job parameter to create or change. This is
	 * case insensitive!
	 * @param newValue the value to assign to the parameter.
	 */
	public DeleteJobParameter(String paramReference) 
	{
		this.paramName = paramReference.toUpperCase();
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
	    
	    DeleteJobParameter other = (DeleteJobParameter) o;
	    
	    return this.paramName.equals(other.paramName);
	}
	
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job) 
	{
		if (job.hasParameter(paramName))
		{
			job.getParameters().removeData(paramName);
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
