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
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
//import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.api.parallel.ResourceAccessMode;

import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;


/**
 * Unit Test for logging on rolling file. This is a class isolated from
 * {@link LogUtilsTest} to ensure independent generation of the logger.
 * 
 * @author Marco Foscato
 */

@Isolated
public class RollingLogTest 
{

	private static String fileSeparator = System.getProperty("file.separator");
	
    @TempDir 
    File tempDir;

//------------------------------------------------------------------------------

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
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
		LoggerContext initialContex = (LoggerContext) LogManager.getContext(true);
    	Configuration initialConfig = initialContex.getConfiguration();

		//TODO del
		//printContextDetails(initialContex);
		
    	// Make a specific XML configuration file meant only for this tests
		ClassLoader classLoader = getClass().getClassLoader();
		File tmplConfigFile = new File(classLoader.getResource(
				"log4j2_config-RollingFileAppender.xml").getFile());
		File myConfigFile = new File(tempDir.getAbsolutePath() + fileSeparator
				+ "myConfigFile.xml");
		IOtools.writeTXTAppend(myConfigFile, 
				IOtools.readTXT(tmplConfigFile), false);
		FileUtils.replaceString(myConfigFile, 
				Pattern.compile("STRINGTOCHANGE"),
				myLogFile.getAbsolutePath());
		
		// Read the customized XML file into a configuration object
		ConfigurationSource source = new ConfigurationSource(new FileInputStream(myConfigFile),
				myConfigFile);
		XmlConfiguration config = new XmlConfiguration(initialContex, source);
		
		// Deploy the customized configuration
		Configurator.reconfigure(config);
		
        // Make a logger according to the latest configuration
        Logger loggerForTest = LogManager.getLogger();
        
    	// Write some log
    	for (int i=0; i<1000; i++)
    	{
    		loggerForTest.log(Level.WARN, "Log entry number " + i);
    	}
    	
    	//TODO del
		//org.apache.commons.io.FileUtils.copyDirectory(tempDir, new File("/tmp/unit"));
    	
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
		LoggerContext restoredContext = Configurator.initialize(initialConfig);
		Configurator.reconfigure(initialConfig);
		
		//TODO del
		//printContextDetails(restoredContext);
		//printContextDetails((LoggerContext) LogManager.getContext(false));
		//System.out.println("");
    }

//------------------------------------------------------------------------------

    private void printLoggerConfigDetails(boolean flag) throws Exception
    {
    	LoggerContext ctx = (LoggerContext) LogManager.getContext(flag);
    	printContextDetails(ctx);
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
