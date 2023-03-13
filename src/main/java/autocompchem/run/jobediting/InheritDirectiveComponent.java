package autocompchem.run.jobediting;


import java.util.List;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.DirComponentTypeAndName;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.run.Job;


/**
 * Task that copies a {@link IDirectiveComponent} from a {@link CompChemJob} 
 * into another one. This task always adds components, does not overwrite, 
 * meaning that if a component with the same {@link DirComponentAddress} and
 * {@link DirComponentTypeAndName} already exists in the destination job, we
 * append the one/s inherited beside the existing one.
 */

public class InheritDirectiveComponent implements IJobSettingsInheritTask
{	
	/**
	 * Defines which type of setting task this is. It also defines what is
	 * the type of content this task is setting.
	 */
	final JobEditType task;
	
	/**
	 * Address to component to inherit from the source job. This is also the 
	 * address that the inherited component will have in the destination job.
	 */
	private final DirComponentAddress path;
	
//------------------------------------------------------------------------------
	
	public InheritDirectiveComponent(DirComponentAddress path) 
	{
		this.path = path;
		switch (path.getLast().type)
		{
		case DIRECTIVE:
			this.task = JobEditType.INHERIT_DIRECTIVE;
			break;
		case DIRECTIVEDATA:
			this.task = JobEditType.INHERIT_DIRECTIVEDATA;
			break;
		case KEYWORD:
			this.task = JobEditType.INHERIT_KEYWORD;
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
 	    
 	    InheritDirectiveComponent other = (InheritDirectiveComponent) o;
 	    
 	    return this.path.equals(other.path);
    }
	
//------------------------------------------------------------------------------

	@Override
	public void inheritSettings(Job source, Job destination)
			throws CloneNotSupportedException 
	{
		if (!(source instanceof CompChemJob))
			return;
		CompChemJob srcCcj = (CompChemJob) source;
		if (!(destination instanceof CompChemJob))
			return;
		CompChemJob dstCcj = (CompChemJob) destination;
		
		List<IDirectiveComponent> toInherit = srcCcj.getDirectiveComponents(
				path);
		if (toInherit.size()==0)
		{
			return;
		}
		DirComponentAddress parentAddress = path.getParent();
		if (parentAddress.size()==0)
		{
			for (IDirectiveComponent component : toInherit)
			{
				if (!(component instanceof Directive))
				{
					// should never happen
					continue;
				}
				// WARNING: we ADD the directives, meaning that if any such
				// directive is already present it remains there. This is
				// because we want to be able to add multiple directives with 
				// the same name. So, to overwrite the previous one we should 
				// go via a REMOVE_DIRECTIVE, first, and then INHERIT_DIRECTIVE.
				dstCcj.addDirective((Directive) component);
			}
		} else {
			dstCcj.ensureDirectiveStructure(parentAddress);
			List<IDirectiveComponent> parentDirs = dstCcj.getDirectiveComponents(
					parentAddress);
			for (IDirectiveComponent parentDirComp : parentDirs)
			{
				// These can only be Directives
				Directive parentDir = (Directive) parentDirComp;
				for (IDirectiveComponent component : toInherit)
				{
					parentDir.addComponent(component);
				}
			}
		}
	}

//------------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return task + " " + path;
	}
	
//------------------------------------------------------------------------------

}
