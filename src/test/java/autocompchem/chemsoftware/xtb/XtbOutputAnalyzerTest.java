package autocompchem.chemsoftware.xtb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftOutputAnalyzer;
import autocompchem.chemsoftware.ChemSoftReaderWriterFactory;

public class XtbOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
      	ChemSoftReaderWriterFactory b = 
      			ChemSoftReaderWriterFactory.getInstance();

		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/xtb_output/log").getFile());
		
      	assertTrue(b.makeOutputReaderInstance(logFile) instanceof XTBOutputAnalyzer);
      	
		File outputFolder = new File(classLoader.getResource(
				"chemSoft_output_examples/xtb_output/log").getFile());

      	assertTrue(b.makeOutputReaderInstance(outputFolder) instanceof XTBOutputAnalyzer);
    }
  
//------------------------------------------------------------------------------
  	
}
