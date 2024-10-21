package autocompchem.wiro.chem.orca;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.wiro.ReaderWriterFactory;

public class OrcaOutputReaderTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/orca.log").getFile());
		
      	assertTrue(ReaderWriterFactory.getInstance().makeOutputReaderInstance(
      			logFile) instanceof OrcaOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
