package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
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

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.worker.Task;
import autocompchem.worker.WorkerConstants;

/**
 * A class of {@link ACCJob}s that in meant to evaluate other jobs.
 *
 * @author Marco Foscato
 */

public class EvaluationJob extends ACCJob 
{
 
//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	
    public EvaluationJob() {
    	super();
        setParallelizable(true);
        setNumberOfThreads(1);
        params.setParameter(WorkerConstants.PARTASK, 
        		JobEvaluator.EVALUATEJOBTASK.casedID);
	}
    
//------------------------------------------------------------------------------

    /**
     * Constructor.
     * @param jobToEvaluate the job to be evaluated (single step isolated job, 
     * or a single step in a workflow, of a job belonging to a batch of jobs).
     * @param containerOfJobToEvaluate the workflow or batch that contains the
     * step to be evaluate. This can be the same of the 
     * <code>jobToEvaluate</code>, meaning that such job is not a part of a 
     * workflow or batch.
     * @param sitsDB the collection of {@link Situation}s that this 
     * {@link EvaluationJob} is made aware of.
     * @param icDB the collection of means give to this {@link EvaluationJob}
     * to perceive the {@link Situation}s.
     */

    public EvaluationJob(Job jobToEvaluate, Job containerOfJobToEvaluate, 
    		SituationBase sitsDB, InfoChannelBase icDB)
    {
        this();
        params.setParameter(ParameterConstants.JOBTOEVALPARENT,
        		NamedDataType.JOB, containerOfJobToEvaluate);
        params.setParameter(ParameterConstants.JOBTOEVALUATE,
        		NamedDataType.JOB, jobToEvaluate);
        params.setParameter(ParameterConstants.SITUATIONSDB, 
        		NamedDataType.SITUATIONBASE, sitsDB);
        params.setParameter(ParameterConstants.INFOCHANNELSDB, 
        		NamedDataType.INFOCHANNELBASE, icDB);
    }
    
//------------------------------------------------------------------------------

}
