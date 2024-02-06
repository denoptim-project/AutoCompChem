package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.ChemSoftOutputReader;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileFingerprint;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for NWChem output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of NWChem. The rest of the functionality is in the superclass
 * {@link ChemSoftOutputReader}.
 * 
 * @author Marco Foscato
 */
public class NWChemOutputReader extends ChemSoftOutputReader
{
	
//------------------------------------------------------------------------------

    @Override
  	public Set<Task> getCapabilities() {
  		return Collections.unmodifiableSet(new HashSet<Task>(
  				Arrays.asList(Task.make("analyseNWChemOutput"))));
  	}

//------------------------------------------------------------------------------

  	@Override
  	public Worker makeInstance(Job job) {
  		return new NWChemOutputReader();
  	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Method that parses the given log file from NWchem and collects all 
     * possible data in local fields.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    protected void readLogFile(LogReader buffRead) throws Exception
    {
        String line = null;
        NamedDataCollector stepData = new NamedDataCollector();
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
        	if (line.matches(".*" + NWChemConstants.OUTINITSTR+ ".*"))
        	{
        		if (first)
        		{
        			first = false;
        		} else {
        			stepEndLineNum = lineNum-1;
        			storeDataOfOneStep(stepId, 
        					stepData, stepInitLineNum, 
        					stepEndLineNum,
        					stepScfConvEnergies, stepGeoms);
        			
        			// ...clear local storage...
        			stepData = new NamedDataCollector();
                    stepScfConvEnergies = new ListOfDoubles();
                    stepGeoms = new AtomContainerSet();
                    
                	//...and move on to next step.
        			stepId++;
        			stepInitLineNum = lineNum;
        		}
        	}
        	
        	if (line.matches(".*" + NWChemConstants.OUTNORMALENDSTR+ ".*"))
        	{
        		normalTerminated = true;
        		// NB: the citation message is reported after a misleading line
        		// that states "NWChem Input Module", which is what we use to 
        		// identify the beginning of a step/task. 
        		// When we find this line we could stop reading, but we go on to
        		// count the lines.
        	}
        	if (normalTerminated)
        		continue;
        	
        	if (line.matches(".*" + NWChemConstants.OUTTOTSCFENERGY+ ".*")
        			|| line.matches(".*" + NWChemConstants.OUTTOTDFTENERGY+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		stepScfConvEnergies.add(Double.parseDouble(p[4]));
        		stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATASCFCONVERGED,true));
        	}
        	
        	// Reads only geometries from optimization jobs
        	if (line.matches(".*" + NWChemConstants.OUTSTARTXYZ+ ".*"))
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
                	if (line.matches(".*" + NWChemConstants.OUTENDXYZ + ".*"))
                	{
                		break;
                	} else {
                		String[] p = line.trim().split("\\s+");
                		if (p.length < 6)
                		{
                			Terminator.withMsgAndStatus("ERROR! Cannot "
                					+ "read coordinates from line '" 
                					+ line + "'.",-1);
                		}
                		String tag = p[1];
                		String symNoTag = tag.replaceAll("[^A-Za-z]+","");
                		String sym = symNoTag;
                		if (symNoTag.trim().length() == 0)
                        {
                            System.out.println(" WARNING! Atom tag '" + tag 
                            		+ "' could not be converted into elemental "
                            		+ "symbol. Using dummy symbol 'Du'.");
                            sym = "Du";
                        }
                		Point3d p3d = new Point3d(Double.parseDouble(p[3]),
                				Double.parseDouble(p[4]),
                				Double.parseDouble(p[5]));
                		IAtom atm = new Atom(sym, p3d);
                		mol.addAtom(atm);
                	}
                }
        		stepGeoms.addAtomContainer(mol);
        	}
        	
        	// Reads only geometries from frequency jobs
        	if (line.matches(".*" + NWChemConstants.OUTHESSTARTXYZ+ ".*"))
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
                	if (line.matches(".*" + NWChemConstants.OUTHESENDXYZ + ".*"))
                	{
                		break;
                	} else {
                		String[] p = line.trim().split("\\s+");
                		if (p.length < 6)
                		{
                			Terminator.withMsgAndStatus("ERROR! Cannot "
                					+ "read coordinates from line '" 
                					+ line + "'.",-1);
                		}
                		String tag = p[0];
                		String symNoTag = tag.replaceAll("[^A-Za-z]+","");
                		String sym = symNoTag;
                		if (symNoTag.trim().length() == 0)
                        {
                            System.out.println(" WARNING! Atom tag '" + tag 
                            		+ "' could not be converted into elemental "
                            		+ "symbol. Using dummy symbol 'Du'.");
                            sym = "Du";
                        }
                		Point3d p3d = new Point3d(
                				Double.parseDouble(p[2].replaceAll("D","E"))
                					/ ACCConstants.ANGSTOMTOBOHR,
                				Double.parseDouble(p[3].replaceAll("D","E"))
            						/ ACCConstants.ANGSTOMTOBOHR,
                				Double.parseDouble(p[4].replaceAll("D","E"))
            						/ ACCConstants.ANGSTOMTOBOHR);
                		IAtom atm = new Atom(sym, p3d);
                		mol.addAtom(atm);
                	}
                }
        		stepGeoms.addAtomContainer(mol);
        	}
        	
        	
        	if (line.matches(".*" + 
        			NWChemConstants.OUTENDCONVGEOMOPTSTEP+ ".*"))
        	{
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBGEOMOPTCONVERGED,true));
        	}
        	
        	if (line.matches(".*" + NWChemConstants.OUTPROJFREQ + ".*"))
        	{
        		ListOfDoubles allFreqs = new ListOfDoubles();
        		NormalModeSet nms = new NormalModeSet();
        		
        		List<Double> freqs = parseFrequencies(line);
        		allFreqs.addAll(freqs);
        		
        		//WARNING! we can read up to 6 modes per block
        		int[] currModes = new int[6];
        		for (int i=0; i<freqs.size(); i++)
        		    currModes[i] = i; 
        		
        		while ((line = buffRead.readLine()) != null)
                {
                	lineNum++;
                	if (line.trim().equals(""))
                	{
                		continue;
                	} else if (line.matches(".*" + NWChemConstants.OUTPROJFREQ 
                			+ ".*")) {
                		allFreqs.addAll(parseFrequencies(line));
                	} else if (line.startsWith("             ")) {
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
                			currModes[i] = Integer.parseInt(p[i])-1;
                		}
                	} else if (line.startsWith(" --------------")) {
                		// End of frequencies 
                		break;
                	} else if (line.matches("^ *[1-9].*$")){
                		// This lines contain the actual components
                		String[] p = line.trim().split("\\s+");
                		int atmId = (Integer.parseInt(p[0])-1)/3;
                		int compId =  ((Integer.parseInt(p[0])-1) % 3);
                    	for (int k=1; k<p.length; k++)
                    	{
                    		nms.setComponent(currModes[k-1], atmId, 
                					compId, Double.parseDouble(p[k]));
                    	}
                    } else {
                		// End of frequencies 
                		break;
                    }
                }

        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.JOBDATAVIBFREQ, allFreqs));
        	    
           	    stepData.putNamedData(new NamedData(
           				ChemSoftConstants.JOBDATAVIBMODES, nms));

           	    double dftNrg = stepScfConvEnergies.get(
           	    		stepScfConvEnergies.size()-1);
           	    double corrH = (Double) stepData.getNamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_H).getValue();
           	    double temp = (Double) stepData.getNamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_TEMP).getValue();
           	    double totS = (Double) stepData.getNamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_TOT).getValue();
           	    
           	    double gibbsFreeNRG = dftNrg + corrH - temp * (totS / 
           	    		(1000.0*NWChemConstants.AUKCAL));
           	    
           	    stepData.putNamedData(new NamedData(
           	    		ChemSoftConstants.JOBDATAGIBBSFREEENERGY,gibbsFreeNRG));
            }
        	
            if (line.matches(".*" + NWChemConstants.OUTTEMP + ".*"))
            {
                String[] p = line.trim().replace("K","").split("\\s+");
                Double val = Double.parseDouble(p[2]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_TEMP,
                        val));
            }

            if (line.matches(".*" + NWChemConstants.OUTTOTS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_TOT,
                        val));
            }

            if (line.matches(".*" + NWChemConstants.OUTVIBS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_VIB,
                        val));
            }

            if (line.matches(".*" + NWChemConstants.OUTTRAS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_TRANS,
                        val));
            }

            if (line.matches(".*" + NWChemConstants.OUTROTS + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[3]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_S_ROT,
                        val));
            }

            if (line.matches(".*" + NWChemConstants.OUTCORRH + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double val = Double.parseDouble(p[8]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_H,
                        val));
            }

            if (line.matches(".*" + NWChemConstants.OUTCORRZPE + ".*"))
            {
                String[] p = line.trim().split("\\s+");
                Double valInEh = Double.parseDouble(p[8]);
                stepData.putNamedData(new NamedData(
                        ChemSoftConstants.JOBDATTHERMOCHEM_ZPE,
                        valInEh));
            }
        	
    
        	// There is plenty of other data in the NWChem log file. 
        	// So, this list of parsed data will grow as needed...
        	// Here is a template of code to be added to parse some data
        	
        	/*

        	if (line.matches(".*" + NWChemConstants.____+ ".*"))
        	{
        		String[] p = line.trim().split("\\s+");
        		//TODO: write code that parses data
        		 
        	    stepData.putNamedData(new NamedData(
    					ChemSoftConstants.____,
    					__data__));
        	}
        	*/
        }
        
		stepEndLineNum = lineNum-1;
		
		// Store data of last job, which ended with the end of the file, unless
		// it is a normally terminated job. In which case, the citation text
		// that identifies the normal termination of a job is reported by
		// the input module, so it looks like a new and empty step, which we
		// do not include in the parsed data.
		if (!normalTerminated)
			storeDataOfOneStep(stepId, stepData, stepInitLineNum, 
					stepEndLineNum, stepScfConvEnergies, 
					stepGeoms);
    }
    
//-----------------------------------------------------------------------------
    
    private List<Double> parseFrequencies(String line)
    {
        String[] p = line.split("\\s+");
        List<Double> freqs = new ArrayList<Double>();
        for (int j=2; j<(p.length); j++)
        {
        	freqs.add(Double.parseDouble(p[j]));
        }
        return freqs;
    }
    
//-----------------------------------------------------------------------------
    
    private void storeDataOfOneStep(
    		int stepId, NamedDataCollector stepData, 
    		int stepInitLineNum, int stepEndLineNum, 
    		ListOfDoubles stepScfConvEnergies,
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
		if (!stepGeoms.isEmpty())
		{
			stepData.putNamedData(new NamedData(
					ChemSoftConstants.JOBDATAGEOMETRIES,
					stepGeoms.clone()));
		}
		
		stepsData.put(stepId,stepData.clone());
    }

//------------------------------------------------------------------------------

  	@Override
  	protected Set<FileFingerprint> getOutputFingerprint() 
  	{
  		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
  		conditions.add(new FileFingerprint(".", 10, 
  			"^\\s*Northwest Computational Chemistry Package \\(NWChem\\) .*$"));
  		return conditions;
  	}

//------------------------------------------------------------------------------

  	@Override
  	protected String getSoftwareID() {
  		return "NWChem";
  	}

//------------------------------------------------------------------------------

  	@Override
  	protected ChemSoftInputWriter getChemSoftInputWriter() {
  		return new NWChemInputWriter();
  	}

//-----------------------------------------------------------------------------
    
}
