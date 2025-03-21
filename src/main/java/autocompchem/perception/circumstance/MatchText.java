package autocompchem.perception.circumstance;

import java.lang.reflect.Type;
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

import autocompchem.perception.infochannel.InfoChannelType;

/**
 * Condition satisfied if a string is matched.
 *
 * @author Marco Foscato
 */

public class MatchText extends Circumstance
{	
    /**
     * Pattern to match
     */
    private final String pattern;

    /**
     * Negation flag: if true then the condition is negated
     */
    protected final boolean negation;

//------------------------------------------------------------------------------

    /**
     * Constructs a MatchText defining the pattern to match and the information 
     * channel where to search for it.
     * @param pattern the pattern to be matched.
     * @param ict the type of information channel where to search for this loop
     * counter.
     */

    public MatchText(String pattern, InfoChannelType ict)
    {
        this(pattern, false, ict);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a MatchText.
     * @param pattern the pattern to be matches
     * @param negation if true the condition is satisfied if the pattern is
     * not matched.
     * @param channel the information channel where to search for this loop
     * counter.
     */

    public MatchText(String pattern, boolean negation, InfoChannelType channel)
    {
        super(channel);
        this.pattern = pattern;
        this.negation = negation;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the pattern
     * @return the pattern
     */
    public String getPattern()
    {
        return pattern;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value between 0.0 and 1.0
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param matches the list of matches in the info channel.
     * @return numerical score.
     */

    public double calculateScore(List<String> matches)
    {
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
        sb.append("MatchText [pattern:").append(pattern);
        sb.append("; channel:").append(super.getChannelType());
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
		map.put("pattern", context.serialize(pattern));
		if (negation)
			map.put("negation", context.serialize(negation));
		return map;
  	}
  	
//------------------------------------------------------------------------------

  	public static class MatchTextSerializer 
  	implements JsonSerializer<MatchText>
  	{
  	    @Override
  	    public JsonElement serialize(MatchText src, Type typeOfSrc,
  	          JsonSerializationContext context)
  	    {
  	    	return ICircumstance.getJsonObject(src, context);
  	    }
  	}
  	
//------------------------------------------------------------------------------
  	
  	public static class MatchTextDeserializer 
  	implements JsonDeserializer<MatchText>
  	{
  	    @Override
  	    public MatchText deserialize(JsonElement json, 
  	    		Type typeOfT, JsonDeserializationContext context) 
  	    				throws JsonParseException
  	    {
  	        JsonObject jsonObject = json.getAsJsonObject();

  	        InfoChannelType ict = context.deserialize(jsonObject.get("channel"),
  	        		InfoChannelType.class);
  	        
			String pattern = jsonObject.get("pattern").getAsString();
			boolean negation = false;
			if (jsonObject.has("negation"))
			{	
				negation = context.deserialize(jsonObject.get("negation"),
						Boolean.class);
			}
			return new MatchText(pattern, negation, ict);
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
         
        MatchText other = (MatchText) o;
         
        if (!this.pattern.equals(other.pattern))
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
    	return Objects.hash(pattern, negation, super.hashCode());
    }

//------------------------------------------------------------------------------

}
