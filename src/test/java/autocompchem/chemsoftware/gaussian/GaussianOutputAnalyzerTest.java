package autocompchem.chemsoftware.gaussian;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftOutputReader;
import autocompchem.chemsoftware.ChemSoftReaderWriterFactory;

public class GaussianOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/g16.log").getFile());
		
      	assertTrue(ChemSoftReaderWriterFactory.getInstance().makeOutputReaderInstance(
      			logFile) instanceof GaussianOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
