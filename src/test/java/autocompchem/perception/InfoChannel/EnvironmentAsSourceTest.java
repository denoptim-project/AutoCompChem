package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.EnvironmentAsSource;
import autocompchem.perception.infochannel.InfoChannelType;

public class EnvironmentAsSourceTest 
{
//------------------------------------------------------------------------------

    /**
     * Produced an instance that is only meant for testing. The instance has a 
     * fake environment made of few dummy environmental variables.
     * @return a fake instance meant only for implementation.
     */
    public static EnvironmentAsSource getTestInstance()
    {
    	Map<String,String> env = new HashMap<String,String>();
    	env.put("VAR_FOO", "bar");
    	env.put("VAR_BAR", "123");

    	EnvironmentAsSource ic = new EnvironmentAsSource();
    	ic.setEnvironment(env);
    	
    	return ic;
    } 
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	EnvironmentAsSource ic = getTestInstance();
    	ic.setType(InfoChannelType.ENVIRONMENT);
    	
    	String json = writer.toJson(ic);
    	
    	EnvironmentAsSource fromJson = reader.fromJson(json, 
    			EnvironmentAsSource.class);
    	assertEquals(ic, fromJson);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	EnvironmentAsSource ic1 = getTestInstance();
    	ic1.setType(InfoChannelType.ENVIRONMENT);

    	EnvironmentAsSource ic2 = getTestInstance();
    	ic2.setType(InfoChannelType.ENVIRONMENT);

      	assertTrue(ic1.equals(ic2));
      	assertTrue(ic2.equals(ic1));
      	assertTrue(ic1.equals(ic1));
      	assertFalse(ic1.equals(null));
      	
      	Map<String,String> env = new HashMap<String,String>();
    	env.put("VAR_FOO", "bar");
      	ic2 = new EnvironmentAsSource();
      	ic2.setEnvironment(env);
    	ic2.setType(InfoChannelType.ENVIRONMENT);
      	assertFalse(ic1.equals(ic2));

      	ic2 = getTestInstance();
    	ic2.setType(InfoChannelType.OUTPUTFILE);
      	assertFalse(ic1.equals(ic2));
      	
      	ic2 = new EnvironmentAsSource();
      	ic2.setEnvironment(env);
    	ic2.setType(InfoChannelType.LOGFEED);
    	assertFalse(ic1.equals(ic2));
    }
    
//------------------------------------------------------------------------------

}
