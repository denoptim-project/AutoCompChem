package autocompchem.text;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autocompchem.run.Terminator;
import autocompchem.utils.StringUtils;


/**
 * Tool for analysing text stream
 * 
 * @author Marco Foscato
 */

public class TextAnalyzer
{

    public static String newline = System.getProperty("line.separator");

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
     * Count lines containing a patter. 
     * @param buffRead the source of text
     * @param query patters string. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @return the number of lines matching (1-n, does not work as .size()!)
     */

    public static int count(BufferedReader buffRead, String query)
    {
        boolean[] startMidEnd = getMatchingMethod(query);

        int num = 0;
        try {
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
            	String pattern = preparePattern(query,startMidEnd);
                if (match(line,pattern,startMidEnd))
                    num++;
            }
        } catch (Throwable t) {
        	//none
        } 

        return num;
    }

//------------------------------------------------------------------------------

    /**
     * Count lines containing patterns. This method correspond to run the Linux 
     * command 
     * <code> ... | grep -c "some pattern" AND grep -n "some pattern" </code>
     * The returned array contains both the line number of the matches
     * and the counts. The last ArrayList in the returned
     * ArrayList of ArrayLists is the one that contains the counts, 
     * while all the other
     * ArrayLists contain the line numbers
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

    public static ArrayList<ArrayList<Integer>> count(BufferedReader buffRead,
                                                        ArrayList<String> lsStr)
    {
        //Prepare storage of counts
        ArrayList<Integer> counts = new ArrayList<Integer>(lsStr.size());
        ArrayList<ArrayList<Integer>> lineNums = 
                                            new ArrayList<ArrayList<Integer>>();
        for (int i=0; i<lsStr.size(); i++)
        {
            counts.add(0);
            ArrayList<Integer> ln = new ArrayList<Integer>();
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

    public static ArrayList<String> extractTxtWithDelimiters(
                                                     BufferedReader buffRead,
                                                             String pattern1,
                                                             String pattern2,
                                                             boolean inclPatts)
    {
        ArrayList<ArrayList<String>> blocks = 
                                     extractMultiTxtBlocksWithDelimiters(
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

    public static ArrayList<ArrayList<String>>
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

    public static ArrayList<ArrayList<String>> 
                         extractMultiTxtBlocksWithDelimiters(
                                                       BufferedReader buffRead,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        ArrayList<ArrayList<String>> blocks =
                                             new ArrayList<ArrayList<String>>();

        Map<String,ArrayList<String>> mapBlocks =
                                  extractMapOfTxtBlocksWithDelimiters(
                                                              buffRead,
                                                              startPattrns,
                                                              endPattrns,
                                                              onlyFirst,
                                                              inclPatts);

//TODO check leftover?
        ArrayList<String> sortKeys = new ArrayList<String>();
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

    public static TreeMap<String,ArrayList<String>>
                        extractMapOfTxtBlocksWithDelimiters(
                                                       BufferedReader buffRead,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
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

    public static ArrayList<TextBlock> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                            String startPattrn,
                                                              String endPattrn,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        ArrayList<String> startPattrns = new ArrayList<String>();
        ArrayList<String> endPattrns = new ArrayList<String>();
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

    public static ArrayList<TextBlock> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
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

    public static ArrayList<TextBlock> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                   ArrayList<String> slPattrns,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
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

    public static ArrayList<TextBlock> extractTextBlocks(
                                                       BufferedReader buffRead,
                                                   ArrayList<String> slPattrns,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts,
                                                      boolean expandBeyondNests)
    {
        // This is what we well return
        ArrayList<TextBlock> blocks = new ArrayList<TextBlock>();

        // Initialise counters
        ArrayList<Integer> countsSL = new ArrayList<Integer>(slPattrns.size());
        for (int pattIdx=0; pattIdx<slPattrns.size(); pattIdx++)
        {
            countsSL.add(-1);
        }
        ArrayList<Integer> countsML =
                                    new ArrayList<Integer>(startPattrns.size());
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
                Map<Integer,TextBlock> openBlocks =
                                               new HashMap<Integer,TextBlock>();

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
                            ArrayList<String> block = new ArrayList<String>();
                            block.add(line);

                            //
                            // WARNING: Single line matches cannot be nested
                            // 

                            TextBlock tb = new TextBlock(block,countMatches,
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

                            TextBlock tb = new TextBlock(countMatches,
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
                            ArrayList<Integer> sortedOpnBlkKeys =
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
                            TextBlock thisBlock = openBlocks.get(newstOBlk);

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

    public static TreeMap<String,ArrayList<String>>
                        extractMapOfTxtBlocksWithDelimiters(
                                                        BufferedReader buffRead,
                                                   ArrayList<String> slPattrns,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        TreeMap<String,ArrayList<String>> blocks = 
                new TreeMap<String,ArrayList<String>>(new MetchKeyComparator());
        ArrayList<Integer> countsSL = new ArrayList<Integer>(slPattrns.size());
        for (int pattIdx=0; pattIdx<slPattrns.size(); pattIdx++)
        {
            countsSL.add(-1);
        }
        ArrayList<Integer> countsML = 
                                    new ArrayList<Integer>(startPattrns.size());
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
                Map<Integer,ArrayList<String>> openBlocks = 
                                       new HashMap<Integer,ArrayList<String>>();
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
                            ArrayList<String> block = openBlocks.get(newstOBlk);
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

        if (str.startsWith("*") && (!str.endsWith("*")))
        {
            startMidEnd[0] = false;
            startMidEnd[1] = false;
            startMidEnd[2] = true;
        } else if (str.endsWith("*") && (!str.startsWith("*")))
        {
            startMidEnd[0] = true;
            startMidEnd[1] = false;
            startMidEnd[2] = false;
        } else if (str.startsWith("*") && str.endsWith("*"))
        {
            startMidEnd[0] = false;
            startMidEnd[1] = true;
            startMidEnd[2] = false;
        } else if ((!str.startsWith("*")) && (!str.endsWith("*")))
        {
            startMidEnd[0] = false;
            startMidEnd[1] = true;
            startMidEnd[2] = false;
        }

        return startMidEnd;
    }
    
//------------------------------------------------------------------------------
    
    private static String preparePattern(String pattern, boolean[] relation)
    {
    	String newPattern = pattern;
        if (relation[0])
        {
        	newPattern = pattern.substring(1);
        } else if (relation[1])
        {
        	newPattern = pattern.substring(1,pattern.length() - 1);
        } else if (relation[2])
        {
        	newPattern = pattern.substring(0,pattern.length() - 1);
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

    private static boolean match(String line, String pattern, boolean[] relation)
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
                        ArrayList<String> lines,
                        String separator, String commentLab,
                        String start, String end) throws Exception
    {
    	TreeMap<String,String> kvMap = new TreeMap<String,String>();
    	
    	ArrayList<ArrayList<String>> form = readKeyValue(lines,separator,
    			commentLab,start,end);
    	for (ArrayList<String> pair : form)
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
     * @return a table with all strings extracted according to the formatting
     * labels.
     */

    public static ArrayList<ArrayList<String>> readKeyValue(
                        ArrayList<String> lines,
                        String separator, String commentLab,
                        String start, String end)
    {
        //Start interpretation of the formatted text
        ArrayList<ArrayList<String>> filledForm =
                                new ArrayList<ArrayList<String>>();
        int nestingLevel = 0;
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            line = StringUtils.escapeSpecialChars(line);

            //Check if it is a multiple line block
            if (line.contains(start))
            {
            	nestingLevel++;
            	
            	// More nesting on this line?
            	String subStr = line.substring(0,line.lastIndexOf(start));
            	while (subStr.contains(start))
            	{
            		nestingLevel++;
            		subStr = subStr.substring(0,subStr.lastIndexOf(start));
            	}
            	
            	// remove the left-most $start
                int idFirstStart = line.indexOf(start);
            	line = line.substring(0,idFirstStart) + line.subSequence(idFirstStart+start.length(), line.length());
            	
                //Check the value of the index of the separator
                int indexKVSep = line.indexOf(separator);
                if (indexKVSep <= 0)
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-1! "
                        + "Check line " + lines.get(i),-1);
                }
                
                //define parameter's key and possibly value
                String key = line.substring(0,indexKVSep);
                String value = "";
                if (indexKVSep < line.length())
                    value = line.substring(indexKVSep + 1);

                //Read other lines
                boolean goon = true;
                while (goon)
                {
                    if ((i+1) >= lines.size())
                    {
                        Terminator.withMsgAndStatus("ERROR"
                            + " ReadFormattedText-2a! NL: "+nestingLevel+" "
                            + " Cannot go beyond last line '" + lines.get(i) + "'.",-1);
                    }
                    i++;
                    String otherLine = lines.get(i);

                    // deal with further nesting
                    if (otherLine.contains(start))
                    {
                    	nestingLevel++;
                    	// More nesting on this line?
                    	String subStr2 = otherLine.substring(0,otherLine.lastIndexOf(start));
                    	while (subStr2.contains(start))
                    	{
                    		nestingLevel++;
                    		subStr2 = subStr2.substring(0,subStr2.lastIndexOf(start));
                    	}
                    }
                    
                    if (otherLine.contains(end))
                    {
                    	// Here we deal with the possibility that multiple 'end' 
                    	// labels are on the same line
                    	String sub = otherLine;
                    	while (sub.contains(end) && nestingLevel>0)
                    	{
                    		nestingLevel--;
                    		sub = sub.substring(0,sub.lastIndexOf(end));
                    	}

                    	if (nestingLevel==0)
                    	{
                    		otherLine = otherLine.substring(0,otherLine.lastIndexOf(end));
	                        goon = false;
                    	}
                    } 
                    value = value + newline + otherLine;
                }

                //Prepare key and value
                key = key.toUpperCase();
                value = value.trim();
                value = StringUtils.deescapeSpecialChars(value);

                //Check parameter
                if ((key.equals("")) || (value.equals("")))
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-3! "
                            + "Check line " + lines.get(i),-1);
                }

                //Store
                ArrayList<String> singleBlock = new ArrayList<String>();
                singleBlock.add(key);
                singleBlock.add(value);
                filledForm.add(singleBlock);

            } else {
                //Skip commented out
                if (line.startsWith(commentLab))
                    continue;

                //Read Single line parameter
                int indexKVSep = line.indexOf(separator);
                //Check the value of the index
                if (indexKVSep <= 0)
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-4! "
                        + "There seem to be no key in line " + lines.get(i),-1);
                }
                if (indexKVSep == line.length())
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-5! "
                      + "There seem to be no value in line " + lines.get(i),-1);
                }

                String key = line.substring(0,indexKVSep);
                String value = line.substring(indexKVSep + 1);

                key = key.toUpperCase();
                value = value.trim();

                //Check
                if ((key.equals("")) || (value.equals("")))
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-6! "
                      + "Blank key and blank value in line " + lines.get(i),-1);
                }

                //Store
                ArrayList<String> singleBlock = new ArrayList<String>();
                singleBlock.add(key);
                singleBlock.add(value);
                filledForm.add(singleBlock);
           }
        } //End of loop over lines

        return filledForm;
    }

//------------------------------------------------------------------------------

}
