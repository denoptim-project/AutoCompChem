package autocompchem.run;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;

/**
 * A dummy test job that can be parallelizable and simply logs into a file
 * every ca. half second, by default, but timings can be set. 
 * The job lasts a time defined in the constructor.
 * 
 * <p><b>WARNING:this type of {@link Job} is  not meant to be 
 * JSON-serialized!</b> 
 * No type adapted is provided in {@link ACCJson}, so attempts to use the
 *  default JSON-serialization will lead to stack overflow.</p>
 */

public class TestJob extends Job
{    	
	private int i = 0;
	private int wallTime = 0;
	private int delay = 500;
	private int period = 490;
	
    /**
     * The name of the parameter determining the prefix in the test job logs.
     */
    protected static final String PREFIX = "prefixForLogRecords";
    
    /**
     * String used to print iteration number in log files
     */
	protected static final String ITERATIONKEY = "Iteration";
    
//------------------------------------------------------------------------------
    
	/**
	 * Create a test job that logs into a text file. The jobs runs iterations
	 * that simply print a line into a file.
	 * @param logPathName pathname where the job should log.
	 * @param wallTime max run time for the job (seconds)
	 * @param parallel <code>true</code> is the job should be flagged as 
	 * parallelizable.
	 */
	public TestJob(String logPathName, int wallTime, boolean parallel)
	{
		super();
		this.appID = SoftwareId.ACC;
		stdout = new File(logPathName);
		this.wallTime = wallTime;
		setParallelizable(parallel);
		setNumberOfThreads(1);
		setParameter(PREFIX, "");
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Create a test job that logs into a text file. The jobs runs iterations
	 * that simply print a line into a file.
	 * @param logPathName pathname where the job should log.
	 * @param wallTime max run time for the job (seconds)
	 * @param delay time to wait after starting the job and before starting to 
	 * print the log (milliseconds).
	 * @param period the period between each printing iteration (milliseconds).
	 * @param parallel <code>true</code> if the job should be flagged as 
	 * parallelizable.
	 */
	public TestJob(String logPathName, int wallTime, int delay, int period,
			boolean parallel)
	{
		this(logPathName, wallTime, parallel);
		this.delay = delay;
		this.period = period;
		setNumberOfThreads(1);
	}
	
//------------------------------------------------------------------------------
	   
	@Override
	public void runThisJobSubClassSpecific()
	{
		// The dummy command will just ping every N-milliseconds
		ScheduledThreadPoolExecutor stpe = 
				new ScheduledThreadPoolExecutor(1);
		stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		stpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		Task tsk = new Task();
		tsk.prefix = getParameter(PREFIX).getValueAsString();
		
		stpe.scheduleAtFixedRate(tsk, delay, period,
				TimeUnit.MILLISECONDS);
		
		CountDownLatch cdl = new CountDownLatch(1);
        try {
			while (!cdl.await(wallTime, TimeUnit.SECONDS)) 
			{
				stpe.shutdownNow();
				break;
			}
		} catch (InterruptedException e) {
			//Ignoring exception: interruption is signalled by Job
			//e.printStackTrace();
		} finally {
			stpe.shutdownNow();
		}
	}
	
//------------------------------------------------------------------------------
	   
	private class Task implements Runnable
	{
		public String prefix = "";
		
		@Override
		public void run() 
		{
			i++;
			IOtools.writeLineAppend(stdout, 
					prefix + ITERATIONKEY + " " + i, 
					true);
		}
	}
}