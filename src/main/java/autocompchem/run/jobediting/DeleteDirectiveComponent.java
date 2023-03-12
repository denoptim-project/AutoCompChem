package autocompchem.run.jobediting;


import java.util.List;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.DirComponentTypeAndName;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.run.Job;


/**
 * Task removing a {@link IDirectiveComponent} from a {@link CompChemJob}.
 */

public class DeleteDirectiveComponent implements IJobEditingTask
{	
	/**
	 * Defines which type of setting task this is. It also defines what is
	 * the type of content this task is setting.
	 */
	final TaskType task;
	
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
			this.task = TaskType.REMOVE_DIRECTIVE;
			break;
		case DIRECTIVEDATA:
			this.task = TaskType.REMOVE_DIRECTIVEDATA;
			break;
		case KEYWORD:
			this.task = TaskType.REMOVE_KEYWORD;
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
	
//------------------------------------------------------------------------------

	@Override
	public void applyChange(Job job)
	{
		if (!(job instanceof CompChemJob))
			return;
		CompChemJob ccj = (CompChemJob) job;
		
		List<IDirectiveComponent> toDel = ccj.getDirectiveComponents(path);
		if (path.size()==1)
		{
			// We are removing outermost directives: no other component can
			// have a path of length 1.
			for (IDirectiveComponent component : toDel)
			{
				ccj.removeDirective((Directive) component);
			}
		} else {
			// We are removing embedded components.
			DirComponentAddress pathToParents = path.getParent();
			DirComponentTypeAndName compToDel = path.getLast();
			List<IDirectiveComponent> parentDirs = ccj.getDirectiveComponents(
					pathToParents);
			for (IDirectiveComponent parentDirComp : parentDirs)
			{
				// These can only be Directives
				Directive parentDir = (Directive) parentDirComp;
				parentDir.deleteComponent(compToDel);
			}
		}
	}
	
//------------------------------------------------------------------------------

}
