package autocompchem.worker;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import autocompchem.wiro.chem.DirComponentAddress;

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
	 * The name of the worker that is actually taking care of doing some
	 * work, but that is not he main worker to which this configuration 
	 * item applies. We use this to fetch documentation on configuration
	 * items that are effective on sub-jobs.
	 */
	final String embeddedWorker;
	
	/**
	 * The specific task of the embedded worker. This may be empty, in which 
	 * case it indicates to produce the documentation for the worker class
	 * rather than its task-specific version.
	 */
	final String task;
	
	
	/**
	 * A very short sentence defining what this configuration item pertains
	 * to.
	 */
	final String tag;
	
	/**
	 * The list of keys of an embedded worker's configuration items to ignore  
	 * when producing documentation strings. Should be not empty only when 
	 * this item defines an embedded worker. This has no effect to the items 
	 * that are given as parameters to embedded workers, i.e., it affects only
	 * the items that are made visible in the documentation, not those that are
	 * actually passed at run time.
	 */
	final List<String> ignorableItems;
	
	/**
	 * The list of further embedded workers of an embedded worker's 
	 * configuration items to ignore when 
	 * producing documentation strings. Should be not empty only when 
	 * this item defines an embedded worker. The content corresponds to workers
	 * that are documented already by being embedded elsewhere, so adding them
	 * here allows to avoid duplication of their documentation.
	 */
	final List<String> ignorableWorkers;
	
	
	public static final int MAXLINELENGTH = 80;
	
//------------------------------------------------------------------------------
	  
	public ConfigItem(String key, String type, String casedKey, String doc, 
			String embeddedWorker, String tag,
			String task,
			List<String> ignorableItems, List<String> ignorableWorkers)
	{
		this.key = key;
		this.casedKey= casedKey;
		this.type = type;
		this.doc = doc;
		this.embeddedWorker = embeddedWorker;
		this.tag = tag;
		this.task = task;
		this.ignorableItems = ignorableItems;
		this.ignorableWorkers = ignorableWorkers;
	}
//------------------------------------------------------------------------------
  
	/**
	 * Creates a string that is formatted to be printed on a command line 
	 * interface, thus with a max line length of 76 characters and using 
	 * list item identifier and indentation to facilitate reading of this
	 * item in a list of items.
	 * @return a string formatted to optimize printing in CLI's help message.
	 */
	public String getStringForHelpMsg() {
		if (embeddedWorker!=null)
		{
			// When we have the embeddedWorker it means this configuration item 
			// is a container for items that control the embedded worker.
			StringBuilder sb = new StringBuilder();
			
			// Make sure the opening sentence fits the max line length
			String openingSentence = "Settings pertaining " + tag + ": ";

			String[] words = openingSentence.split("[^\\S\\r\\n]"); 
			int currentRowLength = 0;
			for (int i=0; i<words.length; i++) 
			{
				String[] splittedWords = words[i].split("[\\r\\n]+",-1);
				for (int j=0; j<splittedWords.length; j++)
				{
					if (j>0)
					{
						sb.append(System.getProperty("line.separator"));
					}
					String word = splittedWords[j];
					int possibleLength = currentRowLength + word.length();
					if (!word.isEmpty())
					{
						possibleLength = possibleLength + 1;
					}
					if (possibleLength < MAXLINELENGTH)
					{
						sb.append(word);
						currentRowLength = possibleLength;
					} else {
						sb.append(System.getProperty("line.separator"));
						sb.append(word);
						currentRowLength = word.length();
					} 
					if (!word.isEmpty())
					{
						sb.append(" ");
						currentRowLength = currentRowLength + 1;
					}
				}
			}
			
			sb.append(System.getProperty("line.separator"));
			Worker w;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Worker> clazzWorker = 
					(Class<? extends Worker>) Class.forName(embeddedWorker);
				w = WorkerFactory.createWorker(clazzWorker);
				if (task!=null && !task.isBlank())
				{
					w.task = Task.make(task);
				}
				for (ConfigItem ci : w.getKnownParameters(ignorableItems, 
						ignorableWorkers))
				{
		    		sb.append(System.getProperty("line.separator"));
		    		sb.append(ci.getStringForHelpMsg());
				}
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
		sbHeader.append(System.getProperty("line.separator"));
		
		StringBuilder sb = new StringBuilder();

		String indent = "        ";
		sb.append(indent);
		int currentRowLength = indent.length();
		
		for (int i=0; i<words.length; i++) 
		{
			String[] splittedWords = words[i].split("[\\r\\n]+",-1);
			for (int j=0; j<splittedWords.length; j++)
			{
				if (j>0)
				{
					sb.append(System.getProperty("line.separator"));
					sb.append(indent);
					currentRowLength = indent.length();
				}
				String word = splittedWords[j];
				int possibleLength = currentRowLength + word.length();
				if (!word.isEmpty())
				{
					possibleLength = possibleLength + 1;
				}
				if (possibleLength < MAXLINELENGTH)
				{
					sb.append(word);
					currentRowLength = currentRowLength + word.length();
				} else {
					sb.append(System.getProperty("line.separator"));
					sb.append(indent).append(word);
					currentRowLength = (indent + word).length();
				} 
				if (!word.isEmpty())
				{
					sb.append(" ");
					currentRowLength = currentRowLength + 1;
				}
			}
		}
		
		sb.append(System.getProperty("line.separator"));
		sbHeader.append(sb.toString());
		return sbHeader.toString();
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
                			+ "'s key '" + key + "' does not correspond to "
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
            String embeddedWorker = null;
            if (jsonObject.has("embeddedWorker"))
            {
                embeddedWorker = jsonObject.get("embeddedWorker").getAsString();
            }
            String task = "";
            if (jsonObject.has("task"))
            {
            	task = jsonObject.get("task").getAsString();
            }
            List<String> ignorableItems = new ArrayList<String>();
            if (jsonObject.has("ignorableItems"))
            {
            	ignorableItems = context.deserialize(
                		jsonObject.get("ignorableItems"),List.class);
            }
            List<String> ignorableWorkers = new ArrayList<String>();
            if (jsonObject.has("ignorableWorkers"))
            {
            	ignorableWorkers = context.deserialize(
                		jsonObject.get("ignorableWorkers"),List.class);
            }
			return new ConfigItem(key, type, casedKey, doc,  
					embeddedWorker, tag, task, ignorableItems, ignorableWorkers);
		}
	}

//------------------------------------------------------------------------------
	  
}
