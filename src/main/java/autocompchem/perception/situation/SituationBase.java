package autocompchem.perception.situation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.BondReviser;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.InfoChannelTypeComparator;


/**
 * A list of situations.
 * 
 * @author Marco Foscato
 */

public class SituationBase
{
    /**
     * List of situations
     */
    private List<Situation> allSituations = new ArrayList<Situation>();

    /**
     * Indexing of situations by info channel type
     */
    private Map<InfoChannelType,List<Situation>> situationsByICType =
                          new HashMap<InfoChannelType,List<Situation>>();
    
    /**
     * Collection (by actual string query) of text queries in any circumstance 
     * contained here. 
     */
    private Map<String,TxtQuery> txtQueriesByQry = new HashMap<String,TxtQuery>();
    
    /**
     * Collection (by {@link InfoChannelType}) of text queries in any  
     * circumstance contained here. 
     */
    private Map<InfoChannelType,Set<TxtQuery>> txtQueriesByICT = 
    		new HashMap<InfoChannelType,Set<TxtQuery>>();
    
    /**
     * Logging tool
     */
    private Logger logger = LogManager.getLogger(SituationBase.class);
    

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty SituationBase
     */

    public SituationBase() 
    {}
    
//------------------------------------------------------------------------------

    /**
     * Creates a database of known situations from a collection of files.
     * Searches the given root folder and in its sub folders.
     * <b>WARNING:</b> we only look for files with a specific file extension 
     * ({@value SituationConstants.SITUATIONTXTFILEEXT}).
     * @param rootFolder root of the folder tree to be searched.
     */

    public SituationBase(File rootFolder)
    {
    	// WARNING: we only look for files with the expected formats
    	
        for (File f : FileUtils.find(rootFolder,"*" 
        		+ SituationConstants.SITUATIONTXTFILEEXT))
        {   
        	try {
				Situation s = (Situation) IOtools.readJsonFile(f, 
						Situation.class);
				this.addSituation(s);
			} catch (Exception e) {
				e.printStackTrace();
				logger.warn("WARNING! Unable to make a known Situation "
						+ "from file '" + f.getAbsolutePath() + "'. I ignore "
						+ "such file, but I keep going on.");
			}
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add a situation.
     * @param situation the situation to be included in this situation base.
     */

    public void addSituation(Situation situation)
    {
        allSituations.add(situation);        

        //Indexing by InfoChannelType
        for (InfoChannelType ict : situation.getInfoChannelTypes())
        {
            if (situationsByICType.keySet().contains(ict))
            {
                situationsByICType.get(ict).add(situation);
            }
            else
            {
                situationsByICType.put(ict,
                           new ArrayList<Situation>(Arrays.asList(situation)));
            }
        }
        
        //Collect any text query to make searching of strings more efficient
        for (ICircumstance circ : situation.getCircumstances())
        {
            if (circ instanceof MatchText)
            {
                String queryStr = ((MatchText) circ).getPattern();
            	InfoChannelType ict = circ.getChannelType();
            	
            	TxtQuery tq;
                if (txtQueriesByQry.keySet().contains(queryStr))
                {
                    tq = txtQueriesByQry.get(queryStr);
                    tq.addReference(situation, circ);
                } else {
                	tq = new TxtQuery(queryStr, situation, circ);
                	txtQueriesByQry.put(queryStr, tq);
                }
                
                if (txtQueriesByICT.containsKey(ict))
                {
                	txtQueriesByICT.get(ict).add(tq);
                } else {
                	Set<TxtQuery> tqs = new HashSet<TxtQuery>();
                	tqs.add(tq);
                	txtQueriesByICT.put(ict, tqs);
                }
            }
        }  
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the situation with the given index.
     */
    
    public Situation getSituation(int i)
    {
    	return allSituations.get(i);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the number of situations in this collection.
     * @return the number of known situations.
     */
    
    public int getSituationCount()
    {
    	return allSituations.size();
    }

//------------------------------------------------------------------------------

    /**
     * Extract situations that are relevant for a given information channel type
     * @param ict the type of information channel
     * @return the set of relevant situations
     */

    public List<Situation> getRelevantSituations(InfoChannelType ict)
    {
        return situationsByICType.get(ict);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the text queries that are embedded in any circumstance that
     * characterizes any situation contained in this collection of situations.
     * @param ict the type of information channel for which the queries are 
     * meant.
     */
    public Set<TxtQuery> getAllTxTQueriesForICT(InfoChannelType ict, 
    		boolean includeCompatibleICT)
    {
    	if (includeCompatibleICT)
    	{
    		Set<TxtQuery> result = new HashSet<TxtQuery>();
    		for (InfoChannelType candICT : txtQueriesByICT.keySet())
    		{
    			if (InfoChannelTypeComparator.checkCompatibility(ict, candICT))
    			{
    				result.addAll(txtQueriesByICT.get(candICT));
    			}
    		}
    		return result;
    	} else {
    		return txtQueriesByICT.get(ict);
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the text queries that are embedded in any circumstance that
     * characterizes any situation contained in this collection of situations.
     */
    public Map<String,TxtQuery> getAllTxTQueriesByQuery()
    {
    	return txtQueriesByQry;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the text queries that are embedded in any circumstance that
     * characterizes any situation contained in this collection of situations.
     */
    public Map<InfoChannelType, Set<TxtQuery>> getAllTxTQueriesByICT()
    {
    	return txtQueriesByICT;
    }

//------------------------------------------------------------------------------

    /**
     * Extract situations that are relevant for a given information channel base
     * @param icb the of information channels made available
     * @return the map of relevant situations by info channel type
     */

    public Map<InfoChannelType,List<Situation>> 
    	getRelevantSituationsByICT(InfoChannelBase icb)
    {
        Map<InfoChannelType,List<Situation>> relevantSituations = 
                           new HashMap<InfoChannelType,List<Situation>>();
        for (InfoChannelType ict : icb.getAllChannelType())
        {
            if (situationsByICType.keySet().contains(ict))
            {
                relevantSituations.put(ict,situationsByICType.get(ict));
            }
        }
        return relevantSituations;
    }

//------------------------------------------------------------------------------

    /**
     * Extract situations that are relevant for a given information channel base
     * @param icb the of information channels made available
     * @return the list of relevant situations
     */

    public List<Situation> getRelevantSituations(InfoChannelBase icb) 
    {
        List<Situation> relevantSituations = new ArrayList<Situation>();
        for (InfoChannelType ict : icb.getAllChannelType())
        {
            if (situationsByICType.keySet().contains(ict))
            {
                for (Situation s : situationsByICType.get(ict))
                {
                    if (!relevantSituations.contains(s))
                    {
                        relevantSituations.add(s);
                    }
                }
            }
        }
        return relevantSituations;
    }

//------------------------------------------------------------------------------

    /**
     * Prints the situations grouped by {@link InfoChannelType}. 
     * Prints on STDOUT
     */

    public void printSituationsByICT()
    {
        String newline = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append(newline);
        sb.append("Situations by InfoChannelType:").append(newline);
        for (InfoChannelType ict : situationsByICType.keySet())
        {
            sb.append(" -> ").append(ict);
            sb.append(" = ").append(situationsByICType.get(ict));
            sb.append(newline);   
        }
        System.out.println(sb.toString());
    }

//------------------------------------------------------------------------------

    /**
     * Return a string describing this knowledge
     * @return a human readable description of the knowledge
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SituationBase [");
        for (Situation s : allSituations)
        {
            sb.append(s.toString()).append("; ");
        }
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
