package autocompchem.datacollections;

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

import java.util.HashMap;
import java.util.Map;

import autocompchem.run.Terminator;

/**
 * Storage of {@link NamedData} objects that are collected by reference.
 *
 * @author Marco Foscato
 */

public class NamedDataCollector implements Cloneable
{

    /**
     * The data structure collecting the NamedData by their reference name 
     * (i.e., key)
     */
	
    protected Map<String,NamedData> allData;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty NamedDataCollector
     */

    public NamedDataCollector()
    {
        allData = new HashMap<String,NamedData>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor from a filled map of NamedData
     * @param allData the map of NamedData
     */
    
    public NamedDataCollector(Map<String,NamedData> allData)
    {
        this.allData = allData;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>false</code> if this NamedDataCollector contains NamedData
     */

    public boolean isEmpty()
    {
        return allData.isEmpty();
    }

//------------------------------------------------------------------------------

    /**
     * Return the NamedData required or null
     * @param ref reference name of the NamedData
     * @return the NamedData with the given reference string
     */

    public NamedData getNamedDataOrNull(String ref)
    {
        if (!this.contains(ref))
        {
            return null;
        }
        return allData.get(ref);
    }


//------------------------------------------------------------------------------

    /**
     * Return the NamedData required
     * @param ref reference name of the NamedData
     * @return the NamedData with the given reference string
     */

    public NamedData getNamedData(String ref)
    {
        if (!this.contains(ref))
        {
            Terminator.withMsgAndStatus("ERROR! Key '" + ref + "' not found in "
                        + "NamedDataCollector!",-1);
        }
        return allData.get(ref);
    }

//------------------------------------------------------------------------------

    /**
     * Return the NamedData required or the given alternative.
     * @param refName the reference name of the NamedData
     * @param defKind the NamedData type to use for the default NamedData
     * @param defValue the fully qualified name of the class from which
     * the default value is to be taken
     * @return the user defined value or the default
     */

    public NamedData getNamedDataOrDefault(String refName, 
    		NamedData.NamedDataType defKind, 
    		Object defValue)
    {
        NamedData p = new NamedData();
        if (this.contains(refName))
        {
             p = allData.get(refName);
        }
        else
        {
        	p = new NamedData(refName, defKind, defValue);
        }
        return p;
    }

//------------------------------------------------------------------------------

    /**
     * Return all the NamedData stored
     * @return the map with all NamedData
     */

    public Map<String,NamedData> getAllNamedData()
    {
        return allData;
    }

//------------------------------------------------------------------------------

    /**
     * Store a NamedData with the given reference name. If the NamedData already
     * exists, it will be overwritten
     * @param data the new NamedData to be stored
     */

    public void putNamedData(NamedData data)
    {
        allData.put(data.getReference(),data); 
    }
    
//------------------------------------------------------------------------------

    /**
     * Store a NamedData with the given reference name. If the NamedData already
     * exists, it will be overwritten
     * @param ref the reference name of the NamedData
     * @param par the new NamedData to be stored
     */

    public void putNamedData(String ref, NamedData par)
    {
        allData.put(ref,par); 
    }

//------------------------------------------------------------------------------

    /**
     * Search for a reference name
     * @param ref the reference name to be searched
     * @return <code>true</code> if this collector contains a NamedData
     * with the given reference name 
     */

    public boolean contains(String ref)
    {
        if (allData.keySet().contains(ref))
            return true;
        else
            return false;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a deep copy of this collection of data
     * @throws CloneNotSupportedException 
     */
    
    public NamedDataCollector clone() throws CloneNotSupportedException
    {
    	NamedDataCollector ndc = new NamedDataCollector();
    	for (Map.Entry<String, NamedData> e : allData.entrySet())
    	{
    		ndc.putNamedData(e.getKey(), e.getValue().clone());
    	}
    	return ndc;
    }

//------------------------------------------------------------------------------

    /**
     * @return the string representation of this NamedData Collector
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[NamedDataCollector [");
        for (String k : allData.keySet())
        {
            sb.append(k).append("=").append(allData.get(k)).append(", ");
        }
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
