package autocompchem.perception.circumstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.perception.infochannel.InfoChannelType;

public class ICircumstanceTest 
{
    
//------------------------------------------------------------------------------

	/**
	 * We do this test for the interface because we want to check if a
	 * collection of instances implementing the interface is deserialized
	 * to get the proper types.
	 */
    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	ICircumstance ic = new MatchText("blabla", true, 
    			InfoChannelType.ENVIRONMENT);
    	String json = writer.toJson(ic);
    	
    	ICircumstance fromJson = reader.fromJson(json, ICircumstance.class);
    	assertEquals(ic, fromJson);
    	
    	ic = new CountTextMatches("ribla", 2, 4, InfoChannelType.LOGFEED, false);
    	json = writer.toJson(ic);
    	fromJson = reader.fromJson(json, ICircumstance.class);
    	assertEquals(ic, fromJson);
    	
    	// Check case-insensitivity of enum strings
    	json = json.replaceAll(InfoChannelType.LOGFEED.toString(),
    			InfoChannelType.LOGFEED.toString().toLowerCase());
    	fromJson = reader.fromJson(json, ICircumstance.class);
    	assertEquals(ic, fromJson);
    }
    
//------------------------------------------------------------------------------

}
