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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParseException;



/**
 * Unit Test for the {@link Worker}
 * 
 * @author Marco Foscato
 */

public class WorkerTest 
{
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetKnownParameters_fromFile() throws Exception
    {
    	List<ConfigItem> knownInput = Worker.getKnownParameters(
    			"inputdefinition/DummyFileForUnitTesting.json");
    	
    	assertEquals(2, knownInput.size());
    	
    	assertEquals(1, knownInput.stream()
    			.filter(i -> i.key != null)
    			.filter(i -> i.key.equals("INFILE"))
    			.count());
    	assertEquals(1, knownInput.stream()
    			.filter(i -> i.key != null)
    			.filter(i -> i.key.equals("OUTFILE"))
    			.count());
    	
    	ConfigItem ci = knownInput.get(0);
    	assertEquals(ci.key, "INFILE");
    	assertEquals(ci.casedKey, "inFile");
    	assertEquals(ci.type, "SomeType");
    	assertTrue(ci.doc.contains("input"));
    	assertEquals(3, ci.doc.split("\\n").length);
    	assertEquals(ci.embeddedWorker, "SomeWorker");
    	assertTrue(ci.tag.contains("ddaattaa"));
    	assertTrue(ci.isForStandalone());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetKnownParameters_wrongCase() throws Exception
    {
        assertThrows(JsonParseException.class, 
                () -> Worker.getKnownParameters(
    			"inputdefinition/DummyFileForUnitTestingWrongCase.json"));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetKnownParameters_wrongKey() throws Exception
    {
        assertThrows(JsonParseException.class, 
                () -> Worker.getKnownParameters(
    			"inputdefinition/DummyFileForUnitTestingWrongKey.json"));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetKnownParameters() throws Exception
    {
    	DummyWorker2 w = new DummyWorker2();
    	
    	List<ConfigItem> knownInput = w.getKnownParameters();
    	
    	assertEquals(4, knownInput.size());
    	
    	assertEquals(1, knownInput.stream()
    			.filter(i -> i.key != null)
    			.filter(i -> i.key.equals("INFILE"))
    			.count());
    	
    	ConfigItem ci = knownInput.get(0);
    	assertEquals("The pathname to the file to read as input.", ci.doc);
    	ci = knownInput.get(2);
    	assertEquals(3, ci.doc.split("\\n").length);
    	
    	ConfigItem ci_standalone = knownInput.get(1);
    	assertTrue(ci_standalone.isForStandalone());
    }

//------------------------------------------------------------------------------

}
