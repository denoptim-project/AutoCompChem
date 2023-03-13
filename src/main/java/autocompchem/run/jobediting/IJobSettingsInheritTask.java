package autocompchem.run.jobediting;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.run.Job;

/**
 * Interface for any task that wants to take settings from a job and copy them 
 * into another job.
 */
public interface IJobSettingsInheritTask 
{
	
	/**
	 * Copy settings from a sours to a target job.
	 * @param source the job from which settings are taken.
	 * @param destination the job receiving settings.
	 * @throws CloneNotSupportedException when the parameter cannot be cloned.
	 */
	public void inheritSettings(Job source, Job destination) 
			throws CloneNotSupportedException;
	
//------------------------------------------------------------------------------
	
	public static class IJobSettingsInheritTaskDeserializer 
	implements JsonDeserializer<IJobSettingsInheritTask>
	{
	    @Override
	    public IJobSettingsInheritTask deserialize(JsonElement json, 
	    		Type typeOfT, JsonDeserializationContext context) 
	    				throws JsonParseException
	    {
	        JsonObject jsonObject = json.getAsJsonObject();

	        JobEditType type = context.deserialize(jsonObject.get("task"),
	                JobEditType.class);
	        
	        IJobSettingsInheritTask result = null;
	        switch (type)
	        {
			case INHERIT_JOB_PARAMETER:
				result = context.deserialize(json, InheritJobParameter.class);
				break;
			case INHERIT_DIRECTIVE:
			case INHERIT_DIRECTIVEDATA:
			case INHERIT_KEYWORD:
				result = context.deserialize(json, InheritDirectiveComponent.class);
				break;
			default:
				throw new IllegalArgumentException("Job settings inheriting "
						+ "task '" + type + "' is not known. "
						+ "Cannot deserialize JSON element: " + json);
	        }
        	return result;
	    }
	}
	
//------------------------------------------------------------------------------
	
}
