package autocompchem.perception;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.situation.Situation;

/**
 * Data structure that stores the scores made while perceiving situations.
 * 
 * @author Marco Foscato
 */

public class ScoreCollector
{
    /**
     * Store scores for each pair Situation:ICircumstance
     */
    private Map<SCPair,Double> scores;

//--------------------------------------------------------------------------

    /**
     * Constructor for an empty Collector
     */
    public ScoreCollector()
    {
        scores = new HashMap<SCPair,Double>();
    }

//--------------------------------------------------------------------------

    /**
     * Returns the score for a given key
     * @param k the key
     * @return the score or null if not found
     */
    public double getScore(SCPair k)
    {
        return  scores.get(k);
    }

//--------------------------------------------------------------------------

    /**
     * Increase the score for a specific Situation and ICircumstance
     */
    public void addScore(Situation n, ICircumstance c, double score)
    {
        SCPair scp = new SCPair(n,c);
        addScore(scp,score);
    }

//--------------------------------------------------------------------------

    /**
     * Increase the score for a specific Situation:ICircumstance pair
     */
    public void addScore(SCPair scp, double score)
    {
        SCPair existingSCPair = keyWithSameSCValues(scp);
        if (existingSCPair != null)
        {
            scores.put(existingSCPair,scores.get(existingSCPair) + score);
        }
        else
        {
            scores.put(scp,score);
        }
    }

//--------------------------------------------------------------------------

    /**
     * Check whether a given combination of Situation and ICircumstance is
     * already used as key
     * @param q the SCPair to compare with the existing pairs
     * @return the key that has the same pair of values
     */
    public SCPair keyWithSameSCValues(SCPair q)
    {
        //NB: the contains method is not good because it distinguishes 
        //different objects with same Situationand and ICircumstance
        //Don't do this here: scores.keySet().contains(q)

        SCPair res = null;
        for (SCPair k : scores.keySet())
        {
            if (0 == k.compareTo(q))
            {
                res = k;
                break;
            }
        } 
        return res;
    }

//--------------------------------------------------------------------------

    /**
     * Prepares an array with all the satisfation scores for a given
     * situation.
     * @param s the situation to consider
     * @param fingerprint the ordered list of scores
     */

    public ArrayList<Boolean> getSatisfationFingerprint(Situation s)
    {
        ArrayList<Boolean> fp = new ArrayList<Boolean>();        
        for (ICircumstance c : s.getCircumstances())
        {
            SCPair key = this.keyWithSameSCValues(new SCPair(s,c));
            if (key != null)
            {
                double dScore = this.getScore(key);
                 Boolean bScore = c.scoreToDecision(dScore);
                fp.add(bScore);
            }
        }

        return fp;
    }
    
//--------------------------------------------------------------------------
    
    /**
     * Utility for printing the scores table
     * @return a multiline string that can be printed on stout
     */

    public String printToString()
    {	
        String newline = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append("====================");
        sb.append(" Scores collection ====================");
        sb.append(newline);
        sb.append("Situation IDs and details:").append(newline);
        Set<Situation> sKeys = new HashSet<Situation>();
        for (SCPair scp : scores.keySet())
        {
            sKeys.add(scp.getSituation());
        }
        List<String> sortedSituations = new ArrayList<String>();
        for (Situation s : sKeys)
        {
        	sortedSituations.add(" -s-> " + Integer.toHexString(s.hashCode()) +
        			": " + s + newline);
        }
        Collections.sort(sortedSituations);
        for (String str : sortedSituations)
        {
            sb.append(str);
        }
        
        sb.append(newline);
        sb.append("Circumstance IDs and details:").append(newline);
        Set<ICircumstance> cKeys = new HashSet<ICircumstance>();
        for (SCPair scp : scores.keySet())
        {
            cKeys.add(scp.getCircumstance());
        }
        List<String> sortedCircs = new ArrayList<String>();
        for (ICircumstance c : cKeys)
        {
        	sortedCircs.add(" -c-> " + Integer.toHexString(c.hashCode()) + 
        			" = " + c + newline);
        }
        Collections.sort(sortedCircs);
        for (String str : sortedCircs)
        {
            sb.append(str);
        }


        sb.append("==========================");
        sb.append(" Scores =========================");
        sb.append(newline);
        List<String> sortedScores = new ArrayList<String>();
        for (SCPair scp : scores.keySet())
        { 
        	sortedScores.add(scp.toIDString() + " -> " + scores.get(scp) + 
        			newline);
        }
        Collections.sort(sortedScores);
        for (String str : sortedScores)
        {
            sb.append(str);
        }
        sb.append("=================");
        sb.append(" End of Score Collection =================");
        sb.append(newline);
        return sb.toString();
    }

//--------------------------------------------------------------------------

}
