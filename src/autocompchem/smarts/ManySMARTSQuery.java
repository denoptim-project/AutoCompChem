package autocompchem.smarts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.Map;

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

    //Container
    private Map<String,Mappings> allMatches = new HashMap<String,Mappings>();
    
    //Counts
    private int totNum;
    private Map<String,Integer> numMatches = new HashMap<String,Integer>();

    //Problems
    private boolean problems = false;
    private String message = "";

//------------------------------------------------------------------------------

    /**
     * Constructs a new ManySMARTSQuery specifying all the parameters needed
     * @param mol the target molecule
     * @param smarts map of SMARTS (with reference names as keys)
     * @param verbosity level of verbosity
     */

    public ManySMARTSQuery(IAtomContainer mol, Map<String,String> smarts, int verbosity)
    {
        totNum = 0;
    	for (String smartsRef : smarts.keySet()) {
            String oneSmarts = smarts.get(smartsRef);

            if (verbosity >= 3)
            {
                System.out.println("Attempt to match query '" 
                                        + smartsRef + "'.");
                System.out.println("SMARTS: " + oneSmarts);
            }

            Pattern sp = SmartsPattern.create(oneSmarts);
            
            // This is required as from CDK-2.3 (or lower, anyway > 1.4.14)
            MolecularUtils.setZeroImplicitHydrogensToAllAtoms(mol);
            MolecularUtils.ensureNoUnsetBondOrders(mol);
            
            if (sp.matches(mol))
            {
                Mappings listOfIds = sp.matchAll(mol);
                allMatches.put(smartsRef,listOfIds);

//CDK BUG here! this number is somehow wrong
//                        int num = query.countMatches();
                int num = 0;
                Iterator<int[]> iter = listOfIds.iterator();
                while (iter.hasNext()) {
                	iter.next();
                	num++;
                }
                
                numMatches.put(smartsRef,num);
                totNum = totNum + num;
                if (verbosity >= 2)
                {
                    System.out.println("Matches for query '" + smartsRef
                        + "': " + num + " => Atoms: " + getStringFor(listOfIds));
                }
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
     * Return all the matches for the specified SMARTS query
     * @param ref the reference name of the SMARTS query
     * @return the list of matches for the specified query
     */

    public Mappings getMappingOfSMARTS(String ref)
    {
    	return allMatches.get(ref);
    }

//------------------------------------------------------------------------------

    /**
     * Return all the matches for the specified SMARTS query
     * @param ref the reference name of the SMARTS query
     * @return the list of matches for the specified query
     * @deprecated use {@link getMappingOfSMARTS}
     */

    @Deprecated
    public List<List<Integer>> getMatchesOfSMARTS(String ref)
    {
    	List<List<Integer>> idsAsNestedList = new ArrayList<List<Integer>>();
    	Iterator<int[]> iter = allMatches.get(ref).iterator();
    	while (iter.hasNext()) {
    		int[] m = iter.next();
    		List<Integer> l = new ArrayList<Integer>();
    		for (int i=0; i<m.length; i++)
    			l.add(m[i]);
    		idsAsNestedList.add(l);
    	}
        return idsAsNestedList;
    }

//------------------------------------------------------------------------------

}
