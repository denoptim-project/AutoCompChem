package autocompchem.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Utilities for logging.
 */

public class LogUtils {

//------------------------------------------------------------------------------

	/**
	 * Converts an integer to a {@link Level}.
	 * @param verbosity the integer to be converted.
	 * @return the level.
	 */
	public static Level verbosityToLevel(int verbosity)
	{
        switch (verbosity)
        {
            case 0:
                return Level.OFF;
            case 1:
                return Level.FATAL;
            case 2:
                return Level.ERROR;
            case 3:
                return Level.WARN;
            case 4:
                return Level.INFO;
            case 5:
                return Level.DEBUG;
            case 6:
                return Level.TRACE;
            case 7:
                return Level.ALL;
            default:
                if (verbosity>7)
                    return Level.ALL;
                else
                    return Level.OFF;
        }
	}
	
//------------------------------------------------------------------------------

	/**
	 * Asks a logger to log a message for each logging level.
	 * @param logger the logger to work with.
	 */
	public static void scanLogLevels(Logger logger)
	{
		for (Level l : Level.values())
			logger.log(l, "Scanning logger " + logger.getName() + " - level '" 
					+ l + "'");
	}
	
//------------------------------------------------------------------------------

}
