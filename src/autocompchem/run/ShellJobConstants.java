package autocompchem.run;

/**
 * Storage of predefined constants for shell commands
 *
 * @author Marco Foscato
 */

public class ShellJobConstants
{
    /**
     * Extension of shell input files
     */
    public final static String INPEXTENSION = ".sh";

    /**
     * Extension of shell job details file
     */
    public final static String JDEXTENSION = ".jd";

    /**
     * Step (i.e., task) separator for jobdetails files
     */
    public final static String TASKSEPARATORJD = "--NEW-TASK--";

    /**
     * Step (i.e., task) separator for input files
     */
    public final static String TASKSEPARATOR = "; ";

    /**
     * Separator for keyword:value pairs in jobdetails files
     */
    public final static String KEYVALSEPARATOR = "=";

    /**
     * Separator for dataName:dataValue pairs in jobdetails files
     */
    public final static String DATAVALSEPARATOR = "=";

    /**
     * Keyword for interpreter
     */
    public final static String LABINTERPRETER = "$EXE_";

    /**
     * Keyword for script
     */
    public final static String LABSCRIPT = "$SCRIPT_";

    /**
     * Keyword for arguments
     */
    public final static String LABARGS = "$ARGS_";

    /**
     * Keyword identifying the beginning of a multiline block
     */
    public final static String LABOPENBLOCK = "$OPENBLOCK";

    /**
     * Keyword identifying the end of a multiline block
     */
    public final static String LABCLOSEBLOCK = "$CLOSEBLOCK";

}
