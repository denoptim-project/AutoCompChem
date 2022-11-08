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

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.perception.Perceptron;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.utils.NumberUtils;
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
	 * The string used to identify the perceived situation in the exposed 
	 * job output data structure.
	 */
	public static final String SITUATIONOUTKEY = "PerceivedSituation";
	
	/**
	 * The string used to identify the reaction to the perceived situation the
	 * exposed job output data structure.
	 */
	public static final String REACTIONTOSITUATION = "ReactionToSituation";
	
    /**
     * Situation base: list of known situations/concepts
     */
    private SituationBase sitsDB; 

    /**
     * Information channels base: list of available information channels
     */
    private InfoChannelBase icDB;
    
    /**
     * The job being evaluated
     */
    private Job job;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor defining the knowledge base and source of information.
     * @param sitsDB the collection of known situations.
     * @param icDB the collection of information channels.
     */
    public JobEvaluator()
    {
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor defining the knowledge base and source of information.
     * @param sitsDB the collection of known situations.
     * @param icDB the collection of information channels.
     * @param job the job being evaluated.
     */
    public JobEvaluator(SituationBase sitsDB, InfoChannelBase icDB, Job job)
    {
        this.sitsDB = sitsDB;
        this.icDB = icDB;
        this.job = job;
    }
    
//-----------------------------------------------------------------------------
	
	@Override
	public void initialize() 
	{   	
		if (hasParameter(ParameterConstants.VERBOSITY)) 
		{
			String vStr= params.getParameter(
					ParameterConstants.VERBOSITY).getValueAsString();
			if (!NumberUtils.isNumber(vStr))
			{
				Terminator.withMsgAndStatus("ERROR! Value '" + vStr + "' "
						+ "cannot be converted to an integer. Check parameter "
						+ ParameterConstants.VERBOSITY, -1);
			}
			verbosity = Integer.parseInt(vStr);
		}		
		
		if (hasParameter(ParameterConstants.SITUATIONSDBROOT)) 
		{
			String pathName = params.getParameter(
					ParameterConstants.SITUATIONSDBROOT).getValueAsString();
			sitsDB = new SituationBase(new File(pathName));
		}
		
		if (hasParameter(ParameterConstants.INFOSRCINPUTFILES)) 
		{
			String pathNames = params.getParameter(
					ParameterConstants.INFOSRCINPUTFILES).getValueAsString();
			if (icDB == null)
			{
				icDB = new InfoChannelBase();
			}
			String[] list = pathNames.trim().split(File.pathSeparator);
			for (int i=0; i<list.length; i++)
			{
				FileUtils.foundAndPermissions(list[i], true, false, false);
				InfoChannel ic = new FileAsSource(list[i]);
				ic.setType(InfoChannelType.INPUTFILE);
				icDB.addChannel(ic);
			}
		}
		
		if (hasParameter(ParameterConstants.INFOSRCLOGFILES)) 
		{
			String pathNames = params.getParameter(
					ParameterConstants.INFOSRCLOGFILES).getValueAsString();
			if (icDB == null)
			{
				icDB = new InfoChannelBase();
			}
			String[] list = pathNames.trim().split(File.pathSeparator);
			for (int i=0; i<list.length; i++)
			{
				FileUtils.foundAndPermissions(list[i], true, false, false);
				InfoChannel ic = new FileAsSource(list[i]);
				ic.setType(InfoChannelType.LOGFEED);
				icDB.addChannel(ic);
			}
		}
		
		if (hasParameter(ParameterConstants.INFOSRCOUTPUTFILES)) 
		{
			String pathNames = params.getParameter(
					ParameterConstants.INFOSRCOUTPUTFILES).getValueAsString();
			if (icDB == null)
			{
				icDB = new InfoChannelBase();
			}
			String[] list = pathNames.trim().split(File.pathSeparator);
			for (int i=0; i<list.length; i++)
			{
				FileUtils.foundAndPermissions(list[i], true, false, false);
				InfoChannel ic = new FileAsSource(list[i]);
				ic.setType(InfoChannelType.OUTPUTFILE);
				icDB.addChannel(ic);
			}
		}
		
		if (hasParameter(ParameterConstants.INFOSRCJOBDETAILS)) 
		{
			String pathNames = params.getParameter(
					ParameterConstants.INFOSRCJOBDETAILS).getValueAsString();
			if (icDB == null)
			{
				icDB = new InfoChannelBase();
			}
			String[] list = pathNames.trim().split(File.pathSeparator);
			for (int i=0; i<list.length; i++)
			{
				FileUtils.foundAndPermissions(list[i], true, false, false);
				InfoChannel ic = new FileAsSource(list[i]);
				ic.setType(InfoChannelType.JOBDETAILS);
				icDB.addChannel(ic);
			}
		}
		
		if (hasParameter(ParameterConstants.JOBDEF)) 
		{
			String pathName = params.getParameter(
					ParameterConstants.JOBDEF).getValueAsString();
			FileUtils.foundAndPermissions(pathName, true, false, false);
			job = JobFactory.buildFromFile(pathName);
		}
    	
		String whatIsNull = "";
		/*
		// Not really a requirement
		if (job==null)
		{
			whatIsNull="the job to evaluate";
		}
		*/
		if (sitsDB==null)
		{
			if (!whatIsNull.equals(""))
			{
				whatIsNull=whatIsNull + ", and ";
			}
			whatIsNull=whatIsNull + "the collection of known situations";
		}
		if (icDB==null)
		{
			if (!whatIsNull.equals(""))
			{
				whatIsNull=whatIsNull + ", and ";
			}
			whatIsNull=whatIsNull + "the collection of information channels";
		}
		if (!whatIsNull.equals(""))
		{
			Terminator.withMsgAndStatus("ERROR! Cannot evaluate job. Missing "
					+ "data in " + whatIsNull + ".", -1);
		}
	}
	
//-----------------------------------------------------------------------------

	@Override
	public void performTask() 
	{
		// Pre-flight checks
		if (sitsDB.getSituationCount()==0)
		{
			Terminator.withMsgAndStatus("ERROR! List of known situations is "
					+ "empty! Some knowledge is needed to do perception.",-1);
		}
		if (icDB.getInfoChannelCount()==0)
		{
			Terminator.withMsgAndStatus("ERROR! List of information channels "
					+ "is empty! Information is needed to do perception.",-1);
		}
		
		//TODO Collect and prepare info
		// e.g. parse output files to extract data and build info from it

		
		// Attempt perception
		Perceptron p = new Perceptron(sitsDB,icDB);
		p.setVerbosity(verbosity-1);
		
		try {
			p.perceive();
			
			if (verbosity > 0)
			{
				//TODO use logger
				if (p.isAware())
				{
					Situation sit = p.getOccurringSituations().get(0);
					System.out.println("JobEvaluator: Situation perceived = " 
							+ sit.getRefName());
					
				} else {
					System.out.println("JobEvaluator: No known situation "
							+ "perceived.");
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Expose conclusions of the evaluation
		if (p.isAware())
		{
			Situation s = p.getOccurringSituations().get(0);
			exposeOutputData(new NamedData(SITUATIONOUTKEY,
					NamedDataType.SITUATION, s));
			
			if (s.hasReaction())
			{
				Action a = s.getReaction();
				exposeOutputData(new NamedData(REACTIONTOSITUATION,
						NamedDataType.ACTION, a));
				//TODO: alter master job with reaction triggered by outcome of analysis
			}
		}
	}
	
//-----------------------------------------------------------------------------	

}
