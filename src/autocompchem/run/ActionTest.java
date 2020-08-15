package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;


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

    @Test
    public void testConstructorFromText() throws Exception
    {
    	
    	String str = ActionConstants.TYPEKEY+SEP+" "+ActionType.REDO+NL
    			+ ActionConstants.OBJECTKEY+SEP+" "+ActionObject.PREVIUSJOB+NL
    			+ ActionConstants.STARTMULTILINE+ActionConstants.DETAILSKEY+SEP
    				+ ActionConstants.STARTMULTILINE+"KEEP"+SEP+" optA"+NL
    				+"optB"+NL+ActionConstants.ENDMULTILINE+NL
    				+ ActionConstants.STARTMULTILINE+"CHANGE"+SEP+" optD 123"+NL
    				+"optE 456"+NL+"optF ABC"+ActionConstants.ENDMULTILINE+NL
    			+ActionConstants.ENDMULTILINE;
    	
    	Action a = new Action(str);
    	
    	assertEquals(ActionType.REDO,a.getType(),"ActionType");
    	assertEquals(ActionObject.PREVIUSJOB,a.getObject(),"ActionObject");
    	assertNotNull(a.getDetail("CHANGE"),"Detail exists: Change");
    	assertTrue(a.getDetail("CHANGE").toString().contains("optD 123"),
    			"Detail content: Change-1");
    	assertTrue(a.getDetail("CHANGE").toString().contains("optF ABC"),
    			"Detail content: Change-2");
    	assertEquals(3,a.getDetail("CHANGE").toString().split(NL).length,
    			"Detail content: Change-3");

    	assertNotNull(a.getDetail("KEEP"),"Detail exists: Keep");
    	assertTrue(a.getDetail("KEEP").toString().contains("optA"),
    			"Detail content: Keep-1");
    	assertTrue(a.getDetail("KEEP").toString().contains("optB"),
    			"Detail content: Keep-2");
    	assertEquals(2,a.getDetail("KEEP").toString().split(NL).length,
    			"Detail content: Keep-3");
    	
    }
    
//------------------------------------------------------------------------------

}
