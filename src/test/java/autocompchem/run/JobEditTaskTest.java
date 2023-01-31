package autocompchem.run;

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
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;
import autocompchem.run.JobEditTask.TargetType;


/**
 * Unit Test for job editing tasks 
 * 
 * @author Marco Foscato
 */

public class JobEditTaskTest 
{

//------------------------------------------------------------------------------

    /**
     * Creates an action filled with content meant only for testing.
     */
    public JobEditTask getTestJobEditingTask()
    {
    	JobEditTask jet = new JobEditTask("TrgProp", TargetType.PARAMETER, 
    			"NEWVALUE");
    	return jet;
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	JobEditTask jet1 = getTestJobEditingTask();
    	JobEditTask jet2 = getTestJobEditingTask();

    	assertTrue(jet1.equals(jet2));
    	assertTrue(jet2.equals(jet1));
    	assertTrue(jet1.equals(jet1));
    	assertFalse(jet1.equals(null));
    	
    	jet2 = new JobEditTask("blabla", jet1.targetType, jet1.newValue);
    	assertFalse(jet1.equals(jet2));
    	
    	jet2 = new JobEditTask(jet1.targetRef, TargetType.DIRECTIVECOMPONENT, 
    			jet1.newValue);
    	assertFalse(jet1.equals(jet2));
    	
    	jet2 = new JobEditTask(jet1.targetRef, jet1.targetType, "blabla");
    	assertFalse(jet1.equals(jet2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	JobEditTask act = getTestJobEditingTask();
    	String json = writer.toJson(act);
    	
    	JobEditTask fromJson = reader.fromJson(json, JobEditTask.class);
    	assertEquals(act,fromJson);
    	
    	//TODO-gg del
    	System.out.println("JobEditTask:");
    	System.out.println(json);
    }
    
//------------------------------------------------------------------------------

}
