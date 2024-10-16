package autocompchem.perception.circumstance;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.JobDetailsAsSource;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.IDirectiveComponent;
import autocompchem.wiro.chem.IValueContainer;

/**
 * Condition satisfied if a directive component exists is a job's step. The 
 * existence is tested in terms of finding a component with the given
 * {@link DirComponentAddress}.
 *
 * @author Marco Foscato
 */

public class MatchDirComponent extends Circumstance implements IScoring
{
    /**
     * Address to match
     */
	public final DirComponentAddress address;

    /**
     * Index of the job's step to focus on, in any.
     */
    public final int stepId;

    /**
     * String representation of the value, if any, of the component at the given 
     * address.
     */
    public final String value;

    /**
     * Negation flag: if true then the condition is negated
     */
    public final boolean negation;

    
//------------------------------------------------------------------------------

    /**
     * Constructor for a circumstance that ignores any value at the 
     * {@link DirComponentAddress} sought after.
     * @param address the address of the directive component to be matched.
     */

    public MatchDirComponent(DirComponentAddress address)
    {
        this(address, null, 0, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a circumstance that ignores any value at the 
     * {@link DirComponentAddress} sought after.
     * @param address the address of the directive component to be matched.
     * @param negation if true the condition is satisfied if the address is
     * not found.
     */

    public MatchDirComponent(DirComponentAddress address, boolean negation)
    {
        this(address, null, 0, negation);
    }

//------------------------------------------------------------------------------
    /**
     * Constructor for a circumstance that considers the value hosted at the
     * {@link DirComponentAddress} sought after.
     * @param address the address of the directive component to be matched.
     * @param value the string representation of the value of the component to 
     * be matched. If this is null, the checking of the value is disabled.
     */

    public MatchDirComponent(DirComponentAddress address, String value)
    {
        this(address, value, 0, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a circumstance that considers the value hosted at the
     * {@link DirComponentAddress} sought after.
     * @param address the address of the directive component to be matched.
     * @param value the string representation of the value of the component to 
     * be matched. If this is null, the checking of the value is disabled.
     * @param negation if true the condition is satisfied if the address is
     * not found.
     */

    public MatchDirComponent(DirComponentAddress address, String value,
    		boolean negation)
    {
        this(address, value, 0, negation);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a circumstance that considers the value hosted at the
     * {@link DirComponentAddress} sought after.
     * @param address the address of the directive component to be matched.
     * @param value the string representation of the value of the component to 
     * be matched. If this is null, the checking of the value is disabled.
     * @param stepId the step number of the step in the job defined by the the
     * {@link InfoChannelType.JOBDETAILS} channel.
     * @param negation if true the condition is satisfied if the address is
     * not found.
     */

    public MatchDirComponent(DirComponentAddress address, String value, 
    		int stepId, boolean negation)
    {
        super(InfoChannelType.JOBDETAILS);
        this.stepId = stepId;
        this.address = address;
        this.value = value;
        this.negation = negation;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value between 0.0 and 1.0
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param job the job in which we search for the {@link DirComponentAddress}.
     * @return numerical score
     */

    public double calculateScore(InfoChannel input)
    {
    	if (input.getClass() != JobDetailsAsSource.class)
    		return 0;
    	CompChemJob job = (CompChemJob) ((JobDetailsAsSource) input).job;
    	
    	List<IDirectiveComponent> matches = job.getDirectiveComponents(address);
    	if (value != null)
	    {
    		List<IDirectiveComponent> toRemove = new ArrayList<>();
	    	for (IDirectiveComponent dc : matches)
	    	{
	    		if (dc instanceof IValueContainer)
	    		{
	    			String vStr = ((IValueContainer) dc).getValue().toString();
	    			if (!value.equals(vStr))
		    			toRemove.add(dc);
	    		} else {
	    			toRemove.add(dc);
	    		}
	    	}
	    	matches.removeAll(toRemove);
	    }
    	
        double score = 0.0;
        if (negation)
        {
            if (matches.size() == 0)
            {
                score = 1.0;
            }
        }
        else
        {
            //NB: here we can make the score dependent on #matches 
            if (matches.size() > 0)
            {
                score = 1.0;
            }
        }
        
        return score;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable representation
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MatchDirComponent [address:").append(address);
        sb.append("; value:").append(value);
        sb.append("; stepId:").append(stepId);
        sb.append("; negation:").append(negation);
        sb.append("]]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

  	@Override
  	public TreeMap<String, JsonElement> getJsonMembers(
			JsonSerializationContext context) 
	{
		TreeMap<String, JsonElement> map = new TreeMap<String, JsonElement>();
		map.putAll(super.getJsonMembers(context));
		map.put("address", context.serialize(address));
		map.put("value", context.serialize(value));
		map.put("stepId", context.serialize(stepId));
		if (negation)
			map.put("negation", context.serialize(negation));
		return map;
  	}
  	
//------------------------------------------------------------------------------

  	public static class MatchDirComponentSerializer 
  	implements JsonSerializer<MatchDirComponent>
  	{
  	    @Override
  	    public JsonElement serialize(MatchDirComponent src, Type typeOfSrc,
  	          JsonSerializationContext context)
  	    {
  	    	return ICircumstance.getJsonObject(src, context);
  	    }
  	}
  	
//------------------------------------------------------------------------------
  	
  	public static class MatchDirComponentDeserializer 
  	implements JsonDeserializer<MatchDirComponent>
  	{
  	    @Override
  	    public MatchDirComponent deserialize(JsonElement json, 
  	    		Type typeOfT, JsonDeserializationContext context) 
  	    				throws JsonParseException
  	    {
  	        JsonObject jsonObject = json.getAsJsonObject();
  	        
  	        DirComponentAddress address = context.deserialize(
  	        		jsonObject.get("address"), DirComponentAddress.class);
  	      
  	        String value = null;
  	        if (jsonObject.has("value"))
			{
  	            value = jsonObject.get("value").getAsString();
			}
  	        
			int stepId = 0;
			if (jsonObject.has("stepId"))
			{
				stepId = jsonObject.get("stepId").getAsInt();
			}
			boolean negation = false;
			if (jsonObject.has("negation"))
			{	
				negation = context.deserialize(jsonObject.get("negation"),
						Boolean.class);
			}
			return new MatchDirComponent(address, value, stepId, negation);
  	    }
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
         
        MatchDirComponent other = (MatchDirComponent) o;
         
        if (!this.address.equals(other.address))
            return false;        

        if (!this.value.equals(other.value))
            return false;
        
        if (this.stepId != other.stepId)
            return false;

        if ((this.negation && !other.negation) 
        		|| (!this.negation && other.negation))
        	return false;
        
        return super.equals(other);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(address, value, stepId, negation, super.hashCode());
    }

//------------------------------------------------------------------------------

}
