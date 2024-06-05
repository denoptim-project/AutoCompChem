package autocompchem.smarts;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.smarts.SmartsPattern;

import autocompchem.molecule.MolecularUtils;

/**
 * SMARTS query tool capable of handling many SMARTS queries in once. 
 *
 * @author Marco Foscato
 */


public class ManySMARTSQuery
{
    /**
     * Logger
     */
    protected static Logger logger = LogManager.getLogger(ManySMARTSQuery.class);

    /**
     * Container for matches
     */
    private Map<String,Mappings> allMatches = new HashMap<String,Mappings>();
    
    /**
     * total number of matches
     */
    private int totNum;
    
    /**
     * Matches per smarts key
     */
    private Map<String,Integer> numMatches = new HashMap<String,Integer>();

    /**
     * flag that is true if we encountered a problem
     */
    private boolean problems = false;
    
    /**
     * Text that may be useful to understand any problem encountered.
     */
    private String message = "";
    

//------------------------------------------------------------------------------

    /**      
     * Constructs a new ManySMARTSQuery specifying all the parameters needed
     * @param mol the target molecule
     * @param smarts map of SMARTS (with reference names as keys)
     * @param verbosity level of verbosity
     */

    @Deprecated
    public ManySMARTSQuery(IAtomContainer mol, Map<String,String> smarts, int verbosity)
    {
    	this(mol, smarts);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a new ManySMARTSQuery specifying all the parameters needed.
     * @param mol the target molecule.
     * @param smarts map of SMARTS (with reference names as keys).
     */

    public ManySMARTSQuery(IAtomContainer mol, Map<String,String> smarts)
    {	
        totNum = 0;
    	for (String smartsRef : smarts.keySet()) 
    	{
            String oneSmarts = smarts.get(smartsRef);

            logger.debug("Attempt to match query '" + smartsRef + "': " 
            		+ oneSmarts);
            
            Pattern sp = SmartsPattern.create(oneSmarts);
            
            // This is required as from CDK-2.3 (or lower, anyway > 1.4.14)
            MolecularUtils.setZeroImplicitHydrogensToAllAtoms(mol);
            MolecularUtils.ensureNoUnsetBondOrders(mol);
            
            if (sp.matches(mol))
            {
                Mappings listOfIds = sp.matchAll(mol);
                int num = listOfIds.count();
                allMatches.put(smartsRef, listOfIds);
                numMatches.put(smartsRef, num);
                
                totNum = totNum + num;
                logger.debug("Matches for query '" + smartsRef + "': " + num 
                		+ " => Atoms: " + getStringFor(listOfIds));
            }
        }
    }

//------------------------------------------------------------------------------

	private String getStringFor(Mappings listOfIds)
    {
    	String matchesString = "";
    	Iterator<int[]> it = listOfIds.iterator();
    	while (it.hasNext())
    	{
    		int[] list = it.next();
    		for (int i=0; i<list.length; i++)
    		{
    			if (i==0)
    			{
    				matchesString = matchesString + "[" + list[i];
    			} else {
    				matchesString = matchesString + ", " + list[i];
    			}
    		}
    		matchesString = matchesString + "]";
    	}
    	return matchesString;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if a problem has been detected during the
     * execution of the ManySMARTSQuery tool. 
     */

    public boolean hasProblems()
    {
        return problems;
    }

//------------------------------------------------------------------------------

    /**
     * Return a message that may help understanding errors.
     * @return the message
     */

    public String getMessage()
    {
        return message;
    }

//------------------------------------------------------------------------------

    /**
     * Return the total number of matches 
     * @return the number of matches
     */

    public int getTotalMatches()
    {
        return totNum;
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of matches for all SMARTS queries in the form
     * referenceName:numOfMatches
     * @return the map with the number of matches per each SMARTS query
     */

    public Map<String,Integer> getNumMatchesMap()
    {
        return numMatches;
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of matches for the specified SMARTS query 
     * @param queryName the reference name of the SMARTS query
     * @return the number of matches for the specified query
     */

    public int getNumMatchesOfQuery(String queryName)
    {
        if (numMatches.keySet().contains(queryName))
            return numMatches.get(queryName);
        else
            return 0;
    }

//------------------------------------------------------------------------------

    /**
     * Check is a specified SMARTS query matches something
     * @param query the reference name of the SMARTS query
     * @return <code>true</code> if the specified query has matches 
     */

    public boolean hasMatches(String query)
    {
        if (numMatches.keySet().contains(query))
            return true;
        else
            return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Return all the matches for the specified SMARTS query.
     * @param ref the reference name of the SMARTS query.
     * @return the list of matches for the specified query.
     */

    public Mappings getMappingOfSMARTS(String ref)
    {
    	return allMatches.get(ref);
    }

//------------------------------------------------------------------------------

    /**
     * Return all the matches for the specified SMARTS query.
     * @param ref the reference name of the SMARTS query.
     * @return the list of matches for the specified query.
     */

    public MatchingIdxs getMatchingIdxsOfSMARTS(String ref)
    {
    	MatchingIdxs matches = new MatchingIdxs();
    	Iterator<int[]> iter = allMatches.get(ref).iterator();
    	while (iter.hasNext()) {
    		int[] m = iter.next();
    		List<Integer> l = new ArrayList<Integer>();
    		for (int i=0; i<m.length; i++)
    			l.add(m[i]);
    		matches.add(l);
    	}
        return matches;
    }

//------------------------------------------------------------------------------

}
