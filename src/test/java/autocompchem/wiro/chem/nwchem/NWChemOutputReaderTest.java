package autocompchem.wiro.chem.nwchem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import autocompchem.wiro.OutputReader;
import autocompchem.wiro.ReaderWriterFactory;

public class NWChemOutputReaderTest 
{
	   
//------------------------------------------------------------------------------
  	  
    @Test
    public void testOutputFingerprint() throws Exception
    {
		ClassLoader classLoader = getClass().getClassLoader();
		File nwchemLogFile = getResourceAsFile(classLoader, "chemSoft_output_examples/nwchem.log");
		
      	OutputReader csoa = ReaderWriterFactory.getInstance()
      			.makeOutputReaderInstance(nwchemLogFile);
      	assertTrue(csoa instanceof NWChemOutputReader);
    }
  
//------------------------------------------------------------------------------

    /**
     * Helper method to properly load resource files, handling URL encoding issues
     * that occur when paths contain spaces (e.g., "OneDrive - University").
     * 
     * @param classLoader the class loader to use
     * @param resourceName the name of the resource file
     * @return File object pointing to the resource
     * @throws RuntimeException if the resource cannot be found or accessed
     */
    private File getResourceAsFile(ClassLoader classLoader, String resourceName) {
        try {
            return Paths.get(classLoader.getResource(resourceName).toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load resource: " + resourceName, e);
        }
    }

//------------------------------------------------------------------------------
  	
}
