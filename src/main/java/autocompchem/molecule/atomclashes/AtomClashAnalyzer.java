package autocompchem.molecule.atomclashes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/*   
 *   Copyright (C) 2014  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool that analyzes interatomic distances and report
 * atom clashes. Being r<sub>A</sub> and r<sub>B</sub> the v.d.w radii of two
 * atoms, 
 * and <i>d</i> the interatomic distance,
 * AtomClashAnalyzer defines an atom clash if the <i>overlap</i> between 
 * the two atoms is larger than a specific <i>cutoff</i>. 
 * <br><br>
 * <i>overlap</i> = r<sub>A</sub> + r<sub>B</sub> - <i>d</i> - <i>allowance</i>
 * <br><br>
 * where <i>allowance</i> is a value in angstrom that allows to specify that 
 * some atoms can came closer to each other that the sum of their van der Waals 
 * radii.<br>
 * 
 * @author Marco Foscato
 */


public class AtomClashAnalyzer extends AtomContainerInputProcessor
{
	/**
	 * File where to print the results
	 */
    private File outFile;

    /**
     * List target atoms to consider when searching for clashes.
     */
    private Map<String,SMARTS> targetsmarts;

    //cutoff
    private double cutoff = 0.6;

    /**
     * General allowance
     */
    private double allowance = 0.4;
    
    /**
     * Allowance for atoms in 1-3 relation
     */
    private double allowance13 = 0.0;
    
    /**
     * Allowance for atoms in 1-4 relation
     */
    private double allowance14 = 0.0;

    /**
     * Custom allowances
     */
    private List<VDWAllowance> allowances = new ArrayList<VDWAllowance>();
    
    /**
     * String defining the task of detecting atom clashes
     */
    public static final String ANALYZEVDWCLASHESTASKNAME = "analyzeVDWClashes";

    /**
     * Task about detecting atom clashes
     */
    public static final Task ANALYZEVDWCLASHESTASK;
    static {
    	ANALYZEVDWCLASHESTASK = Task.make(ANALYZEVDWCLASHESTASKNAME);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomClashAnalyzer()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYZEVDWCLASHESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomClashAnalyzer.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new AtomClashAnalyzer();
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	  
        if (params.contains("OUTFILE"))
        {
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValue().toString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get the list of SMARTS to be matched
        if (params.contains("TARGETSMARTS"))
        {
        	this.targetsmarts = new HashMap<String,SMARTS>();
            String allSamrts = 
                params.getParameter("TARGETSMARTS").getValue().toString();
            String[] lines = allSamrts.split("\\r?\\n");
            int k = 0;
            for (int i=0; i<lines.length; i++)
            {
                String[] parts = lines[i].split("\\s+");
                for (int j=0; j<parts.length; j++)
                {
                    String singleSmarts = parts[j];
                    if (singleSmarts.equals(""))
                        continue;
                    k++;
                    String key2 = "target_" + Integer.toString(k);
                    this.targetsmarts.put(key2, new SMARTS(singleSmarts));
                }
            }
        }

        //Cutoff for atoms overlap
        if (params.contains("CUTOFF"))
        {
            this.cutoff = Double.parseDouble(
                 params.getParameter("CUTOFF").getValue().toString());
        }

        //Allowance for all atoms
        if (params.contains("ALLOWANCE"))
        {
            this.allowance = Double.parseDouble(
                 params.getParameter("ALLOWANCE").getValue().toString());
        }

        //Allowance for atoms in 1-3 relationship
        if (params.contains("ALLOWANCE13"))
        {
            this.allowance13 = Double.parseDouble(
                 params.getParameter("ALLOWANCE13").getValue().toString());
        }

        //Allowance for atoms in 1-4 relationship
        if (params.contains("ALLOWANCE14"))
        {
            this.allowance14 = Double.parseDouble(
                 params.getParameter("ALLOWANCE14").getValue().toString());
        }

        //Get custom allowance
        if (params.contains("CUSTOMALLOWANCE"))
        {
            String allAllowance = 
                params.getParameter("CUSTOMALLOWANCE").getValue().toString();
            String[] lines = allAllowance.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                String[] parts = lines[i].split("\\s+");
                if (parts.length != 3)
                {
                    String msg = "Wrong number of arguments in definition of "
                                 + "VDW contact allowance.";
                    Terminator.withMsgAndStatus(msg,-1);
                }

                VDWAllowance newAlw = new VDWAllowance(parts[0],  //SMARTS 1
                                                       parts[1],  //SMARTS 2
                                     Double.parseDouble(parts[2])); //allowance

                this.allowances.add(newAlw);
            }
        }
    }
      
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @Override
    public void performTask()
    {
    	processInput();
    }
      
//------------------------------------------------------------------------------

  	@Override
    public void processOneAtomContainer(IAtomContainer iac, int i) 
    {
  		if (task.equals(ANALYZEVDWCLASHESTASK))
  		{
  			List<AtomClash> clashes = analyzeAtomClashes(iac);
  			if (clashes.size() > 0)
            {
                String msg = " Found " + clashes.size() + " atom clashes: " + NL;
                for (AtomClash ac : clashes)
                	msg = msg + ac + NL;
                logger.info(msg);

                if (outFile!=null)
                {
                    //store results in output SDF
                    writeAtmClashToSDFFields(iac, clashes, i, outFile);
                }
            }
  		} else {
  			dealWithTaskMismatch();
  		}
    }
  
//-----------------------------------------------------------------------------

    /**
     * Performs the analysis of atom clashes on a single molecule. The analysis
     * is performed according to the parameters provided to the constructor.
     * @param mol the molecule to analyze
     * @return the list of atom clashes.
     */

    public List<AtomClash> analyzeAtomClashes(IAtomContainer mol)
    {
    	return analyzeAtomClashes(mol, targetsmarts, allowances, cutoff, 
    			allowance, allowance13, allowance14, logger);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs the analysis of atom clashes on a single molecule. 
     * Being rA and rB the v.d.w radii of two atoms A and B, and d the 
     * interatomic distance, an atom clash occurs if the 
     * overlap=(rA + rB - d - allowance) between atoms A and B is larger 
     * than a specific cutoff. The allowance is a value in angstrom that allows 
     * to specify that some atoms can came closer to each other that the sum of 
     * their van der Waals radii.
     * @param mol the molecule to analyze
     * @param targetsmarts list of smarts to be included in the analysis
     * @param allowances custom allowances
     * @param cutoff
     * @param allowance
     * @param allowance13
     * @param allowance14
     * @param logger logging tool
     * @return the list of atom clashes
     */
    public static List<AtomClash> analyzeAtomClashes(IAtomContainer mol, 
    		Map<String, SMARTS> targetsmarts, 
    		List<VDWAllowance> allowances, 
    		double cutoff,
    		double allowance,
    		double allowance13,
    		double allowance14,
    		Logger logger)
    {
        List<AtomClash> acList = new ArrayList<AtomClash>();

        //Identify target atoms
        List<IAtom> targets = new ArrayList<IAtom>();
        if (targetsmarts!=null)
        {
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,targetsmarts);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                if (cause.contains("The AtomType") &&
                                        cause.contains("could not be found"))
                {
                    Terminator.withMsgAndStatus("ERROR! " + cause
                        + " To solve the problem try to move this "
                        + "element to \"Du\" an try again.",-1);
                }
                logger.warn("\nWARNING! Problems in using SMARTS queries. "
                                + cause + NL 
                                + "Matches: "+msq.getNumMatchesMap());
            }
            
            if (msq.getTotalMatches() > 0)
            {
                for (String key : targetsmarts.keySet())
                {
                    if (!msq.hasMatches(key))
                    {
                        continue;
                    }
                    MatchingIdxs matches =  msq.getMatchingIdxsOfSMARTS(key);
                    for (List<Integer> innerList : matches)
                    {
                        for (Integer iAtm : innerList)
                        {
                            IAtom targetAtm = mol.getAtom(iAtm);
                            targets.add(targetAtm);
                        }
                    }
                }
            }
        } else {
            for (IAtom a : mol.atoms())
                targets.add(a);
        }
        logger.debug(" Searching clashes among " + targets.size() + " atoms");
        logger.trace("List of atoms: " + targets);

        //Get custom allowances
        Map<String,Double> allowanceMasck = new HashMap<String,Double>();
        if (allowances.size() != 0)
        {
            //make map of smarts
            Map<String,SMARTS> smarts = new HashMap<String,SMARTS>();

            for (int ikey = 0; ikey<allowances.size(); ikey++)
            {
                VDWAllowance a = allowances.get(ikey);
                String[] atmsmarts = a.getSmarts();
                for (int jkey = 0; jkey<2; jkey++)
                {
                    String key = Integer.toString(ikey) + "_"
                                 + Integer.toString(jkey); 
                    smarts.put(key, new SMARTS(atmsmarts[jkey]));
                }
            }

            //identify atoms subject to custom allowance
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                Terminator.withMsgAndStatus("ERROR! " +cause,-1);
            }
            for (int iikey=0; iikey<allowances.size(); iikey++)
            {
                String key1 = Integer.toString(iikey) + "_0";
                String key2 = Integer.toString(iikey) + "_1";
                if (msq.getNumMatchesOfQuery(key1) == 0 || 
                            msq.getNumMatchesOfQuery(key2) == 0)
                {
                    continue;
                }
                MatchingIdxs allMatches1 =  msq.getMatchingIdxsOfSMARTS(key1);
                MatchingIdxs allMatches2 =  msq.getMatchingIdxsOfSMARTS(key2);
                for (List<Integer> innerList1 : allMatches1)
                {
                    for (Integer iAtm1 : innerList1)
                    {
                        String pairkey = "";
                        for (List<Integer> innerList2 : allMatches2)
                        {
                            for (Integer iAtm2 : innerList2)
                            {
                                if (iAtm1 == iAtm2)
                                    continue;
                                if (iAtm1 < iAtm2)
                                    pairkey = iAtm1 + "_" + iAtm2;
                                else
                                    pairkey = iAtm2 + "_" + iAtm1;

                                double alwVal = 
                                          allowances.get(iikey).getAllowance();
                                allowanceMasck.put(pairkey,alwVal);
                            }
                        }
                    }
                }
            }
        }

        //Analyze interatomic destances between pairs of target atoms
        for (int i = 0; i<targets.size(); i++)
        {
            IAtom atmI = targets.get(i);
            List<IAtom> nbrsI = mol.getConnectedAtomsList(atmI);
            
            for (int j = i+1; j<targets.size(); j++)
            {
                IAtom atmJ = targets.get(j);
                List<IAtom> nbrsJ = mol.getConnectedAtomsList(atmJ);

                //Ignore bound pairs
                if (nbrsI.contains(atmJ))
                    continue;

                //Evaluate need of 1-N allowance
                boolean found = false;
                double alw = allowance;
                for (IAtom nI : nbrsI)
                {
                    if (allowance13 > 0 && nbrsJ.contains(nI))
                    {
                        // 1-3 relationship
                        alw = allowance13;
                        break;
                    }
                     else if (allowance14 > 0)
                    {
                        List<IAtom> nbrsNI = mol.getConnectedAtomsList(nI);
                        for (IAtom nnI : nbrsNI)
                        {
                            if (nbrsJ.contains(nnI))
                            {
                                // 1-4 relationship
                                alw = allowance14;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found)
                        break;
                }

                //Evaluate need of custom allowance
                String pairkey = "";
                int iAtm1 = mol.indexOf(atmI);
                int iAtm2 = mol.indexOf(atmJ);
                if (iAtm1 < iAtm2)
                    pairkey = iAtm1 + "_" + iAtm2;
                else
                    pairkey = iAtm2 + "_" + iAtm1;
                if (allowanceMasck.containsKey(pairkey))
                {
                    alw = allowanceMasck.get(pairkey);
                }

                //Get distance 
                double d = MolecularUtils.calculateInteratomicDistance(atmI,
                                                                       atmJ);

                //Get vdw radii sum (corrected with allowance)
                double vdwsum = AtomUtils.getVdwRadius(atmI) 
                                + AtomUtils.getVdwRadius(atmJ);

                double overlap = vdwsum
                                - d 
                                - alw;

                //Check for atom clash
                if (overlap >= cutoff)
                {
                    String refI = MolecularUtils.getAtomRef(atmI,mol);
                    String refJ = MolecularUtils.getAtomRef(atmJ,mol);
                    AtomClash ac = new AtomClash(atmI, refI, atmJ, refJ, d,
                    		vdwsum, alw);
                    acList.add(ac);
                }
            }
        }
        return acList;
    }

//-----------------------------------------------------------------------------

    /**
     * Writes information on atom clashes in SDF fields. The output file can
     * be new of existing, in which case the molecule will be appended at the
     * end.
     * @param mol the molecular system
     * @param clashes the list of atom clashes to consider
     * @param i index of record in the results storage
     * @param file the output SDF file
     */

    public void writeAtmClashToSDFFields(IAtomContainer mol, 
    		List<AtomClash> clashes, int i, File file)
    {
        //find closes
        @SuppressWarnings("unused")
                AtomClash worstac;
        double worstoverlap = 0.0;
        for (AtomClash ac : clashes)
        {
            if (ac.getOverlap() > worstoverlap)
            {
                worstoverlap = ac.getOverlap();
                worstac = ac;
            } 
        }

        //prepare fields
        mol.setProperty("WorstOverlap", worstoverlap);
        mol.setProperty("AtomClashes", clashes);

        //write
        IOtools.writeSDFAppend(file,mol,true);
    }

//-----------------------------------------------------------------------------
    
}
