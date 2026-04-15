package autocompchem.run;

import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.io.IOtools;

/**
 * A dummy job that logs into a file on a fixed schedule, for tests and tooling.
 *
 * <p>JSON serialization uses {@link TestJobSerializer}; deserialization is
 * handled in {@link Job.JobDeserializer} when {@code jobType} is
 * {@code TestJob}.</p>
 */
public class TestJob extends Job
{
	public static final String JSON_LOG_PATH = "logPath";
	public static final String JSON_WALL_TIME = "wallTime";
	public static final String JSON_DELAY = "delay";
	public static final String JSON_PERIOD = "period";

	private int i = 0;
	private int wallTime = 0;
	private int delay = 500;
	private int period = 490;

	/** Parameter name for the prefix prepended to each log line. */
	public static final String PREFIX = "prefixForLogRecords";

	/**
	 * Token printed before the iteration number in log lines.
	 */
	public static final String ITERATIONKEY = "Iteration";

//------------------------------------------------------------------------------

	public TestJob(String logPathName, int wallTime)
	{
		super();
		this.appID = SoftwareId.ACC;
		stdout = getNewFile(logPathName);
		this.wallTime = wallTime;
		setNumberOfThreads(1);
		setParameter(PREFIX, "");
	}

//------------------------------------------------------------------------------

	public TestJob(String logPathName, int wallTime, int delay, int period)
	{
		this(logPathName, wallTime);
		this.delay = delay;
		this.period = period;
		setNumberOfThreads(1);
	}

//------------------------------------------------------------------------------

	public int getWallTime()
	{
		return wallTime;
	}

	public int getDelay()
	{
		return delay;
	}

	public int getPeriod()
	{
		return period;
	}

//------------------------------------------------------------------------------

	@Override
	public void runThisJobSubClassSpecific()
	{
		ScheduledThreadPoolExecutor stpe =
				new ScheduledThreadPoolExecutor(1);
		stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		stpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		Task tsk = new Task();
		tsk.prefix = getParameter(PREFIX).getValueAsString();

		stpe.scheduleAtFixedRate(tsk, delay, period,
				TimeUnit.MILLISECONDS);

		CountDownLatch cdl = new CountDownLatch(1);
		try
		{
			while (!cdl.await(wallTime, TimeUnit.SECONDS))
			{
				stpe.shutdownNow();
				break;
			}
		} catch (InterruptedException e)
		{
			// Ignoring exception: interruption is signalled by Job
		} finally
		{
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
			copyFilesToWorkDir();

			i++;
			IOtools.writeLineAppend(stdout,
					prefix + ITERATIONKEY + " " + i,
					true);
		}
	}

//------------------------------------------------------------------------------

	public static class TestJobSerializer implements JsonSerializer<TestJob>
	{
		@Override
		public JsonElement serialize(TestJob job, Type typeOfSrc,
				JsonSerializationContext context)
		{
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty(Job.JSONJOBTYPE, job.getClass().getSimpleName());
			if (job.getStdOut() != null)
			{
				jsonObject.addProperty(JSON_LOG_PATH, job.getStdOut().getPath());
			}
			jsonObject.addProperty(JSON_WALL_TIME, job.wallTime);
			jsonObject.addProperty(JSON_DELAY, job.delay);
			jsonObject.addProperty(JSON_PERIOD, job.period);
			if (!job.params.isEmpty())
			{
				jsonObject.add(Job.JSONPARAMS, context.serialize(job.params));
			}
			if (!job.steps.isEmpty())
			{
				jsonObject.add(Job.JSONSUBJOBS, context.serialize(job.steps));
			}
			return jsonObject;
		}
	}
}
