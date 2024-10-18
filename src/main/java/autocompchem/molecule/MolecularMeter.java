package autocompchem.molecule;

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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * MolecularMeter collects measuring tools for molecular quantities
 * such as inter atomic distances, angles, and dihedral angles. Quantities
 * can be defined from SMARTS queries or atom indexes.
 * 
 * @author Marco Foscato
 */


public class MolecularMeter extends AtomContainerInputProcessor
{
    /**
     * Unique counter for naming quantities
     */
    private final AtomicInteger CRDID = new AtomicInteger(0);

    /**
     * Map of the SMARTS queries used to define the quantities to measure
     */
    private Map<String,SMARTS> smarts = new HashMap<String,SMARTS>();

    /**
     * Map of the atom indexes used to define the quantities to measure
     */
    private Map<String,List<Integer>> atmIds = 
    		new HashMap<String,List<Integer>>();

    /**
     * Map defining the type of quantity definition 
     */
    private Map<String,String> defTypes = new HashMap<String,String>();

    /**
     * List of the named descriptors in the order requested by the user
     */
    private List<String> sortedKeys = new ArrayList<String>();

    /**
     * Flag: consider only bonded atoms
     */
    private boolean onlyBonded = false;

    /**
     * Flag notifying that the meter has run 
     */
    private boolean alreadyRun = false;
    
    /**
     * String defining the task of measuring geometric descriptors
     */
    public static final String MEASUREGEOMDESCRIPTORSTASKNAME = 
    		"measureGeomDescriptors";

    /**
     * Task about measuring geometric descriptors
     */
    public static final Task MEASUREGEOMDESCRIPTORSTASK;
    static {
    	MEASUREGEOMDESCRIPTORSTASK = Task.make(MEASUREGEOMDESCRIPTORSTASKNAME);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public MolecularMeter()
    {}
    	
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(MEASUREGEOMDESCRIPTORSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularMeter.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularMeter();
    }
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */
    
	@Override
	public void initialize() 
	{
		super.initialize();

        //Get SMARTS based definition of quantities
        if (params.contains("SMARTS"))
        {
            String allSmarts = 
                    params.getParameter("SMARTS").getValue().toString();
            String[] lines = allSmarts.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                addRule(lines[i],"S");
            }
        }

        //Get atom indexes definition of quantities
        if (params.contains("ATOMINDEXES"))
        {
            String allIDs =
                       params.getParameter("ATOMINDEXES").getValue().toString();
            String[] lines = allIDs.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                addRule(lines[i],"A");
            }
        }
        
        //Check for consistency
        if (!params.contains("SMARTS") && !params.contains("ATOMINDEXES"))
        {
            String msg = "ERROR! Neither 'SMARTS' nor 'ATOMINDEXES' keywords"
                            + " found. No definition of quantities to measure!";
            Terminator.withMsgAndStatus(msg,-1);
        }

        //Get optional parameter
        if (params.contains("ONLYBONDED"))
        {
            String val = 
                params.getParameter("ONLYBONDED").getValue().toString();
            if (val.toUpperCase().equals("TRUE"))
            {
                this.onlyBonded = true;
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Appends a quantity-defining rule
     * @param line the string defining a single quantity
     * @param type the string defining the type of rule: "S" for SMARTS, "A" for
     * atom indexes
     */
	
	//TODO: consider using atom tuple matching rule

    private void addRule(String line, String type)
    {
        String[] parts = line.split("\\s+");
        String key = parts[0] + "-" + CRDID.getAndIncrement();
        this.sortedKeys.add(key);
        this.defTypes.put(key,type);
        if (type.equals("S"))
        {
            for (int j=1; j<parts.length; j++)
            {
                String singleSmarts = parts[j];
                if (singleSmarts.equals(""))
                    continue;
                String k2 = key + "_" + Integer.toString(j-1);
                this.smarts.put(k2,new SMARTS(singleSmarts));
            }
        }
        else if (type.equals("A"))
        {
            ArrayList<Integer> ids = new ArrayList<Integer>();
            for (int j=1; j<parts.length; j++)
            {
                if (parts[j].equals(""))
                    continue;
                try
                {
                    ids.add(Integer.parseInt(parts[j]));
                }
                catch (Throwable t)
                {
                    String msg = "ERROR! Unable to convert index '" + parts[j] 
                               + "' into an integer. Check line '" + line + "'";
                    Terminator.withMsgAndStatus(msg,-1);
                }
            }
            this.atmIds.put(key,ids);
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
    	if (task.equals(MEASUREGEOMDESCRIPTORSTASK))
    	{
    		Map<String,List<Double>> descriptors = measureAllQuantities(iac,
    				smarts, sortedKeys, atmIds, onlyBonded, i);
    		
            if (exposedOutputCollector != null)
        	{
    			String molID = "mol-"+i;
	    		for (String descRef : descriptors.keySet())
	    		{
	    			String reference = molID + "_" + descRef;
	  		        exposeOutputData(new NamedData(reference,
	  		        		new ListOfDoubles(descriptors.get(descRef))));
	    		}
        	}
    	} else {
    		dealWithTaskMismatch();
        }
  	}
  
//------------------------------------------------------------------------------

    public static Map<String,List<Double>> measureAllQuantities(
    		IAtomContainer mol, Map<String,SMARTS> smarts, 
    		List<String> sortedKeys, Map<String,List<Integer>> atmIds,
    		boolean onlyBonded, int i)
    {
    	Logger logger = LogManager.getLogger(MolecularMeter.class);
    	
    	String molName = MolecularUtils.getNameOrID(mol);
        Map<String,List<List<IAtom>>> allQuantities =
                      new HashMap<String,List<List<IAtom>>>();
        if (smarts.keySet().size() > 0)
        {
            logger.debug("Matching SMARTS queries");
            ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                Terminator.withMsgAndStatus("ERROR! " +cause,-1);
            }

            Map<String,List<IAtom>> targetGroups = 
            		new HashMap<String,List<IAtom>>();
            for (String key : smarts.keySet())
            {
                ArrayList<IAtom> atomsMatched = new ArrayList<IAtom>();
                if (msq.getNumMatchesOfQuery(key) == 0)
                {
                    logger.warn("WARNING! No match for SMARTS "
                                           + "query " + smarts.get(key)
                                           + " in molecule " + i + ".");
                    break;
                }
                MatchingIdxs allMatches = msq.getMatchingIdxsOfSMARTS(key);
                for (List<Integer> innerList : allMatches)
                {
                    for (Integer iAtm : innerList)
                    {
                        IAtom targetAtm = mol.getAtom(iAtm);
                        atomsMatched.add(targetAtm);
                    }
                }
                targetGroups.put(key,atomsMatched);
            }

            // Collect matches that belong to same quantity
            for (String key : sortedKeys)
            {
                List<String> groups = new ArrayList<String>();
                for (String k2 : targetGroups.keySet())
                {
                    if (k2.toUpperCase().startsWith(key.toUpperCase()))
                    {
                        groups.add(k2);
                    }
                }
                List<List<IAtom>> atmsForQuantity =
                                new ArrayList<List<IAtom>>();
                for (int ig = 0; ig<groups.size(); ig++)
                {
                    String k2qry = key + "_" + Integer.toString(ig);
                    atmsForQuantity.add(targetGroups.get(k2qry));
                }
                allQuantities.put(key,atmsForQuantity);
            }
        }

        if (atmIds.keySet().size() > 0)
        {
            logger.debug("Matching atoms from indexes");
            for (String key : atmIds.keySet())
            {
                List<List<IAtom>> atmsForQuantity = new ArrayList<List<IAtom>>();
                for (Integer id : atmIds.get(key))
                {
                    ArrayList<IAtom> altAtms = new ArrayList<IAtom>();
                    //
                    // WARNING! Change from 1- to 0-based indexing
                    //
                    altAtms.add(mol.getAtom(id-1)); 
                    atmsForQuantity.add(altAtms);
                }
                allQuantities.put(key,atmsForQuantity);
            }
        }

        Map<String,List<Double>> resThisMol = new HashMap<String,List<Double>>();

        for (String key : sortedKeys)
        {
            if (!allQuantities.containsKey(key))
            {
            	logger.debug("No quantity '" + key + "' found.");
                continue;
            }

            List<List<IAtom>> atmsForQuantity = allQuantities.get(key);
            if (key.toUpperCase().startsWith("DIST"))
            {
            	if (atmsForQuantity.size() != 2)
            	{
            		logger.info("Not enough matches for "
                        		+ "quantity '" + key + "'. Found only " 
                        		+ atmsForQuantity.size() + " set.");
            		continue;
            	}
            	
                //Measure distance A-B
                List<Double> distances = new ArrayList<Double>();
                for (IAtom atmA : atmsForQuantity.get(0))
                {
                    for (IAtom atmB : atmsForQuantity.get(1))
                    {
                        if (atmA.equals(atmB))
                            continue;

                        if (onlyBonded)
                        {
                            if (!mol.getConnectedAtomsList(atmA).contains(atmB))
                            {
                                continue;
                            }
                        }

                        double res = MolecularUtils.calculateInteratomicDistance(
                                                                  atmA,
                                                                  atmB);

                        //Report value
                        String strRes = "Mol." + i + " " 
                            + molName + " "
                            + " Dst."
                            + key + " "
                            + MolecularUtils.getAtomRef(atmA,mol) 
                            + ":"
                            + MolecularUtils.getAtomRef(atmB,mol) 
                            + " = "
                            + res;
                        logger.info(strRes);
                        distances.add(res);
                    }
                }

                //Store results for this molecule
                resThisMol.put(key,distances);

            } 
            else if (key.toUpperCase().startsWith("ANG"))
            {
            	if (atmsForQuantity.size() != 3)
            	{
            		logger.info("Not enough matches for "
                        		+ "quantity '" + key + "'. Found only " 
                        		+ atmsForQuantity.size() + " set.");
            		continue;
            	}
            	
                //Measure angle A-B-C
                List<Double> angles = new ArrayList<Double>();
                for (IAtom atmA : atmsForQuantity.get(0))
                {
                    for (IAtom atmB : atmsForQuantity.get(1))
                    {
                        if (atmA.equals(atmB))
                            continue;

                        if (onlyBonded)
                        {
                            if (!mol.getConnectedAtomsList(
                            		atmA).contains(atmB))
                            {
                                continue;
                            }
                        }

                        for (IAtom atmC : atmsForQuantity.get(2))
                        {
                            if (atmB.equals(atmC))
                                continue;

                            if (atmA.equals(atmC))
                                continue;

                            if (onlyBonded)
                            {
                                if (!mol.getConnectedAtomsList(
                                		atmB).contains(atmC))
                                {
                                    continue;
                                }
                            }
                        
                            double res = MolecularUtils.calculateBondAngle(
                            		atmA, atmB, atmC);
                            
                            //Report value
                            String strRes = "Mol." + i + " " 
                                + molName + " " + " Ang."
                                    + key + " "
		                        + MolecularUtils.getAtomRef(atmA,mol) + ":"
		                        + MolecularUtils.getAtomRef(atmB,mol) + ":"
		                        + MolecularUtils.getAtomRef(atmC,mol) +" = "
                                + res;
                            logger.info(strRes);
                            angles.add(res);
                        }
                    }
                }

                //Store results for this molecule
                resThisMol.put(key,angles);
            } 
            else if (key.toUpperCase().startsWith("DIH"))
            {
            	if (atmsForQuantity.size() != 4)
            	{
            		logger.info("Not enough matches for "
                        		+ "quantity '" + key + "'. Found only " 
                        		+ atmsForQuantity.size() + " set.");
            		continue;
            	}
            	
                //Measure dihedral angle A-B-C-D
                List<Double> dihedrals = new ArrayList<Double>();
                for (IAtom atmA : atmsForQuantity.get(0))
                {
                    for (IAtom atmB : atmsForQuantity.get(1))
                    {
                        if (atmA.equals(atmB))
                            continue;

                        if (onlyBonded)
                        {
                            if (!mol.getConnectedAtomsList(
                            		atmA).contains(atmB))
                            {
                                continue;
                            }
                        }
                        
                        for (IAtom atmC : atmsForQuantity.get(2))
                        {
                            if (atmB.equals(atmC))
                                continue;

                            if (atmA.equals(atmC))
                                continue;

                            if (onlyBonded)
                            {
                                if (!mol.getConnectedAtomsList(
                                		atmB).contains(atmC))
                                {
                                    continue;
                                }
                            }

                            for (IAtom atmD : atmsForQuantity.get(3))
                            {
                                if (atmC.equals(atmD))
                                    continue;
                                if (atmB.equals(atmD))
                                    continue;
                                if (atmA.equals(atmD))
                                    continue;

                                if (onlyBonded)
                                {
                                    if (!mol.getConnectedAtomsList(
                                    		atmC).contains(atmD))
                                    {                                   
                                        continue;                       
                                    }
                                }

                                double res = 
                                		MolecularUtils.calculateTorsionAngle(
                                				atmA, atmB, atmC, atmD);

                                //Report value
                                String strRes = "Mol." + i + " "
                                    + molName + " " + " Dih."
                                    + key +" "
		                            + MolecularUtils.getAtomRef(atmA,mol) + ":"
		                            + MolecularUtils.getAtomRef(atmB,mol) + ":"
		                            + MolecularUtils.getAtomRef(atmC,mol) + ":"
		                            + MolecularUtils.getAtomRef(atmD,mol) +" = "
                                        + res;
                                logger.info(strRes);
                                dihedrals.add(res);
                            }
                        }
                    }
                }

                //Store results for this molecule
                resThisMol.put(key,dihedrals);

            } 
            else 
            {
                Terminator.withMsgAndStatus("ERROR! What do you mean "
                  + "with '" + key + "'? Unable to identify the type "
                  + "of quantity to measure.",-1);
            }
        }

        return resThisMol;
    }

//-----------------------------------------------------------------------------
	
}
