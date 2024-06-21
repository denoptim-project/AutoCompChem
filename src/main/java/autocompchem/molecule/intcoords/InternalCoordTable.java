package autocompchem.molecule.intcoords;

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

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.run.Terminator;


/**
 * A collection of internal coordinates.
 *
 * @author Marco Foscato
 */ 

public class InternalCoordTable
{
    /**
     * List of internal coordinate
     */
    protected ArrayList<InternalCoord> intCrds = new ArrayList<InternalCoord>();


//------------------------------------------------------------------------------

    /**
     * Construct an empty InternalCoordTable
     */

    public InternalCoordTable()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Construct an InternalCoordTable from a molecular representation
     * @param mol the chemical system 
     * @param verbosity the verbosity level
     */

    public InternalCoordTable(IAtomContainer mol, int verbosity)
    {
        Terminator.withMsgAndStatus("ERROR! Attempt to use InternalCoordTable "
                + "which is not implemented yet...", -1);
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates if a list of atom indeces corresponds to any of the indeces
     * used to define an internal coordinate contained in this table.
     * @param otherIds the list of indexes to search for
     * @return <code>true</code> if the given list of indexes corresponds is
     * found in the table of coordinate.
     */

    public boolean containsNTuplaOfIDs(ArrayList<Integer> otherIds)
    {
        boolean res = false;
        for (InternalCoord ic : intCrds)
        {
            if (ic.compareIDs(otherIds))
            {
                res = true;
                break;
            }
        }
        return res;
    }

//------------------------------------------------------------------------------

}
