package autocompchem.datacollections;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;

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
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.AtomContainerSet;


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
        		throw new IllegalArgumentException("Key '" + ref 
        				+ "' not found in " + this.getClass().getSimpleName());
        	}
        }
        return allData.get(ref);
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
     * Store a {@link NamedData} with the given reference name. If the key
     * already exists, the corresponding value will be overwritten.
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
     * @return a shallow copy of this collection of data
     */
    
    public NamedDataCollector copy()
    {
    	NamedDataCollector ndc = new NamedDataCollector();
    	for (NamedData e : allData.values())
    	{
    		ndc.putNamedData(e);
    	}
    	return ndc;
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
 	    
 	    if (this.allData.size() != other.allData.size())
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

    /**
     * Extracts the value of any data stored inside this {@link NamedDataCollector}.
     * @param pathIntoExposedData the array of names/integers that allow navigating 
     * the data structure. Names (or integers reported as strings)
     * are interpreted as keys for data containers that are
     * {@link NamedData}, {@link NamedDataCollector}, or {@link Map}s. 
     * Strings representations of integers are needed for 
     * {@link List}s and {@link Map}s with integers as keys. 
     * Special keys like <code>LAST</code> or <code>FIRST</code> are supported for 
     * {@link List}s and {@link Map}s where the ordering of the keys is
     * defined by the {@link NumberAwareStringComparator}.
     * @return the content of the requested data, whcih may be null if the data 
     * stored in this collector is null.
     * @throws DataFetchingException if the data fetching request cannot be satisfied.
     */
    public Object getNestedDataValue(String[] pathIntoExposedData) 
       throws DataFetchingException
    {	
        Object value = this;
    	for (int i=0; i<pathIntoExposedData.length; i++)
    	{
        	String nestedContentID = pathIntoExposedData[i].stripLeading().stripTrailing();
        	Object nestedValue = null;
        	if (value instanceof NamedData)
        	{
        		NamedData container = (NamedData) value;
        		if (!container.getReference().equals(nestedContentID))
        			return null;
        		nestedValue = container.getValue();
        	} else if (value instanceof NamedDataCollector)
	    	{
	    		NamedDataCollector container = (NamedDataCollector) value;
                Set<String> keys = container.getAllNamedData().keySet();
                List<String> sortedKeys = new ArrayList<String>(keys);
                NumberAwareStringComparator comparator = new NumberAwareStringComparator();
                Collections.sort(sortedKeys, comparator);
                if ("LAST".equals(nestedContentID.toUpperCase())) {
                    nestedValue = container.getNamedData(sortedKeys.get(sortedKeys.size() - 1)).getValue();
                } else if ("FIRST".equals(nestedContentID.toUpperCase())) {
                    nestedValue = container.getNamedData(sortedKeys.get(0)).getValue();
                } else {
                    if (!container.contains(nestedContentID))
                    {
                        String pathUpToHere = "";
                        for (int j = 0; j < i; j++) {
                            pathUpToHere += pathIntoExposedData[j] + ",";
                        }
                        throw new DataFetchingException("The data '" 
                            + pathUpToHere + nestedContentID 
                            + "' is not available in this " 
                            + this.getClass().getSimpleName() 
                            + ". Available references: " 
                            + container.getAllNamedData().keySet().toString() + ".");
                    }
                    nestedValue = container.getNamedData(nestedContentID).getValue();
                }
	    	} else if (value instanceof Map) 
	    	{
	    		Map<?,?> map = (Map<?, ?>) value;
	    		nestedValue = null;
	    		for (Object key : map.keySet()) 
	    		{
	    		    if (key instanceof String) 
	    		    {
	    		        String strKey = (String) key;
	    		        if (strKey.equals(nestedContentID)) 
	    		        {
	    		        	nestedValue = map.get(key);
	    		        	break;
	    		        }
	    		    } else if (key instanceof Integer 
	    		    		&& NumberUtils.isParsableToInt(nestedContentID)) 
	    		    {
	    		        Integer intKey = (Integer) key;
	    		        if (intKey == Integer.parseInt(nestedContentID)) 
	    		        {
	    		        	nestedValue = map.get(key);
	    		        	break;
	    		        }
	    		    }
	    		}
	    	} else if (value instanceof List) 
	    	{
                List<?> list = (List<?>) value;
                if (NumberUtils.isParsableToInt(nestedContentID))
	    		{
	    		    nestedValue = list.get(Integer.parseInt(nestedContentID));
                } else {
                    if ("LAST".equals(nestedContentID.toUpperCase())) {
                        nestedValue = list.get(list.size() - 1);
                    } else if ("FIRST".equals(nestedContentID.toUpperCase())) {
                        nestedValue = list.get(0);
                    } else {
                        throw new DataFetchingException(
                            "Reference name '" + nestedContentID 
                            + "' is not valid for a list. "
                            + "Use index or special keys LAST and FIRST.");
                    }
                }
            } else if (value instanceof AtomContainerSet) 
            {
                AtomContainerSet acs = (AtomContainerSet) value;
                if (NumberUtils.isParsableToInt(nestedContentID))
                {
                    nestedValue = acs.getAtomContainer(Integer.parseInt(nestedContentID));
                } else {
                    if ("LAST".equals(nestedContentID.toUpperCase())) {
                        nestedValue = acs.getAtomContainer(acs.getAtomContainerCount() - 1);
                    } else if ("FIRST".equals(nestedContentID.toUpperCase())) {
                        nestedValue = acs.getAtomContainer(0);
                    } else {
                        throw new DataFetchingException(
                            "Reference name '" + nestedContentID 
                            + "' is not valid for an atom container set. "
                            + "Use index or special keys LAST and FIRST.");
                    }
                }
	    	} else if (value == null) {
                throw new DataFetchingException(
                    "Null data in the data fetching request '"
                    + StringUtils.mergeListToString(Arrays.asList(pathIntoExposedData),
                            ".", true) + "' does not allow further navigation.");
            } else {
                throw new DataFetchingException(
                    "Data type '" + value.getClass().getSimpleName() 
                    + "' is not valid for data fetching request '"
                    + StringUtils.mergeListToString(Arrays.asList(pathIntoExposedData),
                            ".", true) + "'.");
	    	}
        	value = nestedValue;
    	}
        return value;
    }

//------------------------------------------------------------------------------

	/**
	 * JSON form is the same as {@link ParameterStorage}: a JSON array of
	 * {@link NamedData} entries (not a keyed object), so it stays compatible
	 * with existing Gson usage for parameters.
	 */
	public static class NamedDataCollectorSerializer
			implements JsonSerializer<NamedDataCollector>
	{
		@Override
		public JsonElement serialize(NamedDataCollector src, Type typeOfSrc,
				JsonSerializationContext context)
		{
			Collection<NamedData> list =
					new ArrayList<NamedData>(src.getAllNamedData().values());
			return context.serialize(list);
		}
	}

//------------------------------------------------------------------------------

	public static class NamedDataCollectorDeserializer
			implements JsonDeserializer<NamedDataCollector>
	{
		@Override
		public NamedDataCollector deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException
		{
			List<NamedData> list = context.deserialize(json,
					new TypeToken<List<NamedData>>()
					{
					}.getType());
			NamedDataCollector ndc = new NamedDataCollector();
			for (NamedData nd : list)
			{
				ndc.putNamedData(nd);
			}
			return ndc;
		}
	}

//------------------------------------------------------------------------------

}
