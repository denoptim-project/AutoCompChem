package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.FileAsSource;

public class FileAsSourceTest {
    
//------------------------------------------------------------------------------

    public static FileAsSource getTestInstance()
    {
    	FileAsSource fas = new FileAsSource("foo.bar");
    	fas.setType(InfoChannelType.INPUTFILE);
    	return fas;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	InfoChannel ic_fas = getTestInstance();
    	
    	String json = writer.toJson(ic_fas);
    	
    	InfoChannel fromJson = reader.fromJson(json, InfoChannel.class);
    	assertEquals(ic_fas, fromJson);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	FileAsSource a1 = getTestInstance();
    	
    	FileAsSource a2 = getTestInstance();

      	assertTrue(a1.equals(a2));
      	assertTrue(a2.equals(a1));
      	assertTrue(a1.equals(a1));
      	assertFalse(a1.equals(null));
      	
      	a2 = new FileAsSource("bar.foo");
    	a2.setType(InfoChannelType.INPUTFILE);
      	assertFalse(a1.equals(a2));

      	a2 = new FileAsSource("foo.bar");
    	a2.setType(InfoChannelType.OUTPUTFILE);
      	assertFalse(a1.equals(a2));
      	
      	a2 = new FileAsSource("bar.foo");
    	a2.setType(InfoChannelType.LOGFEED);
    	assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------

}
