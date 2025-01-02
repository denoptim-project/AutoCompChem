package autocompchem.run;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileAnalyzer;


/**
 * Unit Test for the runner of embarrassingly parallel sets of jobs. 
 * 
 * @author Marco Foscato
 */

public class TestJobTest 
{
    private final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    protected File tempDir;
    
//-----------------------------------------------------------------------------

    /*
     * Only meant to test the private class TestJob
     */

    @Test
    public void testLoggingOfTestJob() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String log = tempDir.getAbsolutePath() + SEP + "testjob.log";
    	Job job = new TestJob(log, 1, 80, 99, false);
    	job.run();
    	
    	int n = FileAnalyzer.count(log, TestJob.ITERATIONKEY+"*");
    	assertTrue(n>8, "Found " + n + " instead of x>8");
    	assertTrue(n<12, "Found " + n + " instead of x<12");
    	assertFalse(job.isInterrupted);
    	assertTrue(job.isCompleted());
    }
    
//------------------------------------------------------------------------------

}
