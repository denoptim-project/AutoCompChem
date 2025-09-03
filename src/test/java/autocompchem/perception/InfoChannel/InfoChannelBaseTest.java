package autocompchem.perception.InfoChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import autocompchem.files.FileUtils;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;

public class InfoChannelBaseTest 
{

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    public static InfoChannelBase getTestInstance()
    {
    	InfoChannelBase icb = new InfoChannelBase();
    	icb.addChannel(FileAsSourceTest.getTestInstance());
    	icb.addChannel(EnvironmentAsSourceTest.getTestInstance());
    	icb.addChannel(ShortTextAsSourceTest.getTestInstance());
    	return icb;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	InfoChannelBase a1 = getTestInstance();
    	
    	InfoChannelBase a2 = getTestInstance();

      	assertTrue(a1.equals(a2));
      	assertTrue(a2.equals(a1));
      	assertTrue(a1.equals(a1));
      	assertFalse(a1.equals(null));
      	
      	a2 = new InfoChannelBase();
      	a2.addChannel(new FileAsSource("dummy"));
      	assertFalse(a1.equals(a2));

      	a2 = getTestInstance();
    	((FileAsSource) a2.getAllChannels().get(0)).setType(
    			InfoChannelType.ENVIRONMENT);
      	assertFalse(a1.equals(a2));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetInfoChannelDB() throws Exception
    {
    	InfoChannelBase sb = InfoChannelBase.getInfoChannelDB("test_infochannels");
    	assertEquals(3, sb.getAllChannels().size());
    	
    	Set<String> expectedNames = Set.of(
    			"ic1",
    			"ic2",
    			"ic3");
    	for (InfoChannel ic : sb.getAllChannels())
    	{
    		assertTrue(ic instanceof FileAsSource);
    		assertTrue(expectedNames.contains(((FileAsSource) ic).getPathName()));
    	}
    }
    
//------------------------------------------------------------------------------

	/*
	 * Here we test is the serialization works well even without knowing which 
	 * implementation of the InfoChannel we are dealing with.
	 */
    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	InfoChannelBase icb = getTestInstance();
    	
    	String json = writer.toJson(icb);
    	
    	InfoChannelBase fromJson = reader.fromJson(json, InfoChannelBase.class);
    	assertEquals(icb, fromJson);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetSpecific() throws Exception
    {
    	// Prepare files for test
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        String filename = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "foo.bar";
        File tmpFile = new File(filename);
        IOtools.writeTXTAppend(tmpFile, "dummy content", false);
        
        String filename0 = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "tmp.txt";
        File tmpFile0 = new File(filename0);
        IOtools.writeTXTAppend(tmpFile0, "dummy content", false);
        
        String filename1 = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "tmp.ext1";
        File tmpFile1 = new File(filename1);
        IOtools.writeTXTAppend(tmpFile1, "dummy content", false);
        
        String filename2 = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "a.ext";
        File tmpFile2 = new File(filename2);
        IOtools.writeTXTAppend(tmpFile2, "dummy content", false);
        
        String filename3 = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "b.ext";
        File tmpFile3 = new File(filename3);
        IOtools.writeTXTAppend(tmpFile3, "dummy content", false);
        
        String dirName = tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "dir";
        File dir = new File(dirName);
        dir.mkdir();
        
        String filename4 = dir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "inner.ext";
        File tmpFile4 = new File(filename4);
        IOtools.writeTXTAppend(tmpFile4, "dummy content", false);
       
    	// No change if none of the channels has wildcards
    	InfoChannelBase icb = new InfoChannelBase();
    	FileAsSource nonRegex = new FileAsSource("[abs", 
    			InfoChannelType.INPUTFILE);
    	icb.addChannel(nonRegex);
    	icb.addChannel(EnvironmentAsSourceTest.getTestInstance());
    	icb.addChannel(ShortTextAsSourceTest.getTestInstance());
    	
    	assertEquals(icb, icb.getSpecific(tempDir.toPath()));
    	
    	// Replace a wildecard-based channel with the concrete ones
    	FileAsSource fasWithStar = new FileAsSource(".*\\.ext$", 
    			InfoChannelType.LOGFEED);
    	icb.addChannel(fasWithStar);
    	
    	InfoChannelBase specICB = icb.getSpecific(tempDir.toPath());
    	
    	assertNotEquals(icb, specICB);
    	assertEquals(5, specICB.getInfoChannelCount());
    	assertEquals(2, specICB.getChannelsOfType(InfoChannelType.LOGFEED).size());
    	Set<String> expected = Set.of("a.ext", "b.ext");
    	for  (InfoChannel ic : specICB.getChannelsOfType(InfoChannelType.LOGFEED))
    	{
    		assertTrue(ic instanceof FileAsSource);
    		assertTrue(expected.contains(
    				(new File(((FileAsSource) ic).getPathName()).getName())));
    	}
    }
    
//------------------------------------------------------------------------------

}
