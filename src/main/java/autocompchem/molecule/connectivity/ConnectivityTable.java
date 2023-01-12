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

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

/** 
 * This object contains the list of neighbors per each atom in the system.
 * Both 0-based and 1-based connectivity can be generated.
 *          
 * @author Marco Foscato
 */


public class ConnectivityTable
{
    /**
     * List of connected atom IDs per each atom ID. The IDs are zero-based.
     */
    private ArrayList<ArrayList<Integer>> cnTab;

//------------------------------------------------------------------------------

    /**
     * Constructor for empty ConnectivityTable object
     */

    public ConnectivityTable()
    {
        cnTab = new ArrayList<ArrayList<Integer>>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an Connectivity table from the CDK representation of
     * the chemical system.
     * @param mol the chemical system for which the connectivity table is to
     * be generated.
     */

    public ConnectivityTable(IAtomContainer mol)
    {
        cnTab = new ArrayList<ArrayList<Integer>>();
        for (IAtom atm : mol.atoms())
        {
            ArrayList<Integer> nbrs = new ArrayList<Integer>();
            for (IAtom nbr : mol.getConnectedAtomsList(atm))
            {
                nbrs.add(mol.indexOf(nbr));
            }
            cnTab.add(nbrs);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add a neighborning role to the connectivity table
     * @param srcId the index of the src atom
     * @param nbrs the list of atom Ids of the neighbors
     * @param zeroBased set <code>true</code> if the input is 0-based
     */

    public void setNeighborsRelation(int srcId, ArrayList<Integer> nbrs, 
                                                              boolean zeroBased)
    {
        //scale to 0-based IDs
        ArrayList<Integer> locNbrs = new ArrayList<Integer>();
        if (!zeroBased)
        {
            srcId = srcId - 1;
            for (Integer i : nbrs)
            {
                locNbrs.add(i - 1);
            }
        }
        else
        {
            locNbrs.addAll(nbrs);
        }

        // ensure size
        if (cnTab.size() < (srcId+1))
        {
            for (int i=0; i<(srcId - cnTab.size() + 1); i++)
            {
                cnTab.add(new ArrayList<Integer>());
            }
        }

        // add neghbours ids
        cnTab.set(srcId,locNbrs);
    }

//------------------------------------------------------------------------------

    /**
     * Get index of neighbors of an atom. Both 0-based and 1-based
     * output indexes can be produced.
     * @param srcId the index of the central atom (always 0-based)
     * @param zeroBased set to <code>true</code> if 0-based indexes are wanted
     * or <code>false</code> for 1-based. 
     * @return the list of IDs of neighbor atoms
     */

    public ArrayList<Integer> getNbrsId(int srcId, boolean zeroBased)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (int i=0; i<cnTab.get(srcId).size(); i++)
        {
            int base = 1;
            if (zeroBased)
            {
                base = 0;
            }
            ids.add(cnTab.get(srcId).get(i) + base);
        }
        return ids;
    }

//------------------------------------------------------------------------------

    /**
     * Get index of neighbors of an atom in a formatted form. 
     * Both 0-based and 1-based
     * output indexes can be produced.
     * @param srcId the index of the central atom (always 0-based)
     * @param zeroBased set to <code>true</code> if 0-based indexes are wanted
     * or <code>false</code> for 1-based.
     * @param sep the separator to be used between the indexes
     * @return the IDs of the neighbor atoms as a string
     */

    public String getNbrsIdAsString(int srcId, boolean zeroBased, String sep) 
    {
        String form = " %1$" + String.valueOf(cnTab.size()).length() + "s";
        String s = "";
        for (Integer id : getNbrsId(srcId,zeroBased))
        {
            s = s + String.format(form,id) + sep;
        }
        return s;                
    }

//------------------------------------------------------------------------------

}
