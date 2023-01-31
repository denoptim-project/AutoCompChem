package autocompchem.chemsoftware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.worker.TaskID;


public class DirectiveComponentTypeTest
{

//------------------------------------------------------------------------------

    @Test
    public void testGetEnum() throws Exception
    {
    	assertEquals(DirectiveComponentType.DIRECTIVE, 
    			DirectiveComponentType.getEnum("dir"));
    	assertEquals(DirectiveComponentType.DIRECTIVE, 
    			DirectiveComponentType.getEnum("diR"));
    	assertEquals(DirectiveComponentType.KEYWORD, 
    			DirectiveComponentType.getEnum("KEY"));
    	assertEquals(DirectiveComponentType.DIRECTIVEDATA, 
    			DirectiveComponentType.getEnum("dat"));
    	DirectiveComponentType c = DirectiveComponentType.getEnum("invalid");
    	assertNull(c);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testShortForms() throws Exception
    {
    	Set<String> collected = new HashSet<String>();
    	for (DirectiveComponentType cdt : DirectiveComponentType.values())
    	{
    		collected.add(cdt.shortString.toUpperCase());
    	}
    	assertEquals(DirectiveComponentType.values().length, collected.size());
    	assertEquals(DirectiveComponentType.getShortForms(), collected);
    }
    
//------------------------------------------------------------------------------

}
