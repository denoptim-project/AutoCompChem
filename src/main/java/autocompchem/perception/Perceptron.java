package autocompchem.perception;

import java.io.BufferedReader;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.InfoChannelTypeComparator;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;

/**
 * Perceptron is a neuron that collects information from a given list of 
 * information channels and compares it against
 * a given collection of possible situations, thus becoming aware of the 
 * presently occurring situation.
 * 
 * @author Marco Foscato
 */

public class Perceptron
{
    /**
     * Situation base: list of known situations/concepts
     */
    private SituationBase sitsBase; 

    /**
     * Information channels base: list of available information channels
     */
    private InfoChannelBase icb;

    /**
     * Data structure collecting actual satisfaction scores
     */
    private ScoreCollector scoreCollector;

    /**
     * Result/s of the perception
     */
    private ArrayList<Situation> occurringSituations;

    /**
     * Flag remembering the status of awareness
     */
    private boolean iamaware = false;

    /**
     * Verbosity level
     */
    private int verbosity = 0;

    /**
     * New line character (for logging)
     */
    private final String newline = System.getProperty("line.separator");


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty Perceptron
     */

    public Perceptron() 
    {
        this.scoreCollector = new ScoreCollector();
        this.occurringSituations = new ArrayList<Situation>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for Perceptron with given situation base and base of 
     * information channels
     */

    public Perceptron(SituationBase sitsBase, InfoChannelBase icb)
    {
        this.sitsBase = sitsBase;
        this.icb = icb;
        this.scoreCollector = new ScoreCollector();
        this.occurringSituations = new ArrayList<Situation>();
    }

//------------------------------------------------------------------------------

    /** 
     * Set the verbosity level to the given number
     * @param l the given verbosity level
     */

    public void setVerbosity(int l)
    {
        this.verbosity = l;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the awareness status of this perceptron
     * @return <code>true</code> if this perceptron has perceives and identified
     * the situation. 
     */

    public boolean isAware()
    {
        return iamaware;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a clone of the list of matched situations.
     * This method cannot be used to alter the results of the perception.
     * @return the number of  situations that were matches by the circumstances.
     */

    public int getOccurringSituationsCount()
    {
        return occurringSituations.size();
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns a clone of the list of matched situations.
     * This method cannot be used to alter the results of the perception.
     * @return the list of matched situations 
     */

    public ArrayList<Situation> getOccurringSituations()
    {
        ArrayList<Situation> copy = new ArrayList<Situation>();
        copy.addAll(occurringSituations);
        return copy;
    }

//------------------------------------------------------------------------------

    /**
     * Print perception scores on stout
     */

    public void printScores()
    {
        System.out.println(scoreCollector.printToString());
    }

//------------------------------------------------------------------------------

    /**
     * Perception of situation that is occurring and do it based on the 
     * available information channels and situation base.
     */

    public void perceive() throws Exception
    {
        if (verbosity > 1)
        {
            System.out.println(newline + "Perception from " + this.hashCode());
        }

        //To read text files only once we run all text-matching queries in once
        analyzeAllText();

        if (verbosity > 3)
        {
            System.out.println(newline + "Scores after text-only");
            System.out.println(scoreCollector.printToString());
        }

        //Do actual perception
        for (Situation s : sitsBase.getRelevantSituations(icb))
        {
            // Check if each circumstance is verified.
            for (ICircumstance c : s.getCircumstances())
            {
                evaluateOneCircumstance(s,c);
            }

            // Collect all the satisfaction scores
            ArrayList<Boolean> fp = scoreCollector.getSatisfationFingerprint(s);

            // Calculate logical result from all the circumstances
            // and remember the situation/s that are occurring (i.e., those
            // for which all the circumstances are verified)
            if (s.isOccurring(fp))
            {
                occurringSituations.add(s);
            }
        }

        if (verbosity > 3)
        {
            System.out.println(newline +"Final Scores");
            System.out.println(scoreCollector.printToString());
        }

        String init = "Perception result: ";
        switch (occurringSituations.size())
        {
            case 0:
                if (verbosity > 1)
                {
                    System.out.println(init + "Unknown situation");
                }
                break;
            case 1:
                iamaware = true;
                if (verbosity > 1)
                {
                    System.out.println(init + "Known situation is " 
                                                  + occurringSituations.get(0));
                }
                break;
           default:
                if (verbosity > 1)
                {
                	StringBuilder sb = new StringBuilder();
                	sb.append(init + "Confusion - The situation matches "
                			+ "multiple known situation. You may have to make "
                            + "the situations more specific as to "
                            + "disctiminate between these: ");
                	for (Situation s : occurringSituations)
                	{
                		sb.append(s.getRefName()).append(" ");
                	}
                    System.out.println(sb.toString());
                }
                break;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates if a circumstance is satisfied. This recalls previous 
     * evaluations, or runs the evaluation if the result is not already
     * available.
     * @param n the Situation owning the ICircumstance this evaluation
     * @param c the actual ICircumstance to be evaluated 
     */

    private void evaluateOneCircumstance(Situation n, ICircumstance c)
    {
        SCPair oldKey = scoreCollector.keyWithSameSCValues(new SCPair(n,c));

        if (oldKey != null)
        {
            //Previous evaluation exists in the scores collector. Nothing to do.
        } else {
            SCPair scp = new SCPair(n,c);

            if (verbosity > 3)
            {
                System.out.println("-?-> Evaluating SCPair: " 
                                                            + scp.toIDString());
                if (verbosity > 4)
                {
                    System.out.println("     Child ICircumstance: "+c);
                    System.out.println("     Parent Situation: " + n);
                }
            }

            //evaluate circumstance now
            double score = 0.0;

            //TODO: write method
//TODO del
System.out.println("has txt: " + c.requiresTXTMatch());
System.out.println("     Child ICircumstance: "+c);
System.out.println("     Parent Situation: " + n);
this.printScores();
            String msg = "ERROR! Evaluation of circumstances not based on text "
            		+ "is still to be implemented.";
            Terminator.withMsgAndStatus(msg,-1);

            scoreCollector.addScore(scp, score);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Perform all text matching in once. This method encapsulates all the 
     * handling of text-matching queries. Different Situations may require 
     * to analyse the text of the same text-based source of information. Thus,
     * to avoid reading the same source multiple times, we collect all the
     * text queries and analyses meant for a single source and read the source 
     * only one time. 
     * <b>WARNING: currently supporting only file-based InfoChannels.</b>
     * <b>WARNING: assuming all text-queries are single line. No new-line!</b>
     */

    private void analyzeAllText()
    {
        //Sort info source by type.
        Map<InfoChannelType,List<Situation>> situationsByICType = 
        		sitsBase.getRelevantSituationsByICT(icb);

//TODO del
//System.out.println("The relevant Situations are:");
//System.out.println(sitsBase.getRelevantSituationsByICT(icb));

/*        
// Define the max theoretical score for each Situation (it depends on
// the number of info channels so, cannot be known a priory),
//        maxScoreCollector = new ScoreCollector();
        Set<Situation> relevantSituations = HashSet<Situation>();
        for (InfoChannelType ict : icb.getAllChannelType())
        {
            relevantSituations.addAll(sitsBase.getRelevantSituationsByICT(ict));
            for (InfoChannel ic : icb.getChannelsOfType(ict))
            {
                for (Situation n : sitsBase.getRelevantSituationsByICT(ict))
                {
                    for (ICircumstance : n.getCircumstances())
                    {
                        if (c.getChannelType() == ict)
                        {
                            maxScoreCollector.addScore(n,1.0);
                        }
                    }
                }                
            }
        }
*/


        // Explore all the information channels by type, so that we can take all
        // the Circumstances that involve such channel type
        for (InfoChannelType ict : situationsByICType.keySet())
        {
            if (verbosity > 2)
            {
                System.out.println(newline+newline+"InfoChannelType: "+ict);
            }
            
            List<TxtQuery> txtQueries = new ArrayList<TxtQuery>(
            		sitsBase.getAllTxTQueriesForICT(ict, true));
            List<String> txtQueriesAsStr = new ArrayList<String>();
            txtQueries.stream().forEach(tq -> txtQueriesAsStr.add(tq.query));

            // Scan all the input channels of the present type
            for (InfoChannel ic : icb.getChannelsOfType(ict))
            {
                if (verbosity > 2)
                {
                    System.out.println(newline +"Scanning InfoChannel: "+ic);
                }

                BufferedReader br = null;
                TreeMap<String,List<String>> allMatches = null;
                try 
                {
                    br = new BufferedReader(ic.getSourceReader());

                    // Extract potentially useful information from text
                    // WARNING!!! No attempt to match multiline blocks here!
                    allMatches = 
                    		TextAnalyzer.extractMapOfTxtBlocksWithDelimiters(br,
                                                        txtQueriesAsStr,
                                                        new ArrayList<String>(),
                                                        new ArrayList<String>(),
                                                        false,
                                                        true);
                } catch (Exception e) {
                    String msg = "ERROR reading InfoChannel "+ic;
                    Terminator.withMsgAndStatus(msg,-1);
                } finally {
                    if (br != null)
                    {
                        try
                        {
                            br.close();
                        } catch (Exception e) {
                            String msg = "ERROR closing InfoChannel "+ic;
                            Terminator.withMsgAndStatus(msg,-1);
                        }
                    }
                }

                // Identify the queries that have matches
                Map<Integer,ArrayList<String>> textQueriesWMatches = 
                                       new HashMap<Integer,ArrayList<String>>();
                for (String strQuery : allMatches.keySet())
                {
                    // The matches are returned with a key that contains
                    // numerical indexes a_b_c.  For details,
                    // see TextAnalyzer.extractMapOfTxtBlocksWithDelimiters.
                    // First, we deal with the indexes
                    String[] parts = strQuery.split("_");
                    //...and get the index of the query given to FileAnalyzer
                    int qID = Integer.parseInt(parts[1]); // the 'b'
                    if (textQueriesWMatches.keySet().contains(qID))
                    {
                        textQueriesWMatches.get(qID).add(strQuery);
                    }
                    else
                    {
                        ArrayList<String> strQryKeys = new ArrayList<String>();
                        strQryKeys.add(strQuery);
                        textQueriesWMatches.put(qID,strQryKeys);
                    }
                }

                // Process matched and unmatched queries
                for (int qID = 0; qID < txtQueriesAsStr.size(); qID++)
                {
                    TxtQuery tq = txtQueries.get(qID);
                    if (verbosity > 4)
                    {
                        System.out.println("Result for text query "+tq);
                    }

                    ArrayList<String> matches = new ArrayList<String>(0);
                    if (textQueriesWMatches.keySet().contains(qID))
                    {
                        // This text query has been matched 
                        for (String strQuery : textQueriesWMatches.get(qID))
                        {
                            matches.addAll(allMatches.get(strQuery));
                        }

                        if (verbosity > 3)
                        {        
                             System.out.println("Matches for text query '" 
                            		 + txtQueriesAsStr.get(qID) + "' = "
                            		 + matches.size() + ". Lines:");
                            for (String m : matches)
                            {
                                System.out.println("  ->  " + m);
                            }
                        }
                    }
                    else 
                    {
                        //This is a text query that does NOT have any match
                        if (verbosity > 3)
                        {        
                            System.out.println("No matches for text query '"
                                             + txtQueriesAsStr.get(qID) + "'");
                        }
                    }

                    // Add scores for all the Situation:ICircumnstance that 
                    // include this text query
                    for (SCPair sc: tq.sources)
                    {
                        Situation s = sc.getSituation();
                        ICircumstance c = sc.getCircumstance();

                        double score = ((MatchText)c).calculateScore(matches);

                        //The resulting score can be zero even if the text has
                        // been matched. For instance, when we don't want to 
                        // find a string in a log feed, but, instead, we find 
                        // it (i.e., negation of of a MatchText circumstance)
                        // Therefore we store also zero scores

                        // Finally store the score
                        scoreCollector.addScore(s,c,score);
                    }
                } //End loop over queries
            } // end loop over InfoChannels
        } // End loop over InfoChannelTypes

/*
TODO NOtes:
- the same matches can be on the same feed
- there might be further analysis to be done on the extracted text (e.g., for 
loop counter of output file analysis)
*/
        
    }


//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
 
}
