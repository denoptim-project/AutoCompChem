package autocompchem.run.jobediting;

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;

public class SetJobParameter implements IJobEditingTask
{
	final TaskType task = TaskType.SET_JOB_PARAMETER;
	
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
	public void applyChange(Job job) {
		// TODO Auto-generated method stub

	}
	
//------------------------------------------------------------------------------

}
