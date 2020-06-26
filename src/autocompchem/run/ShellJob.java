package autocompchem.run;

import java.util.ArrayList;

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
        super(RunnableAppID.SHELL);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param interpreter the interpreter to call for the script
     * @param script the executable script
     * @param args command line arguments and options all collected in a single
     * string
     */

    public ShellJob(String interpreter, String script, String args)
    {
        super(RunnableAppID.SHELL);
        this.interpreter = interpreter;
        this.script = script;
        this.args = args;
        this.setVerbosity(0);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param interpreter the interpreter to call for the script
     * @param script the executable script
     * @param args command line arguments and options all collected in a single
     * string
     * @param verbosity the verbosity level
     */

    public ShellJob(String interpreter, String script, String args, 
    		int verbosity)
    {
        super(RunnableAppID.SHELL);
        this.interpreter = interpreter;
        this.script = script;
        this.args = args;
        this.setVerbosity(verbosity);
    }

//------------------------------------------------------------------------------

    /**
     * Runs this SHELL command
     */

    @Override
    public void runThisJobSubClassSpecific()
    {
        if (getVerbosity() > 0)
        {
            System.out.println("Running SHELL Job: " + this.toString() + " Thread: "
                                            + Thread.currentThread().getName());
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(interpreter).append(" ");
        sb.append(script).append(" ");
        sb.append(args);

        if (!sb.toString().trim().isEmpty())
        {
            try
            {
                ProcessBuilder pb = new ProcessBuilder(interpreter,script,args);
                pb.inheritIO();
                Process p = pb.start();
                try
                {
                    p.waitFor();
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

    /**
     * Produced the text input. The text input is meant for a text file
     * that a specific application can read and use to run the job. If the 
     * application is the autocompchem, then the jobDetails format is used
     * @return the list of lines ready to print a text input file
     */

    public ArrayList<String> toLinesInput()
    {
        ArrayList<String> a = new ArrayList<String>();
        a.add(interpreter + " " + script + " " + args);
        return a;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this job following the format of
     * autocompchem's JobDetail text file.
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines= new ArrayList<String>();
        for (int step = 0; step<steps.size(); step++)
        {
            //Write job-separator
            if (step != 0)
            {
                lines.add(stepSeparatorJd);
            }

            lines.addAll(getStep(step).toLinesJobDetails());
        }
        return lines;
    }

//------------------------------------------------------------------------------

}
