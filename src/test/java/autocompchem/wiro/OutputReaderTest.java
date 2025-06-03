package autocompchem.wiro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.ACCJob;
import autocompchem.worker.WorkerConstants;

/**
 * Unit Test for the OutputReader class.
 * 
 * @author Marco Foscato
 */
public class OutputReaderTest 
{
    private OutputReader outputReader;
    
    @TempDir
    File tempDir;
    
//------------------------------------------------------------------------------

    @BeforeEach
    public void setUp() 
    {
        outputReader = new OutputReader();
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetStepsFound() 
    {
        // Test that a new OutputReader has no steps found initially
        assertEquals(0, outputReader.getStepsFound(), 
                     "New OutputReader should have 0 steps found");
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetNormalTerminationFlag() 
    {
        // Test that a new OutputReader has not terminated normally initially
        assertEquals(false, outputReader.getNormalTerminationFlag(), 
                     "New OutputReader should not have normal termination flag set");
    }

//------------------------------------------------------------------------------

} 