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

//------------------------------------------------------------------------------
	  
	public ConfigItem(String key, String type, String doc)
	{
		this.key = key;
		this.type = type;
		this.doc = doc;
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
		String[] words = doc.split("\\s+");
		StringBuilder sbHeader = new StringBuilder();
		sbHeader.append(" -> ").append(key).append(" ").append(type);
		String indent = "        ";
		sbHeader.append(System.getProperty("line.separator")).append(indent);
		int rowCounter = 1;

		StringBuilder sb = new StringBuilder();
		for (int i=0; i<words.length; i++) 
		{
			int length = sb.length() + words[i].length() + 2 
					+ (indent.length()*rowCounter);
			if ((76*rowCounter) > length)
			{
				sb.append(words[i]).append(" ");
			} else {
				rowCounter++;
				sb.append(System.getProperty("line.separator"));
				sb.append(indent).append(words[i]).append(" ");
			}	
		}
		sb.append(System.getProperty("line.separator"));
		sbHeader.append(sb.toString());
		return sbHeader.toString();
	}

//------------------------------------------------------------------------------
	  
}
