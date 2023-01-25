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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.perception.Perceptron;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.perception.situation.SituationConstants;
import autocompchem.text.TextAnalyzer;
import autocompchem.worker.TaskID;
import autocompchem.worker.WorkerConstants;

/**
 * A class of {@link ACCJob}s that in meant to evaluate other jobs.
 *
 * @author Marco Foscato
 */

//TODO-gg remove is not used

public class EvaluationJob extends ACCJob 
{
	/**
	 * The job that is to be evaluated
	 */
	private Job jobToEvaluate;
	
	/**
	 * The collection of known situations, i.e., situations that a 
	 * {@link Perceptron} can identify.
	 */
	private SituationBase sitsDB;
	
	/**
	 * The collection of information channels, i.e., sources of information that
	 * allow to perceive a known situation. 
	 */
	private InfoChannelBase icDB;
	
 
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
        super();
        setParallelizable(true);
        setNumberOfThreads(1);
        params.setParameter(WorkerConstants.PARTASK, 
        		TaskID.EVALUATEJOB.toString());
        this.jobToEvaluate = jobToEvaluate;
        this.sitsDB = sitsDB;
        this.icDB = icDB;
    }
    
//------------------------------------------------------------------------------

    /**
     * 
     * @return the collection of known situations available to this job.
     */
    public SituationBase getDBSituations()
    {
    	return sitsDB;
    }
    
//------------------------------------------------------------------------------

    /**
     * 
     * @return the collection of information channels this job uses.
     */
    
    public InfoChannelBase getDBInfoChannels()
    {
    	return icDB;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set this job parameters.
     * @param params the new set of parameters
     */
    
    @Override
    public void setParameters(ParameterStorage params)
    {	
    	if (params.contains(ParameterConstants.SITUATION))
    	{
    		if (sitsDB==null)
    		{
    			sitsDB = new SituationBase();
    		}
    		
    		String multilines = params.getParameterValue(
    				ParameterConstants.SITUATION);
    		String[] parts = multilines.trim().split(
    				System.getProperty("line.separator"));
    		ArrayList<String> lines = new ArrayList<String> ();
    		for (int i=0; i<parts.length; i++)
    		{
    			lines.add(parts[i]);
    		}
    		List<List<String>> filledForm = TextAnalyzer.readKeyValue(
                    lines,
    	    		SituationConstants.SEPARATOR,
    	    		SituationConstants.COMMENTLINE,
    	    		SituationConstants.STARTMULTILINE,
    	    		SituationConstants.ENDMULTILINE);

    		Situation situation = new Situation();
    		try {
				situation.configure(filledForm);
			} catch (Exception e) {
				e.printStackTrace();
				Terminator.withMsgAndStatus("ERROR! Unable to create Situation "
						+ "from the given parameters. " + e.getMessage(), -1);
			}
    		sitsDB.addSituation(situation);
    	}
    	
    	super.setParameters(params);
    }

//------------------------------------------------------------------------------

	/**
     * Runs the given task
     */

    @Override
    public void runThisJobSubClassSpecific()
    {   
        Date date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println(" AutoCompChem is initiating the ACC task '" 
                            + TaskID.EVALUATEJOB + "'. ");
        }
        
        JobEvaluator worker = new JobEvaluator(sitsDB, icDB, jobToEvaluate);
        worker.setParameters(params);
        worker.setDataCollector(this.exposedOutput);
        worker.initialize();
        worker.performTask();

        date = new Date();
        if (getVerbosity() > 0)
        {
            System.out.println(" " + date.toString());
            System.out.println("Done with ACC job (" + TaskID.EVALUATEJOB+")");
        }
    }

//------------------------------------------------------------------------------

}
