package autocompchem.perception.infochannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.io.ACCJson;
import autocompchem.io.ResourcesTools;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;

/**
 * A list of information channels
 * 
 * @author Marco Foscato
 */

public class InfoChannelBase
{
    /**
     * List of information channels
     */
    private List<InfoChannel> allInfoChannels = new ArrayList<InfoChannel>();

    /**
     * Indexing of InfoChannels by type
     */
    private Map<InfoChannelType,ArrayList<InfoChannel>> mapByICType = 
                          new HashMap<InfoChannelType,ArrayList<InfoChannel>>();

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty InfoChannelBase
     */

    public InfoChannelBase() 
    {
    }
    
//------------------------------------------------------------------------------
  	
    /**
     * Get default info channels configuration that pertain a specific software 
     * and that is available on the ACC knowledgebase.
     * @param software the software name. 
     * Case insensitive, as it is always converted to lower case.
     * @param version the version identifier. Could be any string, even blank.
     * Case insensitive, as it is always converted to lower case.
     * @return the collection of all the info channels matching the criterion.
     * @throws IOException when failing to read resources.
     */
  	public static InfoChannelBase getDefaultInfoChannelDB(String software, 
  			String version) throws IOException
  	{
  		String prefix = "";
      	if (software!=null && !software.isBlank())
      	{
      		prefix = software.toLowerCase();
      		if (version!=null && !version.isBlank())
          	{
      			prefix = prefix + "_" + version.toLowerCase();
          	}
      	}
      	return getDefaultInfoChannelDB(prefix);
  	}
	
//------------------------------------------------------------------------------
    	
    /**
     * Get default info channels from the ACC knowledgebase and that match the
     * given prefix.
     * @param prefix the partial folder tree under which to search for info
     * channels. Case insensitive, as it is always converted to lower case.
     * @return the collection of all the info channels that could be imported  
     * from the ACC knowledgebase.
     * @throws IOException when failing to read resources.
     */
    public static InfoChannelBase getDefaultInfoChannelDB(String prefix) 
    			throws IOException
    {
       	String dbRoot = "knowledgebase/infochannels";
       	if (prefix!=null && !prefix.isBlank())
       	{
       		dbRoot = dbRoot + "/" + prefix.toLowerCase();
       	}
       	return getInfoChannelDB(dbRoot);
    }  	
	
//------------------------------------------------------------------------------
  	
    /**
     * Get info channels from any base path in the resources.
     * @param basepath the base path under which to search for info channels.
     * @return the collection of all the info channels found in
     * the base path.
     * @throws IOException when failing to read resources.
     */
  	public static InfoChannelBase getInfoChannelDB(String basepath) 
  			throws IOException
  	{
      	Gson reader = ACCJson.getReader();
      	InfoChannelBase icb = new InfoChannelBase();
    	ClassLoader cl = reader.getClass().getClassLoader();
        try 
        {
        	for (InputStream is : ResourcesTools.getAllResourceStreams(basepath))
			{
        		BufferedReader br = new BufferedReader(new InputStreamReader(is));
        		InfoChannel ic = reader.fromJson(br, InfoChannel.class);
			    try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					br.close();
				}
			    icb.addChannel(ic);
			}
		} catch (Throwable t) {
			throw new IOException("Could not read InfoChannel from '" + basepath 
					+ "'.", t);
		}
  		return icb;
  	}

//------------------------------------------------------------------------------

    /**
     * Add a channel
     * @param channel the channel to be included in this list
     */

    public void addChannel(InfoChannel channel)
    {
        allInfoChannels.add(channel);

//TODO deal with UNDEFINED type as if it was a whildcard

        //Indexing by InfoChannelType
        InfoChannelType ict = channel.getType();
        if (mapByICType.keySet().contains(ict))
        {
            mapByICType.get(ict).add(channel);
        }
        else
        {
            mapByICType.put(ict,
                            new ArrayList<InfoChannel>(Arrays.asList(channel)));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Get all information channels
     * @return all the information channels
     */

    public List<InfoChannel> getAllChannels()
    {
        return allInfoChannels;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the number of channels in this collection
     * @return the number of information channels
     */
    
    public int getInfoChannelCount()
    {
    	return allInfoChannels.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get all information channel types
     * @return all the information channel types
     */

    public Set<InfoChannelType> getAllChannelType()
    {
        return mapByICType.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Get all channels of a specific type
     * @param itc the specific info channel type
     * @return the list of matching channels or an empty list.
     */

    public List<InfoChannel> getChannelsOfType(InfoChannelType ict)
    {
    	List<InfoChannel> matches = mapByICType.get(ict);
    	if (matches!=null)
    		return matches;
        return new ArrayList<InfoChannel>();
    }

//------------------------------------------------------------------------------

    /**
     * Return a string describing this list
     * @return a human readable description of the list
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("InfoChannelBase [");
        for (InfoChannel ic : allInfoChannels)
        {
            sb.append(ic.toString()).append("; ");
        }
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    public static class InfoChannelBaseSerializer 
    implements JsonSerializer<InfoChannelBase>
    {
        @Override
        public JsonElement serialize(InfoChannelBase fas, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();
            
            jsonObject.add("InfoChannels", context.serialize(fas.getAllChannels()));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class InfoChannelBaseDeserializer 
    implements JsonDeserializer<InfoChannelBase>
    {
        @Override
        public InfoChannelBase deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();
            
            List<InfoChannel> channels = context.deserialize(
            		jsonObject.get("InfoChannels"),
					new TypeToken<ArrayList<InfoChannel>>(){}.getType());
            
            // This way we construct also the mapping that is not serialized
            InfoChannelBase icb = new InfoChannelBase();
            for (InfoChannel ic : channels)
            {
            	icb.addChannel(ic);
            }
        	
        	return icb;
        }
    }
  	
//-----------------------------------------------------------------------------

  	@Override
  	public InfoChannelBase clone()
  	{
  		InfoChannelBase clone = new InfoChannelBase();
  		// This way we construct also the mapping that is not serialized
        InfoChannelBase icb = new InfoChannelBase();
        for (InfoChannel ic : this.allInfoChannels)
        {
        	icb.addChannel(ic);
        }
  		return clone;
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
         
        InfoChannelBase other = (InfoChannelBase) o;
         
        if (!this.allInfoChannels.equals(other.allInfoChannels))
            return false;
        
        return true;
    }

//------------------------------------------------------------------------------

}
