package autocompchem.run.jobediting;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.SoftwareId;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.Keyword;


/**
 * Unit Test for Action 
 * 
 * @author Marco Foscato
 */

public class ActionTest 
{

//------------------------------------------------------------------------------

    /**
     * Creates an action filled with content meant only for testing.
     */
    public static Action getTestAction()
    {
    	Action act = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
    	
    	act.addJobEditingTask(new AddDirectiveComponent("*:*|Dir:DirName", 
    			new Keyword("KeyName", false, 1.234)));
    	act.addJobEditingTask(new DeleteJobParameter("NameOfParamToRemove"));
    	act.addJobEditingTask(new SetJobParameter(
    			new NamedData("ParamToSet", "valueOfParam")));
    	
    	act.addJobArchivingDetails(new DataArchivingRule(ArchivingTaskType.COPY, "toCp*"));
    	act.addJobArchivingDetails(new DataArchivingRule(ArchivingTaskType.DELETE, "toDel*"));
   	 
	   	Job prerefinementWorkflow = JobFactory.createJob(SoftwareId.ACC);
	   	CompChemJob ccj = new CompChemJob();
	   	Directive d = new Directive("GEOM");
	   	d.addKeyword(new Keyword("value", false, 0));
	   	d.addKeyword(new Keyword("format", true, "xyz"));
	   	ccj.addDirective(d);
	   	Directive d2 = new Directive("Opt");
	   	d2.addKeyword(new Keyword("MaxVal", true, 150));
	   	ccj.addDirective(d2);
	   	prerefinementWorkflow.addStep(ccj);
	   	CompChemJob ccj2 = new CompChemJob();
	   	Directive d3 = new Directive("GEOM");
	   	d3.addKeyword(new Keyword("value", false, 10));
	   	ccj2.addDirective(d3);
	   	Directive d4 = new Directive("Opt");
	   	d4.addKeyword(new Keyword("MaxVal", true, 300));
	   	ccj2.addDirective(d4);
	   	prerefinementWorkflow.addStep(ccj2);
	   	act.addPrerefinementStep(prerefinementWorkflow);
	   	
	   	act.addSettingsInheritedTask(new InheritDirectiveComponent(
   			 DirComponentAddress.fromString("Dir:Inherited|Key:KKK")));
    	
    	return act;
    }
 
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	Action a1 = getTestAction();
    	Action a2 = getTestAction();

    	assertTrue(a1.equals(a2));
    	assertTrue(a2.equals(a1));
    	assertTrue(a1.equals(a1));
    	assertFalse(a1.equals(null));
    	
    	a2 = getTestAction();
    	a2.setType(ActionType.STOP);
    	assertFalse(a1.equals(a2));

    	a2 = getTestAction();
    	a2.setObject(ActionObject.PARALLELJOB);
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.addJobEditingTask(new DeleteJobParameter("different"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.jobEditTasks.set(0, new DeleteJobParameter("different"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.jobArchivingRules.set(0, new DataArchivingRule(ArchivingTaskType.MOVE, "toCp*"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.addJobArchivingDetails(new DataArchivingRule(ArchivingTaskType.MOVE, "toCp*"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.inheritedSettings.set(0, new InheritJobParameter("paramName"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.addSettingsInheritedTask(new InheritJobParameter("paramName"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.prerefinementSteps.set(0, JobFactory.createJob(SoftwareId.SHELL));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.prerefinementSteps.add(JobFactory.createJob(SoftwareId.SHELL));
    	assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	Action act = getTestAction();
    	String json = writer.toJson(act);
    	
    	Action fromJson = reader.fromJson(json, Action.class);
    	assertEquals(act,fromJson);
    	
    	// Case sensitivity of fields. JSON is case sensitive, but we want to
    	// allow some flexibility on the case of the strings meant to represent
    	// enums, so we allow case-insensitive string-like enums.
    	
    	act = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
    	json = writer.toJson(act);
    	json = json.replaceAll(ActionType.REDO.toString(), 
    			ActionType.REDO.toString().toLowerCase());
    	json = json.replaceAll(ActionObject.FOCUSJOB.toString(), 
    			ActionObject.FOCUSJOB.toString().toLowerCase());
    	fromJson = reader.fromJson(json, Action.class);
    	assertEquals(act,fromJson);
    }
    
//------------------------------------------------------------------------------

}
