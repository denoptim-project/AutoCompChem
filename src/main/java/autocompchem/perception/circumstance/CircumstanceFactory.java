package autocompchem.perception.circumstance;

import autocompchem.perception.infochannel.InfoChannelType;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * Factory building Circumstances. 
 * 
 * @author Marco Foscato
 */

public class CircumstanceFactory
{

//-----------------------------------------------------------------------------

    /**
     * Create a new Circumstance from a formatted string of text. The string
     * is expected to have this format:
     * <pre>InfoChannelType CircumstaceKind [more fields depending of the 
     * kind of circumstance]</pre>
     * 
     * @param str the string to parse and decode.
     * @return the kind of circumstance reflecting the given string.
     * @throws Exception when decoding of string goes wrong.
     */ 

	@Deprecated
    public static Circumstance createFromString(String str) throws Exception
    {
    	String[] parts = str.trim().split("\\s+");
    	if (parts.length < 2)
    	{
    		throw new Exception("Unable to parse definition of Circumstance"
    				+ " from string '" + str + "'");
    	}
    	
    	// Decode type of information channel
    	String ic = parts[0].toUpperCase();
    	InfoChannelType ict = null;
    	for (InfoChannelType cand : InfoChannelType.values())
    	{
    		if (cand.toString().equals(ic))
    		{
    			ict = cand;
    			break;
    		}
    	}
    	if (ict == null)
    	{
    		throw new Exception("Unable to understand InfoChannelType '" 
    					+ ic + "' while creacting circumstance "
    					+ "from string '" + str + "'.");
    	}
    	
    	// Choose kind of circumstance to create
    	Circumstance c = null;
    	String act = parts[1];
    	String restOfStr = str.substring(str.indexOf(act) + act.length()).trim();
    	switch (act.toUpperCase())
    	{
    		case CircumstanceConstants.MATCHES:
    			c = new MatchText(restOfStr, ict);
    			break;
    			
    		case CircumstanceConstants.NOMATCH:
    			c = new MatchText(restOfStr, true, ict);
    			break;
    			
    		case CircumstanceConstants.MATCHESCOUNT:
    			String[] words = restOfStr.trim().split("\\s+");
    			String errMsg = "Cannot understand condition '" 
						+ str + "'. Expecting "
								+    "'<pattern> <num>', "
								+ "or '<pattern> <num> MIN/MAX',"
								+ "or '<pattern> <num> <num>',"
								+ "or '<pattern> <num> <num> NOT'. "
								+ "Where 'MIN', 'MAX', and 'NOT' are "
								+ "not variables, but labels controlling"
								+ " the meaning of the numerical variables.";
    			if (words.length < 2)
    			{
    				throw new Exception(errMsg);
    			}
    			
    			String pattern = words[0];
    			int num = Integer.parseInt(words[1]);
    			if (words.length == 2)
    			{
    				// exact match
    				c = new CountTextMatches(pattern, num, ict);
    			} else if (words.length == 3 && 
    					(words[2].toUpperCase().equals("MIN") || 
    					 words[2].toUpperCase().equals("MAX")))
    			{
    				//MIN or MAX
    				boolean pol = false;
    				if (words[2].toUpperCase().equals("MIN"))
    				{
    					pol = true;
    				}
    				c = new CountTextMatches(pattern, num, pol, ict);
    			} else if (words.length == 3 && words[2].matches("[0-9]*"))
    			{
    				// simple range
    				int max = Integer.parseInt(words[2]);
    				c = new CountTextMatches(pattern, num, max, ict);
    			} else if (words.length == 4)
    			{
    				//Range with negation
    				int max = Integer.parseInt(words[2]);
    				c = new CountTextMatches(pattern, num, max, ict, true);
    			} else {
    				throw new Exception(errMsg);
    			}
    			
    			break;
    			
    		default:
    			throw new Exception("Unable to understand kind of " 
    					+ "circumstance '" + act + "' while creacting "
    					+ "circumstance from string '" + str + "'.");
    	}
    	
    	return c;
    }
    
//-----------------------------------------------------------------------------

}

