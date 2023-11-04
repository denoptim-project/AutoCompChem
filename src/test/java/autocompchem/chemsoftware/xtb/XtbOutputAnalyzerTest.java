package autocompchem.chemsoftware.xtb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftOutputAnalyzer;
import autocompchem.chemsoftware.ChemSoftOutputAnalyzerBuilder;

public class XtbOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
      	ChemSoftOutputAnalyzerBuilder b = new ChemSoftOutputAnalyzerBuilder();

		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/xtb_output/log").getFile());
		
      	assertTrue(b.makeInstance(logFile) instanceof XTBOutputAnalyzer);
      	
		File outputFolder = new File(classLoader.getResource(
				"chemSoft_output_examples/xtb_output/log").getFile());

      	assertTrue(b.makeInstance(outputFolder) instanceof XTBOutputAnalyzer);
    }
  
//------------------------------------------------------------------------------
  	
}
