package autocompchem.run.jobediting;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import autocompchem.run.Job;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.IDirectiveComponent;
import autocompchem.wiro.chem.Keyword;

/**
 * Task setting a {@link IDirectiveComponent} somewhere in the 
 * {@link Directive}s' structure of a {@link CompChemJob}. 
 * Note the difference between
 * {@link Job}'s parameters and {@link CompChemJob}'s {@link Directive}s and 
 * their components.
 */

public class AddDirectiveComponent implements IJobEditingTask
{
	/**
	 * Defines which type of setting task this is. It also defines what is
	 * the type of content this task is setting.
	 */
	protected final JobEditType task;
	
	/**
	 * Address to parent directive where the component is to be set
	 */
	protected final DirComponentAddress path;
	
	/**
	 * The content to set for the directive component.
	 */
	protected final IDirectiveComponent content;
	
//------------------------------------------------------------------------------

	public AddDirectiveComponent(DirComponentAddress parent, 
			IDirectiveComponent content)
	{
		this(parent, content, defineTask(content));
	}
	
//------------------------------------------------------------------------------

	public AddDirectiveComponent(String pathToParent, 
			IDirectiveComponent content)
	{
		this(DirComponentAddress.fromString(pathToParent), content, 
				defineTask(content));
	}
	
//------------------------------------------------------------------------------

	public AddDirectiveComponent(DirComponentAddress addressToParent, 
			IDirectiveComponent content, JobEditType type)
	{
		this.path = addressToParent;
		this.content = content;
		this.task = type;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Sets the task identifier according to the type of content given.
	 */
	private static JobEditType defineTask(IDirectiveComponent content)
	{
		if (content instanceof Directive)
			return JobEditType.ADD_DIRECTIVE;
		else if (content instanceof Keyword)
			return JobEditType.ADD_KEYWORD;
		else if (content instanceof DirectiveData)
			return JobEditType.ADD_DIRECTIVEDATA;
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
 	    
 	    AddDirectiveComponent other = (AddDirectiveComponent) o;
 	    
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
		ccj.addDirectiveComponent(path, content, false, true);
	}
	
//------------------------------------------------------------------------------
	
	public static class AddDirectiveComponentDeserializer 
	implements JsonDeserializer<AddDirectiveComponent>
	{
	    @Override
	    public AddDirectiveComponent deserialize(JsonElement json, Type typeOfT,
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
			case ADD_DIRECTIVE:
				content = context.deserialize(jsonObject.get("content"), 
						Directive.class);
				break;
			case ADD_DIRECTIVEDATA:
				content = context.deserialize(jsonObject.get("content"), 
						DirectiveData.class);
				break;
			case ADD_KEYWORD:
				content = context.deserialize(jsonObject.get("content"), 
						Keyword.class);
				break;
			default:
				throw new IllegalArgumentException("Job editing task '" 
        				+ type + "' is not known. Cannot deserialize JSON "
        				+ "element: " + json);
	        }
	        
	        AddDirectiveComponent result = new AddDirectiveComponent(
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
