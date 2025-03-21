package autocompchem.run;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.config.Configurator;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.ACCFileType;
import autocompchem.files.FileAnalyzer;
import autocompchem.io.IOtools;
import autocompchem.log.LogUtils;
import autocompchem.text.TextBlockIndexed;
import autocompchem.worker.Task;
import autocompchem.worker.WorkerConstants;


/**
 * Factory building jobs.
 * 
 * @author Marco Foscato
 */

public final class JobFactory
{
	/**
	 * The collection of registered types of {@link Job}s by the declared 
	 * running software identifier.
	 */
	private static Map<SoftwareId, Job> knownJobTypes = 
			new HashMap<SoftwareId, Job>();

	/**
	 * Singleton instance of this class
	 */
	private static JobFactory INSTANCE;

//------------------------------------------------------------------------------

	private JobFactory()
	{
		registerType(new ACCJob());
		registerType(new ShellJob());
	}

//-----------------------------------------------------------------------------

	/**
	 * Returns the singleton instance of this class, i.e., the sole factory of
	 * {@link Job}s that can be configured and used.
	 * @return the singleton instance.
	 */
	public synchronized static JobFactory getInstance()
	{
		if (INSTANCE==null)
			INSTANCE = new JobFactory();
		return INSTANCE;
	}

//-----------------------------------------------------------------------------

	/**
	 * Registers any type of {@link Job} using a concrete implementation as 
	 * example. The given object will not be used for any task.
	 */
	public synchronized void registerType(Object object)
	{
		if (object instanceof Job)
		{
			Job job = (Job) object;
			knownJobTypes.put(job.appID, job);
		} else {
			throw new Error("Registration of " 
					+ object.getClass().getSimpleName() 
					+ " has failed because the given example object is not an "
					+ "instance of "
					+ Job.class.getSimpleName() + ".");
		}
	}
	
//------------------------------------------------------------------------------
	
    /**
     * Build a {@link Job} from from an existing file.
     * @param file the file from which to read the definition of the job.
     * @return the resulting job
     */
	
	public static Job buildFromFile(File file)
	{
		return buildFromFile(file, null);
	}
	
//------------------------------------------------------------------------------
	
    /**
     * Build a {@link Job} from from an existing file.
     * @param file the file from which to read the definition of the job.
     * @param imposedStr a string that has to be used to replace 
     * {@value ParameterConstants#STRINGFROMCLI} in the definition 
     * of the job. This is a way to customize a general-purpose job definition 
     * making it specific for the given string.
     * @return the resulting job.
     * @throws IOException 
     */
	
	public static Job buildFromFile(File file, String imposedStr)
	{
    	Job job = null;
    	
    	ACCFileType type = FileAnalyzer.detectFileType(file);
    	switch (type)
    	{
    	case JSON:
    		try {
				job = buildFromJSONFile(file, imposedStr);
			} catch (IOException e) {
				Terminator.withMsgAndStatus("Format of file '" + file + "' does "
						+ "not allow to create a Job. Cause: " + e.getMessage(),
						-1, e);
			}
    		break;
    		
    	case PAR:
    	case TXT:
    		job = buildFromParametersFile(file, imposedStr);
    		break;
    		
		default:
			Terminator.withMsgAndStatus("Format of file '" + file + "' does "
					+ "not allow to create a Job. Please make sure the file is "
					+ "either a JSON file or TXT parameters file adhering to "
					+ "ACC format." , -1);
			break;	
    	}
    	return job;
	}
	
//------------------------------------------------------------------------------

    /**
     * Build a {@link Job} from from an existing definition stored in a JSON
     * file. It also
     * allows you to given string that will replace the placeholder (i.e.,
     * {@value ParameterConstants.STRINGFROMCLI}).
     * @param file the file from which to read the definition of the job.
     * @param imposedStr  a string that has to be used to replace 
     * {@value ParameterConstants#STRINGFROMCLI} in the definition 
     * of the job. This is a way to customize a general-purpose job definition 
     * making it specific for the given string.
     * @return the resulting job.
     * @throws IOException is the file is not readable somehow.
     */

    public static Job buildFromJSONFile(File file, String imposedStr) throws IOException
    {
    	Object obj = null;
    	if (imposedStr!=null)
    	{
    		obj = IOtools.readJsonFile(file, Job.class, 
    				ParameterConstants.STRINGFROMCLI, imposedStr);
    	} else {
    		obj = IOtools.readJsonFile(file, Job.class);
    	}
    	if (!(obj instanceof Job))
    	{
    		Terminator.withMsgAndStatus("Format of file '" + file + "' does "
    				+ "not allow to create a Job. Deseralized object is '"
    				+ obj.getClass().getName() + "'.", -1);
    	}
    	Job job = (Job) obj;
    	job = procesParametersUponJobCreation(job);
		return job;
    }

//------------------------------------------------------------------------------

    /**
     * Build a {@link Job} from from an existing definition stored in a 
     * text file adhering to the format of ACC's parameter's file. It also
     * allows you to given string that will replace the placeholder (i.e.,
     * {@value ParameterConstants.STRINGFROMCLI}).
     * @param file the file from which to read the definition of the job.
     * @param imposedStr  a string that has to be used to replace 
     * {@value ParameterConstants#STRINGFROMCLI} in the definition 
     * of the job. This is a way to customize a general-purpose job definition 
     * making it specific for the given string.
     * @return the resulting job
     */

    public static Job buildFromParametersFile(File file, String imposedStr)
    {
        List<TextBlockIndexed> blocks = FileAnalyzer.extractTextBlocks(
        		file,
                ParameterConstants.STARTJOB, //delimiter
                ParameterConstants.ENDJOB, //delimiter
                false,  //don't take only first
                false); //don't include delimiters
		
        if (imposedStr != null && !imposedStr.isBlank())
        {
	        for (TextBlockIndexed tb : blocks)
	        {
	        	tb.replaceAll(ParameterConstants.STRINGFROMCLI,imposedStr);
	        }
        }
        
        if (blocks.size() == 0)
        {
        	// Since there are no JOBSTART/JOBEND blocks we interpret the text
        	// as parameters for a single job
        	List<String> lines = IOtools.readTXT(file);
        	lines.add(ParameterConstants.RUNNABLEAPPIDKEY 
        			+ ParameterConstants.SEPARATOR + SoftwareId.ACC);
        	TextBlockIndexed tb = new TextBlockIndexed(lines, 0, 0, 0);
        	if (imposedStr != null && !imposedStr.isBlank())
        		tb.replaceAll(ParameterConstants.STRINGFROMCLI,imposedStr);
        	blocks.add(tb);
        }
        
        return createJob(blocks);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Creates a job from a collection of text blocks extracted from a
     * ACC's parameter's file
     * @param blocks the blocks of text, each corresponding to a single job
     * that can possibly contain nested jobs.
     * @return the outermost job, with any nested job within it.
     */
    
    private static Job createJob(List<TextBlockIndexed> blocks)
    {
    	// Unless there is only one set of parameters the outermost job serves
        // as a container of a possibly nested structure of sub-jobs.
        Job job = new Job();
        if (blocks.size() == 1)
        {
        	job = createJob(blocks.get(0));
        }
        else
        {
        	job = createJob(SoftwareId.ACC);
            for (TextBlockIndexed tb : blocks)
            {
                Job subJob = createJob(tb);
                job.addStep(subJob);
            }
        }
        return job;
    }
    
//------------------------------------------------------------------------------

    /**
     * Create a job from the text block of the job's parameters. Handles
     * nested text blocks creating nested jobs of any deepness.
     * @param tb the outermost text block that may include nested blocks
     * @return the job, possibly including nested sub-jobs
     */ 

    private static Job createJob(TextBlockIndexed tb)
    {
        ParameterStorage locPar = new ParameterStorage();
        locPar.importParameters(tb);
        
        Job job = createJob(locPar);
        if (tb.getNestedBlocks().size() > 0)
        {
        	//NB: here they are called steps, but for a parallelized job they
        	// are the independent jobs to be submitted in parallel
            for (TextBlockIndexed intTb : tb.getNestedBlocks())
            {
                // Recursive exploration of nested structure of TextBlocks
                Job subJob = createJob(intTb);
                job.addStep(subJob);
            }
        }
        
        return job;
    }
    
//------------------------------------------------------------------------------

    /**
     * Create a job from the parameters defining it.
     * Since {@link ParameterStorage} cannot be nested, this method does can
     * only create a single job.
     * @param params the collection of parameters defining the job.
     * @return the job defined by the given parameters.
     */ 

    private static Job createJob(ParameterStorage locPar)
    {
        Job job = new Job();
        
        SoftwareId appId = SoftwareId.ACC;
        if (locPar.contains(ParameterConstants.RUNNABLEAPPIDKEY))
        {
        	String app = locPar.getParameter(
        			ParameterConstants.RUNNABLEAPPIDKEY).getValueAsString();
        	appId = new SoftwareId(app.trim().toUpperCase());
        }
        job = createJob(appId);
        
        job.setParameters(locPar);
        
        job = procesParametersUponJobCreation(job);

        return job;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * We need to process some of the parameters read-in upon creation of the 
     * job.
     */
    private static Job procesParametersUponJobCreation(Job job)
    {
    	ParameterStorage params = job.getParameters();
    
    	Job newJob = job;
        if (params.contains(WorkerConstants.PARTASK))
        {
        	Task task = Task.make(params.getParameterValue(
        			WorkerConstants.PARTASK));
        	if (new JobEvaluator().getCapabilities().contains(task))
	        {
	        	if (params.contains(MonitoringJob.PERIODPAR) 
	        			|| params.contains(MonitoringJob.DELAYPAR))
	        	{
	        		newJob = new MonitoringJob();
	        	} else {
	        		newJob = new EvaluationJob();
	        	}

	            newJob.setParameters(params);
	        }
        }
        //TODO-gg test input from json
        
        if (params.contains(ParameterConstants.VERBOSITY))
        {
        	Configurator.setLevel(job.logger.getName(),
            		LogUtils.verbosityToLevel(Integer.parseInt(
            				params.getParameter(ParameterConstants.VERBOSITY)
            					.getValueAsString())));
        }
        
        if (params.contains(ParameterConstants.PARALLELIZE))
        {
        	int nThreadsPerSubJob = Integer.parseInt(params.getParameter(
        			ParameterConstants.PARALLELIZE).getValueAsString());
        	job.setNumberOfThreads(nThreadsPerSubJob);
        }
        return newJob;
    }

//------------------------------------------------------------------------------

    /**
     * Create a new job calling the appropriate subclass.
     * @param appID the application to be used to do the job.
     * @return the job, possibly including nested sub-jobs.
     */ 

    public static Job createJob(SoftwareId appID)
    {
    	return createJob(appID, 1);
    }
    
//------------------------------------------------------------------------------

    /**
     * Create a new job calling the appropriate subclass.
     * @param appID the application to be used to do the job.
     * @param nThreads max parallel threads for independent sub-jobs.
     * @return the job, possibly including nested sub-jobs.
     */ 

    synchronized static Job createJob(SoftwareId appID, int nThreads)
    {
    	if (INSTANCE==null)
    		getInstance();
    	
    	Job job;
    	if (knownJobTypes.containsKey(appID))
    	{
    		job = knownJobTypes.get(appID).makeInstance();
    	} else {
			job = new Job();
    	}
    	job.setNumberOfThreads(nThreads);
    	return job;
    }

//------------------------------------------------------------------------------

    /**
     * Create an empty job of the same type of the given one.
     * @param template the job from which to infer the type of the result
     * @return the new job.
     */ 

    public static Job createTypedJob(Job template)
    {
  		String type = template.getClass().getName();
  		Job newJob = null;
  		ClassLoader classLoader = template.getClass().getClassLoader();
  		try {
  			@SuppressWarnings("unchecked")
  			Class<? extends Job> c = 
              		(Class<? extends Job>) classLoader.loadClass(type);
  			for (@SuppressWarnings("rawtypes") Constructor constructor : 
	        	c.getConstructors()) 
	        {
  				try {
					newJob = (Job) constructor.newInstance();
				} catch (InstantiationException 
						| IllegalAccessException 
						| IllegalArgumentException 
						| InvocationTargetException e) {
					continue;
				}
  				break;
	        }
        } catch (NoClassDefFoundError | ClassNotFoundException  e) {
        	template.logger.error("Could not build job with type '"
        			+ type + "'.", e);
        }
  		return newJob;
    }

//------------------------------------------------------------------------------

}
