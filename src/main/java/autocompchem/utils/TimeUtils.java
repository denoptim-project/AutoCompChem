package autocompchem.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tools for dealing with time
 * 
 * @author Marco Foscato
 */

public class TimeUtils
{

//------------------------------------------------------------------------------

    /**
     * Returns a string with a time stamp in a line filled with asterisks.
     */

    public static String getTimestampLine()
    {
    	LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
        		  "dd-MM-yyyy HH:mm:ss.SSS");
    	return "*************************** " + date.format(formatter)
               + " ***************************";
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns a string with a time stamp 
     */

    public static String getTimestamp()
    {
     	LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
          		  "dd-MM-yyyy HH:mm:ss.SSS");
      	return date.format(formatter);
    }

//------------------------------------------------------------------------------

}
