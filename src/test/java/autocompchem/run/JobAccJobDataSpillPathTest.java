package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Job#resolveExposedOutputSpillDirectory(File, String)}.
 */
public class JobAccJobDataSpillPathTest
{

	@TempDir
	File tempDir;

//------------------------------------------------------------------------------

	@Test
	public void rootJobIdMapsToAccJobDataWithRootSegmentSuffix()
	{
		assertTrue(tempDir.isDirectory(), "Should be a directory");
		File w = tempDir;
		File spill = Job.resolveExposedOutputSpillDirectory(w, "hash", "#0");
		assertEquals(new File(w, Job.ACC_JOB_DATA_DIR_NAME + "-hash_0"), spill);
	}

//------------------------------------------------------------------------------

	@Test
	public void nestedIdsMapToSubfolders()
	{
		assertTrue(tempDir.isDirectory(), "Should be a directory");
		File w = tempDir;
		File base = new File(w, Job.ACC_JOB_DATA_DIR_NAME + "-hash_0");
		assertEquals(new File(base, "0"),
				Job.resolveExposedOutputSpillDirectory(w, "hash", "#0.0"));
		assertEquals(new File(new File(base, "0"), "0"),
				Job.resolveExposedOutputSpillDirectory(w, "hash", "#0.0.0"));
		assertEquals(new File(base, "1"),
				Job.resolveExposedOutputSpillDirectory(w, "hash", "#0.1"));
		assertEquals(new File(new File(w, Job.ACC_JOB_DATA_DIR_NAME + "-hash_2"), "2"),
				Job.resolveExposedOutputSpillDirectory(w, "hash", "#2.2"));
		assertEquals(new File(base, "4"),
				Job.resolveExposedOutputSpillDirectory(w, "hash", "#0.4"));
		assertEquals(new File(new File(base, "4"), "0"),
				Job.resolveExposedOutputSpillDirectory(w, "hash", "#0.4.0"));
	}

//------------------------------------------------------------------------------

	@Test
	public void rejectsUnsafeSegments()
	{
		assertTrue(tempDir.isDirectory(), "Should be a directory");
		File w = tempDir;
		assertThrows(IllegalArgumentException.class,
				() -> Job.resolveExposedOutputSpillDirectory(w, "hash", "#0..1"));
	}

//------------------------------------------------------------------------------
}
