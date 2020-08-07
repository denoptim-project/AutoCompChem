package autocompchem.perception.circumstance;

import autocompchem.perception.infochannel.InfoChannelType;

/*
 *   Copyright (C) 2020  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
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
    	String act = parts[1].toUpperCase();
    	String restOfStr = str.substring(str.indexOf(act) + act.length());
    	switch (act)
    	{
    		case CircumstanceConstants.MATCHES:
    			c = new MatchText(restOfStr, ict);
    			break;
    			
    		case CircumstanceConstants.NOMATCH:
    			c = new MatchText(restOfStr, true, ict);
    			break;
    			
    		case CircumstanceConstants.LOOPCOUNTER:
    			String counterId = "none";
    			int n = 0;
    			int m = 0;
    			if (parts[2].matches("[0-9]*"))
    			{
    				String firstOp = "";
    				String errMsg = "Cannot understand condition '" 
							+ str + "'. Expecting 'ID >/< Num',"
							+ " 'Num >/< ID', or 'N > ID > M'.";
    				switch (parts.length)
    				{
    					case 5:
    						// We have a string like: N >/< ID    	    				
    	    				n = Integer.parseInt(parts[2]);
    	    				firstOp = parts[3];
    	    				counterId = parts[4];
    	    				switch (firstOp)
    	    				{
    	    				    // WARNING: careful with the < and > as they are not
    	    				    // the same as in other switch blocks of this kind!
    	    					case "<":
    	    						c = new LoopCounter(counterId, n, 
    	    								Integer.MAX_VALUE, ict);
    	    						break;
    	    						
    	    					case ">":
    	    						c = new LoopCounter(counterId, 
    	    								Integer.MIN_VALUE, n, ict);
    	    						break;
    	    						
    	    					default:
    	    						throw new Exception("Cannot undertsnd operator '" 
    	    							+ firstOp + "' while reading "
    	    		    				+ "string '" + str + "'."
    	    		    				+ " Use either '>' or '<'.");
    	    				}
    						break;
    						
    					case 7:
    						// We have a string like: N >/< ID >/< M
    	    				n = Integer.parseInt(parts[2]);
    	    				firstOp = parts[3];
    	    				counterId = parts[4];
    	    				String secondOp = parts[5];
    	    				m = Integer.parseInt(parts[6]);
    	    				if (firstOp.equals(">") && secondOp.equals(">"))
    	    				{
    	    					if (m>n)
    	    					{
    	    						throw new Exception("Inconsistent range "
    	    								+ "limits in definition of loop "
    	    								+ "counter range '" + str + "'.");
    	    					}
    	    					c = new LoopCounter(counterId, m, n, ict);
    	    				} else if (firstOp.equals("<") && secondOp.equals("<"))
    	    				{
    	    					if (n>m)
    	    					{
    	    						throw new Exception("Inconsistent range "
    	    								+ "limits in definition of loop "
    	    								+ "counter range '" + str + "'.");
    	    					}
    	    					c = new LoopCounter(counterId, n, m, ict);
    	    				} else {
    	    					throw new Exception(errMsg);
    	    				} 
    	    				break;
    						
    					default:
    						throw new Exception(errMsg);
    				}
       			} else {   				
    				// We have a string like: ID <=> N
    				counterId = parts[2];
    				String operator = parts[3];
    				n = Integer.parseInt(parts[4]); 
    				
    				// Value of M is put to the extreme
    				switch (operator)
    				{
    					case ">":
    						c = new LoopCounter(counterId, n, 
    								Integer.MAX_VALUE, ict);
    						break;
    						
    					case "<":
    						c = new LoopCounter(counterId, 
    								Integer.MIN_VALUE, n, ict);
    						break;
    						
    					default:
    						throw new Exception("Cannot undertsnd operator '" 
    							+ operator + "' while reading "
    		    				+ "string '" + str + "'."
    		    				+ " Use either '>' or '<'.");
    				}
    			}
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

