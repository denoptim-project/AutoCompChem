package autocompchem.run.jobediting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

import autocompchem.files.FileUtils;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.MonitoringJob;
import autocompchem.run.Terminator;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;
import autocompchem.utils.SetUtils;
import autocompchem.utils.StringUtils;

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
  	 */
    
    public static void performActionOnSerialWorkflow(Action action, 
    		  Job workflow, int triggeringStepId, int restartCounter)
    {
    	ActionType aType = action.getType();
    	// No need to archive or edit the workflow if we just stop it. 
    	if (aType==ActionType.STOP)
    		return;
    	
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
    	
    	// Archive previous results found on disk for the steps to re-run.
    	// NB: here we assume that all files in the work directory relate
    	// to a single workflow, i.e., no other independent workflow is using 
    	// the same work directory. 
    	// Also, we assume any of the jobs to re-do, is not running.
    	// So we archive any of the files that, according to the patterns,
    	// should be those somehow connected with the jobs to re run.
    	archivePreviousResults(workflow.getSteps(), restartCounter, 
    			action.getFilenamePatterns(ArchivingTaskType.COPY), 
    			action.getFilenamePatterns(ArchivingTaskType.MOVE), 
    			action.getFilenamePatterns(ArchivingTaskType.DELETE));
    	
    	// Pre-pend (i.e., added in front) any step that should be pre-pended
    	for (int i=(action.prerefinementSteps.size()-1); i>-1; i--)
    	{
    		Job preliminaryStep = action.prerefinementSteps.get(i);
    		for (IJobSettingsInheritTask jsi : action.inheritedSettings)
    		{
    			try {
					jsi.inheritSettings(actionTiggeringStep, preliminaryStep);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					Terminator.withMsgAndStatus("ERROR: trying to clone data "
							+ "that cannot be cloned. If you see the need to "
							+ "do such operation, please, contact the "
							+ "developers and present your use case.", -1);
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
    	
    	// Archive previous results found on disk for the steps to rerun.
    	// NB: here we assume that all files in the work directory relate
    	// to a single master job, i.e., no other independent job is using 
    	// the same work directory. 
    	// Also, we assume any of the jobs to rerun, is not running.
    	// So we archive any of the files that, according to the patterns,
    	// should be those somehow connected with the jobs to rerun.
    	archivePreviousResults(stepsToReRun, restartCounter, 
    			action.getFilenamePatterns(ArchivingTaskType.COPY), 
    			action.getFilenamePatterns(ArchivingTaskType.MOVE), 
    			action.getFilenamePatterns(ArchivingTaskType.DELETE));
    	
    	// Pre-pend (i.e., added in front) is not possible in parallel batches
    	if (action.prerefinementSteps != null && 
    			action.prerefinementSteps.size()>0)
    	{
			Terminator.withMsgAndStatus("ERROR: trying to pre-pend steps in a"
					+ "parallel batch of jobs. This is not possible in this"
					+ "implementation. If you see the need to pre-pend steps"
					+ "itno a parallel batch, please, contact the "
					+ "developers and present your use case.", -1);
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
	 * @param fileNamePatternToCopy pattern that identifies files that, 
	 * if present, should be copied instead of moved to the archive as they are 
	 * needed to restart the jobs.The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 * @param fileNamePatternToArchive pattern identifying files to move into 
	 * the archive. The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 * @param fileNamePatternToTrash pattern identifying files to remove without
	 * keeping a copy. The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the last component of 
	 * the pathname.
     */
    static void archivePreviousResults(List<Job> jobs, int restartCounter,
    		Set<String> fileNamePatternToCopy, 
    		Set<String> fileNamePatternToArchive,
    		Set<String> fileNamePatternToTrash)
    {
		for (Job job : jobs)
		{
			if (job instanceof MonitoringJob)
				continue;
			
			archivePreviousResults(job, restartCounter, fileNamePatternToCopy,
					fileNamePatternToArchive, fileNamePatternToTrash);
		}
    }
 
//------------------------------------------------------------------------------
    
    /**
     * Moves files related to the given job into dedicated archive sub-folders
     * named after the <code>restartCounter</code>.
     * @param job the job that may be altered by the action. It should not be 
     * running.
	 * @param restartCounter an unique counter used to archive data from 
	 * previous runs of the job that may be altered by the action. We use this 
	 * to create archives for existing data generated by previous runs of the
	 * jobs that may be altered.
	 * @param fileNamePatternToCopy pattern that identifies files that, 
	 * if present, should be copied instead of moved to the archive as they are 
	 * needed to restart the jobs.The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 * @param fileNamePatternToArchive pattern identifying files to move into 
	 * the archive. The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 * @param fileNamePatternToTrash pattern identifying files to remove without
	 * keeping a copy. The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the last component of 
	 * the pathname.
     */
    static void archivePreviousResults(Job job, int restartCounter,
    		Set<String> fileNamePatternToCopy, 
    		Set<String> fileNamePatternToArchive,
    		Set<String> fileNamePatternToTrash)
    {
    	Logger logger = LogManager.getLogger(ActionApplier.class);
        
    	// Define the file system location where the job's files are located
		String path = ".";
		if (job.getUserDir()!=null)
			path = job.getUserDir().getAbsolutePath();
		File jobsRootPath = new File(path);
		if (!jobsRootPath.exists())
		{
            Terminator.withMsgAndStatus("ERROR! Folder '" + path
            		+ "' is expected to contain the data for job "
            		+ job.getId() + ", but is not found.", -1);   
		}
		
		// Collect all files related to this job and that may need to be 
		// archived (i.e., moved) to avoid overwriting upon job restart.
        Set<File> filesToArchive = new HashSet<File>();
        if (job.getStdOut()!=null)
        	filesToArchive.add(job.getStdOut());
        if (job.getStdErr()!=null)
        	filesToArchive.add(job.getStdErr());
        for (String pattern : fileNamePatternToArchive)
        { 
        	filesToArchive.addAll(FileUtils.findByGlob(jobsRootPath, pattern, true));
        }
        
        // Collect files that we want to copy instead of moving to the archive
        Set<File> filesToCopy = new HashSet<File>();
        for (String pattern : fileNamePatternToCopy)
        { 
        	filesToCopy.addAll(FileUtils.findByGlob(jobsRootPath, pattern, true));
        }
        
        // Collect files that we want to trash
        Set<File> filesToTrash = new HashSet<File>();
        for (String pattern : fileNamePatternToTrash)
        { 
        	filesToTrash.addAll(FileUtils.findByGlob(jobsRootPath, pattern, true));
        }
        Set<File> intersectionTrashArchive = SetUtils.getIntersection(
        		filesToTrash, filesToArchive);
        if (filesToTrash.removeAll(intersectionTrashArchive))
        {
        	logger.warn("WARNING: the following files will not be "
        			+ "removed as they match one or more pattern for "
        			+ "archiving. " + StringUtils.mergeListToString(
        					Arrays.asList(intersectionTrashArchive), ",", true));
        }
        Set<File> intersectionTrashCopy = SetUtils.getIntersection(
        		filesToTrash, filesToCopy);
        if (filesToTrash.removeAll(intersectionTrashCopy))
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
        
		if (filesToArchive.size()==0)
		{
			// Nothing else to do.
			return;
		}
		
    	File archiveFolder = new File(path + File.separator 
    			+ "Job_" + job.getId() + "_" + restartCounter);
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
        		archiveFolder = new File(path + File.separator + "Job_" 
        			+ job.getHashCodeSnapshot() + idx + "_" + restartCounter);
        		if (archiveFolder.mkdirs())
	            {
		        	logger.debug(str + archiveFolder + "'.");
		        	break;
	            }
        		idx++;
            }
        }
        String pathToArchive = archiveFolder.getAbsolutePath() 
        		+ File.separator;
        
        // Move/copy files into the archive 
        for (File file : filesToArchive)
        {
        	File newFile = new File(pathToArchive + file.getName());
        	try {
				Files.copy(file, newFile);
			} catch (IOException e) {
				logger.warn("WARNING: cannot copy file '" + file 
						+ "' to '" + newFile + "'. " + e.getMessage());
			}
        	if (!filesToCopy.contains(file))
        		file.delete();
        }
        
        // Cleanup files to trash
        for (File file : filesToTrash)
        {
        	file.delete();
        }
    }
   
//------------------------------------------------------------------------------
    
}
