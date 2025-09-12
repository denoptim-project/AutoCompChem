package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import autocompchem.datacollections.NamedData;
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.SetDirectiveComponent;
import autocompchem.run.jobediting.SetJobParameter;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.DirectiveComponentType;
import autocompchem.wiro.chem.Keyword;


/**
 * Unit Test 
 * 
 * @author Marco Foscato
 */

public class JobAssistantTest 
{
    
//-----------------------------------------------------------------------------

    @Test
    public void testHealJob() throws Exception
    {	
    	Job jobToHeal = new ACCJob();
    	jobToHeal.setParameter("PARAM_1", "value_1");
    	jobToHeal.setParameter("PARAM_2", "value_2");
    	
    	// Nothing dome more than embedding the step into a workflow
    	
    	Action cure = new Action(ActionType.REDO, ActionObject.FOCUSJOB);
    	
    	Job healedJob = JobAssistant.healJob(jobToHeal, cure, 0, null, null);
    	
    	assertNotNull(healedJob);
    	assertEquals(1, healedJob.getNumberOfSteps());
    	assertEquals("value_1", healedJob.getStep(0).getParameter("PARAM_1").getValue());
    	assertEquals("value_2", healedJob.getStep(0).getParameter("PARAM_2").getValue());
    	
    	// This time the action should alter the parameters
    	jobToHeal = new ACCJob();
    	jobToHeal.setParameter("PARAM_1", "value_1");
    	jobToHeal.setParameter("PARAM_2", "value_2");
    	cure.addJobEditingTask(new SetJobParameter(
    			new NamedData("PARAM_2", "NEW_VALUE")));
    	
    	healedJob = JobAssistant.healJob(jobToHeal, cure, 0, null, null);
    	
    	assertNotNull(healedJob);
    	assertEquals(1, healedJob.getNumberOfSteps());
    	assertEquals("value_1", healedJob.getStep(0).getParameter("PARAM_1").getValue());
    	assertEquals("NEW_VALUE", healedJob.getStep(0).getParameter("PARAM_2").getValue());
    	assertEquals(2, healedJob.getStep(0).getParameters().size());
    	
    	// This time the action should alter the directives
    	jobToHeal = new CompChemJob();
    	jobToHeal.setParameter("PARAM_1", "value_1");
    	jobToHeal.setParameter("PARAM_2", "value_2");
    	DirComponentAddress address = new DirComponentAddress();
    	address.addStep("DirA", DirectiveComponentType.DIRECTIVE);
    	address.addStep("DirB", DirectiveComponentType.DIRECTIVE);
    	cure.addJobEditingTask(new SetDirectiveComponent(address, 
    			new Keyword("NEW_KEY", false, "keyword_value")));
    	
    	healedJob = JobAssistant.healJob(jobToHeal, cure, 0, null, null);
    	
    	assertNotNull(healedJob);
    	assertEquals(1, healedJob.getNumberOfSteps());
    	CompChemJob wrappedJob = (CompChemJob) healedJob.getStep(0);
    	assertEquals("value_1", wrappedJob.getParameter("PARAM_1").getValue());
    	assertEquals("NEW_VALUE", wrappedJob.getParameter("PARAM_2").getValue());
    	assertEquals(2, wrappedJob.getParameters().size());
    	address.addStep("NEW_KEY", DirectiveComponentType.KEYWORD);
    	assertTrue(wrappedJob.hasDirectiveStructure(address));
    }
    
//------------------------------------------------------------------------------

}
