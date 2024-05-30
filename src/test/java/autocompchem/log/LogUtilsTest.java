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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
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
 * Unit Test for logging utilities and logging configurations.
 * 
 * @author Marco Foscato
 */

@Isolated
public class LogUtilsTest 
{
	
	public static final String STRINGTOCHANGE = "STRINGTOCHANGE";

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

    @Test
    public void testLogFormat() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

		ClassLoader classLoader = getClass().getClassLoader();
		
		/*
		 * Simple file appender
		 */
		File tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-A.xml").getFile());
		
		Map<String,Integer> expectedHitsInLogFile = new HashMap<String,Integer>();
		expectedHitsInLogFile.put("BuiltForTest*", 5);
		expectedHitsInLogFile.put("*OFF*", 2);
		expectedHitsInLogFile.put("*INFO*", 0);
		
		logToFileWithCustomLoggerConfig(tmplConfigFile, "A", 
				expectedHitsInLogFile, true);
		
		/*
		 * Configuration where INFO level messages are printed without any
		 * decoration, so they represent the STDOUT feed, while DEBUG and TRACE
		 * levels are reported with a time stamp.
		 */
		tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-B.xml").getFile());
		
		expectedHitsInLogFile = new HashMap<String,Integer>();
		expectedHitsInLogFile.put("default pattern*", 5);
		expectedHitsInLogFile.put("pattern for info*", 1);
		expectedHitsInLogFile.put("pattern for debug*", 1);
		
		logToFileWithCustomLoggerConfig(tmplConfigFile, "B2", 
				expectedHitsInLogFile, true);
		
		/*
		 * Configuration using rolling file: it creates files with limited size
		 * to avoid filling the disk with log files.
		 */
		tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-RollingFileAppender.xml").getFile());
		
		expectedHitsInLogFile = new HashMap<String,Integer>();
		expectedHitsInLogFile.put("* Log entry number 999", 1);
		
		logToFileWithCustomLoggerConfig(tmplConfigFile, "R", 
				expectedHitsInLogFile, false, 3);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Utility to test an XML configuration of log4j2 that is meant to writ to 
     * log files.
     * Takes a template log4j2 configuration XML file, copies it into the 
     * {@link #tempDir} and replace the string {@value #STRINGTOCHANGE} to the
     * pathname under {@link #tempDir} that is meant to be the log file on which
     * the logger configured by the XML file should write. 
     * Then creates and uses a logger, and finally verifies the resulting log.
     * @param xmlConfig the XML file with the configuration to test.
     * @param id a unique string used to make any tmp file specific to a call
     * of this method.
     * @param expectedHitsInLogFile the map of patterns to find on the log
     * file with the corresponding expected number of hits.
     * @param exploreLevels if <code>true</code> we send log messages for each 
     * logging level. To this end, we use integers from -1 to 9 and convert then
     * to logging levels with {@link LogUtils#verbosityToLevel(int)}. Otherwise
     * we send 1000 log messages to {@value Level#WARN}.
     * @param numRollover the expected number of log files generated by a 
     * rolling file appender even if some of them were later removed.
     * @param maxRollover the maximum number of rolled-over files.
     * @throws Exception
     */
    public void logToFileWithCustomLoggerConfig(File xmlConfig, String id, 
    		Map<String,Integer> expectedHitsInLogFile, boolean exploreLevels)
    				throws Exception
    {
    	logToFileWithCustomLoggerConfig(xmlConfig, id, expectedHitsInLogFile,
    			exploreLevels, 0);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Utility to test an XML configuration of log4j2 that is meant to writ to 
     * log files.
     * Takes a template log4j2 configuration XML file, copies it into the 
     * {@link #tempDir} and replace the string {@value #STRINGTOCHANGE} to the
     * pathname under {@link #tempDir} that is meant to be the log file on which
     * the logger configured by the XML file should write. 
     * Then creates and uses a logger, and finally verifies the resulting log.
     * @param xmlConfig the XML file with the configuration to test.
     * @param id a unique string used to make any tmp file specific to a call
     * of this method.
     * @param expectedHitsInLogFile the map of patterns to find on the log
     * file with the corresponding expected number of hits.
     * @param exploreLevels if <code>true</code> we send log messages for each 
     * logging level. To this end, we use integers from -1 to 9 and convert then
     * to logging levels with {@link LogUtils#verbosityToLevel(int)}. Otherwise
     * we send 1000 log messages to {@value Level#WARN}.
     * @param numRollover the expected number of log files generated by a 
     * {@link RollingFileAppender} under the assumption that 
     * <code>filePattern="{@value #STRINGTOCHANGE}-%i"></code>
     * @throws Exception
     */
    public void logToFileWithCustomLoggerConfig(File xmlConfig, String id, 
    		Map<String,Integer> expectedHitsInLogFile, boolean exploreLevels,
    		int numRollover)
    				throws Exception
    {
		File myLogFile = new File(tempDir.getAbsolutePath() + fileSeparator 
				+ id + "ID_logFile.log");
		
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

    	// Make a tmp XML configuration file meant only for this run
		File myConfigFile = new File(tempDir.getAbsolutePath() + fileSeparator
				+ id + "ID_configFile.xml");
		IOtools.writeTXTAppend(myConfigFile, 
				IOtools.readTXT(xmlConfig), false);
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

    	if (exploreLevels)
    	{
	    	// Write some log scanning the logging levels
	    	for (int i=-1; i<9; i++)
	    	{
	    		Level level = LogUtils.verbosityToLevel(i);
	    		loggerForTest.log(level, i + ": " + id + "ID " + level + " txt");	
	    	}
    	} else {
    		// Write abundant log
        	for (int i=0; i<1000; i++)
        	{
        		loggerForTest.log(Level.WARN, id + "ID. Log entry number " + i);
        	}
    	}

    	// Test existence of primary log file
    	assertTrue(myLogFile.exists());
    	
    	// Test content of primary log file
    	List<String> queries = new ArrayList<String>(
    			expectedHitsInLogFile.keySet());
    	List<List<Integer>> analysis = FileAnalyzer.count(myLogFile, queries);
    	List<Integer> counts = analysis.get(analysis.size()-1);
    	for (int iQuery=0; iQuery<queries.size(); iQuery++)
    	{
        	assertEquals(expectedHitsInLogFile.get(queries.get(iQuery)), 
        			counts.get(iQuery), 
        			"Number of lines matching '" + queries.get(iQuery) 
        			+ "' (query " + iQuery + ") is incorrect.");
    	}
    	
    	// Test existence of rolled-over log files
    	for (int iRollover=0; iRollover<numRollover; iRollover++)
    	{
    		// WARNING: assumption that filePattern="STRINGTOCHANGE-%i">
        	File rolledOverLog = new File(myLogFile.getAbsolutePath() + "-"
        			+ (iRollover+1)); //NB: convention is to have 1-based names
        	assertTrue(rolledOverLog.exists());
    	}
    	
		// Restore initial configuration
		Configurator.reconfigure(initConfig);
    }
    
//------------------------------------------------------------------------------

    /*
     * Utility meant to print some details of the configuration when debugging.
     */
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
          		Method m = appender.getClass().getDeclaredMethod("getFileName", 
          				null);
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
