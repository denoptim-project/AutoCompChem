package autocompchem.perception.circumstance;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.perception.infochannel.DataAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.utils.StringUtils;

/**
 * Condition satisfied if data respects some specific criterion.
 * <p>The location of the data to assess is defined
 * by a given data path, which is a comma-separated list of strings that identify how
 * to navigate the data structure of the {@link NamedDataCollector} identified by the
 * {@link DataAsSource} info channel. Only info channels pointing to the same
 * data path as this {@link Circumstance} will be considered for assessment.
 * </p>
 * <p>The criterion for the assessment can be defined in the following ways:<ul>
 * 
 * <li><b>Expression Language (EL)</b>: using an expression formulated according to the 
 * Java Expression Language, i.e., "${...}" to define the content of the 
 * expression to be evaluated, and using placeholder "DATA", which is replaced by the
 * value of the data fetched from the {@link DataAsSource} info channel. 
 * For example, the equation "${DATA > 0}" will be evaluated to <code>true</code> if the data
 * value is greater than 0, and to <code>false</code> otherwise.</li>
 *
 * </ul></p>
 * 
 * <p>The assessment is considered satisfied if the equation evaluates to true. 
 * The negation flag can be used to invert the assessment, i.e., to consider the
 * assessment satisfied if the equation evaluates to false.
 * </p>
 */

public class AssessData extends Circumstance implements IScoring
{	
    /**
     * Data path to assess.
     */
    private final String dataPath;

    /**
     * Criteria to assess the data against.
     */
    private final String criteria;

    /**
     * Negation flag: if true then the condition is negated
     */
    protected final boolean negation;

//------------------------------------------------------------------------------

    /**
     * Constructs a AssessData defining the data path to assess and the criteria 
     * to assess it against.
     * @param dataPath the data path to assess.
     * @param criteria the criteria to assess the data against.
     */

    public AssessData(String dataPath, String criteria)
    {
        this(dataPath, criteria, false);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a AssessData.
     * @param dataPath the data path to assess.
     * @param criteria the criteria to assess the data against.
     * @param negation if true the condition is satisfied if the criteria is
     * not met.
     */

    public AssessData(String dataPath, String criteria, boolean negation)
    {
        super(InfoChannelType.DATA);
        this.dataPath = dataPath;
        this.criteria = criteria;
        this.negation = negation;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the data path
     * @return the data path
     */
    public String getDataPath()
    {
        return dataPath;
    }

//------------------------------------------------------------------------------

    /**
     * Assess the data against the criteria.
     * @param data the data to assess.
     * @return true if the data satisfies the criteria, false otherwise.
     */
    public boolean assessData(Object data)
    {
        String result = criteria.replace("DATA", data.toString());
        result = StringUtils.evaluateEmbeddedExpressionsInString(result);
        return Boolean.parseBoolean(result);
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value between 0.0 and 1.0
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param data the {@link InfoChannel} providing the data to assess.
     * @return numerical score.
     */

    @Override
    public double calculateScore(InfoChannel input)
    {
        if (input.getClass() != DataAsSource.class)
            return 0;
        DataAsSource das = (DataAsSource) input;

        if (!das.getDataPath().equals(dataPath))
            return 0;

        double score = 0.0;
        boolean assessment = assessData(das.getData());

        if ((negation && !assessment) || (!negation && assessment))
        {
            score = 1.0;
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
        sb.append(this.getClass().getSimpleName());
        sb.append(" [dataPath:").append(dataPath);
        sb.append("; criteria:").append(criteria);
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
		map.put("dataPath", context.serialize(dataPath));
		map.put("criteria", context.serialize(criteria));
		if (negation)
			map.put("negation", context.serialize(negation));
		return map;
  	}
  	
//------------------------------------------------------------------------------

  	public static class AssessDataSerializer 
  	implements JsonSerializer<AssessData>
  	{
  	    @Override
  	    public JsonElement serialize(AssessData src, Type typeOfSrc,
  	          JsonSerializationContext context)
  	    {
  	    	return ICircumstance.getJsonObject(src, context);
  	    }
  	}
  	
//------------------------------------------------------------------------------
  	
  	public static class AssessDataDeserializer 
  	implements JsonDeserializer<AssessData>
  	{
  	    @Override
  	    public AssessData deserialize(JsonElement json, 
  	    		Type typeOfT, JsonDeserializationContext context) 
  	    				throws JsonParseException
  	    {
  	        JsonObject jsonObject = json.getAsJsonObject();

			String dataPath = jsonObject.get("dataPath").getAsString();
			String criteria = jsonObject.get("criteria").getAsString();
			boolean negation = false;
			if (jsonObject.has("negation"))
			{	
				negation = context.deserialize(jsonObject.get("negation"),
						Boolean.class);
			}
			return new AssessData(dataPath, criteria, negation);
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
         
        AssessData other = (AssessData) o;
         
        if (!this.dataPath.equals(other.dataPath))
            return false;

        if (!this.criteria.equals(other.criteria))
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
    	return Objects.hash(dataPath, criteria, negation, super.hashCode());
    }

//------------------------------------------------------------------------------

}
