package autocompchem.run.jobediting;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.run.Job;

/**
 * Class for tasks that affects a parameter of a {@link Job}. 
 */
public abstract class JobParameterEditTask extends EditTask
{
	
	public static final String TARGETELMINJSON = "targetJobParameter";

//------------------------------------------------------------------------------

	/**
	 * Constructor
	 * @param name the name of the target parameter to edit. 
	 * @param type the type of change to operate on the target
	 */
	public JobParameterEditTask(String name, TaskType type)
	{
		super(name, type);
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Returns the string pointing at the target for JSON format.
	 * @return
	 */
	public String getTargetPointerInJSON()
	{
		return target.toString();
	}
	
//------------------------------------------------------------------------------

	/**
	 * Returns the string used in JSON format to define the pointer to the 
	 * target.
	 */
	@Override
	public String getTargetElementInJSON()
	{
		return TARGETELMINJSON;
	}
	
//------------------------------------------------------------------------------
	
}
