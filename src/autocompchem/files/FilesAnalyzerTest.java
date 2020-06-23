package autocompchem.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import autocompchem.io.IOtools;


/**
 * Unit Test for text analyzer
 * 
 * @author Marco Foscato
 */

public class FilesAnalyzerTest 
{
    @Test
    public void testCountMatches() throws Exception
    {
        String tmpPathName = "/tmp/__tmp_acc_junit";
        IOtools.writeTXTAppend(tmpPathName,"First line",false);
        IOtools.writeTXTAppend(tmpPathName,"Second line",true);
        IOtools.writeTXTAppend(tmpPathName,"Third line",true);
        
        assertEquals(3,FilesAnalyzer.count(tmpPathName,"line"),"Total matches");
    }

}
