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
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.ChemSoftOutputReader;
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
import autocompchem.run.jobediting.ActionApplier;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
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
	//TODO-gg use only the general tasks
    /**
     * String defining the task of evaluation any job output
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
	 * Tasks about evaluating jobs of computational chemistry software.
	 */
	public static final Set<Task> EVALCOMPCHEMJOBTASKS =
			Collections.unmodifiableSet(new HashSet<Task>(
					Arrays.asList(Task.make("evaluateGaussianOutput"),
							Task.make("evaluateNWChemOutput")
	//TODO-gg add these, after checking them
					/*
					Task.make("EVALUATENWCHEMOUTPUT,
					Task.make("EVALUATEORCAOUTPUT,
					Task.make("EVALUATEXTBOUTPUT,
					Task.make("EVALUATESPARTANOUTPUT,
					*/
							)));
	/**
	 * Tasks about evaluating jobs of computational chemistry software.
	 */
	public static final Set<Task> CURECOMPCHEMJOBTASKS =
			Collections.unmodifiableSet(new HashSet<Task>(
					Arrays.asList(Task.make("cureGaussianJob"),
							Task.make("cureNWChemJob"))));
	
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
	 * The string used to identify the details of the job being evaluated in the
	 * exposed job output data structure.
	 */
	public static final String EVALUATEDJOB = "evaluatedJob";
	
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
     * The index of the job step that is last, whether it is failed or not.
     * By default this value is 0.
     */
    private int lastJobStepId = 0;
   
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
		Set<Task> tmpSet = new HashSet<Task>(EVALCOMPCHEMJOBTASKS);
		tmpSet.addAll(CURECOMPCHEMJOBTASKS);
		tmpSet.add(Task.make("evaluateJob"));
		//TODO-gg tmpSet.add(Task.make("cureJob"));
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
		
		// Moreover, if JOBDEF==null then we know we are not supposed to perform
		// any action that changes the focus job.
		// TODO-gg keep this comment of not?
		
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
		
		// Detect if this is a standalone cure job.
		boolean standaloneCureJob = myJob.getObserver()==null 
				&& CURECOMPCHEMJOBTASKS.contains(task);
		
		// Prepare to perception.
		Perceptron p = new Perceptron(sitsDB, icDB);
		p.setVerbosity(verbosity);
		p.setTolerantMissingIC(tolerateMissingIC);
		
		if (EVALCOMPCHEMJOBTASKS.contains(task) 
				|| CURECOMPCHEMJOBTASKS.contains(task)
				|| jobBeingEvaluated instanceof CompChemJob)
		{
			analyzeCompChemJobResults(p);
		}
		
		//TODO: need parsing of ACCJob log files to detect number of steps
		// which is otherwise assumed to be 1 (see lastJobStepId)
		
		try {
			p.perceive();
			// Minimal log that is done by Perceptron if the verbosity is higher
			if (p.isAware())
			{
				Situation sit = p.getOccurringSituations().get(0);
				System.out.println("JobEvaluator: Situation perceived = " 
						+ sit.getRefName());
			} else {
				if (verbosity > 0)
				{
					// TODO log
					System.out.println("JobEvaluator: No known situation "
							+ "perceived.");
				}
				if (standaloneCureJob)
				{
					Terminator.withMsgAndStatus("Standalone job evaluation is "
							+ "expected to detect a known error/situation, but "
							+ "none was percieved.", -1);
				}
			}
		} catch (Exception e) {
			// TODO to log
			e.printStackTrace();
			exposeOutputData(new NamedData(EXCEPTION, e.toString()));
		}
		
		exposeOutputData(new NamedData(NUMSTEPSKEY,
				NamedDataType.INTEGER, lastJobStepId));
		
		// Expose conclusions of the evaluation
		if (p.isAware())
		{
			Situation s = p.getOccurringSituations().get(0);
			exposeOutputData(new NamedData(SITUATIONOUTKEY,
					NamedDataType.SITUATION, s));
			
			if (s.hasReaction())
			{
				// NB: this triggers notification of a request of action on the
				// observer (if any observer is present)
				exposeOutputData(new NamedData(REACTIONTOSITUATION,
						NamedDataType.ACTION, s.getReaction()));
				// ...and these are used when performing the action
				exposeOutputData(new NamedData(EVALUATEDJOB,
						NamedDataType.JOB, jobBeingEvaluated));
				
				// In case this is a stand-alone CURE-type job, we do the 
				// action triggered by the jobBeingEvaluated here,
				// but this is has limited capability: it cannot restart the job,
				// but it can prepare a new input.
				if (standaloneCureJob)
				{

					if (verbosity > 0)
					{
						// TODO log
						System.out.println("Attempting to cure job. Reaction: " 
								+ s.getReaction().getType() + " " 
								+ s.getReaction().getObject());
					}
					ActionApplier.performAction(s.getReaction(), myJob, 
							Arrays.asList(jobBeingEvaluated), 1);
					
					// Prepare generation of new input file
					ParameterStorage makeInputPars = new ParameterStorage();
					
					makeInputPars.setParameter(WorkerConstants.PARTASK, 
							Task.make("prepareInput").casedID);
					makeInputPars.setParameter(ChemSoftConstants.SOFTWAREID, 
							exposedOutputCollector.getNamedData(
									ChemSoftConstants.SOFTWAREID)
							.getValueAsString());
					makeInputPars.setParameter(
							ChemSoftConstants.PARJOBDETAILSOBJ, 
							NamedDataType.JOB, jobBeingEvaluated);
					
					//TODO-gg this was the wrong way to do this. We need to make
					// the action control whether or not to update the geometry 
					// and which geometry to use as the new one (last, initial, 
					// before oscillation, lowest energy) it 
					/*
					if (jobBeingEvaluated instanceof CompChemJob)
					{
						// Get geometry/ies for restart
						List<IAtomContainer> iacs = ActionApplier.getRestartGeoms(
								s.getReaction(), myJob);
						makeInputPars.setParameter(ChemSoftConstants.PARGEOM, 
								NamedDataType.UNDEFINED, iacs);
					}
					*/
					if (hasParameter(ChemSoftConstants.PAROUTFILE))
					{
						makeInputPars.setParameter(ChemSoftConstants.PAROUTFILE,
							params.getParameter(ChemSoftConstants.PAROUTFILE)
								.getValueAsString());
					}
					
					/*
					 * TODO-gg: do like you have done for the output analyzer:
					 * - use the PREPAREINPUT task that is agnostic
					 * - that will call the AspecificOutputAnalyzer via WorkerFactory.createInstance()
					 * - AspecificOutputAnalyzer.makeInstance/jobToDoByInstance)
					 * - ChemSoftOutputReaderBuilder.makeInstance(outputFile) makes 
					 *   the instance based on the type detected in the output
					 *  - The workerFactory initialises the worked
					 *  - the performTask of the Worker (a concrete impl) 
					 *  does the actual analysis of the output.
					 */
					
					
					ChemSoftInputWriter worker;
					try {
						worker = (ChemSoftInputWriter) 
								WorkerFactory.createWorker(makeInputPars, myJob);
					} catch (ClassNotFoundException e) {
						throw new Error("Unable to make worker for " + task);
					}
					worker.performTask();
				}
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
		if (EVALCOMPCHEMJOBTASKS.contains(task)
				|| CURECOMPCHEMJOBTASKS.contains(task))
		{
			analysisParams.setParameter(WorkerConstants.PARTASK, 
					Task.make("analyseOutput").casedID);
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

			String pathname = ((FileAsSource)logChannels.get(0)).getPathName();
			File fileToParse = new File(pathname);
			if (fileToParse.exists())
			{
				analysisParams.setParameter(ChemSoftConstants.PARJOBOUTPUTFILE, 
						pathname);
				
			} else {
				if (!tolerateMissingIC)
				{
					Terminator.withMsgAndStatus("ERROR: File '" + pathname 
							+ "' is listed as " + InfoChannelType.LOGFEED
							+ " but is not found.", -1);
				}
			}
			p.setInfoChannelAsRead(logChannels.get(0));
			
			/*
			List<AnalysisTask> tasks = new ArrayList<AnalysisTask>();
			tasks.add(new AnalysisTask(AnalysisKind....));
			analysisParams.setParameter(ChemSoftConstants.PARANALYSISTASKS,
					tasks);
			//TODO-gg add any AnalisisTask? Perhaps, depending on what the 
			// ICircumbstances want to check during perception.
			*/
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
		ChemSoftOutputReader outputParser;
		try {
			outputParser = (ChemSoftOutputReader) 
					WorkerFactory.createWorker(analysisParams, this.getMyJob());
		} catch (ClassNotFoundException e) {
			throw new Error("Unable to make worker for " 
					+ analysisParams.getParameterValue(WorkerConstants.PARTASK));
		}
		outputParser.setSituationBaseForPerception(sitsDB);
		NamedDataCollector exposedByAnalzer = new NamedDataCollector();
		outputParser.setDataCollector(exposedByAnalzer);
		outputParser.performTask();
		
		// Expose information from the analysis as part of the results of the
		// task done by this worker. Some of the info is already embedded in the
		// JOBOUTPUTDATA, but it is convenient to expose 
		// it even further to simplify access to some pivotal bit of 
		// information.
		exposeOutputData(new NamedData(NORMALTERMKEY, NamedDataType.BOOLEAN, 
				outputParser.getNormalTerminationFlag()));
		exposeOutputData(exposedByAnalzer.getNamedData(
				ChemSoftConstants.JOBOUTPUTDATA));
		exposeOutputData(exposedByAnalzer.getNamedData(
				ChemSoftConstants.SOFTWAREID));
		if (jobBeingEvaluated!=null 
				&& jobBeingEvaluated.hasParameter(ChemSoftConstants.PARGEOM))
		{
			exposeOutputData(jobBeingEvaluated.getParameter(
					ChemSoftConstants.PARGEOM));
		}
		
		lastJobStepId = outputParser.getStepsFound()-1;
		/*
		This is done for any job type, therefore it is done outside this method
		exposeOutputData(new NamedData(NUMSTEPSKEY, NamedDataType.INTEGER, 
				outputParser.getStepsFound()));
		*/
		
		//TODO-gg we should put any of the data exposed by the outputParser
		// into info channels, so that it can be used by perception.
		// This may need to add a data feed-like type of info channel
		
		@SuppressWarnings("unchecked")
		Map<TxtQuery,List<String>> matchesByTQ = (Map<TxtQuery,List<String>>)
				exposedByAnalzer.getNamedData(
						ChemSoftOutputReader.MATCHESTOTEXTQRYSFORPERCEPTION)
				.getValue();
		for (TxtQuery tq : matchesByTQ.keySet())
			p.collectPerceptionScoresForTxtMatchers(tq, matchesByTQ.get(tq));
	}
	
//-----------------------------------------------------------------------------	

}
