package autocompchem.run.jobediting;

import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;

public class DeleteJobParameter implements IJobEditingTask
{	
	final TaskType task = TaskType.REMOVE_JOB_PARAMETER;
	
	/**
	 * The name of the parameter to delete from the job
	 */
	final String paramReference;

	
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param name the name of the job parameter to create or change.
	 * @param newValue the value to assign to the parameter.
	 */
	public DeleteJobParameter(String paramReference) 
	{
		this.paramReference = paramReference;
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
	    
	    return this.paramReference.equals(other.paramReference);
	}
	
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job) {
		// TODO Auto-generated method stub

	}
	
//------------------------------------------------------------------------------

}
