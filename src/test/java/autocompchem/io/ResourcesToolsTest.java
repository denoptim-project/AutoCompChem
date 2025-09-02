package autocompchem.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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


import org.junit.jupiter.api.Test;


/**
 * Unit Test for I/O tools
 * 
 * @author Marco Foscato
 */

public class ResourcesToolsTest 
{
	
//------------------------------------------------------------------------------

    @Test
    public void testGetAllResources() throws Exception
    {
    	ClassLoader cl = getClass().getClassLoader();
    	List<String> allLeaves = ResourcesTools.getAllResources(cl, "tree");
    	assertEquals(3, allLeaves.size());
    	assertTrue(allLeaves.contains("leaf-0"));
    	assertTrue(allLeaves.contains("branch-a/leaf-a1"));
    	assertTrue(allLeaves.contains("branch-b/leaf-b1"));
    }
    
//------------------------------------------------------------------------------

}
