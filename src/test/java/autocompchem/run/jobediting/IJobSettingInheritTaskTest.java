package autocompchem.run.jobediting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.io.ACCJson;

public class IJobSettingInheritTaskTest 
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
    	
    	IJobSettingsInheritTask act = new InheritDirectiveComponent(
    			DirComponentAddress.fromString("*:*|Dir:DirName"));
    	String json = writer.toJson(act);
    	IJobSettingsInheritTask fromJson = reader.fromJson(json, 
    			IJobSettingsInheritTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new InheritJobParameter("paramToInherit");
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobSettingsInheritTask.class);
    	assertEquals(act, fromJson);
    }
    
//------------------------------------------------------------------------------

}
