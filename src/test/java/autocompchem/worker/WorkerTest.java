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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParseException;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.ACCJob;
import autocompchem.run.Job;



/**
 * Unit Test for the {@link Worker}
 * 
 * @author Marco Foscato
 */

public class WorkerTest 
{
    private TestableWorker testableWorker;
    
    @TempDir
    File tempDir;
    
    /**
     * Testable subclass of Worker that exposes the protected 
     * resolvePathname method for testing.
     */
    private static class TestableWorker extends DummyWorker 
    {
        // Expose the protected method for testing
        public String testResolvePathname(String pathname) 
        {
            return resolvePathname(pathname);
        }
        
        // Allow setting the job for testing
        public void setJobForTesting(ACCJob job) 
        {
            this.myJob = job;
        }
    }
    
//------------------------------------------------------------------------------

    @BeforeEach
    public void setUp() 
    {
        testableWorker = new TestableWorker();
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
    public void testWorkDirectoryHandling(@TempDir File tempDir) throws Exception
    {
        // Create a test work directory
        File workDir = new File(tempDir, "test-workdir");
        
        // Create parameters with WORKDIR parameter
        ParameterStorage params = new ParameterStorage();
        params.setParameter(WorkerConstants.PARTASK, DummyWorker.DUMMYTASKTASKNAME);
        params.setParameter(WorkerConstants.PARWORKDIR, workDir.getAbsolutePath());
        
        // Create a job and worker
        Job job = new ACCJob(params);
        DummyWorker worker = new DummyWorker();
        worker.myJob = job;
        worker.setParameters(params);
        
        // Initialize the worker (this should create the work directory)
        worker.initialize();
        
        // Verify the work directory was created
        assertTrue(workDir.exists(), "Work directory should be created");
        assertTrue(workDir.isDirectory(), "Work directory should be a directory");
        
        // Verify the job's work directory was set
        assertNotNull(job.getUserDir(), "Job should have a work directory set");
        assertEquals(workDir.getAbsolutePath(), job.getUserDir().getAbsolutePath(), 
                     "Job work directory should match the specified directory");
        
        // Verify worker can access work directory via utility methods
        assertNotNull(worker.getWorkDirectory(), "Worker should return work directory");
        assertEquals(workDir.getAbsolutePath(), worker.getWorkDirectory().getAbsolutePath(),
                     "Worker work directory should match job work directory");
        assertEquals(workDir.getAbsolutePath(), worker.getWorkDirectoryPath(),
                     "Worker work directory path should match expected path");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithNullInput() 
    {
        String result = testableWorker.testResolvePathname(null);
        assertNull(result, "Null input should return null");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithAbsolutePath() 
    {
        String absolutePath = "/absolute/path/to/file.txt";
        String result = testableWorker.testResolvePathname(absolutePath);
        assertEquals(absolutePath, result, "Absolute path should be returned unchanged");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithRelativePathNoWorkDir() 
    {
        String relativePath = "relative/path/file.txt";
        String result = testableWorker.testResolvePathname(relativePath);
        assertEquals(relativePath, result, 
                     "Relative path should be unchanged when no work directory is set");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithRelativePathAndWorkDir() throws Exception
    {
        // Set up a work directory
        File workDir = new File(tempDir, "workdir");
        workDir.mkdirs();
        
        // Create parameters with just the task (no specific file needed for this test)
        ParameterStorage params = new ParameterStorage();
        params.setParameter(WorkerConstants.PARTASK, DummyWorker.DUMMYTASKTASKNAME);
        
        // Create a job and manually set its user directory
        ACCJob job = new ACCJob(params);
        job.setUserDirAndStdFiles(workDir);
        
        // Associate the job with the worker
        testableWorker.setJobForTesting(job);
        
        // Test relative path resolution
        String relativePath = "subdir/file.txt";
        String result = testableWorker.testResolvePathname(relativePath);
        
        File expectedFile = new File(workDir, relativePath);
        assertEquals(expectedFile.getPath(), result, 
                     "Relative path should be resolved against work directory");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithDotRelativePath() throws Exception
    {
        // Set up a work directory
        File workDir = new File(tempDir, "workdir");
        workDir.mkdirs();
        
        // Create parameters with just the task
        ParameterStorage params = new ParameterStorage();
        params.setParameter(WorkerConstants.PARTASK, DummyWorker.DUMMYTASKTASKNAME);
        
        // Create a job and manually set its user directory
        ACCJob job = new ACCJob(params);
        job.setUserDirAndStdFiles(workDir);
        
        // Associate the job with the worker
        testableWorker.setJobForTesting(job);
        
        // Test paths starting with ./
        String relativePath = "./data/input.sdf";
        String result = testableWorker.testResolvePathname(relativePath);
        
        File expectedFile = new File(workDir, relativePath);
        assertEquals(expectedFile.getPath(), result, 
                     "Relative path with ./ should be resolved against work directory");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithParentDirPath() throws Exception
    {
        // Set up a work directory
        File workDir = new File(tempDir, "workdir");
        workDir.mkdirs();
        
        // Create parameters with just the task
        ParameterStorage params = new ParameterStorage();
        params.setParameter(WorkerConstants.PARTASK, DummyWorker.DUMMYTASKTASKNAME);
        
        // Create a job and manually set its user directory
        ACCJob job = new ACCJob(params);
        job.setUserDirAndStdFiles(workDir);
        
        // Associate the job with the worker
        testableWorker.setJobForTesting(job);
        
        // Test paths with parent directory references
        String relativePath = "../other/file.txt";
        String result = testableWorker.testResolvePathname(relativePath);
        
        File expectedFile = new File(workDir, relativePath);
        assertEquals(expectedFile.getPath(), result, 
                     "Relative path with ../ should be resolved against work directory");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWithEmptyString() 
    {
        String emptyPath = "";
        String result = testableWorker.testResolvePathname(emptyPath);
        assertEquals(emptyPath, result, "Empty string should be returned unchanged");
    }

//------------------------------------------------------------------------------

    @Test
    public void testResolvePathnameWindowsAbsolutePath() 
    {
        String windowsAbsolutePath = "C:\\Windows\\System32\\file.txt";
        String result = testableWorker.testResolvePathname(windowsAbsolutePath);
        assertEquals(windowsAbsolutePath, result, 
                     "Windows absolute path should be returned unchanged");
    }

//------------------------------------------------------------------------------

}
