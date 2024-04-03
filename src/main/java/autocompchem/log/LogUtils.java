package autocompchem.log;

import org.apache.logging.log4j.Level;

/**
 * Utilities for logging.
 */

public class LogUtils {

//------------------------------------------------------------------------------

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

}
