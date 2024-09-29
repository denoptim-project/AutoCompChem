package autocompchem.run.jobediting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Keyword;

public class IJobEditingTaskTest 
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
    	
    	IJobEditingTask act = new AddDirectiveComponent("*:*|Dir:DirName", 
    			new Keyword("KeyName", false, 1.234));
    	String json = writer.toJson(act);
    	IJobEditingTask fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new SetJobParameter(new NamedData("ParamToSet", "valueOfParam"));
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new DeleteJobParameter("NameOfParamToRemove");
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	act = new DeleteDirectiveComponent(
    			DirComponentAddress.fromString(
    					"*:" + DirComponentAddress.ANYNAME 
    					+ "|Dir:DirName"));
    	json = writer.toJson(act);
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    	
    	// Check case-insensitivity of enum strings
    	json = json.replaceAll(JobEditType.REMOVE_DIRECTIVE.toString(),
    			JobEditType.REMOVE_DIRECTIVE.toString().toLowerCase());
    	
    	fromJson = reader.fromJson(json, IJobEditingTask.class);
    	assertEquals(act, fromJson);
    }
    
//------------------------------------------------------------------------------

}
