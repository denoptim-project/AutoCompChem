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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftOutputAnalyzer;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.perception.Perceptron;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.perception.situation.SituationConstants;
import autocompchem.text.TextAnalyzer;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;


/**
 * A worker that evaluates a job. The job can be active (i.e., monitoring) 
 * or terminated (evaluation of output)
 * 
 * @author Marco Foscato
 */

public class JobEvaluator extends Worker
{
	/**
	 * Tasks about evaluating jobs of computational chemistry software.
	 */
	public static final Set<TaskID> EVALCOMPCHEMJOBTASKS =
			Collections.unmodifiableSet(new HashSet<TaskID>(
					Arrays.asList(TaskID.EVALUATEGAUSSIANOUTPUT,
							TaskID.CUREGAUSSIANJOB
	//TODO-gg add these 
					/*
					TaskID.EVALUATENWCHEMOUTPUT,
					TaskID.EVALUATEORCAOUTPUT,
					TaskID.EVALUATEXTBOUTPUT,
					TaskID.EVALUATESPARTANOUTPUT,
					*/
							)));
	// WARNING: what you add 
	
	/**
	 * Declaration of what this worker is capable of.
	 */
	public static final Set<TaskID> capabilities;
	static {
		Set<TaskID> tmpSet = new HashSet<TaskID>(EVALCOMPCHEMJOBTASKS);
		tmpSet.add(TaskID.EVALUATEJOB);
		//TODO-gg tmpSet.add(TaskID.CUREJOB);
		capabilities = Collections.unmodifiableSet(tmpSet);
	}
	
	/**
	 * The string used to identify the kind of termination of the evaluated job.
	 */
	public static final String NORMALTERMKEY = "PerceivedNormalTermination";
	
	/**
	 * The string used to identify the value indicating how many steps where
	 * found in the evaluated job.
	 */
	public static final String NUMSTEPSKEY = "NumberOfStepsFoundInEvaluatedJob";
	
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
    private Job jobBeingEvaluated;
    
    /**
     * The index of the job step that is last, whether it is failed or not.
     * By default this value is 0.
     */
    private int lastJobStepId = 0;

    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobEvaluator()
    {
        super("inputdefinition/JobEvaluator.json");
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
    	this();
        this.sitsDB = sitsDB;
        this.icDB = icDB;
        this.jobBeingEvaluated = job;
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
		
		if (hasParameter(ParameterConstants.SITUATION)) 
		{
			String multilines = params.getParameter(
					ParameterConstants.SITUATION).getValueAsString();
			String[] parts = multilines.trim().split(
					System.getProperty("line.separator"));
			List<String> lines = Arrays.asList(parts);
			List<List<String>> filledForm = TextAnalyzer.readKeyValue(lines,
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
			if (sitsDB==null)
			{
				sitsDB = new SituationBase();
			}
			sitsDB.addSituation(situation);
		}
		
		if (hasParameter(ParameterConstants.SITUATIONSDBROOT)) 
		{
			if (sitsDB!=null)
			{
				Terminator.withMsgAndStatus("ERROR! A database of known "
						+ "situations has been provided to the job evaluation, "
						+ "but there is an attempt to overwrite this data by "
						+ "using the '" + ParameterConstants.SITUATIONSDBROOT 
						+ "' parameter. Check your input.", -1);
			}
			String pathName = params.getParameter(
					ParameterConstants.SITUATIONSDBROOT).getValueAsString();
			sitsDB = new SituationBase(new File(pathName));
		} else if (hasParameter(ParameterConstants.SITUATIONSDB)) 
		{
			sitsDB = (SituationBase) params.getParameter(
					ParameterConstants.SITUATIONSDB).getValue();
		}
		
		if (hasParameter(ParameterConstants.INFOCHANNELSDB)) 
		{
			icDB = (InfoChannelBase) params.getParameter(
					ParameterConstants.INFOCHANNELSDB).getValue();
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
		
		//NB: ParameterConstants.INFOSRCJOBDETAILS is not really equivalent to
		// ParameterConstants.JOBDEF even though they will probably have
		// the same content, but INFOSRCJOBDETAILS allows to include more
		// so we keep it separated.
		
		// Moreover, if JOBDEF==null then we know we are not supposed to perform
		// any action that changes the focus job.
		// TODO-gg keep this comment of not?
		
		if (hasParameter(ParameterConstants.JOBDEF)) 
		{
			String pathName = params.getParameter(
					ParameterConstants.JOBDEF).getValueAsString();
			FileUtils.foundAndPermissions(pathName, true, false, false);
			try {
				jobBeingEvaluated = (Job) IOtools.readJsonFile(pathName, Job.class);
			} catch (IOException e) {
				e.printStackTrace();
				Terminator.withMsgAndStatus("ERROR! could not read JSON file "
						+ "with definitiong of job to evaluate.", -1);
			}
		}
		
		if (hasParameter(ParameterConstants.JOBTOEVALUATE)) 
		{
			if (jobBeingEvaluated!=null)
			{
				Terminator.withMsgAndStatus("ERROR! A job to evaluate "
						+ "been provided to the evaluation job, "
						+ "but there is an attempt to overwrite this data by "
						+ "using the '" + ParameterConstants.JOBTOEVALUATE 
						+ "' parameter. Check your input.", -1);
			}
			jobBeingEvaluated = (Job) params.getParameter(
					ParameterConstants.JOBTOEVALUATE).getValue();
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
			whatIsNull=whatIsNull + "a collection of known situations";
		}
		if (icDB==null)
		{
			if (!whatIsNull.equals(""))
			{
				whatIsNull=whatIsNull + ", and ";
			}
			whatIsNull=whatIsNull + "a collection of information channels";
		}
		if (!whatIsNull.equals(""))
		{
			Terminator.withMsgAndStatus("ERROR! Cannot evaluate job. Missing "
					+ whatIsNull + ".", -1);
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * @return the collection of known situations that this evaluate can use.
	 */
	public SituationBase getSitsDB() 
	{
		return sitsDB;
	}

//-----------------------------------------------------------------------------

	/**
	 * @return the collection of information channels that this evaluator can 
	 * use.
	 */
	public InfoChannelBase getIcDB() {
		return icDB;
	}

//-----------------------------------------------------------------------------

	/**
	 * @return the job this evaluation is meant to evaluate.
	 */
	public Job getJobBeingEvaluated() {
		return jobBeingEvaluated;
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
		
		// Prepare to perception.
		Perceptron p = new Perceptron(sitsDB, icDB);
		p.setVerbosity(verbosity-1);
		
		if (EVALCOMPCHEMJOBTASKS.contains(task) || jobBeingEvaluated instanceof CompChemJob)
		{
			analyzeCompChemJobResults(p);
		}
		
		//TODO: need parsing of ACCJob log files to detect number of steps
		// which is otherwise assumed to be 1 (see lastJobStepId)
		
		try {
			p.perceive();
			
			if (verbosity == 1)
			{
				// Minimal log that is done by Perceptron if the verbosity is higher
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
				Action reaction = s.getReaction();
				exposeOutputData(new NamedData(REACTIONTOSITUATION,
						NamedDataType.ACTION, reaction));
				
				/*
				 TODO-gg delete
				if (jobBeingEvaluated!=null && jobBeingEvaluated.getParent()==null)
				{
					// curting actions can only operate on the job, not
					// on the mater job
					 
					this cannot work if the job has no steps!!!
					Job failingStep = jobBeingEvaluated.getStep(lastJobStepId);
							
					//TODO-gg: This should be done where?
					// create additional steps
					
					// remove previous steps
					
					// report new job details/input
				
				}
				*/
			}
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Deals with the parsing of data from log/output files of comp. chem jobs.
	 * For efficiency, we also search for matches for any query that operates on
	 * such info channels. Thus, we read and collect scores from those 
	 * information channels before perceptions. Therefore, we need to 
	 * communicate the findings to the perceptron.
	 */
	private void analyzeCompChemJobResults(Perceptron p)
	{
		ParameterStorage analysisParams = new ParameterStorage();
		if (EVALCOMPCHEMJOBTASKS.contains(task))
		{
			TaskID analysisTask = TaskID.UNSET;
			switch (task) {

			case CUREGAUSSIANJOB:
			case EVALUATEGAUSSIANOUTPUT:
				analysisTask = TaskID.ANALYSEGAUSSIANOUTPUT;
				break;
/*
			case CURENWCHEMJOB:
			case EVALUATENWCHEMOUTPUT:
				analysisTask = TaskID.ANALYSENWCHEMOUTPUT;
				break;
				
			case CUREORCAJOB:	
			case EVALUATEORCAOUTPUT:
				analysisTask = TaskID.ANALYSEORCAOUTPUT;
				break;
				
			case CUREXTBJOB:
			case EVALUATENXTBOUTPUT:
				analysisTask = TaskID.ANALYSEXTBOUTPUT;
				break;
				
			case CURESPARTANJOB:
			case EVALUATESPAARTANOUTPUT:
				analysisTask = TaskID.ANALYSESPARTANOUTPUT;
				break;
*/
			default:
				break;
			}
			analysisParams.setParameter("TASK", analysisTask);
			List<InfoChannel> logChannels = icDB.getChannelsOfType(
					InfoChannelType.LOGFEED);
			String msg = "";
			if (logChannels.size()>1)
			{
				msg = "more than one";
			} else if (logChannels.size()>1)
			{
				msg = "no";
			}
			if (!msg.equals(""))
			{
				Terminator.withMsgAndStatus("ERROR: Found "+ msg + " info "
						+ "channel for type " + InfoChannelType.LOGFEED + ". "
						+ "This type of channel is expected to contain the log"
						+ "from a comp. chem. software. "
						+ "Please, check your input.",-1);
			}

			analysisParams.setParameter(ChemSoftConstants.PARJOBOUTPUTFILE, 
					((FileAsSource)logChannels.get(0)).getPathName());
			p.setInfoChannelAsRead(logChannels.get(0));
			
			//TODO-gg add any AnalisisTask? Perhaps, depending on what the 
			// ICircumbstances want to check during perception.
		} else {
			// For the moment we do not try to detect the kind of job.
			Terminator.withMsgAndStatus("ERROR: cannot yet detect type of comp."
					+ "chem. job. Please, specify one of these tasks to your"
					+ "JobEvaluator: "
					+ StringUtils.mergeListToString(Arrays.asList(
							EVALCOMPCHEMJOBTASKS), ", ", true), -1);
		}
		
		// Prepare a worker that parses data and searches for strings that may
		// be requested by the perceptron.
		ChemSoftOutputAnalyzer outputParser = (ChemSoftOutputAnalyzer) 
				WorkerFactory.createWorker(analysisParams, this.getMyJob());
		outputParser.setSituationBaseForPerception(sitsDB);
		NamedDataCollector results = new NamedDataCollector();
		outputParser.setDataCollector(results);
		outputParser.performTask();
		
		exposeOutputData(new NamedData(NORMALTERMKEY, NamedDataType.BOOLEAN, 
				outputParser.getNormalTerminationFlag()));
		lastJobStepId = outputParser.getStepsFound();
		/*
		This is done for any job type, so outside of this method
		exposeOutputData(new NamedData(NUMSTEPSKEY, NamedDataType.INTEGER, 
				outputParser.getStepsFound()));
		*/
		
		//TODO-gg we should put any of the data exposed by the outputParser
		// into info channels, so that it can be used by perception.
		// This may need to add a data feed-like type of info channel
		
		@SuppressWarnings("unchecked")
		Map<TxtQuery,List<String>> matchesByTQ = (Map<TxtQuery,List<String>>)
				results.getNamedData(
						ChemSoftOutputAnalyzer.MATCHESTOTEXTQRYSFORPERCEPTION)
				.getValue();
		for (TxtQuery tq : matchesByTQ.keySet())
			p.collectPerceptionScoresForTxtMatchers(tq, matchesByTQ.get(tq));
	}
	
//-----------------------------------------------------------------------------	

}
