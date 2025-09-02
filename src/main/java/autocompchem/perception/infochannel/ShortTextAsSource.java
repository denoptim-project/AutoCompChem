package autocompchem.perception.infochannel;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;

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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;


/**
 * Class representing a few lines of text to be used as information source. 
 * The text is meant to be small enough that it is convenient
 * or useful to keep it in an ArrayList.
 *
 * @author Marco Foscato
 */

public class ShortTextAsSource extends ReadableIC
{
    /**
     * Text organized by lines
     */
    private List<String> txt = new ArrayList<String>();

//------------------------------------------------------------------------------

    /**
     * Constructs an empty ShortTextAsSource
     */

    public ShortTextAsSource()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a ShortTextAsSource with one line of text
     * @param line the text
     */

    public ShortTextAsSource(String line)
    {
        super();
        this.txt = new ArrayList<String>(Arrays.asList(line));
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a ShortTextAsSource and specify the text
     */

    public ShortTextAsSource(List<String> txt)
    {
        super();
        this.txt = txt;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the Reader of the string source.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defines that the Reader is no longer 
     * needed.
     * @return a readed for reading the character-info from the source
     */

    public Reader getSourceReader()
    {
        StringBuilder sb = new StringBuilder();
        for (String l : txt)
        {
            sb.append(l);
            sb.append(System.getProperty("line.separator"));
        }
        super.reader = new StringReader(sb.toString());
        return super.reader;
    }
    
//------------------------------------------------------------------------------

    public static class ShortTextAsSourceSerializer 
    implements JsonSerializer<ShortTextAsSource>
    {
        @Override
        public JsonElement serialize(ShortTextAsSource tas, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONICIMPLEMENTATION, 
            		InfoChannelImplementation.valueOf(
            				tas.getClass().getSimpleName().toUpperCase())
            		.toString());
            jsonObject.addProperty(JSONINFOCHANNELTYPE, 
            		tas.getType().toString());
            
            jsonObject.add("txt", context.serialize(tas.txt));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class ShortTextAsSourceDeserializer 
    implements JsonDeserializer<ShortTextAsSource>
    {
        @Override
        public ShortTextAsSource deserialize(JsonElement json, Type typeOfT,
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

			List<String> lines = context.deserialize(jsonObject.get("txt"),
					new TypeToken<ArrayList<String>>(){}.getType());

            InfoChannelType type = context.deserialize(
            		jsonObject.get(JSONINFOCHANNELTYPE), InfoChannelType.class);
            
            ShortTextAsSource tas = new ShortTextAsSource(lines);
            tas.setType(type);
        	
        	return tas;
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
        sb.append("ShortTextAsSource [ICType:").append(super.getType());
        sb.append("; text:").append(txt);
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

	@Override
	public boolean canBeRead() 
	{
		return txt!=null;
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
         
        ShortTextAsSource other = (ShortTextAsSource) o;
         
        if (!this.txt.equals(other.txt))
            return false;
        
        return super.equals(other);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(txt, super.hashCode());
    }

//------------------------------------------------------------------------------


//------------------------------------------------------------------------------

}
