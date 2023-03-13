package autocompchem.perception.infochannel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

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

/**
 * Definition of information channel types. This information is used to group
 * the channels, but it does not define the type or source (i.e., file vs 
 * environment vs network vs stdin)
 *
 * @author Marco Foscato
 */

public enum InfoChannelType
{
        /** Undefined channel */
        NOTDEFINED,

        /** No channel */
        NONE,

        /** Any channel (wildcard) */
        ANY,

        /** Input file */
        INPUTFILE,

        /** Log file */
        LOGFEED,

        /** Output file */
        OUTPUTFILE,

        /** Job details/parameters */
        JOBDETAILS,

        /** Environment */
        ENVIRONMENT,

        /** Real life time */
        WALLTIME;
    
//--------------------------------------------------------------------------
  	
  	public static class InfoChannelTypeDeserializer 
  	implements JsonDeserializer<InfoChannelType>
  	{
		@Override
		public InfoChannelType deserialize(JsonElement json, 
				Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
			// JSON is case sensitive, but we want to
	    	// allow some flexibility on the case of the strings meant to represent
	    	// enums, so we allow case-insensitive string-like enums.
			return InfoChannelType.valueOf(json.getAsString().toUpperCase());
		}
  	}
  
//--------------------------------------------------------------------------

}
