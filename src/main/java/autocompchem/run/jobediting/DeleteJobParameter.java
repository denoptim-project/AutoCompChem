package autocompchem.run.jobediting;

import autocompchem.run.Job;

public class DeleteJobParameter extends JobParameterEditTask
{
	
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param name the name of the job parameter to create or change.
	 * @param newValue the value to assign to the parameter.
	 */
	public DeleteJobParameter(String name) 
	{
		super(name, TaskType.DELETE);
	}
	
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job) {
		// TODO Auto-generated method stub

	}
	
//------------------------------------------------------------------------------

}
