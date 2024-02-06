package autocompchem.worker;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.run.jobediting.JobEditType;

/*   
 *   Copyright (C) 2022  Marco Foscato 
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
 * A documented configuration item that can be used to set something in a 
 * {@link Worker}. Configuration items define the way to give input to 
 * {@link Worker}s and couple it with some documentations.
 *
 * @author Marco Foscato
 */

public class ConfigItem
{
	/**
	 * The string that identified this item. This is used for comparing items,
	 * which is done in case insensitive way.
	 */
	final String key;
	
	/**
	 * A cool, nice looking, specially-formatted lower/upper-case version of 
	 * the string that
	 * identified this item. Even though, we compare item keys in case
	 * insensitive ways, we like to work with strings that facilitate the
	 * reading and interpretation of the keys.
	 */
	final String casedKey;
	
	/**
	 * A String-like description of the JAVA type of the value expected as input
	 * for this item.
	 */
	final String type;
	
	/**
	 * Doc string to be reported in the help message that described this 
	 * configuration item.
	 */
	final String doc;
	
	/**
	 * Flag defining if this item is meant to configure stand alone instances
	 * of this worker.
	 */
	private Boolean isForStandalone = false;
	
	/**
	 * The name of the worker that is actually taking care of doing some
	 * work, but that is not he main worker to which this configuration 
	 * item applies. We use this to fetch documentation on configuration
	 * items that are effective on sub-jobs.
	 */
	final String embeddedWorker;
	
	/**
	 * A very short sentence defining what this configuration item pertains
	 * to.
	 */
	final String tag;

//------------------------------------------------------------------------------
	  
	public ConfigItem(String key, String type, String casedKey, String doc, 
			boolean isForStandalone, String embeddedWorker, String tag)
	{
		this.key = key;
		this.casedKey= casedKey;
		this.type = type;
		this.doc = doc;
		this.isForStandalone = isForStandalone;
		this.embeddedWorker = embeddedWorker;
		this.tag = tag;
	}
//------------------------------------------------------------------------------
  
	/**
	 * Creates a string that is formatted to be printed on a command line 
	 * interface, thus with a max line length of 76 characters and using 
	 * list item identifier and indentation to facilitate reading of this
	 * item in a list of items.
	 * @return a string formatted to optimize printing in CLI's help message.
	 */
	public Object getStringForHelpMsg() {
		if (embeddedWorker!=null)
		{
			// When we have the embeddedWorker it means this configuration item 
			// is a container for items that control the embedded worker.
			StringBuilder sb = new StringBuilder();
			sb.append(" Settings pertaining ").append(tag).append(": ");
			sb.append(System.getProperty("line.separator"));
			Worker w;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Worker> clazzWorker = 
					(Class<? extends Worker>) Class.forName(embeddedWorker);
				w = WorkerFactory.createWorker(clazzWorker);
				sb.append(w.getEmbeddedTaskSpecificHelp());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				sb.append(" Error fetching help msg for '" + embeddedWorker 
						+ "'");
			}
			return sb.toString();
		} else {
			return getStringThisKey();
		}
	}
	
//------------------------------------------------------------------------------
	  
	/**
	 * Creates a string that is formatted to be printed on a command line 
	 * interface, thus with a max line length of 76 characters and using 
	 * list item identifier and indentation to facilitate reading of this
	 * item in a list of items.
	 * @return a string formatted to optimize printing in CLI's help message.
	 */
	private String getStringThisKey() {
		// The regex is meant to ignore any newline chars combination. This
		// to enable usage of newline characters in the documentation string.
		String[] words = doc.split("[^\\S\\r\\n]"); 
		StringBuilder sbHeader = new StringBuilder();
		sbHeader.append(" -> ").append(casedKey).append(" ").append(type);
		String indent = "        ";
		sbHeader.append(System.getProperty("line.separator"));
		
		int previousRowsLength = 0;
		int maxLineLength = 76;

		StringBuilder sb = new StringBuilder();
		sb.append(indent);
		for (int i=0; i<words.length; i++) 
		{
			int currentRowLength = sb.length() - (previousRowsLength);
			int possibleLength = currentRowLength + words[i].length();
			
			if (possibleLength < maxLineLength)
			{
				sb.append(words[i]);
				currentRowLength = possibleLength;
			} else {
				previousRowsLength = previousRowsLength + currentRowLength;
				sb.append(System.getProperty("line.separator"));
				sb.append(indent).append(words[i]);
			}
			if (words[i].matches(".*[\\r\\n]+"))
			{
				previousRowsLength = previousRowsLength + currentRowLength;
				sb.append(indent);
			} else {
				sb.append(" ");
			}
		}
		sb.append(System.getProperty("line.separator"));
		sbHeader.append(sb.toString());
		return sbHeader.toString();
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * @return <code>true<code> if this item is meant to configure stand alone 
	 * instances of this worker.
	 */
	public boolean isForStandalone()
	{
		if (isForStandalone==null)
			return false;
		return isForStandalone;
	}
	
//------------------------------------------------------------------------------
	
	public static class ConfigItemTypeDeserializer 
	implements JsonDeserializer<ConfigItem>
	{
		@Override
		public ConfigItem deserialize(JsonElement json, 
				Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
  	        JsonObject jsonObject = json.getAsJsonObject();
  	        
  	        ConfigItem ci = context.deserialize(
  	        		jsonObject.get("address"), DirComponentAddress.class);
  	      	
  	        String key = null;
  	        if (jsonObject.has("key"))
			{
  	            key = jsonObject.get("key").getAsString();
                if (!key.toUpperCase().equals(key))
                {
                	throw new JsonParseException(
                			ConfigItem.class.getSimpleName()
                			+ "'s key '" + key + "' is not all upper case.");
                }
			}
            String casedKey = null;
            if (jsonObject.has("casedKey"))
            {
                casedKey = jsonObject.get("casedKey").getAsString();
                if (!key.equals(casedKey.toUpperCase()))
                {
                	throw new JsonParseException(
                			ConfigItem.class.getSimpleName()
                			+ "'s key '" + key + "' doues not correspond to "
                					+ "casedKey '" + casedKey + "'.");
                }
            } else if (jsonObject.has("key")) {
            	casedKey = key;
            }
            String type = null;
            if (jsonObject.has("type"))
            {
                type = jsonObject.get("type").getAsString();
            }
            String doc = null;
            if (jsonObject.has("doc"))
            {
                doc = jsonObject.get("doc").getAsString();
            }
            
            String tag = null;
            if (jsonObject.has("tag"))
            {
                tag = jsonObject.get("tag").getAsString();
            }
			boolean isForStandalone = false;
			if (jsonObject.has("isForStandalone"))
			{	
				isForStandalone = context.deserialize(
						jsonObject.get("isForStandalone"),
						Boolean.class);
			}
            String embeddedWorker = null;
            if (jsonObject.has("embeddedWorker"))
            {
                embeddedWorker = jsonObject.get("embeddedWorker").getAsString();
            }
			return new ConfigItem(key, type, casedKey, doc, isForStandalone, 
					embeddedWorker, tag);
		}
	}

//------------------------------------------------------------------------------
	  
}
