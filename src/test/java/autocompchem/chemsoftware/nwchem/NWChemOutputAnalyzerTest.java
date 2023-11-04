package autocompchem.chemsoftware.nwchem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftOutputAnalyzer;
import autocompchem.chemsoftware.ChemSoftOutputAnalyzerBuilder;

public class NWChemOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
      	ChemSoftOutputAnalyzerBuilder b = new ChemSoftOutputAnalyzerBuilder();

		ClassLoader classLoader = getClass().getClassLoader();
		File nwchemLogFile = new File(classLoader.getResource(
				"chemSoft_output_examples/nwchem.log").getFile());
		
      	ChemSoftOutputAnalyzer csoa = b.makeInstance(nwchemLogFile);
      	assertTrue(csoa instanceof NWChemOutputAnalyzer);
    }
  
//------------------------------------------------------------------------------
  	
}
