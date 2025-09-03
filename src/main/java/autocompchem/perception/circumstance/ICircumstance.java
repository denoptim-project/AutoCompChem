package autocompchem.perception.circumstance;

/*
 *   Copyright (C) 2018  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;

/**
 * Interface for circumstance.
 *
 * @author Marco Foscato
 */

public interface ICircumstance
{

//------------------------------------------------------------------------------

    /**
     * Return the data channel, or feed, this circumstance relates to
     * @return the data channel
     */

    public InfoChannelType getChannelType();
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the type-specific elements to add to the JSON 
     * representation of a ICircumstance.
     */
    public TreeMap<String, JsonElement> getJsonMembers(
    		JsonSerializationContext context);
    
//------------------------------------------------------------------------------

    //TODO
    /**
     * Convert a score from numeric to boolean. Uses a threshold that can be set
     * by method ___TODO:write name___.
     * @param dScore the score in numeric double
     * @return a true/false value
     */

    public boolean scoreToDecision(double dScore);
  	    
//------------------------------------------------------------------------------
	
  	/**
  	 * Constructs the JSON object of implementations of {@link ICircumstance}.
  	 * This method is meant for Type-specific JSON serializers for
  	 * types implementing ICircumstance.
  	 * @param src the instance to serialize.
  	 * @param context 
  	 * @return the JSON object that a 
  	 * {@link JsonSerializer#serialize(Object, Type, JsonSerializationContext)}
  	 * can return.
  	 */
  	public static JsonObject getJsonObject(ICircumstance src, 
  			JsonSerializationContext context)
  	{
      	JsonObject jsonObject = new JsonObject();
      	jsonObject.addProperty("circumstance", 
      			src.getClass().getSimpleName());
      	for (Entry<String, JsonElement> e : src.getJsonMembers(
      			context).entrySet())
      	{
      		jsonObject.add(e.getKey(), e.getValue());
      	}
      	return jsonObject;
  	}
  	
//------------------------------------------------------------------------------
  	
  	public static class ICircumstanceDeserializer 
  	implements JsonDeserializer<ICircumstance>
  	{
  	    @Override
  	    public ICircumstance deserialize(JsonElement json, 
  	    		Type typeOfT, JsonDeserializationContext context) 
  	    				throws JsonParseException
  	    {
  	        JsonObject jsonObject = json.getAsJsonObject();

  	        String type = context.deserialize(jsonObject.get("circumstance"),
  	                String.class);
  	        
  	        ICircumstance result = null;
  	        switch (type)
  	        {
  			case "MatchText":
  				result = context.deserialize(jsonObject, MatchText.class);
  				break;
  				
  			case "CountTextMatches":
  				result = context.deserialize(jsonObject, CountTextMatches.class);
  				break;
  				
  			case "MatchDirComponent":
  				result = context.deserialize(jsonObject, MatchDirComponent.class);
  				break;
  				
  			//TODO: this needs to include all the classes implementing this interface
  			// or search for class in classpath.
  			
  			default:
  				throw new IllegalArgumentException("Job settings inheriting "
  						+ "task '" + type + "' is not known. "
  						+ "Cannot deserialize JSON element: " + json);
  	        }
          	return result;
  	    }
  	}
  	
//------------------------------------------------------------------------------

}
