package autocompchem.run.jobediting;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.run.Job;

/**
 * Class for tasks that affects a {@link IDirectiveComponent}s of a 
 * {@link CompChemJob}. Any other job is not affected by this kind of task.
 */
public abstract class DirComponentEditTask extends EditTask
{

	public static final String TARGETELMINJSON = "targetDirComponent";
	
//------------------------------------------------------------------------------

	/**
	 * Constructor
	 * @param address the location of the {@link IDirectiveComponent}s to create 
	 * or change. 
	 * @param type the type of change to operate on the target
	 */
	public DirComponentEditTask(DirComponentAddress address, TaskType type)
	{
		super(address, type);
	}	

//------------------------------------------------------------------------------

	/**
	 * Returns the string pointing at the target for JSON format.
	 * @return
	 */
	@Override
	public String getTargetPointerInJSON()
	{
		return ((DirComponentAddress) target).toString();
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
