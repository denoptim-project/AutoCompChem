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
import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.Constraint;
import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.run.jobediting.JobEditTask;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.EditTask.EditingTask;
import autocompchem.run.jobediting.JobEditTask.TargetType;


/**
 * Unit Test for job editing tasks 
 * 
 * @author Marco Foscato
 */

public class SetDirComponentValueTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	DirComponentAddress path = new DirComponentAddress();
    	path.addStep("*","Dir");
    	path.addStep("E","*");
    	path.addStep("KeyName","key");
    	SetDirComponentValue sdc1 = new SetDirComponentValue(path, "newValue");
    	DirComponentAddress path2 = new DirComponentAddress();
    	path2.addStep("*","dir");
    	path2.addStep("E","*");
    	path2.addStep("KeyName","Key");
    	SetDirComponentValue sdc2 = new SetDirComponentValue(path2, "newValue");

    	assertTrue(sdc1.equals(sdc2));
    	assertTrue(sdc2.equals(sdc1));
    	assertTrue(sdc1.equals(sdc1));
    	assertFalse(sdc1.equals(null));
    	
    	DirComponentAddress path3 = new DirComponentAddress();
    	path3.addStep("My","dir");
    	sdc2 = new SetDirComponentValue(path3, "newValue");
    	assertFalse(sdc1.equals(sdc2));
    	
    	sdc2 = new SetDirComponentValue(path, "otherVal");
    	assertFalse(sdc1.equals(sdc2));
    }
    
//------------------------------------------------------------------------------

}
