package autocompchem.run;

import autocompchem.io.ACCJson;

/**
 * A dummy test job that triggers and exception every time it runs.
 * 
 * <p><b>WARNING:this type of {@link Job} is  not meant to be 
 * JSON-serialized!</b> 
 * No type adapted is provided in {@link ACCJson}, so attempts to use the
 *  default JSON-serialization will lead to stack overflow.</p>
 */

public class TestJobTriggeringException extends Job
{ 
	/**
	 * The message that will be placed in the {@link Exception} triggered by 
	 * this job.
	 */
    public static final String MSG = "Triggered for testing purpuses.";
    
//------------------------------------------------------------------------------
    
	public TestJobTriggeringException()
	{
		super();
		this.appID = AppID.ACC;
		this.setParallelizable(true);
	}
	
//------------------------------------------------------------------------------
	   
	@Override
	public void runThisJobSubClassSpecific()
	{
		thrownExc = new Exception(MSG);
		hasException = true;
	}
	
//------------------------------------------------------------------------------
	
}