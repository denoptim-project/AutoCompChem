package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.ShortTextAsSource;
import autocompchem.perception.infochannel.FileAsSource;

public class InfoChannelTest {
    
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
    	
    	InfoChannel ic = new FileAsSource("foo.bar");
    	ic.setType(InfoChannelType.INPUTFILE);
    	
    	String json = writer.toJson(ic);
    	
    	InfoChannel fromJson = reader.fromJson(json, InfoChannel.class);
    	assertEquals(ic, fromJson);
    	
    	ic = new ShortTextAsSource("foo.bar");
    	ic.setType(InfoChannelType.INPUTFILE);
    	
    	json = writer.toJson(ic);
    	
    	fromJson = reader.fromJson(json, InfoChannel.class);
    	assertEquals(ic, fromJson);
    }
}
