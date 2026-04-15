package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;

/**
 * JSON round-trip tests for {@link TestJob} via {@link ACCJson}.
 */
public class TestJobJsonTest
{
	@TempDir
	File tempDir;

	@Test
	public void testRoundTripFourArgConstructorFields() throws Exception
	{
		File log = new File(tempDir, "ping.log");
		TestJob original = new TestJob(log.getAbsolutePath(), 7, 80, 99);
		original.setParameter(TestJob.PREFIX, "pre-");

		Gson writer = ACCJson.getWriter();
		Gson reader = ACCJson.getReader();
		String json = writer.toJson(original);
		assertTrue(json.contains("\"jobType\": \"TestJob\""));
		assertTrue(json.contains("\"logPath\""));

		Job parsed = reader.fromJson(json, Job.class);
		assertInstanceOf(TestJob.class, parsed);
		TestJob copy = (TestJob) parsed;
		assertEquals(original.getStdOut().getPath(), copy.getStdOut().getPath());
		assertEquals(original.getWallTime(), copy.getWallTime());
		assertEquals(original.getDelay(), copy.getDelay());
		assertEquals(original.getPeriod(), copy.getPeriod());
		assertEquals("pre-", copy.getParameter(TestJob.PREFIX).getValueAsString());
	}

	@Test
	public void testRoundTripTwoArgConstructorUsesDefaultDelayAndPeriod()
			throws Exception
	{
		File log = new File(tempDir, "simple.log");
		TestJob original = new TestJob(log.getAbsolutePath(), 2);

		Gson writer = ACCJson.getWriter();
		Gson reader = ACCJson.getReader();
		String json = writer.toJson(original);

		TestJob copy = (TestJob) reader.fromJson(json, Job.class);
		assertEquals(500, copy.getDelay());
		assertEquals(490, copy.getPeriod());
		assertEquals(2, copy.getWallTime());
	}
}
