package autocompchem.run;

import java.io.File;

/*
 *   Copyright (C) 2026  Marco Foscato
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;


/**
 * A worker that loops over a job, possibly chanign some settings of the job.
 * 
 * @author Marco Foscato
 */

public class JobLooper extends Worker
{
    /**
     * String defining the task of looping over a job
     */
    public static final String LOOPJOBTASKNAME = "loopJob";

    /**
     * Task about evaluating any job output
     */
    public static final Task LOOPJOBTASK;
    static {
    	LOOPJOBTASK = Task.make(LOOPJOBTASKNAME);
    }

    /**
     * Keyword defining the job to loop over
     */
    public static final String PARLOOPEDJOB = "LOOPEDJOB";

	/**
	 * The job to loop over
	 */
	private Job loopedJob;
    
    /**
     * Keyword defining the parameter controlling the maximum number of iterations
     * of the loop.
     */
    public static final String PARMAXITERATIONS = "MAXITERATIONS";

	/**
	 * The maximum number of iterations of the loop.
	 */
	private int maxIterations = 10;

	/**
	 * The rules defining string-replacements operations typically dependent on the
	 * iteration number. These are used to alter the job definition at any iteration.
	 */
	private Map<String, String> replacementRules = new HashMap<String, String>();

	/**
	 * Keyword defining the parameter that define the string replacement rules.
	 * 
	 */
	public static final String PARREPLACEMENTRULES = "REPLACEMENTRULES";

	/**
	 * Keyword defining the parameter that defines whether to write the iteration JSON files.
	 * 
	 */
	public static final String PARWRITEITERATIONJSONFILES = "WRITEITERATIONJSONFILES";

	/**
	 * Whether to write the iteration JSON files.
	 */
	private boolean writeIterationJSONFiles = false;

    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobLooper()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
		Set<Task> tmpSet = new HashSet<Task>();
		tmpSet.add(LOOPJOBTASK);
		return Collections.unmodifiableSet(tmpSet);
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/JobLooper.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new JobLooper();
    }
    
//------------------------------------------------------------------------------
	
	public void initialize() 
	{   	
    	super.initialize();
    	
		if (hasParameter(PARLOOPEDJOB)) 
		{
			loopedJob = (Job) params.getParameter(PARLOOPEDJOB).getValue();
			if (params.contains(JobConstants.PARLOWMEMORYMODE))
			{
				loopedJob.setParameter(new NamedData(JobConstants.PARLOWMEMORYMODE, 
					params.getParameter(JobConstants.PARLOWMEMORYMODE).getValue()), true);
			}
			if (params.contains(WorkerConstants.PAROVERWRITEOUTPUT))
			{
				loopedJob.setParameter(new NamedData(WorkerConstants.PAROVERWRITEOUTPUT, 
					params.getParameter(WorkerConstants.PAROVERWRITEOUTPUT).getValue()), true);
			}
		}
    	
		if (hasParameter(PARMAXITERATIONS)) 
		{
			String value = params.getParameter(PARMAXITERATIONS).getValueAsString();
			maxIterations = NumberUtils.parseValueOrExpressionToInt(value);
		}

		if (hasParameter(PARREPLACEMENTRULES))
		{
			String value = params.getParameter(PARREPLACEMENTRULES).getValueAsString();
			String[] rules = value.split("\\r?\\n|\\r");
			for (String rule : rules)
			{
				String[] parts = rule.split("\\s+",2);
				replacementRules.put(parts[0], parts[1]);
			}
		}

		if (hasParameter(PARWRITEITERATIONJSONFILES))
		{
			String value = params.getParameter(PARWRITEITERATIONJSONFILES).getValueAsString();
			writeIterationJSONFiles = StringUtils.parseBoolean(value, true);
		}
	}

//------------------------------------------------------------------------------
	
	@Override
	public void performTask() 
	{
    	if (task.equals(LOOPJOBTASK))
    	{
			runLoopedJob(getMyJob(), loopedJob, 
				maxIterations, replacementRules, writeIterationJSONFiles, 
				"Job"+getMyJob().getId());
    	} else {
			dealWithTaskMismatch();
		}
	}

//------------------------------------------------------------------------------

	/**
	 * Configures a loop repeating a {@link Job} a given number of times using the 
	 * iteration number to alter the job definition. The iterationa are then executed
	 * by the {@link Job} class as steps of the container job.
	 * @param containerJob the container job that will contain the steps of the loop.
	 * @param jobTmpl the template job to loop over.
	 * @param maxIterations the maximum number of iterations of the loop.
	 * @param replacements a map of rules defining string-replacements operations
	 * typically dependent on the iteration number. These are used to alter the
	 * job definition at any iteration.
	 * @param writeIterationJSONFiles set to <code>true</code> to write the job
	 * @param looperJobName the name of the looper job, used to name the 
	 * iteration JSON files.
	 */
	
    public static void runLoopedJob(Job containerJob, Job jobTmpl, 
		int maxIterations, Map<String, String> replacementRules, 
		boolean writeIterationJSONFiles, String looperJobName)
	{
    	Logger logger = LogManager.getLogger(JobLooper.class);

		// Job details alteration happens in the JSON representation of the job
		String jobTemplateAsJson = ACCJson.getWriter().toJson(jobTmpl);

		for (int i=0; i<maxIterations; i++)
		{
			logger.debug("Configuring iteration " + i + " of " + maxIterations);

			// Compute the altered strings
			Map<String, String> replacements = new HashMap<String, String>();
			for (String originalString : replacementRules.keySet())
			{
				String replacementForOriginalString = replacementRules.get(originalString);
				if (replacementForOriginalString.startsWith("${"))
				{
					// Replacement string is an expression
					try {
						replacementForOriginalString = NumberUtils.calculateNewValue(
							replacementForOriginalString, Double.valueOf(i+"")).toString();
					} catch (Exception e) {
						throw new RuntimeException("Error evaluating expression '" 
							+ replacementForOriginalString + "' at iteration " + i + ". " 
							+ "Please, check your input for parameter '" 
							+ PARREPLACEMENTRULES + "'.", e);
					}
				}
				replacements.put(originalString, replacementForOriginalString);
			}

			// Alter job definition according to iteration number
			String alteredJobJson = jobTemplateAsJson;
			for (String match : replacements.keySet())
			{
				alteredJobJson = alteredJobJson.replace(match, replacements.get(match));
			}

			if (writeIterationJSONFiles)
			{
				IOtools.writeTXTAppend(new File(looperJobName + "_iteration-" + i + ".json"), alteredJobJson, false);
			}

			// Create the altered job
			Job currentStep = (Job) ACCJson.getReader().fromJson(alteredJobJson, Job.class);
			containerJob.addStep(currentStep);
		}
		logger.info("Configured " + maxIterations + " iterations of the template job.");
	}
	
//------------------------------------------------------------------------------

}
