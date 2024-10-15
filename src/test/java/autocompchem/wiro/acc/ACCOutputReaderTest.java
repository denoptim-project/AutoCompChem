package autocompchem.wiro.acc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.chem.xtb.XTBOutputReader;

public class ACCOutputReaderTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
      	ReaderWriterFactory b = 
      			ReaderWriterFactory.getInstance();

		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/acc.log").getFile());
		
      	assertTrue(b.makeOutputReaderInstance(logFile) 
      			instanceof ACCOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
