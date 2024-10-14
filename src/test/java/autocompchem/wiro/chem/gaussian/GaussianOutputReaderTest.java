package autocompchem.wiro.chem.gaussian;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.chem.gaussian.GaussianOutputReader;

public class GaussianOutputReaderTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/g16.log").getFile());
		
      	assertTrue(ReaderWriterFactory.getInstance().makeOutputReaderInstance(
      			logFile) instanceof GaussianOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
