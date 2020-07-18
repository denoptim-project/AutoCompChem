package autocompchem.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import autocompchem.datacollections.ParameterStorage;

/**
 * Unit Test for methods in ACCMain. 
 * 
 * @author Marco Foscato
 */

public class ACCMainTest 
{
    
//------------------------------------------------------------------------------

    @Test
    public void testCLIArgsParsing() throws Exception
    {
    	String[] args = {"-00", "-t", "DummyTask", 
    			"--input", "~/path/input.in", 
    			"-o", "file.out",
    			"--long", "\"many", "words", "all", "quoted\"",
    			"-z", "-z2"};
    	ParameterStorage params = new ParameterStorage();
    	ACCMain.parseCLIArgs(args, params);

    	assertTrue(params.contains("long"),"Parsed long and quoted option.");
    	assertEquals(4,params.getParameter("long").getValue().toString()
    			.split("\\s+").length,"Length of long and quoted option.");

    	assertEquals("file.out",params.getParameter("o").getValue().toString(),
    			"Value of option '-o'.");
    	assertTrue(params.contains("00"),"Parsed value-less option (first)");
    	assertTrue(params.contains("z2"),"Parsed value-less option (last)");
    	
    	
    }

//------------------------------------------------------------------------------

}
