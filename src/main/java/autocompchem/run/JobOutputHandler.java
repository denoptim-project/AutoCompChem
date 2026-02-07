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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * A worker that provides access to output data exposed by jobs executed in the
 * workflow that includes a call to this worker.
 * 
 * @author Marco Foscato
 */

public class JobOutputHandler extends Worker
{
    /**
     * String defining the task of printing the list of data exposed by a job.
     */
    public static final String LISTJOBDATATASKNAME = "listJobData";

    /**
     * Task about evaluating any job output
     */
    public static final Task LISTJOBDATATASK;
    static {
    	LISTJOBDATATASK = Task.make(LISTJOBDATATASKNAME);
    }

	/**
	 * The key of the parameter defining the pointer to the job the output of which is to be processed.
	 */
	private static final String PARJOBPOINTER = "JOBPOINTER";

	/**
	 * The pointer to the job the output of which is to be processed.
	 */
	private String pathToOtherJob;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobOutputHandler()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
		Set<Task> tmpSet = new HashSet<Task>();
		tmpSet.add(LISTJOBDATATASK);
		return Collections.unmodifiableSet(tmpSet);
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/JobOutputHandler.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new JobOutputHandler();
    }
    
//------------------------------------------------------------------------------
	
	public void initialize() 
	{   	
    	super.initialize();
    	
		if (hasParameter(PARJOBPOINTER)) 
		{
			pathToOtherJob = params.getParameter(PARJOBPOINTER).getValueAsString();
		}
	}

//------------------------------------------------------------------------------
	
	@Override
	public void performTask() 
	{
    	if (task.equals(LISTJOBDATATASK))
    	{
			printExposedDataTree(pathToOtherJob, getMyJob(), logger);
    	} else {
			dealWithTaskMismatch();
		}
	}

//------------------------------------------------------------------------------

	public static void printExposedDataTree(String pathToOtherJob, Job job, 
		Logger logger)
	{
		Job targetJob = Job.navigateToJob(job, pathToOtherJob);
		if (targetJob == null)
		{
			logger.info("Job not found");
			return;
		}
		NamedDataCollector exposedDataCollector = targetJob.getOutputCollector();
		logger.info("Exposed data from job " + targetJob.getId() + " is:");

		if (exposedDataCollector.getAllNamedData().isEmpty())
		{
			logger.info("No exposed data");
			return;
		}
		listDataIdentifiers(exposedDataCollector, "    ", logger);
	}

//------------------------------------------------------------------------------

    private static void listDataIdentifiers(NamedDataCollector dataCollector, 
		String prefix, Logger logger)
    {
		for (NamedData data : dataCollector.getAllNamedData().values())
		{
			listDataIdentifiers(data, prefix, logger);
		}
    }

//------------------------------------------------------------------------------

	private static void listDataIdentifiers(NamedData data, String prefix, 
		Logger logger)
	{
		String newPrefix = prefix + data.getReference() + ", ";
		Object value = data.getValue();
		if (value instanceof NamedData)
		{
			NamedData container = (NamedData) value;
			listDataIdentifiers(container, newPrefix, logger);
		} else if (value instanceof NamedDataCollector)
		{
			NamedDataCollector container = (NamedDataCollector) value;
			listDataIdentifiers(container, newPrefix, logger);
		} else if (value instanceof Map) 
		{
			Map<?,?> map = (Map<?, ?>) value;
			for (Object key : map.keySet()) 
			{
				Object mapValue = map.get(key);
				if (mapValue instanceof NamedData)
				{
					NamedData mapData = (NamedData) mapValue;
					listDataIdentifiers(mapData, newPrefix + key + ", ", logger);
				} else if (mapValue instanceof NamedDataCollector)
				{
					NamedDataCollector mapcontainer = (NamedDataCollector) mapValue;
					listDataIdentifiers(mapcontainer, newPrefix + key + ", ", logger);
				} else {
					logger.info(newPrefix + key + ", [" + mapValue.getClass().getName() + "]");
				}
			}
		} else if (value instanceof List) 
		{
			List<?> list = (List<?>) value;
			for (int i=0; i<list.size(); i++)
			{
				Object listValue = list.get(i);
				if (listValue instanceof NamedData)
				{
					NamedData listData = (NamedData) listValue;
					listDataIdentifiers(listData, newPrefix + i + ", ", logger);
				} else if (listValue instanceof NamedDataCollector)
				{
					NamedDataCollector listcontainer = (NamedDataCollector) listValue;
					listDataIdentifiers(listcontainer, newPrefix + i + ", ", logger);
				} else {
					logger.info(newPrefix + i + ", [" + listValue.getClass().getName() + "]");
				}
			}
		} else {
			logger.info(newPrefix + "[ " + value.getClass().getName() + "]");
		}
	}

//------------------------------------------------------------------------------

}
