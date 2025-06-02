package autocompchem.datacollections;

import java.io.File;
import java.lang.reflect.Type;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlockIndexed;

//TODO A "parameter" in this storage is effectively a DirectiveData. 
// And this storage is a simplified access to the underlying NamedData.
// Since the parameter storage in the DirectiveComponents is mostly (but NOT ONLY
// used to define ACC tasks, we "replace" (i.e., add beside) an ACCJob that would 
// define the tasks to do in/for the DirectiveComponent, and use a NamedDataCollector
// to collect additional data in the Directive that is used only as runtime 
// property storage.
// This way, the definition of an ACCJob could be moved from being a ParameterStorage 
// to being a job details in all equal to that of any compchemjob, and json.


/**
 * Storage of parameters, i.e., information that is collected in a list of 
 * entries that can be either 
 * keywords (i.e., strings that have a meaning of their own) or
 * keyword:value pairs (i.e., data that is named: the keyword is the name and
 * the value the data that name refer to).
 * This class has the capability of importing string-based parameters 
 * directly from a formatted text file.
 * The recognized format is as follows:
 * <ul>
 * <li> lines beginning with 
 * {@value  autocompchem.datacollections.ParameterConstants#COMMENTLINE} 
 * are ignored as comments</li>
 * <li> lines beginning with 
 * {@value autocompchem.datacollections.ParameterConstants#STARTMULTILINE} are
 * considered part of a multi line block, together with all the lines that
 * follow until a line beginning with 
 * {@value autocompchem.datacollections.ParameterConstants#ENDMULTILINE}
 * is found. All lines of a multi line block are interpreted as pertaining to a 
 * single parameter (i.e., a {@link NamedData}. The text in between 
 * {@value autocompchem.datacollections.ParameterConstants#STARTMULTILINE} and 
 * the 
 * {@value autocompchem.datacollections.ParameterConstants#ENDMULTILINE}, 
 * apart from containing one or more
 * new line characters, follows the same syntax defined below for the single
 * line definition of a parameter, i.e., a {@link NamedData}.</li>
 * <li> all other lines define each one a single parameter, 
 * i.e., a {@link NamedData}.
 * All text before the separator (i.e., the first 
 * {@value autocompchem.datacollections.ParameterConstants#SEPARATOR} 
 * character) is interpreted as the reference name of the {@link NamedData},
 * while the rest as its value/content. Reference name is case insensitive,
 * and is stored as upper case.</li>
 * </ul>
 *
 * @author Marco Foscato
 */

public class ParameterStorage extends NamedDataCollector
{
	
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ParameterStorage.
     */

    public ParameterStorage()
    {
        super();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Case insensitive evaluation.
     * @param ref the reference name to search form (case insensitive).
     * @return <code>true</code> if the upper case reference name is found
     */
    
    public boolean contains(String ref)
    {
    	return super.contains(ref.toUpperCase());
    }

//------------------------------------------------------------------------------

    /**
     * Return the parameter corresponding to the given reference name.
     * @param ref reference name of the parameter (case insensitive).
     * @return the parameter with the given reference string or null if no
     * such parameter is found.
     */

    public NamedData getParameterOrNull(String ref)
    {
        if (!contains(ref))
        {
            return null;
        }
        return allData.get(ref.toUpperCase());
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the parameter required, which is expected to exist. Kills process
     * with an error if the parameter does not exist.
     * @param ref reference name of the parameter (case insensitive).
     * @return the parameter with the given reference string.
     */

    public NamedData getParameter(String ref)
    {
        if (!contains(ref))
        {
            Terminator.withMsgAndStatus("ERROR! Key '" + ref + "' not found in "
                        + "ParameterStorage!",-1);
        }
        return getParameterOrNull(ref);
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the value of the parameter, which is expected to exist, or null.
     * @param ref reference name of the parameter (case insensitive).
     * @return the parameter with the given reference string, or null
     */

    public String getParameterValue(String ref)
    {
        if (contains(ref))
        {
        	return allData.get(ref.toUpperCase()).getValueAsString();
        }
        return null;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the set of reference names. This method is meant to be the case
     * insensitive analog of <code>map.keySet()</code>.
     * @return the set of reference names for the parameters included in this 
     * collection.
     */
    
    public Set<String> getRefNamesSet()
    {
    	Set<String> s = new HashSet<String>();
    	for (String k : allData.keySet())
    	{
    		s.add(k.toUpperCase());
    	}
    	return s;
    }
    
//------------------------------------------------------------------------------

    /**
     * Store a parameter with the given reference name. If the parameter already
     * exists, it will be overwritten.
     * @param par the new parameter to be stored.
     */

    public void setParameter(NamedData par)
    {
        allData.put(par.getReference().toUpperCase(), par); 
    }

//------------------------------------------------------------------------------

    /**
     * Store a value-less parameter that only has its given reference name. 
     * Such a value-less parameter is typically a keyword: its existence is
     * sufficient to convey some information.
     * If the parameter already
     * exists, it will be overwritten.
     * @param ref the reference name of the parameter.
     * @param value the value of the parameter to be stored.
     */

    public void setParameter(String ref)
    {
        setParameter(new NamedData(ref.toUpperCase(), null)); 
    }
    
//------------------------------------------------------------------------------

    /**
     * Store a parameter with the given reference name and value. 
     * If the parameter already exists, it will be overwritten.
     * @param ref the reference name of the parameter.
     * @param value the value of the parameter to be stored.
     */

    public void setParameter(String ref, Object value)
    {
        setParameter(new NamedData(ref.toUpperCase(), value)); 
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted text file and import all parameters.
     * Meant only for single-job parameter files. Cannot handle parameter files
     * including more than one job nor nested jobs.
     * @param paramFile name of the text file to read.
     */

    public void importParameters(File file) 
    {
        //Get filled form
        List<List<String>> form = IOtools.readFormattedText(file,
                                                   ParameterConstants.SEPARATOR,
                                                 ParameterConstants.COMMENTLINE,
                                              ParameterConstants.STARTMULTILINE,
                                               ParameterConstants.ENDMULTILINE);

        //Make the ParameterStorage object
        importParameterBlocks(form);
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted block of text and import all parameters. 
     * Ignored nested blocks.
     * @param tb the block of lines to read
     */

    public void importParameters(TextBlockIndexed tb)
    {
        importParameterBlocks(TextAnalyzer.readKeyValue(tb.getText(),
                                                  ParameterConstants.SEPARATOR,
                                                ParameterConstants.COMMENTLINE,
                                             ParameterConstants.STARTMULTILINE,
                                              ParameterConstants.ENDMULTILINE));
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted text and import all parameters
     * @param lines the block of lines to read
     */

    public void importParametersFromLines(List<String> lines)
    {
        //Get filled form
        List<List<String>> form = TextAnalyzer.readKeyValue(
                lines,
                ParameterConstants.SEPARATOR,
                ParameterConstants.COMMENTLINE,
                ParameterConstants.STARTMULTILINE,
                ParameterConstants.ENDMULTILINE);

        //Make the ParameterStorage object
        importParameterBlocks(form);
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted blocks and import parameters
     * @param blocks the block of text to read
     */

    public void importParameterBlocks(List<List<String>> blocks)
    {
        for (int i=0; i<blocks.size(); i++)
        {
            List<String> signleBlock = blocks.get(i);
            String key = signleBlock.get(0).toUpperCase();
            String value = signleBlock.get(1);

            //All params read from text file are seen as Strings for now
            NamedData prm = new NamedData(key, value);
            setParameter(prm);
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
 	   
 	    return super.equals(o);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(super.hashCode());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Return a deep clone
     * @returns a deep clone
     */
    public ParameterStorage clone()
    {
    	ParameterStorage newPar = new ParameterStorage();
    	for (String ref: super.getAllNamedData().keySet())
    	{
    		NamedData nd =  super.getNamedData(ref);
    		NamedData ndClone = null;
			try { 
				ndClone= nd.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				Terminator.withMsgAndStatus("ERROR! Could not clone "
						+ this.getClass().getSimpleName(), -1);
			}
			newPar.putNamedData(ndClone.getReference().toUpperCase(), ndClone);
    	}
    	return newPar;
    }  
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a block of lines that can be used to define the parameters 
     * in a parameters' file format.
     * @return the text as a list of lines.
     */
    
    public List<String> toLinesParametersFileFormat()
    {
        List<String> lines = new ArrayList<String>();
        for (String ref : getRefNamesSet())
        {
        	NamedData par = getParameter(ref);
        	String parStr = par.getReference() + ParameterConstants.SEPARATOR;
        	String valueStr = par.getValueAsString();
        	if (valueStr.split("\\r?\\n").length > 1)
        	{
        		valueStr = ParameterConstants.STARTMULTILINE + valueStr 
        				+ ParameterConstants.ENDMULTILINE;
        	}
        	parStr = parStr + valueStr;
            lines.add(parStr);
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * @return the string representation of this parameter storage
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[ParameterStorage [");
        for (String k : allData.keySet())
        {
            sb.append(k).append("=").append(allData.get(k)).append(", ");
        }
        sb.append("]]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    public static class ParameterStorageSerializer 
    implements JsonSerializer<ParameterStorage>
    {
		@Override
		public JsonElement serialize(ParameterStorage src, Type typeOfSrc, 
				JsonSerializationContext context) 
		{
			Collection<NamedData> list = src.allData.values();
			return context.serialize(list);
		}
    }
    
//-----------------------------------------------------------------------------

    public static class ParameterStorageDeserializer 
    implements JsonDeserializer<ParameterStorage>
    {
		@Override
		public ParameterStorage deserialize(JsonElement json, Type typeOfT, 
				JsonDeserializationContext context)
				throws JsonParseException 
		{
        	List<NamedData> list = context.deserialize(json, 
        			new TypeToken<List<NamedData>>(){}.getType());
        	
        	ParameterStorage ps = new ParameterStorage();
        	for (NamedData nd : list)
        	{
        		ps.setParameter(nd);
        	}
         	return ps;
		}
    }
    
//------------------------------------------------------------------------------

}
