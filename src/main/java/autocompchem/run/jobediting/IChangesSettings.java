package autocompchem.run.jobediting;

import autocompchem.run.Job;

/**
 * Interface for anything that wants to be able to edit jobs of any type.
 */
public interface IChangesSettings 
{	
	/**
	 * Applies this editing task to the given job.
	 * @param job the job to edit.
	 */
	public void applyChange(Job job);
}
