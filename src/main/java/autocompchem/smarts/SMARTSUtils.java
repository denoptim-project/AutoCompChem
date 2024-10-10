package autocompchem.smarts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;

/**
 * Utilities for using and dealing with SMARTS.
 */
public class SMARTSUtils 
{

    /**
     * Static logger
     */
    protected static Logger logger = LogManager.getLogger(SMARTSUtils.class);

//-----------------------------------------------------------------------------

    /**
     * Look for the atoms defined by the given list of SMARTS.
     * @param mol the molecule to work with.
     * @param smarts the list of smarts queries with reference names. The latter
     * are used to group the hits so that you can easily find out which SMARTS/s
     * matched a specific atom.
     * @return the list of matched atoms grouped by reference name of the 
     * given SMARTS queries.
     */

    public static Map<String, List<List<List<IAtom>>>> identifyAtomTuples(
    		IAtomContainer mol, Map<String, List<SMARTS>> smartsTuples)
    {
    	Map<String,List<MatchingIdxs>> matchesAsIdxs = 
    			identifyAtomIdxTuples(mol, smartsTuples);
    	
    	Map<String, List<List<List<IAtom>>>> matchesAsObjs = 
    			new HashMap<String, List<List<List<IAtom>>>>();
    	for (String smartaKey : matchesAsIdxs.keySet())
    	{
    		List<MatchingIdxs> matchesForSMARTSTuple = matchesAsIdxs.get(
    				smartaKey);
    		List<List<List<IAtom>>> atmMatchesForSMARtTuple =
    				new ArrayList<List<List<IAtom>>>();
    		for (MatchingIdxs idxsOneEntryInTuple : matchesForSMARTSTuple)
    		{
    			List<List<IAtom>> atmsMatchingOneEntryInTuple = 
    					new ArrayList<List<IAtom>>();
    			for (List<Integer> atmIdxsMatchingSMARTS : idxsOneEntryInTuple)
    			{
    				List<IAtom> atmsMatchingSMARTS = new ArrayList<IAtom>();
    				for (Integer atmIdx : atmIdxsMatchingSMARTS)
    				{
    					atmsMatchingSMARTS.add(mol.getAtom(atmIdx));
    				}
    				atmsMatchingOneEntryInTuple.add(atmsMatchingSMARTS);
    			}
    			atmMatchesForSMARtTuple.add(atmsMatchingOneEntryInTuple);
    		}
    		matchesAsObjs.put(smartaKey, atmMatchesForSMARtTuple);
    	}
    	return matchesAsObjs;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Look for the atoms defined by the given list of SMARTS.
     * @param mol the molecule to work with.
     * @param smarts the list of smarts queries with reference names. The latter
     * are used to group the hits so that you can easily find out which SMARTS/s
     * matched a specific atom.
     * @return the list of matched atoms grouped by reference name of the 
     * given SMARTS queries.
     */

    public static Map<String, List<MatchingIdxs>> identifyAtomIdxTuples(
    		IAtomContainer mol, Map<String, List<SMARTS>> smartsTuples)
    {
    	// Here we collect all atom tuples by the reference name of
    	// the tuple of SMARTS given as parameter
        Map<String,List<MatchingIdxs>> allIDsForEachTuple =
                new HashMap<String,List<MatchingIdxs>>();
    	
    	// Extract single SMARTS from tuples of SMARTS, but keep track of
    	// the tuples via the reference names
    	Set<String> sortedKeys = new TreeSet<String>();
    	Map<String,SMARTS> smarts = new HashMap<String,SMARTS>();
        for (Entry<String,List<SMARTS>> tupleRule : smartsTuples.entrySet())
        {
        	String key = tupleRule.getKey();
    		sortedKeys.add(key);
    		for (int i=0; i<tupleRule.getValue().size(); i++)
        	{
        		String refName = key + "_" + i;
        		smarts.put(refName, tupleRule.getValue().get(i));
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
        	logger.warn("No SMARTS among " + StringUtils.mergeListToString(
        		Arrays.asList(smarts.keySet()), ", ") 
        		+ " matched anything.");
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
            			+ "' ('" + smarts.get(key).getString()
            			+ "') did not match anything.");
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

//------------------------------------------------------------------------------

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
    		IAtomContainer mol, Map<String, SMARTS> smarts)
    {
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

}
