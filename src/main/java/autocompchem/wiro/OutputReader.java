package autocompchem.wiro;

import java.io.BufferedReader;

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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.files.FileUtils;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.Job;
import autocompchem.run.SoftwareId;
import autocompchem.run.Terminator;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;

/**
 * Core components of any reader and analyzer of software's output files. 
 * It is not meant to ever be chosen as the worker charged to perform a task.
 * It only provides general purpose functionality to purpose specific 
 * implementations.
 * 
 * @author Marco Foscato
 */

public class OutputReader extends Worker
{   
    /**
     * String defining the task for analyzing any job output
     */
    public static final String ANALYSEOUTPUTTASKNAME = "analyseOutput";

    /**
     * Task about analyzing any job output
     */
    public static final Task ANALYSEOUTPUTTASK;
    static {
    	ANALYSEOUTPUTTASK = Task.make(ANALYSEOUTPUTTASKNAME);
    }
    
    /**
     * Name of the log (commonly referred to as the "output") file from 
     * comp.chem. software, i.e., the input for this class.
     */
    protected File inFile;
    
    /**
     * Root pathname for any potential output file
     */
    protected String outFileRootName;

    /**
     * Number steps/jobs/tasks found in job under analysis
     */
    protected int numSteps = 0;

    /**
     * Flag recording normal termination of job under analysis
     */
    protected boolean normalTerminated = false;

    /**
     * Flag recording whether we have read the log or not
     */
    protected boolean logHasBeenRead = false;
    
    /**
     * Data structure holding all data parsed from the job output files
     */
    protected Map<Integer,NamedDataCollector> stepsData = 
    		new TreeMap<Integer, NamedDataCollector>();
    
    /**
     * Text-based queries associated to perception of any event that requires
     * the analysis of a software log.
     */
    protected List<TxtQuery> perceptionTxtQueriesForLog;
    
    /**
     * Collection of lines matching the text-based queries associated with 
     * perception and involving the parsing of a software log.
     */
    protected Map<TxtQuery,List<String>> perceptionTQMatches = 
    		new HashMap<TxtQuery,List<String>>();
    
	/**
	 * Name of data containing the matches to {@link TxtQuery}s involved in 
	 * perception and detected upon analysis of a software's output files.
	 */
	public static final String MATCHESTOTEXTQRYSFORPERCEPTION = 
			"PERCEPTIONTXTQUERYMATCHES";
	
    protected static String NL = System.getProperty("line.separator");
    

//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "inputdefinition/OutputReader.json";
	}
	
//------------------------------------------------------------------------------

  	@Override
	public Set<Task> getCapabilities() {
		return Collections.unmodifiableSet(new HashSet<Task>(
                        Arrays.asList(ANALYSEOUTPUTTASK)));
	}
  	
//-----------------------------------------------------------------------------

	@Override
	public Worker makeInstance(Job job)  
	{
		if (job==null)
		{
			// This happens when requesting the generation of help message
			return new OutputReader();
		}
		
		if (!job.hasParameter(WIROConstants.PARJOBOUTPUTFILE))
		{
			logger.warn("WARNING: cannot detect the type of "
					+ "output to analyze. Make sure the parameter '" 
					+ WIROConstants.PARJOBOUTPUTFILE + "' is given.");
			return new OutputReader();
		}
		
		String fileName = job.getParameter(
        		WIROConstants.PARJOBOUTPUTFILE).getValueAsString();
		ReaderWriterFactory builder = ReaderWriterFactory.getInstance();
		
		try {
			Worker w = builder.makeOutputReaderInstance(new File(fileName));
			if (w==null)
			{
				Terminator.withMsgAndStatus("ERROR: log/output file '"
						+ fileName + "' could not be understood as any "
						+ "log/output "
						+ "that can be parsed by AutoCompChem.", -1);	
			}
			return w;
		} catch (FileNotFoundException e) {
			Terminator.withMsgAndStatus("ERROR: log/output file '"
					+ fileName + "' is defined by '" 
					+ WIROConstants.PARJOBOUTPUTFILE 
					+ "' but does not exist.", -1);
		}
		return new OutputReader();
	}

//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters given to the 
     * constructor.
     */

    public void initialize()
    {
    	super.initialize();

        //Get and check the input file (which is an output from a software)
        if (params.contains(WIROConstants.PARJOBOUTPUTFILE))
        {
	        String inFileName = params.getParameter(
	        		WIROConstants.PARJOBOUTPUTFILE).getValueAsString();
	        FileUtils.foundAndPermissions(inFileName,true,false,false);
	        this.inFile = new File(inFileName);
        } else {
        	Terminator.withMsgAndStatus("ERROR! No definition of the output to "
        			+ "analyse. Please provide a value for '"
        			+ WIROConstants.PARJOBOUTPUTFILE + "'.", -1);
        }
        
        //Get and check the output filename
        if (params.contains(WIROConstants.PAROUTFILEROOT))
        {
            outFileRootName = params.getParameter(
            		WIROConstants.PAROUTFILEROOT).getValueAsString();
        } else {
        	if (inFile!=null)
        	{
        		outFileRootName = FileUtils.getRootOfFileName(inFile.getName());
        	}
        }
        
        if (params.contains(WIROConstants.PAROUTFILEROOT))
        {
        	outFileRootName = params.getParameter(
                    WIROConstants.PAROUTFILEROOT).getValueAsString();
        } else {
        	outFileRootName = decideRootPathName(inFile);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Performs any of the analysis tasks set upon initialization.
     */

    @Override
    public void performTask()
    {
    	// The task is the same for any output kind. This because the actual
    	// parsing of a specific software output is done by a method that is 
    	// overwritten by subclasses
        
    	analyzeFiles();
    	
        if (exposedOutputCollector != null)
        {
        	exposeOutputData(new NamedData(MATCHESTOTEXTQRYSFORPERCEPTION, 
        			perceptionTQMatches));
        	exposeOutputData(new NamedData(WIROConstants.JOBOUTPUTDATA, 
        			stepsData));
        	exposeOutputData(new NamedData(WIROConstants.SOFTWAREID, 
        			getSoftwareID()));
        }
    }

//------------------------------------------------------------------------------

    /**
     * This method allows to alter how to define the log file to
     * read and interpret.
     * @return the log file
     */
    public File getLogPathName()
    {
    	return inFile;
    }
    
//------------------------------------------------------------------------------

    /**
     * This method allows to alter how to behave when the log/output file 
     * defined by {@link #getLogPathName()} is not found. The default is
     * to throw {@link FileNotFoundException}.
     * @throws FileNotFoundException
     */
    protected void reactToMissingLogFile(File logFile) 
    		throws FileNotFoundException
    {
    	throw new FileNotFoundException("File Not Found: "+logFile);
    }

//------------------------------------------------------------------------------

    /**
     * Reads the output files and parses all data that can be found
     * on each step/job.
     */

    protected void analyzeFiles()
    {	
        //Read and parse log files (typically called "output file")
    	File logFile = getLogPathName();
    	LogReader logReader = null;
    	try {
    		if (logFile!=null && logFile.exists())
    		{
	    		// This encapsulated any perception-related parsing of strings
	    		logReader = new LogReader(new FileReader(logFile));
	    		// This encapsulated any software-specificity in the log format
				readLogFile(logReader);
				logHasBeenRead = true;
    		} else {
    			reactToMissingLogFile(logFile);
    		}
		} catch (FileNotFoundException fnf) {
        	Terminator.withMsgAndStatus("ERROR! File Not Found: " 
        			+ logFile.getAbsolutePath(),-1);
        } catch (IOException ioex) {
			ioex.printStackTrace();
        	Terminator.withMsgAndStatus("ERROR! While reading file '" 
        			+ logFile.getAbsolutePath() + "'. Details: "
        			+ ioex.getMessage(),-1);
        } catch (Exception e) {
			e.printStackTrace();
			Terminator.withMsgAndStatus("ERROR! Unable to parse data from "
					+ "file '" + inFile + "'. Cause: " + e.getCause() 
					+ ". Message: " + e.getMessage(), -1);
        } finally {
            try {
                if (logReader != null)
                	logReader.close();
            } catch (IOException ioex2) {
                Terminator.withMsgAndStatus("ERROR! Unable to close software "
                		+ "log file reader! " + ioex2.getMessage() ,-1);
            }
        }
    	
        String strForlog = "NOT ";
        if (normalTerminated)
        {
        	strForlog = "";
        }
        
        numSteps = stepsData.size();
        
        // We may have nullified the ref to logFile, if it does not exist.
        if (logHasBeenRead)
        {
        	logger.info("Log file '" + logFile + "' contains " 
        			+ numSteps + " steps.");
        	logger.info("The overall run did " + strForlog 
        			+ "terminate normally!");
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a flag indicating if the analyzed output describes a normally 
     * terminated job or not. Calling this method before performing the analysis
     *  task will always return <code>false</code>.
     * @return the normal termination flag.
     */
    public boolean getNormalTerminationFlag()
    {
    	return normalTerminated;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the number of steps found in the analyzed output.
     * @return the number of steps found in the analyzed output.
     */
    public int getStepsFound()
    {
    	return stepsData.size();
    }
    
//------------------------------------------------------------------------------
    
	/**
     * Concrete implementation that does not really do anything more than
     * driving the reading of the entire log/output file. Software-specific 
     * subclasses of {@link OutputReader} should override this method to
     * provide adequate parsing of the log/output file.
     * @param reader the line-by-line reader that reads the log file.
     */
    protected void readLogFile(LogReader reader) throws Exception
    {
        while ((reader.readLine()) != null) {}
    }
    
//------------------------------------------------------------------------------

	/**
	 * Provides info on how to identify software output that can be analyzed
	 * by this class. All conditions referring to the same pathname (see 
	 * {@link FileFingerprint#FileFingerprint(String, int, String)}) must be 
	 * satisfied simultaneously, while those referring to different pathnames 
	 * are independent.
	 * 
	 * However, since {@link OutputReader} is not meant to be
	 * software specific, the returned value is empty when this is called from 
	 * {@link OutputReader}, meaning that no condition
	 * exists to make {@link OutputReader} be the specific reader for a file.
	 * @return an empty set, i.e., it is impossible to match the condition that
	 * if satisfied, qualifies {@link OutputReader} as the reader of choice.
	 */
	protected Set<FileFingerprint> getOutputFingerprint()
	{
		return new HashSet<FileFingerprint>();
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Return a string that identifies the software that has generated the 
	 * output that the concrete implementations of this class can analyze. 
	 * However, since {@link OutputReader} is not meant to be
	 * software specific, this returns <code>null</code> when this is called from 
	 * {@link OutputReader}.
	 * @return <code>null</code>
	 */
	public SoftwareId getSoftwareID()
	{
		return null;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Return the implementation of {@link InputWriter} that is meant 
	 * to prepare input files for the software the output of which can be 
	 * analyzed by a concrete implementation of this class. 
	 * However, since {@link OutputReader} is not meant to be
	 * software specific, this returns just the base implementation
	 * when this is called from 
	 * {@link OutputReader}.
	 */
	protected ITextualInputWriter getSoftInputWriter()
	{
		return new InputWriter();
	}
    
//------------------------------------------------------------------------------
    
    /**
     * A text file reader that overwrites the {@link #readLine()} method so that
     * every line that is read is also checked for matches to any of the
     * {@link TxtQuery}s associated with perceptions tasks.
     */
    public class LogReader extends BufferedReader
    {
		public LogReader(Reader in) {
			super(in);
		}
		
		@Override
		public String readLine() throws IOException
		{
			String line = super.readLine();
			if (line!=null)
			{
				parseLogLineForPerceptionRelevantQueries(line);
			}
			return line;
		}
    }

//------------------------------------------------------------------------------
    	
    /**
     * Sets a collection of known situations that can be perceived and that may
     * involve analysis of the log/output file from the software.
     * When the analysis
     * of result is associated with automated perception, it is often 
     * needed to search for strings in a log file. Such 
     * {@link TxtQuery}s are embedded in the situations and by giving 
     * the situations here we make the reader aware of the
     * strings that should be searched in the files. 
     * This way we search them while parsing the 
     * log/output, thus avoiding to read the file one or more times after the 
     * parsing of the output.
     * @param sitsBase the collection of known situations, each defined by a 
     * context that may include circumstances involving the matching of test
     * if the log or output files from a software.
     */
	public void setSituationBaseForPerception(SituationBase sitsBase) 
	{
		perceptionTxtQueriesForLog = new ArrayList<TxtQuery>(
				sitsBase.getAllTxTQueriesForICT(InfoChannelType.LOGFEED, 
						true));
		for (TxtQuery tq : perceptionTxtQueriesForLog)
			perceptionTQMatches.put(tq, new ArrayList<String>());
		
		//NB: the handling of the output is not yet implemented but will involve
		// this definition of the TxtQueries.
		/*
		perceptionTxtQueriesForOut = new ArrayList<TxtQuery>(
				sitsBase.getAllTxTQueriesForICT(InfoChannelType.OUTPUTFILE, 
						true));
						
		for (TxtQuery tq : perceptionTxtQueriesForOut)
			perceptionTQMatchesOut.put(tq, new ArrayList<String>());
		*/
	}

//------------------------------------------------------------------------------

    /**
     * Parses a single line looking for strings matching any of the 
     * {@link TxtQuery}s that are associated with perception tasks needed to 
     * analyze the log of a software.
     * @param line the line of text to analyze.
     */
    protected void parseLogLineForPerceptionRelevantQueries(String line)
    {
    	if (perceptionTxtQueriesForLog!=null)
    	{
			for (TxtQuery tq : perceptionTxtQueriesForLog)
			{
				if (line.matches(".*"+tq.query+".*"))
				{
					perceptionTQMatches.get(tq).add(line);
				}
			}
		}
    }
    
//------------------------------------------------------------------------------
	
}
