package autocompchem.run.jobediting;

import java.lang.reflect.Type;
import java.util.ArrayList;
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
import autocompchem.chemsoftware.IValueContainer;
import autocompchem.chemsoftware.Keyword;
import autocompchem.run.Job;
import autocompchem.utils.NumberUtils;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

/**
 * Task setting a {@link IDirectiveComponent} somewhere in the 
 * {@link Directive}s' structure of a {@link CompChemJob}. 
 * Note the difference between
 * {@link Job}'s parameters and {@link CompChemJob}'s {@link Directive}s and 
 * their components.
 */

public class SetDirectiveComponent extends AddDirectiveComponent
{	
	
//------------------------------------------------------------------------------

	public SetDirectiveComponent(DirComponentAddress parent, 
			IDirectiveComponent content)
	{
		super(parent, content, defineTask(content));
	}
	
//------------------------------------------------------------------------------

	public SetDirectiveComponent(String pathToParent, 
			IDirectiveComponent content)
	{
		super(DirComponentAddress.fromString(pathToParent), content, 
				defineTask(content));
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Sets the task identifier according to the type of content given.
	 */
	private static JobEditType defineTask(IDirectiveComponent content)
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
 	    
 	    return super.equals(o);
    }
    
//------------------------------------------------------------------------------
	
	@Override
	public void applyChange(Job job) 
	{
		if (!(job instanceof CompChemJob))
			return;
		CompChemJob ccj = (CompChemJob) job;
		
		// Deal with any previous component at the given address
		DirComponentAddress pathToComponent = path.clone();
		pathToComponent.addStep(content.getName(), content.getComponentType());
		
		// Define a function to alter existing values
		boolean alterExisting = false;
		String expr = "";
		if (content instanceof IValueContainer)
		{
			expr = ((IValueContainer) content).getValue().toString();
			if (expr.startsWith("${") && expr.endsWith("}"))
			{
				alterExisting = true;
			}
		}
		
		if (alterExisting)
		{
			ExpressionFactory expFact = ExpressionFactory.newInstance();
			for (IDirectiveComponent comp : ccj.getDirectiveComponents(
					pathToComponent))
			{
				// Get previous value 
				//WARNING: we assume it is parseable to a double
				if (!(comp instanceof IValueContainer))
					continue;
				IValueContainer compWithVal = (IValueContainer) comp;
				String oldStr = compWithVal.getValue().toString();
				String newVal = NumberUtils.calculateNewValueWithUnits(expr, 
						expFact, oldStr);
				
				// reassign to dir component
				compWithVal.setValue(newVal);
			}
		} else {
			// Remove previous
			ccj.removeDirectiveComponent(pathToComponent);
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
	    			dir.addComponent(content);
	    		}
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
