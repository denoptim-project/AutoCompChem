package autocompchem.run;

/**
 * A dummy test job that triggers and exception every time it runs.
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