package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;

public class InfoChannelBaseTest {
    
//------------------------------------------------------------------------------

    public static InfoChannelBase getTestInstance()
    {
    	InfoChannelBase icb = new InfoChannelBase();
    	icb.addChannel(FileAsSourceTest.getTestInstance());
    	icb.addChannel(EnvironmentAsSourceTest.getTestInstance());
    	icb.addChannel(ShortTextAsSourceTest.getTestInstance());
    	return icb;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	InfoChannelBase a1 = getTestInstance();
    	
    	InfoChannelBase a2 = getTestInstance();

      	assertTrue(a1.equals(a2));
      	assertTrue(a2.equals(a1));
      	assertTrue(a1.equals(a1));
      	assertFalse(a1.equals(null));
      	
      	a2 = new InfoChannelBase();
      	a2.addChannel(new FileAsSource("dummy"));
      	assertFalse(a1.equals(a2));

      	a2 = getTestInstance();
    	((FileAsSource) a2.getAllChannels().get(0)).setType(
    			InfoChannelType.ENVIRONMENT);
      	assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetSituationDB() throws Exception
    {
    	InfoChannelBase sb = InfoChannelBase.getInfoChannelDB("test_infochannels");
    	assertEquals(3, sb.getAllChannels().size());
    	
    	Set<String> expectedNames = Set.of(
    			"ic1",
    			"ic2",
    			"ic3");
    	for (InfoChannel ic : sb.getAllChannels())
    	{
    		assertTrue(ic instanceof FileAsSource);
    		assertTrue(expectedNames.contains(((FileAsSource) ic).getPathName()));
    	}
    }
    
//------------------------------------------------------------------------------

	/*
	 * Here we test is the serialization works well even without knowing which 
	 * implementation of the InfoChannel we are dealing with.
	 */
    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	InfoChannelBase icb = getTestInstance();
    	
    	String json = writer.toJson(icb);
    	
    	InfoChannelBase fromJson = reader.fromJson(json, InfoChannelBase.class);
    	assertEquals(icb, fromJson);
    }
    
//------------------------------------------------------------------------------

}
