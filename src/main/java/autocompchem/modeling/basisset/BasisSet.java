package autocompchem.modeling.basisset;

import java.util.ArrayList;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;


/**
 * Object representing a basis set as a combination of center-specific basis 
 * sets.
 *
 * @author Marco Foscato
 */

public class BasisSet
{
    /**
     * List center-specific basis sets
     */
    public List<CenterBasisSet> centerBSs = new ArrayList<CenterBasisSet>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty object
     */

    public BasisSet()
    {}

//------------------------------------------------------------------------------

    /**
     * Add a new center-specific basis set
     * @param cbs the center-specific basis set to append
     */

    public void addCenterSpecBSet(CenterBasisSet cbs)
    {
        centerBSs.add(cbs);
    }

//------------------------------------------------------------------------------

    /**
     * Checks if this basis set contains an element-specific basis set.
     * @param elSymbol the elemental symbol (case insensitive).
     * @return <code>true</code> if this basis set contains for the specified
     * elemental symbol.
     */

    public boolean hasElement(String elSymbol)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex()==null 
            		&& cbs.getElement().toUpperCase().equals(
            				elSymbol.toUpperCase()))
            {
                return true;
            }
        }
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if this basis set contains a center with the given reference name
     * @param centerId the reference name of the center (i.e., atom)
     * @param elSymbol the elemental symbol (case insensitive).
     * @return <code>true</code> if this basis set contains a center with the 
     * given reference string.
     */

    public boolean hasCenter(int id, String elSymbol)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex()!=null && cbs.getCenterIndex()==id 
            		&& cbs.getElement().toUpperCase().equals(
            				elSymbol.toUpperCase()))
            {
                return true;
            }
        }
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the basis set for the specified element. If no center-specific
     * basis set is assigned to such element, then it creates a new one within
     * this object.
     * @param elSymbol the elemental symbol to search for.
     * @return the center-specific basis set associated with the given elemental
     * symbol. If none is found a new 
     * {@link autocompchem.modeling.basisset.CenterBasisSet} is created, added to
     * this basis set, and returned.
     */
    public CenterBasisSet getCenterBasisSetForElement(String elSymbol)
    {
    	for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex()==null && cbs.getElement().equals(elSymbol))
            {
                return cbs;
            }
        }
        CenterBasisSet newCbs = new CenterBasisSet(null, null, elSymbol);
        centerBSs.add(newCbs);
        return newCbs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the basis set for the specified center ID. If no center-specific
     * basis set is assigned to such center ID, then it creates a new one within
     * this object.
     * @param tag the tag assigned to the center for which we want to get the 
     * basis set.
     * @param id the index of the center.
     * @param elSymb the elemental symbol of the center.
     * @return the center-specific basis set associated with the given center ID
     * ID. If there is no such center ID in this Basis set, then a new 
     * {@link autocompchem.modeling.basisset.CenterBasisSet} is created, added to
     * this basis set, and returned.
     */

    public CenterBasisSet getCenterBasisSetForCenter(String tag, Integer id, 
    		String elSymb)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
        	if (cbs.getCenterTag()!=null && tag!=null 
        			&& cbs.getCenterTag().equals(tag))
        	{
        		return cbs;
        	} else if (cbs.getElement()!=null && elSymb!=null 
        			&& cbs.getCenterIndex()!=null
        			&& cbs.getElement().equals(elSymb) 
            		&& cbs.getCenterIndex()==id)
            {
                return cbs;
            }
        }
        CenterBasisSet newCbs = new CenterBasisSet(tag, id, elSymb);
        centerBSs.add(newCbs);
        return newCbs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the complete list of <code>CenterBasisSet</code>s
     * @return the list of center-specific basis set
     */

    public List<CenterBasisSet> getAllCenterBSs()
    {
        return centerBSs;
    }

//------------------------------------------------------------------------------

    /** 
     * Checks whether there is any ECP in this basis set
     * @return <code>true</code> if any of the centers has an ECP
     */

    public boolean hasECP()
    {
        boolean res = false;
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getECPShells().size() > 0)
            {
                res = true;
                break;
            }
        }
        return res;
    }
    

//------------------------------------------------------------------------------

    /** 
     * Checks whether there is any center that is defined as an index (i.e.,
     * atom index in the list of atoms).
     * @return <code>true</code> if any of the centers is identified by atom 
     * index. Returned <code>false</code> if al centers are defined by elemental
     * symbol.
     */

    public boolean hasIndexSpecificComponents()
    {
        boolean res = false;
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex() != null)
            {
                res = true;
                break;
            }
        }
        return res;
    }
      
//------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o)
    {
    	if (!(o instanceof BasisSet))
    		return false;
    	BasisSet other = (BasisSet) o;
    	
    	if (this.centerBSs.size()!=other.centerBSs.size())
    		return false;
    	
    	for (int i=0; i<centerBSs.size(); i++)
    	{
    		if (!this.centerBSs.get(i).equals(other.centerBSs.get(i)))
    			return false;
    	}
    	return true;
    }

//------------------------------------------------------------------------------

}
