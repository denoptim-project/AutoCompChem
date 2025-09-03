package autocompchem.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
      	List<InputStream> allStreams = ResourcesTools.getAllResourceStreams("tree");
      	assertEquals(4, allStreams.size());
      	
      	Set<String> expectedContent = Set.of(
      			"content of leaf-0\n", 
      			"content of leaf-a1\n", 
      			"content of leaf-b1\n", 
      			"content of leaf-aa2\n");
      	for (InputStream is : allStreams)
      	{
      		String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      		assertTrue(expectedContent.contains(content));
      	}
    }
    
//------------------------------------------------------------------------------

}
