package autocompchem.perception;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.BufferedReader;

import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.FileAsSource;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.circumstance.*;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.text.TextAnalyzer;
import autocompchem.run.Terminator;

/**
 * Perceptron is a neuron that uses  
 * a given situation base to perceive from a set of information channels.
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
     * Data structure collecting actual satisfation scores
     */
    private ScoreCollector scoreCollector;

    /**
     * Result/s of the perception
     */
    private ArrayList<Situation> occurringSituations;

    /**
     * Flag remembering the statur of awareness
     */
    private boolean iamaware = false;

    /**
     * Verbosity level
     */
    private int verbosity = 0;


    /**
     * Utilitynew line character (for logging)
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
     * @return the list ofmatched situations 
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
     * Perception of situation occurring based on the available information 
     * channels and situation base.
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
            // Check if each circumstnce is verified.
            for (ICircumstance c : s.getCircumstances())
            {
                evaluateOneCircumstance(s,c);
            }

            // Collect all the satisfation scores
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
                    System.out.println(init + "Confusion - The situation "
                                        + "matches multiple known "
                                        + "situation.");
                }
                break;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates if a circumnstance is satisfied. This recalls previous 
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
            //Previous evaluation exists in the scores collector 
        }
        else
        {
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
            String msg = "ERROR! Evaluation of circumstances not based on text is still to be implemented.";
            Terminator.withMsgAndStatus(msg,-1);

            scoreCollector.addScore(scp, score);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Perform all text matching in once. Thie method encapsulates all the 
     * handling of text-matching queries. Different Situations may require 
     * to analyze the text of the same text-based source of information. Thus,
     * to avoid reading the same source multiple times, we collect all the
     * text queries and analyze meant for a single source and read the source 
     * only one time. 
     * <b>WARNING: currently supporting only file-based InfoChannels.</b>
     * <b>WARNING: assuming all text-queries are single line. No new-line!</b>
     */

    private void analyzeAllText()
    {
        //Sort info source by type.
        Map<InfoChannelType,ArrayList<Situation>> situationsByICType = 
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

	// Utility for comparing InfoChannelTypes
	InfoChannelTypeComparator ictComparator =
                                                new InfoChannelTypeComparator();

        // Explore all the information channels by type, so that we can take all
        // the Circumstances that involve such channel type
        for (InfoChannelType ict : situationsByICType.keySet())
        {
            if (verbosity > 2)
            {
		System.out.println(newline+newline+"InfoChannelType: "+ict);
	    }

            // Here we collect all the text queries removing duplicates
            // The queries are collected ialso as objects to keep track of
            // the combination of Situation and ICircumstance they belong to.
            ArrayList<TxtQuery> txtQueries = new ArrayList<TxtQuery>();
            // and in as plain strings to be send to text parser
            ArrayList<String> txtQueriesAsStr = new ArrayList<String>();
            for (Situation s : situationsByICType.get(ict))
            {
                for (ICircumstance c : s.getCircumstances())
                {
                    if (c.requiresTXTMatch() && 
                        ictComparator.checkCompatibility(ict,
                                                         c.getChannelType())) 
                    {
                        String queryStr = ((MatchText) c).getPattern();

                        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        //
                        // WARNING!!!
                        // No handling of multiline matches!!!!
                        //
                        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                        // Keep only one text query among duplicates
                        if (txtQueriesAsStr.contains(queryStr))
                        {
                            TxtQuery tq = txtQueries.get(
                                           txtQueriesAsStr.indexOf(queryStr));
                            tq.addReference(s,c);
                        }
                        else
                        {
                            txtQueries.add(new TxtQuery(queryStr,s,c));
                            txtQueriesAsStr.add(queryStr);
                        }
                    }
                }  
            }

            // Scan all the input channels of the present type
            for (InfoChannel ic : icb.getChannelsOfType(ict))
            {
                if (verbosity > 2)
                {
                    System.out.println(newline +"Scanning InfoChannel: "+ic);
		}

		BufferedReader br = null;
		TreeMap<String,ArrayList<String>> allMatches = null;
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
		}
		catch (Exception e)
		{
		    String msg = "ERROR reading InfoChannel "+ic;
		    Terminator.withMsgAndStatus(msg,-1);
		}
		finally
		{
		    if (br != null)
		    {
			try
			{
			    br.close();
	                }
                        catch (Exception e)
                        {
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
		    if (verbosity > 3)
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
                             System.out.println("Matches for text query " 
                                                  + txtQueriesAsStr.get(qID));
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
                            System.out.println("No matches for text query ="
                                                  + txtQueriesAsStr.get(qID));
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
                        // Therefore wi store also zer oscores

                        // Finally store the score
                        scoreCollector.addScore(s,c,score);
                    }
                } //End loop over queries
            } // end loop over InfoChannels
        } // End loop over InfoChannelTypes

/*
TODO NOtes:
- the same mathes can be on the same feed
- there might be further analysis to be done on the extracted text (e.g., for loop counter of output file analysis)
*/
        
    }


//------------------------------------------------------------------------------

    /**
     * Utility class representing a text query that is associatied with one or
     * more pairs of situation:circumstance/s
     */

    private class TxtQuery
    {
        /**
         * The text query
         */
        public String query = "";

        /**
         * List of sources as
         * pairs of Situation and Circumstance that require matching
         * this very same text.
         */
        public ArrayList<SCPair> sources = new ArrayList<SCPair>();

    //-------------------------------------------------------------------------

        /**
         * Constructor with arguments
         * @param query the actual text query 
         * @param n the source situation that includes the circumstance 
         * that include the query.
         * @param c the circumstance that requires the quety
         */

        public TxtQuery(String query, Situation n, ICircumstance c)
        {
            this.query = query;
            this.sources.add(new SCPair(n,c));
        }

    //-------------------------------------------------------------------------

        /**
         * Add a pair of references
         * @param n the source situation that includes the circumstance
         * that include the query.
         * @param c the circumstance that requires the quety
         */

        public void addReference(Situation n, ICircumstance c)
        {
            this.sources.add(new SCPair(n,c));
        }

    //-------------------------------------------------------------------------
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[TxtQuery '"+query+"' in sources:").append(newline);
            for (SCPair nc : sources)
            {
                sb.append("   -> "+nc.toString());
            }
	    sb.append("]");
            return sb.toString();
        }
    }

//------------------------------------------------------------------------------
 
}