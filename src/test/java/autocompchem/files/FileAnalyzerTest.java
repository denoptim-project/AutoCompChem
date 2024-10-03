package autocompchem.files;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;


/**
 * Unit Test for text analyzer
 * 
 * @author Marco Foscato
 */

public class FileAnalyzerTest 
{
    @TempDir 
    File tempDir;
    
    public static String NL = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------

    @Test
    public void testCountMatches() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        String tmpPathName = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "tmp.txt";
        File tmpFile = new File(tmpPathName);
        IOtools.writeLineAppend(tmpFile, "First line #", false);
        IOtools.writeLineAppend(tmpFile, "Second line #", true);
        IOtools.writeLineAppend(tmpFile, "Third line #", true);
        IOtools.writeLineAppend(tmpFile, "last", true);
        
        assertEquals(3,FileAnalyzer.count(tmpFile,"line #"),
        		"Total matches");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFileTypeByProbing() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String tmpPathName = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "fiel.json";
        Gson writer = ACCJson.getWriter();
        File tmpFile = new File(tmpPathName);
        IOtools.writeTXTAppend(tmpFile, writer.toJson(
        		new ArrayList<String>(Arrays.asList("A", "222", "t h i r d"))),
        		false);
    	
    	assertEquals(ACCFileType.JSON,
    			FileAnalyzer.detectFileType(tmpFile));
    }

//------------------------------------------------------------------------------

}
