package autocompchem.run.jobediting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.MonitoringJob;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;
import autocompchem.utils.SetUtils;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.WIROConstants;

/**
 * A tool for performing {@link Action}s that edit a workflow, 
 * i.e., a {@link Job} that defines a sequence or a batch of sub {@link Job}s, 
 * i.e., the steps.
 * 
 * @author Marco Foscato
 */

public class ActionApplier 
{
  	
//------------------------------------------------------------------------------

    /**
     * <p>Performs the {@link Action} that alters a serial workflow, i.e.,
     * the steps of a {@link Job}. 
     * Since {@link Action}s are triggered by the evaluation of
     * step, it required to define which one is the step that triggered the 
     * {@link Action} (i.e., the triggering step).</p>
     * 
     * <p>Note that any action is expected to be molecule-agnostic, so any change 
     * on job features that are dependent on the identify of the chemical system
     *  should not be done here.</p>
     *  
     * <p>Note that this also calls {@link Job#resetRunStatus()} on the jobs
     * in <code>todoJobs</code> so that any usage of info stored in such jobs
     * must be done before calling this method.</p>
     * 
  	 * @param action The action to be performed on the workflow.
  	 * @param workflow The job that defined the workflow.
  	 * @param triggeringStepId the index of the step of the original
  	 * workflow that performed in such way as to trigger a reaction
  	 * from a monitoring/evaluation job. It is not the evaluation/monitoring
  	 * job. It is the evaluated/monitored job.
  	 * @param restartCounter an unique counter used to identify the version of
  	 * the workflow. It is used to archive data from 
  	 * previous runs of the workflow.
	 * @param customUserDir the custom user directory of the job that triggered the action.
  	 */
    
    public static void performActionOnSerialWorkflow(Action action, 
    		Job workflow, int triggeringStepId, int restartCounter,
			File customUserDir)
    {
    	ActionType aType = action.getType();
    	// No need to archive or edit the workflow if we just stop it. 
    	if (aType==ActionType.STOP)
    		return;
    	
    	// If triggering step ID is -1, it means the job wasn't found
    	if (triggeringStepId < 0)
    	{
    		Logger logger = LogManager.getLogger(ActionApplier.class);
    		logger.warn("Triggering step ID is " + triggeringStepId + 
    				", which means the focus job was not found. Skipping action processing.");
    		return;
    	}
    	
    	// Validate triggering step ID
    	if (triggeringStepId >= workflow.getNumberOfSteps())
    	{
    		throw new IllegalArgumentException("Invalid triggering step ID: " 
    				+ triggeringStepId + ". Workflow has " 
    				+ workflow.getNumberOfSteps() + " steps.");
    	}
    	
    	// Modify the workflow: Trim steps to have as first step the first one
    	// that should re-run.
    	if (aType==ActionType.REDO)
    	{
    		for (int i=0; i<triggeringStepId; i++)
    			workflow.getSteps().remove(0);
    	}
    	Job actionTiggeringStep = workflow.getStep(0);
    	if (aType==ActionType.SKIP && workflow.getNumberOfSteps()>0)
    	{
    		if (workflow.getNumberOfSteps()>0)
    			workflow.getSteps().remove(0);
    		else
    			return;
    	}
    	
    	// Archive previous results found on disk.
    	// NB: here we assume that all files in the work directory relate
    	// to a single workflow, i.e., no other independent workflow is using 
    	// the same work directory. 
    	// Also, we assume any of the jobs to re-do, is not running.
    	// So we archive any of the files that, according to the patterns,
    	// should be those somehow connected with the job steps run so far.
		archivePreviousResults(actionTiggeringStep, triggeringStepId, restartCounter, action, customUserDir);
    	
    	// Pre-pend (i.e., added in front) any step that should be pre-pended
    	for (int i=(action.prerefinementSteps.size()-1); i>-1; i--)
    	{
    		Job preliminaryStep = action.prerefinementSteps.get(i);
    		for (IJobSettingsInheritTask jsi : action.inheritedSettings)
    		{
    			try {
					jsi.inheritSettings(actionTiggeringStep, preliminaryStep);
				} catch (CloneNotSupportedException e) {
					throw new RuntimeException("trying to clone data "
							+ "that cannot be cloned. If you see the need to "
							+ "do such operation, please, contact the "
							+ "developers and present your use case.", e);
				}
    		}
    		workflow.getSteps().add(0, preliminaryStep);
    	}
    	
    	// Modify job settings
    	for (IJobEditingTask jet : action.jobEditTasks)
    	{
    		// Checks if the action applies to the downstream workflow
    		if (action.getObject().equals(ActionObject.FOCUSANDFOLLOWINGJOBS)
    				&& workflow.getNumberOfSteps()>0)
    		{
    			for (Job step : workflow.getSteps())
    			{
    				jet.applyChange(step);
    			}
    		} else {
    			jet.applyChange(actionTiggeringStep);
    		}
    	}
    	
    	// NB: any data that may be needed to restart of fix things should be 
    	// taken and used before resetting the jobs.
    	
    	// Reset status of all jobs to re-run them.
    	for (Job job : workflow.getSteps())
    	{
    		if (aType==ActionType.SKIP && job==actionTiggeringStep)
    			continue;
    		
    		job.resetRunStatus();
    	}
    }

//------------------------------------------------------------------------------

	/**
	 * Performs the {@link Action} that alters the input preparation job.
	 * @param action The action to be performed. Mind that this could be a "cure"
	 * action that alters the input preparation job to prepare the input for a 
	 * job that will be run after the input preparation job.
	 * @param inputPreparationJob The job configured to prepare the input for 
	 * the job that will be run after the input preparation job.
	 */
    public static void performActionOnInputPreparationJob(Action action, 
		Job inputPreparationJob)
    {
        for (IJobEditingTask jet : action.inputEditingTasks)
        {
            jet.applyChange(inputPreparationJob);
        }
	}
    
//------------------------------------------------------------------------------
    
    /**
     * Does anything that is defined in the given {@link Action} to the given 
     * batch of parallel {@link Job}s defined in a {@link Job} that acts as 
     * container of the parallel batch.
     * @param action defined what to do
     * @param batch is the list of jobs that are/were run in parallel and that
     * contain the job that triggered the action.
     * @param triggerJob the job that with its behavior triggered the action
     * @param jobRequestingAction the job that detected the triggering behavior
     * and created the request for action.
     * @param restartCounter an index that identifies the iteration on the batch
     * and is used to archive the files in a way that allows to identify which 
     * iteration created archives.
     * @return the altered batch of jobs. 
     */
    public static List<Job> performActionOnParallelBatch(Action action, 
    		  Job batch, Job triggerJob, EvaluationJob jobRequestingAction, 
    		  int restartCounter)
    {
    	ActionType aType = action.getType();
		
    	// No need to archive or edit the batch if we just stop or skip. 
    	if (aType==ActionType.STOP || aType==ActionType.SKIP)
    		return new ArrayList<Job>();

		List<Job> actionObjects = new ArrayList<Job>();
		switch (action.getObject()) 
		{	
			case PARALLELJOB:
			{
				actionObjects.addAll(batch.getSteps());
				break;
			}
			
			case FOCUSJOB:
			default:
			{
				actionObjects.add(triggerJob);
				break;
			}
		}
    	
    	// Modify the batch of parallel job: keep only what should be re run.
		List<Job> stepsToReRun  = new ArrayList<Job>();
		switch (aType) 
		{
			case REDO:
			{
				for (Job j : batch.getSteps())
	    		{
	    			if (actionObjects.contains(j) || j==jobRequestingAction)
	    				stepsToReRun.add(j);
	    		}
				break;
			}
			
			// Both SKIP and STOP cannot have been intercepted before, but we 
			// keep them to facilitate the identification on incomplete switch 
			// block in case other action types are added in the future,
			// instead of using the 'default' case.
			case SKIP:
			case STOP:
			{
				break;
			}
		}

		int triggeringStepId = batch.getSteps().indexOf(triggerJob);
    	
    	// Archive previous results found on disk for the steps to rerun.
    	// NB: here we assume that all files in the work directory relate
    	// to a single master job, i.e., no other independent job is using 
    	// the same work directory. 
    	// Also, we assume any of the jobs to rerun, is not running.
    	// So we archive any of the files that, according to the patterns,
    	// should be those somehow connected with the jobs to rerun.
    	archivePreviousResults(stepsToReRun, restartCounter, 
    		triggeringStepId, action, jobRequestingAction.getUserDir());
    	
    	// Pre-pend (i.e., added in front) is not possible in parallel batches
    	if (action.prerefinementSteps != null && 
    			action.prerefinementSteps.size()>0)
    	{
			throw new UnsupportedOperationException("trying to pre-pend steps in a"
					+ "parallel batch of jobs. This is not possible in this"
					+ "implementation. If you see the need to pre-pend steps"
					+ "itno a parallel batch, please, contact the "
					+ "developers and present your use case.");
    	}
    	
    	// Modify job settings
    	for (IJobEditingTask jet : action.jobEditTasks)
    	{
			for (Job step : actionObjects)
			{
				jet.applyChange(step);
			}
    	}
    	
    	// NB: any data that may be needed to restart of fix things should be 
    	// taken and used before resetting the jobs.
    	
    	// Reset status of all jobs to re-run them.
    	for (Job job : stepsToReRun)
    	{
    		job.resetRunStatus();
    	}
    	
    	return stepsToReRun;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Moves files related to the given jobs into dedicated archive sub-folders
     * named after the <code>restartCounter</code>.
     * @param jobs list of jobs that may be altered by the action. None
	 * of these jobs should be running.
	 * @param restartCounter an unique counter used to archive data from 
	 * previous runs of the jobs that may be altered by the action. We use this 
	 * to create archives for existing data generated by previous runs of the
	 * jobs that may be altered.
	 * @param action the object defining what are the tasks to perform in 
	 * the job's files.
	 * @param customUserDir the custom user directory of the evaluation job 
	 * that triggered the action. This is used if the jobs to archive do not 
	 * define their own user directory.	
     */
    static void archivePreviousResults(List<Job> jobs, int restartCounter,
    		int idxTriggeringStep, Action action,
			File customUserDir)
    {
		for (Job job : jobs)
		{
			if (job instanceof MonitoringJob)
				continue;
			
			archivePreviousResults(job, idxTriggeringStep, restartCounter, action, customUserDir);
		}
    }
 
//------------------------------------------------------------------------------
    
    /**
     * Moves files related to the given job into dedicated archive sub-folders
     * named after the <code>restartCounter</code>.
     * @param job the job that may be altered by the action. It should not be 
     * running.
	 * @param idxTriggeringStep the index of the step of the original workflow 
	 * that performed in such way as to trigger a reaction from a 
	 * monitoring/evaluation job. 
	 * @param restartCounter an unique counter used to archive data from 
	 * previous runs of the job that may be altered by the action. We use this 
	 * to create archives for existing data generated by previous runs of the
	 * jobs that may be altered.
	 * @param action the object defining what are the tasks to perform in 
	 * the job's files.
	 * @param customUserDir the custom user directory of the evaluation job that triggered 
	 * the action. This is used if the job to archive does not define its own user directory.
     */
    static void archivePreviousResults(Job job, int idxTriggeringStep, int restartCounter,
			Action action, File customUserDir)
    {
    	Logger logger = LogManager.getLogger(ActionApplier.class);
        
    	// Define the file system location where the job's files are located
		File jobsRootPath = job.getNewFile("placeholder").getParentFile();
		if (jobsRootPath==null && customUserDir!=null)
		{
			// When the job to heal does not define a custom work space we use the given one
			logger.debug("NB: the job to heal does not define its work space."
					+ " Using the one of the evaluation job that triggered the action.");
			job.setUserDir(customUserDir);
			jobsRootPath = job.getNewFile("placeholder").getParentFile();
		}
		if (jobsRootPath==null)
		{
			jobsRootPath = new File(System.getProperty("user.dir"));
		}
		if (!jobsRootPath.exists())
		{
			String msg = "null";
			if (jobsRootPath!=null)
			{
				msg = jobsRootPath.getAbsolutePath();
			}
            throw new IllegalArgumentException("Folder '" + msg
            		+ "' is expected to contain the data for job "
            		+ job.getId() + ", but is not found.");   
		}

		// Collect files that match any DELETE-type rule
		Set<File> candidateFilesToTrash = new HashSet<File>();
		for (DataArchivingRule rule : action.getJobArchivingRules())
		{
			if (rule.getType().equals(ArchivingTaskType.DELETE))
			{
				candidateFilesToTrash.addAll(FileUtils.findByGlob(jobsRootPath, rule.getPattern(), true));
			}
		}
		
		// Collect all files related to this job and that may need to be 
		// archived (i.e., moved) to avoid overwriting upon job restart.
        Set<File> filesToArchive = new HashSet<File>();
        if (job.getStdOut()!=null)
        	filesToArchive.add(job.getStdOut());
        if (job.getStdErr()!=null)
        	filesToArchive.add(job.getStdErr());
        for (String pattern : action.getFilenamePatterns(ArchivingTaskType.MOVE))
        { 
        	filesToArchive.addAll(FileUtils.findByGlob(jobsRootPath, pattern, true));
        }

		// The rename-copy files are treated like move files in the sense that the original file will
		// not be present in its original location after the action is performed.
		Map<File, String> filesToRenameCopyMap = new HashMap<File, String>();
		for (DataArchivingRule rule : action.getJobArchivingRules())
		{
			if (rule.getType().equals(ArchivingTaskType.RENAME_COPY_LAST_SEQUENTIAL))
			{
				List<File> lastMatches = new ArrayList<File>();
				int idx = -1;
				while (true)
				{
					idx++;
					String pattern = rule.getPattern().replace(
						DataArchivingRule.INDEX_PLHLD, String.valueOf(idx));
					List<File> matches = FileUtils.findByGlob(jobsRootPath, pattern, true);
					if (matches.size()>0)
					{
						lastMatches = matches;
					} else {
						if (idx>1)
						{ 
							break;
						}
					}
				}
				filesToArchive.addAll(lastMatches);
				for (File file : lastMatches)
				{
					filesToRenameCopyMap.put(file, rule.getNewName());
				}
			} else if (rule.getType().equals(ArchivingTaskType.RENAME_COPY_BASENAME_IDX))
			{
				String pattern = rule.getPattern().replace(
					DataArchivingRule.INDEX_PLHLD, 
					String.valueOf(idxTriggeringStep));
				pattern = StringUtils.evaluateEmbeddedExpressionsInString(pattern);

				String BASENAME_PLHLD = "";
				if (job.hasParameter(WIROConstants.PAROUTFILEROOT))
					BASENAME_PLHLD = job.getParameter(WIROConstants.PAROUTFILEROOT).getValueAsString();
				else {
					NamedData paramValue = Job.getParameterInContainers(job, 
						WIROConstants.PAROUTFILEROOT);
					if (paramValue != null)
					{
						BASENAME_PLHLD = paramValue.getValueAsString();
					} else {
						if (job.hasParameter(WIROConstants.PAROUTFILE))
						{
							BASENAME_PLHLD = FileUtils.getRootOfFileName(job.getParameter(
								WIROConstants.PAROUTFILE).getValueAsString());
						} else {
							BASENAME_PLHLD = job.getId();
						}
					}
				}
				pattern = pattern.replace(DataArchivingRule.BASENAME_PLHLD, BASENAME_PLHLD);

				logger.debug("Pattern for " + ArchivingTaskType.RENAME_COPY_BASENAME_IDX + " rule '" 
				   + rule.getPattern() + "' becomes '" + pattern + "' for job " + job.getId());

				List<File> matches = FileUtils.findByGlob(jobsRootPath, pattern, true);
				for (File file : matches)
				{
					filesToRenameCopyMap.put(file, rule.getNewName());
				}
			}
		}
        
        // Collect files that we want to copy instead of moving to the archive
        Set<File> filesToCopy = new HashSet<File>();
        for (String pattern : action.getFilenamePatterns(ArchivingTaskType.COPY))
        { 
        	filesToCopy.addAll(FileUtils.findByGlob(jobsRootPath, pattern, true));
        }
        
		// Warn about any collision between DELETE-type and MOVE/COPY/RENAME_COPY-type rules
        Set<File> intersectionTrashArchive = SetUtils.getIntersection(
			candidateFilesToTrash, filesToArchive);
        if (candidateFilesToTrash.removeAll(intersectionTrashArchive))
        {
        	logger.warn("WARNING: the following files will not be "
        			+ "removed as they match one or more pattern for "
        			+ "archiving. " + StringUtils.mergeListToString(
        					Arrays.asList(intersectionTrashArchive), ",", true));
        }
        Set<File> intersectionTrashCopy = SetUtils.getIntersection(
			candidateFilesToTrash, filesToCopy);
        if (candidateFilesToTrash.removeAll(intersectionTrashCopy))
        {
        	logger.warn("WARNING: the following files will not be "
        			+ "removed as they match one or more pattern for copying."
        			+ StringUtils.mergeListToString(
        					Arrays.asList(intersectionTrashCopy), ",", true));
        }
        
        // We do this here to be able to distinguish the two intersection cases 
        // above this point.
        filesToArchive.addAll(filesToCopy);
        
        //TODO: if appID is defined for comp.chem. software packages, 
        // default filenames could be inferred based on the value of j.getAppID()
        
        Set<File> nonExisting = new HashSet<File>();
        for (File file : filesToArchive)
        {
        	if (!file.exists() || !file.canRead())
        	{
        		nonExisting.add(file);
        	}
        }
        filesToArchive.removeAll(nonExisting);
        
		if (filesToArchive.size()==0 && filesToCopy.size()==0 && filesToRenameCopyMap.size()==0)
		{
			// Nothing else to do.
			return;
		}
		
    	// Create archive folder using getNewFile to respect work directory configuration
    	File archiveFolder = job.getNewFile("Job_" + job.getId() + "_" + restartCounter);
        if (!archiveFolder.mkdirs())
        {
        	//When jobs are not related to a common parent job, their IDs are
        	//not guaranteed to be unique, so we try to use the hash.
        	
        	String str = "WARNING: folder '" + archiveFolder + "' exists. "
        			+ "Trying to use folder ";
        	
        	// WARNING: we search for a usable name, but we do not expect to
        	// compete with other threads/instances doing the same.
        	int idx = 0;
        	while (true) 
        	{
        		archiveFolder = job.getNewFile("Job_" 
        			+ job.getHashCodeSnapshot() + idx + "_" + restartCounter);
        		if (archiveFolder.mkdirs())
	            {
		        	logger.debug(str + archiveFolder + "'.");
		        	break;
	            }
        		idx++;
            }
        }

		// Make renamed copies
		for (File file : filesToRenameCopyMap.keySet())
		{
			// Use getNewFile to ensure the archive file path respects work directory configuration
			File newFile = job.getNewFile(jobsRootPath.getAbsolutePath() 
					+ File.separator + filesToRenameCopyMap.get(file));
			// Avoid that the new file is moved after being overwritten
			filesToArchive.remove(newFile);
			candidateFilesToTrash.remove(newFile);
			try {
				FileUtils.copy(file, newFile);
			} catch (IOException e) {
				logger.warn("WARNING: cannot copy file '" + file 
						+ "' to '" + newFile + "'. " + e.getMessage());
			}
		}
        
        // Move/copy files into the archive 
        for (File file : filesToArchive)
        {
        	// Use getNewFile to ensure the archive file path respects work directory configuration
        	File newFile = job.getNewFile(archiveFolder.getAbsolutePath() 
        			+ File.separator + file.getName());
        	try {
				FileUtils.copy(file, newFile);
			} catch (IOException e) {
				logger.warn("WARNING: cannot copy file '" + file 
						+ "' to '" + newFile + "'. " + e.getMessage());
			}
        	if (!filesToCopy.contains(file))
        	{
        		try {
					FileUtils.delete(file);
				} catch (IOException e) {
					logger.warn("WARNING: cannot delete file '" + file 
							+ "' to '" + newFile + "'. " + e.getMessage());
				}
        	}
        }
        
        // Cleanup files to trash
        for (File file : candidateFilesToTrash)
        {
        	file.delete();
        }
    }
   
//------------------------------------------------------------------------------
    
}
