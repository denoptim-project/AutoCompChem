package autocompchem.run;

import java.io.File;
import java.util.Date;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;

/**
 * A shell job is work to be done by the shell
 *
 * @author Marco Foscato
 */

public class ShellJob extends Job
{
    /**
     * Interpreter
     */
    private String interpreter = "";

    /**
     * script
     */
    private String script = "";
   
    /**
     * Arguments
     */
    private String args = "";
 
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ShellJob()
    {
        super();
        this.appID = RunnableAppID.SHELL;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param interpreter the interpreter to call for the script
     * @param script the executable script
     */

    public ShellJob(String interpreter, String script)
    {
        super();
        this.appID = RunnableAppID.SHELL;
        this.interpreter = interpreter;
        this.script = script;
        this.args = "";
        this.setVerbosity(0);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param interpreter the interpreter to call for the script.
     * @param script the executable script.
     * @param args command line arguments and options all collected in a single
     * string.
     */

    public ShellJob(String interpreter, String script, String args)
    {
        super();
        this.appID = RunnableAppID.SHELL;
        this.interpreter = interpreter;
        this.script = script;
        this.args = args;
        this.setVerbosity(0);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param interpreter the interpreter to call for the script.
     * @param script the executable script.
     * @param args command line arguments and options all collected in a single
     * string.
     * @param verbosity the verbosity level.
     */

    public ShellJob(String interpreter, String script, String args, int verbosity)
    {
        super();
        this.appID = RunnableAppID.SHELL;
        this.interpreter = interpreter;
        this.script = script;
        this.args = args;
        this.setVerbosity(verbosity);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param interpreter the interpreter to call for the script.
     * @param script the executable script.
     * @param args command line arguments and options all collected in a single
     * string.
     * @param workdir the (existing) directory from which to run the script.
     * @param verbosity the verbosity level.
     */

    public ShellJob(String interpreter, String script, String args, 
    		File customUserDir, int verbosity)
    {
        super();
        this.appID = RunnableAppID.SHELL;
        this.interpreter = interpreter;
        this.script = script;
        this.args = args;
        this.customUserDir = customUserDir;
        this.setVerbosity(verbosity);
    }

//------------------------------------------------------------------------------

    /**
     * Runs this SHELL command
     */

    @Override
    public void runThisJobSubClassSpecific()
    {
        Date date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Running SHELL Job: " + this.toString() 
                            + " Thread: " + Thread.currentThread().getName());
        }
        
        // Build the actual command
        StringBuilder sb = new StringBuilder();
        sb.append(interpreter).append(" ");
        sb.append(script).append(" ");
        sb.append(args);

        if (!sb.toString().trim().isEmpty())
        {
            try
            {
                ProcessBuilder pb = new ProcessBuilder(interpreter,script,args);
                if (customUserDir != null)
                {
                	pb.directory(customUserDir);
                }
                
                // Recover environmental variables to be exposed as output data
                // NB: this is the INITIAL environment! 
                // There is no way (yet) the get the environment after running 
                // the process... sadly.
                
                NamedData nd = new NamedData("INITIALENV", 
            			NamedDataType.STRING, pb.environment().toString());
                exposedOutput.putNamedData(nd);
                
                if (pb.directory() != null)
                {
                	customUserDir = pb.directory();
                }
                
                if (redirectOutErr)
                {
	                //Redirect stdout and stderr
	                pb.redirectOutput(stdout);
	                pb.redirectError(stderr);
                } 
                else
                {
	                pb.inheritIO();
                }
                
                Process p = pb.start();
                try
                {
                    int exitCode = p.waitFor();
                    exposedOutput.putNamedData(new NamedData("EXITCODE",
                    		NamedDataType.INTEGER, exitCode));
                }
                catch (InterruptedException ie)
                {
                    if(!super.jobIsBeingKilled)
                    {
                        throw ie;
                    }
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                Terminator.withMsgAndStatus("ERROR while running command line "
                                   + "operation '" + sb.toString() + "'.",-1);
            }
        }

        if (getVerbosity() > 0)
        {
            System.out.println("Done with SHELL Job " + this.toString());
        }
    }

//------------------------------------------------------------------------------

}
