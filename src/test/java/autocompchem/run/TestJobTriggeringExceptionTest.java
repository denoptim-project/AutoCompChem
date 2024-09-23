package autocompchem.run;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit Test for the {@link TestJobTriggeringException}
 * 
 * @author Marco Foscato
 */

public class TestJobTriggeringExceptionTest 
{
    
//-----------------------------------------------------------------------------

    /*
     * Only meant to test the private class TestJob
     */

    @Test
    public void testTestJobTriggeringException() throws Exception
    {
    	boolean triggered = false;
    	try {
			Job job = new TestJobTriggeringException();
			job.run();
		} catch (Throwable t) {
			triggered = true;
			//NB: we catch the Error that is caused by the Exception that is
			// thrown by the job
			assertEquals(TestJobTriggeringException.MSG, 
					t.getCause().getMessage());
		}
    	assertTrue(triggered);
    }
    
//------------------------------------------------------------------------------

}
