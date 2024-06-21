package autocompchem.chemsoftware;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.AnalysisTask.AnalysisKind;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileFingerprint;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.log.LogUtils;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Worker;

/**
 * Core components of any reader and analyser of computational chemistry 
 * software's output files.
 * 
 * @author Marco Foscato
 */

public abstract class ChemSoftOutputReader extends Worker
{   
    /**
     * Name of the log (commonly referred to as the "output") file from 
     * comp.chem. software, i.e., the input for this class.
     */
    protected File inFile;
    
    /**
     * Root pathname for any potential output file
     */
    private String outFileRootName;

    /**
     * Number steps/jobs/tasks found in job under analysis
     */
    private int numSteps = 0;

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
     * List of analysis to perform on all steps
     */
    private ArrayList<AnalysisTask> analysisAllTasks = 
    		new ArrayList<AnalysisTask>();
    
    /**
     * List of analysis to perform on the overall job. This might be
     * something like getting the overall last geometry,
     */
    protected ArrayList<AnalysisTask> analysisGlobalTasks = 
    		new ArrayList<AnalysisTask>();
    
    /**
     * List analysis to perform on each step
     */
    protected Map<Integer,ArrayList<AnalysisTask>> analysisTasks = 
    		new TreeMap<Integer, ArrayList<AnalysisTask>>();

    /**
     * Template for connectivity
     */
    protected IAtomContainer connectivityTemplate;

    /**
     * Flag controlling definition of connectivity from template
     */
    protected boolean useTemplateConnectivity = false;
    
    /**
     * Tolerance on the variation of interatomic distance of bonded atom pairs.
     * With a tolerance equal to <i>t</i> and a reference interatomic distance 
     * <i>d</i> only an absolute deviation larger than <i>abs(t*d)</i> will be 
     * seen as incompatible. Default <i>t = 0.05</i>.
     */
    private double connectivityCheckTol = 0.05;
    
    /**
     * Text-based queries associated to perception of any event that requires
     * the analysis of some comp.chem. software log.
     */
    private List<TxtQuery> perceptionTxtQueriesForLog;
    
    /**
     * Collection of lines matching the text-based queries associated with 
     * perception and involving the parsing of comp. chem. software log.
     */
    protected Map<TxtQuery,List<String>> perceptionTQMatches = 
    		new HashMap<TxtQuery,List<String>>();
    
	/**
	 * Name of data containing the matches to {@link TxtQuery}s involved in 
	 * perception and detected upon analysis of chem. soft. output files.
	 */
    //TODO-gg fix typo
	public static final String MATCHESTOTEXTQRYSFORPERCEPTION = 
			"PERCEPTIONTXTWUERYMATCHES";
	
    private static String NL = System.getProperty("line.separator");
    

//------------------------------------------------------------------------------

	@Override
	public String getKnownInputDefinition() {
		return "inputdefinition/ChemSoftOutputReader.json";
	}

//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters given to the 
     * constructor.
     */

    public void initialize()
    {
    	super.initialize();

        //Get and check the input file (which is an output from a comp.chem. 
        // software)
        if (params.contains(ChemSoftConstants.PARJOBOUTPUTFILE))
        {
	        String inFileName = params.getParameter(
	        		ChemSoftConstants.PARJOBOUTPUTFILE).getValueAsString();
	        FileUtils.foundAndPermissions(inFileName,true,false,false);
	        this.inFile = new File(inFileName);
        } else {
        	Terminator.withMsgAndStatus("ERROR! No definition of the ouput to "
        			+ "analyse. Please provide a value for '"
        			+ ChemSoftConstants.PARJOBOUTPUTFILE + "'.", -1);
        }
        
        //Get and check the output filename
        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileRootName = params.getParameter(
            		ChemSoftConstants.PAROUTFILEROOT).getValueAsString();
        } else {
        	if (inFile!=null)
        	{
        		outFileRootName = FileUtils.getRootOfFileName(inFile.getName());
        	}
        }

        if (params.contains(ChemSoftConstants.PARPRINTLASTGEOMEACH))
        {
        	String s = params.getParameter(
        			ChemSoftConstants.PARPRINTLASTGEOMEACH).getValueAsString();
            String[] p = s.split("\\s+");
            ParameterStorage ps = new ParameterStorage();
            ps.setParameter(ChemSoftConstants.GENERALFORMAT,
            		p[0]);
            if (p.length>1)
            {
                ps.setParameter(ChemSoftConstants.GENERALFILENAME, p[1]);
            }
            AnalysisTask a = new AnalysisTask(AnalysisKind.PRINTLASTGEOMETRY,ps);
            analysisAllTasks.add(a);
        }
        
        if (params.contains(ChemSoftConstants.PARPRINTLASTGEOM))
        {
        	String s = params.getParameter(
        			ChemSoftConstants.PARPRINTLASTGEOM).getValueAsString();
            String[] p = s.split("\\s+");
            ParameterStorage ps = new ParameterStorage();
            ps.setParameter(ChemSoftConstants.GENERALFORMAT, p[0]);
            if (p.length>1)
            {
                ps.setParameter(ChemSoftConstants.GENERALFILENAME, p[1]);
            }
            AnalysisTask a = new AnalysisTask(AnalysisKind.PRINTLASTGEOMETRY,ps);
            analysisGlobalTasks.add(a);
        }
        
        if (params.contains(ChemSoftConstants.PARPRINTALLGEOM))
        {
        	String s = params.getParameter(
        			ChemSoftConstants.PARPRINTALLGEOM).getValueAsString();
            String[] p = s.split("\\s+");
            ParameterStorage ps = new ParameterStorage();
            ps.setParameter(ChemSoftConstants.GENERALFORMAT, p[0]);
            if (p.length>1)
            {
                ps.setParameter(ChemSoftConstants.GENERALFILENAME, p[1]);
            }
            AnalysisTask a = new AnalysisTask(AnalysisKind.PRINTALLGEOM, ps);
            analysisAllTasks.add(a);
        }

        if (params.contains(ChemSoftConstants.PARPRINTVIBMODES))
        {
            String s = params.getParameter(
            		ChemSoftConstants.PARPRINTVIBMODES).getValueAsString();
            String[] p = s.split("\\s+");
            ArrayList<String> list = new ArrayList<String>();
            for (int i=0; i<(p.length-1); i++)
            {
                String w = p[i];
                int val = 0;
                try
                {
                    val = Integer.parseInt(w);
                } 
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot convert '" + w 
                        + "' to an integer. Check selection of vibrational "
                        + "modes in " + ChemSoftConstants.PARPRINTVIBMODES 
                        + ".", -1); 
                }
                if (val < 0)
                {
                    Terminator.withMsgAndStatus("ERROR! Negative number "
                        + "cannot be used to select a vibrational mode.",-1);
                }
                list.add(w);
            }
            ParameterStorage ps = new ParameterStorage();
            ps.setParameter(ChemSoftConstants.GENERALFILENAME, p[p.length-1]);
            ps.setParameter(ChemSoftConstants.GENERALINDEXES,
            		StringUtils.mergeListToString(list, " "));
            AnalysisTask a = new AnalysisTask(AnalysisKind.VIBMODE,ps);
            analysisAllTasks.add(a);
        }

        if (params.contains(ChemSoftConstants.PARTEMPLATECONNECTIVITY))
        {
            this.useTemplateConnectivity = true;
            String val = params.getParameter(
            		ChemSoftConstants.PARTEMPLATECONNECTIVITY)
            		.getValueAsString();
            String[] p = val.split("\\s+");
            File fileWithTplt = new File(p[0]);
            FileUtils.foundAndPermissions(fileWithTplt,true,false,false);
            //NB: assumption: only one atom container as template.
            this.connectivityTemplate = IOtools.readSDF(fileWithTplt).get(0);
            
            ParameterStorage ps = new ParameterStorage();
            for (int i=1; i<(p.length); i++)
            {
                String arg = p[i];
                if (arg.toUpperCase().startsWith(
                		ChemSoftConstants.PARBONDLENGTHTOLETANCE))
                {
                	String w = arg.substring(
                			(ChemSoftConstants.PARBONDLENGTHTOLETANCE + "=")
                				.length());
	                try
	                {
	                	connectivityCheckTol = Double.parseDouble(w);
	                } 
	                catch (Throwable t)
	                {
	                    Terminator.withMsgAndStatus("ERROR! Cannot convert '" 
	                    	+ w + "' to a double. Check "
	                        + ChemSoftConstants.PARBONDLENGTHTOLETANCE + " for "
	                        + ChemSoftConstants.PARTEMPLATECONNECTIVITY 
	                        + ".", -1); 
	                }
	                //Silly: we check the conversion to double and then store a string...
	                ps.setParameter(ChemSoftConstants.PARBONDLENGTHTOLETANCE, w);
                }
            }
            
        	// We have to ensure that last geometry is extracted
        	boolean addLastGeom = true;
        	for (AnalysisTask at : analysisGlobalTasks)
        	{
        		if (at.getKind() == AnalysisKind.PRINTLASTGEOMETRY)
        		{
        			addLastGeom = false;
        		}
        	}
        	if (addLastGeom)
        	{
        		//TODO-gg distinguish between PRINT and GET (which is supposed to get the data without printing to file)
        		analysisGlobalTasks.add(new AnalysisTask(
        				AnalysisKind.PRINTLASTGEOMETRY));
        	}
        	
            AnalysisTask a = new AnalysisTask(
            		AnalysisKind.BLVSCONNECTIVITY,ps);
            // WARNING: we assume that this analysis task as added AFTER
            // the extraction of the last geometry!!!
            analysisGlobalTasks.add(a);
            // NB: here we could have the possibility to decide which 
            // geometry to analyse: all or just the last one.
        }

        if (params.contains(ChemSoftConstants.PARGETENERGY))
        {
            String s = params.getParameter(ChemSoftConstants.PARGETENERGY)
            		.getValueAsString();
            String[] p = s.split("\\s+");
            AnalysisTask a = new AnalysisTask(AnalysisKind.QHTHERMOCHEMISTRY);
            ParameterStorage ps = new ParameterStorage();
            for (int i=0; i<p.length; i++)
            {
            	String w = p[i].toUpperCase();
            	switch(w) 
            	{
	            	case (ChemSoftConstants.QHARMTHRSLD):
	            	case (ChemSoftConstants.QHARMIGNORE):
	            	case (ChemSoftConstants.QHARMTOREAL): 
	            	{
	            		if ((i+1)>=p.length || !NumberUtils.isNumber(p[i+1]))
	            		{ 
	            			Terminator.withMsgAndStatus("ERROR! expecting a "
	            					+ "value after " + w + ".",-1);
	            		}
	            		ps.setParameter(w, p[i+1]);
	            	}
            	}
            }
            a.setParams(ps);
            analysisAllTasks.add(a);
        }
        
        if (params.contains(ChemSoftConstants.PARCRITICALPOINTKIND))
        {
            String s = params.getParameter(
            		ChemSoftConstants.PARCRITICALPOINTKIND)
            		.getValueAsString();
            String[] p = s.split("\\s+");
            ParameterStorage ps = new ParameterStorage();
            for (int i=0; i<p.length; i++)
            {
            	String w = p[i].toUpperCase();
            	switch(w) 
            	{
	            	case (ChemSoftConstants.SMALLESTFREQ): 
	            	{
	            		if ((i+1)>=p.length || !NumberUtils.isNumber(p[i+1]))
	            		{
	            			Terminator.withMsgAndStatus("ERROR! expecting a "
	            					+ "value after LOWESTFREQ.",-1);
	            		}
	            		ps.setParameter(ChemSoftConstants.SMALLESTFREQ, p[i+1]);
	            	}
            	}
            }
        	AnalysisTask a = 
        			new AnalysisTask(AnalysisKind.CRITICALPOINTKIND,ps);
        	analysisAllTasks.add(a);
        }
        
        if (params.contains(ChemSoftConstants.PARANALYSISTASKS))
        {
        	@SuppressWarnings("unchecked")
			List<AnalysisTask> tasks = (List<AnalysisTask>) params.getParameter(
            		ChemSoftConstants.PARANALYSISTASKS).getValue();
        	analysisAllTasks.addAll(tasks);
        }
        
        String msg = "Settings from parameter storage";
        for (AnalysisTask a : analysisAllTasks) 
        {
        	msg = msg + "-->"+a.toString() + NL;
        }
        logger.debug(msg);
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
        	exposeOutputData(new NamedData(ChemSoftConstants.JOBOUTPUTDATA, 
        			stepsData));
        	exposeOutputData(new NamedData(ChemSoftConstants.SOFTWAREID, 
        			getSoftwareID()));
/*
//TODO
            String refName = "";
            exposeOutputData(new NamedData(refName,
                  NamedDataType.DOUBLE, ));
*/
        }
    }

//------------------------------------------------------------------------------

    /**
     * This method allows to alter how to define the log file to
     * read and interpret.
     * @return the log file
     */
    protected File getLogPathName()
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
                Terminator.withMsgAndStatus("ERROR! Unable to close comp. "
                		+ "chem. software log file reader! "  
                		+ ioex2.getMessage() ,-1);
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
        
        // Prepare collector of analysis results
    	StringBuilder resultsString = new StringBuilder();
        AtomContainerSet geomsToExpose = new AtomContainerSet();
        TextBlock critPointKinds = new TextBlock();
        IAtomContainer lastGeomToExpose = null;
        ListOfDoubles convergedScfEnergies = new ListOfDoubles();
        ListOfDoubles qhGibbsEnergies = new ListOfDoubles();
        
        // Unless we have defined tasks for specific job steps, we take the 
        // analysis tasks defined upon initialization and perform them on all
        // job steps.
        if (analysisTasks.size() == 0)
        {
        	for (int i=0; i<numSteps; i++)
        	{
        		analysisTasks.put(i, analysisAllTasks);
        	}
        }
        
        // Analyse one or more steps
        for (Integer stepId : analysisTasks.keySet())
        {
        	// We might have requested analysis of a step that was not run...
        	if (stepId > numSteps)
        	{
        		continue;
        	}
        	
        	//This contains all the data parsed from the file/s
        	NamedDataCollector stepData = stepsData.get(stepId);
        	resultsString.append(NL + "Step " + stepId + ":" + NL);
        	
        	//TODO-gg move outside of loop over analysis: do it as long as a 
        	// templateconnectivity has been given.
            // We inherit connectivity here so all analysis of geometries can
            // make use of the connectivity
    		if (useTemplateConnectivity)
    		{
    			if (stepData.contains(ChemSoftConstants.JOBDATAGEOMETRIES))
    			{
    				AtomContainerSet acs = (AtomContainerSet) stepData
    						.getNamedData(ChemSoftConstants.JOBDATAGEOMETRIES)
    						.getValue();
    				if (acs.getAtomContainerCount() > 0)
    				{
	    				if (acs.getAtomContainer(0).getAtomCount() !=
	    						connectivityTemplate.getAtomCount())
	    				{
	    					Terminator.withMsgAndStatus("ERROR! Number of "
	    							+ "atom in template structure does "
	    							+ "not correspond to number of atom "
	    							+ "in file '" + inFile + "' (" 
	    							+ connectivityTemplate.getAtomCount() 
	    							+ " vs. "
	    							+ acs.getAtomContainer(0).getAtomCount()
	    							+ ").", -1);
	    				}
	    				for (int i=0; i<acs.getAtomContainerCount(); i++)
	    				{
	    					IAtomContainer iac = acs.getAtomContainer(i);
	    					ConnectivityUtils.
	    					importConnectivityFromReference(iac, 
	    							connectivityTemplate);
	    				}
    				}
    			}
    		}
    		
        	// What do we have to do on the current step?
        	List<AnalysisTask> todoList = analysisTasks.get(stepId);
        	if (todoList.size()==0)
        	{
        		resultsString.append(" -> No analysis has been requested." + NL);
        	}
        	for (AnalysisTask at : todoList)
        	{
        		ParameterStorage atParams = at.getParams();
        		switch (at.getKind())
        		{		
	        		case PRINTALLGEOM:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAGEOMETRIES))
	        			{
	        				logger.debug("No geometries found in "
		        						+ "step " + stepId + ".");
	        				break;
	        			}
	        			String format = "XYZ";
	        			format = changeIfParameterIsFound(format,
	        					ChemSoftConstants.GENERALFORMAT,atParams);
	        			String outFileName = outFileRootName+"_allGeoms."
	        					+ format.toLowerCase();
	        			outFileName = changeIfParameterIsFound(outFileName,
	        					ChemSoftConstants.GENERALFILENAME,atParams);
	        			File outFile = new File(outFileName);
	        			
	        			AtomContainerSet acs = (AtomContainerSet) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAGEOMETRIES).getValue();
	        			
	        			if (acs.getAtomContainerCount()==0)
	        			{
	        				logger.debug("Empty list of geometry in "
		        						+ "step " + stepId + ".");
	        				break;
	        			}
	        			
	        			geomsToExpose.add(acs);
	        			
	        			//NB: this code is repeated twice in this class: make a method
	        			if (format.equals("XYZSDF") || format.equals("SDFXYZ"))
	        			{
	        				String pathnameBase = FileUtils.getPathToPatent(
	        						outFile.getAbsolutePath()) 
	        						+ File.separator
	        						+ FileUtils.getRootOfFileName(outFile);
	        				File outXYZ = new File(pathnameBase + ".xyz");
	        				File outSDF = new File(pathnameBase + ".sdf");
		        			logger.debug("Writing all geometries ("
		        						+ acs.getAtomContainerCount() + ") of "
		        						+ "step " + stepId + " to files '" 
		        						+ outXYZ + "' and '" + outSDF + "'.");
		        			IOtools.writeAtomContainerSetToFile(outXYZ, acs,
		        					"XYZ", true);
		        			IOtools.writeAtomContainerSetToFile(outSDF, acs,
		        					"SDF", true);
	        			} else {
		        			logger.debug("Writing all geometries ("
		        						+ acs.getAtomContainerCount() + ") of "
		        						+ "step " + stepId + " to file '" 
		        						+ outFile+"'");
		        			IOtools.writeAtomContainerSetToFile(outFile, acs,
		        					format, true);
	        			}
	        			
	        			resultsString.append("-> #geometries ").append(
	        					acs.getAtomContainerCount());
	        			resultsString.append(NL);
	        			break;
	        		}
	        		
	        		case PRINTLASTGEOMETRY:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAGEOMETRIES))
	        			{
	        				logger.debug("No geometry found in step " 
	        						+ stepId + ".");
	        				break;
	        			}
	        			String format = "XYZ";
	        			format = changeIfParameterIsFound(format,
	        					ChemSoftConstants.GENERALFORMAT,atParams);
	        			String outFileName = outFileRootName+"_lastGeom."
	        					+ format.toLowerCase();
	        			outFileName= changeIfParameterIsFound(outFileName,
	        					ChemSoftConstants.GENERALFILENAME,atParams);
	        			File outFile = new File(outFileName);
	        			
	        			AtomContainerSet acs = (AtomContainerSet) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAGEOMETRIES).getValue();
	        			
	        			if (acs.getAtomContainerCount() == 0)
	        			{
	        				logger.debug("Empty list of geometries "
		        						+ "in step " + stepId + ".");
	        				break;
	        			}
	        			
	        			IAtomContainer mol = acs.getAtomContainer(
	        					acs.getAtomContainerCount()-1);
	        			
	        			lastGeomToExpose = mol;
	        			
	        			if (format.equals("XYZSDF") || format.equals("SDFXYZ"))
	        			{
	        				String pathnameBase = FileUtils.getPathToPatent(
	        						outFile.getAbsolutePath()) 
	        						+ File.separator
	        						+ FileUtils.getRootOfFileName(outFile);
	        				File outXYZ = new File(pathnameBase + ".xyz");
	        				File outSDF = new File(pathnameBase + ".sdf");
		        			logger.debug("Writing last geometry of "
		        						+ "step " + stepId + " to files '" 
		        						+ outXYZ + "' and '" + outSDF + "'");
		        			IOtools.writeAtomContainerToFile(outXYZ, mol,
		        					"XYZ", true);
		        			IOtools.writeAtomContainerToFile(outSDF, mol,
		        					"SDF", true);
	        			} else {
		        			logger.debug("Writing last geometry of "
		        						+ "step " + stepId + " to file '" 
		        						+ outFileName + "'");
		        			IOtools.writeAtomContainerToFile(outFile, mol,
		        					format, true);
	        			}

                        resultsString.append("-> extracting last geometry out"
                        		+ " of ")
                        	.append(acs.getAtomContainerCount());
                        resultsString.append(NL);

	        			break;
	        		}
	        		
	        		case CRITICALPOINTKIND:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAVIBFREQ))
	        			{
	        				logger.debug("No frequencies found in "
		        						+ "step " + stepId + ".");
	        				break;
	        			}
	        			Double smallest = 0.0;
	        			smallest = changeIfParameterIsFound(smallest,
	        					ChemSoftConstants.SMALLESTFREQ,atParams);
	        			ListOfDoubles freqs = (ListOfDoubles) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAVIBFREQ).getValue();
	        			ListOfDoubles imgFreq = new ListOfDoubles();
	        			boolean ignoredSome = false;
	        			for (Double freq : freqs)
	        			{
	        				if (Math.abs(freq)>smallest && freq<0)
	        				{
	        					imgFreq.add(freq);
	        				}
	        				if (Math.abs(freq)<smallest && freq<0)
	        				{
	        					ignoredSome = true;
	        				}
	        			}
	        			String kindOfCriticalPoint = "";
	        			String imgFreqStr = "";
	        			switch (imgFreq.size())
	        			{
	        			case 0:
	        				kindOfCriticalPoint = "MINIMUM";
	        				break;
	        			case 1:
	        				kindOfCriticalPoint = "TRANSITION STATE";
	        				imgFreqStr = imgFreq.toString();
	        				break;
	        			default:
	        				kindOfCriticalPoint = "SADDLE POINT (order " 
	        						+ imgFreq.size() + ")";
	        				imgFreqStr = imgFreq.toString().replace("-","i");
	        				break;	        				
	        			}
        				imgFreqStr = imgFreqStr.replace("-", "i");
        				resultsString.append("-> Critial Point: ").append(
        						kindOfCriticalPoint);
	        			if (Math.abs(smallest) > 0.000001 && ignoredSome)
	        			{
	        				resultsString.append(" (ignoring v<"+smallest+")");
	        			}
	        			resultsString.append(" ").append(imgFreqStr);
	        			resultsString.append(NL);
	        			
	        			critPointKinds.add(kindOfCriticalPoint);
	        			break;
	        		}
	        		
	        		case SCFENERGY:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATASCFENERGIES))
	        			{
	        				logger.debug("No SCF converged energy "
		        						+ "found in step " + stepId + ".");
	        				break;
	        			}
	        			ListOfDoubles l = (ListOfDoubles) stepData.getNamedData(
	        					ChemSoftConstants.JOBDATASCFENERGIES).getValue();
	        			if (l.size()==0)
	        			{
	        				logger.debug("Zero SCF converged energy "
		        						+ "found in step " + stepId + ".");
	        				break;
	        			}
	        			Double energy = l.get(l.size()-1);
	        			resultsString.append("-> SCF Energy ").append(energy);
	        			resultsString.append(NL);
	        			
	        			convergedScfEnergies.add(energy);
	        			break;
	        		}
	        		
	        		case QHTHERMOCHEMISTRY:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAVIBFREQ))
	        			{
	        				logger.debug("No frequencies found in "
		        						+ "step " + stepId 
		        						+ ". Skipping " + at +".");
	        				break;
	        			}
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY))
	        			{
	        				logger.debug("No Gibbs free energy found"
		        						+ " in step " + stepId 
		        						+ ". Skipping " + at + ".");
	        				break;
	        			}
	        			
	        			Double gibbsFreeEnergy = (Double) stepData.getNamedData(
	        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY)
	        					.getValue();
	        			
	        			String qhTitlePrefix = "";
	        			String qhTitleSuffix = "";
	        			Double qhThrsh = 0.0; //NB: hard-coded value used below!
	        			qhThrsh = changeIfParameterIsFound(qhThrsh,
	        					ChemSoftConstants.QHARMTHRSLD,atParams);
	        			Double imThrsh = 0.0; //NB: hard-coded value used below!
	        			imThrsh  = changeIfParameterIsFound(imThrsh,
	        					ChemSoftConstants.QHARMTOREAL,atParams);
	        			Double ignThrsh = 0.01;//NB: hard-coded value used below!
	        			ignThrsh = changeIfParameterIsFound(ignThrsh,
	        					ChemSoftConstants.QHARMIGNORE,atParams);
	        			if (!NumberUtils.closeEnough(qhThrsh, 0.0) 
	        					|| !NumberUtils.closeEnough(imThrsh, 0.0) 
	        					|| !NumberUtils.closeEnough(ignThrsh, 0.01) )
	        			{
	        				qhTitlePrefix = "Quasi-Harm. corrected ";
	        				qhTitleSuffix =  " (" 
	        						+ ChemSoftConstants.QHARMTHRSLD + " "
	    	        				+ qhThrsh + "; "
	    	        				+ ChemSoftConstants.QHARMTOREAL + " "
	    	        			    + imThrsh + "; "
	    	        			    + ChemSoftConstants.QHARMIGNORE + " " 
	    	        			    + ignThrsh + ")";
	        			
		        			Double temp = changeIfParameterIsFound(298.15,
		        					ChemSoftConstants.JOBDATTHERMOCHEM_TEMP,
		        					atParams);
		        			
		        			@SuppressWarnings("unchecked")
							Double vibS = 
								CompChemComputer.vibrationalEntropyCorr(
		        					(ArrayList<Double>) stepData.getNamedData(
		    	        					ChemSoftConstants.JOBDATAVIBFREQ)
		        					.getValue(), temp); // J/(mol*K)
		        			vibS = vibS / ACCConstants.HARTREETOJOULEPERMOLE;
		        			// J/(mol*K) * ((Eh * mol)/J) = Eh/K = Hartree/K
	
		        			@SuppressWarnings("unchecked")
							Double qhVibS = 
								CompChemComputer.vibrationalEntropyCorr(
		        					(ArrayList<Double>) stepData.getNamedData(
		    	        					ChemSoftConstants.JOBDATAVIBFREQ)
		        					.getValue(), temp, qhThrsh, imThrsh, 
		        						ignThrsh, logger); // J/(mol*K)
		        			qhVibS = qhVibS / ACCConstants.HARTREETOJOULEPERMOLE;
		        			// J/(mol*K) * ((Eh * mol)/J) = Eh/K = Hartree/K
	
		        			logger.debug("Quasi-harmonic approx "
		        						+ "changes vibrational entropy from "
		        						+ vibS + " (a.u.) to " + qhVibS 
		        						+ " (a.u.).");
	        			
		        			gibbsFreeEnergy = gibbsFreeEnergy + vibS*temp 
		        					- qhVibS*temp;
	        			}
	        			
	        			resultsString.append("-> ")
	        				.append(qhTitlePrefix)
	        				.append("Gibbs free energy ")
	        				.append(gibbsFreeEnergy)
	        				.append(qhTitleSuffix).append(NL);
	        			
	        			qhGibbsEnergies.add(gibbsFreeEnergy);
	        			break;
	        		}
	        		
	        		case VIBMODE:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAVIBMODES))
	        			{
	        				logger.debug("No normal modes found in "
		        						+ "step " + stepId + ".");
	        				break;
	        			}
	        			NormalModeSet nms = 
	        					(NormalModeSet) stepData.getNamedData(
	        					ChemSoftConstants.JOBDATAVIBMODES).getValue();
	        			String outFileName = outFileRootName + "_nm.xyz";
	        			outFileName = changeIfParameterIsFound(outFileName, 
	        					ChemSoftConstants.GENERALFILENAME, atParams);
	        			File outFile = new File (outFileName);
	        			
	        			String idxsStr = "";
	        			idxsStr = changeIfParameterIsFound(idxsStr, 
	        					ChemSoftConstants.GENERALINDEXES, atParams);
	        			boolean all = false;
	        			ListOfIntegers idxs = new ListOfIntegers();
	        			if (!idxsStr.equals(""))
	        			{
		        			String[] p = idxsStr.split("\\s+");
		        			for (int i=0; i<p.length; i++)
		        			{
		        				idxs.add(Integer.parseInt(p[i]));
		        			}
	        			} else {
	        				all = true;
	        				for (int i=0; i<nms.size(); i++)
	        				{
	        					idxs.add(i);
	        				}
	        			}
	        			StringBuilder sb = new StringBuilder();
	        			String msg = "";
        				if (all)
        				{
        					msg = "Exporting all normal modes";
        				} else {
        					msg = "Exporting normal modes ";
        				}
	        			for (Integer id : idxs)
	        			{
	        				if (!all)
		        			{ 
	        					msg = msg + id + "  ";
		        			}
	        				sb.append("# Normal mode #").append(id).append(NL);
	        				sb.append(nms.get(id).toLines());
	        			}
	        			msg = msg + "to file '" + outFile + "'";
	        			logger.debug(msg);
	        			
	        			IOtools.writeTXTAppend(outFile, sb.toString(), true);
	        			
	        			resultsString.append("-> #vibrational Modes ").append(
	        					nms.size());
	        			resultsString.append(NL);
	        			break;
	        		}
	        		
	        		case BLVSCONNECTIVITY:
	        		{
	        			// Nothing to do. This is assumed to be a global task:
	        			// it is performed only on the overall final result, not
	        			// in intermediate steps (i.e., not here).
						break;
	        		}
        		}
        		
        		//TODO: more? ...just add it here
        		
        	} // loop over analysis tasks
        } // loop over steps
     	
        String results = resultsString.toString();
        if (results!=null && !results.isBlank())
        {
        	logger.info("Summary of the results:" + NL + results);
        }
        
        // Prepare collector of final analysis results
    	StringBuilder finalResultsString = new StringBuilder();
    	
        // WARNING: the LASTGEOMETRY analysis is expected to run before
        // the BLVSCONNECTIVITY
        
        // Analyse the collection of steps takes as a whole
        for (AnalysisTask at : analysisGlobalTasks)
        {
        	ParameterStorage atParams = at.getParams();
    		switch (at.getKind())
    		{
				case PRINTLASTGEOMETRY:
				{
					String format = "XYZ";
					format = changeIfParameterIsFound(format,
							ChemSoftConstants.GENERALFORMAT,atParams);
					String outFileName = outFileRootName+"_lastGeom."
							+ format.toLowerCase();
					outFileName = changeIfParameterIsFound(outFileName,
							ChemSoftConstants.GENERALFILENAME,atParams);
					File outFile = new File(outFileName);
					
					IAtomContainer lastGeom = null;
					for (int stepId=0; stepId<numSteps; stepId++)
					{
						if (!stepsData.containsKey(stepId))
						{
							continue;
						}
						NamedDataCollector stepData = stepsData.get(stepId);
						if (stepData.contains(
								ChemSoftConstants.JOBDATAGEOMETRIES))
						{
							AtomContainerSet acs = (AtomContainerSet) 
									stepData.getNamedData(ChemSoftConstants
											.JOBDATAGEOMETRIES).getValue();
							lastGeom = acs.getAtomContainer(
									acs.getAtomContainerCount()-1);
						}
					}
					
					if (lastGeom == null || lastGeom.isEmpty())
					{
						logger.warn("WARNING! Empty list of "
		    						+ "geometries from this job. "
		    						+ "I cannot find the last "
		    						+ "geometry ");
						break;
					}
					
					//NB: this code is repeated twice in this class: make a method
					if (format.equals("XYZSDF") || format.equals("SDFXYZ"))
        			{
        				String pathnameBase = FileUtils.getPathToPatent(
        						outFile.getAbsolutePath()) 
        						+ File.separator
        						+ FileUtils.getRootOfFileName(outFile);
        				File outXYZ = new File(pathnameBase + ".xyz");
        				File outSDF = new File(pathnameBase + ".sdf");
	        			logger.debug("Writing overall last geometry "
	        						+ "to files '" 
	        						+ outXYZ + "' and '" + outSDF + "'.");
	        			IOtools.writeAtomContainerToFile(outXYZ, lastGeom,
	        					"XYZ", true);
	        			IOtools.writeAtomContainerToFile(outSDF, lastGeom,
	        					"SDF", true);
        			} else {
						logger.debug("Writing overal last geometry to "
									+ "file '" + outFile+"'");
						IOtools.writeAtomContainerToFile(outFile, lastGeom,
	        					format, true);
        			}
					lastGeomToExpose = lastGeom;
					break;
				}
				
				case BLVSCONNECTIVITY:
				{
					connectivityCheckTol = changeIfParameterIsFound(
							connectivityCheckTol,
							ChemSoftConstants.PARBONDLENGTHTOLETANCE, atParams);

					String result = " compatible ";
					StringBuffer log = new StringBuffer();
					
					// WARNING: lastGeomToExpose is not null because this task
					// should come (if at all) after the extraction of the last 
					// geometry. Still, it might be that the geometry was not 
					// available in the log/output files.
					boolean isCompatible = true;
					if (lastGeomToExpose == null)
					{
						logger.warn("WARNING! Undefined last geometry. "
								+ "I cannot compare connectivity with bond "
								+ "lengths. ");
						isCompatible = false;
					} else if (!ConnectivityUtils.compareBondDistancesWithReference(
							lastGeomToExpose, connectivityTemplate, 
							connectivityCheckTol, logger, log))
					{
						isCompatible = false;
					}
					if (!isCompatible)
					{
						result = " NOT compatible! " + log.toString();
					}
					finalResultsString.append("Bond lengths vs. connectivity:")
						.append(result).append(NL);
					break;
				}
    		}
        }
        
        //TODO-gg expose more
        if (geomsToExpose.getAtomContainerCount()>0)
        {
	        exposeOutputData(new NamedData(ChemSoftConstants.JOBDATAGEOMETRIES,
	        		NamedDataType.UNDEFINED, geomsToExpose));
        }
        
        if (!finalResultsString.toString().isBlank())
        {
        	logger.info(NL + "Summary of final results: " 
        			+ finalResultsString.toString());
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Looks for a parameter named <code>key</code> in the given 
     * {@link ParameterStorage} and return its value, if such parameter is found,
     * or the given <code>default</code> is such parameter is not found in the
     * {@link ParameterStorage}.
     * @param defaultValue the default value.
     * @param key the reference name of the parameter to try to find.
     * @return either the default value, or, if the parameter is found, 
     * the value of that parameter.
     */
    protected static double changeIfParameterIsFound(double defaultValue, 
    		String key, ParameterStorage ps) 
    {
    	String iStr = Double.toString(defaultValue);
    	iStr = changeIfParameterIsFound(iStr, key, ps);
    	return Double.parseDouble(iStr);
    }

//------------------------------------------------------------------------------
    
    /**
     * Looks for a parameter named <code>key</code> in the given 
     * {@link ParameterStorage} and return its value, if such parameter is found,
     * or the given <code>default</code> is such parameter is not found in the
     * {@link ParameterStorage}.
     * @param defaultValue the default value.
     * @param key the reference name of the parameter to try to find.
     * @return either the default value, or, if the parameter is found, 
     * the value of that parameter.
     */
    protected static ListOfIntegers changeIfParameterIsFound(
    		ListOfIntegers defaultValue, 
    		String key, ParameterStorage ps) 
    {
		if (ps.contains(key))
		{
			return (ListOfIntegers) ps.getParameter(key).getValue();
		}
    	return defaultValue;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Looks for a parameter named <code>key</code> in the given 
     * {@link ParameterStorage} and return its value, if such parameter is found,
     * or the given <code>default</code> is such parameter is not found in the
     * {@link ParameterStorage}.
     * @param defaultValue the default value.
     * @param key the reference name of the parameter to try to find.
     * @return either the default value, or, if the parameter is found, 
     * the value of that parameter.
     */
    protected static int changeIfParameterIsFound(int defaultValue, String key,
    		ParameterStorage ps) 
    {
    	String iStr = Integer.toString(defaultValue);
    	iStr = changeIfParameterIsFound(iStr, key, ps);
    	return Integer.parseInt(iStr);
    }
    
//------------------------------------------------------------------------------

    /**
     * Looks for a parameter named <code>key</code> in the given 
     * {@link ParameterStorage} and return its value, if such parameter is found,
     * or the given <code>default</code> is such parameter is not found in the
     * {@link ParameterStorage}.
     * @param defaultValue the default value.
     * @param key the reference name of the parameter to try to find.
     * @return either the default value, or, if the parameter is found, 
     * the value of that parameter.
     */
    protected static String changeIfParameterIsFound(String defaultValue, 
    		String key, ParameterStorage ps) 
    {
		if (ps.contains(key))
		{
			return ps.getParameter(key).getValueAsString();
		}
		return defaultValue;
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
     * Method that parses the log file of a comp.chem. software.
     * This method is meant to be overwritten by subclasses. 
     * @param reader the line-by-line reader that reads the log file.
     */
    protected abstract void readLogFile(LogReader reader) throws Exception;
    
//------------------------------------------------------------------------------

	/**
	 * Provides info on how to identify software output that can be analyzed
	 * by this class.
	 * @return the data structure defining how to identify an output file 
	 * readable by this class.
	 */
	protected abstract Set<FileFingerprint> getOutputFingerprint();
	
//------------------------------------------------------------------------------
	
	/**
	 * Return a string that identifies the software that has generated the 
	 * output that the concrete implementations of this class can analyze.
	 */
	protected abstract String getSoftwareID();
	
//------------------------------------------------------------------------------

	/**
	 * Return the implementation of {@link ChemSoftInputWriter} that is meant 
	 * to prepare input files for the software the output of which can be 
	 * analyzed by a concrete implementation of this class.
	 */
	protected abstract ChemSoftInputWriter getChemSoftInputWriter();
    
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
     * involve analysis of the log/output file from comp chem software.
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
     * if the log or output files from a comp. chem. software.
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
     * analyze the log of a comp. chem. software.
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
