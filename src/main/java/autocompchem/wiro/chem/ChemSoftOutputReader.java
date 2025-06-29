package autocompchem.wiro.chem;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Job;
import autocompchem.run.JobFactory;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.WIROConstants;
import autocompchem.wiro.chem.AnalysisTask.AnalysisKind;

/**
 * Core components of any reader and analyzer of computational chemistry 
 * software's output files.
 * 
 * @author Marco Foscato
 */

public abstract class ChemSoftOutputReader extends OutputReader
{
    
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
     * The identifier of the molecule/model to focus on, ignoring the rest.
     */
    protected String selectedMolID;
    
    /**
     * Flag requesting any alteration that is specific to a single model in a
     * pool of models (e.g., altering filenames to contain model identifier)
     */
    protected boolean modelSpecific;
    

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

	@Override
    public void initialize()
    {
    	super.initialize();
    	
    	if (params.contains(ChemSoftConstants.PARMODELID))
        {
    		selectedMolID = params.getParameter(
            		ChemSoftConstants.PARMODELID).getValueAsString();
        }
    	
    	if (params.contains(ChemSoftConstants.PARMODELSPECIFIC))
        {
    		modelSpecific = true;
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
            AnalysisTask a = new AnalysisTask(AnalysisKind.SAVELASTGEOMETRY, ps);
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
            AnalysisTask a = new AnalysisTask(AnalysisKind.SAVELASTGEOMETRY, ps);
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
            AnalysisTask a = new AnalysisTask(AnalysisKind.SAVEALLGEOM, ps);
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
            AnalysisTask a = new AnalysisTask(AnalysisKind.SAVEVIBMODE,ps);
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
        		if (at.getKind() == AnalysisKind.SAVELASTGEOMETRY)
        		{
        			addLastGeom = false;
        		}
        	}
        	if (addLastGeom)
        	{
        		analysisGlobalTasks.add(new AnalysisTask(
        				AnalysisKind.SAVELASTGEOMETRY));
        	}
        	
            AnalysisTask a = new AnalysisTask(
            		AnalysisKind.BLVSCONNECTIVITY,ps);
            // WARNING: we assume that this analysis task as added AFTER
            // the extraction of the last geometry!!!
            analysisGlobalTasks.add(a);
            // NB: here we could have the possibility to decide which 
            // geometry to analyse: all or just the last one.
        }

        if (params.contains(ChemSoftConstants.PARGETFREEENERGY))
        {
            String s = params.getParameter(ChemSoftConstants.PARGETFREEENERGY)
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
        
        if (params.contains(ChemSoftConstants.PARGETENERGY))
        {
        	AnalysisTask a = new AnalysisTask(AnalysisKind.SCFENERGY);
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
     * Reads the output files and parses all data that can be found
     * on each step/job.
     */

	@Override
    protected void analyzeFiles()
    {
		// Consider possibility to spin out multiple readers for output data
		// that contains multiple models
		List<File> subModelsOutFiles = getSubModelOutputFiles();
		if (subModelsOutFiles.size()>0)
		{
			dealWithMultiModelContainers(subModelsOutFiles);
			return;
		}
		
    	super.analyzeFiles();
    	
        // Prepare collector of global analysis results
    	StringBuilder resultsString = new StringBuilder();
        AtomContainerSet geomsToExpose = new AtomContainerSet();
        IAtomContainer lastGeomToExpose = null;
        
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

        // Apply connectivity template, if given
		if (useTemplateConnectivity)
		{
			for (Integer stepId : stepsData.keySet())
			{
				NamedDataCollector stepData = stepsData.get(stepId);
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
	        		case SAVEALLGEOM:
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
	        			if (modelSpecific)
	        			{
	        				outFileName = FileUtils.getIdSpecPathName(
	        						outFileName, selectedMolID);
	        			}
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
	        		
	        		case SAVELASTGEOMETRY:
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
	        			if (modelSpecific)
	        			{
	        				outFileName = FileUtils.getIdSpecPathName(
	        						outFileName, selectedMolID);
	        			}
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
	        			
	        			IAtomContainer mol = getLastGeometryWithProperties(
	        					stepData);
	        			
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
	        			stepData.putNamedData(new NamedData(
	        					ChemSoftConstants.JOBDATACRITICALPOINTKIND, 
	        					kindOfCriticalPoint));
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
	        			stepData.putNamedData(new NamedData(
	        					ChemSoftConstants.JOBDATAFINALSCFENERGY, energy));
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
		        			
		        			stepData.putNamedData(new NamedData(
		        					ChemSoftConstants.JOBDATAQHGIBBSFREEENERGY, 
		        					gibbsFreeEnergy));
	        			}
	        			
	        			resultsString.append("-> ")
	        				.append(qhTitlePrefix)
	        				.append("Gibbs free energy ")
	        				.append(gibbsFreeEnergy)
	        				.append(qhTitleSuffix).append(NL);
	        			break;
	        		}
	        		
	        		case SAVEVIBMODE:
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
	        			if (modelSpecific)
	        			{
	        				outFileName = FileUtils.getIdSpecPathName(
	        						outFileName, selectedMolID);
	        			}
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
    	
        // WARNING: the SAVELASTGEOMETRY analysis is expected to run before
        // the BLVSCONNECTIVITY
        
        // Analyse the collection of steps takes as a whole
        for (AnalysisTask at : analysisGlobalTasks)
        {
        	ParameterStorage atParams = at.getParams();
    		switch (at.getKind())
    		{
				case SAVELASTGEOMETRY:
				{
					String format = "XYZ";
					format = changeIfParameterIsFound(format,
							ChemSoftConstants.GENERALFORMAT,atParams);
					String outFileName = outFileRootName+"_lastGeom."
							+ format.toLowerCase();
					outFileName = changeIfParameterIsFound(outFileName,
							ChemSoftConstants.GENERALFILENAME,atParams);
        			if (modelSpecific)
        			{
        				outFileName = FileUtils.getIdSpecPathName(
        						outFileName, selectedMolID);
        			}
					File outFile = new File(outFileName);
					
					IAtomContainer lastGeom = null;
					for (int stepId=0; stepId<numSteps; stepId++)
					{
						if (!stepsData.containsKey(stepId))
						{
							continue;
						}
						NamedDataCollector stepData = stepsData.get(stepId);
						lastGeom = getLastGeometryWithProperties(stepData);
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
        
        if (geomsToExpose.getAtomContainerCount()>0)
        {
	        exposeOutputData(new NamedData(ChemSoftConstants.JOBDATAGEOMETRIES,
	        		geomsToExpose));
        }
        
        if (!finalResultsString.toString().isBlank())
        {
        	logger.info(NL + "Summary of final results: " 
        			+ finalResultsString.toString());
        }
    }
	
//------------------------------------------------------------------------------
	
	/**
	 * Takes the last geometry from the data for a single step/job of 
	 * a comp.chem. software and returns it with relevant data saved as 
	 * proeprties. Relevant data can be the SCF or Gibbs free energy or the
	 * kind of critical point, if any of this is available in that step.
	 * @param stepData
	 * @return the decorated atom container.
	 */
	
	private IAtomContainer getLastGeometryWithProperties(
			NamedDataCollector stepData)
	{
		IAtomContainer geom = null;
		if (stepData.contains(
				ChemSoftConstants.JOBDATAGEOMETRIES))
		{
			AtomContainerSet acs = (AtomContainerSet) 
					stepData.getNamedData(ChemSoftConstants
							.JOBDATAGEOMETRIES).getValue();
			geom = acs.getAtomContainer(acs.getAtomContainerCount()-1);
			
			// Add properties specific to this geometry in this step
			if (stepData.contains(
					ChemSoftConstants.JOBDATAFINALSCFENERGY))
			{
				geom.setProperty("SCF_ENERGY", 
						stepData.getNamedData(
        					ChemSoftConstants.JOBDATAFINALSCFENERGY)
								.getValue());
			}
			if (stepData.contains(
					ChemSoftConstants.JOBDATACRITICALPOINTKIND))
			{
				geom.setProperty("KIND",
						stepData.getNamedData(
	        					ChemSoftConstants.JOBDATACRITICALPOINTKIND)
									.getValue());
			}
			if (stepData.contains(
					ChemSoftConstants.JOBDATAGIBBSFREEENERGY))
			{
				geom.setProperty("GIBBS_FREE_ENERGY",
						stepData.getNamedData(
	        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY)
									.getValue());
			}
			if (stepData.contains(
					ChemSoftConstants.JOBDATAQHGIBBSFREEENERGY))
			{
				geom.setProperty("QH_GIBBS_FREE_ENERGY",
						stepData.getNamedData(
	        					ChemSoftConstants.JOBDATAQHGIBBSFREEENERGY)
									.getValue());
			}
		}
		return geom;
	}

//------------------------------------------------------------------------------
	
	/**
	 * This is where we intercept the possibility of an output file to contain 
	 * multiple chemical models (e.g., multiple conformations, reaction species).
	 * By default we detect whether there are multiple models, which may be made
	 * software-dependent by overwriting {@link #getSubModelOutputFiles()},
	 * and if we see 
	 * @param subModelsOutFiles the unordered list of model-specific output 
	 * files/folders.
	 */
	protected void dealWithMultiModelContainers(List<File> subModelsOutFiles)
	{
		logger.info("Multi-model ouptut detected. Spinning multiple "
				+ "readers from '" + inFile + "'");
		for (File subModelOutFile : subModelsOutFiles)
		{
			String subModelID = subModelOutFile.getName();
			//NB: the list of files is not bound to follow any order 
			// so do not rely on the index of the item in the list.
			ParameterStorage subModPars = myJob.getParameters().clone();
			subModPars.setParameter(WIROConstants.PARJOBOUTPUTFILE, 
					subModelOutFile);
			subModPars.setParameter(ChemSoftConstants.PARMODELID, subModelID);
			subModPars.setParameter(ChemSoftConstants.PARMODELSPECIFIC);
			
			Job subModelJob = JobFactory.createTypedJob(myJob);
			subModelJob.setParameters(subModPars);
			subModelJob.run();

	        exposeOutputData(new NamedData(WIROConstants.JOBOUTPUTDATA + "_" 
	        		+ subModelID, subModelJob.exposedOutput));
		}
	}	
//------------------------------------------------------------------------------
	
	/**
	 * Finds the output files corresponding to submodels (i.e., models being 
	 * part of the same software output, for instance, multiple conformations 
	 * produced by the same run). Subclasses can override this method
	 * create the capability to spin out multiple readers, one per each of
	 * the submodels, which is then processed independently.
	 * @return always an empty list unless it is overwritten by subclasses, in 
	 * which case this documentation should be overwridden as well.
	 */
	protected List<File> getSubModelOutputFiles()
	{
		return new ArrayList<File>();
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
	
}
