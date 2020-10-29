package autocompchem.run;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.utils.StringUtils;

/**
 * A shell job is work to be done by the shell
 *
 * @author Marco Foscato
 */

public class ShellJob extends Job
{
	
    /**
     * The command will try to run
     */
    private List<String> command;
 
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
     * @param interpreter the interpreter to call for the script.
     * @param script the executable script.
     * @param args command line arguments and options all collected in a single
     * string.
     */

    public ShellJob(String interpreter, String script, String args)
    {
    	this(interpreter,script,args,0);
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
        this.command = new ArrayList<String>();
        this.command.add(interpreter);
        this.command.add(script);
        this.command.add(args);
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
     * @param customUserDir the (existing) directory from which to run the script.
     * @param verbosity the verbosity level.
     */

    public ShellJob(String interpreter, String script, String args, 
    		File customUserDir, int verbosity)
    {
        super();
        this.appID = RunnableAppID.SHELL;
        this.command = new ArrayList<String>();
        this.command.add(interpreter);
        this.command.add(script);
        this.command.add(args);
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
    	// First we need to see if the command comes from the constructor of
    	// from parameter storage
    	if (params.contains(ShellJobConstants.LABINTERPRETER))
    	{
    		command = new ArrayList<String>();
    		command.add(params.getParameter(
    				ShellJobConstants.LABINTERPRETER).getValueAsString());
    	
    		if (!params.contains(ShellJobConstants.LABSCRIPT))
    		{
    			Terminator.withMsgAndStatus("Expecting a script pathname, but "
    					+ ShellJobConstants.LABSCRIPT + " parameter is not "
    							+ "found.", -1);
    		}
    		
    		String script = params.getParameter(
    				ShellJobConstants.LABSCRIPT).getValueAsString();
    		script = script.replaceFirst("^~", System.getProperty("user.home")); 
    		File scriptFile = new File(script);
    		command.add(scriptFile.getAbsolutePath());
    		
    		if (params.contains(ShellJobConstants.LABARGS))
        	{
    			String list = params.getParameter(
    					ShellJobConstants.LABARGS).getValueAsString();
    			for (String w : list.split("\\s+"))
    			{
    				command.add(w);
    			}
        	}
    	}
    	
        Date date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Running SHELL Job: " + this.toString() 
                            + " Thread: " + Thread.currentThread().getName());
        }
        
        String commandAsString = StringUtils.mergeListToString(command, " ");

        if (!commandAsString.trim().isEmpty())
        {
            try
            {
                ProcessBuilder pb = new ProcessBuilder(command);
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
                    if (jobIsBeingKilled || isInterrupted)
                    {
                    	p.destroy();
                    } else {
                        throw ie;
                    }
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                Terminator.withMsgAndStatus("ERROR while running command line "
                                   + "operation '" + commandAsString + "'.",-1);
            }
        }

        if (getVerbosity() > 0)
        {
            System.out.println("Done with SHELL Job " + this.toString());
        }
    }

//------------------------------------------------------------------------------

}
