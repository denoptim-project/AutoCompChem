package autocompchem.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;


/**
 * Unit Test for I/O tools
 * 
 * @author Marco Foscato
 */

public class IOtoolsTest 
{
    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromString() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String inFile = tempDir.getAbsolutePath() + SEP + "in.txt";
        String outFile = tempDir.getAbsolutePath() + SEP + "out.txt";
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<30; i++) 
        {
        	sb.append("Line "+i+NL);
        }
        IOtools.writeTXTAppend(inFile, sb.toString(), false);
        
        IOtools.copyPortionOfTxtFile(inFile, outFile, 6, 26);
        
        assertTrue((new File(outFile)).exists(),"Out file created.");
        assertEquals(1,FileAnalyzer.count(outFile, "Line 6"),"Found start.");
        assertEquals(1,FileAnalyzer.count(outFile, "Line 26"),"Found end.");
        assertEquals(0,FileAnalyzer.count(outFile, "Line 5"),
        		"Text before start is not included.");
        assertEquals(0,FileAnalyzer.count(outFile, "Line 27"),
        		"Text after end is not included.");
    }
    
//------------------------------------------------------------------------------

}
