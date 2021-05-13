package autocompchem.chemsoftware;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.AnalysisTask.AnalysisKind;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Core components of any reader and analyser of computational chemistry 
 * software output files.
 * 
 * @author Marco Foscato
 */

public abstract class ChemSoftOutputHandler extends Worker
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
     * List of analysis to perform on the overall job. This might be
     * something like getting the overall last geometry,
     */
    private ArrayList<AnalysisTask> analysisGlobalTasks = 
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
    
    //TODO del
    /**
     * Flag controlling whether to check consistency between connectivity and 
     * interatomic distances,
     */
    private boolean connectivityCheckBL = false;
    
    /**
     * Tolerance on the variation of interatomic distance of bonded atom pairs.
     * With a tolerance equal to <i>t</i> and a reference interatomic distance 
     * <i>d</i> only an absolute deviation larger than <i>abs(t*d)</i> will be 
     * seen as incompatible. Default <i>t = 0.05</i>.
     */
    private double connectivityCheckTol = 0.05;
    
    private static String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters given to the 
     * constructor.
     */

    @Override
    public void initialize()
    {
    	//TODO: should be clean up previous initializations? I guess so.
    	//we could change the params and re-initialize this class.
    	
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

        if (params.contains(ChemSoftConstants.PARPRINTLASTGEOMEACH))
        {
        	String s = params.getParameter(
        			ChemSoftConstants.PARPRINTLASTGEOMEACH).getValueAsString();
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
            analysisGlobalTasks.add(a);
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
            String val = params.getParameter(
            		ChemSoftConstants.PARTEMPLATECONNECTIVITY)
            		.getValueAsString();
            String[] p = val.split("\\s+");
            String fileWithTplt = p[0];
            FileUtils.foundAndPermissions(fileWithTplt,true,false,false);
            //NB: assumption: only one atom container as template.
            this.connectivityTemplate = IOtools.readSDF(fileWithTplt).get(0);
            
            ParameterStorage ps = new ParameterStorage();
            boolean checkBLvsCT = true;
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
	                	double v = Double.parseDouble(w);
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
	                ps.setParameter(new Parameter(
	                		ChemSoftConstants.PARBONDLENGTHTOLETANCE,w));
	                checkBLvsCT = true;
                }
            }
            if (checkBLvsCT)
            {
            	// We have to ensure that last geometry is extracted
            	boolean addLastGeom = true;
            	for (AnalysisTask at : analysisGlobalTasks)
            	{
            		if (at.getKind() == AnalysisKind.LASTGEOMETRY)
            		{
            			addLastGeom = false;
            		}
            	}
            	if (addLastGeom)
            	{
            		analysisGlobalTasks.add(new AnalysisTask(
            				AnalysisKind.LASTGEOMETRY));
            	}
            	
                AnalysisTask a = new AnalysisTask(
                		AnalysisKind.BLVSCONNECTIVITY,ps);
                // WARNING: we assume that this analysis task as added AFTER
                // the extraction of the last geometry!!!
                analysisGlobalTasks.add(a);
                // NB: here we could have the possibility to decide which 
                // geometry to analyse: all or just the last one.
            }
        }

        if (params.contains(ChemSoftConstants.PARGETENERGY))
        {
            String s = params.getParameter(ChemSoftConstants.PARGETENERGY)
            		.getValueAsString();
            String[] p = s.split("\\s+");
            AnalysisTask a = new AnalysisTask(AnalysisKind.SCFENERGY);
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
        ListOfDoubles convergedScfEnergies = new ListOfDoubles();
        ListOfDoubles qhGibbsEnergies = new ListOfDoubles();
        
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
        	
        	//This contains all the data parsed from the file/s
        	NamedDataCollector stepData = stepsData.get(stepId);
        	resultsString.append(NL + "Step " + stepId + ":" + NL);
        	
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
		        				System.out.println("No geometries found in "
		        						+ "step " + stepId + ".");
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
	        			
	        			AtomContainerSet acs = (AtomContainerSet) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAGEOMETRIES).getValue();
	        			
	        			if (acs.getAtomContainerCount()==0)
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("Empty list of geometry in "
		        						+ "step " + stepId + ".");
		        			}
	        				break;
	        			}

	        			if (verbosity > 1)
	        			{
	        				System.out.println("Writing all geometries of "
	        						+ "step " + stepId + " to file '" 
	        						+ outFileName+"'");
	        			}
	        			
	        			geomsToExpose.add(acs);
	        			
	        			IOtools.writeAtomContainerSetToFile(outFileName, acs,
	        					format,true);
	        			
	        			resultsString.append("-> #geometries ").append(
	        					acs.getAtomContainerCount());
	        			resultsString.append(NL);
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
	        			
	        			AtomContainerSet acs = (AtomContainerSet) 
	        					stepData.getNamedData(ChemSoftConstants
	        							.JOBDATAGEOMETRIES).getValue();
	        			
	        			if (acs.getAtomContainerCount() == 0)
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("Empty list of geometries "
		        						+ "in step " + stepId + ".");
		        			}
	        				break;
	        			}
	        			
	        			if (verbosity > 1)
	        			{
	        				System.out.println("Writing last geometry of "
	        						+ "step " + stepId + " to file '" 
	        						+ outFileName+"'");
	        			}
	        			
	        			IAtomContainer mol = acs.getAtomContainer(
	        					acs.getAtomContainerCount()-1);
	        			
	        			lastGeomToExpose = mol;
	        			
	        			IOtools.writeAtomContainerToFile(outFileName, mol,
	        					format,true);

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
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No frequencies found in "
		        						+ "step " + stepId + ".");
		        			}
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
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No SCF converged energy "
		        						+ "found in step " + stepId + ".");
		        			}
	        				break;
	        			}
	        			ListOfDoubles l = (ListOfDoubles) stepData.getNamedData(
	        					ChemSoftConstants.JOBDATASCFENERGIES).getValue();
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
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No frequencies found in "
		        						+ "step " + stepId 
		        						+ ". Skipping QHThermochemistry.");
		        			}
	        				break;
	        			}
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY))
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No Gibbs free energy found"
		        						+ " in step " + stepId 
		        						+ ". Skipping QHThermochemistry.");
		        			}
	        				break;
	        			}
	        			
	        			Double gibbsFreeEnergy = (Double) stepData.getNamedData(
	        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY)
	        					.getValue();
	        			
	        			Double temp = 298.15;
	        			@SuppressWarnings("unchecked")
						Double vibS = CompChemComputer.vibrationalEntropyCorr(
	        					(ArrayList<Double>) stepData.getNamedData(
	    	        					ChemSoftConstants.JOBDATAVIBFREQ)
	        					.getValue(), 
	        					temp);

	        			Double qhThrsh = 0.0;
	        			qhThrsh = changeIfParameterIsFound(qhThrsh,
	        					ChemSoftConstants.QHARMTHRSLD,atParams);
	        			Double imThrsh = 0.0;
	        			imThrsh  = changeIfParameterIsFound(imThrsh,
	        					ChemSoftConstants.QHARMTOREAL,atParams);
	        			Double ignThrsh = 0.1;
	        			ignThrsh = changeIfParameterIsFound(ignThrsh,
	        					ChemSoftConstants.QHARMIGNORE,atParams);
	        			@SuppressWarnings("unchecked")
						Double qhVibS = CompChemComputer.vibrationalEntropyCorr(
	        					(ArrayList<Double>) stepData.getNamedData(
	    	        					ChemSoftConstants.JOBDATAVIBFREQ)
	        					.getValue(), 
	        					temp, qhThrsh, imThrsh, ignThrsh, verbosity-1);
	        			
	        			gibbsFreeEnergy = gibbsFreeEnergy - vibS + qhVibS;
	        			
	        			resultsString.append("-> Quasi-Harm. corrected "
	        					+ "Gibbs free energy ").append(gibbsFreeEnergy);
	        			resultsString.append(" (").append(qhThrsh).append("; ");
	        			resultsString.append(imThrsh).append("; ");
	        			resultsString.append(ignThrsh);
	        			resultsString.append(")").append(NL);
	        			
	        			qhGibbsEnergies.add(gibbsFreeEnergy);
	        			break;
	        		}
	        		
	        		case VIBMODE:
	        		{
	        			if (!stepData.contains(
	        					ChemSoftConstants.JOBDATAVIBMODES))
	        			{
	        				if (verbosity > 1)
		        			{
		        				System.out.println("No normal modes found in "
		        						+ "step " + stepId + ".");
		        			}
	        				break;
	        			}
	        			NormalModeSet nms = 
	        					(NormalModeSet) stepData.getNamedData(
	        					ChemSoftConstants.JOBDATAVIBMODES).getValue();
	        			String outFile = outFileRootName + "_nm.xyz";
	        			outFile = changeIfParameterIsFound(outFile, 
	        					ChemSoftConstants.GENERALFILENAME, atParams);
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
	        			if (verbosity > 1)
	        			{ 
	        				if (all)
	        				{
	        					System.out.print("Esporting all normal modes");
	        				} else {
	        					System.out.print("Esporting normal modes ");
	        				}
	        			}
	        			for (Integer id : idxs)
	        			{
	        				if (verbosity>1 && !all)
		        			{ 
	        					System.out.print(id + "  ");
		        			}
	        				sb.append("# Normal mode #").append(id).append(NL);
	        				sb.append(nms.get(id).toLines());
	        			}
	        			if (verbosity > 1)
	        			{ 
        					System.out.println(" to file '" + outFile + "'");
	        			}
	        			IOtools.writeTXTAppend(outFile, sb.toString(), true);
	        			
	        			resultsString.append("-> #vibrational Modes ").append(
	        					nms.size());
	        			resultsString.append(NL);
	        			break;
	        		}
        		}
        		
        		//TODO: more? ...just add it here
        	}
        }
        
     	
        if (verbosity > 0)
        {
        	System.out.println(" ");
        	System.out.println("Summary of the results:");
            System.out.println(resultsString.toString());
        }
        
        // Prepare collector of final analysis results
    	StringBuilder finalResultsString = new StringBuilder();
    	
        // WARNING: the LASTGEOMETRY analysis is expected to run before
        // the BLVSCONNECTIVITY
        
        // Analyse one on the collection of steps takes as a whole
        for (AnalysisTask at : analysisGlobalTasks)
        {
        	ParameterStorage atParams = at.getParams();
    		switch (at.getKind())
    		{
				case LASTGEOMETRY:
				{
					String format = "XYZ";
					format = changeIfParameterIsFound(format,
							ChemSoftConstants.GENERALFORMAT,atParams);
					String outFileName = outFileRootName+"_lastGeom."
							+ format.toLowerCase();
					outFileName = changeIfParameterIsFound(outFileName,
							ChemSoftConstants.GENERALFILENAME,atParams);
					
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
						if (verbosity > 1)
		    			{
		    				System.out.println("WARNING! Empty list of "
		    						+ "geometries from this job. "
		    						+ "I cannot find the last "
		    						+ "geometry ");
		    			}
						break;
					}
					
					if (verbosity > 1)
					{
						System.out.println("Writing overal last geometry to "
								+ "file '" + outFileName+"'");
					}
					IOtools.writeAtomContainerToFile(outFileName,lastGeom,
        					format,true);
					lastGeomToExpose = lastGeom;
					break;
				}
				
				case BLVSCONNECTIVITY:
				{
					// WARNING: lastGeomToExpose is not null because this task
					// should come (if at all) after the extraction of the last 
					// geometry
					double tolerance = 0.05;
					tolerance = changeIfParameterIsFound(tolerance,
							ChemSoftConstants.PARBONDLENGTHTOLETANCE, atParams);
					String result = " compatible ";
					StringBuffer log = new StringBuffer();
					if (!ConnectivityUtils.compareBondDistancesWithReference(
							lastGeomToExpose, connectivityTemplate, tolerance, 
							0, log))
					{
						result = " NOT compatible! " + log.toString();
					}
					finalResultsString.append("Bond lengths vs. connectivity:")
						.append(result).append(NL);
					break;
				}
    		}
        }
        
     	
        if (verbosity > 0)
        {
        	System.out.println(" ");
        	System.out.println("Summary of final results:");
            System.out.println(finalResultsString.toString());
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @param original the original value
     * @param key the reference name of the parameter to try to find
     * @return either the original value, or, if the given parameter is found, 
     * the value of that parameter
     */
    private double changeIfParameterIsFound(double original, String key,
    		ParameterStorage ps) 
    {
    	String iStr = Double.toString(original);
    	iStr = changeIfParameterIsFound(iStr, key, ps);
    	return Double.parseDouble(iStr);
    }

//------------------------------------------------------------------------------
    
    /**
     * @param original the original value
     * @param key the reference name of the parameter to try to find
     * @return either the original value, or, if the given parameter is found, 
     * the value of that parameter
     */
    private ListOfIntegers changeIfParameterIsFound(ListOfIntegers original, 
    		String key, ParameterStorage ps) 
    {
		if (ps.contains(key))
		{
			return (ListOfIntegers) ps.getParameter(key).getValue();
		}
    	return original;
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
    
    protected abstract void readLogFile(File file) throws Exception;

//------------------------------------------------------------------------------

}
