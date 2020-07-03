package autocompchem.datacollections;

import java.util.HashMap;
import java.util.Map;

import autocompchem.run.Terminator;

/**
 * Storage of {@link NamedData} objects that are collected by reference.
 *
 * @author Marco Foscato
 */

public class NamedDataCollector 
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
     * @param ref the reference name of the NamedData
     * @param par the new NamedData to be stores
     */

    public void setNamedData(String ref, NamedData par)
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
