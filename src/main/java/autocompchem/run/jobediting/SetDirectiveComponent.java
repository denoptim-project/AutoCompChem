package autocompchem.run.jobediting;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.IDirectiveComponent;
import autocompchem.chemsoftware.Keyword;
import autocompchem.run.Job;

/**
 * Task setting a {@link IDirectiveComponent} somewhere in the 
 * {@link Directive}s' structure of a {@link CompChemJob}. 
 * Note the difference between
 * {@link Job}'s parameters and {@link CompChemJob}'s {@link Directive}s and 
 * their components.
 */

public class SetDirectiveComponent implements IJobEditingTask
{
	/**
	 * Defines which type of setting task this is. It also defines what is
	 * the type of content this task is setting.
	 */
	final JobEditType task;
	
	/**
	 * Address to parent directive where the component is to be set
	 */
	private final DirComponentAddress path;
	
	/**
	 * The content to set for the directive component.
	 */
	final IDirectiveComponent content;
	
//------------------------------------------------------------------------------

	public SetDirectiveComponent(DirComponentAddress parent, 
			IDirectiveComponent content)
	{
		this.path = parent;
		this.content = content;
		this.task = defineTask();
	}
	
//------------------------------------------------------------------------------

	public SetDirectiveComponent(String pathToParent, 
			IDirectiveComponent content)
	{
		this.path = DirComponentAddress.fromString(pathToParent);
		this.content = content;
		this.task = defineTask();
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Sets the task identifier according to the type of content given.
	 */
	private JobEditType defineTask()
	{
		if (content instanceof Directive)
			return JobEditType.SET_DIRECTIVE;
		else if (content instanceof Keyword)
			return JobEditType.SET_KEYWORD;
		else if (content instanceof DirectiveData)
			return JobEditType.SET_DIRECTIVEDATA;
		else
			throw new Error("Unrecognized type of directive "
					+ "component to set. Please, contact the developers.");
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
 	    
 	    SetDirectiveComponent other = (SetDirectiveComponent) o;
 	    
 	    if (this.content.getClass() != other.content.getClass())
    		return false;
 	    
 	    if (this.content instanceof Directive)
 	    {
 	    	Directive thisContent = (Directive) this.content;
 	    	Directive otherContent = (Directive) other.content;
 	    	if (!thisContent.equals(otherContent))
 	    		return false;
 	    } else if (this.content instanceof Keyword)
 	    {
 	    	Keyword thisContent = (Keyword) this.content;
 	    	Keyword otherContent = (Keyword) other.content;
 	    	if (!thisContent.equals(otherContent))
 	    		return false;
 	    } else if (this.content instanceof DirectiveData)
 	    {
 	    	DirectiveData thisContent = (DirectiveData) this.content;
 	    	DirectiveData otherContent = (DirectiveData) other.content;
 	    	if (!thisContent.equals(otherContent))
 	    		return false;
 	    } else {
 	    	throw new Error("Missing implementation for content of type "
 	    			+ this.content.getClass().getName() + " in " 
 	    			+ this.getClass().getName() + ".equals(). Please, "
 	    			+ "report this to the developers.");
 	    }
 	    
 	    return this.path.equals(other.path);
    }
    
//------------------------------------------------------------------------------
	
	@Override
	public void applyChange(Job job) 
	{
		if (!(job instanceof CompChemJob))
			return;
		CompChemJob ccj = (CompChemJob) job;
		ccj.ensureDirectiveStructure(path);
		if (path.size()==0)
		{
			// We add a root directive: an outermost one.
			ccj.addDirective((Directive) content);
		}
		List<IDirectiveComponent> parents = ccj.getDirectiveComponents(
    			path);
    	for (IDirectiveComponent parent : parents)
    	{
    		if (parent instanceof Directive)
    		{
    			Directive dir = (Directive) parent;
    			dir.setComponent(content);

				//TODO-gg make optional by extending this class to have the
    			// APPEND_COMPONENT task

    			//TODO-gg use addComponent!!!!
    		}
    	}
	}
	
//------------------------------------------------------------------------------
	
	public static class SetDirectiveComponentDeserializer 
	implements JsonDeserializer<SetDirectiveComponent>
	{
	    @Override
	    public SetDirectiveComponent deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context) throws JsonParseException
	    {
	        JsonObject jsonObject = json.getAsJsonObject();

	        JobEditType type = context.deserialize(jsonObject.get("task"),
	                JobEditType.class);
	        
	        DirComponentAddress address = context.deserialize(
	        		jsonObject.get("path"), 
	        		DirComponentAddress.class);
	        
	        IDirectiveComponent content = null;
	        switch (type)
	        {
			case SET_DIRECTIVE:
				content = context.deserialize(jsonObject.get("content"), 
						Directive.class);
				break;
			case SET_DIRECTIVEDATA:
				content = context.deserialize(jsonObject.get("content"), 
						DirectiveData.class);
				break;
			case SET_KEYWORD:
				content = context.deserialize(jsonObject.get("content"), 
						Keyword.class);
				break;
			default:
				throw new IllegalArgumentException("Job editing task '" 
        				+ type + "' is not known. Cannot deserialize JSON "
        				+ "element: " + json);
	        }
	        
	        SetDirectiveComponent result = new SetDirectiveComponent(
	        		address, content);
        	return result;
	    }
	}
	
//------------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return task + " " + path + " " + content;
	}
   
//------------------------------------------------------------------------------
	
}
