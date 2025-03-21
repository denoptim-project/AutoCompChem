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
 * Condition satisfied if a string is matched in a number of lines of a single 
 * source. The number of matches can be a constrained with minimum or maximum
 * or within/outside a range, or exactly equal to a number. Different
 * constructors are used to specify each of these constraints.
 *
 * @author Marco Foscato
 */

public class CountTextMatches extends MatchText
{
    /**
     * Minimum number of matches
     */
    private int min;

    /**
     * Max number of matches
     */
    private int max;

    /**
     * The chosen type of constraints to calculate the score
     */
    private ConstrainType cnstrType = ConstrainType.EXACT;

    /**
     * All the known types of constraints
     */
    private enum ConstrainType
    {
        MIN, MAX, RANGE, EXACT;
    } 

//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and  
     * a given number of matches required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param ict the type of information channel where to search for this loop
     * counter.
     * @param num the number of matches
     */

    public CountTextMatches(String pattern, int num, InfoChannelType ict)
    {
        this(pattern, num, ict, false);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * a given number of matches required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param num the number of matches
     * @param ict the type of information channel where to search for this loop
     * counter.
     * @param negation if true the condition is satisfied if the count is not
     * equal to the given number.
     */

    public CountTextMatches(String pattern, int num, InfoChannelType ict,
            boolean negation)
    {
        super(pattern, negation, ict);
        this.min = num;
        this.max = num;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * the min/max number of matches required to satisfy this circumstance.
     * Negation is not supported for this method.
     * @param pattern the pattern to be matches
     * @param minOrMax the numerical definition of the limited number of matches
     * found (the limit is always included in the acceptable range. I.e., we use
     * &le; or &ge;)
     * @param pol the polatiry, meaning is <code>true</code> minOrMax will be
     * used to set the minimum, otherwise is the maximum number of matches
     */

    public CountTextMatches(String pattern, int minOrMax, boolean pol, 
            InfoChannelType ict)
    {
        super(pattern,ict);
        if (pol)
        {
            this.min = minOrMax;
            this.cnstrType = ConstrainType.MIN;
        }
        else
        {
            this.max = minOrMax;
            this.cnstrType = ConstrainType.MAX;
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * the range (min and max, included) within which number of matches
     * required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param min the minimum number of matches (acceptable range is &ge; this)
     * @param max the maximum number of matches (acceptable range is &le; this)
     * @param channel the information channel where to search for this loop
     * counter.
     */

    public CountTextMatches(String pattern, int min, int max,
            InfoChannelType ict)
    {
        this(pattern, min, max, ict, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * the range (min and max, included) within which number of matches
     * required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param min the minimum number of matches (acceptable range is &ge; this)
     * @param max the maximum number of matches (acceptable range is &le; this)
     * @param channel the information channel where to search for this loop
     * counter.
     * @param negation if true the condition is satisfied if the count is 
     * outside the given range.
     */

    public CountTextMatches(String pattern, int min, int max,
            InfoChannelType ict, boolean negation)
    {
        super(pattern, negation, ict);
        if (min!=-1)
            this.min = min;
        if (max!=-1)
            this.max = max;
        this.cnstrType = ConstrainType.RANGE;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value 
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param matches the list of matches
     * @return numerical score
     */

    @Override
    public double calculateScore(List<String> matches)
    {
        double score = 0.0;
        int numMatches = matches.size();

        switch (cnstrType)
        {
            case MIN:
                if (numMatches >= min)
                {
                    score = 1.0;
                }
                break;

            case MAX:
                if (numMatches <= max)
                {
                    score = 1.0;
                }
                break;

            case RANGE:
                if ((numMatches >= min) && (numMatches <= max))
                {
                    score = 1.0;
                    if (negation)
                    {
                        score = 0.0;
                    }
                }
                else
                {
                    if (negation)
                    {
                        score = 1.0;
                    }
                }
                break;

            case EXACT:
                // NB: min and max are be the same at this point.
                if (numMatches == min)
                {
                    score = 1.0;
                    if (negation)
                    {
                        score = 1.0;
                    }
                }
                else
                {
                    if (negation)
                    {
                        score = 1.0;
                    }
                }
                break;

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
        sb.append("CountTextMatches [pattern:").append(super.getPattern());
        sb.append("; min: ").append(min);
        sb.append("; max: ").append(max);
        sb.append("; cnstrType: ").append(cnstrType);
        sb.append("; channel:").append(super.getChannelType());
        sb.append("; negation:").append(super.negation);
        sb.append("]]");
        return sb.toString();
    }
    
  //------------------------------------------------------------------------------

      @Override
      public TreeMap<String, JsonElement>  getJsonMembers(
            JsonSerializationContext context) 
    {
        TreeMap<String, JsonElement> map = new TreeMap<String, JsonElement>();
        map.putAll(super.getJsonMembers(context));
          switch (cnstrType) 
          {
        case EXACT:
            // NB: min==max
            map.put("value", context.serialize(min));
            break;
            
        case MAX:
            map.put("max", context.serialize(min));
            break;
            
        case MIN:
            map.put("min", context.serialize(min));
            break;
            
        case RANGE:
            map.put("min", context.serialize(min));
            map.put("max", context.serialize(max));
            break;
        }
        return map;
      }
      
//------------------------------------------------------------------------------

      public static class CountTextMatchesSerializer 
      implements JsonSerializer<CountTextMatches>
      {
          @Override
          public JsonElement serialize(CountTextMatches src, Type typeOfSrc,
                JsonSerializationContext context)
          {
              return ICircumstance.getJsonObject(src, context);
          }
      }
      
//------------------------------------------------------------------------------
      
      public static class CountTextMatchesDeserializer 
      implements JsonDeserializer<CountTextMatches>
      {
          @Override
          public CountTextMatches deserialize(JsonElement json, 
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
              if (jsonObject.has("value"))
              {
                  //EXACT
                  return new CountTextMatches(pattern,
                          jsonObject.get("value").getAsInt(),
                          ict, negation);
              } else {
                  if (jsonObject.has("min") && jsonObject.has("max"))
                  {
                      // RANGE
                      return new CountTextMatches(pattern,
                              jsonObject.get("min").getAsInt(),
                              jsonObject.get("max").getAsInt(),
                              ict, negation);
                  } else {
                      if (jsonObject.has("min"))
                      {
                          return new CountTextMatches(pattern,
                                  jsonObject.get("min").getAsInt(),
                                  true,
                                  ict);
                      } else {
                          //jsonObject.has("max") is true here
                          return new CountTextMatches(pattern,
                                  jsonObject.get("max").getAsInt(),
                                  false,
                                  ict);
                      }
                  }
              }
          }
      }
      
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o== null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
            return false;
         
        CountTextMatches other = (CountTextMatches) o;
         
        if (this.min != other.min)
            return false;
        
        if (this.max != other.max)
           return false;
        
        if (!this.cnstrType.equals(other.cnstrType))
            return false;
        
        return super.equals(other);
    }
    
 //-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(min, max, cnstrType, super.hashCode());
    }

//------------------------------------------------------------------------------

}
