package autocompchem.log;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;


/**
 * Unit Test for logging utilities
 * 
 * @author Marco Foscato
 */

@Isolated
public class LogUtilsTest 
{

	private static String fileSeparator = System.getProperty("file.separator");
	
    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testVerbosityToLevel() throws Exception
    {
    	assertEquals(Level.OFF, LogUtils.verbosityToLevel(-1));
    	assertEquals(Level.ALL, LogUtils.verbosityToLevel(8));
    	assertEquals(Level.ALL, LogUtils.verbosityToLevel(7));
    	assertEquals(Level.INFO, LogUtils.verbosityToLevel(4));
    }   

//------------------------------------------------------------------------------

    /*
     * src/test/resources/log4j2_config-A.xml
     */
    
    @Test
    public void testLogFormatA() throws Exception
    {
    	// Define location of log time
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
		File myLogFile = new File(tempDir.getAbsolutePath() + fileSeparator 
				+ "myLogFile.log");
		
		// Keep the original configuration. NB: using 'false' does not work
		//
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// !!                                                                !!
		// !!   The initial configuration changes between when we run this   !!
		// !!   in Eclipse and when we run it in the maven workflow.         !!
		// !!                                                                !!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//
		LoggerContext initContex = (LoggerContext) LogManager.getContext(true);
    	Configuration initConfig = initContex.getConfiguration();

    	// Make a specific XML configuration file meant only for this tests
		ClassLoader classLoader = getClass().getClassLoader();
		File tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-A.xml").getFile());
		File myConfigFile = new File(tempDir.getAbsolutePath() + fileSeparator
				+ "myConfigFile.xml");
		IOtools.writeTXTAppend(myConfigFile, 
				IOtools.readTXT(tmplConfigFile), false);
		FileUtils.replaceString(myConfigFile, 
				Pattern.compile("STRINGTOCHANGE"),
				myLogFile.getAbsolutePath());
		
		// Read the customized XML file into a configuration object
		ConfigurationSource source = new ConfigurationSource(
				new FileInputStream(myConfigFile), myConfigFile);
		XmlConfiguration config = new XmlConfiguration(initContex, source);
		
		// Deploy the customized configuration
		Configurator.reconfigure(config);

        // Make a logger according to the latest configuration
    	Logger loggerForTest = LogManager.getLogger();

    	// Write some log
    	for (int i=-1; i<9; i++)
    	{
    		Level level = LogUtils.verbosityToLevel(i);
    		loggerForTest.log(level, i +" L:My " + level + " message " + i);	
    	}
    	
    	// Test content of log file
    	assertTrue(myLogFile.exists());
    	List<List<Integer>> analysis = FileAnalyzer.count(myLogFile, 
    			new ArrayList<String>(Arrays.asList(
    					"BuiltForTest*", "*OFF*", "*INFO*")));
    	List<Integer> counts = analysis.get(analysis.size()-1);
    	assertEquals(5, counts.get(0)); //BuiltForTest
    	assertEquals(2, counts.get(1)); //OFF
    	assertEquals(0, counts.get(2)); //INFO
    	
		// Restore initial configuration
		Configurator.reconfigure(initConfig);
    }
    
//------------------------------------------------------------------------------

    /*
     * Testing of src/test/resources/log4j2_config-B.xml
     * This is mosly a repetition of the previous method, but the assertions
     * change!
     */
    
    @Test
    public void testLogFormatB() throws Exception
    {
    	// Define location of log time
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
		File myLogFile = new File(tempDir.getAbsolutePath() + fileSeparator 
				+ "myLogFileB.log");
		
		// Keep the original configuration. NB: using 'false' does not work
		//
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// !!                                                                !!
		// !!   The initial configuration changes between when we run this   !!
		// !!   in Eclipse and when we run it in the maven workflow.         !!
		// !!                                                                !!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//
		LoggerContext initContex = (LoggerContext) LogManager.getContext(true);
    	Configuration initConfig = initContex.getConfiguration();

    	// Make a specific XML configuration file meant only for this tests
		ClassLoader classLoader = getClass().getClassLoader();
		File tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-B.xml").getFile());
		File myConfigFile = new File(tempDir.getAbsolutePath() + fileSeparator
				+ "myConfigFileB.xml");
		IOtools.writeTXTAppend(myConfigFile, 
				IOtools.readTXT(tmplConfigFile), false);
		FileUtils.replaceString(myConfigFile, 
				Pattern.compile("STRINGTOCHANGE"),
				myLogFile.getAbsolutePath());
		
		// Read the customized XML file into a configuration object
		ConfigurationSource source = new ConfigurationSource(
				new FileInputStream(myConfigFile), myConfigFile);
		XmlConfiguration config = new XmlConfiguration(initContex, source);
		
		// Deploy the customized configuration
		Configurator.reconfigure(config);

        // Make a logger according to the latest configuration
    	Logger loggerForTest = LogManager.getLogger();

    	// Write some log
    	for (int i=-1; i<9; i++)
    	{
    		Level level = LogUtils.verbosityToLevel(i);
    		loggerForTest.log(level, i +" L:BBB " + level + " message " + i);	
    	}
    	
    	// Test content of log file
    	assertTrue(myLogFile.exists());
    	List<List<Integer>> analysis = FileAnalyzer.count(myLogFile, 
    			new ArrayList<String>(Arrays.asList(
    					"default pattern*", "pattern for info*", 
    					"pattern for debug*")));
    	List<Integer> counts = analysis.get(analysis.size()-1);
    	assertEquals(5, counts.get(0)); //BuiltForTest
    	assertEquals(1, counts.get(1)); //OFF
    	assertEquals(1, counts.get(2)); //INFO
    	
		// Restore initial configuration
		Configurator.reconfigure(initConfig);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testRollingLog() throws Exception
    {
    	// Define location of log time
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
		File myLogFile = new File(tempDir.getAbsolutePath() + fileSeparator 
				+ "myRollingLog.log");
		
		// Keep the original configuration. NB: using 'false' does not work
		//
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// !!                                                                !!
		// !!   The initial configuration changes between when we run this   !!
		// !!   in Eclipse and when we run it in the maven workflow.         !!
		// !!                                                                !!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//
		LoggerContext initContex = (LoggerContext) LogManager.getContext(true);
    	Configuration initConfig = initContex.getConfiguration();
		
    	// Make a specific XML configuration file meant only for this tests
		ClassLoader classLoader = getClass().getClassLoader();
		File tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-RollingFileAppender.xml").getFile());
		File myConfigFile = new File(tempDir.getAbsolutePath() + fileSeparator
				+ "myConfigFileForRollingLog.xml");
		IOtools.writeTXTAppend(myConfigFile, 
				IOtools.readTXT(tmplConfigFile), false);
		FileUtils.replaceString(myConfigFile, 
				Pattern.compile("STRINGTOCHANGE"),
				myLogFile.getAbsolutePath());
		
		// Read the customized XML file into a configuration object
		ConfigurationSource source = new ConfigurationSource(
				new FileInputStream(myConfigFile), myConfigFile);
		XmlConfiguration config = new XmlConfiguration(initContex, source);
		
		// Deploy the customized configuration
		Configurator.reconfigure(config);
		
        // Make a logger according to the latest configuration
        Logger loggerForTest = LogManager.getLogger();
        
    	// Write some log
    	for (int i=0; i<1000; i++)
    	{
    		loggerForTest.log(Level.WARN, "Log entry number " + i);
    	}
    	
    	// Test existence of log latest files
    	assertTrue(myLogFile.exists());
    	
    	// Ensure the given (3, see xml file) number of rolled over files exists
    	// NB: the naming strategy is defined in the xml file
    	File rolledOverLog1 = new File(myLogFile.getAbsolutePath() + "-1");
    	assertTrue(rolledOverLog1.exists());
    	File rolledOverLog2 = new File(myLogFile.getAbsolutePath() + "-2");
    	assertTrue(rolledOverLog2.exists());
    	File rolledOverLog3 = new File(myLogFile.getAbsolutePath() + "-3");
    	assertTrue(rolledOverLog3.exists());
    	File rolledOverLog4 = new File(myLogFile.getAbsolutePath() + "-4");
    	assertFalse(rolledOverLog4.exists());
    	
    	List<List<Integer>> analysis = FileAnalyzer.count(myLogFile, 
    			new ArrayList<String>(Arrays.asList(
    					"* Log entry number 999", "Roller WARN*")));
    	List<Integer> counts = analysis.get(analysis.size()-1);
    	assertEquals(1, counts.get(0));
    	assertTrue(counts.get(1) > 5);

		// Restore initial configuration
		Configurator.reconfigure(initConfig);
    }


//------------------------------------------------------------------------------

    private void printContextDetails(LoggerContext ctx) throws Exception
    {
    	System.out.println("----------------------------------------------");
     	Configuration config = ctx.getConfiguration();
      	System.out.println("ConfigClass: "+config.getClass().getCanonicalName());
      	System.out.println("Config: "+config.toString());
      	
      	for (String appenderName : config.getAppenders().keySet())
      	{
      		Appender appender = config.getAppender(appenderName);
      		System.out.println("Appender "+appender.getName());
      		try {
          		Method m = appender.getClass().getDeclaredMethod("getFileName", null);
          		if (m!=null)
          		{
          			System.out.println("  FileName:"+m.invoke(appender, null));
          		}
      		} catch (Throwable t) {
      			//Nothing
      		}
      	}
    }
    
//------------------------------------------------------------------------------

}
