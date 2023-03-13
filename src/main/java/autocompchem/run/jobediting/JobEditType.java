package autocompchem.run.jobediting;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Known types of job editing task. It also defines what is
 * the type of content this task is setting.
 */
public enum JobEditType {
	INHERIT_DIRECTIVE,
	INHERIT_DIRECTIVEDATA,
	INHERIT_KEYWORD,
	INHERIT_JOB_PARAMETER,
	REMOVE_JOB_PARAMETER, 
	REMOVE_DIRECTIVE,
	REMOVE_KEYWORD,
	REMOVE_DIRECTIVEDATA,
	SET_JOB_PARAMETER,
	SET_DIRECTIVE,
	SET_KEYWORD,
	SET_DIRECTIVEDATA;
	
//--------------------------------------------------------------------------

	public static class JobEditTypeDeserializer 
	implements JsonDeserializer<JobEditType>
	{
		@Override
		public JobEditType deserialize(JsonElement json, 
				Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
			// JSON is case sensitive, but we want to
	    	// allow some flexibility on the case of the strings meant to represent
	    	// enums, so we allow case-insensitive string-like enums.
			return JobEditType.valueOf(json.getAsString().toUpperCase());
		}
	}
	
//--------------------------------------------------------------------------

}