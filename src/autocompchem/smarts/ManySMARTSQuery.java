package autocompchem.smarts;

import java.util.ArrayList;
import java.util.HashMap;

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

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * SMARTS query tool capable of handling many SMARTS queries in once. 
 *
 * @author Marco Foscato
 */


public class ManySMARTSQuery
{

    //Container
    private Map<String,List<List<Integer>>> allMatches = 
                                new HashMap<String,List<List<Integer>>>();
    //Counts
    private int totNum;
    private Map<String,Integer> numMatches = new HashMap<String,Integer>();

    //Problems
    private boolean problems = false;
    private String message = "";

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ManySMARTSQuery tool
     */

    public ManySMARTSQuery()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a new ManySMARTSQuery specifying all the parameters needed
     * @param mol the target molecule
     * @param smarts map of SMARTS (with reference names as keys)
     * @param verbosity level of verbosity
     */

    public ManySMARTSQuery(IAtomContainer mol, Map<String,String> smarts, int verbosity)
    {
        super();
        totNum = 0;
        String blankSmarts = "[*]";

        String err="";

        try {
                SMARTSQueryTool query = new SMARTSQueryTool(blankSmarts);
                for (String smartsRef : smarts.keySet())
                {
                    //get the new query
                    String oneSmarts = smarts.get(smartsRef);
                    err = smartsRef;

                    if (verbosity >= 3)
                    {
                        System.out.println("Attempt to match query '" 
                                                + smartsRef + "'.");
                        System.out.println("SMARTS: " + oneSmarts);
                    }

                    //Update the query tool
                    query.setSmarts(oneSmarts);

                    
                    if (query.matches(mol))
                    {
                        //Store matches
                        List<List<Integer>> listOfIds = 
                                                 new ArrayList<List<Integer>>();
                        listOfIds = query.getUniqueMatchingAtoms();
                        allMatches.put(smartsRef,listOfIds);
                        //Store number
//CDK BUG here! this number is somehow wrong
//                        int num = query.countMatches();
                        int num = listOfIds.size();
                        numMatches.put(smartsRef,num);
                        totNum = totNum + num;
                        if (verbosity >= 2)
                        {
                            System.out.println("Matches for query '" + smartsRef
                                + "': " + num + " => Atoms: " + listOfIds);
                        }
                     }
                }
        } catch (CDKException cdkEx) {
                if (verbosity > 1)
                    cdkEx.printStackTrace();
                String cause = cdkEx.getCause().getMessage();
                err = "\nWARNING! For query " + err + " => " + cause;
                problems = true;
                message = err;
        } catch (Throwable t) {
                java.lang.StackTraceElement[] stes = t.getStackTrace();
                String cause = "";
                int s = stes.length;
                if (s >= 1)
                {
                    java.lang.StackTraceElement ste = stes[0];
                    cause = ste.getClassName();
                } else {
                    cause = "'unknown' (try to process this molecule alone to "
                                                            + "get more infos)";
                }
                err = "\nWARNING! For query " + err + " => Exception returned "
                                                                + "by " + cause;
                problems = true;
                message = err;
        }
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
     * @return the number of mathces for the specified query
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
     * Returnn all the matches for the specified SMARTS query
     * @param ref the reference name of the SMARTS query
     * @return the list of mathced for the specified query
     */

    public List<List<Integer>> getMatchesOfSMARTS(String ref)
    {
        return allMatches.get(ref);
    }

//------------------------------------------------------------------------------

}
