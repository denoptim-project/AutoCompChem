package autocompchem.run.jobediting;


import java.util.Objects;

import autocompchem.run.Job;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.IDirectiveComponent;


/**
 * Task removing a {@link IDirectiveComponent} from a {@link CompChemJob}.
 */

public class DeleteDirectiveComponent implements IJobEditingTask
{	
	/**
	 * Defines which type of setting task this is. It also defines what is
	 * the type of content this task is setting.
	 */
	final JobEditType task;
	
	/**
	 * Address to component to remove.
	 */
	private final DirComponentAddress path;
	
//------------------------------------------------------------------------------
	
	public DeleteDirectiveComponent(DirComponentAddress path) 
	{
		this.path = path;
		switch (path.getLast().type)
		{
		case DIRECTIVE:
			this.task = JobEditType.REMOVE_DIRECTIVE;
			break;
		case DIRECTIVEDATA:
			this.task = JobEditType.REMOVE_DIRECTIVEDATA;
			break;
		case KEYWORD:
			this.task = JobEditType.REMOVE_KEYWORD;
			break;
		default:
			throw new Error("Unrecognized type of directive "
					+ "component. Please, contact the developers.");
		}
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
 	    
 	    DeleteDirectiveComponent other = (DeleteDirectiveComponent) o;
 	    
 	    return this.path.equals(other.path);
    }

//-----------------------------------------------------------------------------
      
      @Override
      public int hashCode()
      {
      	return Objects.hash(path);
      }
      
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job)
	{
		if (!(job instanceof CompChemJob))
			return;
		CompChemJob ccj = (CompChemJob) job;
		ccj.removeDirectiveComponent(path);
	}
	
//------------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return task + " " + path;
	}
	
//------------------------------------------------------------------------------

}
