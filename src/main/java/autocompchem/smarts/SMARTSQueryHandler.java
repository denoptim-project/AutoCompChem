package autocompchem.smarts;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import autocompchem.run.Terminator;
import autocompchem.utils.NumberAwareStringComparator;

/**
 * Functionality to deal with SMARTS-based queries that may include multiple 
 * SMARTS strings and additional non-SMARTS components
 */
public class SMARTSQueryHandler 
{
    /**
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);
    
    /**
     * Storage of SMARTS queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Label used to identify single-atom smarts in the smarts reference name
     */
    private static final String SUBRULELAB = "_p";

    /**
     * Root of the smarts reference names
     */
    private static final String SMARTSNAMEROOT = "smarts ";
    
    /**
     * Verbosity level
     */
    private int verbosity = 0;
    
//------------------------------------------------------------------------------

    /**
     * get the sorted list of master names 
     */

    private ArrayList<String> getSortedSMARTSRefNames(Map<String,String> smarts)
    {
        ArrayList<String> sortedMasterNames = new ArrayList<String>();
        for (String k : smarts.keySet())
        {
            String[] p = k.split(SUBRULELAB);
            if (!sortedMasterNames.contains(p[0]))
            {
                sortedMasterNames.add(p[0]);
            }
        }
        Collections.sort(sortedMasterNames, new NumberAwareStringComparator());
        return sortedMasterNames;
    }

//------------------------------------------------------------------------------

    /**
     * Read options and values associated with SMARTS queries. 
     * This methos collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumend to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @param smarts the map of previously red SMARTS queries for which this
     * method is collecting the options
     * @return the map of all named details. Naming is based on incremental and
     * unique indexing where an integer index is used to identify the list of 
     * strings red from the same line.
     */

    private Map<String,ArrayList<String>> getOptsForNamedICSMARTS(
                                     String allLines, Map<String,String> smarts)
    {
        ArrayList<String> sortedMasterNames = getSortedSMARTSRefNames(smarts);
        
        Map<String,ArrayList<String>> map = 
                                        new HashMap<String,ArrayList<String>>();
        if (verbosity > 1)
        {
            System.out.println(" Importing options for IC-identifying SMARTS");
        }
        String[] lines = allLines.split("\\r?\\n");
        int ii=-1;
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            ii++;
            String[] parts = lines[i].split("\\s+");
            ArrayList<String> lstDetails = new ArrayList<String>();
            for (int j=0; j<parts.length; j++)
            {
                String str = parts[j].trim();

                // Ignore single-atom SMARTS
                if (str.equals("") || SMARTS.isSingleAtomSMARTS(str))
                {
                    continue;
                }

                lstDetails.add(str);
            }
            map.put(sortedMasterNames.get(ii),lstDetails);
        }

        return map;
    }

//------------------------------------------------------------------------------

    /**
     * Reads SMARTS for defining internal coordinates. 
     * This method collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumed to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @return the map of all named smarts. Naming is based on incremental and
     * unique indexing where a major index is used to identify sets of SMARTS
     * red from the same line, and another index is used to identify the 
     * order of the SMARTS red in the same line.
     */

    private Map<String,String> getNamedICSMARTS(String allLines)
    {
        Map<String,String> map = new HashMap<String,String>();
        if (verbosity > 1)
        {
            System.out.println(" Importing SMARTS to identify ICs");
        }
        String[] lines = allLines.split("\\r?\\n");
        int ii = -1;
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            // This allows to retrace the exact order in which lines are
            // given, yet without using the line number as index and allowing
            // to store multiple blocks of SMARTS queries in the same map
            ii = iNameSmarts.getAndIncrement();
            String masterName = SMARTSNAMEROOT + ii;

            String[] parts = lines[i].split("\\s+");
            int jj = -1;
            for (int j=0; j<parts.length; j++)
            {
                String singleSmarts = parts[j].trim();

                // Ignore any string that is not a single-atom SMARTS
                if (singleSmarts.equals("") ||
                    !SMARTS.isSingleAtomSMARTS(singleSmarts))
                {
                    continue;
                }

                if (jj > 3)
                {
                    Terminator.withMsgAndStatus("ERROR! More than 4 atomic "
                               + "SMARTS for IC-defining SMARTS rule "
                               + ii + " (last SMARTS:" + singleSmarts + "). "
                               + "These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check the input.",-1);
                }
                jj++;
                String childName = masterName + SUBRULELAB + jj;
                map.put(childName,singleSmarts);
            }
            if (jj < 1)
            {
                Terminator.withMsgAndStatus("ERROR! Less than 2 atomic "
                               + "SMARTS for IC-freezing SMARTS rule "
                               + ii + ". These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check input.",-1);
            }
        }
        return map;
    }

//------------------------------------------------------------------------------

    /**
     * Parses SMARTS from a single string containing SMARTS queries (one 
     * per line), and returns named SMARTS in a map. The names are unique, but
     * arbitrary chosen.
     * @param allLines the string collecting all lines in one single string that
     * contains newline characters.
     */

    //TODO: use SMARTS object
    
    public static Map<String,String> getNamedAtomSMARTS(String allLines)
    {
        Map<String,String> map = new HashMap<String,String>();

        String[] lines = allLines.split("\\r?\\n");
        for (int i=0; i<lines.length; i++)
        {
            String singleSmarts = lines[i].trim();
            if (singleSmarts.equals(""))
            {
                continue;
            }
            String singleSmartsName = SMARTSNAMEROOT + i; 
            //		+ iNameSmarts.getAndIncrement();
            map.put(singleSmartsName,singleSmarts);
        }
        return map;
    }
    
//------------------------------------------------------------------------------


}
