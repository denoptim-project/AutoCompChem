package autocompchem.run.jobediting;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.chemsoftware.Keyword;
import autocompchem.run.Job;

public class SetKeyword implements IJobEditingTask
{
	final TaskType task = TaskType.SET_KEYWORD;
	
	/**
	 * Address to parent directive where the keyword is to be set
	 */
	private final DirComponentAddress pathToDirective;
	
	/**
	 * The value to set for the parameter.
	 */
	final Keyword keyword;
	

//------------------------------------------------------------------------------

	public SetKeyword(DirComponentAddress parent, Keyword key)
	{
		this.pathToDirective = parent;
		this.keyword = key;
	}
	
//------------------------------------------------------------------------------

	public SetKeyword(String pathToParent, Keyword key)
	{
		this.pathToDirective = DirComponentAddress.fromString(pathToParent);
		this.keyword = key;
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
 	    
 	   SetKeyword other = (SetKeyword) o;
 	    
 	    if (!this.keyword.equals(other.keyword))
 	    	return false;
 	    
 	    return this.pathToDirective.equals(other.pathToDirective);
    }
    
//------------------------------------------------------------------------------
	
	@Override
	public void applyChange(Job job) {
		// TODO Auto-generated method stub
		
	}
    
//------------------------------------------------------------------------------
	
}
