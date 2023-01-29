package autocompchem.run;

import autocompchem.chemsoftware.CompChemJob;
import autocompchem.datacollections.NamedData.NamedDataType;

/**
 * Class representing the editing of something in a {@link Job}.
 * 
 * @author Marco Foscato
 */
public class JobEditTask 
{
	public enum TargetType {PARAMETER, DIRECTIVECOMPONENT};

	/**
	 * The reference name of the feature to change.
	 */
	final String targetRef;
	
	/**
	 * The type of feature to change.
	 */
	final TargetType targetType;
	
	/**
	 * The new value to assign to the feature to change.
	 */
	final Object newValue;

//------------------------------------------------------------------------------
	
	/**
	 * Constructor for an editing task that changes a specific job feature by 
	 * assigning a given value to it.
	 * @param targetRef the reference name of the feature to change.
	 * @param targetType the type of feature to change.
	 * @param newValue the new value to assign to the feature.
	 */
	public JobEditTask(String targetRef, TargetType targetType, Object newValue)
	{
		this.targetRef = targetRef;
		this.targetType = targetType;
		this.newValue = newValue;
	}

//------------------------------------------------------------------------------

	/**
	 * Applies this editing task to the given job.
	 * @param job the job to edit.
	 */
	public void apply(Job job)
	{
		switch (targetType) {
		case DIRECTIVECOMPONENT:
			if (job instanceof CompChemJob)
			{
				CompChemJob ccj = (CompChemJob) job;
				//TODO-gg
				/*
				ccj.setDirective(null);
				*/
			}
			break;
			
		case PARAMETER:
			//TODO-gg this will change with the refactoring of setParameter(String, Object);
			job.setParameter(targetRef, NamedDataType.STRING, newValue.toString());
			break;
		}
	}
	
//------------------------------------------------------------------------------

}
