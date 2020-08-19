package autocompchem.chemsoftware.orca;

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
import autocompchem.io.IOtools;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reader for Orca output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of Orca. The rest of the functionality is in the superclass
 * {@link ChemSoftOutputHandler}.
 * 
 * @author Marco Foscato
 */
public class OrcaOutputHandler extends ChemSoftOutputHandler
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
                    Collections.unmodifiableSet(new HashSet<TaskID>(
                                    Arrays.asList(TaskID.ANALYSEORCAOUTPUT)));
    
//-----------------------------------------------------------------------------
    
    /**
     * Method that parses the given log file from Orca and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    protected void readLogFile(File file) throws Exception
    {	
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            NamedDataCollector stepData = new NamedDataCollector();
            // NB: yes, lists of STrings: we'll convert later. This
            // to enjoy the use of NamedData, which cannot distinguish
            // between an ArrayList<Double> and ArrayList<String>
            ListOfIntegers stepScfConvSteps = new ListOfIntegers();
            ListOfDoubles stepScfConvEnergies = new ListOfDoubles();
            AtomContainerSet stepGeoms = new AtomContainerSet();
        	int stepInitLineNum = 0;
        	int stepEndLineNum = Integer.MAX_VALUE;
            int stepId = 0;
            boolean first = true;
            int lineNum = -1;
            while ((line = buffRead.readLine()) != null)
            {
            	lineNum++;
            	
            	/*
            	//TODO del
        		System.out.println(" ");
        		System.out.println("LINE: "+line);
        		*/
            	
            	if (line.matches(".*" + OrcaConstants.LOGJOBSTEPSTART+ ".*"))
            	{
            		if (first)
            		{
            			first = false;
            		} else {
            			stepEndLineNum = lineNum-1;
            			stepData.putNamedData(new NamedData(
            					ChemSoftConstants.JOBDATAINITLINE,
            					stepInitLineNum));
            			stepData.putNamedData(new NamedData(
            					ChemSoftConstants.JOBDATAENDLINE,
            					stepEndLineNum));
                		stepData.putNamedData(new NamedData(
            					ChemSoftConstants.JOBDATASCFENERGIES,
            					stepScfConvEnergies.clone()));
                		stepData.putNamedData(new NamedData(
            					ChemSoftConstants.JOBDATASCFSTEPS,
            					stepScfConvSteps.clone()));
                		stepData.putNamedData(new NamedData(
            					ChemSoftConstants.JOBDATAGEOMETRIES,
            					stepGeoms.clone()));
                		
                		//TODO del
                		/*
            			System.out.println("STEP: "+stepId);
            			for (String k: stepData.getAllNamedData().keySet())
            			{
            				NamedData d = stepData.getNamedData(k);
            				System.out.println("-> "+d);
            			}
            			IOtools.pause();
            			*/
            			
            			// All done for this step. So, store data
            			stepsData.put(stepId,stepData.clone());
            			
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
            	
            	if (line.matches(".*" + OrcaConstants.LOGNORMALTERM+ ".*"))
            	{
            		normalTerminated = true;
            	}
            	
            	if (line.matches(".*" + OrcaConstants.LOGFINALSPENERGY+ ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		stepScfConvEnergies.add(Double.parseDouble(p[4]));
            	}
            	
            	if (line.matches(".*" + OrcaConstants.LOGSCFSUCCESS + ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		stepScfConvSteps.add(Integer.parseInt(p[4]));
            		stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATASCFCONVERGED,true));
            	}
            	
            	if (line.matches(".*" + 
            			OrcaConstants.JOBLOGCURRENTGEOMETRY+ ".*"))
            	{
            		IAtomContainer mol = new AtomContainer();
            		int skipped = 0;
            		while ((line = buffRead.readLine()) != null)
                    {
                    	lineNum++;
                    	skipped++;
                    	if (skipped<2)
                    	{
                    		continue;
                    	}
                    	if (line.trim().equals(""))
                    	{
                    		break;
                    	} else {
                    		String[] p = line.trim().split("\\s+");
                    		if (p.length < 4)
                    		{
                    			Terminator.withMsgAndStatus("ERROR! Cannot "
                    					+ "read coordinates from line '" 
                    					+ line + "'.",-1);
                    		}
                    		String sym = p[0];
                    		Point3d p3d = new Point3d(Double.parseDouble(p[1]),
                    				Double.parseDouble(p[2]),
                    				Double.parseDouble(p[3]));
                    		IAtom atm = new Atom(sym,p3d);
                    		mol.addAtom(atm);
                    	}
                    }
            		stepGeoms.addAtomContainer(mol);
            	}
            	
            	if (line.matches(".*" + 
            			OrcaConstants.LOGGEOMOPTCONVERGED+ ".*"))
            	{
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBGEOMOPTCONVERGED,true));
            	}

            	if (line.matches(".*" + OrcaConstants.LOGVIBFREQ+ ".*"))
            	{
            		ListOfDoubles list = new ListOfDoubles();
            		int skipped = 0;
            		int emptyLinesCount = 0;
            		while ((line = buffRead.readLine()) != null)
                    {
                    	lineNum++;
                    	skipped++;
                    	if (skipped<4)
                    	{
                    		continue;
                    	}
                    	if (line.trim().equals(""))
                    	{
                    		emptyLinesCount++;
                    		if (emptyLinesCount>2)
                    		{
                    			break;
                    		}
                    	} else {
                    		String[] p = line.trim().split("\\s+");
                    		if (p.length < 3)
                    		{
                    			Terminator.withMsgAndStatus("ERROR! Cannot "
                    					+ "read coordinates from line '" 
                    					+ line + "'.",-1);
                    		}
                    		list.add(Double.parseDouble(p[1]));
                    	}
                    }
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATAVIBFREQ,list));
            	}
            	
            	if (line.matches(".*" + OrcaConstants.LOGVIBMODES + ".*"))
            	{
            		NormalModeSet nms = new NormalModeSet();
            		
            		//WARNING! we can read up to 6 modes per block
            		int[] currModes = new int[6];
            		
            		int skipped = 0;
            		while ((line = buffRead.readLine()) != null)
                    {
                    	lineNum++;
                    	skipped++;
                    	if (skipped<7)
                    	{
                    		continue;
                    	}
                    	if (line.trim().equals(""))
                    	{
                    		// Initial empty lines were skipped, so they do not
                    		// count
                    		break;
                    	} else if (line.startsWith("           ")) {
                    		//This lines contain only the indexes of the modes
                    		String[] p = line.trim().split("\\s+");
                    		if (p.length < 2)
                    		{
                    			Terminator.withMsgAndStatus("ERROR! Cannot "
                    					+ "read normal modes indexes from line"
                    					+ " '" + line + "'.",-1);
                    		}
                    		currModes = new int[6]; //Cleanup previous numbers
                    		for (int i=0; i<p.length; i++)
                    		{
                    			currModes[i] = Integer.parseInt(p[i]);
                    		}
                    	} else {
                    		// This lines contain the actual components
                    		String[] p = line.trim().split("\\s+");
                    		int atmId = Integer.parseInt(p[0])/3;
                    		int compId =  (Integer.parseInt(p[0]) % 3);
                        	for (int k=1; k<p.length; k++)
                        	{
                        		nms.setComponent(currModes[k-1], atmId, 
                    					compId, Double.parseDouble(p[k]));
                        	}
                    	}
                    }
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATAVIBMODES,nms));
            	}
   
            	if (line.matches(".*" + OrcaConstants.LOGGIBBSFREEENERGY+ ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.JOBDATAGIBBSFREEENERGY,
        					Double.parseDouble(p[5])));
            	}
            	
                if (line.matches(".*" + OrcaConstants.LOGTHERMOCHEM_S_ELECTR
                        + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[3]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_S_ELECTR,
                                val));
                    }

                    if (line.matches(".*" + OrcaConstants.LOGTHERMOCHEM_S_VIB
                        + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[3]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_S_VIB,
                                val));
                    }

                    if (line.matches(".*" + OrcaConstants.LOGTHERMOCHEM_S_TRANS
                        + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[3]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_S_TRANS,
                                val));
                    }

                    if (line.matches(".*" + OrcaConstants.LOGTHERMOCHEM_S_ROT
                        + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[3]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_S_ROT,
                                val));
                    }

                    if (line.matches(".*" + OrcaConstants.LOGTHERMOCHEM_H
                        + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[3]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_H,
                                val));
                    }

                    if (line.matches(".*" + OrcaConstants.LOGTHERMOCHEM_ZPE
                        + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[4]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_ZPE,
                                val));
                    }

                    if (line.matches(".*" 
                    		+ OrcaConstants.LOGTHERMOCHEM_UCORR_VIB + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[4]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_VIB,
                                val));
                    }

                    if (line.matches(".*" 
                    		+ OrcaConstants.LOGTHERMOCHEM_UCORR_ROT + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[4]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_ROT,
                                val));
                    }

                    if (line.matches(".*" 
                    		+ OrcaConstants.LOGTHERMOCHEM_UCORR_TRANS + ".*"))
                    {
                        String[] p = line.trim().split("\\s+");
                        Double val = Double.parseDouble(p[4]);
                        stepData.putNamedData(new NamedData(
                                ChemSoftConstants.JOBDATTHERMOCHEM_UCORR_TRANS,
                                val));
                    }
            	
            	// There is plenty of other data in the Orca log file. 
            	// So, this list of parsed data will grow as neede...
            	// Here is a template of code to be added to parse some data
            	
            	/*

            	if (line.matches(".*" + OrcaConstants.____+ ".*"))
            	{
            		String[] p = line.trim().split("\\s+");
            		//TODO: write code that parses data
            		 
            	    stepData.putNamedData(new NamedData(
        					ChemSoftConstants.____,
        					__data__));
            	}
            	 */
            }
            
            // We still have to store the data of the last step
    		//Store data of last job
    		stepEndLineNum = lineNum-1;
    		stepData.putNamedData(new NamedData(
    				ChemSoftConstants.JOBDATAINITLINE,
    				stepInitLineNum));
    		stepData.putNamedData(new NamedData(
    				ChemSoftConstants.JOBDATAENDLINE,
    				stepEndLineNum));
    		stepData.putNamedData(new NamedData(
    				ChemSoftConstants.JOBDATASCFENERGIES,
    				stepScfConvEnergies));
    		stepData.putNamedData(new NamedData(
    				ChemSoftConstants.JOBDATASCFSTEPS,
    				stepScfConvSteps));
    		stepData.putNamedData(new NamedData(
					ChemSoftConstants.JOBDATAGEOMETRIES,
					stepGeoms.clone()));
    		
    		
    		// All done for this step. So, store data
    		stepsData.put(stepId,stepData.clone());
    		
    		
    		/*
    		//TODO del
			System.out.println("FINAL DATA OF THE ABOVE STEP:");
			for (String k: stepData.getAllNamedData().keySet())
			{
				NamedData d = stepData.getNamedData(k);
				System.out.println("-> "+d);
			}
			IOtools.pause();
			*/
			
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
    
}
