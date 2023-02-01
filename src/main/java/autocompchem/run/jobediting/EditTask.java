package autocompchem.run.jobediting;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Directive;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.ACCJob;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.MonitoringJob;
import autocompchem.run.ShellJob;

/**
 * Base class for any task wanting to edit jobs.
 */
public abstract class EditTask implements IChangesSettings
{
	public static enum TaskType {SET, DELETE, INHERIT}

	/**
	 * The thing to edit
	 */
	final Object target;
	
	/**
	 * The type of editing task
	 */
	private final TaskType task;
	

//------------------------------------------------------------------------------
	
	/**
	 * Constructor for an editing task that changes a specific job feature by 
	 * assigning a given value to it.
	 * @param target pointer to the thing to edit. This is not a reference, but
	 * a way to find the object to edit.
	 * @param task the type of editing task
	 */
	public EditTask(Object target, TaskType task)
	{
		this.target = target;
		this.task = task;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Returns the string pointing at the target in a human readable manner,
	 * easy to parse, and compatible with serialization in JSON format.
	 * @return the string 
	 */
	public abstract String getTargetPointerInJSON();
	
//------------------------------------------------------------------------------

	/**
	 * Returns the string used in JSON as the element name for providing the
	 * pointing at the target.
	 * @return the string 
	 */
	public abstract String getTargetElementInJSON();
	
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
    	if (o == null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	    
 	    EditTask other = (EditTask) o;
 	   
 	    if (!this.target.equals(other.target))
 	    	return false;
 	    
 	    if (!this.task.equals(other.task))
 	    	return false;
 	    
 	    return true;
    }
	  
//------------------------------------------------------------------------------

	public static class EditTaskSerializer 
	implements JsonSerializer<EditTask>
	{
	    @Override
	    public JsonElement serialize(EditTask et, Type typeOfSrc,
	          JsonSerializationContext context)
	    {
	        JsonObject jsonObject = new JsonObject();
	
	        jsonObject.addProperty("task", et.task.toString());
	        jsonObject.addProperty(et.getTargetElementInJSON(), 
	        		et.getTargetPointerInJSON());
	
	        return jsonObject;
	    }
	}
	
//------------------------------------------------------------------------------
	
	public static class EditTaskDeserializer 
	implements JsonDeserializer<EditTask>
	{
	    @Override
	    public EditTask deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context) throws JsonParseException
	    {
	        JsonObject jsonObject = json.getAsJsonObject();

	        TaskType type = context.deserialize(jsonObject.get("task"),
	                TaskType.class);
	        
	        NamedData newValue = null;
        	if (jsonObject.has("newValue"))
        	{
        		newValue = context.deserialize(jsonObject.get("target"),
    	                NamedData.class);
        	}
	        
	        DirComponentAddress targetPath = null;
	        if (jsonObject.has(DirComponentEditTask.TARGETELMINJSON))
        	{
	        	targetPath = DirComponentAddress.fromString(context.deserialize(
	        			jsonObject.get(DirComponentEditTask.TARGETELMINJSON), 
	        			String.class));
        	}
	        
	        String targetPar = null;
	        if (jsonObject.has(JobParameterEditTask.TARGETELMINJSON))
        	{
	        	targetPar = context.deserialize(jsonObject.get(
	        			JobParameterEditTask.TARGETELMINJSON), String.class);
	        	
        	}
	        
        	EditTask et = null;
        	if (type==TaskType.SET)
        	{
        		if (targetPar!=null)
        			et = new SetJobParameter(targetPar, newValue);
        		else if (targetPath!=null)
        			et = new SetDirComponentValue(targetPath, newValue);
        		//TODO-gg et = new SetDirComponentParameter(targetPath, targetStr, newValue);
        	} else if (type==TaskType.DELETE)
        	{
        		if (targetPar!=null)
        			et = new DeleteJobParameter(targetPar);
        		//TODO-gg
        	} else if (type==TaskType.INHERIT)
        	{
        		//TODO-gg
        	} else {
        		throw new IllegalArgumentException("Job editing task '" 
        				+ type + "' is not known. Cannot deserialize JSON "
        				+ "element: " + json);
        	}
        	return et;
	    }
	}

//------------------------------------------------------------------------------

}
