package autocompchem.molecule.conformation;

/*   
 *   Copyright (C) 2016  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;


/**
 * The conformational space as the combination of a list of conformational
 * changes (i.e., the conformational coordinates).
 * 
 * @author Marco Foscato 
 */

public class ConformationalMovesDefinition
{
    /**
     * Storage of named SMARTS queries identifying the foldable moieties
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Storage of details/options for SMARTS queries identifying the foldable 
     * moieties 
     */
    private Map<String,ArrayList<String>> smartsOpts =
                                        new HashMap<String,ArrayList<String>>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ConformationalMovesDefinition
     */

    public ConformationalMovesDefinition()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Add a single definition of a conformational move. 
     * @param refName name of the rule
     * @param smarts the SMARTS queries defining the foldable moiety
     * @param opts the details/otions assiciated with this move
     */

    public void addMoveDefinition(String refName, String smarts, 
                                                         ArrayList<String> opts)
    {
        this.smarts.put(refName,smarts);
        this.smartsOpts.put(refName,opts);
    }

//------------------------------------------------------------------------------

    /**
     * Get na iterator over the reference names
     * @return the iterator
     */

    public Set<String> refNames()
    {
        return smarts.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Get the SMARTS query with a given reference name
     * @param refName the reference mane
     * @return the SMARTS query
     */

    public String getSMARTS(String refName)
    {
        return smarts.get(refName);
    }

//------------------------------------------------------------------------------

    /**
     * Get the map of named SMARTS queries
     * @return the map of SMARTS queries
     */

    public Map<String,String> getSMARTSQueries() 
    {
	return smarts;
    }

//------------------------------------------------------------------------------

    /**
     * Get the options associated to a SMARTS query with a given reference name
     * @param refName the reference mane
     * @return the options
     */

    public ArrayList<String> getOpts(String refName)
    {
        return smartsOpts.get(refName);
    }

//------------------------------------------------------------------------------

    /**
     * Get the map of options for named SMARTS queries
     * @return the map of options for SMARTS queries
     */

    public Map<String,ArrayList<String>> getSMARTSQueriesOpts()
    {
        return smartsOpts;
    }

//------------------------------------------------------------------------------

    /**
     * Get a string representation 
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ConformationalMovesDefintition:");
        sb.append(System.getProperty("line.separator"));
        for (String key : smarts.keySet())
        {
             sb.append(" RefName=").append(key);
             sb.append(" SMARTS=").append(smarts.get(key));
             sb.append(" options=").append(smartsOpts.get(key));
             sb.append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------
}
