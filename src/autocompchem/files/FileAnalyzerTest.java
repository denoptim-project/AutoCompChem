package autocompchem.files;

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
        
        assertEquals(3,FileAnalyzer.count(tmpPathName,"line #"),"Total matches");
    }

}
