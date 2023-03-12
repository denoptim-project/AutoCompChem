package autocompchem.perception.circumstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

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

import autocompchem.perception.infochannel.InfoChannelType;

/**
 * A general circumstance reflects a condition that can manifest itself in 
 * in a specific information channel.
 *
 * @author Marco Foscato
 */

public class Circumstance implements ICircumstance
{	
    /**
     * Information channel where this circumstance occurs
     */
    private InfoChannelType ict = InfoChannelType.NOTDEFINED;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty Circumstance with a specific information channel
     */

    public Circumstance(InfoChannelType ict)
    {
    	if (ict!=null)
    		this.ict = ict;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value between 0.0 and 1.0
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param input an object that contains all information
     * needed to calculate the satisfaction score. Can be null if not needed.
     * @return numerical score 
     */

    public double calculateScore(Object input)
    {
        return 0.0;
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of information channel where this circumstance manifests
     * itself
     * @return the channel type
     */

    public InfoChannelType getChannelType()
    {
        return ict;
    }

//------------------------------------------------------------------------------

    /**
     * Set the type of information channel where this circumstance manifests
     * itself
     * @param ict the type of channel
     */

    public void setChannelType(InfoChannelType ict)
    {
        this.ict = ict;
    }

//------------------------------------------------------------------------------

    //TODO
    /**
     * Convert a score from numeric to boolean. Uses a threshold that can be set
     * by method ___TODO: write method__.
     * @param dScore the score in numeric double
     * @return a true/false value
     */

    public boolean scoreToDecision(double dScore)
    {
        Boolean res = false;
        if (dScore >= 1.0)
        {
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable representation
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" [channelType:").append(ict);
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

	@Override
	public TreeMap<String, JsonElement>  getJsonMembers(
			JsonSerializationContext context) 
	{
		TreeMap<String, JsonElement> map = new TreeMap<String, JsonElement>();
		map.put("channel", context.serialize(ict));
		return map;
	}

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o== null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
            return false;
         
        Circumstance other = (Circumstance) o;
        
        if (!this.ict.equals(other.ict))
            return false;
        
        return true;
    }

//------------------------------------------------------------------------------

}
