package autocompchem.ui;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.Job;
import autocompchem.run.Job.RunnableAppID;
import autocompchem.run.JobFactory;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
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
            ParameterStorage ACCParameters = new ParameterStorage();
            ACCParameters.setDefault();
        	parseCLIArgs(args, ACCParameters);
        	job = JobFactory.createJob(RunnableAppID.ACC);
        	job.setParameters(ACCParameters);
        }

        // Do the task
        try {
            job.run();
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
                Terminator.withMsgAndStatus(t.getMessage(),-1);
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
     * @param params the storage where parsed parameters will be stored.
     * @return the string defining the task, or null.
     */
    
    protected static String parseCLIArgs(String[] args, ParameterStorage params)
    {
    	String task = null;

    	// First, look for the -t/--task
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
    						+ "only '-t' or '--task'.",-1);
    			}
    			task = args[iarg+1];
    			//NB: this will kill me with an error message in case 
    			// the given string does not correspond to a registered 
    			// task.
    			TaskID.getFromString(task);
    			
    			Parameter par = new Parameter(WorkerConstants.PARTASK, 
    					NamedDataType.STRING,task);
    			params.setParameter(par);
    			
    			break;
    		}
    	}
    	
    	//Then, read-in all CLI arguments in parameter storage unit
    	
    	//NB: the following block of code makes it so that we can only run
    	// single step jobs when submitting via CLI interface using CLI 
    	// arguments/options
    	
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
    			Parameter par = new Parameter(arg, NamedDataType.STRING, 
    					"none");
    			params.setParameter(par);
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
    			Parameter par = new Parameter(arg, NamedDataType.STRING, 
    					value);
    			params.setParameter(par);
    		}
    	}
    	return task;
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
     * Write the manual/usage information.
     */
    
    private static void printUsage()
    {
        System.out.println(NL + " Usage: "
        		+ NL + " java -jar AutoCompChem.jar <parameters_file>" + NL
        		+ NL + " java -jar AutoCompChem.jar -t <task> [other command "
        				+ "line options]");
    }

//------------------------------------------------------------------------------
}
