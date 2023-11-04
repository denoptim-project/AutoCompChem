package autocompchem.chemsoftware.gaussian;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftOutputAnalyzer;
import autocompchem.chemsoftware.ChemSoftOutputAnalyzerBuilder;

public class GaussianOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
      	ChemSoftOutputAnalyzerBuilder b = new ChemSoftOutputAnalyzerBuilder();

		ClassLoader classLoader = getClass().getClassLoader();
		File logFile = new File(classLoader.getResource(
				"chemSoft_output_examples/g16.log").getFile());
		
      	assertTrue(b.makeInstance(logFile) instanceof GaussianOutputAnalyzer);
    }
  
//------------------------------------------------------------------------------
  	
}
