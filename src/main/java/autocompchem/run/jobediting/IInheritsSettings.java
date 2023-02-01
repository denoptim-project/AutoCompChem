package autocompchem.run.jobediting;

import autocompchem.run.Job;

/**
 * Interface for anything that wants to be able to edit jobs of any type by
 * taking information from another job.
 */
public interface IInheritsSettings 
{	
	/**
	 * Takes the value this editing task to the given job
	 * @param original the job from which we want to inherit information.
	 * @param job the job to edit.
	 */
	public void inheritSetting(Job original, Job job);
}
