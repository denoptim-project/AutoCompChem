package autocompchem.run.jobediting;

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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;

/**
 * This class defines actions altering a workflow, i.e., a
 * chain of jobs. Actions are typically events triggered upon  
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


//TODO-gg for the moment, action's objects can only operate in the workflow of the focus
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
    
    //--------------------------------------------------------------------------
  	
  	public static class ActionTypeDeserializer 
  	implements JsonDeserializer<ActionType>
  	{
		@Override
		public ActionType deserialize(JsonElement json, 
				java.lang.reflect.Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
			// JSON is case sensitive, but we want to
	    	// allow some flexibility on the case of the strings meant to represent
	    	// enums, so we allow case-insensitive string-like enums.
			return ActionType.valueOf(json.getAsString().toUpperCase());
		}
  	}
  	
    //--------------------------------------------------------------------------
  	
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
    
    //--------------------------------------------------------------------------
  	
  	public static class ActionObjectDeserializer 
  	implements JsonDeserializer<ActionObject>
  	{
		@Override
		public ActionObject deserialize(JsonElement json, 
				java.lang.reflect.Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
			// JSON is case sensitive, but we want to
	    	// allow some flexibility on the case of the strings meant to represent
	    	// enums, so we allow case-insensitive string-like enums.
			return ActionObject.valueOf(json.getAsString().toUpperCase());
		}
  	}
  	
    //--------------------------------------------------------------------------
  	
    
    /**
     * Tasks to perform on action's object jobs
     */
    List<IJobEditingTask> jobEditTasks = new ArrayList<IJobEditingTask>();
    
    /**
     * List of job steps to pre-pend (i.e., append before) to the action's 
     * object job.
     */
    List<Job> prerefinementSteps = new ArrayList<Job>();
    
    /**
     * Details on how to archive previous data from action's object job.
     * Here "archive" means "keep a copy so we do not overwrite previous data",
     * and possibly remove unneeded data.
     */
    List<DataArchivingRule> jobArchivingRules = 
            new ArrayList<DataArchivingRule>();
    
    /**
     * List of settings that prepended job steps should inherit from the 
     * action's object job.
     */
    List<IJobSettingsInheritTask> inheritedSettings = 
            new ArrayList<IJobSettingsInheritTask>();
    
    
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
     * Appends a task that edits a feature of the action's object job.
     */
    public void addJobEditingTask(IJobEditingTask jet)
    {
        jobEditTasks.add(jet);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Appends a step in the pre-restart data-refinement workflow. Such workflow
     * is performed before restarting the original workflow (i.e., before 
     * trying to re-run the step that failed in the original workflow) 
     * that triggered this
     * action: it takes the input that is available at restart time, and 
     * typically tries to refine it to improve the performance of the original 
     * workflow.
     * @param prerefinementStep the job to append to the list of pre-refinement 
     * steps.
     */
    public void addPrerefinementStep(Job prerefinementStep)
    {
        prerefinementSteps.add(prerefinementStep);
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends data archiving details, i.e., rules defining what to do with 
     * files found in the job's directory when performing the action. 
     * Typically, we
     * want to archive some data (i.e., keep a copy to avoid overwrite data from
     * previous runs of the job), copy (i.e., keep a snapshot of previous data),
     * or delete previous data.
     */
    public void addJobArchivingDetails(DataArchivingRule jad)
    {
        jobArchivingRules.add(jad);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Append a rule defining which settings to copy from the original
     * workflow into the pre-refinement workflow performed upon restarting
     * the original workflow from any step that triggered this action.
     */
    public void addSettingsInheritedTask(IJobSettingsInheritTask sit)
    {
        inheritedSettings.add(sit);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Collects all the patterns for filenames for a given type of data 
     * archiving task.
     * @return the list of patterns.
     */
    public Set<String> getFilenamePatterns(ArchivingTaskType type)
    {
        return jobArchivingRules.stream()
            .filter(r -> r.getType().equals(type))
            .map(r -> r.getPattern())
            .collect(Collectors.toSet());
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
     * Sets the object on which to perform this action in relation to the 
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
        if (o == null)
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
        
        if (this.jobArchivingRules.size()!=other.jobArchivingRules.size())
            return false;
        
        for (int i=0; i<this.jobArchivingRules.size(); i++)
            if (!this.jobArchivingRules.get(i).equals(
                    other.jobArchivingRules.get(i)))
                return false;
        
        if (this.inheritedSettings.size()!=other.inheritedSettings.size())
            return false;
        
        for (int i=0; i<this.inheritedSettings.size(); i++)
            if (!this.inheritedSettings.get(i).equals(
                   other.inheritedSettings.get(i)))
                return false;
        
        if (this.prerefinementSteps.size()!=other.prerefinementSteps.size())
            return false;
        
        for (int i=0; i<this.prerefinementSteps.size(); i++)
            if (!this.prerefinementSteps.get(i).equals(
                    other.prerefinementSteps.get(i)))
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
