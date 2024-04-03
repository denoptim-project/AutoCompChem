package autocompchem.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileAnalyzer;


/**
 * Unit Test for logging utilities
 * 
 * @author Marco Foscato
 */

public class LogUtilsTest 
{
    //TODO-gg del
	public static final String LOG4J2CONFIGFILEPATHNAME = "log4j2.xml";
	
//------------------------------------------------------------------------------

    //TODO-gg del
	@BeforeAll
	public static void configureDefaults()
	{
		System.setProperty("log4j.configurationFile",
				LOG4J2CONFIGFILEPATHNAME);
	}
    
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

    //TODO-gg del
    @Test
    public void testDefaultLogConfig() throws Exception
    {
    	assertEquals(LOG4J2CONFIGFILEPATHNAME, 
    			System.getProperty("log4j.configurationFile"));
    	Logger logger = LogManager.getLogger(LogUtils.class);
    	Logger output = LogManager.getLogger("OUTPUT");
    	Logger other = LogManager.getLogger();
    	
    	LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    	Configuration config = ctx.getConfiguration();
    	LoggerConfig loggerConfig = config.getLoggerConfig("OUTPUT"); 
    	loggerConfig.setLevel(Level.TRACE);
    	ctx.updateLoggers();
    	
    	for (int i=-1; i<9; i++)
    	{
    		Level level = LogUtils.verbosityToLevel(i);
    		logger.log(level, "L:My " + level + " message");
    		output.log(level, "O:My " + level + " message");
    		other.log(level, "R:My " + level + " message");
    	}
    }
    
//------------------------------------------------------------------------------

}
