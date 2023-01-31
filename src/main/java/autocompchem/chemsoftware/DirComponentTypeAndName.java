package autocompchem.chemsoftware;

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

import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.ACCJob;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.JobEditTask;
import autocompchem.run.MonitoringJob;
import autocompchem.run.ShellJob;

/**
 * The pair of two bits of info: the type and the reference name of a directive
 * component. This class does not contain any reference to an actual instance of
 * directive component, but it represents the possibility to have a directive 
 * component with the specified name and type. Note that such component might
 * not exist.
 */
public class DirComponentTypeAndName 
{
	/**
	 * The type of the component
	 */
	public final DirectiveComponentType type;
	
	/**
	 * The reference name of the component
	 */
	public final String name;

//------------------------------------------------------------------------------
	
	public DirComponentTypeAndName(String name, DirectiveComponentType type)
	{
		this.type = type;
		this.name = name;
	}
	
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
 	    
 	    DirComponentTypeAndName other = (DirComponentTypeAndName) o;
 	   
 	    return this.name.equals(other.name) 
 	    		&& this.type.equals(other.type);
	}
	
//------------------------------------------------------------------------------
	
}
