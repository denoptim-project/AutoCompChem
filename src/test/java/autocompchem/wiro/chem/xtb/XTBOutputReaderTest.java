package autocompchem.wiro.chem.xtb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.wiro.ReaderWriterFactory;

public class XTBOutputReaderTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
      	ReaderWriterFactory b = 
      			ReaderWriterFactory.getInstance();

		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/xtb_output/log").getFile());
		
      	assertTrue(b.makeOutputReaderInstance(logFile) instanceof XTBOutputReader);
      	
		File outputFolder = new File(classLoader.getResource(
				"chemSoft_output_examples/xtb_output/log").getFile());

      	assertTrue(b.makeOutputReaderInstance(outputFolder) instanceof XTBOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
