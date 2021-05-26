package autocompchem.files;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlockIndexed;

/**
 * Tool for analysing txt files
 * 
 * @author Marco Foscato
 */

public class FileAnalyzer
{

//------------------------------------------------------------------------------

    /**
     * Construct an empty FilesAnalyzer
     */

    public FileAnalyzer()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Count lines containing a patter. This method correspond to the Linux 
     * command grep -c "some pattern" filename.
     * @param filename name of the file to be analysed
     * @param str patters string. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @return the number of lines matching (1-n, does not work as .size()!)
     */

    public static int count(String filename, String str)
    {
        int num = 0;
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(filename));
            num = TextAnalyzer.count(buffRead,str);
        } catch (Throwable t) {
            badTermination = true;
            msg = t.getMessage();            
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                badTermination = true;
                msg = t2.getMessage();                
            }
        }

        if (badTermination)
            Terminator.withMsgAndStatus("ERROR! " + msg, -1);

        return num;
    }

//------------------------------------------------------------------------------

    /**
     * Count lines containing patterns. This method correspond to run the Linux 
     * command grep -c "some pattern" filename AND grep -n "some pattern" in 
     * once. The returned array contains both the linenumber of the matches
     * and the counts. The last ArrayList in the returned
     * ArrayList of ArrayLists is the one that contains the counts, 
     * while all the other
     * ArrayLists contain the line numbers
     * @param filename name of the file to be analysed
     * @param lsStr list of pattern strings. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the pattern
     * will be searches in all the line (as for *blabla*).
     * If a line matches more than one query it is counted more than one
     * @return the counts per each pattern and the line numbers lists 
     * (1-n, does not work as .size()!) 
     */

    public static ArrayList<ArrayList<Integer>> count(String filename, 
                                                        ArrayList<String> lsStr)
    {
        ArrayList<ArrayList<Integer>> counts = 
                                            new ArrayList<ArrayList<Integer>>();
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(filename));
            counts = TextAnalyzer.count(buffRead,lsStr);
        } catch (Throwable t) {
            badTermination = true;
            msg = t.getMessage();
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                badTermination = true;
                msg = t2.getMessage();
            }
        }

        if (badTermination)
            Terminator.withMsgAndStatus("ERROR! " + msg, -1);

        return counts;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts the lines between the first matches of given patterns.
     * The patterns are intended to match the entire line.
     * @param inFile the file to read
     * @param pattern1 identifies the beginning of the target section
     * @param pattern2 identifies the end of the target section
     * @param inclPatts set to <code>true</code> to include the lines
     * containing <code>pattern1</code> and <code>pattern2</code> into the
     * target section.
     * @return the target section as array of lines
     */

    public static ArrayList<String> extractTxtWithDelimiters(String inFile,
                                                             String pattern1,
                                                             String pattern2,
                                                             boolean inclPatts)
    {
        ArrayList<ArrayList<String>> blocks = 
                                     extractMultiTxtBlocksWithDelimiters(inFile,
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
     * @param inFile the file to read
     * @param pattern1 identifies the beginning of the target section.
     * @param pattern2 identifies the end of the target section
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the firts match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing <code>pattern1</code> and <code>pattern2</code> into the
     * target section.
     * @return the list of target sections, each as an array of lines
     */

    public static ArrayList<ArrayList<String>>
                         extractMultiTxtBlocksWithDelimiters(String inFile,
                                                             String pattern1,
                                                             String pattern2,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        return extractMultiTxtBlocksWithDelimiters(inFile,
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
     * @param inFile the file to read
     * @param startPattrns list of patterns that identify the beginning of a 
     * target section. Each entry must have a corresponding one in endPattrns
     * @param endPattrns list of patterns that identify the end of a 
     * target section. Each entry must have a corresponding one in startPattrns
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the firts match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the
     * target section.
     * @return the list of target sections, each as an array of lines
     */

    public static ArrayList<ArrayList<String>> 
                         extractMultiTxtBlocksWithDelimiters(String inFile,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        ArrayList<ArrayList<String>> blocks =
                                             new ArrayList<ArrayList<String>>();

        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(inFile));
            blocks = TextAnalyzer.extractMultiTxtBlocksWithDelimiters(buffRead,
                                                              startPattrns,
                                                              endPattrns,
                                                              onlyFirst,
                                                              inclPatts);
        } catch (Throwable t) {
            badTermination = true;
            msg = t.getMessage();
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                badTermination = true;
                msg = t2.getMessage();
            }
        }

        if (badTermination)
            Terminator.withMsgAndStatus("ERROR! " + msg, -1);

        return blocks;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts blocks of text from text file.
     * Extracts multiple blocks of lines that are delimited by one of the given
     * pairs of REGEX.
     * Each REGEX pattern is intended to match text within a single line and to
     * match the entire line.
     * Nested blocks are dealt with, and the returned list includes only the
     * first, outermost level. Nested TextBlocks are embedded inside their
     * nesting TextBlocks.
     * @param pathName the file to read
     * @param startPattrn REGEXs that identify the beginning of a
     * target section.
     * @param endPattrn REGEXs that identify the end of a
     * target section.
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the firts match.
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @return a list of matched text blocks that may include nested blocks.
     */

    public static ArrayList<TextBlockIndexed> extractTextBlocks(String pathName,
                                                            String startPattrn,
                                                              String endPattrn,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        ArrayList<String> startPattrns = new ArrayList<String>();
        ArrayList<String> endPattrns = new ArrayList<String>();
        startPattrns.add(startPattrn);
        endPattrns.add(endPattrn);
        
        FileUtils.foundAndPermissions(pathName,true,false,false);
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(pathName));
        }
        catch (Exception e)
        {
            // the file manager will have stopped us before this point
            Terminator.withMsgAndStatus("ERROR! File "+ pathName
                                                          + " not found!.",-1);
        }
        ArrayList<TextBlockIndexed> blocks = 
              TextAnalyzer.extractTextBlocks(br, new ArrayList<String>(),
                                                                  startPattrns,
                                                                    endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts,
                                                                         false);
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
     * @param inFile the file to read
     * @param startPattrns list of patterns that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns
     * @param endPattrns list of patterns that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the firts match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the
     * target section.
     * @return a map with all the target sections, each as an array of lines,
     * and each identified by a key wich corresponds to the following format:
     * a_b_c where "a" is the position of the block in the overall sequence of
     * block (sorted according to their appearence in the file) [i.e., a
     * number 0-n],
     * "b" is the index of the pair of patterns in the
     * startPattrns/endPattrns lists [i.e., an integer 0-n],
     * and "c" is the index of the block among blocks of same type
     * [i.e., an integer 0-n].
     */

    public static TreeMap<String,ArrayList<String>>
                        extractMapOfTxtBlocksWithDelimiters(String inFile,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        return               extractMapOfTxtBlocksWithDelimiters(inFile,
                                                       new ArrayList<String>(),
                                                                  startPattrns,
                                                                    endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts);
    }

//------------------------------------------------------------------------------

    /**
     * Extracts text from text files: single lines matching any of the REGEX,
     * and multiple blocks of lines that are delimited by one of the given
     * pairs of patters.
     * Each patter is intended to match text within a single line. 
     * Nested blocks are dealt with.
     * @param inFile the file to read
     * @param slPattrns the list of patterns meant to match single line content
     * @param startPattrns list of patterns that identify the beginning of a
     * target section. Each entry must have a corresponding one in endPattrns
     * @param endPattrns list of patterns that identify the end of a
     * target section. Each entry must have a corresponding one in startPattrns
     * @param onlyFirst set to <code>true</code> to stop the search after the
     * end of the firts match
     * @param inclPatts set to <code>true</code> to include the lines
     * containing the patterns into the target section.
     * @return a map with all the target sections, each as an array of lines,
     * and each identified by a key wich corresponds to the following format:
     * a_b_c where "a" is the position of the line or block in the overall 
     * sequence of matches lines of blocks of lines
     * (sorted according to their appearence in the file) [i.e., a 
     * number 0-n], 
     * "b" is the index of the pattern, for single line matches, or, for multi 
     * line bloks, of the pair of patterns in the slPattrns or
     * startPattrns/endPattrns lists [i.e., an integer 0-n],
     * and "c" is the index of the block among blocks of same type 
     * [i.e., an integer 0-n].
     */

    public static TreeMap<String,ArrayList<String>>
                        extractMapOfTxtBlocksWithDelimiters(String inFile,
                                                   ArrayList<String> slPattrns,
                                                ArrayList<String> startPattrns,
                                                  ArrayList<String> endPattrns,
                                                             boolean onlyFirst,
                                                             boolean inclPatts)
    {
        TreeMap<String,ArrayList<String>> blocks = 
                new TreeMap<String,ArrayList<String>>(new MetchKeyComparator());

        BufferedReader buffRead = null;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(inFile));
            blocks = TextAnalyzer.extractMapOfTxtBlocksWithDelimiters(buffRead,
                                                                     slPattrns,
                                                                  startPattrns,
                                                                    endPattrns,
                                                                     onlyFirst,
                                                                     inclPatts);
        } catch (Throwable t) {
            msg = t.getMessage();
            Terminator.withMsgAndStatus("ERROR! Unable to read file " + inFile
                + ". " + msg,-1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                msg = t2.getMessage();
                Terminator.withMsgAndStatus("ERROR! Unable to read file " 
                    + inFile + ". " + msg,-1);
            }
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
     * @param inFile the file to read
     * @param pattern1 identifies the beginning of the target section
     * @param size the number of lines to keep after the match
     * @param inclPatt set to <code>true</code> to include the line
     * containing <code>pattern1</code> into the
     * target section.
     * @return the list of target sections, each as an array of lines
     */

    public static ArrayList<ArrayList<String>>
                     extractMultiTxtBlocksWithDelimiterAndSize(String inFile,
                                                               String pattern1,
                                                               int size,
                                                               boolean inclPatt)
    {
        ArrayList<ArrayList<String>> blocks =
                                             new ArrayList<ArrayList<String>>();
        BufferedReader buffRead = null;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(inFile));
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
            Terminator.withMsgAndStatus("ERROR! Unable to read file " + inFile
                + ". " + msg,-1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                msg = t2.getMessage();
                Terminator.withMsgAndStatus("ERROR! Unable to read file "
                    + inFile + ". " + msg,-1);
            }
        }
        return blocks;
    }

//------------------------------------------------------------------------------

    /**
     * Find matches line by line
     * @param inFile the file to analyze
     * @param patterns the list of patterns to find
     * @return a list of the matches: each entry is a line matching any of the 
     * queries
     */
   
    public static ArrayList<String> grep(String inFile, Set<String> patterns)
    {
        ArrayList<String> matches = new ArrayList<String>();
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(inFile));
            matches = TextAnalyzer.grep(buffRead,patterns);
        } catch (Throwable t) {
            badTermination = true;
            msg = t.getMessage();
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                badTermination = true;
                msg = t2.getMessage();
            }
        }

        if (badTermination)
            Terminator.withMsgAndStatus("ERROR! " + msg, -1);
        
        return matches;
    }    

//------------------------------------------------------------------------------

}
