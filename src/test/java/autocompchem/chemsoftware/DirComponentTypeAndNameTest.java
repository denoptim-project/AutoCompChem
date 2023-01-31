package autocompchem.chemsoftware;

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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.worker.TaskID;


public class DirComponentTypeAndNameTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	DirComponentTypeAndName d1 = new DirComponentTypeAndName("myName",
    			DirectiveComponentType.DIRECTIVEDATA);
    	DirComponentTypeAndName d2 = new DirComponentTypeAndName("myName",
    			DirectiveComponentType.DIRECTIVEDATA);

    	assertTrue(d1.equals(d1));
    	assertTrue(d1.equals(d2));
    	assertTrue(d2.equals(d1));
    	assertFalse(d1.equals(null));
    	
    	d2 = new DirComponentTypeAndName("myNameDifferent",
    			DirectiveComponentType.DIRECTIVEDATA);
    	assertFalse(d1.equals(d2));

    	d2 = new DirComponentTypeAndName("myName",
    			DirectiveComponentType.KEYWORD);
    	assertFalse(d1.equals(d2));

    	d2 = new DirComponentTypeAndName("*",
    			DirectiveComponentType.ANY);
    	assertFalse(d1.equals(d2));
    }
    
//------------------------------------------------------------------------------

}
