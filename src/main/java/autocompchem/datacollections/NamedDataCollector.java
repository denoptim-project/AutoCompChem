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
import java.util.Objects;

import autocompchem.run.Terminator;

/**
 * Storage of {@link NamedData} objects that are collected by reference.
 *
 * @author Marco Foscato
 */

public class NamedDataCollector implements Cloneable
{

    /**
     * The data structure collecting the {@link NamedData} by their reference name 
     * (i.e., key)
     */
	
    protected Map<String, NamedData> allData;

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
     * Constructor from a filled map of {@link NamedData}
     * @param allData the map of {@link NamedData}
     */
    
    public NamedDataCollector(Map<String,NamedData> allData)
    {
        this.allData = allData;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>false</code> if this NamedDataCollector contains {@link NamedData}
     */

    public boolean isEmpty()
    {
        return allData.isEmpty();
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the number of {@link NamedData} in this collector.
     */

    public int size()
    {
        return allData.size();
    }

//------------------------------------------------------------------------------

    /**
     * Return the {@link NamedData} required or null
     * @param ref reference name of the {@link NamedData}
     * @return the {@link NamedData} with the given reference string
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
     * Return the {@link NamedData} required. Triggers fatal error if no data is
     * found with that name.
     * @param ref reference name of the {@link NamedData}.
     * @return the {@link NamedData} with the given reference string.
     */

    public NamedData getNamedData(String ref)
    {
    	return getNamedData(ref, false);
    }

//------------------------------------------------------------------------------

    /**
     * Return the {@link NamedData} required
     * @param ref reference name of the {@link NamedData}
     * @param tolerant use <code>true</code> to allow returning null if no data 
     * with the given name is found.
     * @return the {@link NamedData} with the given reference string
     */

    public NamedData getNamedData(String ref, boolean tolerant)
    {
        if (!this.contains(ref))
        {
        	if (tolerant)
        	{
        		return null;
        	} else {
        		Exception e = new Exception("Key not found");
        		e.printStackTrace();
        		Terminator.withMsgAndStatus("ERROR! Key '" + ref 
        				+ "' not found in " + this.getClass().getSimpleName(),
        				-1);
        	}
        }
        return allData.get(ref);
    }

//------------------------------------------------------------------------------

    /**
     * Return the {@link NamedData} required or the given alternative.
     * @param refName the reference name of the {@link NamedData}
     * @param defKind the {@link NamedData} type to use for the default {@link NamedData}
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
     * Return all the {@link NamedData} stored
     * @return the map with all {@link NamedData}
     */

    public Map<String,NamedData> getAllNamedData()
    {
        return allData;
    }

//------------------------------------------------------------------------------

    /**
     * Store a {@link NamedData} with the given reference name. If the {@link NamedData} already
     * exists, it will be overwritten.
     * @param data the new {@link NamedData} to be stored
     */

    public void putNamedData(NamedData data)
    {
        allData.put(data.getReference(),data); 
    }
    
//------------------------------------------------------------------------------

    /**
     * Store a {@link NamedData} with the given reference name. If the {@link NamedData} already
     * exists, it will be overwritten.
     * @param ref the reference name of the {@link NamedData}
     * @param par the new {@link NamedData} to be stored
     */

    public void putNamedData(String ref, NamedData par)
    {
        allData.put(ref,par); 
    }

//------------------------------------------------------------------------------

    /**
     * Search for a reference name
     * @param ref the reference name to be searched
     * @return <code>true</code> if this collector contains a {@link NamedData}
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
    
    @Override
    public boolean equals(Object o) 
    {
    	if (o == null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
    		return false;
 	   
 	    NamedDataCollector other = (NamedDataCollector) o;
 	    
 	    if (this.allData.size()!=other.allData.size())
 	    	return false;
 	  
 	    for (String nameOfData : this.allData.keySet())
 	    {
 		    NamedData oND = other.getNamedData(nameOfData, true);
 		    if (oND == null)
 		    	return false;
 		    
 		    NamedData tND = this.getNamedData(nameOfData, true);
 		    if (!tND.equals(oND))
 		    	return false;
 	    }
 	    return true;
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(allData);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string representation of this collector
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

    /**
     * Removes the data with the given reference name, if present.
     * @param ref
     */
  	public void removeData(String ref) {
  		allData.remove(ref);
  	}

//------------------------------------------------------------------------------

    /**
     * Removes all the data from this collector. This collector will be empty
     * after calling this method.
     */
  	public void clear() 
  	{
  		allData.clear();
  	}

//------------------------------------------------------------------------------

}
