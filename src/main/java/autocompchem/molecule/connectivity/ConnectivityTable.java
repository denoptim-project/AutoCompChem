package autocompchem.molecule.connectivity;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.common.collect.Lists;

/** 
 * Map of neighbors defining which item is connected to with other items. 
 * Items are identified by 0-based indexes. 
 *          
 * @author Marco Foscato
 */


//TODO-gg rename neighboring map

public class ConnectivityTable extends HashMap<Integer, List<Integer>>
{

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty table.
     */

    public ConnectivityTable()
    {}
    
//------------------------------------------------------------------------------

    /**
     * Constructor a table to the given atom container. Considers all atoms
     * in the container.
     * @param mol the chemical system for which the table is to
     * be generated.
     */

    public ConnectivityTable(IAtomContainer mol)
    {
    	this(Lists.newArrayList(mol.atoms()), mol);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor a table to the given set of atoms. Note that the 
     * Neighboring relations will only consider atoms in the subset.
     * @param subset the set of atoms to include in the table.
     * @param mol the chemical system for which the table is to
     * be generated.
     */

    public ConnectivityTable(Collection<IAtom> subset, IAtomContainer mol)
    {
        for (IAtom atm : subset)
        {
            List<Integer> nbrs = new ArrayList<Integer>();
            for (IAtom nbr : mol.getConnectedAtomsList(atm))
            {
            	if (subset.contains(nbr))
            		nbrs.add(mol.indexOf(nbr));
            }
            put(mol.indexOf(atm), nbrs);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add a neighboring relation in this table. Does not overwrite existing
     * relations. This method assumes the indexes are all 0-based.
     * @param srcId the index of the item from witch we are setting the 
     * neighbors.
     * @param nbrs the list of item Ids of the neighbors. The list will be 
     * copied, not used as it is.
     */

    public void addNeighborningRelation(int srcId, List<Integer> nbrs)
    {
    	addNeighborningRelation(srcId, nbrs, true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a neighboring relation in this table. Does not overwrite existing
     * relations.
     * @param srcId the index of the item from witch we are setting the 
     * neighbors.
     * @param nbrs the list of item Ids of the neighbors. The list will be 
     * copied, not used as it is.
     * @param zeroBased set <code>true</code> if the given indexes are 0-based.
     */

    public void addNeighborningRelation(int srcId, List<Integer> nbrs, 
    		boolean zeroBased)
    {
    	int offset = 0;
    	if (!zeroBased)
    		offset = -1;
    	
    	int zeroBasedIdSrc = srcId + offset;

		List<Integer> zeroBasedNbrs = new ArrayList<Integer>();
		for (Integer id : nbrs)
			zeroBasedNbrs.add(id+offset);
		
    	if (this.containsKey(zeroBasedIdSrc))
    	{
    		this.get(zeroBasedIdSrc).addAll(zeroBasedNbrs);
    	} else {
    		this.put(zeroBasedIdSrc, zeroBasedNbrs);
    	}
    	for (Integer nbr : nbrs)
    	{
    		int zeroBasedNbr = nbr+offset;
    		if (this.containsKey(zeroBasedNbr))
        	{
        		this.get(zeroBasedNbr).add(zeroBasedIdSrc);
        	} else {
        		this.put(zeroBasedNbr, new ArrayList<Integer>(
        				Arrays.asList(zeroBasedIdSrc)));
        	}
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Get neighboring relations for a given item.
     * @param srcId the (0-based) index of the item from which we want to get  
     * the indexes of the neighbors.
     * @return the list of IDs of neighbor atoms (0-based).
     */

    public List<Integer> getNbrsId(int srcId)
    {
    	return get(srcId);
    }
    
//------------------------------------------------------------------------------

    /**
     * Get a copy of the neighboring relations for a given item.
     * @param srcId the index of the central atom (always 0-based)
     * @param zeroBased set to <code>true</code> if 0-based indexes are wanted
     * or <code>false</code> for 1-based. 
     * @return a copy of the list of IDs of neighbors to the given item. Note,
     * this is a copy is not the data structure where those indexes are
     * stored.
     */

    public List<Integer> getNbrsId(int srcId, boolean zeroBased)
    {
    	int offset = 0;
    	if (!zeroBased)
    		offset = 1;
    	
        List<Integer> ids = new ArrayList<Integer>();
        
        if (!containsKey(srcId))
        	return ids;
        
        for (int i=0; i<get(srcId).size(); i++)
        {
            ids.add(get(srcId).get(i) + offset);
        }
        return ids;
    }
    
//------------------------------------------------------------------------------

    /*
  	@Override
  	public boolean equals(Object o)
  	{
      	if (o== null)
      		return false;
      	
   	    if (o == this)
   		    return true;
   	   
   	    if (o.getClass() != getClass())
       		return false;
   	    
  		Constraint other = (Constraint) o;
  	}
  	*/

//------------------------------------------------------------------------------

}
