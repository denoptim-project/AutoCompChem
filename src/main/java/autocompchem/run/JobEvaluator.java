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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.datacollections.NamedData;
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
import autocompchem.run.jobediting.Action;
import autocompchem.run.jobediting.ActionApplier;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.WIROConstants;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
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
     * String defining the task of evaluating any job output
     */
    public static final String EVALUATEJOBTASKNAME = "evaluateJob";

    /**
     * Task about evaluating any job output
     */
    public static final Task EVALUATEJOBTASK;
    static {
    	EVALUATEJOBTASK = Task.make(EVALUATEJOBTASKNAME);
    }
    
    /**
     * String defining the task of healing/fixing any job
     */
    public static final String CUREJOBTASKNAME = "cureJob";

    /**
     * Task about healing/fixing any job output
     */
    public static final Task CUREJOBTASK;
    static {
    	CUREJOBTASK = Task.make(CUREJOBTASKNAME);
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
	 * The string used to identify the reaction to the perceived situation in 
	 * the exposed job output data structure.
	 */
	public static final String REACTIONTOSITUATION = "ReactionToSituation";
	
	/**
	 * The string used to identify the exception triggered by perception.
	 */
	public static final String EXCEPTION = "exception";
	
	/**
	 * The string used to define a parameter that makes this evaluator run in
	 * standalone fashion. This is used only for tests.
	 */
	protected static final String RUNSTANDALONE = "RUNSTANDALONE";
	
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
     * Flags indicating we tolerate missing information channels.
     */
    private boolean tolerateMissingIC = false;

    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public JobEvaluator()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
		Set<Task> tmpSet = new HashSet<Task>();
		tmpSet.add(EVALUATEJOBTASK);
		tmpSet.add(CUREJOBTASK);
		return Collections.unmodifiableSet(tmpSet);
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/JobEvaluator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new JobEvaluator();
    }
    
//------------------------------------------------------------------------------
	
	public void initialize() 
	{   	
    	super.initialize();
		
		if (hasParameter(ParameterConstants.TOLERATEMISSINGIC))
		{
			tolerateMissingIC = Boolean.valueOf(params.getParameter(
					ParameterConstants.TOLERATEMISSINGIC).getValueAsString());
		}
		
		if (hasParameter(ParameterConstants.SITUATION)) 
		{
			String multilines = params.getParameter(
					ParameterConstants.SITUATION).getValueAsString();
			Situation situation = null;
			try {
				situation = Situation.fromJSON(multilines);
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
				if (!tolerateMissingIC)
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
				if (!tolerateMissingIC)
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
				if (!tolerateMissingIC)
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
				if (!tolerateMissingIC)
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
		
		if (hasParameter(ParameterConstants.JOBDEF)) 
		{
			File file = new File(params.getParameter(
					ParameterConstants.JOBDEF).getValueAsString());
			FileUtils.foundAndPermissions(file, true, false, false);
			try {
				jobBeingEvaluated = (Job) IOtools.readJsonFile(file, Job.class);
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
				Terminator.withMsgAndStatus("ERROR! A job to evaluate has "
						+ "been provided to the evaluation job, "
						+ "but there is an attempt to overwrite this data by "
						+ "using the '" + ParameterConstants.JOBTOEVALUATE 
						+ "' parameter. Check your input.", -1);
			}
			jobBeingEvaluated = (Job) params.getParameter(
					ParameterConstants.JOBTOEVALUATE).getValue();
		}
    	
		String whatIsNull = "";
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
	
//------------------------------------------------------------------------------

	/**
	 * @return the collection of known situations that this evaluate can use.
	 */
	public SituationBase getSitsDB() 
	{
		return sitsDB;
	}

//------------------------------------------------------------------------------

	/**
	 * @return the collection of information channels that this evaluator can 
	 * use.
	 */
	public InfoChannelBase getIcDB() {
		return icDB;
	}

//------------------------------------------------------------------------------

	/**
	 * @return the job this evaluation is meant to evaluate.
	 */
	public Job getJobBeingEvaluated() {
		return jobBeingEvaluated;
	}

//------------------------------------------------------------------------------
	
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
		
		// Detect if this is a standalone cure job.
		boolean standaloneCureJob = myJob.getObserver()==null 
				&& (CUREJOBTASK.equals(task) || hasParameter(RUNSTANDALONE));
		
		// Prepare to perception.
		Perceptron p = new Perceptron(sitsDB, icDB);
		p.setTolerantMissingIC(tolerateMissingIC);

		// NB: by default we think that there is only one step. If we can parse
		// the log/output and detect which step of the job failed, then we get
		// an accurate value, otherwise we assume it is only a single-step
		// job.
		int idxFocusJob = 0;
		if (jobBeingEvaluated != null
				&& jobBeingEvaluated.runsParallelSubjobs()
				&& jobBeingEvaluated.getNumberOfSteps()>1)
		{
			Terminator.withMsgAndStatus("Analysis of paralle batches is "
					+ "not implemented yet. Please, contact the developers "
					+ "and present your use case.", -1);
			// Must define idxFocusJob in here
		} else {
			// In here we define what is the "focus job", i.e., the job
			// that triggers the reaction. It can be jobBeingEvaluated, or
			// one of its steps.
			analyzeLogFileaSerialJob(p);
			if (exposedOutputCollector.contains(NUMSTEPSKEY))
			{
				idxFocusJob = ((int) exposedOutputCollector.getNamedData(
						NUMSTEPSKEY).getValue()) - 1;
			}
		}
		
		try {
			p.perceive();
			// Minimal log is done by Perceptron according to verbosity
			if (p.isAware())
			{
				Situation sit = p.getOccurringSituations().get(0);
				logger.info("JobEvaluator: Situation perceived = " 
						+ sit.getRefName());
			} else {
				logger.info("JobEvaluator: No known situation perceived.");
				if (standaloneCureJob)
				{
					Terminator.withMsgAndStatus("Standalone job evaluation is "
							+ "expected to detect a known error/situation, but "
							+ "none was percieved.", -1);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while doing perception. ", e);
			exposeOutputData(new NamedData(EXCEPTION, e.toString()));
		}
		
		// Process and expose conclusions of the evaluation
		if (p.isAware())
		{
			Situation s = p.getOccurringSituations().get(0);
			exposeOutputData(new NamedData(SITUATIONOUTKEY, s));
			
			if (s.hasReaction())
			{
				Action reaction = s.getReaction();
				
				// NB: this triggers notification of a request of action on the
				// observer (if any observer is present)
				((EvaluationJob) myJob).setRequestedAction(reaction);
				exposeOutputData(new NamedData(REACTIONTOSITUATION, reaction));
				
				if (jobBeingEvaluated!=null)
				{
					// In case this is a stand-alone CURE-type job, we do the 
					// action triggered by the jobBeingEvaluated here,
					// but this is has limited capability: 
					// it cannot restart the job,
					// but it can prepare a new input.
					if (standaloneCureJob)
					{
						healJob(jobBeingEvaluated, reaction, idxFocusJob);
					}
				}
			}
		}
	}
	
//------------------------------------------------------------------------------
	
	private void healJob(Job jobToHeal, Action cure, int idxFocusJob) 
	{
		logger.info("Attempting to cure job. Reaction: " 
				+ cure.getType() + " " 
				+ cure.getObject());
	
		Job jobResultingFromAction = null;
		if (jobToHeal.hasContainer())
		{
			if (jobToHeal.getContainer().runsParallelSubjobs())
			{
				// We have evaluated a job that is a part of a batch
				// so, any action must be compatible with the lack
				// of a linear workflow.
				List<Job> newJobSteps = 
						ActionApplier.performActionOnParallelBatch(
								cure,   //action to perform
								jobToHeal.getContainer(), //parallel batch
								jobToHeal, //job causing the reaction
								(EvaluationJob) myJob, //job doing the evaluation 
								1); // restart counter
				jobToHeal.getContainer().steps = newJobSteps;
			} else {
				int idxStepEvaluated = jobToHeal
						.getContainer().getSteps().indexOf(
								jobToHeal);
				ActionApplier.performActionOnSerialWorkflow(
						cure,   //action to perform
						jobToHeal.getContainer(), //serial workflow
						idxStepEvaluated, //id of step triggering reaction
						1); // restart counter
			}
			jobResultingFromAction = jobToHeal.getContainer();
		} else {
			if (jobToHeal.getNumberOfSteps() > 0)
			{
				// jobToHeal is a workflow or a batch:
				if (jobToHeal.runsParallelSubjobs())
				{
					// jobToHeal is a batch
					List<Job> newJobSteps = 
							ActionApplier.performActionOnParallelBatch(
									cure,   //action to perform
									jobToHeal, //parallel batch
									jobToHeal.getStep(idxFocusJob), //job causing the reaction
									(EvaluationJob) myJob, //job doing the evaluation 
									1); // restart counter
					jobToHeal.steps = newJobSteps;
				} else {
					// jobToHeal is a workflow
					ActionApplier.performActionOnSerialWorkflow(
							cure,   //action to perform
							jobToHeal, //serial workflow
							idxFocusJob, //id of step triggering reaction
							1); // restart counter
				}
				jobResultingFromAction = jobToHeal;
			} else {
				// jobToHeal is a single, self-contained job.
				// We can add any preliminary step only by embedding
				// it into a workflow.
				Job embeddingWorkflow = JobFactory.createTypedJob(jobToHeal);
				embeddingWorkflow.addStep(jobToHeal);
	
				ActionApplier.performActionOnSerialWorkflow(
						cure,   //action to perform
						embeddingWorkflow, //serial workflow
						0, //id of step triggering reaction
						1); // restart counter
				jobResultingFromAction = embeddingWorkflow;
			}
		}
		
		// Prepare generation of new input file
		ParameterStorage makeInputPars = new ParameterStorage();
		
		makeInputPars.setParameter(WorkerConstants.PARTASK, 
				Task.make("prepareInput").casedID);
		if (exposedOutputCollector.contains(
				WIROConstants.SOFTWAREID))
		{
			makeInputPars.setParameter(WIROConstants.SOFTWAREID, 
					exposedOutputCollector.getNamedData(
							WIROConstants.SOFTWAREID)
					.getValueAsString());
		} else {
			makeInputPars.setParameter(WIROConstants.SOFTWAREID,
					"ACC");
		}
		makeInputPars.setParameter(WIROConstants.PARJOBDETAILSOBJ, 
				jobResultingFromAction);
		
		//TODO-gg this was the wrong way to do this. We need to make
		// the action control whether or not to update the geometry 
		// and which geometry to use as the new one (last, initial, 
		// before oscillation, lowest energy) it 
		/*
		if (jobToHeal instanceof CompChemJob)
		{
			// Get geometry/ies for restart
			List<IAtomContainer> iacs = ActionApplier.getRestartGeoms(
					s.getReaction(), myJob);
			makeInputPars.setParameter(ChemSoftConstants.PARGEOM, 
					NamedDataType.UNDEFINED, iacs);
		}
		*/
		if (hasParameter(WIROConstants.PAROUTFILE))
		{
			makeInputPars.setParameter(WIROConstants.PAROUTFILE,
				params.getParameter(WIROConstants.PAROUTFILE)
					.getValueAsString());
		}
		
		Worker worker;
		try {
			worker = (Worker) 
					WorkerFactory.createWorker(makeInputPars, myJob);
		} catch (ClassNotFoundException e) {
			throw new Error("Unable to make worker for " + task);
		}
		worker.performTask();
	}
	
//------------------------------------------------------------------------------

	/**
	 * Deals with the parsing of data from log/output files of serial jobs.
	 * For efficiency, we also search for matches for any query that operates on
	 * such info channels. Thus, we read and collect scores from those 
	 * information channels before perception. Therefore, we need to 
	 * communicate the findings to the perceptron.
	 */
	private void analyzeLogFileaSerialJob(Perceptron p)
	{
		List<InfoChannel> logChannels = icDB.getChannelsOfType(
				InfoChannelType.LOGFEED);
		for (InfoChannel log : logChannels)
		{
			if (!(log instanceof FileAsSource))
			{
				Terminator.withMsgAndStatus("ERROR! Information channel '"
						+ log + "' is not a log/output file I can read.",-1);
			}
			analyzeLogFileOfSerialJob(p, (FileAsSource) log);
		}
	}
	
//------------------------------------------------------------------------------

	/**
	 * Deals with the parsing of one single log/output feed. The parsing of
	 * data is done according to the type of log that is detected within this
	 * method.
	 */
	private void analyzeLogFileOfSerialJob(Perceptron p, FileAsSource log)
	{
		ParameterStorage analysisParams = new ParameterStorage();
		analysisParams.setParameter(WorkerConstants.PARTASK, Task.make(
				"analyseOutput").casedID);
		
		// Define the pathname to the file to parse;
		File fileToParse = new File(log.getPathName());
		if (fileToParse.exists())
		{
			analysisParams.setParameter(WIROConstants.PARJOBOUTPUTFILE, 
					fileToParse.getAbsolutePath());
		} else {
			if (!tolerateMissingIC)
			{
				Terminator.withMsgAndStatus("ERROR: File '" + fileToParse 
						+ "' is listed as " + InfoChannelType.LOGFEED
						+ " but is not found.", -1);
			}
			return;
		}
		
		//TODO: add analysis tasks according to the requirements of the 
		// situations that can be perceived. E.g., extraction of geometries.
		
		// Get appropriate parser to use
		ReaderWriterFactory builder = ReaderWriterFactory.getInstance();
		OutputReader outputParser;
		try {
			outputParser = builder.makeOutputReaderInstance(fileToParse);
		} catch (FileNotFoundException e) {
			throw new Error("File '" + fileToParse + "' not found.");
		}
		if (outputParser==null)
		{
			logger.warn("WARNING: "
					+ "No suitable parser found for log/output file '"
					+ fileToParse + "'.");
			return;
		}
		outputParser.setParameters(analysisParams);
		outputParser.initialize();
		outputParser.setSituationBaseForPerception(sitsDB);
		NamedDataCollector exposedByAnalzer = new NamedDataCollector();
		outputParser.setDataCollector(exposedByAnalzer);
		outputParser.performTask();
		
		// Expose information from the analysis as part of the results of the
		// task done by this worker. Some of the info is already embedded in the
		// JOBOUTPUTDATA, but it is convenient to expose 
		// it even further to simplify access to some pivotal bit of 
		// information.
		exposeOutputData(new NamedData(NORMALTERMKEY,
				outputParser.getNormalTerminationFlag()));
		exposeOutputData(exposedByAnalzer.getNamedData(
				WIROConstants.JOBOUTPUTDATA));
		exposeOutputData(exposedByAnalzer.getNamedData(
				WIROConstants.SOFTWAREID));
		exposeOutputData(new NamedData(NUMSTEPSKEY,
				outputParser.getStepsFound()));
		
		if (jobBeingEvaluated!=null 
				&& jobBeingEvaluated.hasParameter(ChemSoftConstants.PARGEOM))
		{
			exposeOutputData(jobBeingEvaluated.getParameter(
					ChemSoftConstants.PARGEOM));
		}
		
		//TODO we should put any of the data exposed by the outputParser
		// into info channels, so that it can be used by perception.
		// This may need to add a data feed-like type of info channel
		
		@SuppressWarnings("unchecked")
		Map<TxtQuery,List<String>> matchesByTQ = (Map<TxtQuery,List<String>>)
				exposedByAnalzer.getNamedData(
						ChemSoftOutputReader.MATCHESTOTEXTQRYSFORPERCEPTION)
				.getValue();
		for (TxtQuery tq : matchesByTQ.keySet())
			p.collectPerceptionScoresForTxtMatchers(tq, matchesByTQ.get(tq));
		

		p.setInfoChannelAsRead(log);
	}
	
//------------------------------------------------------------------------------

}
