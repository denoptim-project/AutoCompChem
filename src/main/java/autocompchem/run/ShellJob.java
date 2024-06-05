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
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.utils.StringUtils;
import autocompchem.utils.TimeUtils;

/**
 * A shell job is work to be done by the shell. The shell command can be executed
 * in a newly created subfolder. In this case any pathname should reflect the
 * fact that `pwd` would return the pathname of the subfolder.
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
        this.appID = AppID.SHELL;
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
        this.appID = AppID.SHELL;
        this.command = new ArrayList<String>();
        this.command.add(interpreter);
        this.command.add(script);
        this.command.add(args);
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
        this.appID = AppID.SHELL;
        this.command = new ArrayList<String>();
        this.command.add(interpreter);
        this.command.add(script);
        this.command.add(args);
        this.customUserDir = customUserDir;
    }

//------------------------------------------------------------------------------

    /**
     * Runs this SHELL command
     */

    @Override
    public void runThisJobSubClassSpecific()
    {
		if (params.contains(ShellJobConstants.LABINTERPRETER)
				&& params.contains(ShellJobConstants.LABCOMMAND))
		{
			Terminator.withMsgAndStatus("ERROR! Cannot have both "
					+ ShellJobConstants.LABCOMMAND + " and " 
					+ ShellJobConstants.LABINTERPRETER + " as parameters "
					+ "in a shell job. Use either one or the other.",-1);
		}
		
    	// First we need to see if the command comes from the constructor or
    	// from parameter storage
    	if (params.contains(ShellJobConstants.LABINTERPRETER))
    	{
    		command = new ArrayList<String>();
    		command.add(params.getParameter(
    				ShellJobConstants.LABINTERPRETER).getValueAsString());
    	
    		if (!params.contains(ShellJobConstants.LABSCRIPT))
    		{
    			Terminator.withMsgAndStatus("ERROR! Expecting a script "
    					+ "pathname, but " + ShellJobConstants.LABSCRIPT 
    					+ " parameter is not found.", -1);
    		}
    		
    		String script = params.getParameter(
    				ShellJobConstants.LABSCRIPT).getValueAsString();
    		script = script.replaceFirst("^~", System.getProperty("user.home")); 
    		File scriptFile = new File(script);
    		command.add(scriptFile.getAbsolutePath());
    	} else if (params.contains(ShellJobConstants.LABCOMMAND))
    	{
    		command = new ArrayList<String>();
    		Pattern regexMatchingArgs = Pattern.compile(
    				"[^\\s\"']+|\"[^\"]*\"|'[^']*'");
    		Matcher matcher = regexMatchingArgs.matcher(params.getParameter(
					ShellJobConstants.LABCOMMAND).getValueAsString());
    		while (matcher.find()) 
    		{
    		    command.add(matcher.group());
    		}	
    	}
    	
    	if (params.contains(ShellJobConstants.LABARGS))
    	{	
			Pattern regexMatchingArgs = Pattern.compile(
    				"[^\\s\"']+|\"[^\"]*\"|'[^']*'");
    		Matcher matcher = regexMatchingArgs.matcher(params.getParameter(
					ShellJobConstants.LABARGS).getValueAsString());
    		while (matcher.find()) 
    		{
    		    command.add(matcher.group());
    		}
    	}
    	
    	// We might want to run this in a subfolder
    	if (params.contains(ShellJobConstants.WORKDIR))
    	{
    		File workDir = new File(params.getParameter(
    				ShellJobConstants.WORKDIR).getValueAsString());
    		if (!workDir.exists() && !workDir.mkdirs())
    		{
    			Terminator.withMsgAndStatus("ERROR! Could not make the "
    					+ "required subfolder '" + workDir + "'.",-1);
    		}
    		System.out.println("WARNING: setting work directory to '"
    				+ workDir + "'.");
    		this.setUserDirAndStdFiles(workDir);
    	}
    	
    	if (params.contains(ShellJobConstants.COPYTOWORKDIR))
    	{
    		String listAsStr = params.getParameter(
    				ShellJobConstants.COPYTOWORKDIR).getValueAsString();
    		String[] list = listAsStr.split(",");
    		for (int i=0; i<list.length; i++)
    		{
    			File source = new File(list[i].trim());
    			File dest = new File(this.customUserDir 
    					+ System.getProperty("file.separator")
    					+ source.getName());
    			if (source.exists())
    			{
    				try {
						com.google.common.io.Files.copy(source,dest);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Terminator.withMsgAndStatus("ERROR! Could not copy "
								+ "file '" + source + "' to work directory.",-1);
					}
    			} else {
    				System.out.println("WARNING: file '" + source 
    						+ "' was listed among "
    						+ "those to copy into the work directory, "
    						+ "but it does not exist. I'll skipp it.");
    			}
    		}
    	}
    	
        logger.info("Running " + appID + " Job: " + this.toString() 
                + " Thread: " + Thread.currentThread().getName()
        		+ " " + TimeUtils.getTimestamp());
        
        String commandAsString = StringUtils.mergeListToString(command, " ");

        if (!commandAsString.trim().isEmpty())
        {
            try
            {
                ProcessBuilder pb = new ProcessBuilder(command);
                if (customUserDir != null)
                {
                	// Here is where we move to the work space
                	pb.directory(customUserDir);
                }
                
                // Recover environmental variables to be exposed as output data
                // NB: this is the INITIAL environment for the VM! 
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

        logger.info("Done with " + appID + " Job " + this.toString() + " " 
        		+ TimeUtils.getTimestamp());
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

            jsonObject.addProperty(JSONJOVTYPE, job.getClass().getSimpleName());
            
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
