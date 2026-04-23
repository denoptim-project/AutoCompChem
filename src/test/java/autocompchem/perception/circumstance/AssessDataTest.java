package autocompchem.perception.circumstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannelType;

/**
 * Unit tests for {@link AssessData}.
 */
public class AssessDataTest
{

//------------------------------------------------------------------------------

	@Test
	public void testEqualsAndHashCode()
	{
		AssessData a = new AssessData("d", "crit", false);
		AssessData b = new AssessData("d", "crit", false);
		assertTrue(a.equals(b));
		assertTrue(b.equals(a));
		assertTrue(a.equals(a));
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(null));

		assertFalse(a.equals(new AssessData("other", "crit", false)));
		assertFalse(a.equals(new AssessData("d", "other", false)));
		assertFalse(a.equals(new AssessData("d", "crit", true)));
	}

//------------------------------------------------------------------------------

	@Test
	public void testChannelTypeIsData()
	{
		AssessData ad = new AssessData("p", "c");
		assertEquals(InfoChannelType.DATA, ad.getChannelType());
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testJsonRoundTripAsICircumstanceAndConcrete() throws Exception
	{
		Gson writer = ACCJson.getWriter();
		Gson reader = ACCJson.getReader();

		AssessData original = new AssessData("a.b.c", "${x} > 0", true);
		String json = writer.toJson(original);

		ICircumstance fromIface = reader.fromJson(json, ICircumstance.class);
		assertEquals(original, fromIface);

		AssessData concrete = reader.fromJson(json, AssessData.class);
		assertEquals(original, concrete);
		assertEquals("a.b.c", concrete.getDataPath());
	}

//------------------------------------------------------------------------------

}
