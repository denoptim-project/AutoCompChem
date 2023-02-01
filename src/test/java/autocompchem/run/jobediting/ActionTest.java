package autocompchem.run.jobediting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import autocompchem.chemsoftware.DirComponentAddress;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData;
import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.run.ActionConstants;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.JobEditTask;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.JobEditTask.TargetType;


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
    public Action getTestAction()
    {
    	Action act = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
    	act.addJobEditingTask(new SetKeyword("*:*|Dir:DirName", 
    			new Keyword("KeyName", false, 1.234)));
    	act.addJobEditingTask(new DeleteJobParameter("NameOfParamToRemove"));
    	act.addJobEditingTask(new SetJobParameter(
    			new NamedData("ParamToSet", "valueOfParam")));
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
    	
    	//TODO-gg add clauses comparing other fields
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	Action act = getTestAction();
    	String json = writer.toJson(act);

    	//TODO-gg del
    	System.out.println(json);
    	
    	Action fromJson = reader.fromJson(json, Action.class);
    	assertEquals(act,fromJson);
    }
    
//------------------------------------------------------------------------------

}
