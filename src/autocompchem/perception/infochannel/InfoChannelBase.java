package autocompchem.perception.infochannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

import java.util.Set;

/**
 * A list of information channels
 * 
 * @author Marco Foscato
 */

public class InfoChannelBase
{
    /**
     * List of information channels
     */
    private ArrayList<InfoChannel> allInfoChans = new ArrayList<InfoChannel>();

    /**
     * Indexing of InfoChannels by type
     */
    private Map<InfoChannelType,ArrayList<InfoChannel>> mapByICType = 
                          new HashMap<InfoChannelType,ArrayList<InfoChannel>>();

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty InfoChannelBase
     */

    public InfoChannelBase() 
    {
    }

//------------------------------------------------------------------------------

    /**
     * Add a channel
     * @param channel the channel to be included in this list
     */

    public void addChannel(InfoChannel channel)
    {
        allInfoChans.add(channel);

//TODO deal with UNDEFINED type as if it was a whildcard

        //Indexing by InfoChannelType
        InfoChannelType ict = channel.getType();
        if (mapByICType.keySet().contains(ict))
        {
            mapByICType.get(ict).add(channel);
        }
        else
        {
            mapByICType.put(ict,
                            new ArrayList<InfoChannel>(Arrays.asList(channel)));
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the number of channels in this collection
     * @return the number of information channels
     */
    
    public int getInfoChannelCount()
    {
    	return allInfoChans.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get all information channel types
     * @return all the information channel types
     */

    public Set<InfoChannelType> getAllChannelType()
    {
        return mapByICType.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Get all channels of a specific type
     * @param itc the specific info channel type
     */

    public ArrayList<InfoChannel> getChannelsOfType(InfoChannelType ict)
    {
        return mapByICType.get(ict);
    }

//------------------------------------------------------------------------------

    /**
     * Print the list of information channels to STDOUT
     */

    public void printInfoChannels()
    {
        String newline = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append(newline).append("Information Channels:").append(newline);
        for (InfoChannel ic : allInfoChans)
        {
            sb.append(" -> ").append(ic.toString()).append(newline);
        }
        System.out.println(sb.toString());
    }

//------------------------------------------------------------------------------

    /**
     * Return a string describing this list
     * @return a human readable description of the list
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("InfoChannelBase [");
        for (InfoChannel ic : allInfoChans)
        {
            sb.append(ic.toString()).append("; ");
        }
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
