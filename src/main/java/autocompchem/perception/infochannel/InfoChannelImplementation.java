package autocompchem.perception.infochannel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


public enum InfoChannelImplementation {
	FILEASSOURCE,
	SHORTTEXTASSOURCE,
	ENVIRONMENTASSOURCE,
	JOBDETAILSASSOURCE;
	
//--------------------------------------------------------------------------

	public static class InfoChannelImplementationDeserializer 
	implements JsonDeserializer<InfoChannelImplementation>
	{
		@Override
		public InfoChannelImplementation deserialize(JsonElement json, 
				Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
			// JSON is case sensitive, but we want to
	    	// allow some flexibility on the case of the strings meant to represent
	    	// enums, so we allow case-insensitive string-like enums.
			return InfoChannelImplementation.valueOf(json.getAsString().toUpperCase());
		}
	}
	
//--------------------------------------------------------------------------

}
