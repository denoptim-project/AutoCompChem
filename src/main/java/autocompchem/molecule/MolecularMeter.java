package autocompchem.molecule;

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
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * MolecularMeter collects measuring tools for molecular quantities
 * such as inter atomic distances, angles, and dihedral angles. Quantities
 * can be defined from SMARTS queries or atom indexes.
 * 
 * @author Marco Foscato
 */


public class MolecularMeter extends Worker
{
    /**
     * Unique counter for naming quantities
     */
    private final AtomicInteger CRDID = new AtomicInteger(0);

    /**
     * The file containing the molecules to analyse
     */
    private File inFile;

    /**
     * Map of the SMARTS queries used to define the quantities to measure
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Map of the atom indexes used to define the quantities to measure
     */
    private Map<String,ArrayList<Integer>> atmIds = 
                                       new HashMap<String,ArrayList<Integer>>();

    /**
     * Map defining the type of quantity definition 
     */
    private Map<String,String> defTypes = new HashMap<String,String>();

    /**
     * List of the named descriptors in the order requested by the user
     */
    private ArrayList<String> sortedKeys = new ArrayList<String>();

    /**
     * Flag: consider only bonded atoms
     */
    private boolean onlyBonded = false;

    /**
     * Storage of results (per molecule)
     */
    private ArrayList<Map<String,ArrayList<Double>>> results = 
                                new ArrayList<Map<String,ArrayList<Double>>>();

    /**
     * Flag notifying that the meter has run 
     */
    private boolean alreadyRun = false;

    /**
     * Verbosity level
     */
    private int verbosity = 1;
    
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
        //Define verbosity
        String vStr = params.getParameterOrDefault("VERBOSITY",
        		NamedDataType.STRING, "1").getValueAsString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to MolecularMeter");


        //Get and check the input file (which has to be an SDF file)
        String pathname = params.getParameter("INFILE").getValue().toString();
        this.inFile = new File(pathname);
        FileUtils.foundAndPermissions(pathname,true,false,false);

        //Get SMARTS based definition of quantities
        if (params.contains("SMARTS"))
        {
            String allSmarts = 
                    params.getParameter("SMARTS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing SMARTS queries ");
            }
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
            if (verbosity > 0)
            {
                System.out.println(" Importing atom indexes ");
            }
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
                this.smarts.put(k2,singleSmarts);
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
    	if (task.equals(MEASUREGEOMDESCRIPTORSTASK))
    	{
    		measureAllQuantities();
    	} else {
    		dealWithTaskMistMatch();
        }
  	}
  
//------------------------------------------------------------------------------

    /**
     * Measure all the quantities required according to the information
     * provided to the constructor. The values of the measured quantities are
     * reported in the log (if verbosity lever higher than 0) or recovered 
     * from this MolecularMeter with the method {@link #getAllResults} and
     * {@link #getSingleResults getSingleResults}.
     */

    public void measureAllQuantities()
    {
        int i = 0;
        try {
            List<IAtomContainer> mols = IOtools.readMultiMolFiles(inFile);
            for (IAtomContainer mol : mols) 
            {
                // Get the molecule
                i++;
                boolean skipMol = true;
                String molName = MolecularUtils.getNameOrID(mol);
                if (verbosity > 0)
                {
                    System.out.println(" Analyzing molecule " + i);
                }

                // Get target atoms                
                Map<String,ArrayList<ArrayList<IAtom>>> allQuantities =
                              new HashMap<String,ArrayList<ArrayList<IAtom>>>();
                if (smarts.keySet().size() > 0)
                {
                    if (verbosity > 1)
                    {
                        System.out.println(" Matching SMARTS queries");
                    }
                    ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,
                                                                     verbosity);
                    if (msq.hasProblems())
                    {
                        String cause = msq.getMessage();
                        Terminator.withMsgAndStatus("ERROR! " +cause,-1);
                    }
    
                    Map<String,ArrayList<IAtom>> targetGroups = 
                                         new HashMap<String,ArrayList<IAtom>>();
                    for (String key : smarts.keySet())
                    {
                        ArrayList<IAtom> atomsMatched = new ArrayList<IAtom>();
                        if (msq.getNumMatchesOfQuery(key) == 0)
                        {
                            System.out.println("WARNING! No match for SMARTS "
                                                   + "query " + smarts.get(key)
                                                   + " in molecule " + i + ".");
                            break;
                        }
                        else
                        {
                            skipMol = false;
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
                        ArrayList<String> groups = new ArrayList<String>();
                        for (String k2 : targetGroups.keySet())
                        {
                            if (k2.toUpperCase().startsWith(key.toUpperCase()))
                            {
                                groups.add(k2);
                            }
                        }
                        ArrayList<ArrayList<IAtom>> atmsForQuantity =
                                        new ArrayList<ArrayList<IAtom>>();
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
                    if (verbosity > 1)
                    {
                        System.out.println(" Matching atoms from indexes");
                    }
                    skipMol = false;
                    for (String key : atmIds.keySet())
                    {
                        ArrayList<ArrayList<IAtom>> atmsForQuantity =
                                              new ArrayList<ArrayList<IAtom>>();
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
    
                //In case of no match
                if (skipMol)
                {
                    results.add(null);
                    continue;
                }

                //Prepare storage of results
                Map<String,ArrayList<Double>> resThisMol = 
                                        new HashMap<String,ArrayList<Double>>();

                //Measure all quantities
                for (String key : sortedKeys)
                {
                    if (!allQuantities.containsKey(key))
                    {
                        if (verbosity > 1)
                        {
                            System.out.println(" No quantity '" + key 
                                                                  + "' found.");
                        }
                        continue;
                    }

                    ArrayList<ArrayList<IAtom>> atmsForQuantity = 
                                                         allQuantities.get(key);
                    if (key.toUpperCase().startsWith("DIST"))
                    {
                    	if (atmsForQuantity.size() != 2)
                    	{
                    		if (verbosity > 0)
                            {
                                System.out.println(" Not enough matches for "
                                		+ "quantity '" + key + "'. Found only " 
                                		+ atmsForQuantity.size() + " set.");
                            }
                    		continue;
                    	}
                    	
                        //Measure distance A-B
                        ArrayList<Double> distances = new ArrayList<Double>();
                        for (IAtom atmA : atmsForQuantity.get(0))
                        {
                            for (IAtom atmB : atmsForQuantity.get(1))
                            {
                                if (atmA.equals(atmB))
                                    continue;

                                if (onlyBonded)
                                {
                                    if (
                                !mol.getConnectedAtomsList(atmA).contains(atmB))
                                    {
                                        continue;
                                    }
                                }

                                double res = 
                                   MolecularUtils.calculateInteratomicDistance(
                                                                          atmA,
                                                                          atmB);

                                //Report value
                                if (verbosity > 0)
                                {
                                    String strRes = "Mol." + i + " " 
                                        + molName + " "
                                        + " Dst."
                                        + key + " "
                                        + MolecularUtils.getAtomRef(atmA,mol) 
                                        + ":"
                                        + MolecularUtils.getAtomRef(atmB,mol) 
                                        + " = "
                                        + res;
                                    System.out.println(strRes);
                                }
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
                    		if (verbosity > 0)
                            {
                                System.out.println(" Not enough matches for "
                                		+ "quantity '" + key + "'. Found only " 
                                		+ atmsForQuantity.size() + " set.");
                            }
                    		continue;
                    	}
                    	
                        //Measure angle A-B-C
                        ArrayList<Double> angles = new ArrayList<Double>();
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
                                
                                    double res = 
                                            MolecularUtils.calculateBondAngle(
                                                                        atmA,
                                                                        atmB,
                                                                        atmC);
                                    //Report value
                                    if (verbosity > 0)
                                    {
                                        String strRes = "Mol." + i + " " 
                                            + molName + " " + " Ang."
                                                + key + " "
                                    + MolecularUtils.getAtomRef(atmA,mol) + ":"
                                    + MolecularUtils.getAtomRef(atmB,mol) + ":"
                                    + MolecularUtils.getAtomRef(atmC,mol) +" = "
                                            + res;
                                        System.out.println(strRes);
                                    }
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
                    		if (verbosity > 0)
                            {
                                System.out.println(" Not enough matches for "
                                		+ "quantity '" + key + "'. Found only " 
                                		+ atmsForQuantity.size() + " set.");
                            }
                    		continue;
                    	}
                    	
                        //Measure dihedral angle A-B-C-D
                        ArrayList<Double> dihedrals = new ArrayList<Double>();
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
                                                                        atmA,
                                                                        atmB,
                                                                        atmC,
                                                                        atmD);

                                        //Report value
                                        if (verbosity > 0)
                                        {
                                            String strRes = "Mol." + i + " "
                                                + molName + " " + " Dih."
                                                + key +" "
                                    + MolecularUtils.getAtomRef(atmA,mol) + ":"
                                    + MolecularUtils.getAtomRef(atmB,mol) + ":"
                                    + MolecularUtils.getAtomRef(atmC,mol) + ":"
                                    + MolecularUtils.getAtomRef(atmD,mol) +" = "
                                                + res;
                                            System.out.println(strRes);
                                        }
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

                //Store output
                results.add(resThisMol);
        
            } //end loop over molecules

        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
        }
        
        if (exposedOutputCollector != null)
    	{
	    	int ii = 0;
	    	for (Map<String,ArrayList<Double>> molDescr : results)
	    	{
	    		ii++;
	    		if (molDescr != null)
	    		{
	    			String molID = "mol-"+ii;
		    		for (String descRef : molDescr.keySet())
		    		{
		    			String reference = molID + "_" + descRef;
		  		        exposeOutputData(new NamedData(reference, 
		  		        		NamedDataType.DOUBLE, molDescr.get(descRef)));
		    		}
	    		}
	    	}
    	}

        //Set flag
        alreadyRun = true;
    }

//-----------------------------------------------------------------------------

    /**
     * Return the results for all measured quantities. Vectors of values
     * are returned per each molecule in the form of 
     * a map using as keys the strings identifying the quantities
     * (plus an index to avoid duplicate) as
     * provided via the SMARTS parameter when constructing the object.
     * @return the results for all measured quantities
     */

    public ArrayList<Map<String,ArrayList<Double>>> getAllResults()
    {
        if (!alreadyRun)
            this.measureAllQuantities();

        return results;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Return the results for a single molecule and a single quantity. 
     * The molecule is identified by the position (0-n) in the input.
     * The quantity is identified by
     * its position (0-n) in the list of quantities provided to the constructor 
     * by means of the SMARTS parameter. Note that the result for a single 
     * quantity can consist in more than one value, thus a vector is returned.
     * @param molID the index of the molecule for which the result is required
     * @param quantityID the index of the required quantity
     * @return the results for a single molecule and a single quantity
     */

    public ArrayList<Double> getSingleResults(int molID, int quantityID)
    {
        if (!alreadyRun)
            this.measureAllQuantities();

        Map<String,ArrayList<Double>> resThisMol = results.get(molID);
        ArrayList<Double> res = resThisMol.get(sortedKeys.get(quantityID));
        return res;
    }

//-----------------------------------------------------------------------------
	
}
