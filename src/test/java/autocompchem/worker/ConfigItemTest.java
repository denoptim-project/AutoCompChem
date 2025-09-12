package autocompchem.worker;


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



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import autocompchem.io.ACCJson;



/**
 * Unit Test for the {@link ConfigItem}
 * 
 * @author Marco Foscato
 */

public class ConfigItemTest 
{
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetStringForHelpMsg() throws Exception
    {
    	ClassLoader classLoader = getClass().getClassLoader();
    	Gson reader = ACCJson.getReader();
    	List<ConfigItem> knownParams = new ArrayList<ConfigItem>();
        InputStream ins = classLoader.getResourceAsStream("testConfigItems.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(ins));
        knownParams = reader.fromJson(br,
        		new TypeToken<List<ConfigItem>>(){}.getType());
        br.close();
		
        assertEquals(2, knownParams.size());
        
    	String str = knownParams.get(0).getStringForHelpMsg();
    	
    	String[] lines = str.split("[\\r\\n]+");
    	assertEquals(12, lines.length);
    	
    	for (int i=0; i<lines.length; i++)
    	{
    		assertTrue(lines[i].length() < ConfigItem.MAXLINELENGTH, 
    				"Line is "+lines[i].length());
    	}
    }

//------------------------------------------------------------------------------

}
