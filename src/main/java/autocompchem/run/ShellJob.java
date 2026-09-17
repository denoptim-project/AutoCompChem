package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.datacollections.NamedData;
import autocompchem.utils.TimeUtils;

/**
 * A shell job is work to be done by the shell. The shell command can be executed
 * in a newly created subfolder. In this case any pathname should reflect the
 * fact that `pwd` would return the pathname of the subfolder.
 * <p>
 * Commands are always launched via a system shell ({@code sh -c} or
 * {@code cmd /c}) so that the shell can expand wildcards and other shell
 * features in {@code CMD} / {@code ARGS} text. The {@code CMD} and
 * {@code EXE}+{@code SCRIPT} parameter styles remain available; they only
 * differ in how the command line string is assembled before it is handed to
 * the shell.
 *
 * @author Marco Foscato
 */

public class ShellJob extends Job
{
    /**
     * The logical command components (for serialization / debugging).
     * Execution always goes through a shell; see {@link #runThisJobSubClassSpecific()}.
     */
    private List<String> command;
 
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ShellJob()
    {
        super();
        this.appID = SoftwareId.SHELL;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ShellJob with a defined interpreter, script and 
     * arguments/options.
     * @param commandComponents an array of string where each string is a 
     * component of the overall command.
     */

    public ShellJob(String... commandComponents)
    {
    	this();
    	this.command = new ArrayList<String>(Arrays.asList(commandComponents));
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
        this.appID = SoftwareId.SHELL;
        this.command = new ArrayList<String>();
        this.command.add(interpreter);
        this.command.add(script);
        this.command.add(args);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that may return a subclass
     */
    public Job makeInstance()
    {
    	return new ShellJob();
    }

//------------------------------------------------------------------------------

    /**
     * Runs this SHELL command via a system shell so that wildcards and related
     * shell features in the command line are expanded by the interpreter.
     */

    @Override
    public void runThisJobSubClassSpecific()
    {
		if (params.contains(ShellJobConstants.LABINTERPRETER)
				&& params.contains(ShellJobConstants.LABCOMMAND))
		{
			throw new IllegalArgumentException("Cannot have both "
					+ ShellJobConstants.LABCOMMAND + " and " 
					+ ShellJobConstants.LABINTERPRETER + " as parameters "
					+ "in a shell job. Use either one or the other.");
		}
		
    	String commandLine = "";
    	
    	// First we need to see if the command comes from the constructor or
    	// from parameter storage
    	if (params.contains(ShellJobConstants.LABINTERPRETER))
    	{
    		String interpreter = params.getParameter(
    				ShellJobConstants.LABINTERPRETER).getValueAsString();
    	
    		if (!params.contains(ShellJobConstants.LABSCRIPT))
    		{
    			throw new IllegalArgumentException("Expecting a script "
    					+ "pathname, but " + ShellJobConstants.LABSCRIPT 
    					+ " parameter is not found.");
    		}
    		
    		String script = params.getParameter(
    				ShellJobConstants.LABSCRIPT).getValueAsString();
    		script = script.replaceFirst("^~", System.getProperty("user.home")); 
    		File scriptFile = getNewFile(script);
    		String scriptPath = scriptFile.getAbsolutePath();
    		
    		command = new ArrayList<String>();
    		command.add(interpreter);
    		command.add(scriptPath);
    		// Quote EXE/SCRIPT so paths with spaces stay one word; leave
    		// ARGS raw so the shell can parse quotes and expand globs.
    		commandLine = shellQuote(interpreter) + " " + shellQuote(scriptPath);
    	} else if (params.contains(ShellJobConstants.LABCOMMAND))
    	{
    		String cmd = params.getParameter(
					ShellJobConstants.LABCOMMAND).getValueAsString();
    		command = new ArrayList<String>();
    		command.add(cmd);
    		// Raw CMD text: shell tokenizes and expands wildcards.
    		commandLine = cmd;
    	} else if (command != null && !command.isEmpty())
    	{
    		// Constructor-built components: quote each token so a single
    		// multi-word args component stays one argv entry (historical
    		// ProcessBuilder behaviour), while unquoted-safe globs still expand.
    		commandLine = command.stream()
    				.map(ShellJob::shellQuote)
    				.collect(Collectors.joining(" "));
    	}
    	
    	if (params.contains(ShellJobConstants.LABARGS))
    	{	
    		String args = params.getParameter(
					ShellJobConstants.LABARGS).getValueAsString();
    		if (command == null)
    		{
    			command = new ArrayList<String>();
    		}
    		command.add(args);
    		if (!commandLine.isEmpty())
    		{
    			commandLine += " ";
    		}
    		// Raw ARGS: shell parses quotes and expands globs.
    		commandLine += args;
    	}
    	
        logger.info("Running " + appID + " Job: " + this.toString() 
                + " Thread: " + Thread.currentThread().getName()
        		+ " " + TimeUtils.getTimestamp());

        if (commandLine != null && !commandLine.trim().isEmpty())
        {
            try
            {
                ProcessBuilder pb = new ProcessBuilder(wrapInShell(commandLine));
                if (customUserDir != null)
                {
                	// Here is where we move to the work space
                	pb.directory(customUserDir);
                }
                
                // Recover environmental variables to be exposed as output data
                // NB: this is the INITIAL environment for the VM! 
                // There is no way (yet) the get the environment after running 
                // the process... sadly.
                
                NamedData nd = new NamedData("INITIALENV"
                		, pb.environment().toString());
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
                    		exitCode));
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
                throw new RuntimeException("Error while running command line "
                                   + "operation '" + commandLine + "'.", t);
            }
        }

        logger.info("Done with " + appID + " Job " + this.toString() + " " 
        		+ TimeUtils.getTimestamp());
    }

//------------------------------------------------------------------------------

    /**
     * Wraps a command line so it is interpreted by a system shell.
     * @param commandLine the full command line to run.
     * @return argv for {@link ProcessBuilder}: shell, flag, command line.
     */
    static List<String> wrapInShell(String commandLine)
    {
    	if (isWindows())
    	{
    		return Arrays.asList("cmd.exe", "/c", commandLine);
    	}
    	return Arrays.asList("/bin/sh", "-c", commandLine);
    }

//------------------------------------------------------------------------------

    /**
     * Quotes {@code s} for inclusion in a POSIX {@code sh -c} command line.
     * Strings that contain only path-safe characters and glob metacharacters
     * ({@code * ? [ ]}) are left unquoted so the shell can expand them.
     * @param s the token to quote.
     * @return a shell-safe token.
     */
    static String shellQuote(String s)
    {
    	if (s == null || s.isEmpty())
    	{
    		return "''";
    	}
    	// Unquoted: avoid spaces and shell metacharacters that are not globs
    	if (s.matches("[A-Za-z0-9_./:@%+=,\\-\\*\\?\\[\\]]+"))
    	{
    		return s;
    	}
    	return "'" + s.replace("'", "'\\''") + "'";
    }

//------------------------------------------------------------------------------

    private static boolean isWindows()
    {
    	String os = System.getProperty("os.name");
    	return os != null && os.toLowerCase().contains("win");
    }
    
//------------------------------------------------------------------------------

    public static class ShellJobSerializer 
    implements JsonSerializer<ShellJob>
    {
        @Override
        public JsonElement serialize(ShellJob job, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONJOBTYPE, job.getClass().getSimpleName());
            
            if (!job.params.isEmpty())
            	jsonObject.add(JSONPARAMS, context.serialize(job.params));
            if (!job.steps.isEmpty())
            	jsonObject.add(JSONSUBJOBS, context.serialize(job.steps));
            jsonObject.add("command", context.serialize(job.command));

            return jsonObject;
        }
    }

//------------------------------------------------------------------------------

}
