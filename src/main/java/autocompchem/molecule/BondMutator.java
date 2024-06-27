package autocompchem.molecule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule.RuleType;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to add, modify, or remove one or more bonds.
 * 
 * @author Marco Foscato
 */


public class BondMutator extends AtomContainerInputProcessor
{
    /**
     * Name of the output file
     */
    private File outFile;

    /**
     * List (with string identifier) of smarts queries to identify target bonds.
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * List (with string identifier) of feature for target bond
     */
    private Map<String,Object> newFeatureValue = new HashMap<String,Object>();
    
    /**
     * String defining the task of mutating bonds
     */
    public static final String MUTATEBONDSTASKNAME = "mutateBonds";

    /**
     * Task about mutating atoms
     */
    public static final Task MUTATEBONDSTASK;
    static {
    	MUTATEBONDSTASK = Task.make(MUTATEBONDSTASKNAME);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public BondMutator()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(MUTATEBONDSTASK)));
    }

//------------------------------------------------------------------------------

    //TODO-gg
    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/BondMutator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new BondMutator();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters set for this worker.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	
        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get the list of SMARTS to be matched
        String allSMARTS = params.getParameter("SMARTSMAP").getValueAsString();


        //TODO-gg split the functionality of AtomTupleMatchingRule into
        // MAtchingRule and AtomTupleMatchingRule + BondMatchingRule
        
        // NB: the REGEX makes this compatible with either new-line character
        String[] lines = allSMARTS.split("\\r?\\n|\\r");
        for (int i=0; i<lines.length; i++)
        {
            String line = lines[i];
            if (line.equals(""))
                continue;
            String[] words = line.split("\\s+");
            String refName = "mutDef" + Integer.toString(i);
            this.smarts.put(refName, words[0]);
            this.newFeatureValue.put(refName, words[1]);
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
      	if (task.equals(MUTATEBONDSTASK))
      	{
      		editBonds(iac, smarts, newFeatureValue);
            
            if (exposedOutputCollector != null)
            {
            	String molID = "mol-"+i;
		        exposeOutputData(new NamedData(molID, 
		      		NamedDataType.IATOMCONTAINER, iac));
            }
            
            if (outFile!=null)
            {
				IOtools.writeSDFAppend(outFile, iac, true);
            }
      	} else {
      		dealWithTaskMismatch();
        }
    }
	
//-----------------------------------------------------------------------------

	//TODO: this method is general enough to be moved in a more general toolbox
	
    /**
     * Look for the atoms defined by the given list of SMARTS.
     * @param mol the molecule to work with.
     * @param smarts the list of smarts queries with reference names. The latter
     * are used to group the hits so that you can easily find out which SMARTS/s
     * matched a specific atom.
     * @return the list of matched atoms grouped by reference name of the 
     * given SMARTS queries.
     */

    public static Map<String, List<MatchingIdxs>> identifyAtomIdxTuplesBySMARTS(
    		IAtomContainer mol, Map<String, String> smartsTuples)
    {
    	Logger logger = LogManager.getLogger();

    	// Here we collect all atom tuples by the reference name of
    	// the tuple of SMARTS given as parameter
        Map<String,List<MatchingIdxs>> allIDsForEachTuple =
                new HashMap<String,List<MatchingIdxs>>();
    	
    	// Extract single SMARTS from tuples of SMARTS, but keep track of
    	// the tubles via the reference names
    	Set<String> sortedKeys = new TreeSet<String>();
    	Map<String,String> smarts = new HashMap<String,String>();
        for (Entry<String,String> tupleRule : smartsTuples.entrySet())
        {
        	String key = tupleRule.getKey();
        	String[] smarts_for_key = tupleRule.getValue().trim().split("\\s+");
        	for (int i=0; i<smarts_for_key.length; i++)
        	{
        		sortedKeys.add(key);
        		SMARTS oneSMARTS = new SMARTS(smarts_for_key[i]);
        		//NB: this format is assumed here and elsewhere
        		String refName = key + "_" + i;
        		smarts.put(refName, oneSMARTS.getString());
        	}
        }

    	//First apply all SMARTS in once, for the sake of efficiency
        ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! Cannot use SMARTS to find "
            		+ "specific atoms " + cause, -1);
        }
        if (msq.getTotalMatches() == 0)
        {
        	logger.warn("None of the SMARTS matched anything.");
        	return allIDsForEachTuple;
        }
        
        //Get matches grouped by the ref names of SMARTS queries
        Map<String, MatchingIdxs> groupedByTuple = 
        		new HashMap<String,MatchingIdxs>();
        for (String key : smarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
            	logger.warn("WARNING: SMARTS query '" + key
            			+ "' did not match anything.");
                continue;
            }
            groupedByTuple.put(key, msq.getMatchingIdxsOfSMARTS(key));
        }
        
        // Collect matches that belong to same tuple
        for (String key : sortedKeys)
        {
            List<String> smartsRefNamesForTuple = new ArrayList<String>();
            for (String k2 : groupedByTuple.keySet())
            {
                if (k2.toUpperCase().startsWith(key.toUpperCase()))
                {
                	smartsRefNamesForTuple.add(k2);
                }
            }
            boolean allComponentsMatched = true;;
            List<MatchingIdxs> atmsForMR = new ArrayList<MatchingIdxs>();
            for (int ig = 0; ig<smartsRefNamesForTuple.size(); ig++)
            {
            	//NB: here we assume the format of the SMARTS ref names
                String k2qry = key + "_" + Integer.toString(ig);
                if (groupedByTuple.containsKey(k2qry))
                {
                	atmsForMR.add(groupedByTuple.get(k2qry));
                } else {
                	allComponentsMatched = false;
                }
            }
            if (allComponentsMatched)
            {
            	allIDsForEachTuple.put(key, atmsForMR);
            }
        }
        
        return allIDsForEachTuple;
    }

//-----------------------------------------------------------------------------	
	
	//TODO-gg: this method is general enough to be moved in a more general toolbox
	
    /**
     * Look for the bonds defined by the given list of SMARTS.
     * @param mol the molecule to work with
     * @param smarts the list of smarts queries with reference names. The latter
     * are used to group the hits so that you can easily find out which SMARTS/s
     * matched a specific bond.
     * @return the list of matched atoms grouped by reference name of the 
     * given SMARTS queries.
     */

    public static Map<String, List<IBond>> identifyBondsBySMARTS(
    		IAtomContainer mol, Map<String, String> smarts)
    {
    	Logger logger = LogManager.getLogger();
        Map<String, List<IBond>> targets = new HashMap<String, List<IBond>>();
        ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! Cannot identify bonds: "
                + "attempt to use the SMARTS returns an error. "
                + "Details: " + cause,-1);
        }
        if (msq.getTotalMatches() == 0)
        {
        	logger.warn("None of the SMARTS matched anything.");
        	return targets;
        }
  
        for (String key : smarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(key) == 0)
            {
            	logger.warn("WARNING: SMARTS query '" + key
            			+ "' did not match anything.");
                continue;
            }

            List<IBond> targetsForThisKey = new ArrayList<IBond>();
            MatchingIdxs allMatches = msq.getMatchingIdxsOfSMARTS(key);
            for (List<Integer> innerList : allMatches)
            {
            	if (innerList.size() != 2)
                {
                    logger.warn("WARNING! Query '" + key 
	                    + "' matched a number of atoms not equal to "
	                    + "two. Match " + innerList + " ignored!");
                    continue;
                }

                IBond targetBond = mol.getBond(
                                        mol.getAtom(innerList.get(0)),
                                        mol.getAtom(innerList.get(1)));

                targetsForThisKey.add(targetBond);
            }
            targets.put(key, targetsForThisKey);
        }

        return targets;
    }

//------------------------------------------------------------------------------


    /**
     * Mutate bonds in a container.
     * @param iac the container to mutate.
     * @param smarts the SMARTS matching the bonds to mutate, or the pair of 
     * atoms between which to define a bond (if none is already present). 
     * In the latter case the SMARTS should be separated by a space.
     * @param newFeatureValue map defining with to do or edit in the matches 
     * bonds. Depending on the type of the value, the matched bonds may be set 
     * to have a new value of {@link IBond.Order}, {@link IBond.Stereo}, or
     * be removed.
     * @return a new container.
     */

    public static void editBonds(IAtomContainer iac, Map<String,String> smarts, 
    		Map<String,Object> newFeatureValue)
    {
    	Logger logger = LogManager.getLogger();
    	
    	Map<String,String> atomPairSmarts = new HashMap<String,String>();
    	Map<String,String> bondSmarts = new HashMap<String,String>();
    	for (Entry<String, String> e : smarts.entrySet())
    	{
    		if (e.getValue().contains(" "))
    			atomPairSmarts.put(e.getKey(), e.getValue());
    		else
    			bondSmarts.put(e.getKey(), e.getValue());
    	}

    	// Add new bonds, if not already present
    	Map<String, List<MatchingIdxs>> atomPairs = identifyAtomIdxTuplesBySMARTS(iac, 
    			atomPairSmarts);
    	for (String key : atomPairs.keySet())
  		{
    		IBond.Order bo = IBond.Order.SINGLE;
    		if (newFeatureValue.containsKey(key) 
    				&& newFeatureValue.get(key) instanceof IBond.Order)
    		{
    			bo = (IBond.Order) newFeatureValue.get(key);
    		}
    		List<MatchingIdxs> pair = atomPairs.get(key);
    		if (pair.size() != 2)
    		{
    			logger.warn("Match of SMARTS tuple '" + key 
    					+ "' is not a pair. Skipped.");
    			continue;
    		}
	    		for (List<Integer> idxGroupA : pair.get(0))
	    		{
	    			int idxA = idxGroupA.get(0);
	    			IAtom atmA = iac.getAtom(idxA);
	    			for (List<Integer> idxGroupB : pair.get(1))
		    		{
		    			int idxB = idxGroupB.get(0);
	    				if (idxA==idxB)
	    					continue;
		    			IAtom atmB = iac.getAtom(idxB);
		    			if (!iac.getConnectedAtomsList(atmA).contains(atmB))
		    			{
		    				IBond bnd = new Bond(atmA, atmB, bo);
		    				iac.addBond(bnd);
		    			}
		    		}
	    		}
  		}
    	
    	// Alter previously existing bonds
    	Map<String,List<IBond>> targets = identifyBondsBySMARTS(iac, bondSmarts);

		List<IBond> toRemove = new ArrayList<IBond>();
  		for (String key : targets.keySet())
  		{
  			Object newFeature = newFeatureValue.get(key);
  			if (newFeature instanceof IBond.Order) {
  				for (IBond bnd : targets.get(key))
  				{
  					bnd.setOrder((IBond.Order) newFeature);
  				}
  			} else if (newFeature instanceof IBond.Stereo) {
  				for (IBond bnd : targets.get(key))
  				{
  					bnd.setStereo((IBond.Stereo) newFeature);
  				}
  			} else if (
  					(newFeature.toString().toUpperCase().equals("REMOVE")) 
  					||
  					(newFeature.toString().toUpperCase().equals("DELETE"))){
  				toRemove.addAll(targets.get(key));
  			} else {
  				logger.warn("No implementation is available for mutating "
  						+ "bond feature to '" + newFeature + "'.");
  			}
  		}
  		
  		for (IBond bnd : toRemove)
  		{
  			iac.removeBond(bnd);
  		}
    }

//------------------------------------------------------------------------------

}
