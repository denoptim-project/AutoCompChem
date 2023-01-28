package autocompchem.ui;

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

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.run.ACCJob;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
/**
 * Main for AtomCompChem (Automated Computational Chemist). The entry point
 * for both CLI and GUI based runs.
 *
 * @version 3 Aug 2020
 * @author Marco Foscato
 */

public class ACCMain
{
    //Software version number //TODO: move to logging class
    private static final String version = "2.0";
    
    // System.spec line separator
    private static final String NL = System.getProperty("line.separator");

    /**
     * The command line argument that triggers the printing of the help message.
     */
	private static final Object CLIHELP = "--HELP";

    /**
     * The short form of the command line argument that triggers the printing of
     *  the help message.
     */
	private static final Object CLIHELPSHORT = "-H";

//------------------------------------------------------------------------------
    
    /**
     * The entry point of ACC runs submitted from command line.
     * @param args the command line arguments
     */
    
    public static void main(String[] args)
    {
        // Logging in message
        printInit();
        
        // Detect kind of run (command line arguments or parameter file)
        // and what is the job to be done
        Job job = null;
        if (args.length < 1)
        {
        	//TODO eventually here we will launch the gui.
            printUsage();
            Terminator.withMsgAndStatus("ERROR! No input or command line "
                + "argument given. " + NL
                + "AutoCompChem requires either command line arguments, or "
                + "a single argument that is the pathname to a parameters "
                + "file.",1);
        }
        else if (args.length == 1)
        {
        	String pathName = args[0];
        	
        	if (CLIHELP.equals(pathName.toUpperCase()) 
        			|| CLIHELPSHORT.equals(pathName.toUpperCase()))
        	{
        		printUsage();
        		Terminator.withMsgAndStatus("Normal termination", 0);
        	}
        	
            try {
            	job = JobFactory.buildFromFile(pathName);
            } catch (Throwable t) {
            	t.printStackTrace();
                String msg = "ERROR! Exception returned while reading "
                		+ "job settings from file '" + pathName + "'.";
                Terminator.withMsgAndStatus(msg,-1);
            }
        }
        else if (args.length > 1)
        {
        	job = parseCLIArgs(args);
        	
        	boolean requiresHelp = false;
        	for (int i=0; i<args.length; i++)
        	{
        		if (CLIHELP.equals(args[i].toUpperCase()) 
            			|| CLIHELPSHORT.equals(args[i].toUpperCase()))
            	{
        			requiresHelp = true;
        			break;
            	}
        	}
        	
        	if (requiresHelp)
        	{
	        	if (job instanceof ACCJob)
	        	{
	        		Worker w = ((ACCJob) job).getUninitializedWorker();
	        		System.out.println(w.getTaskSpecificHelp());
	        	} else {
	        		//TODO-gg what here?
	        		System.out.println("Not implemented yet!!! ");
	        	}
	        	Terminator.withMsgAndStatus("Exiting upon request to print "
	        			+ "help message",0);
        	}
        }

        // Do the task
        try {
            job.run();
        } catch (IllegalArgumentException iae) {
        	Terminator.withMsgAndStatus("ERROR! Input led to illegal argument. "
        			+ NL + "Hint: " + iae.getMessage(), -1);
        } catch (Throwable t) {
            t.printStackTrace();
            String msg = t.getMessage();
            if (msg == null)
            {
                Terminator.withMsgAndStatus("Exception occurred! But 'null' "
                        + "message returned. Please "
                        + "report this to the author.", -1);
            }

            if (msg.startsWith("ERROR!"))
            {
                Terminator.withMsgAndStatus(t.getMessage(), -1);
            } else {
                t.printStackTrace();
                Terminator.withMsgAndStatus("Exception occurred! Please "
                        + "report this to the author.", -1);
            }
        }
        
        // Exit
        Terminator.withMsgAndStatus("Normal Termination",0);
    }

//------------------------------------------------------------------------------
    
	/**
     * Parses the vector of command line arguments.
     * @param args the vector of arguments to be parsed.
     * @return job the job set up from the parameters parsed from command line.
     */
    
    protected static Job parseCLIArgs(String[] args)
    {
    	Job job = null;
    	
    	ParameterStorage params = new ParameterStorage();
    	params.setDefault();
    	
    	// First, look for the -t/--task or for -p/--params: 
    	// either one must be there
    	boolean foundTask = false;
    	String task = null;
    	boolean foundParams = false;
    	String paramsFile = null;
    	String cliString = "";
    	for (int iarg=0; iarg<args.length; iarg++)
    	{
    		String arg = args[iarg];
    		if (arg.equalsIgnoreCase("-t") || arg.equalsIgnoreCase("--task"))
    		{	
    			if (iarg+1 >= args.length)
    			{
    				Terminator.withMsgAndStatus("ERROR! Option -t (--task)"
    						+ " seems to have no value. I expect to find "
    						+ "something like '-t <taskID>', but I see "
    						+ "only '" + arg + "'.",-1);
    			}
    			task = args[iarg+1];
    			foundTask = true;
    		}
    		
    		if (arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("--params"))
    		{	
    			if (iarg+1 >= args.length)
    			{
    				Terminator.withMsgAndStatus("ERROR! Option -p (--params)"
    						+ " seems to have no value. I expect to find "
    						+ "something like '-p <pathname>', but I see "
    						+ "only '" + arg + "'.",-1);
    			}
    			paramsFile = args[iarg+1];
    			foundParams=true;
    		}
    		
    		if (arg.equalsIgnoreCase("--"+ParameterConstants.STRINGFROMCLI))
    		{	
    			if (iarg+1 >= args.length)
    			{
    				Terminator.withMsgAndStatus("ERROR! Option " + arg
    						+ " seems to have no value. I expect to find "
    						+ "a string after '" + arg + "'.",-1);
    			}
    			cliString = args[iarg+1];
    		}
    	}
    	
    	// Check consistency between use of -t and -p
    	if (foundTask && foundParams)
    	{
    		Terminator.withMsgAndStatus("ERROR! Found both -t/--task and "
    				+ "-p/--params options. "
    				+ "You can use only one of the two.",-1);
    	}
    	if (!foundTask && !foundParams)
    	{
    		Terminator.withMsgAndStatus("ERROR! Neither -t/--task nor "
    				+ "-p/--params options found. "
    				+ "You must use either -t/-p, or provide a single argument"
    				+ " that is the pathname to an existing paremeters file.",
    				-1);
    	}
    	
    	if (foundTask)
    	{
			//NB: this will kill me with an error message in case 
			// the given string does not correspond to a registered 
			// task.
			TaskID.getFromString(task);
			params.setParameter(WorkerConstants.PARTASK, task);
    	}
    	
    	if (foundParams)
    	{
    		//NB: this will kill me with an error if the file is not found
			// or not readable.
			FileUtils.foundAndPermissions(paramsFile, true, false, false);
			
			if (cliString.equals(""))
			{
				job = JobFactory.buildFromFile(paramsFile);
			} else {
				job = JobFactory.buildFromFile(paramsFile,cliString);
			}
    	}
    	
    	//Read-in all CLI arguments in parameter storage unit
	    	
    	//NB: the following block of code makes it so that we can only run
    	// single step jobs when submitting via CLI interface using CLI 
    	// arguments/options and -t/--task option
		
    	for (int iarg=0; iarg<args.length; iarg++)
    	{
    		String arg = args[iarg];
    		
    		//Skip -t/--task
    		if (arg.equalsIgnoreCase("-t") 
    				|| arg.equalsIgnoreCase("--task"))
    		{
    			iarg++;
    			continue;
    		}
	    		
    		//Read-in the option or the key:value pair
    		arg = arg.replaceFirst("^-*", "");
    		if ((iarg+1 >= args.length) || args[iarg+1].startsWith("-"))
			{
    			// A value-less parameter
    	    	if (foundTask && !foundParams)
    	    	{
    	    		params.setParameter(arg);
    	    	} else if (!foundTask && foundParams)
    	    	{
    	    		job.setParameter(arg);
    	    	}
			}
    		else
    		{
    			// There is a value of some sort, and we read it in
    			String value = "none";
    			if (args[iarg+1].startsWith("\""))
    			{
        			value = args[iarg+1];
        			for (int jarg=iarg+2; jarg<args.length; jarg++)
        			{
        				value = value + " " + args[jarg];
        				iarg++;
        				if (args[jarg].contains("\""))
        				{
        					break;
        				}
        			}
        			iarg++; // this is the one of the very first word
        			value = value.substring(1,value.lastIndexOf("\""));
    			}
    			else
    			{
    				value = args[iarg+1];
    				iarg++;
    			}
    			
    			if (foundTask && !foundParams)
    	    	{
    	    		params.setParameter(arg, value);
    	    	} else if (!foundTask && foundParams)
    	    	{
    	    		job.setParameter(arg, value);
    	    	}
    		}
	    }
	    	
    	if (foundTask && !foundParams)
    	{
	    	// Finally, pack all into a job
	    	job = JobFactory.createJob(RunnableAppID.ACC);
	    	job.setParameters(params);
    	}
    	
    	if (job == null)
    	{
    		Terminator.withMsgAndStatus("ERROR! Could not parse command line "
    				+ "arguments to make a job. Check your input.",-1);
    	}
    	
    	return job;
    }

//------------------------------------------------------------------------------

    /**
     * Write the initial log message.
     */
    
    private static void printInit()
    {
    	
        System.out.println(NL + NL 
        		+ "**********************************************"
                + "*****************************"
                + NL + "                              AutoCompChem"
                + NL + "                              Version: " + version
                + NL + "**********************************************"
                + "*****************************" + NL);
    }

//------------------------------------------------------------------------------
    
    /**
     * Write the manual/usage information to stdout.
     */
    
    private static void printUsage()
    {
    	String s = NL + " Alternative usage from the command line: " + NL
    		+ NL + " java -jar AutoCompChem.jar <parameters_file>"
    		+ NL + " java -jar AutoCompChem.jar -t/--task <task> [more args]"
			+ NL + " java -jar AutoCompChem.jar -p/--params "
					+ "<file> [more args]"
			+ NL + NL + " Where:" + NL
			+ NL + "  -t/--task and -p/--params indicate that these options"
			+ " can be specified "
			+ NL + "         using either a long (e.g., --task) or short "
			+ "(e.g., -t) version." + NL
			+ NL + "  <file> is the filename or pathname to a file "
			+ "containing the job details." + NL
			+ NL + "  <task> is any string among the following ones. "
			+ "(case-insensitive).";
    	
    	String indent ="          -> ";
    	for (TaskID t : TaskID.values())
    	{
    		switch (t) 
    		{
				case UNSET:
				case DUMMYTASK: 
					break;
				default:
					s = s + NL + indent + t;
					break;
			}
    	}
    	s = s + NL + NL 
    			+ "  [more args] are optional arguments that depend on the "
    			+ "task at hand. " + NL 
    			+ "  See user manual for further instructions.";
        System.out.println(s);
    }
    
//------------------------------------------------------------------------------
    
    /*
    private static void printHelp() 
    {
    	//TODO implement help that prints options available based on the 
    	// arguments already present in the CLI
	}
	*/

//------------------------------------------------------------------------------
}
