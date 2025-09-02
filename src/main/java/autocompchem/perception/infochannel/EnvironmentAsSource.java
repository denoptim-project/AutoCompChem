package autocompchem.perception.infochannel;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.HashMap;

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

import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


/**
 * Class representing the use of the environmental variables as info source. 
 *
 * @author Marco Foscato
 */

public class EnvironmentAsSource extends ReadableIC
{
    /**
     * Environment
     */
    private Map<String,String> env;

//------------------------------------------------------------------------------

    /**
     * Constructor. Environment is detected already here.
     */

    public EnvironmentAsSource()
    {
        super();
        env = System.getenv();
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the map of environmental variables
     */

    public void setEnvironment(Map<String,String> env)
    {
    	this.env = env;
    }
    
//------------------------------------------------------------------------------

  	@Override
  	public boolean canBeRead() 
  	{
  		return env!=null && !env.isEmpty();
  	}

//------------------------------------------------------------------------------

    /**
     * Returns the Reader of the string source.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defined that the Reader is no longer 
     * needed.
     * @return a reader for reading the character-info from the source
     */

    public Reader getSourceReader()
    {
        StringBuilder sb = new StringBuilder();
        for (String k : env.keySet())
        {
            sb.append(k).append("=").append(env.get(k));
            sb.append(System.getProperty("line.separator"));
        }
        super.reader = new StringReader(sb.toString());
        return super.reader;
    }
    
//------------------------------------------------------------------------------

    public static class EnvironmentAsSourceSerializer 
    implements JsonSerializer<EnvironmentAsSource>
    {
        @Override
        public JsonElement serialize(EnvironmentAsSource eas, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONICIMPLEMENTATION, 
            		InfoChannelImplementation.valueOf(
            				eas.getClass().getSimpleName().toUpperCase())
            		.toString());
            jsonObject.addProperty(JSONINFOCHANNELTYPE, 
            		eas.getType().toString());
            
            jsonObject.add("env", context.serialize(eas.env));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class EnvironmentAsSourceDeserializer 
    implements JsonDeserializer<EnvironmentAsSource>
    {
        @Override
        public EnvironmentAsSource deserialize(JsonElement json, Type typeOfT,
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
            if (this.getClass().getSimpleName().toUpperCase().equals(
            		impl.toString()))
            {
            	String msg = "Cannot to deserialize '" + impl + "' into "
            			+ this.getClass().getSimpleName() + ".";
                throw new JsonParseException(msg);
            }

            InfoChannelType type = context.deserialize(
            		jsonObject.get(JSONINFOCHANNELTYPE), InfoChannelType.class);
            EnvironmentAsSource eas = new EnvironmentAsSource();
            eas.setType(type);
            Map<String,String> env = context.deserialize(
            		jsonObject.get("env"), Map.class);
            eas.setEnvironment(env);
            
            return eas;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("EnvironmentAsSource [ICType:").append(super.getType());
        sb.append("; text:").append(env);
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
           
        EnvironmentAsSource other = (EnvironmentAsSource) o;
           
        if (!this.env.equals(other.env))
            return false;
          
        return super.equals(other);
    }
      
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
   	    return Objects.hash(env, super.hashCode());
    }  

//------------------------------------------------------------------------------

}
