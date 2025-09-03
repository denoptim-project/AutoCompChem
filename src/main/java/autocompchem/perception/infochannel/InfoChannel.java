package autocompchem.perception.infochannel;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import autocompchem.io.ACCJson;
import autocompchem.perception.situation.Situation;

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
 * Abstract class representing any kind of information source.
 *
 * @author Marco Foscato
 */

public abstract class InfoChannel implements Cloneable
{
	/**
	 * JSON name to identify subclass implementation
	 */
	public static final String JSONICIMPLEMENTATION = "Implementation";
	
	/**
	 * JSON name to identify subclass implementation
	 */
	public static final String JSONINFOCHANNELTYPE = "infoChannelType";

    /**
     * The type of information channel
     */
     private InfoChannelType type;

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public InfoChannel()
    {
        type = InfoChannelType.NONE;
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of information channel
     * @return the type of channel
     */

    public InfoChannelType getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Set the type of this information channel
     * @param ict the type of this information channel
     */

    public void setType(InfoChannelType ict)
    {
        type = ict;
    }
    
//------------------------------------------------------------------------------

    public static class InfoChannelDeserializer 
    implements JsonDeserializer<InfoChannel>
    {
        @Override
        public InfoChannel deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();
            
            if (!jsonObject.has(JSONICIMPLEMENTATION))
            {
                String msg = "Missing '" + JSONICIMPLEMENTATION + "': found a "
                        + "JSON string that cannot be converted into any "
                        + "InfoChannel subclass.";
                throw new JsonParseException(msg);
            }   

            InfoChannelImplementation impl = context.deserialize(
            		jsonObject.get(JSONICIMPLEMENTATION),
            		InfoChannelImplementation.class);
            
            InfoChannel ic = null;
            switch (impl)
            {
                case FILEASSOURCE:
                	ic = context.deserialize(jsonObject, FileAsSource.class);
                	break;
                
		        case JOBDETAILSASSOURCE:
                	ic = context.deserialize(jsonObject, JobDetailsAsSource.class);
		        	break;
		        
			    case ENVIRONMENTASSOURCE:
                	ic = context.deserialize(jsonObject, EnvironmentAsSource.class);
			    	break;
			    
				case SHORTTEXTASSOURCE:
                	ic = context.deserialize(jsonObject, ShortTextAsSource.class);
					break;

				default:
					throw new IllegalArgumentException("InfoChannel "
							+ "implementation '" + impl + "' is not known. "
							+ "Cannot deserialize JSON element: " + json);
            }
        	return ic;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("InfoChannel [type:").append(type);
        sb.append("]");
        return sb.toString();
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
         
        InfoChannel other = (InfoChannel) o;
         
        if (!this.type.equals(other.type))
            return false;
        
        return true;
    }
  	
//-----------------------------------------------------------------------------

  	@Override
  	public InfoChannel clone()
  	{
  		// Shortcut via json serialization to avoid implementing Cloneable
  		// in all implementations of ICircumstance
  		InfoChannel clone = ACCJson.getReader().fromJson(
  				ACCJson.getWriter().toJson(this), InfoChannel.class);
  		return clone;
  	}
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(this.getClass(), type);
    }

//------------------------------------------------------------------------------

}
