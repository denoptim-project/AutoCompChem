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

    private final String NL = System.getProperty("line.separator");
    private final String SEP = ActionConstants.SEPARATOR;

//------------------------------------------------------------------------------

    /**
     * Creates an action filled with content meant only for testing.
     */
    public Action getTestAction()
    {
    	Action act = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
    	act.addJobEditingTask("TrgProp", TargetType.PARAMETER, "NEWVALUE");
    	act.addJobEditingTask("TrgProp2", TargetType.PARAMETER, "OTHER");
    	act.addJobEditingTask("TrgKey", TargetType.DIRECTIVECOMPONENT, "NEWKEY");
    	act.addJobEditingTask("TrgDD", TargetType.DIRECTIVECOMPONENT, "NEWDD");
    	
    	return act;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromText() throws Exception
    {
    	
    	String str = ActionConstants.TYPEKEY+SEP+" "+ActionType.REDO+NL
    			+ ActionConstants.OBJECTKEY+SEP+" "+ActionObject.PREVIOUSJOB+NL;
    	
    	Action a = new Action(str);
    	
    	assertEquals(ActionType.REDO,a.getType(),"ActionType");
    	assertEquals(ActionObject.PREVIOUSJOB,a.getObject(),"ActionObject");
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
    	a2.addJobEditingTask("New", TargetType.PARAMETER, "neVal");
    	assertFalse(a1.equals(a2));
    	
    	a2 = getTestAction();
    	a2.jobEditTasks.set(0, 
    			new JobEditTask("bb",TargetType.PARAMETER,"blabla"));
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
    	
    	Action fromJson = reader.fromJson(json, Action.class);
    	assertEquals(act,fromJson);
    	
    	//TODO-gg del
    	System.out.println(json);
    }
    
//------------------------------------------------------------------------------

}
