package autocompchem.run;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.core.config.Configurator;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
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

public class JobFactory
{

//------------------------------------------------------------------------------
	
    /**
     * Build a Job from from an existing definition stored in a file.
     * Currently supported file formats: 
     * <ul>
     * <li><i>jobDetails</i> format.</li>
     * </ul>
     * @param file the file to read
     * @return the collection of parameters
     */

    public static Job buildFromFile(File file)
    {
        List<TextBlockIndexed> blocks = FileAnalyzer.extractTextBlocks(
        		file,
                ParameterConstants.STARTJOB, //delimiter
                ParameterConstants.ENDJOB, //delimiter
                false,  //don't take only first
                false); //don't include delimiters
        
        if (blocks.size() == 0)
        {
        	// Since there are no JOBSTART/JOBEND blocks we interpret the text
        	// as parameters for a single job
        	List<String> lines = IOtools.readTXT(file);
        	lines.add(ParameterConstants.RUNNABLEAPPIDKEY 
        			+ ParameterConstants.SEPARATOR + AppID.ACC);
        	TextBlockIndexed tb = new TextBlockIndexed(lines, 0, 0, 0);
        	blocks.add(tb);
        }
        
        return createJob(blocks);
    }
	
//------------------------------------------------------------------------------

    /**
     * Build a Job from from an existing definition stored in a file and
     * a given string that will replace the placeholder (i.e.,
     * {@value ParameterConstants.STRINGFROMCLI}).
     * Currently supported file formats: 
     * <ul>
     * <li><i>jobDetails</i> format.</li>
     * </ul>
     * @param file the file to read
     * @return the collection of parameters
     */

    public static Job buildFromFile(File file, String cliString)
    {
        List<TextBlockIndexed> blocks = FileAnalyzer.extractTextBlocks(
        		file,
                ParameterConstants.STARTJOB, //delimiter
                ParameterConstants.ENDJOB, //delimiter
                false,  //don't take only first
                false); //don't include delimiters
		
        for (TextBlockIndexed tb : blocks)
        {
        	tb.replaceAll(ParameterConstants.STRINGFROMCLI,cliString);
        }
        
        if (blocks.size() == 0)
        {
        	// Since there are no JOBSTART/JOBEND blocks we interpret the text
        	// as parameters for a single job
        	List<String> lines = IOtools.readTXT(file);
        	lines.add(ParameterConstants.RUNNABLEAPPIDKEY 
        			+ ParameterConstants.SEPARATOR + AppID.ACC);
        	TextBlockIndexed tb = new TextBlockIndexed(lines, 0, 0, 0);
        	tb.replaceAll(ParameterConstants.STRINGFROMCLI,cliString);
        	blocks.add(tb);
        }
        
        return createJob(blocks);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Creates a job from a collection of text blocks typically extracted from a
     * job details file.
     * @param blocks the blocks of text, each corresponding to a single job
     * that can possibly contain nested jobs.
     * @return the outermost job, with any nested job within it.
     */
    
    public static Job createJob(List<TextBlockIndexed> blocks)
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
        	job = createJob(AppID.ACC);
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
     * Create a new job calling the appropriate subclass.
     * @param appID the application to be used to do the job.
     * @return the job, possibly including nested sub-jobs.
     */ 

    public static Job createJob(AppID appID)
    {
    	return createJob(appID, 1, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Create a new job calling the appropriate subclass.
     * @param appID the application to be used to do the job.
     * @param nThreads max parallel threads for independent sub-jobs.
     * @return the job, possibly including nested sub-jobs.
     */ 

    public static Job createJob(AppID appID, int nThreads)
    {
    	return createJob(appID, nThreads, false);
    }

  //------------------------------------------------------------------------------

    /**
     * Create a new job calling the appropriate subclass.
     * @param appID the application to be used to do the job.
     * @param parallelizable set <code>true</code> if this job can be 
     * parallelized.
     * @return the job, possibly including nested sub-jobs.
     */ 

    public static Job createJob(AppID appID, boolean parallelizable)
    {
    	return createJob(appID, 1, parallelizable);
    }
    
//------------------------------------------------------------------------------

    /**
     * Create a new job calling the appropriate subclass.
     * @param appID the application to be used to do the job.
     * @param nThreads max parallel threads for independent sub-jobs.
     * @param parallelizable set <code>true</code> if this job can be 
     * parallelized.
     * @return the job, possibly including nested sub-jobs.
     */ 

    public static Job createJob(AppID appID, int nThreads, 
    		boolean parallelizable)
    {
    	Job job;
    	switch (appID) 
    	{
			case ACC: {
				job = new ACCJob();
				break;
			}
			case SHELL: {
				job = new ShellJob();
				break;
			}
			default: {
				job = new Job();
				break;
			}
    	}
    	job.setParallelizable(parallelizable);
    	job.setNumberOfThreads(nThreads);
    	return job;
    }


//------------------------------------------------------------------------------

    /**
     * Create a job from the text block of the job's parameters. Handles
     * nested text blocks creating nested jobs of any deepness.
     * @param tb the outermost text block that may include nested blocks
     * @return the job, possibly including nested sub-jobs
     */ 

    public static Job createJob(TextBlockIndexed tb)
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

    public static Job createJob(ParameterStorage locPar)
    {
        Job job = new Job();
        
        AppID appId = AppID.ACC;
        if (locPar.contains(ParameterConstants.RUNNABLEAPPIDKEY))
        {
        	String app = locPar.getParameter(
        			ParameterConstants.RUNNABLEAPPIDKEY).getValueAsString();
        	appId = AppID.valueOf(app.trim().toUpperCase());
        }
        job = createJob(appId);

        if (locPar.contains(WorkerConstants.PARTASK))
        {
        	Task task = Task.make(locPar.getParameterValue(
        			WorkerConstants.PARTASK));
        	if (new JobEvaluator().getCapabilities().contains(task))
	        {
	        	if (locPar.contains(MonitoringJob.PERIODPAR) 
	        			|| locPar.contains(MonitoringJob.DELAYPAR))
	        	{
	        		job = new MonitoringJob();
	        	} else {
	        		job = new EvaluationJob();
	        	}
	        }
        }
        
        if (locPar.contains(ParameterConstants.VERBOSITY))
        {
        	Configurator.setLevel(job.logger.getName(),
            		LogUtils.verbosityToLevel(Integer.parseInt(
            				locPar.getParameter(ParameterConstants.VERBOSITY)
            					.getValueAsString())));
        }
        
        if (locPar.contains(ParameterConstants.PARALLELIZE))
        {
        	int nThreadsPerSubJob = Integer.parseInt(locPar.getParameter(
        			ParameterConstants.PARALLELIZE).getValueAsString());
        	job.setParallelizable(true);
        	job.setNumberOfThreads(nThreadsPerSubJob);
        }
        
        if (locPar.contains(ParameterConstants.PARALLELIZABLE))
        {
        	job.setParallelizable(true);
        }
        
        job.setParameters(locPar);

        return job;
    }

//------------------------------------------------------------------------------

}
