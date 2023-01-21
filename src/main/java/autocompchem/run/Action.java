package autocompchem.run;

/*
 *   Copyright (C) 2017  Marco Foscato
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
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import autocompchem.text.TextAnalyzer;
import autocompchem.worker.WorkerConstants;

/**
 * Action to be taken in a job workflow.
 * 
 * Naming conventions for job relations in  workflow:
 * <ul>
 *   <li>EVALJOB: is the job evaluation job itself.</li>
 *   <li>FOCUS: is the job that is being or has been evaluated.</li>
 *   <li>MASTER: is the job of which the FOCUS job is a sub job.</li>
 *   <li>PREVIUS (or PREV): is the job that was done before starting the FOCUS job.</li>
 *   <li>SUBSEQUENT (or SUBSQ): is the job that comes sequentially after the FOCUS job.</li>
 *   <li>SUBJOB (or SUBJ): is any job of which the FOCUS job is the master job.</li>
 *   <li>PARALLEL (or PARJOB): is any job that was started in parallel to the FOCUS job.</li>
 * </ul>
 *
 * @author Marco Foscato
 */

public class Action implements Cloneable
{
    /**
     * Type of action
     */
    private ActionType type = ActionType.GOON;
    
    /**
     * Thing on which we do the action
     */
    private ActionObject object = ActionObject.FOCUSJOB;
    
    /**
     * Known actions
     */
    public enum ActionType {REDO, REDOAFTER, GOON, STOP, WAIT};
    
    /**
     * Possible Action objects (i.e., the thing on which we do the action)
     */
    public enum ActionObject {FOCUSJOB, MASTERJOB, PREVIOUSJOB, PARALLELJOB, 
    	SUBSEQUENTJOB, EVALJOB};
    	
    /**
     * Details pertaining this action
     */
    private Map<String,String> details = new HashMap<String,String>();
    
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty Action
     */
    
    public Action()
    {}
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an Action with given fields.
     * @param type the type of action to perform.
     * @param object the object on which the action is to be performed.
     * @param details the map of details associated to this action.
     */
    
    public Action(ActionType type, ActionObject object)
    {
    	this(type, object, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an Action with given fields.
     * @param type the type of action to perform.
     * @param object the object on which the action is to be performed.
     * @param details the map of details associated to this action.
     */
    
    public Action(ActionType type, ActionObject object, 
    		Map<String,String> details)
    {
    	this.type = type;
    	this.object = object;
    	this.details = details;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs an Action that is defined in a string of text. This method 
     * converts a text-based definition of actions in an Action object. The text
     * is expected to contain one or more lines (i.e., line separators), each
     * with one of the settings defining the action,
     * though multiple line blocks are also allowed, if properly identified by
     * {@link WorkerConstants.STARTMULTILINE} and 
     * {@link WorkerConstants.ENDMULTILINE} labels.
     * @param txt the text to decode.
     * @throws Exception if unable to interpret text.
     */

    public Action(String txt) throws Exception
    {
    	ArrayList<String> lines = new ArrayList<String>();
    	String[] parts = txt.split(System.getProperty("line.separator"));
    	Collections.addAll(lines, parts);
    	makeFromLines(lines);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs an Action that is defined in a set of lines. This method 
     * converts a text-based definition of actions in an Action object. Each
     * line is expected to contain one of the settings defining the action,
     * though multiple line blocks are also allowed, if properly identified by
     * {@link WorkerConstants.STARTMULTILINE} and 
     * {@link WorkerConstants.ENDMULTILINE} labels.
     * @param lines the text to decode.
     * @throws Exception if unable to interpret text.
     */

    public Action(ArrayList<String> lines) throws Exception
    {
    	makeFromLines(lines);
    }
    
//------------------------------------------------------------------------------

    /**
     * Method that parses lines of text and sets this Action according to the
     * information found in the lines of text.
     * @param lines the lines of text.
     * @throws Exception if unable to interpret text.
     */
    
    private void makeFromLines(ArrayList<String> lines) throws Exception 
    {
    	//If needed to parse multiple instances of the same KEY, then use
        //ArrayList<ArrayList<String>> form = TextAnalyzer.readKeyValue(
    	TreeMap<String,String> form = TextAnalyzer.readKeyValuePairs(
                lines,
                ActionConstants.SEPARATOR,
                ActionConstants.COMMENTLINE,
                ActionConstants.STARTMULTILINE,
                ActionConstants.ENDMULTILINE);
    	
    	for (String key : form.keySet())
    	{
    		switch (key)
    		{
    		case (ActionConstants.TYPEKEY):
    			type = ActionType.valueOf(form.get(key).toUpperCase());
    			break;
    			
    		case (ActionConstants.OBJECTKEY):
    			object = ActionObject.valueOf(form.get(key).toUpperCase());
    			break;
    			
    		case (ActionConstants.DETAILSKEY):
    		    List<List<String>> inFrm = TextAnalyzer.readKeyValue(
    		    	new ArrayList<String>(Arrays.asList(
    		    	form.get(key).split(System.getProperty("line.separator")))),
    	                ActionConstants.SEPARATOR,
    	                ActionConstants.COMMENTLINE,
    	                ActionConstants.STARTMULTILINE,
    	                ActionConstants.ENDMULTILINE);
    		    for (List<String> arr : inFrm)
    		    {
    		    	details.put(arr.get(0), arr.get(1));
    		    }
    			break;
    			
    		default:
    			throw new Exception("Unable to understand keyword '" + key 
    					+ "' while creation an Action.");
    		}
    	}
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the type of this action.
     * @return the type.
     */

    public ActionType getType()
    {
        return type;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the object of this action. I.e., the thing on which the action is 
     * performed.
     * @return the object of the action.
     */

    public ActionObject getObject()
    {
        return object;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the details attached to this action.
     * @return the details.
     */

    public Map<String, String> getDetails()
    {
        return details;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return a specific detail attached to this action.
     * @return the detail or null
     */

    public String getDetail(String ref)
    {
        return details.get(ref);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep copy of this object
     * @return a deep copy
     */
    
    public Action clone()
    {
    	Map<String,String> newMap = new HashMap<String,String>();
    	for (String key : details.keySet())
    	{
    		newMap.put(key, details.get(key).toString());
    	}
    	Action a = new Action(type, object, newMap);
    	return a;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return a human readable representation of the action.
     * @return a string.
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Action [type:").append(type).append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
