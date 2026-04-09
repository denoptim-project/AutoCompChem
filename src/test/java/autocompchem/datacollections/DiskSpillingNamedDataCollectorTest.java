package autocompchem.datacollections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.io.ACCJson;
import autocompchem.molecule.vibrations.NormalMode;

/**
 * Tests for {@link DiskSpillingNamedDataCollector}.
 */
public class DiskSpillingNamedDataCollectorTest
{

	@TempDir
	File tempDir;

//------------------------------------------------------------------------------

	@Test
	public void testSpilledJsonableRoundTrip() throws Exception
	{
		File spill = new File(tempDir, "spill");
		DiskSpillingNamedDataCollector c =
				new DiskSpillingNamedDataCollector(spill);
		c.putNamedData(new NamedData("k1", "hello"));
		assertTrue(spill.listFiles().length >= 1);
		assertTrue(new File(spill, "k1.nd.json").isFile());

		NamedData back = c.getNamedData("k1");
		assertEquals("hello", back.getValue());
	}

//------------------------------------------------------------------------------

	@Test
	public void testNonJsonableStaysInMemory() throws Exception
	{
		File spill = new File(tempDir, "spill2");
		DiskSpillingNamedDataCollector c =
				new DiskSpillingNamedDataCollector(spill);
		NormalMode nm = new NormalMode();
		c.putNamedData(new NamedData("NM", nm));

		assertEquals(nm, c.getNamedData("NM").getValue());
		assertEquals(0, spill.listFiles().length);
	}

//------------------------------------------------------------------------------

	@Test
	public void testClearRemovesSpillFiles() throws Exception
	{
		File spill = new File(tempDir, "spill3");
		DiskSpillingNamedDataCollector c =
				new DiskSpillingNamedDataCollector(spill);
		c.putNamedData(new NamedData("n", 42));
		int nFilesBefore = spill.listFiles().length;
		assertTrue(nFilesBefore >= 1);

		c.clear();
		assertTrue(c.isEmpty());
		assertEquals(0, spill.listFiles().length);
	}

//------------------------------------------------------------------------------

	@Test
	public void testReservedReferenceUsesEncodedFilename() throws Exception
	{
		assertFalse(DiskSpillingNamedDataCollector.canUsePlainReferenceFilename(
				"CON"));
		File spill = new File(tempDir, "spill4");
		DiskSpillingNamedDataCollector c =
				new DiskSpillingNamedDataCollector(spill);
		c.putNamedData(new NamedData("CON", "x"));
		boolean foundPlain = new File(spill, "CON.nd.json").exists();
		assertFalse(foundPlain);
		assertEquals("x", c.getNamedData("CON").getValue());
	}

//------------------------------------------------------------------------------

	@Test
	public void testReadsLegacyBase64NamedSpillFile() throws Exception
	{
		File spill = new File(tempDir, "spill5");
		String ref = "LegacyKey";
		DiskSpillingNamedDataCollector c =
				new DiskSpillingNamedDataCollector(spill);
		c.putNamedData(new NamedData(ref, "from-legacy"));
		File plain = new File(spill, ref + ".nd.json");
		assertTrue(plain.delete());

		String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(
				ref.getBytes(StandardCharsets.UTF_8));
		File legacy = new File(spill, b64 + ".nd.json");
		try (FileWriter fw = new FileWriter(legacy))
		{
			ACCJson.getWriter().toJson(new NamedData(ref, "from-legacy"), fw);
		}

		assertEquals("from-legacy", c.getNamedData(ref).getValue());
	}
}
