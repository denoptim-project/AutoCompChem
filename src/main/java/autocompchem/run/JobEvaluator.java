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
import java.nio.file.Paths;
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
import autocompchem.utils.StringUtils;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.WIROConstants;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;


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
			sitsDB = new SituationBase(getNewFile(pathName));
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
			File file = getNewFile(params.getParameter(
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
    	
		if (sitsDB==null || icDB==null)
		{

			SoftwareId software = null;
			//Try to detect the kind of ouput to analyze.
			if (params.contains(WIROConstants.PARJOBOUTPUTFILE))
			{
				File jobOutFile = getNewFile(params.getParameter(
						WIROConstants.PARJOBOUTPUTFILE).getValueAsString());
				FileUtils.foundAndPermissions(jobOutFile, true, false, false);
				try {
					software = ReaderWriterFactory.detectOutputFormat(jobOutFile);
				} catch (FileNotFoundException e) {
					// Cannot happen!
				}
			} else if (params.contains(WIROConstants.SOFTWAREID)) {
				software = new SoftwareId(params.getParameter(
						WIROConstants.SOFTWAREID).getValueAsString());
			} else if (jobBeingEvaluated!=null) {
				SoftwareId temptative = jobBeingEvaluated.getAppID();
				if (!SoftwareId.UNDEFINED.equals(temptative))
				{
					software = new SoftwareId(params.getParameter(
							WIROConstants.SOFTWAREID).getValueAsString());
				}
			} else {
				Terminator.withMsgAndStatus("ERROR: cannot infer the type of "
						+ "output to analyze. External software not identified "
						+ "by '" + WIROConstants.SOFTWAREID + "' and no '" 
						+ WIROConstants.PARJOBOUTPUTFILE + "' is given. "
						+ "Cannot choose software-specific configuration. "
						+ "Please, provide one of the above parameters or "
						+ "manually specify which lists of knwon situations "
						+ "and info channels to use.", -1);
			} 
			
			if (sitsDB==null)
			{
				logger.info("Trying to use default list of known situations.");
				try {
					sitsDB = SituationBase.getDefaultSituationDB(
							software.toString());
				} catch (IOException e) {
					Terminator.withMsgAndStatus("ERROR: cannot use default "
							+ "list of known situations when using software '" 
							+ software + "'. "
							+ "Try to specify a dedicated list on known "
							+ "situations with " 
							+ ParameterConstants.SITUATION + ", "
							+ ParameterConstants.SITUATIONSDB + ", or "
							+ ParameterConstants.SITUATIONSDBROOT + ".", -1, e);
				}
			}
			if (icDB==null)
			{
				logger.info("Trying to use default configuration of info "
						+ "channels.");
				try {
					icDB = InfoChannelBase.getDefaultInfoChannelDB(
							software.toString());
				} catch (Exception e) {
					Terminator.withMsgAndStatus("ERROR: cannot use default "
							+ "configuration of info channels when using "
							+ "software '" + software + "'. "
							+ "Try to specify a dedicated configuration of "
							+ "info channels using "
							+ ParameterConstants.INFOCHANNELSDB + ", or "
							+ ParameterConstants.INFOSRCLOGFILES + ", "
							+ ParameterConstants.INFOSRCOUTPUTFILES + ", "
							+ ParameterConstants.INFOSRCJOBDETAILS + ", and "
							+ ParameterConstants.INFOSRCINPUTFILES + ".", -1, e);
				}
				tolerateMissingIC = true;
				if (params.contains(WIROConstants.PARJOBOUTPUTFILE))
				{
					File jobOutFile = getNewFile(params.getParameter(
							WIROConstants.PARJOBOUTPUTFILE).getValueAsString());
					icDB.addChannel(new FileAsSource(jobOutFile.getAbsolutePath(), 
						InfoChannelType.LOGFEED));
				}
			}
		}
		
		// Ensure consistency with the job object using this worker
		if (jobBeingEvaluated==null)
		{
			// We take the job to evaluate from the job running this worker
			if (myJob!= null && (myJob instanceof EvaluationJob)
				&& ((EvaluationJob)myJob).getJobBeingEvaluated()!=null)
			{
				jobBeingEvaluated = ((EvaluationJob)myJob).getJobBeingEvaluated();
			} else {
				logger.warn("WARNING: The job to be evaluated is defined "
						+ "neither in the parameters nor as an "
						+ "argument to the constructor of the evaluation job. "
						+ "I can only evaluate the outcome and related info "
						+ "channels.");
			}
		} else {
			// Endure consistency with job running this worker
			if (myJob!=null && (myJob instanceof EvaluationJob))
			{
				Job jobInMyJob = ((EvaluationJob)myJob).getJobBeingEvaluated();
				if (jobInMyJob!=null)
				{
					if (jobBeingEvaluated != jobInMyJob)
					{
						throw new IllegalStateException("Inconsistent definition "
								+ "of the job to evaluate");
					}
				} else {
					((EvaluationJob)myJob).setJobBeingEvaluated(jobBeingEvaluated);
				}
			}
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
	 * Returns the collection of {@link InfoChannel}s that was originally 
	 * configures for this evaluation. Note that the {@link Perceptron} needs
	 * to convert general-purpose {@link InfoChannel} (i.e., those based on
	 * matching rules) into actual information sources (e.g., the actual matches
	 * of the matching rules).
	 * @return the collection of information channels 
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
		// Prepare to perception.
		Perceptron p = new Perceptron(sitsDB, icDB, 
				Paths.get(getWorkDir().getAbsolutePath()));
		p.setTolerantMissingIC(tolerateMissingIC);
		if (!tolerateMissingIC && p.getLostICs().size()>0)
		{ 
			IllegalStateException e = new IllegalStateException(
					"These info channels were not found: " 
					+ StringUtils.mergeListToString(p.getLostICs(), ", ", true));
			logger.error("Exception while reading logs. ", e);
			exposeOutputData(new NamedData(EXCEPTION, e.toString()));
		}

		// NB: by default we think that there is only one step. If we can parse
		// the log/output and detect which step of the job failed, then we get
		// an accurate value, otherwise we assume it is only a single-step
		// job.
		if (jobBeingEvaluated != null
				&& jobBeingEvaluated.runsParallelSubjobs()
				&& jobBeingEvaluated.getNumberOfSteps()>1)
		{
			Terminator.withMsgAndStatus("Analysis of parallel batches is "
					+ "not implemented yet. Please, contact the developers "
					+ "and present your use case.", -1);
			// Must define idxFocusJob in here
		} else {
			// In here, we define what is the "focus job", i.e., the job
			// that triggers the reaction. It can be jobBeingEvaluated, or
			// one of its steps.
			try {
				analyzeLogFilesSerialJob(p);
			} catch (Exception e) {
				logger.error("Exception while reading logs. ", e);
				exposeOutputData(new NamedData(EXCEPTION, e.toString()));
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
			}
		}
	}
	
//------------------------------------------------------------------------------

	/**
	 * Deals with the parsing of data from log/output files of serial jobs.
	 * For efficiency, we also search for matches for any query that operates on
	 * such info channels. Thus, we read and collect scores from those 
	 * information channels before perception. Therefore, we need to 
	 * communicate the findings to the perceptron.
	 */
	private void analyzeLogFilesSerialJob(Perceptron p)
	{
		List<InfoChannel> logs = p.getContextSpecificICB().getChannelsOfType(
				InfoChannelType.LOGFEED);
		for (InfoChannel ic : logs)
		{
			if (!(ic instanceof FileAsSource))
			{
				logger.warn("Information channel '"
						+ ic + "' is not a log/output file I can read.");
				continue;
			}
			FileAsSource fas = (FileAsSource) ic;
			
			analyzeLogFileOfSerialJob(p, fas);
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
		
		// The info channel has been already made context-specific, so
		// the pathname does not need to account for potential change of user.dir
		File fileToParse = getNewFile(log.getPathName());
		if (fileToParse.exists())
		{
			analysisParams.setParameter(WIROConstants.PARJOBOUTPUTFILE, 
					fileToParse.getAbsolutePath());
		} else {
			if (!tolerateMissingIC)
			{
				throw new IllegalStateException("File '" + fileToParse 
						+ "' is listed as " + InfoChannelType.LOGFEED
						+ " but is not found.");
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
			//TODO: we should have a general-purpose output reader where we can
			// set step_separators from the job's Parameter_Storage
			// Now in the unit tests we trigger selection of a comp.chem reader
			// but that is not a good solution.
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
		{
			p.collectPerceptionScoresForTxtMatchers(tq, matchesByTQ.get(tq));
		}

		p.setInfoChannelAsRead(log);
	}
	
//------------------------------------------------------------------------------

}
