package autocompchem.chemsoftware.xtb;

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

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftOutputHandler;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reader for XTB output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of XTB. The rest of the functionality is in the superclass
 * {@link ChemSoftOutputHandler}.
 * 
 * @author Marco Foscato
 */
public class XTBOutputHandler extends ChemSoftOutputHandler
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
                    Collections.unmodifiableSet(new HashSet<TaskID>(
                                    Arrays.asList(TaskID.ANALYSEXTBOUTPUT)));
    
//-----------------------------------------------------------------------------
    
    /**
     * Method that parses the given log file from XTB and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    protected void readLogFile(File file) throws Exception
    {	
    	// Extract the pathname of the log file: we'll use it to find output 
    	// that is printed on other files
		String path = "";
		if (file.getParent() != null)
		{
        	path = file.getParent() + System.getProperty(
        			"file.separator");
		}
    	
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            NamedDataCollector stepData = new NamedDataCollector();
            ListOfIntegers stepScfConvSteps = new ListOfIntegers();
            ListOfDoubles stepScfConvEnergies = new ListOfDoubles();
            AtomContainerSet stepGeoms = new AtomContainerSet();
            
            boolean foundOptTrajectory = false;
            
        	int stepInitLineNum = 0;
        	int stepEndLineNum = Integer.MAX_VALUE;
            int stepId = 0;
            boolean first = true;
            int lineNum = -1;
            while ((line = buffRead.readLine()) != null)
            {
            	lineNum++;
            	
            	if (line.matches(".*" + XTBConstants.LOGJOBSTEPSTART+ ".*"))
            	{
            		if (first)
            		{
            			first = false;
            		} else {
            			stepEndLineNum = lineNum-1;
            			
            			if (!foundOptTrajectory)
            			{
                    		File xyzOpt = new File(path + "xtbopt.log");
                    		if (xyzOpt.exists())
                    		{
                    			ArrayList<IAtomContainer> mols = IOtools.readXYZ(
                    				xyzOpt.getAbsolutePath());
                    		
                    			for (IAtomContainer mol : mols)
                    			{
                    				stepGeoms.addAtomContainer(mol);
                    			}
                            }
            			}
            			
            			storeDataOfOneStep(stepId, 
            					stepData, stepInitLineNum, 
            					stepEndLineNum, stepScfConvSteps, 
            					stepScfConvEnergies, stepGeoms);
            			
            			// ...clear local storage...
            			stepData = new NamedDataCollector();
            			stepScfConvSteps = new ListOfIntegers();
                        stepScfConvEnergies = new ListOfDoubles();
                        stepGeoms = new AtomContainerSet();
                        
                    	//...and move on to next step.
            			stepId++;
            			stepInitLineNum = lineNum;
            		}
            	}
            	
            	if (line.matches(".*" + XTBConstants.LOGNORMALTERM+ ".*"))
            	{
            		normalTerminated = true;
            	}
            	
            	if (line.matches(".*" + XTBConstants.LOGFINALSPENERGY+ ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		stepScfConvEnergies.add(Double.parseDouble(p[3]));
            	}
            	
            	if (line.matches(".*" + XTBConstants.LOGSCFSUCCESS + ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		stepScfConvSteps.add(Integer.parseInt(p[5]));
            		stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATASCFCONVERGED,true));
            	}
            	
            	if (line.matches(".*" + 
            			XTBConstants.LOGGEOMOPTCONVERGED+ ".*"))
            	{
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBGEOMOPTCONVERGED,true));
            	}

            	if (line.matches(".*" + XTBConstants.LOGVIBFREQ+ ".*"))
            	{
            		ListOfDoubles list = new ListOfDoubles();
            		int skipped = 0;
            		while ((line = buffRead.readLine()) != null)
                    {
                    	lineNum++;
                    	skipped++;
                    	if (skipped<3)
                    	{
                    		continue;
                    	}
                    	if (line.contains("reduced masses"))
                    	{
                    		break;
                    	}
                		String[] p = line.trim().split("\\s+");
                		if (p.length < 3)
                		{
                			Terminator.withMsgAndStatus("ERROR! Cannot "
                					+ "read vibrationsl frequencies from line '" 
                					+ line + "'.",-1);
                		}
                		for (int i=2; i<p.length; i++)
                		{
                			list.add(Double.parseDouble(p[i]));
                		}
                    }
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATAVIBFREQ,list));
            	}
            	
            	if (line.matches(".*" + XTBConstants.LOGGIBBSFREEENERGY+ ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY,
        					Double.parseDouble(p[4])));
            	}
            	
            	
            	/*
                if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_ELECTR
                        + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[3]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_S_ELECTR,
                            val));
                }

                if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_VIB
                    + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[3]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_S_VIB,
                            val));
                }

                if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_TRANS
                    + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[3]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_S_TRANS,
                            val));
                }

                if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_S_ROT
                    + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[3]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_S_ROT,
                            val));
                }

                if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_H
                    + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[3]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_H,
                            val));
                }

                if (line.matches(".*" + XTBConstants.LOGTHERMOCHEM_ZPE
                    + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[4]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_ZPE,
                            val));
                }

                if (line.matches(".*" 
                		+ XTBConstants.LOGTHERMOCHEM_UCORR_VIB + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[4]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_VIB,
                            val));
                }

                if (line.matches(".*" 
                		+ XTBConstants.LOGTHERMOCHEM_UCORR_ROT + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[4]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_ROT,
                            val));
                }

                if (line.matches(".*" 
                		+ XTBConstants.LOGTHERMOCHEM_UCORR_TRANS + ".*"))
                {
                    String[] p = line.trim().split("\\s+");
                    Double val = Double.parseDouble(p[4]);
                    stepData.putNamedData(new NamedData(
                            ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_TRANS,
                            val));
                }
            	*/
            	
            	
            	if (line.matches(".*" + XTBConstants.LOGOPTTRJFILENAME + ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		String optGeomFileName = p[5];
            		optGeomFileName = optGeomFileName.replace("\'","");
            		optGeomFileName = optGeomFileName.replace("\'","");
            		
            		File xyzOpt = new File(path + optGeomFileName);
            		if (!xyzOpt.exists())
            		{
            			System.out.println("WARNING! Found redirection"
                					+ " to additional output file, " 
                					+ "but '"
                					+ xyzOpt.getAbsolutePath() + "' could "
                					+ "be found! " + System.getProperty(
                							"line.separator") 
                					+ "I cannot find geometries for step "
							+  (stepId+1) + "!");
            		} else {
            			ArrayList<IAtomContainer> mols = IOtools.readXYZ(
            				xyzOpt.getAbsolutePath());
            		
            			for (IAtomContainer mol : mols)
            			{
            				stepGeoms.addAtomContainer(mol);
            			}
                    }
            		foundOptTrajectory = true;
            	}
        	
            	// There is plenty of other data in the XTB log file. 
            	// So, this list of parsed data will grow as needed...
            	// Here is a template of code to be added to parse some data
            	
            	/*

            	if (line.matches(".*" + XTBConstants.____+ ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		//TODO: write code that parses data
            		 
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.____,
        					__data__));
            	}
            	 */
            }
            
            if (!foundOptTrajectory)
			{
        		File xyzOpt = new File(path + "xtbopt.log");
        		if (xyzOpt.exists())
        		{
        			ArrayList<IAtomContainer> mols = IOtools.readXYZ(
        				xyzOpt.getAbsolutePath());
        		
        			for (IAtomContainer mol : mols)
        			{
        				stepGeoms.addAtomContainer(mol);
        			}
                }
			}
            
            //TOTO read vib modes from g98.out (a.k.a. the fake output)
            /*
    	    stepData.putNamedData(new NamedData(
					ChemSoftConstants.JOBDATAVIBMODES,nms));
    	    */
            
    		// Store data of last job, which ended with the end of the file
    		stepEndLineNum = lineNum-1;
    		storeDataOfOneStep(stepId, stepData, stepInitLineNum, 
    				stepEndLineNum, stepScfConvSteps, stepScfConvEnergies, 
    				stepGeoms);
			
        } catch (FileNotFoundException fnf) {
        	Terminator.withMsgAndStatus("ERROR! File Not Found: " 
        			+ file.getAbsolutePath(),-1);
        } catch (IOException ioex) {
        	Terminator.withMsgAndStatus("ERROR! While reading file '" 
        			+ file.getAbsolutePath() + "'. Details: "
        			+ ioex.getMessage(),-1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
                System.err.println(ioex2.getMessage());
                System.exit(-1);
            }
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void storeDataOfOneStep(
    		int stepId, NamedDataCollector stepData, 
    		int stepInitLineNum, int stepEndLineNum, 
    		ListOfIntegers stepScfConvSteps, ListOfDoubles stepScfConvEnergies,
    		AtomContainerSet stepGeoms) throws CloneNotSupportedException
    {
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAINITLINE,
				stepInitLineNum));
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATAENDLINE,
				stepEndLineNum));
		stepData.putNamedData(new NamedData(
				ChemSoftConstants.JOBDATASCFENERGIES,
				stepScfConvEnergies));
		if (!stepScfConvSteps.isEmpty())
		{
    		stepData.putNamedData(new NamedData(
    				ChemSoftConstants.JOBDATASCFSTEPS,
    				stepScfConvSteps));
		}
		if (!stepGeoms.isEmpty())
		{
			stepData.putNamedData(new NamedData(
					ChemSoftConstants.JOBDATAGEOMETRIES,
					stepGeoms.clone()));
		}
		
		stepsData.put(stepId,stepData.clone());
    }

//-----------------------------------------------------------------------------
    
}