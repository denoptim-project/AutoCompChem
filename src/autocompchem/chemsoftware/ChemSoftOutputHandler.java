package autocompchem.chemsoftware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import autocompchem.chemsoftware.AnalysisTask.AnalysisKind;
import autocompchem.chemsoftware.nwchem.NWChemTask;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlock;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reader and analyser for computational chemistry software output files.
 * 
 * @author Marco Foscato
 */

public class ChemSoftOutputHandler extends Worker
{   
    /**
     * Name of the output file from comp.chem. software, i.e., 
     * the input for this class.
     */
    private File inFile;
    
    /**
     * Root pathname for any potential output file
     */
    private String outFileRootName;

    /**
     * Pathnames where to print results such output geometries, trajectories,
     * vibrational modes, etc.
     */
    private Map<String,String> outPathNames = new HashMap<String,String>();

    /**
     * Verbosity level
     */
    private int verbosity = 1;

    /**
     * Number steps/jobs/tasks found in job under analysis
     */
    private int numSteps = 0;

    /**
     * Flag recording normal termination of job under analysis
     */
    protected boolean normalTerminated = false;
    
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
     * List analysis to perform on each step
     */
    private Map<Integer,ArrayList<AnalysisTask>> analysisTasks = 
    		new TreeMap<Integer, ArrayList<AnalysisTask>>();

    /**
     * Template for connectivity
     */
    private IAtomContainer connectivityTemplate;

    /**
     * Flag controlling definition of connectivity from template
     */
    private boolean useTemplateConnectivity = false;
    
    /**
     * String collecting the results of the analysis
     */
    private String analysisResultLog = "";
    
    private static String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValueAsString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to "
            		+ "ChemSoftwareOutputHandler");

        //Get and check the input file (which is an output from a comp.chem. 
        // software)
        String inFileName = params.getParameter(
        		ChemSoftConstants.PARJOBOUTPUTFILE).getValueAsString();
        FileUtils.foundAndPermissions(inFileName,true,false,false);
        this.inFile = new File(inFileName);

        //Get and check the output filename
        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileRootName = params.getParameter(
            		ChemSoftConstants.PAROUTFILEROOT).getValueAsString();
        } 
        else
        {
        	outFileRootName = FileUtils.getRootOfFileName(inFileName);
        }

        if (params.contains(ChemSoftConstants.PARPRINTLASTGEOM))
        {
        	String s = params.getParameter(
        			ChemSoftConstants.PARPRINTLASTGEOM).getValueAsString();
            String[] p = s.split("\\s+");
            ParameterStorage ps = new ParameterStorage();
            ps.setParameter(new Parameter(ChemSoftConstants.GENERALFORMAT,
            		p[0]));
            if (p.length>1)
            {
                ps.setParameter(new Parameter(
                		ChemSoftConstants.GENERALFILENAME, p[1]));
            }
            AnalysisTask a = new AnalysisTask(AnalysisKind.LASTGEOMETRY,ps);
            analysisAllTasks.add(a);
        }
        
        if (params.contains(ChemSoftConstants.PARPRINTALLGEOM))
        {
        	String s = params.getParameter(
        			ChemSoftConstants.PARPRINTALLGEOM).getValueAsString();
            String[] p = s.split("\\s+");
            ParameterStorage ps = new ParameterStorage();
            ps.setParameter(new Parameter(ChemSoftConstants.GENERALFORMAT,
            		p[0]));
            if (p.length>1)
            {
                ps.setParameter(new Parameter(
                		ChemSoftConstants.GENERALFILENAME, p[1]));
            }
            AnalysisTask a = new AnalysisTask(AnalysisKind.ALLGEOM,ps);
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
            ps.setParameter(new Parameter(ChemSoftConstants.GENERALFILENAME,
            		p[p.length-1]));
            ps.setParameter(new Parameter(ChemSoftConstants.GENERALINDEXES,
            		StringUtils.mergeListToString(list, " ")));
            AnalysisTask a = new AnalysisTask(AnalysisKind.VIBMODE,ps);
            analysisAllTasks.add(a);
        }

        if (params.contains(ChemSoftConstants.PARTEMPLATECONNECTIVITY))
        {
            this.useTemplateConnectivity = true;
            String fileWithTplt = params.getParameter(
            		ChemSoftConstants.PARTEMPLATECONNECTIVITY)
            		.getValueAsString();
            FileUtils.foundAndPermissions(fileWithTplt,true,false,false);
            this.connectivityTemplate = IOtools.readSDF(fileWithTplt).get(0);
        }

        if (params.contains(ChemSoftConstants.PARGETENERGY))
        {
            String s = params.getParameter(ChemSoftConstants.PARGETENERGY)
            		.getValueAsString();
            String[] p = s.split("\\s+");
            AnalysisTask a = new AnalysisTask(AnalysisKind.ENERGY);
            ParameterStorage ps = new ParameterStorage();
            for (int i=0; i<p.length; i++)
            {
            	String w = p[i].toUpperCase();
            	switch(w) 
            	{
	            	case (ChemSoftConstants.QHARMTHRSLD): 
	            	{
	            		a = new AnalysisTask(AnalysisKind.QHTHERMOCHEMISTRY);
	            		if ((i+1)>=p.length || !NumberUtils.isNumber(p[i+1]))
	            		{
	            			Terminator.withMsgAndStatus("ERROR! expecting a "
	            					+ "value after QUASIHARM.",-1);
	            		}
	            		ps.setParameter(new Parameter(
	            				ChemSoftConstants.QHARMTHRSLD, p[i+1]));
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
	            		ps.setParameter(new Parameter(
	            				ChemSoftConstants.SMALLESTFREQ, p[i+1]));
	            	}
            	}
            }
        	AnalysisTask a = 
        			new AnalysisTask(AnalysisKind.CRITICALPOINTKIND,ps);
        	analysisAllTasks.add(a);
        }
        
        if (verbosity > 2)
        {
        	System.out.println("Settings from parameter storage");
	        for (AnalysisTask a : analysisAllTasks) 
	        {
	        	System.out.println("-->"+a.toString());
	        }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Performs any of the analysis tasks set upon initialisation
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
     * Reads the output files and parses all data that can be found
     * on each step/job.
     */

    private void analyzeFiles()
    {	
        //Read and parse log files (typically called "output file")
    	try {
    		//software-specificity encapsulated in here
			readLogFile(inFile); 
		} catch (Exception e) {
			e.printStackTrace();
			Terminator.withMsgAndStatus("ERROR! Unable to parse data from "
					+ "file '" + inFile + "'. Cause: " + e.getCause() 
					+ ". Message: " + e.getMessage(), -1);
		}
    	
        String strForlog = "NOT ";
        if (normalTerminated)
        {
        	strForlog = "";
        }
        
        numSteps = stepsData.size();
        
        if (verbosity > -1)
        {
        	System.out.println(" Log file '" + inFile + "' contains " 
        			+ numSteps + " steps.");
        	System.out.println(" The overall run did " + strForlog 
        			+ "terminate normally!");
        }
        
        // Prepare collector of analysis results
    	StringBuilder resultsString = new StringBuilder();
        AtomContainerSet geomsToExpose = new AtomContainerSet();
        TextBlock critPointKinds = new TextBlock();
        IAtomContainer lastGeomToExpose = null;
        
        // Unless we have defined tasks for specific job steps, we take the 
        // analysis tasks defined upon initialisation and perform them on all
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
        	
        	NamedDataCollector stepData = stepsData.get(stepId);
        	resultsString.append(NL + "Step " + stepId + ":" + NL);
        	
        	// What do we have to do on the current step?
        	ArrayList<AnalysisTask> todoList = analysisTasks.get(stepId);
        	for (AnalysisTask at : todoList)
        	{
        		ParameterStorage atParams = at.getParams();
        		switch (at.getKind())
        		{
	        		case ALLGEOM:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAGEOMETRIES))
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No geometry found in step "
		        						+ stepId + ".");
		        			}
	        				break;
	        			}
	        			String format = "XYZ";
	        			format = changeIfParameterIsFound(format,
	        					ChemSoftConstants.GENERALFORMAT,atParams);
	        			String outFileName = outFileRootName+"_allGeoms."
	        					+ format.toLowerCase();
	        			outFileName= changeIfParameterIsFound(outFileName,
	        					ChemSoftConstants.GENERALFILENAME,atParams);
	        			
	        			if (verbosity > 1)
	        			{
	        				System.out.println("Writing all geometries of "
	        						+ "step " + stepId + " to file '" 
	        						+ outFileName+"'");
	        			}
	        			AtomContainerSet acs = (AtomContainerSet) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAGEOMETRIES).getValue();
	        			
	        			geomsToExpose.add(acs);
	        			
	        			IOtools.writeAtomContainerSetToFile(outFileName, acs,
	        					format,true);
	        			break;
	        		}
	        		
	        		case LASTGEOMETRY:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAGEOMETRIES))
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No geometry found in step "
		        						+ stepId + ".");
		        			}
	        				break;
	        			}
	        			String format = "XYZ";
	        			format = changeIfParameterIsFound(format,
	        					ChemSoftConstants.GENERALFORMAT,atParams);
	        			String outFileName = outFileRootName+"_lastGeom."
	        					+ format.toLowerCase();
	        			outFileName= changeIfParameterIsFound(outFileName,
	        					ChemSoftConstants.GENERALFILENAME,atParams);
	        			
	        			if (verbosity > 1)
	        			{
	        				System.out.println("Writing last geometry of "
	        						+ "step " + stepId + " to file '" 
	        						+ outFileName+"'");
	        			}
	        			AtomContainerSet acs = (AtomContainerSet) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAGEOMETRIES).getValue();
	        			
	        			IAtomContainer mol = acs.getAtomContainer(
	        					acs.getAtomContainerCount()-1);
	        			
	        			lastGeomToExpose = mol;
	        			
	        			IOtools.writeAtomContainerToFile(outFileName, mol,
	        					format,true);
	        			break;
	        		}
	        		
	        		case CRITICALPOINTKIND:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAVIBFREQ))
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No frequencies found in "
		        						+ "step " + stepId + ".");
		        			}
	        				break;
	        			}
	        			String smallestStr = "0.0";
	        			smallestStr = changeIfParameterIsFound(smallestStr,
	        					ChemSoftConstants.SMALLESTFREQ,atParams);
	        			Double smallest = Double.parseDouble(smallestStr);
	        			ListOfDoubles freqs = (ListOfDoubles) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAVIBFREQ).getValue();
	        			ListOfDoubles imgFreq = new ListOfDoubles();
	        			for (Double freq : freqs)
	        			{
	        				if (Math.abs(freq)>smallest && freq<0)
	        				{
	        					imgFreq.add(freq);
	        				}
	        			}
	        			String kindOfCriticalPoint = "";
	        			switch (imgFreq.size())
	        			{
	        			case 0:
	        				kindOfCriticalPoint = "MINIMUM";
	        				break;
	        			case 1:
	        				kindOfCriticalPoint = "TRANSITION STATE";
	        				break;
	        			default:
	        				kindOfCriticalPoint = "SADDLE POINT (" 
	        						+ "order " + imgFreq.size()+ ")";
	        				break;	        				
	        			}
	        			resultsString.append("-> ").append(kindOfCriticalPoint);
	        			resultsString.append(" ").append(imgFreq.toString());
	        			resultsString.append(NL);
	        			
	        			critPointKinds.add(kindOfCriticalPoint);
	        		}
	        		
	        		case ENERGY:
	        		{
	        			
	        		}
	        		
	        		case QHTHERMOCHEMISTRY:
	        		{
	        			
	        		}
	        		
	        		case VIBMODE:
	        		{
	        			
	        		}
	        		
	        		case VIBMODENUM:
	        		{
	        			
	        		}
        		}
        		
        		//TODO
        		// Store result in output collector (to be exposed)
        	}
        }
     	
        if (verbosity > 0)
        {
            System.out.println(resultsString.toString());
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @param original the original value
     * @param key the reference name of the parameter to try to find
     * @return either the original value, or, if the given parameter is found, 
     * the value of that parameter
     */
    private int changeIfParameterIsFound(int original, String key,
    		ParameterStorage ps) 
    {
    	String iStr = Integer.toString(original);
    	iStr = changeIfParameterIsFound(iStr, key, ps);
    	return Integer.parseInt(iStr);
    }
    
//------------------------------------------------------------------------------

    /**
     * @param original the original value
     * @param key the reference name of the parameter to try to find
     * @return either the original value, or, if the given parameter is found, 
     * the value of that parameter
     */
    private String changeIfParameterIsFound(String original, String key,
    		ParameterStorage ps) 
    {
		if (ps.contains(key))
		{
			return ps.getParameter(key).getValueAsString();
		}
		return original;
	}

//------------------------------------------------------------------------------
	/**
     * Method that parses the log file of a comp.chem. software.
     * This method is meant to be overwritten by subclasses. 
     * @param file
     */
    protected void readLogFile(File file) throws Exception
    {
    	// This is a template to be used in the subclasses
    	/*
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
            	
                 ...and here goes the software-specific code!
            	
            }
        } catch (FileNotFoundException fnf) {
            System.err.println("File Not Found: " + file.getAbsolutePath());
            System.err.println(fnf.getMessage());
            System.exit(-1);
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
            System.exit(-1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
                System.err.println(ioex2.getMessage());
                System.exit(-1);
            }
        }
        */
    	Terminator.withMsgAndStatus(" ERROR! A subclass of "
    			+ "ChemSoftOutputHandler did "
    			+ "not overwrite the readLogFile method. Please, report this "
    			+ "to the authors.",-1);
    }

//------------------------------------------------------------------------------

    /**
     * TODO
     */

    public void printLastOutputGeometry(String outFormat)
    {
    	//TODO ensure we have run the analysis
    	
    	//TODO split the part where you chose which molecule/s
    	// from that where you write them
    	
    	IAtomContainer mol = null;
        switch (outFormat.toUpperCase())
        {
            case "SDF":
            {
                String outFile = outFileRootName + ".sdf";
                if (useTemplateConnectivity)
                {
                    ConnectivityUtils.importConnectivityFromReference(
                                                mol,connectivityTemplate);
                }
                IOtools.writeSDFAppend(outFile,mol,false);
                break;
            }

            case "XYZ":
            {
            	String outFile = outFileRootName + ".sdf";
                IOtools.writeXYZAppend(outFile,mol,false);
                break;
            }

            case "SDFXYZ":
            {
            	String outFile = outFileRootName + ".xyz";
                IOtools.writeXYZAppend(outFile + ".xyz",mol,false);
                if (useTemplateConnectivity)
                {
                    ConnectivityUtils.importConnectivityFromReference(
                                                mol,connectivityTemplate);
                }
                outFile = outFileRootName + ".sdf";
                IOtools.writeSDFAppend(outFile + ".sdf",mol,false);
                break;
            }

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + outFormat 
                    + "' cannot be use for output in this context. Try SDF,"
                    + " XYZ, or SDFXYZ (will print both).",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Print selected vibrational modes projected in Cartesian coordinates
     * according to the parameters defined by constructor.
     */

    /*
    private void printVibrationalModes()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int vmId : selectedVibModes)
        {
            if (first)
            {
                first = false;
            } else {
                sb.append(">").append(System.getProperty("line.separator"));
            }
            ArrayList<Double> mode = vibModes.get(vmId);
            for (int atmId=0; atmId<(mode.size()/3); atmId++)
            {
                sb.append(String.format(" %5.5f",mode.get(atmId*3)));
                sb.append(String.format(" %5.5f",mode.get(atmId*3+1)));
                sb.append(String.format(" %5.5f",mode.get(atmId*3+2)));
                sb.append(System.getProperty("line.separator"));
            }
        }
        IOtools.writeTXTAppend(outFileVibModes,sb.toString(),false);
        if (verbosity > 1)
        {
            System.out.println(" Vibrational mode " + selectedVibModes 
                + " written to '" + outFileVibModes + "'.");
        }
    }
    */


//------------------------------------------------------------------------------

    /**
     * Returns a string summarising the results of the evaluation 
     * @return the summary as a string
     */

    public String getResultsAsString()
    {
        String str = "File:" + inFile + " Steps:" + numSteps + " NormalTerm:"
                        + normalTerminated;
        if (!normalTerminated)
        {
        	//TODO
        	/*
            str = str + " Error:";
            if (errorIsDecoded)
                str = str + actualEM.getName();
            else
                str = str + "Not Known";
                */
        }
        else
        {
            str = str + "\n " + analysisResultLog;
        }

        return str;
    }

//------------------------------------------------------------------------------

}
