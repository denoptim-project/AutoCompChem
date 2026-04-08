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



import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParseException;

import autocompchem.datacollections.ParameterStorage;



/**
 * Unit Test for the {@link Worker}
 * 
 * @author Marco Foscato
 */

public class WorkerTest 
{
    @TempDir
    File tempDir;

    private static final String SEP = System.getProperty("file.separator");

    /**
     * Creates a {@link DummyWorker} with the given parameters, plus the
     * mandatory TASK parameter needed to call {@link Worker#setParameters}.
     */
    private DummyWorker makeWorker(ParameterStorage extra)
    {
        DummyWorker w = new DummyWorker();
        ParameterStorage ps = new ParameterStorage();
        ps.setParameter(WorkerConstants.PARTASK, DummyWorker.DUMMYTASKTASK.casedID);
        if (extra != null)
        {
            for (String key : extra.getRefNamesSet())
            {
                ps.setParameter(extra.getParameter(key));
            }
        }
        w.setParameters(ps);
        return w;
    }

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
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_nullFile()
    {
        DummyWorker w = makeWorker(null);
        assertThrows(IllegalArgumentException.class,
                () -> w.manageExistingOutputFile(null));
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_nonExistingFile()
    {
        DummyWorker w = makeWorker(null);
        File absent = new File(tempDir, "absent.txt");
        assertDoesNotThrow(() -> w.manageExistingOutputFile(absent));
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_existingFile_noOverwrite() throws Exception
    {
        DummyWorker w = makeWorker(null);
        File existing = new File(tempDir, "existing.txt");
        existing.createNewFile();
        assertThrows(IllegalStateException.class,
                () -> w.manageExistingOutputFile(existing));
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_existingFile_overwriteTrue() throws Exception
    {
        ParameterStorage extra = new ParameterStorage();
        extra.setParameter(WorkerConstants.PAROVERWRITEOUTPUT, "true");
        DummyWorker w = makeWorker(extra);
        File existing = new File(tempDir, "existing_ow.txt");
        existing.createNewFile();
        assertDoesNotThrow(() -> w.manageExistingOutputFile(existing));
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_existingFile_overwriteYes() throws Exception
    {
        ParameterStorage extra = new ParameterStorage();
        extra.setParameter(WorkerConstants.PAROVERWRITEOUTPUT, "yes");
        DummyWorker w = makeWorker(extra);
        File existing = new File(tempDir, "existing_yes.txt");
        existing.createNewFile();
        assertDoesNotThrow(() -> w.manageExistingOutputFile(existing));
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_existingFile_overwriteValueless() throws Exception
    {
        // Value-less parameter (null value) is treated as true by StringUtils.parseBoolean(..., true)
        ParameterStorage extra = new ParameterStorage();
        extra.setParameter(WorkerConstants.PAROVERWRITEOUTPUT);
        DummyWorker w = makeWorker(extra);
        File existing = new File(tempDir, "existing_kw.txt");
        existing.createNewFile();
        assertDoesNotThrow(() -> w.manageExistingOutputFile(existing));
    }

//------------------------------------------------------------------------------

    @Test
    public void testManageExistingOutputFile_existingFile_overwriteFalse() throws Exception
    {
        ParameterStorage extra = new ParameterStorage();
        extra.setParameter(WorkerConstants.PAROVERWRITEOUTPUT, "false");
        DummyWorker w = makeWorker(extra);
        File existing = new File(tempDir, "existing_no.txt");
        existing.createNewFile();
        assertThrows(IllegalStateException.class,
                () -> w.manageExistingOutputFile(existing));
    }

//------------------------------------------------------------------------------

}
