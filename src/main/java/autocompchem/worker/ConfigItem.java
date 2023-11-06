package autocompchem.worker;

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
	final String key;
	final String type;
	final String doc;
	private Boolean isForStandalone = false;
	final String embeddedWorker;
	final String tag;

//------------------------------------------------------------------------------
	  
	public ConfigItem(String key, String type, String doc, 
			boolean isForStandalone, String embeddedWorker, String tag)
	{
		this.key = key;
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
				w = WorkerFactory.createWorker(embeddedWorker);
				sb.append(w.getEmbeddedTaskSpecificHelp());
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
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
		sbHeader.append(" -> ").append(key).append(" ").append(type);
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
	  
}
