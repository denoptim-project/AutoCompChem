package autocompchem.wiro.chem;

import java.lang.reflect.Type;

/*
 *   Copyright (C) 2020  Marco Foscato
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.text.TextBlock;

/**
 * A keyword is a string with an associated value. Keywords, together with
 * {@link DirectiveData} provide specifics to a {@link Directive}.
 * Keywords differ from {@link DirectiveData} in two aspects. Firstly,
 * Keyword can be either "mute" or "loud". A "loud" keyword is written in 
 * the software's input file as <code>KEY&lt;SEPARATOR&gt;VALUE</code> while 
 * a "mute" keyword is given to the designated software using only its 
 * <code>VALUE</code>. In other words,
 * the <code>KEY</code> part (i.e., the name) of a "mute" keyword is omitted 
 * when preparing the input file for the designated software.
 * Secondly, regardless of whether it is loud or mute, a Keyword is expected to 
 * generate a string that fits in one line (contains no newline characters) 
 * and can often be appended to the corresponding string from other keywords
 * thus building a line that contains multiple keywords. Instead, 
 * {@link DirectiveData} can often generate multiple
 * lines of text upon conversion into an input file for a third party software. 
 * Note that the value can be any {@link Object}, which may need to be be
 * converted into a string when translating a keyword into a string for 
 * preparation of an input file.
 *
 * @author Marco Foscato
 */

public class Keyword extends DirectiveData
{
    /**
     * Keyword type: either "mute" (false), or "loud" (true).
     */
    private boolean isLoud = false;
    
//-----------------------------------------------------------------------------

    /**
     * Constructor for a keyword from name, type, and value.
     * @param name the name of the keyword (i.e., the actual keyword).
     * @param isLoud use <code>false</code> to specify a mute-type of keyword
     * or <code>true</code> for loud type.
     * @param value the value of the keywords.
     */

    public Keyword(String name, boolean isLoud, Object value)
    {
        this(name);
        this.isLoud = isLoud;
        this.setValue(value);
        if (value instanceof ArrayList || value instanceof String)
        	extractTask();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor for a keyword that has only its named defined.
     * @param name the name of the keyword (i.e., the actual keyword).
     */

    public Keyword(String name)
    {
        super(name);
    }

//-----------------------------------------------------------------------------

    /**
     * Maker for a keyword from formatted text (i.e., job details line).
     * @param line the formatted text to be parsed.
     */

    public static Keyword makeFromJDLine(String line)
    {
    	boolean isLoud = false;
        String upLine = line.toUpperCase();
        String[] nameAndValue = null;
        if (upLine.startsWith(ChemSoftConstants.JDLABLOUDKEY))
        {
            isLoud = true;
            nameAndValue = parseLine(line,ChemSoftConstants.JDLABLOUDKEY);
        }
        else if (upLine.startsWith(ChemSoftConstants.JDLABMUTEKEY))
        {
            isLoud = false;
            nameAndValue = parseLine(line,ChemSoftConstants.JDLABMUTEKEY);
        }
        else
        {
            throw new IllegalArgumentException("Expected '" 
            		+ ChemSoftConstants.JDLABLOUDKEY + "' or '" 
            		+ ChemSoftConstants.JDLABMUTEKEY + "' in front of key"
            		+ " name '" + line + "'.");
        }
        Keyword k = new Keyword(nameAndValue[0], isLoud, nameAndValue[1]);
        k.extractTask();
        return k;
    }
    
//-----------------------------------------------------------------------------
    
    private void extractTask()
    {
	    if (hasACCTask())
	    {
			List<String> lines = getValueAsLines();
			if (lines==null)
				return;
			// WARNING! Here we assume that the entire content of the 
			// keyword value, is about the ACC task. Thus, we add the 
			// multiline start/end labels so that the getACCTaskParams
			// method will keep read all the lines as one.
			if (lines.size()>1)
			{
				lines.set(0, ChemSoftConstants.JDOPENBLOCK + lines.get(0));
				lines.set(lines.size()-1, lines.get(lines.size()-1) 
						+ ChemSoftConstants.JDCLOSEBLOCK);
			}
			ParameterStorage ps = Directive.parseACCTaskParams(lines);
			ps.setParameter(ChemSoftConstants.JDACCTASK,
					ps.getParameterValue(
							ChemSoftConstants.JDLABACCTASK));
			ps.removeData(ChemSoftConstants.JDLABACCTASK);
			setTaskParams(ps);
			removeValue();
	    }
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the name (i.e., the actual keyword) of this keyword.
     * @return the name of this keyword.
     */

    public String getName()
    {
        return getReference();
    }

//-----------------------------------------------------------------------------
    
    /**
     * @return the kind of directive component this is.
     */
	public DirectiveComponentType getComponentType() 
	{
		return DirectiveComponentType.KEYWORD;
	}

//-----------------------------------------------------------------------------
    
    /**
     * Defines if this keyword is loud, meaning that it is 
     * meant to be written as a <code>KEY&lt;SEPARATOR&gt;VALUE</code> pair 
     * or not.
     * @param isLoud use <code>true</code> to specify that this 
     * keyword is loud.
     */
    public void setLoud(boolean isLoud)
    {
    	this.isLoud = isLoud;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if this keyword is loud, meaning that it is 
     * meant to be written as a <code>KEY&lt;SEPARATOR&gt;VALUE</code> pair
     */
    public boolean isLoud()
    {
    	return isLoud;
    }

//-----------------------------------------------------------------------------

    /**
     * Parse a job details line and get keyword name and value.
     * @param line the line to parse.
     * @param label the label identifying the type of keyword.
     * @return a pair where the first entry is the keyword name and the second 
     * its value.
     */

    private static String[] parseLine(String line, String label)
    {   
        String[] p = line.split(ChemSoftConstants.JDKEYVALSEPARATOR,2);
        String[] nameAndValue = new String[2];
        nameAndValue[0] = p[0].substring(label.length()).trim();
        nameAndValue[1] = p[1];
        return nameAndValue;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Produces a formatted line according to autocompchem's job details format.
     * @return the formatted string ready to print the line setting this
     * keyword in a job details file
     */

    @SuppressWarnings("unchecked")
	public String toStringJobDetails()
    {
        StringBuilder sb = new StringBuilder();
        if (isLoud)
        {
            sb.append(ChemSoftConstants.JDLABLOUDKEY);
        }
        else
        {
            sb.append(ChemSoftConstants.JDLABMUTEKEY);
        }
        sb.append(getReference()).append(ChemSoftConstants.JDKEYVALSEPARATOR);
        
        if (getType().equals(NamedDataType.TEXTBLOCK))
        {
        	TextBlock tb = new TextBlock((ArrayList<String>) getValue());
	        if (tb.size()>1)
	        {
	        	sb.append(ChemSoftConstants.JDOPENBLOCK);
	        }
	        
	        for (int i=0;i<tb.size(); i++)
	        {
	        	String v = tb.get(i);
	            sb.append(v);
	            if (i<(tb.size()-1))
	            	sb.append(System.getProperty("line.separator"));
	        }
	        if (tb.size()>1)
	        {
	        	sb.append(ChemSoftConstants.JDCLOSEBLOCK);
	        }
    	} else {
    		sb.append(getValueAsString());
        }
        return sb.toString();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Produced a string representation of this keyword
     */
    public String toString()
    {
    	return "[Keyword: '" + getReference() + "', " + isLoud + ", '" 
    			 + getValueAsString() + "']";
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Produced a string formatted according to the loud/mute property
     * and the given separator
     */
    public String toString(String separator)
    {
    	if (isLoud())
		{
			return getName() + separator + getValueAsString();
		} else {
			return getValueAsString();
		}
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o) 
    {
    	if (o == null)
    		return false;
	
	    if (o == this)
		    return true;
	   
	    if (o.getClass() != getClass())
	    	return false;
 	   
 	    Keyword other = (Keyword) o;
 	    
 	    if ((this.isLoud && !other.isLoud) || (!this.isLoud && other.isLoud))
 	    	return false;
 	   
 	    return super.equals(o);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(isLoud, super.hashCode());
    }
    
//------------------------------------------------------------------------------

    public static class KeywordSerializer implements JsonSerializer<Keyword>
    {
		@Override
		public JsonElement serialize(Keyword src, Type typeOfSrc, 
				JsonSerializationContext context) 
		{
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("reference", src.getReference());
            NamedDataType type = NamedDataType.UNDEFINED;
            if (src.getType()!=null)
            {
            	type = src.getType();
            }
            if (type != NamedDataType.STRING)
            {
                jsonObject.addProperty("type", type.toString());	
            }
			if (!jsonable.contains(type))
			{
	            jsonObject.addProperty("value", NONJSONABLE);
			} else {
				jsonObject.add("value", context.serialize(src.getValue()));
			}
			if (!src.isLoud())
			{
				jsonObject.add("isLoud", context.serialize(src.isLoud()));
			}
			if (src.getTaskParams()!=null)
			{
				jsonObject.add("accTaskParams", context.serialize(
						src.getTaskParams()));
			}
            return jsonObject;
		}
    }
    
//-----------------------------------------------------------------------------

    public static class KeywordDeserializer 
    implements JsonDeserializer<Keyword>
    {
        @Override
        public Keyword deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
        	JsonObject jsonObjSrc = json.getAsJsonObject(); 
        	
        	// We create the JSON object corresponding to the super class
        	JsonObject jsonDirectiveData = new JsonObject();
        	jsonDirectiveData.add("reference", jsonObjSrc.get("reference"));
            jsonDirectiveData.add("value", jsonObjSrc.get("value"));
        	NamedDataType typ = NamedDataType.STRING;
            if (jsonObjSrc.has("type"))
            {
            	jsonDirectiveData.add("type", jsonObjSrc.get("type"));
            } else {
            	jsonDirectiveData.addProperty("type", typ.toString());
            }
            if (jsonObjSrc.has("accTaskParams"))
            {
            	jsonDirectiveData.add("accTaskParams", jsonObjSrc.get("accTaskParams"));
            }
            
        	// Exploit deserialization of super class
            DirectiveData dd = context.deserialize(jsonDirectiveData, 
        			DirectiveData.class);
        	
        	boolean isLoud = true;
        	if (jsonObjSrc.has("isLoud"))
        	{
        		isLoud = context.deserialize(jsonObjSrc.get("isLoud"), 
        				Boolean.class);
        	}
        	Keyword k = new Keyword(dd.getReference());
        	k.setType(dd.getType());
        	k.setValue(dd.getValue());
        	k.setTaskParams(dd.getTaskParams());
        	k.isLoud = isLoud;
         	return k;
        }
    }

//-----------------------------------------------------------------------------
      
    /**
     * Creates a clone of this keyword.
     * @param dirName the name of the directive to create.
     * @return the new directive.
     * @throws CloneNotSupportedException 
     */
    @Override
  	public Keyword clone() throws CloneNotSupportedException 
  	{
    	DirectiveData dd = super.clone();
    	Keyword k = new Keyword(dd.getReference());
    	k.setType(dd.getType());
    	k.setValue(dd.getValue());
    	k.setTaskParams(dd.getTaskParams());
    	k.isLoud = isLoud;
  		return k;
  	}

//-----------------------------------------------------------------------------
 
}
