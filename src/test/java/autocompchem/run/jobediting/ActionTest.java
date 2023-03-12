package autocompchem.run.jobediting;


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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.DataArchivingRule.Type;


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
    	
    	act.addJobEditingTask(new SetDirectiveComponent("*:*|Dir:DirName", 
    			new Keyword("KeyName", false, 1.234)));
    	act.addJobEditingTask(new DeleteJobParameter("NameOfParamToRemove"));
    	act.addJobEditingTask(new SetJobParameter(
    			new NamedData("ParamToSet", "valueOfParam")));
    	
    	act.addJobArchivingDetails(new DataArchivingRule(Type.COPY, "toCp*"));
    	act.addJobArchivingDetails(new DataArchivingRule(Type.DELETE, "toDel*"));
   	 
	   	Job preferinementWorkflow = new Job();
	   	CompChemJob ccj = new CompChemJob();
	   	Directive d = new Directive("GEOM");
	   	d.addKeyword(new Keyword("value", false, 0));
	   	d.addKeyword(new Keyword("format", true, "xyz"));
	   	ccj.addDirective(d);
	   	Directive d2 = new Directive("Opt");
	   	d2.addKeyword(new Keyword("MaxVal", true, 150));
	   	ccj.addDirective(d2);
	   	preferinementWorkflow.addStep(ccj);
	   	CompChemJob ccj2 = new CompChemJob();
	   	Directive d3 = new Directive("GEOM");
	   	d3.addKeyword(new Keyword("value", false, 10));
	   	ccj2.addDirective(d3);
	   	Directive d4 = new Directive("Opt");
	   	d4.addKeyword(new Keyword("MaxVal", true, 300));
	   	ccj2.addDirective(d4);
	   	preferinementWorkflow.addStep(ccj2);
	   	act.addPrerefinementStep(preferinementWorkflow);
	   	
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
    	a2.jobArchivingRules.set(0, new DataArchivingRule(Type.MOVE, "toCp*"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.addJobArchivingDetails(new DataArchivingRule(Type.MOVE, "toCp*"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.inheritedSettings.set(0, new InheritJobParameter("paramName"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.addSettingsInheritedTask(new InheritJobParameter("paramName"));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.prerefinementSteps.set(0, JobFactory.createJob(RunnableAppID.SHELL));
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.prerefinementSteps.add(JobFactory.createJob(RunnableAppID.SHELL));
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
    }
    
//------------------------------------------------------------------------------

}
