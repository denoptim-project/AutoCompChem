package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.ShortTextAsSource;

public class ShortTextAsSourceTest {
    
//------------------------------------------------------------------------------

    public static ShortTextAsSource getTestInstance()
    {
    	List<String> lines = new ArrayList<String>();
    	lines.add("first line");
    	lines.add("foo");
    	lines.add("bar");
    	ShortTextAsSource ic = new ShortTextAsSource(lines);
    	ic.setType(InfoChannelType.INPUTFILE);
    	return ic;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	ShortTextAsSource ic = getTestInstance();
    	
    	String json = writer.toJson(ic);
    	
    	ShortTextAsSource fromJson = reader.fromJson(json, 
    			ShortTextAsSource.class);
    	assertEquals(ic, fromJson);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ShortTextAsSource ic1 = getTestInstance();

    	ShortTextAsSource ic2 = getTestInstance();

      	assertTrue(ic1.equals(ic2));
      	assertTrue(ic2.equals(ic1));
      	assertTrue(ic1.equals(ic1));
      	assertFalse(ic1.equals(null));
      	
      	ic2 = new ShortTextAsSource("new text");
      	assertFalse(ic1.equals(ic2));

      	ic2 = getTestInstance();
    	ic2.setType(InfoChannelType.OUTPUTFILE);
      	assertFalse(ic1.equals(ic2));
      	
      	ic2 = new ShortTextAsSource("new text");
    	ic2.setType(InfoChannelType.LOGFEED);
    	assertFalse(ic1.equals(ic2));
    }
    
//------------------------------------------------------------------------------

}
