package autocompchem.run.jobediting;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;

/**
 * Interface for anything that wants to be able to edit jobs of any type.
 */
public interface IJobEditingTask 
{	
	/**
	 * Applies this editing task to the given job.
	 * @param job the job to edit.
	 */
	public void applyChange(Job job);

//------------------------------------------------------------------------------

	//TODO-gg remove. this is not used! see also IInherit...
	public static class IJobEditingTaskSerializer 
	implements JsonSerializer<IJobEditingTask>
	{
	    @Override
	    public JsonElement serialize(IJobEditingTask src, Type typeOfSrc,
	          JsonSerializationContext context)
	    {
	    	return context.serialize(src, src.getClass());
	    }
	}
	
//------------------------------------------------------------------------------
	
	public static class IJobEditingTaskDeserializer 
	implements JsonDeserializer<IJobEditingTask>
	{
	    @Override
	    public IJobEditingTask deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context) throws JsonParseException
	    {
	        JsonObject jsonObject = json.getAsJsonObject();

	        TaskType type = context.deserialize(jsonObject.get("task"),
	                TaskType.class);
	        
	        
	        
	        
	        //TODO-gg add other types with their test and the rests
	        
	        
	        
	        
	        IJobEditingTask result = null;
	        switch (type)
	        {
			case REMOVE_JOB_PARAMETER:
				result = context.deserialize(json, DeleteJobParameter.class);
				break;
			case SET_JOB_PARAMETER:
				result = context.deserialize(json, SetJobParameter.class);
				break;
			case SET_KEYWORD:
			case SET_DIRECTIVE:
			case SET_DIRECTIVEDATA:
				result = context.deserialize(json, SetDirectiveComponent.class);
				break;
			case REMOVE_KEYWORD:
			case REMOVE_DIRECTIVE:
			case REMOVE_DIRECTIVEDATA:
				result = context.deserialize(json, DeleteDirectiveComponent.class);
				break;
			default:
				throw new IllegalArgumentException("Job editing task '" 
        				+ type + "' is not known. Cannot deserialize JSON "
        				+ "element: " + json);
	        }
        	return result;
	    }
	}
	
//------------------------------------------------------------------------------

}
