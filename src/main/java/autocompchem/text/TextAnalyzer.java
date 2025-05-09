package autocompchem.text;

/*
 *   Copyright (C) 2016  Marco Foscato
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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.chem.ChemSoftConstants;


/**
 * Tool for analysing text stream
 * 
 * @author Marco Foscato
 */

public class TextAnalyzer
{

    public static final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------

    /**
     * Construct an empty TextAnalyzer
     */

    public TextAnalyzer()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Count non-overlapping matches in a single line. Careful about the 
     * non-overlapping property: wild cards can easily be messing with you!
     * @param pattern the regex to find
     * @param line the string to analyse
     * @return the number of non-overlapping matches
     */

    public static int countInLine(String pattern, String line)
    {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(line);

        int count = 0;
        while (m.find())
            count++;

        return count;
    }
    
//------------------------------------------------------------------------------

    /**
     * Count non-overlapping occurrences of a string (not a regex!) in a single
     * line. 
     * @param str the string (NB, this is NOT a regex!) to match
     * @param line the string to analyse
     * @return the number of non-overlapping matches
     */

    public static int countStringInLine(String str, String line)
    {
    	int count = 0;
    	while (line.contains(str))
    	{
    		count++;
    		line = line.substring(line.indexOf(str) + str.length());
    	}
    	
        return count;
    }

//------------------------------------------------------------------------------

    /**
     * Count lines from buffer containing a patter. 
     * @param buffRead the source of text
     * @param query patters string. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @return the number of matching lines.
     */

    public static int count(BufferedReader buffRead, String query)
    {
        List<List<Integer>> counts = count(buffRead, Arrays.asList(query));
        return counts.get(counts.size()-1).get(0);
    }

//------------------------------------------------------------------------------

    /**
     * Count lines containing patterns. This method correspond to run the Linux 
     * command 
     * <code> ... | grep -c "some pattern" AND grep -n "some pattern" </code>
     * The returned array contains both the line number of the matches
     * and the counts. The last List in the returned
     * List of Lists is the one that contains the counts, 
     * while all the other
     * Lists contain the line numbers
     * @param buffRead the source of text
     * @param lsStr list of pattern strings. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the pattern
     * will be searches in all the line (as for *blabla*).
     * If a line matches more than one query it is counted more than one
     * @return the counts per each pattern and the line numbers lists 
     * (1-n, does not work as .size()!) 
     */

    public static List<List<Integer>> count(BufferedReader buffRead,
    		List<String> lsStr)
    {
        //Prepare storage of counts
        List<Integer> counts = new ArrayList<Integer>(lsStr.size());
        List<List<Integer>> lineNums = new ArrayList<List<Integer>>();
        for (int i=0; i<lsStr.size(); i++)
        {
            counts.add(0);
            List<Integer> ln = new ArrayList<Integer>();
            lineNums.add(ln);
        }
        
        //Read file and count
        int ln = 0;
        try {
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
                ln++;
                for (int i=0; i<lsStr.size(); i++)
                {
                    String pattern = lsStr.get(i);
                    boolean[] startMidEnd = getMatchingMethod(pattern);
                    pattern = preparePattern(pattern,startMidEnd);
                    if (match(line,pattern,startMidEnd))
                    {
                        counts.set(i,counts.get(i) + 1);
                        lineNums.get(i).add(ln);
                    }
                }
            }
        } catch (Throwable t) {
            //none
        } 

        //Make a unique object for returning
        lineNums.add(counts);
        return lineNums;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts the lines between the first matches of given patterns.
     * The patterns are intended to match the entire line.
     * @param buffRead the source of text
     * @param pattern1 identifies the beginning of the target section
     * @param pattern2 identifies the end of the target section
     * @param inclPatts set to <code>true</code> to include the lines
     * containing <code>pattern1</code> and <code>pattern2</code> into the
     * target section.
     * @return the target section as array of lines
     */

    public static List<String> extractTxtWithDelimiters(
                                                     BufferedReader buffRead,
                                                             String pattern1,
                                                             String pattern2,
                                                             boolean inclPatts)
    {
        List<List<String>> blocks = extractMultiTxtBlocksWithDelimiters(
                                                                      buffRead,
                                                                      pattern1,
                                                                      pattern2,
                                                                          true,
                                                                     inclPatts);
        return blocks.get(0);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts multiple blocks of lines that are all delimited by the same
     * pair of given patters.
     * The patterns are intended to match the entire line.
     * @param buffRead the source of text
     * @param pattern1 identifies the beginning of the target section.
     * @param pattern2 identifies the end of the target section
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing <code>pattern1</code> and <code>pattern2</code> into the
     * target section.
     * @return the list of target sections, each as an array of lines
     */

    public static List<List<String>>
                         extractMultiTxtBlocksWithDelimiters(
                                                       BufferedReader buffRead,
                                                               String pattern1,
                                                               String pattern2,
                                                             boolean onlyFirst,
                                                              boolean inclPatts)
    {
        return extractMultiTxtBlocksWithDelimiters(buffRead,
                                 new ArrayList<String>(Arrays.asList(pattern1)),
                                 new ArrayList<String>(Arrays.asList(pattern2)),
                                                           onlyFirst,inclPatts);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts multiple blocks of lines that are delimited by one of the given
     * pairs of patters.
     * Each patter is intended to match text within a single line.
     * Nested blocks are dealt with.
     * @param buffRead the source of text
     * @param startPattrns list of patterns that identify the beginning of a 
     * target section. Each entry must have a corresponding one in endPattrns
     * @param endPattrns list of patterns that identify the end of a 
     * target section. Each entry must have a corresponding one in startPattrns
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the
     * target section.
     * @return the list of target sections, each as an array of lines
     */

    public static List<List<String>> 
                         extractMultiTxtBlocksWithDelimiters(
                                                       BufferedReader buffRead,
                                                List<String> startPattrns,
                                                  List<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        List<List<String>> blocks = new ArrayList<List<String>>();

        Map<String,List<String>> mapBlocks =
        		extractMapOfTxtBlocksWithDelimiters(buffRead, startPattrns,
        				endPattrns, onlyFirst, inclPatts);

//TODO check leftover?
        List<String> sortKeys = new ArrayList<String>();
        Collections.sort(sortKeys);
        
        TreeMap<Integer,String> keysMap = new TreeMap<Integer,String>();
        for (String k : mapBlocks.keySet())
        {
            String[] p = k.split("_");
            keysMap.put(Integer.parseInt(p[0]),k);
        }
        
        for (Integer k : keysMap.keySet())
        {
            blocks.add(mapBlocks.get(keysMap.get(k)));
        }

        return blocks;
    }

//------------------------------------------------------------------------------

    /**
     * Comparator for string-like keys used to identify matched blocks of text
     * in method extractMapOfTxtBlocksWithDelimiters.
     */

    private static class MetchKeyComparator implements Comparator<String>
    {
        @Override
        public int compare(String a, String b)
        {
            final int EQUAL = 0;

            String[] pA = a.split("_");
            String[] pB = b.split("_");

            if (pA.length != 3 || pB.length != 3)
            {
                return EQUAL;
            }

            Integer idA = Integer.parseInt(pA[0]);
            Integer idB = Integer.parseInt(pB[0]);

            if (idA.equals(idB))
            {
                Integer id1A = Integer.parseInt(pA[1]);
                Integer id1B = Integer.parseInt(pB[1]);
                Integer id2A = Integer.parseInt(pA[2]);
                Integer id2B = Integer.parseInt(pB[2]);
                idA = 100000000*idA + 1000000*id1A + id2A;
                idB = 100000000*idB + 1000000*id1B + id2B;
            }
            return idA.compareTo(idB);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Extracts multiple blocks of lines that are delimited by one of the given
     * pairs of patters.
     * Each patter is intended to match text within a single line.
     * Nested blocks are dealt with.
     * @param buffRead the source of text
     * @param startPattrns list of patterns that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns
     * @param endPattrns list of patterns that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the
     * target section.
     * @return a map with all the target sections, each as an array of lines,
     * and each identified by a key which corresponds to the following format:
     * a_b_c where "a" is the position of the block in the overall sequence of
     * block (sorted according to their appearance in the file) [i.e., a
     * number 0-n],
     * "b" is the index of the pair of patterns in the
     * startPattrns/endPattrns lists [i.e., an integer 0-n],
     * and "c" is the index of the block among blocks of same type
     * [i.e., an integer 0-n].
     */

    public static TreeMap<String,List<String>>
                        extractMapOfTxtBlocksWithDelimiters(
                                                       BufferedReader buffRead,
                                                List<String> startPattrns,
                                                  List<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        return               extractMapOfTxtBlocksWithDelimiters(buffRead,
                                                       new ArrayList<String>(),
                                                                  startPattrns,
                                                                    endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts blocks of text from text.
     * Extracts multiple blocks of lines that are delimited by one of the given
     * pairs of REGEX.
     * Each REGEX pattern is intended to match text within a single line and to
     * match the entire line.
     * Nested blocks are dealt with, and the returned list includes only the
     * first, outermost level. Nested TextBlocks are embedded inside their
     * nesting TextBlocks.
     * @param buffRead the source of text
     * @param startPattrn REGEXs that identify the beginning of a
     * target section. 
     * @param endPattrn REGEXs that identify the end of a
     * target section. 
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match.
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @return a list of matched text blocks that may include nested blocks.
     */

    public static List<TextBlockIndexed> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                            String startPattrn,
                                                              String endPattrn,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        List<String> startPattrns = new ArrayList<String>();
        List<String> endPattrns = new ArrayList<String>();
        startPattrns.add(startPattrn);
        endPattrns.add(endPattrn);

        return extractTextBlocks(buffRead, new ArrayList<String>(),
                                                                  startPattrns,
                                                                    endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts,
                                                                         false);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts blocks of text from text.
     * Extracts multiple blocks of lines that are delimited by one of the given
     * pairs of REGEX.
     * Each REGEX pattern is intended to match text within a single line and to
     * match the entire line.
     * Nested blocks are dealt with, and the returned list includes only the
     * first, outermost level. Nested TextBlocks are embedded inside their
     * nesting TextBlocks.
     * @param buffRead the source of text
     * @param startPattrns list of REGEXs that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns.
     * @param endPattrns list of REGEXs that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns.
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match.
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @return a list of matched text blocks that may include nested blocks.
     */

    public static List<TextBlockIndexed> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                List<String> startPattrns,
                                                  List<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        return extractTextBlocks(buffRead, new ArrayList<String>(), 
                                                                  startPattrns,
                                                                    endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts,
                                                                         false);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts blocks of text from text.
     * Extracts either single lines matching any of the REGEX,
     * and multiple blocks of lines that are delimited by one of the given
     * pairs of REGEX.
     * Each REGEX pattern is intended to match text within a single line and to
     * match the entire line.
     * Nested blocks are dealt with, and the returned list includes only the
     * first, outermost level. Nested TextBlocks are embedded inside their
     * nesting TextBlocks.
     * @param buffRead the source of text
     * @param slPattrns the list of REGEX meant to match single line content.
     * @param startPattrns list of REGEXs that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns.
     * @param endPattrns list of REGEXs that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns.
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match.
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @return a list of matched text blocks that may include nested blocks.
     */

    public static List<TextBlockIndexed> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                   List<String> slPattrns,
                                                List<String> startPattrns,
                                                  List<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        return extractTextBlocks(buffRead, slPattrns, startPattrns, endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts,
                                                                         false);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts blocks of text from text. 
     * Extracts either single lines matching any of the REGEX,
     * and multiple blocks of lines that are delimited by one of the given
     * pairs of REGEX.
     * Each REGEX pattern is intended to match text within a single line and to 
     * match the entire line.
     * Nested blocks are dealt with, and the returned list includes only the 
     * first, outermost level. Nested TextBlocks are embedded inside their
     * nesting TextBlocks.
     * @param buffRead the source of text
     * @param slPattrns the list of REGEX meant to match single line content.
     * @param startPattrns list of REGEXs that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns.
     * @param endPattrns list of REGEXs that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns.
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match.
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @param expandBeyondNests set to <code>true</code> to ignore the nest
     * structure while expanding the text blocks. That is, add each line of 
     * a nested block also to the upper level (i.e., nesting) block.
     * @return a list of matched text blocks that may include nested blocks.
     */

    public static List<TextBlockIndexed> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                   List<String> slPattrns,
                                                List<String> startPattrns,
                                                  List<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts,
                                                      boolean expandBeyondNests)
    {
        // This is what we well return
        List<TextBlockIndexed> blocks = new ArrayList<TextBlockIndexed>();

        // Initialise counters
        List<Integer> countsSL = new ArrayList<Integer>(slPattrns.size());
        for (int pattIdx=0; pattIdx<slPattrns.size(); pattIdx++)
        {
            countsSL.add(-1);
        }
        List<Integer> countsML = new ArrayList<Integer>(startPattrns.size());
        for (int pattIdx=0; pattIdx<startPattrns.size(); pattIdx++)
        {
            countsML.add(-1);
        }
        int countMatches = -1;

        // Start reading the input text feed line by line
        String msg = "";
        try {
            String line = null;
            outerLoopOnLines:
            {
                //A unique identified for each match: becomes the key in maps
                AtomicInteger opnBlkKey = new AtomicInteger(0);

                //Indexed list of open blocks that await closure
                Map<Integer,TextBlockIndexed> openBlocks =
                		new HashMap<Integer,TextBlockIndexed>();

                //Map of open block key to pattern ID
                Map<Integer,Integer> oBKey2PId = new HashMap<Integer,Integer>();

                //Map of patter ID to actively expanding open block
                // (there might be nested blocks, in which case we add lines         
                // only to the actively growing block, which is the youngest)
                Map<Integer,Integer> pId2AOBK = new HashMap<Integer,Integer>();

                //Blocks that have been opened in the latest read line
                Set<Integer> justOpenBlocks = new HashSet<Integer>();

                //Blocks that have been closed in the latest read line
                Set<Integer> closedBlocks = new HashSet<Integer>();

                while ((line = buffRead.readLine()) != null)
                {
                    //
                    // Handling of lines that match any of the queries
                    //

                    //Check for any single line match
                    boolean firstSlMatch = true;
                    for (int pattIdx=0; pattIdx<slPattrns.size(); pattIdx++)
                    {
                        String slPattern = slPattrns.get(pattIdx);
                        if (line.matches(slPattern))
                        {
                            if (firstSlMatch)
                            {
                                //NB: same 1st ID for all matches of this line
                                countMatches++;
                                firstSlMatch = false;
                            }
                            countsSL.set(pattIdx,1 + countsSL.get(pattIdx));
                            List<String> block = new ArrayList<String>();
                            block.add(line);

                            //
                            // WARNING: Single line matches cannot be nested
                            // 

                            TextBlockIndexed tb = 
                            		new TextBlockIndexed(block,countMatches,
                            				pattIdx,countsSL.get(pattIdx));
                            blocks.add(tb);
                        }
                    }

                    //Check for the start of any multiline block
                    boolean firstMlMatch = true;
                    for (int pattIdx=0; pattIdx<startPattrns.size(); pattIdx++)
                    {
                        String pattern1 = startPattrns.get(pattIdx);
                        if (line.matches(pattern1))
                        {
                            if (firstMlMatch)
                            {
                                //NB: same 1st ID for all matches of this line
                                countMatches++;
                                firstMlMatch = false;
                            }
                            countsML.set(pattIdx,1 + countsML.get(pattIdx));

                            TextBlockIndexed tb = 
                            		new TextBlockIndexed(countMatches,
                                                      pattIdx+slPattrns.size(),
                                                         countsML.get(pattIdx));
                            if (inclPatts)
                            {
                                tb.appendText(line);
                            }

                            Integer unqOpnBlkKey = opnBlkKey.getAndIncrement();
                            justOpenBlocks.add(unqOpnBlkKey);
                            openBlocks.put(unqOpnBlkKey,tb);
                            oBKey2PId.put(unqOpnBlkKey,pattIdx);
                            pId2AOBK.put(pattIdx,unqOpnBlkKey);
                        }
                    }

                    //Check for the end of any multiline block
                    Set<Integer> opnBlkPattIdx = new HashSet<Integer>();
                    opnBlkPattIdx.addAll(oBKey2PId.values());
                    for (int pattIdx : opnBlkPattIdx)
                    {
                        String pattern2 = endPattrns.get(pattIdx);
                        if (line.matches(pattern2))
                        {
                            // We close the newest open block of this specific
                            // pattIdx. Find it by sorting list of open blocks
                            List<Integer> sortedOpnBlkKeys =
                                                       new ArrayList<Integer>();
                            for (Integer k : oBKey2PId.keySet())
                            {
                                //Keep only those pertaining the current pair of
                                //patterns
                                if (pattIdx ==
                                    oBKey2PId.get(k).intValue())
                                {
                                    sortedOpnBlkKeys.add(k);
                                }
                            }
                            Collections.sort(sortedOpnBlkKeys); //small -> big
                            Collections.reverse(sortedOpnBlkKeys);//big -> small

                            //Get the block that we are closing now
                            Integer newstOBlk = sortedOpnBlkKeys.get(0);
                            TextBlockIndexed thisBlock = 
                            		openBlocks.get(newstOBlk);

                            if (inclPatts)
                            {
                                thisBlock.appendText(line);
                            }
                            
                            //Nested blocks are handled in a different way
                            if (sortedOpnBlkKeys.size() > 1)
                            {
                                //This is a nested block
                                //Find the parent block
                                Integer parentOBlk = sortedOpnBlkKeys.get(1);

                                //Append this current block as a nested block 
                                //of its parent block
                                openBlocks.get(parentOBlk).appendNestedBlock(
                                                                     thisBlock);
                            }
                            else
                            {
                                //This block is not nested thus we ass it to the
                                //list of outermost, first-level blocks
                                blocks.add(thisBlock);
                            }

                            closedBlocks.add(newstOBlk);
                        }
                    }

                    // Clear placeholder of closed blocks
                    if (closedBlocks.size() > 0)
                    {
                        for (Integer unqOpnBlkKey : closedBlocks)
                        {
                            openBlocks.remove(unqOpnBlkKey);
                            oBKey2PId.remove(unqOpnBlkKey);
                        }
                        closedBlocks.clear();
                    }

                    //
                    // General handling of lines 
                    //

                    if (expandBeyondNests)
                    {
                        //Keep expanding all open blocks no matter the nesting
                        for (Integer unqOpnBlkKey : openBlocks.keySet())
                        {
                            if (!justOpenBlocks.contains(unqOpnBlkKey))
                            {
                                openBlocks.get(unqOpnBlkKey).appendText(line);
                            }
                        }
                    }
                    else
                    {
                        //Expand only the innermost block, i.e.., the most 
                        // deeply nested, i.e., the youngest block
                        // for each pattern pair ID
                        Set<Integer> activePattIdx = new HashSet<Integer>();
                        activePattIdx.addAll(oBKey2PId.values());
                        for (int pattIdx : activePattIdx)
                        {
                            //Ignore strings that match matters of this ID
                            String p1 = startPattrns.get(pattIdx);
                            String p2 = endPattrns.get(pattIdx);
                            if (line.matches(p1) || line.matches(p2))
                            {
                                continue;
                            }
                            Integer youngestKey = -1;
                            for (Map.Entry<Integer,Integer> e 
                            		: oBKey2PId.entrySet())
                            {
                                if (pattIdx != 
                                            ((Integer) e.getValue()).intValue())
                                {
                                    continue;
                                }
                                int candKey = ((Integer) e.getKey()).intValue();
                                //Ignore those just opened
                                if (justOpenBlocks.contains(candKey))
                                {
                                    continue;
                                }
                                if (candKey > youngestKey)
                                {
                                    youngestKey = candKey;
                                }
                            }
                            if (youngestKey>-1)
                            {
                                openBlocks.get(youngestKey).appendText(line);
                            }
                        }
                    }
                    justOpenBlocks.clear();

                    if (onlyFirst && countMatches>1)
                    {
                        break outerLoopOnLines;
                    }
                }
            }
        } catch (Throwable t) {
            msg = t.getMessage();
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Unable to read bufferedReader "
                + ". " + msg,-1);
        }

        return blocks;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts text from text: single lines matching any of the REGEX,
     * and multiple blocks of lines that are delimited by one of the given
     * pairs of patters.
     * Each patter is intended to match text within a single line. 
     * Nested blocks are dealt with.
     * @param buffRead the source of text
     * @param slPattrns the list of patterns meant to match single line content
     * @param startPattrns list of patterns that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns
     * @param endPattrns list of patterns that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the first match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @return a map with all the target sections, each as an array of lines,
     * and each identified by a key which corresponds to the following format:
     * a_b_c where "a" is the position of the line or block in the overall 
     * sequence of matches lines of blocks of lines
     * (sorted according to their appearance in the file) [i.e., a 
     * number 0-n], 
     * "b" is the index of the pattern, for single line matches, or, for multi 
     * line blocks, of the pair of patterns in the slPattrns or
     * startPattrns/endPattrns lists [i.e., an integer 0-n],
     * and "c" is the index of the block among blocks of same type 
     * [i.e., an integer 0-n].
     */

    public static TreeMap<String,List<String>>
                        extractMapOfTxtBlocksWithDelimiters(
                                                        BufferedReader buffRead,
                                                   List<String> slPattrns,
                                                List<String> startPattrns,
                                                  List<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        TreeMap<String,List<String>> blocks = 
                new TreeMap<String,List<String>>(new MetchKeyComparator());
        List<Integer> countsSL = new ArrayList<Integer>(slPattrns.size());
        for (int pattIdx=0; pattIdx<slPattrns.size(); pattIdx++)
        {
            countsSL.add(-1);
        }
        List<Integer> countsML = new ArrayList<Integer>(startPattrns.size());
        for (int pattIdx=0; pattIdx<startPattrns.size(); pattIdx++)
        {
            countsML.add(-1);
        }
        int countMatches = -1;
        String msg = "";
        try {
            String line = null;
            outerLoopOnLines: 
            {
                AtomicInteger opnBlkKey = new AtomicInteger(0);
                Map<Integer,List<String>> openBlocks = 
                		new HashMap<Integer,List<String>>();
                Map<Integer,Integer> oBKey2PId = new HashMap<Integer,Integer>();
                Map<Integer,String> oBKey2Key= new HashMap<Integer,String>();
                Set<Integer> justOpenBlocks = new HashSet<Integer>();
                Set<Integer> closedBlocks = new HashSet<Integer>();

                while ((line = buffRead.readLine()) != null)
                {
                    //Check for any single line match
                    boolean firstSlMatch = true;
                    for (int pattIdx=0; pattIdx<slPattrns.size(); pattIdx++)
                    {
                        String slPattern = slPattrns.get(pattIdx);
                        if (line.matches(slPattern))
                        {
                            if (firstSlMatch)
                            {
                                //NB: same 1st ID for all matches of this line
                                countMatches++;
                                firstSlMatch = false;
                            }
                            countsSL.set(pattIdx,1 + countsSL.get(pattIdx));
                            ArrayList<String> block = new ArrayList<String>();
                            block.add(line);
                            String key = countMatches + "_" + pattIdx + "_"
                                                        + countsSL.get(pattIdx);
                            blocks.put(key,block);
                        }
                    }

                    //Check for the start of any multiline block
                    boolean firstMlMatch = true;
                    for (int pattIdx=0; pattIdx<startPattrns.size(); pattIdx++)
                    {
                        String pattern1 = startPattrns.get(pattIdx);
                        if (line.matches(pattern1))
                        {
                            if (firstMlMatch)
                            {
                                //NB: same 1st ID for all matches of this line
                                countMatches++;
                                firstMlMatch = false;
                            }
                            countsML.set(pattIdx,1 + countsML.get(pattIdx));
                            ArrayList<String> block = new ArrayList<String>();
                            if (inclPatts)
                            {
                                block.add(line);
                            }
                            String key = countMatches + "_" 
                                         + (pattIdx+slPattrns.size())
                                         + "_" + countsML.get(pattIdx);
                            Integer unqOpnBlkKey = opnBlkKey.getAndIncrement();
                            justOpenBlocks.add(unqOpnBlkKey);
                            openBlocks.put(unqOpnBlkKey,block);
                            oBKey2PId.put(unqOpnBlkKey,pattIdx);
                            oBKey2Key.put(unqOpnBlkKey,key);
                        }
                    }

                    //Check for the end of any multiline block
                    for (int pattIdx : oBKey2PId.values())
                    {
                        String pattern2 = endPattrns.get(pattIdx);
                        if (line.matches(pattern2))
                        {
                            // We close the newest open block of this specific
                            // pattIdx. This deals with nesting of blocks
                            Integer newstOBlk = -1;
                            for (Integer unqOpnBlkKey : oBKey2PId.keySet())
                            {
                                if (pattIdx == 
                                    oBKey2PId.get(unqOpnBlkKey).intValue())
                                {
                                    if (unqOpnBlkKey > newstOBlk)
                                    {
                                        newstOBlk = unqOpnBlkKey;
                                    }
                                }
                            }
                            List<String> block = openBlocks.get(newstOBlk);
                            if (inclPatts)
                            {
                                block.add(line);
                            }
                            //Finally store the closed block
                            blocks.put(oBKey2Key.get(newstOBlk),block);
                            closedBlocks.add(newstOBlk);
                        }
                    }

                    // Clear placeholder of closed blocks
                    if (closedBlocks.size() > 0)
                    {
                        for (Integer unqOpnBlkKey : closedBlocks)
                        {
                            openBlocks.remove(unqOpnBlkKey);
                            oBKey2PId.remove(unqOpnBlkKey);
                            oBKey2Key.remove(unqOpnBlkKey);
                        }
                        closedBlocks.clear();
                    }

                    // keep extending the open blocks
                    for (Integer unqOpnBlkKey : openBlocks.keySet())
                    {
                        if (!justOpenBlocks.contains(unqOpnBlkKey))
                        {
                            openBlocks.get(unqOpnBlkKey).add(line);
                        }
                    }
                    justOpenBlocks.clear();

                    if (onlyFirst && countMatches>1)
                    {
                        break outerLoopOnLines;
                    }
                }
            }
        } catch (Throwable t) {
            msg = t.getMessage();
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Unable to read bufferedReader " 
                + ". " + msg,-1);
        } 
        return blocks;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts the lines between the first matches of given patterns.
     * @param lines the lines to read
     * @param pattern1 identifies the beginning of the target section
     * @param pattern2 identifies the end of the target section
     * @param inclPatts set to <code>true</code> to include the lines
     * containing <code>pattern1</code> and <code>pattern2</code> into the
     * target section.
     * @return the target section as array of lines
     */

    public static ArrayList<String> extractTxtWithDelimiters(
                                                        ArrayList<String> lines,
                                                        String pattern1,
                                                        String pattern2,
                                                        boolean inclPatts)
    {
        ArrayList<String> target = new ArrayList<String>();
        boolean found1st = false;
        for (String line : lines)
        {
            if (line.matches(pattern1))
            {
                found1st = true;
                if (inclPatts)
                    target.add(line);
                continue;
            }

            if (found1st)
            {
                if (line.matches(pattern2))
                {
                    if (inclPatts)
                        target.add(line);
                    break;
                }
                target.add(line);
            }
        }
        return target;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts multiple blocks of lines that are all initiated the same
     * string and last for a given number of lines.
     * The patterns are intended to match the entire line.
     * @param buffRead the source of text
     * @param pattern1 identifies the beginning of the target section
     * @param size the number of lines to keep after the match
     * @param inclPatt set to <code>true</code> to include the line
     * containing <code>pattern1</code> into the
     * target section.
     * @return the list of target sections, each as an array of lines
     */

    public static ArrayList<ArrayList<String>>
                     extractMultiTxtBlocksWithDelimiterAndSize(
                                                       BufferedReader buffRead,
                                                               String pattern1,
                                                               int size,
                                                               boolean inclPatt)
    {
        ArrayList<ArrayList<String>> blocks =
                                             new ArrayList<ArrayList<String>>();
        String msg = "";
        try {
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
                if (line.matches(pattern1))
                {
                    ArrayList<String> block = new ArrayList<String>();
                    if (inclPatt)
                    {
                        block.add(line);
                    }
                    for (int i=0; i<size; i++)
                    {
                        line = buffRead.readLine();
                        if (line == null)
                        {
                            break;
                        }
                        block.add(line);
                    }
                    blocks.add(block);
                }
            }
        } catch (Throwable t) {
            msg = t.getMessage();
            Terminator.withMsgAndStatus("ERROR! Unable to read bufferedReader. "
                + msg,-1);
        } 

        return blocks;
    }

//------------------------------------------------------------------------------

    /**
     * Identify the way to use the string according to the '*'
     * @param str pattern containing '*'
     * @return the list of booleans flags defining how the string containing 
     * the asterisk corresponds to 
     * i) a substring to match at the beginning of a longer string, 
     * ii) a substring to match in the middle of a larger string,
     * iii) a substring to match at the end of a longer string. 
     */

    private static boolean[] getMatchingMethod(String str)
    {
        boolean[] startMidEnd = new boolean[3];

        boolean understood = false;
        if (str.startsWith("*") && (!str.endsWith("*")))
        {
            startMidEnd[0] = false;
            startMidEnd[1] = false;
            startMidEnd[2] = true;
            understood = true;
        } else if (str.endsWith("*") && (!str.startsWith("*")))
        {
            startMidEnd[0] = true;
            startMidEnd[1] = false;
            startMidEnd[2] = false;
            understood = true;
        } else if (str.startsWith("*") && str.endsWith("*"))
        {
            startMidEnd[0] = false;
            startMidEnd[1] = true;
            startMidEnd[2] = false;
            understood = true;
        } else if ((!str.startsWith("*")) && (!str.endsWith("*")))
        {
            startMidEnd[0] = false;
            startMidEnd[1] = true;
            startMidEnd[2] = false;
            understood = true;
        }

        if (!understood)
        {
        	Terminator.withMsgAndStatus("ERROR! "
        			+ "TextAnalyzer.getMatchingMethod failed to "
        			+ "understand string '"+str+"'.", -1);
        }
        
        return startMidEnd;
    }
    
//------------------------------------------------------------------------------
    
    private static String preparePattern(String pattern, boolean[] relation)
    {
    	String newPattern = pattern;
        if (relation[0])
        {
        	newPattern = pattern.substring(0,pattern.length() - 1);
        } else if (relation[1])
        {
        	if (pattern.startsWith("*") && pattern.endsWith("*"))
        	{
        		newPattern = pattern.substring(1,pattern.length() - 1);
        	}
        } else if (relation[2])
        {
        	newPattern = pattern.substring(1);
        }
    	return newPattern;
    }

//------------------------------------------------------------------------------

    /**
     * Compare a line with a pattern specifying the relation between the two.
     * @param line string that is searches for the pattern.
     * @param pattern string to be searched in the line.
     * @param relation array of 3 booleans defining if the pattern has to be
     * in the beginning of the line (T,F,F), in the middle (F,T,F), or 
     * at the end (F,F,T).
     * @return <code>true</code> in case of match
     */

    private static boolean match(String line, String pattern, 
    		boolean[] relation)
    {
        boolean answ = false;
        if (relation[0])
        {
            if (line.startsWith(pattern))
                answ = true;
        } else if (relation[1])
        {
            if (line.contains(pattern))
                answ = true;
        } else if (relation[2])
        {
            if (line.endsWith(pattern))
                answ = true;
        }
        return answ;
    }

//------------------------------------------------------------------------------

    /**
     * Find matches line by line.
     * @param buffRead the source of text.
     * @param patterns the set of patterns to try to match in each line.
     * @return a list of the matches: each entry is a line matching any of the 
     * queries.
     */
 
    public static ArrayList<String> grep(BufferedReader buffRead, 
                                     Set<String> patterns) throws Exception
    {
        ArrayList<String> matches = new ArrayList<String>();
        String line = null;
        while ((line = buffRead.readLine()) != null)
        {
            for (String pattern : patterns)
            {
                boolean[] startMidEnd = getMatchingMethod(pattern);
                if (match(line,pattern,startMidEnd))
                {
                    matches.add(line);
                    break;
                }
            }
        }
        return matches;
    }    
    
//------------------------------------------------------------------------------
    
    /**
     * Extract '<code>key|separator|value</code>' field in a properly formatted
     * text block. 
     * This method is capable of handling single- and multi-line
     * records. For multiline records, two special labels are use:
     * <code>start</code> to identify the beginning of a multiline record
     * '<code>key|separator|value</code>', and <code>end</code> for the end.
     * Text in between these two labels will be seen as belonging to a
     * single '<code>key|separator|value</code>' where the 'key' must be
     * following <code>start</code> label in the same line.
     * <br>
     * For example, A single line '<code>key|separator|value</code>' is:
     * <\br><\br>
     * <code>thisIsAKey: this is the value</code>
     * <\br><\br>
     * For example, a multiline '<code>key|separator|value</code>' is:
     * (Using $START and $END as labels)
     * <br><br>
     * <code>$STARTthisIsAKey: this is part of the value<br>
     * this is still part of the value<br>
     * and also this<br>
     * $END</code>
     * <br><br>
     * In addition, a label is defined for the commented out lines.
     * All the labels must be in the very beginning of the line!
     * <b>WARNING:</b> This method cannot handle nested blocks.
     *
     * @param lines the text to analyse already divided into lines 
     * @param separator string defining the separator in
     * '<code>key|separator|value</code>'
     * @param commentLab string identifying the comments
     * @param start string identifying the beginning of a multiline block
     * @param end string identifying the end of a multiline block
     * @return the set of key values pairs.
     * @throws exception when the same key is found more than once.
     */

    public static TreeMap<String,String> readKeyValuePairs(
                        List<String> lines,
                        String separator, String commentLab,
                        String start, String end) throws Exception
    {
    	TreeMap<String,String> kvMap = new TreeMap<String,String>();
    	
    	List<List<String>> form = readKeyValue(lines,separator,
    			commentLab,start,end);
    	for (List<String> pair : form)
    	{
    		String key = pair.get(0);
    		String value = pair.get(1);
    		if (kvMap.containsKey(key))
    		{
    			throw new Exception("Duplicate key '" + key + "' found! "
    					+ "Cannot continue reading key:value pairs.");
    		}
    		kvMap.put(key, value);
    	}
    	return kvMap;
    }

//------------------------------------------------------------------------------

    /**
     * Extract '<code>key|separator|value</code>' field in a properly formatted
     * text block. The separator is optional, and, when not present the, a line 
     * line will be interpreted as a keyword without value.
     * This method is capable of handling single- and multi-line
     * records. For multiline records, two special labels are use:
     * <code>start</code> to identify the beginning of a multiline block,
     * and <code>end</code> for the end of it.
     * Text in the lined including and between these two labels will be seen as 
     * belonging to a single line, and thus to a single 
     * '<code>key|separator|value</code>'.
     * <br>
     * For example, a single line '<code>key|separator|value</code>' is:
     * <\br><\br>
     * <code>thisIsAKey: this is the value</code>
     * <\br><\br>
     * While a multiline '<code>key|separator|value</code>' is:
     * (Using $START and $END as labels)
     * <br><br>
     * <code>$STARTthisIsAKey: this is part of the value<br>
     * this is still part of the value<br>
     * and also this<br>
     * $END</code>
     * <br><br>
     * In addition, a label is defined for the commented out lines.
     * All the labels must be in the very beginning of the line!
     * <b>WARNING:</b> This method cannot handle nested blocks.
     *
     * @param lines the text to analyse already divided into lines 
     * @param separator string defining the separator in
     * '<code>key|separator|value</code>'
     * @param commentLab string identifying the comments
     * @param start string identifying the beginning of a multiline block
     * @param end string identifying the end of a multiline block
     * @return a table with all strings extracted according to the formatting
     * labels.
     */

    public static List<List<String>> readKeyValue(
                        List<String> lines,
                        String separator, String commentLab,
                        String start, String end)
    {   
        List<String> condencedLines = readTextWithMultilineBlocks(lines, 
        		commentLab, start, end);
        
        return readKeyValue(condencedLines, separator, commentLab);
    }
    
//------------------------------------------------------------------------------

    /**
     * Extract '<code>key|separator|value</code>' field in a properly formatted
     * text block. The separator is optional, and, when not present the, a line 
     * line will be interpreted as a keyword without value.
     * In addition, a label is defined for the commented out lines.
     *
     * @param lines the text to analyse already divided into lines 
     * @param separator string defining the separator in
     * '<code>key|separator|value</code>'
     * @param commentLab string identifying the comments
     * @return a table with all strings extracted according to the formatting
     * labels.
     */

    public static List<List<String>> readKeyValue(
    		List<String> lines, String separator, String commentLab)
    {
        List<List<String>> keysValues = new ArrayList<List<String>>();
        
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            line = StringUtils.escapeSpecialChars(line);
            
            //Skip commented out
            if (line.startsWith(commentLab))
                continue;
            	
            String key = "";
            String value = "";
            
            int indexKVSep = line.indexOf(separator);
            if (indexKVSep <= 0)
            {
            	key = line;
            } else {
            	key = line.substring(0,indexKVSep);
            }

            if (indexKVSep < line.length())
                value = line.substring(indexKVSep + 1);
            

            value = StringUtils.deescapeSpecialChars(value).trim();

            //Check parameter
            if (key.equals(""))
            {
                continue;
            }

            //Store
            List<String> singleBlock = new ArrayList<String>();
            singleBlock.add(key);
            singleBlock.add(value);
            keysValues.add(singleBlock);

        } 

        return keysValues;
    }
    
//------------------------------------------------------------------------------

    /**
     * Converts multilines blocks into single strings that includes newline 
     * characters (i.e., line separators), and collects them together with
     * single lines in a list of lines.
     * A multiline block is a group of lines of text that is identified
     * by a start and an end label. For instance, 
     * using $START and $END as labels:
     * <br><br>
     * <code>$STARTthisIsAKey: this is part of the value<br>
     * this is still part of the value<br>
     * and also this<br>
     * $END</code>
     * <br><br>
     * Nested blocks are also possible. In case of nesting, the outermost block
     * is the one what will be transformed into a single string, while inner 
     * blocks will remain labelled with the start/end labels.
     *
     * @param lines the text to analyse 
     * @param commentLab string identifying the comments
     * @param start string identifying the beginning of a multiline block
     * @param end string identifying the end of a multiline block
     * @return a list with all strings extracted 
     */

    public static List<String> readTextWithMultilineBlocks(
                        List<String> lines, String commentLab,
                        String start, String end)
    {
        //Start interpretation of the formatted text
        List<String> newLines = new ArrayList<String>();
        
        int nestingLevel = 0;
        List<String> blockOpeninglines = new ArrayList<String>();
        String growingLine = "";
        boolean isGrowing = false;
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            line = StringUtils.escapeSpecialChars(line);
            
            if (!isGrowing && line.startsWith(commentLab))
            {
            	continue;
            }
            
            
        	String subStr = line;
        	while (subStr.contains(start))
        	{
        		nestingLevel++;
        		blockOpeninglines.add(line);
        		subStr = subStr.substring(0,subStr.lastIndexOf(start));
        	}
        	subStr = line;
        	while (subStr.contains(end))
        	{
        		nestingLevel--;
        		blockOpeninglines.remove(blockOpeninglines.size()-1);
                subStr = subStr.substring(0,subStr.lastIndexOf(end));
        	}

        	if (nestingLevel == 0)
        	{
        		// Nested structure is all contained in one line (if it exists)
        		line = StringUtils.deescapeSpecialChars(line);
        		if (isGrowing)
        		{
        			// the line is the end of a previously opened nest, so
        			// we take away the rightmost 'end' label
        			line = line.substring(0,line.lastIndexOf(end))
        					+ line.substring(line.lastIndexOf(end) 
        							+ end.length());
        			// and append it to the previously collected parts of line
        			line = growingLine + NL + line;
        			// As we have finished appending, we reset the pointer
        			isGrowing = false;
        		}
        		// The line is now ready to be archived.
                newLines.add(line);
        	} 
        	else if (nestingLevel > 0) 
        	{
        		// We are inside the nested system, so we append lines
        		if (!isGrowing)
        		{
        			// We just entered the outermost nest. Take note of level
        			isGrowing = true;
        			// Take away the leftmost 'start' label, and start growing 
        			// the new collective line (which will contain newLine chars
        			
        			growingLine = line.substring(0,line.indexOf(start))
        					+ line.substring(line.indexOf(start) 
        							+ start.length());
        			// To keep growing we jump to the next iteration of the loop
        			continue;
        		}
        		else
        		{
        			growingLine = growingLine + NL + line;
        			continue;
        		}
        	} 
        	else if (nestingLevel < 0) 
        	{
        		StringBuilder sb = new StringBuilder();
        		for (String l : lines)
        		{
        			sb.append(l + NL);
        		}
        		Terminator.withMsgAndStatus("ERROR! There is an label '" 
        				+ ChemSoftConstants.JDCLOSEBLOCK+ "' indicating the "
        				+ "end of a multi line block of text, but no opening "
        				+ "label '" + ChemSoftConstants.JDOPENBLOCK + "' was "
        				+ "found before this point. Check input up to this: "
        				+ NL + sb.toString(), -1);
        	}
        }
        
        if (nestingLevel>0)
        {
        	String msg = NL + "ERROR! ";
        	if (nestingLevel > 1)
        	{
        		msg = msg + "There are "+nestingLevel+" unterminated multi "
        				+ "line blocks. " + NL + "The following lines "
                		+ "opened blocks that could not closed: ";
        	} else {
        		msg = msg + "There is 1 unterminated multi line block. " + NL
        				+ "The following line opened a block that could not be "
        				+ "closed: ";
        	}
            for (String l : blockOpeninglines)
            {
            	msg = msg + NL + " -> '" + l + "'";
            }
        	Terminator.withMsgAndStatus(msg, -1);
        }
        if (nestingLevel<0)
        {
        	Terminator.withMsgAndStatus("ERROR! There is an label '" 
    				+ ChemSoftConstants.JDCLOSEBLOCK+ "' indicating the "
    				+ "end of a multi line block of text, but no opening "
    				+ "label '" + ChemSoftConstants.JDOPENBLOCK + "' was "
    				+ "found before the last line of text.", -1);
        }
        
        return newLines;
    }

//------------------------------------------------------------------------------

}
