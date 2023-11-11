package autocompchem.chemsoftware.nwchem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.chemsoftware.ChemSoftOutputReader;
import autocompchem.chemsoftware.ChemSoftReaderWriterFactory;

public class NWChemOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
		ClassLoader classLoader = getClass().getClassLoader();
		File nwchemLogFile = new File(classLoader.getResource(
				"chemSoft_output_examples/nwchem.log").getFile());
		
      	ChemSoftOutputReader csoa = ChemSoftReaderWriterFactory.getInstance()
      			.makeOutputReaderInstance(nwchemLogFile);
      	assertTrue(csoa instanceof NWChemOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
