package autocompchem.wiro.chem.nwchem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import autocompchem.wiro.OutputReader;
import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.wiro.chem.nwchem.NWChemOutputReader;

public class NWChemOutputAnalyzerTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
		ClassLoader classLoader = getClass().getClassLoader();
		File nwchemLogFile = new File(classLoader.getResource(
				"chemSoft_output_examples/nwchem.log").getFile());
		
      	OutputReader csoa = ReaderWriterFactory.getInstance()
      			.makeOutputReaderInstance(nwchemLogFile);
      	assertTrue(csoa instanceof NWChemOutputReader);
    }
  
//------------------------------------------------------------------------------
  	
}
