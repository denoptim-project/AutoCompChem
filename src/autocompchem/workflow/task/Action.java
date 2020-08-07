package autocompchem.workflow.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import autocompchem.text.TextAnalyzer;

/*
 *   Copyright (C) 2017  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * Action to be taken in a job workflow.
 * 
 * Naming conventions for job relations in  workflow:
 * <ul>
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

public class Action
{
    /**
     * Type of action
     */
    private ActionType type = ActionType.GOON;
    
    /**
     * Thing on Which we do the action
     */
    private ActionObject object = ActionObject.FOCUSJOB;
    
    /**
     * Known actions
     */
    public enum ActionType {REDO, REDOAFTER, GOON, STOP, WAIT};
    
    /**
     * Possible Action objects (i.e., the thing on which we do the action)
     */
    public enum ActionObject {FOCUSJOB,
    	MASTERJOB, PREVIUSJOB, PARALLELJOB, SUBSEQUENTJOB};
    
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty Action
     */
    public Action()
    {}
    
//------------------------------------------------------------------------------

    /**
     * Constructs an Action that is defined in a string of text. This method 
     * converts a text-based definition of actions in an Action object. The text
     * is expected to contain one or more lines (i.e., line separators), each
     * with one of the settings defining the action,
     * though multiline blocks are also allowed, if properly identified by the
     * {@link ActionConstants.STARTMULTILINE} and 
     * {@link ActionConstants.ENDMULTILINE} labels.
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
     * though multiline blocks are also allowed, if properly identified by the
     * {@link ActionConstants.STARTMULTILINE} and 
     * {@link ActionConstants.ENDMULTILINE} labels.
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
    			type = ActionType.valueOf(form.get(key));
    			break;
    		case (ActionConstants.OBJECTKEY):
    			object = ActionObject.valueOf(form.get(key));
    			break;
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
