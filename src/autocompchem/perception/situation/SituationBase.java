package autocompchem.perception.situation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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

import java.util.Map;

import autocompchem.files.FileUtils;
import autocompchem.perception.infochannel.InfoChannelBase;
import autocompchem.perception.infochannel.InfoChannelType;


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
    private ArrayList<Situation> allSituations = new ArrayList<Situation>();

    /**
     * Indexing of situations by info channel type
     */
    private Map<InfoChannelType,ArrayList<Situation>> situationsByICType =
                          new HashMap<InfoChannelType,ArrayList<Situation>>();

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty SituationBase
     */

    public SituationBase() 
    {
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a database of known situations from a collection of files.
     * Searches the given root folder and in its sub folders.
     * @param rootFolder root of the folder tree to be searched.
     */

    public SituationBase(File rootFolder)
    {
    	// WARNING: we only look for files with the expected formats
        ArrayList<File> listFiles = FileUtils.find(rootFolder,
        		SituationConstants.SITUATIONTXTFILEEXT);
        //listFiles.addAll(FilesManager.find(pathNameRoot,
        //		SituationConstants.SITUATIONXMLFILEEXT));

        for (File f : listFiles)
        {   
        	try {
				Situation s = new Situation(f);
				this.addSituation(s);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("WARNING! Unable to make a known Situation "
						+ "from file '" + f.getAbsolutePath() + "'. I ignore "
						+ "such file, but I keep going on.");
			}
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add a situation
     * @param situation the situation to be included in this situation base
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
    }

//------------------------------------------------------------------------------

    /**
     * Extract situations that are relevant for a given information channel type
     * @param ict the type of information channel
     * @return the set of relevant situations
     */

    public ArrayList<Situation> getRelevantSituations(InfoChannelType ict)
    {
        return situationsByICType.get(ict);
    }

//------------------------------------------------------------------------------

    /**
     * Extract situations that are relevant for a given information channel base
     * @param icb the of information channels made available
     * @return the map of relevant situations by info channel type
     */

    public Map<InfoChannelType,ArrayList<Situation>> 
                                 getRelevantSituationsByICT(InfoChannelBase icb)
    {
        Map<InfoChannelType,ArrayList<Situation>> relevantSituations = 
                           new HashMap<InfoChannelType,ArrayList<Situation>>();
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

    public ArrayList<Situation> getRelevantSituations(InfoChannelBase icb) 
    {
        ArrayList<Situation> relevantSituations = new ArrayList<Situation>();
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
     * Prints the situations grouped by InfoChannelType. Prints on STDOUT
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
