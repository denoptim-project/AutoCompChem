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
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.SituationBase;
import autocompchem.worker.TaskID;
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
        		TaskID.EVALUATEJOB.toString());
	}
    
//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public EvaluationJob(Job jobToEvaluate, SituationBase sitsDB,
    		InfoChannelBase icDB)
    {
        this();
        params.setParameter(ParameterConstants.JOBTOEVALUATE,
        		NamedDataType.JOB, jobToEvaluate);
        params.setParameter(ParameterConstants.SITUATIONSDB, 
        		NamedDataType.SITUATIONBASE, sitsDB);
        params.setParameter(ParameterConstants.INFOCHANNELSDB, 
        		NamedDataType.INFOCHANNELBASE, icDB);
    }

//------------------------------------------------------------------------------

}
