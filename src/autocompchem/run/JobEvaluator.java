package autocompchem.run;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.perception.Perceptron;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;


/**
 * A worker that evaluates a job. The job can be active (i.e., monitoring) 
 * or terminated (evaluation of output)
 * 
 * @author Marco Foscato
 */

public class JobEvaluator extends Worker
{
	/**
	 * Declaration of what this worker is capable of.
	 */
	public static final Set<TaskID> capabilities = 
			Collections.unmodifiableSet(new HashSet<TaskID>(
					Arrays.asList(TaskID.EVALUATEJOB)));
	
	/**
	 * The string used to identify the perceived situation when exposing output.
	 */
	public static final String SITUATIONOUTKEY = "PerceivedSituation";
	
	/**
	 * The parameter key used to provide pathname to a list of situations
	 */
	public static final String SITUATIONSDBPARAM = "SITUATIONSDBROOT";

    /**
     * Situation base: list of known situations/concepts
     */
    private SituationBase sitsDB; 

    /**
     * Information channels base: list of available information channels
     */
    private InfoChannelBase icDB;
    
//-----------------------------------------------------------------------------
	
	@Override
	public void initialize() 
	{
		
		if (params.contains(SITUATIONSDBPARAM)) 
		{
			
		}
		
		
		//TODO: read in sitsDB from params
		
		//TODO: read in icDB from params

	}
	
//-----------------------------------------------------------------------------

	@Override
	public void performTask() 
	{
		// Attempt perception
		Perceptron p = new Perceptron(sitsDB,icDB);
		try {
			p.perceive();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Expose conclusions of the evaluation
		if (p.isAware())
		{
			Situation s = p.getOccurringSituations().get(0);
			exposeOutputData(
					new NamedData(SITUATIONOUTKEY,NamedDataType.SITUATION,s));
		}

	}
	
//-----------------------------------------------------------------------------	

}
