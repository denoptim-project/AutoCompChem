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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    
//------------------------------------------------------------------------------

    @Test
    public void testCountMatches() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        String tmpPathName = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "tmp.txt";
        IOtools.writeTXTAppend(tmpPathName,"First line #",false);
        IOtools.writeTXTAppend(tmpPathName,"Second line #",true);
        IOtools.writeTXTAppend(tmpPathName,"Third line #",true);
        IOtools.writeTXTAppend(tmpPathName,"last",true);
        
        assertEquals(3,FileAnalyzer.count(tmpPathName,"line #"),
        		"Total matches");
    }

//------------------------------------------------------------------------------

}
