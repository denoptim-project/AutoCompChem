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

import autocompchem.molecule.conformation.ConformationalCoordinate;
import autocompchem.run.JobEditTask.TargetType;
import autocompchem.text.TextAnalyzer;
import autocompchem.worker.WorkerConstants;

/**
 * This class defines actions altering a workflow, i.e., a
 * chain of jobs. Actions are events triggered upon  
 * perception of a situation when evaluating a job. 
 * Since the evaluation itself is an {@link EvaluationJob},
 * there are two jobs an action is related to: the <i>evaluating job</i> (i.e., 
 * an {@link EvaluationJob}), and the <i>evaluated job</i>, a.k.a the 
 * <i>focus job</i>.
 * The action uses the <i>focus job</i> as a reference point 
 * in the workflow on which to operate, so if the <i>focus job</i> is step 
 * <i>i</i> in the workflow, the action can be instructed to operate on step
 * <i>i+/-N</i>.
 * 
 * @author Marco Foscato
 */


//TODO-gg for the moment, action's objects can only in the workflow of the focus
// job, not in that of the evaluating job.
//TODO-gg Should we add the possibility to have the evaluating job (and job in
// its workflow, i.e., +/-N in the evaluating job's workflow or the parent of 
// the evaluating job) as action's object? 
// This could allow to repeat (and modify) a sequence of jobs
// by adding a nest of new steps while flagging the original 
// "sequence of next steps"
// as to-be-skipped.

public class Action implements Cloneable
{
    /**
     * The type of this action.
     */
    private ActionType type = ActionType.SKIP;
    
    /**
     * Job on which we do the action.
     */
    private ActionObject object = ActionObject.FOCUSJOB;
    
    /**
     * Action types define the main feature of the action.
     */
    public enum ActionType {
    	/**
    	 * Requests to re-run the action's object. This may or may not require:
    	 * <ul>
    	 * <li>alteration of the action's object,</li>
    	 * <li>addition of steps before the action's object,</li>
    	 * <li>addition of steps after the action's object.</li>
    	 * </ul>
    	 */
    	REDO, 
    	
    	/**
    	 * Requests to skip the action's object and move to the next step, 
    	 * if any.
    	 */
    	SKIP, 
    	
    	/**
    	 * Requests to stop the action's object. This request is passed to a
    	 * manager of the action's object, if any. 
    	 */
    	STOP
    
    };
    
    /**
     * Possible target of the action defined with respect to the job that has 
     * been evaluated.
     */
    public enum ActionObject {
    	/**
    	 * The job that has been evaluated.
    	 */
    	FOCUSJOB, 
    	
    	/**
    	 * The parent of the job that has been evaluated.
    	 */
    	FOCUSJOBPARENT, 
    	
    	/**
    	 * The job that is step <i>i-1</i> if the job that has been evaluated 
    	 * is step <i>i</i> in a list of jobs.
    	 */
    	PREVIOUSJOB, 
    	
    	/**
    	 * Any job that is parallel to the job that has been evaluated.
    	 */
    	PARALLELJOB, 
    	
    	/**
    	 * The job that is step <i>i+1</i> if the job that has been evaluated 
    	 * is step <i>i</i> in a list of jobs.
    	 */
    	SUBSEQUENTJOB
    };
    
    /**
     * Tasks to perform on action's object jobs
     */
    List<JobEditTask> jobEditTasks = new ArrayList<JobEditTask>();
    
    /**
     * List of job steps to prepend to the action's object job.
     */
    List<Job> preliminarySteps = new ArrayList<Job>();
    
    /**
     * Task to perform to archive previous data from action's object job.
     * Here "archive" means "keep a copy so we do not overwrite previous data".
     */
    List<JobArchiviationTask> jobArchiviationTasks = 
    		new ArrayList<JobArchiviationTask>();
    
    /**
     * List of settings that prepended job steps should inherit from the 
     * action's object job.
     */
    List<JobEditTask> inheritedSettings = new ArrayList<JobEditTask>();
    
    
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
    	this.type = type;
    	this.object = object;
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

    @Deprecated
    public Action(String txt) throws Exception
    {
    	List<String> lines = new ArrayList<String>();
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

    @Deprecated
    public Action(List<String> lines) throws Exception
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
    
    @Deprecated
    private void makeFromLines(List<String> lines) throws Exception 
    {
    	//If needed to parse multiple instances of the same KEY, then use
        //List<List<String>> form = TextAnalyzer.readKeyValue(
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
    			
    		default:
    			throw new Exception("Unable to understand keyword '" + key 
    					+ "' while creation an Action.");
    		}
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Appends a task that edits a feature of the action's object job.
     */
    public void addJobEditingTask(String targetRef, TargetType targetType, 
    		Object newValue)
    {
    	jobEditTasks.add(new JobEditTask(targetRef, targetType, newValue));
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
     * Sets the type of this action.
     */

    public void setType(ActionType type)
    {
        this.type = type;
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
     * Sets the object on whcih to perform this action in relation to the 
     * focus job, i.e., the job that triggered the action.
     */

    public void setObject(ActionObject obj)
    {
        this.object = obj;
    }

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
    	if (o== null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	    
 	   Action other = (Action) o;
 	   
 	    if (this.type!=other.type)
 	    	return false;
 	    
 	   if (this.object!=other.object)
	    	return false;
 	   
 	   if (this.jobEditTasks.size()!=other.jobEditTasks.size())
 		   return false;
 	   
 	   for (int i=0; i<this.jobEditTasks.size(); i++)
 		   if (!this.jobEditTasks.get(i).equals(other.jobEditTasks.get(i)))
 			   return false;
 	   
 	   
 	   return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep copy of this object
     * @return a deep copy
     */
    
    public Action clone()
    {
    	return new Action(type, object);
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
