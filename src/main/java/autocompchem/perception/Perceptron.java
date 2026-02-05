package autocompchem.perception;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.IScoring;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.infochannel.InfoChannel;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.ReadableIC;
import autocompchem.perception.situation.Situation;
import autocompchem.perception.situation.SituationBase;
import autocompchem.text.TextAnalyzer;
import autocompchem.utils.StringUtils;

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
     * that may include channels with wildcards.
     */
    private InfoChannelBase originalICB;
    
    /**
     * Perception-specific Information channels base: this is made from the
     * InfoChannelBase configured to this perceptron by replacing 
     * wildcard-containing InfoChannel with concrete ones obtained after
     * trying to match the wildcards within the current context. 
     */
    private InfoChannelBase specICB;
    
    /**
     * Collection of channels that are lost upon creating the context-specific
     * {@link InfoChannelBase}.
     */
    private List<InfoChannel> lostICs = new ArrayList<InfoChannel>();
    
    /**
     * Information channels that have been visited
     */
    private Set<InfoChannel> previouslyReadInfoChannels = new HashSet<>();

    /**
     * Data structure collecting actual satisfaction scores
     */
    private ScoreCollector scoreCollector;

    /**
     * Result/s of the perception
     */
    private List<Situation> occurringSituations;

    /**
     * Flag remembering the status of awareness
     */
    private boolean iamaware = false;

    /**
     * Flag enabling tolerance towards missing info channels
     */
    private boolean tolerateMissingIC = false;

    /**
     * New line character (for logging)
     */
    private final String newline = System.getProperty("line.separator");
    
    /**
     * Logging tool
     */
    private static Logger logger;


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty {@link Perceptron} with no configuration of
     * {@link Situation}s or {@link InfoChannel}s.
     */

    public Perceptron() 
    {
    	logger = LogManager.getLogger(Perceptron.class);
        this.scoreCollector = new ScoreCollector();
        this.occurringSituations = new ArrayList<Situation>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for {@link Perceptron} with given a collection of known 
     * {@link Situation}s and a collection of {@link InfoChannel}s. Since 
     * channels may be contain rules aimed at searching actual sources of
     * information to be identified thanks to matching rules (e.g., REGEX-based 
     * file search), this method transforms the given {@link InfoChannelBase}
     * into a the present context-specific version to be used for perception.
     * Assumes the present work directory as context.
     * @param sitsBase the collection of {@link Situation}s to be aware of.
     * @param icb the collection of {@link InfoChannel}s, which may be aspecific,
     * i.e., use matching rules to search for specific sources of information.
     */
    protected Perceptron(SituationBase sitsBase, InfoChannelBase icb)
    {
    	this(sitsBase, icb, Paths.get("").toAbsolutePath());
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for {@link Perceptron} with given a collection of known 
     * {@link Situation}s and a collection of {@link InfoChannel}s. Since 
     * channels may be contain rules aimed at searching actual sources of
     * information to be identified thanks to matching rules (e.g., REGEX-based 
     * file search), this method transforms the given {@link InfoChannelBase}
     * into a context-specific version to be used for perception.
     * @param sitsBase the collection of {@link Situation}s to be aware of.
     * @param icb the collection of {@link InfoChannel}s, which may be aspecific,
     * i.e., use matching rules to search for specific sources of information.
     * @param wdir the work directory used to search for relative pathnames
     * when making the {@link InfoChannel}s context-specific.
     * @param lostICs the collector of any {@link InfoChannel} that does not 
     * match any concrete source of information, and is, therefore, removed
     * from the {@link InfoChannelBase}.
     */
    public Perceptron(SituationBase sitsBase, InfoChannelBase icb, Path wdir)
    {
    	this();
        this.sitsBase = sitsBase;
        this.originalICB = icb;
        this.scoreCollector = new ScoreCollector();
        this.occurringSituations = new ArrayList<Situation>();
        specifyInfoChannelBase(wdir);
    }
    
//------------------------------------------------------------------------------

    /**
     * Convert {@link InfoChannel}s originally given upon construction of this
     * {@link Perceptron} and that are based on matching rules 
     * into analogues with the concrete matches,
     * hence creates a {@link InfoChannelBase} specific
     * to the context (work directory and environment) given as parameter. 
     * Does not alter the original {@link InfoChannelBase}, but overwrites the
     * context-specific {@link InfoChannelBase} used for perception.
     * @param wdir the work directory used to search for relative pathnames.
     * @return the list of channels that were lost because there is no 
     * concrete information source that matches their matching rules in the
     * present context. This list is the same object returned by 
     * {@link #getLostICs()}
     */
    public List<InfoChannel> specifyInfoChannelBase(Path wdir)
    {
    	lostICs = new ArrayList<InfoChannel>();
		specICB = originalICB.getSpecific(wdir, lostICs);
		return lostICs;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of channels that were lost upon making the
     * {@link InfoChannelBase} context-specific. 
     * These channels were lost because there is no 
     * concrete information source that matches their matching rules in the
     * present context.
     *  access to the context-specific {@link InfoChannelBase} generated
     * by the {@link #specifyInfoChannelBase} method.
     * @return the collection of channels or <code>null</code> if
     * the {@link #specifyInfoChannelBase} method has not yet been run. 
     */
    public List<InfoChannel>  getLostICs()
    {
    	return lostICs;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Provides access to the context-specific {@link InfoChannelBase} generated
     * by the {@link #specifyInfoChannelBase} method.
     * @return the collection of channels or <code>null</code> if
     * the {@link #specifyInfoChannelBase} method has not yet been run. 
     */
    public InfoChannelBase getContextSpecificICB()
    {
    	return specICB;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds the given info channel to the set of those that are not explored as
     * they have been read before perception occurs. Info channels may be read
     * prior to perception when further data has to be parsed from them. 
     * To avoid reading them twice, we read them before perception and store
     * the corresponding scores with 
     * {@link Perceptron#collectPerceptionScoresForTxtMatchers(TxtQuery, List)}.
     * Then we add them to the list of previously visited ones to avoid reading
     * them again.
     */
    
    public void setInfoChannelAsRead(InfoChannel ic)
    {
    	previouslyReadInfoChannels.add(ic);    	
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
     * Perception of situation that is occurring and do it based on the 
     * available information channels and situation base.
     */

    public void perceive() throws Exception
    {
    	logger.trace(newline + "Perception from " + this.hashCode());
		if (!tolerateMissingIC && lostICs.size()>0)
		{ 
			throw new IllegalStateException("These info channels were not "
					+ "found: " 
					+ StringUtils.mergeListToString(lostICs, ", ", true));
		}
    	
        //To read text files only once we run all text-matching queries in once
        analyzeAllText();

        logger.trace("Scores after text-only: " + scoreCollector.printToString());

        //Do actual perception
        for (Situation s : sitsBase.getRelevantSituations(specICB))
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

        logger.trace("Final Scores: " + scoreCollector.printToString());

        String init = "Perception result: ";
        switch (occurringSituations.size())
        {
        case 0:
            logger.debug(init + "No situation has been detected");
            break;
            
        case 1:
            iamaware = true;
            logger.debug(init + "Known situation is " 
                		+ occurringSituations.get(0).getRefName());
            break;
            
       default:
        	List<String> names = new ArrayList<String>();
        	occurringSituations.stream().forEach(s -> names.add(
        			s.getRefName()));
        	String msg = init + "Confusion - The situation matches "
        			+ "multiple known situation. You may have to make "
                    + "the situations more specific as to "
                    + "disctiminate between these: " 
                    + StringUtils.mergeListToString(names, ", ", true);
        	logger.debug(msg);
            break;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates if a circumstance is satisfied. This recalls previous 
     * evaluations, or runs the evaluation if the result is not already
     * available.
     * @param n the situation in which the circumstance is evaluated.
     * @param c the actual circumstance to be evaluated.
     */

    private void evaluateOneCircumstance(Situation n, ICircumstance c)
    {
        SCPair oldKey = scoreCollector.keyWithSameSCValues(new SCPair(n,c));

        if (oldKey != null)
        {
            // Previous evaluation exists in the scores collector. Nothing to do.
        } else {
            SCPair scp = new SCPair(n,c);
            
            if (!(c instanceof IScoring))
            	return;
            
            IScoring sc = (IScoring) c;
            
            // evaluate circumstance now
            double score = 1.0;
            
            // Scan all the input channels of relevant type
            for (InfoChannel ic : specICB.getChannelsOfType(c.getChannelType()))
            {	
            	logger.trace(newline +"Scanning InfoChannel: "+ic);
                score = score * sc.calculateScore(ic);
            }

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
        		sitsBase.getRelevantSituationsByICT(specICB);

        // Explore all the information channels by type, so that we can take all
        // the Circumstances that involve such channel type
        for (InfoChannelType ict : situationsByICType.keySet())
        {
            logger.trace("InfoChannelType: "+ict);
            
            List<TxtQuery> txtQueries = new ArrayList<TxtQuery>(
            		sitsBase.getAllTxTQueriesForICT(ict, true));
            
            // Scan all the input channels of the present type
            for (InfoChannel ic : specICB.getChannelsOfType(ict))
            {
            	if (previouslyReadInfoChannels.contains(ic))
            		continue;
            	
            	if (!(ic instanceof ReadableIC))
            		continue;
            	
                logger.trace("Scanning InfoChannel: "+ic);
                
                Map<TxtQuery,List<String>> matches = getTxtMatchesFromICReader(
                		txtQueries, ic, tolerateMissingIC, 0);
                for (TxtQuery tq : matches.keySet())
                {   
                    if (matches.get(tq).size()!=0)
                    {
                    	String msg = "#Matches for text query '" 
                    			+ tq.query + "' = "
                    			+ matches.size() + ". Lines: " 
                    			+ StringUtils.mergeListToString(matches.get(tq),
                    					" ");
                        logger.trace(msg);
                    } else {
                    	logger.trace("No matches for text query '" + tq.query + "'");
                    }

                    // Collect scores afterwards
                    collectPerceptionScoresForTxtMatchers(tq, matches.get(tq));
                } //End loop over queries
            } // end loop over InfoChannels
        } // End loop over InfoChannelTypes   
    }
    
//------------------------------------------------------------------------------
    
    public void collectPerceptionScoresForTxtMatchers(TxtQuery tq, 
    		List<String> matches)
    {
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
            // it (i.e., negation of a MatchText circumstance)
            // Therefore we store also zero scores.

            // Finally store the score
            scoreCollector.addScore(s,c,score);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a feed searching for lines matching the given list of queries.
     * @param txtQueries the queries defining what to search for in the text.
     * @param ic channel we are reading now (only for logging).
     * @param tolerant use <code>true</code> to simply skip info channels that
     * cannot be read. Use <code>false</code> to throw 
     * {@link IllegalStateException} upon finding
     * that an info channel cannot be read.
     * @param verbosity the level of verbosity (only for logging).
     * @return a mapping of what lines matches which query.
     */
    public static Map<TxtQuery,List<String>> getTxtMatchesFromICReader(
    		List<TxtQuery> txtQueries, InfoChannel ic, boolean tolerant,
    		int verbosity)
    {	
    	List<String> txtQueriesAsStr = new ArrayList<String>();
        txtQueries.stream().forEach(tq -> txtQueriesAsStr.add(tq.query));
        
        // Check readability
        ReadableIC ric = null;
        if (ic instanceof ReadableIC)
        	ric = (ReadableIC) ic;
        
        if (ric==null || !ric.canBeRead())
        {
        	if (tolerant)
        	{
        		Map<TxtQuery,List<String>> emptyResult = 
                		new HashMap<TxtQuery,List<String>>();
                for (int qIdx = 0; qIdx < txtQueriesAsStr.size(); qIdx++)
                {
                    TxtQuery tq = txtQueries.get(qIdx);
                    emptyResult.put(tq, new ArrayList<String>());
                }
                return emptyResult;
        	} else {
        		throw new IllegalStateException("Unreadable info channel "+ic);
        	}
        }

        // The keys are "a_b_c" where b is the index of the string query
        // in txtQueriesAsStr
        TreeMap<String,List<String>> allMatches;
        BufferedReader br = new BufferedReader(ric.getSourceReader());
        try 
        {   // Extract potentially useful information from text
            // WARNING!!! No attempt to match multi-line blocks here!
            allMatches = TextAnalyzer.extractMapOfTxtBlocksWithDelimiters(br,
                                                txtQueriesAsStr,
                                                new ArrayList<String>(),
                                                new ArrayList<String>(),
                                                false,
                                                true);
        } catch (Exception e) {
        	if (!tolerant)
        	{
        		throw new IllegalStateException("Could not read InfoChannel "+ic);
        	} else {
                allMatches = new TreeMap<String,List<String>>();
            }
        } finally {
            if (br != null)
            {
                try
                {
                    br.close();
                } catch (Exception e) {
                	if (!tolerant)
                	{
                		throw new IllegalStateException("Could not close reader "
                				+ "on InfoChannel "+ic);
                	}
                }
            }
        }
        
        // We now need to convert the "a_b_c" string from allMatches into
        // references to the TxtQuery objects.
        
        // First create a mapping between TxtQuery identifier (key) and the 
        // "a_b_c" strings (value. The key on this map is the
        // index of the TxtQuery.patterm string in txtQueriesAsStr and of the 
        // corresponding TxtQuery object in txtQueries. 
        // The value is the list of "a_b_c"
        // Strings that identify the matches. They are many because the string
        // may have been found multiple times in the channel.
        Map<Integer,List<String>> matchesIDsbyTxtQueryIdx = 
                               new HashMap<Integer,List<String>>();
        for (String strQuery : allMatches.keySet())
        {
            // The matches are returned with a key that contains
            // numerical indexes a_b_c.  For details,
            // see TextAnalyzer.extractMapOfTxtBlocksWithDelimiters.
            // First, we deal with the indexes
            String[] parts = strQuery.split("_");
            //...and get the index of the query given to TextAnalyzer
            int qID = Integer.parseInt(parts[1]); // the 'b'
            if (matchesIDsbyTxtQueryIdx.keySet().contains(qID))
            {
            	matchesIDsbyTxtQueryIdx.get(qID).add(strQuery);
            } else {
                List<String> strQryKeys = new ArrayList<String>();
                strQryKeys.add(strQuery);
                matchesIDsbyTxtQueryIdx.put(qID,strQryKeys);
            }
        }
    	
        Map<TxtQuery,List<String>> mapOfMatches = 
        		new HashMap<TxtQuery,List<String>>();
        for (int qIdx = 0; qIdx < txtQueriesAsStr.size(); qIdx++)
        {
            TxtQuery tq = txtQueries.get(qIdx);

            // The lines that match the present text query
            List<String> matches = new ArrayList<String>();
            if (matchesIDsbyTxtQueryIdx.keySet().contains(qIdx))
            {
                // This text query has been matched, so collect matching lines.
                for (String strQuery : matchesIDsbyTxtQueryIdx.get(qIdx))
                {
                    matches.addAll(allMatches.get(strQuery));
                }     
                logger.trace("Matches for text query '" 
                		 + txtQueriesAsStr.get(qIdx) + "' = "
                		 + matches.size() + ". Lines: " 
                		 + StringUtils.mergeListToString(matches, " "));
            } else {
                logger.trace("No matches for text query '"
                		+ txtQueriesAsStr.get(qIdx) + "'");
            }
            mapOfMatches.put(tq, matches);
        }
    	return mapOfMatches;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the possibility to tolerate the absence of some info channel.
     * @param tolerateMissingIC use <code>true</code> to consider the absence
     * of an info channel as lack of verification of a circumstance.
     */
	public void setTolerantMissingIC(boolean tolerateMissingIC) 
	{
		this.tolerateMissingIC = tolerateMissingIC;
	}

//------------------------------------------------------------------------------
 
}
