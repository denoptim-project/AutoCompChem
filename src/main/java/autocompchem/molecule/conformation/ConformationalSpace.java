package autocompchem.molecule.conformation;

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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The conformational space as the combination of a list of conformational
 * changes (i.e., the conformational coordinates).
 * 
 * @author Marco Foscato 
 */

public class ConformationalSpace
{
    /**
     * Unique counter for coordinates names
     */
    private final AtomicInteger CRDID = new AtomicInteger(0);

    /**
     * The list of conformational coordinates
     */
    private ArrayList<ConformationalCoordinate> coords;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ConformationalSpace
     */

    public ConformationalSpace()
    {
        this.coords = new ArrayList<ConformationalCoordinate>();
    }

//------------------------------------------------------------------------------

    /**
     * Get unique name for coord
     * @return a string unique within this ConformationalSpace
     */

    public String getUnqCoordName()
    {
        return ConformationalConstants.COORDNAMEROOT + CRDID.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Get the list of coordinates
     * @return the list of conformational coordinates
     */

    public ArrayList<ConformationalCoordinate> coords()
    {
        return coords;
    }

//------------------------------------------------------------------------------

    /**
     * Add a conformational coordinated to this confromational space
     * @param coord the coordinate to add
     */

    public void addCoord(ConformationalCoordinate coord)
    {
        coords.add(coord);
    }

//------------------------------------------------------------------------------

    /**
     * Check if the conformational space already contains a given conformational
     * coord.
     * @param queryCoord the given conformational coordinate to look for
     * @return <code>true</code> if the move is already contined
     */

    public boolean contains(ConformationalCoordinate queryCoord)
    {
        return coords.contains(queryCoord);
    }

//------------------------------------------------------------------------------

    /**
     * Get number of combinations. The number of combinations represent the
     * size of this ConformationalSpace.
     * @return the number of combinations, or the largest calculated number 
     * multiplied by -1 if the actual result is to big 
     * to be calculated.
     */

    public int getSize()
    {
        int sz = 1;
        for (ConformationalCoordinate cc : coords)
        {
            sz = sz * cc.getFold();
            int diff = Integer.MAX_VALUE - sz;
            if (diff < 10000)
            {
                sz = -sz;
                break;
            }
        }
        return sz;
    }

//------------------------------------------------------------------------------

    /**
     * Get a string representation of this conformational space
     * @return a string representation of this confomational space
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[ConformationalSpace coords=[");
        boolean fist = true;
        for (int i=0; i<coords.size(); i++)
        {
            if (i>0)
            {
                sb.append(", ");
            }
            sb.append(coords.get(i).toString());
        }
        sb.append("]");
        sb.append("] ");
        return sb.toString();
    }

//------------------------------------------------------------------------------
}
