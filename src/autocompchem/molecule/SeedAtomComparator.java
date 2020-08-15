package autocompchem.molecule;

/*   
 *   Copyright (C) 2014  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Comparator;

import autocompchem.atom.AtomUtils;

/**
 * Comparator for Seed atoms. Uses atomic mass number and number of connected
 * neighbours. Atoms with low number of connections (NC) come first than those 
 * larg NC. In case of same NC, the atom with highest mass number comes first.
 * Dummy atoms come first.
 * 
 * @author Marco Foscato 
 */

public class SeedAtomComparator implements Comparator<SeedAtom>
{

    @Override
    public int compare(SeedAtom a, SeedAtom b)
    {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

        int res = EQUAL;

        // Connection numbers
        int cnA = a.getConnectedBondsCount();
        int cnB = b.getConnectedBondsCount();

        // Masses
        int massA;
        int massB;

        // Assign special values to special atoms
        if (AtomUtils.isAccDummy(a.getAtom()))
        {
            massA = 1;
            if (cnA == 1)
            {
                cnA = -100;
            }
        }
        else if (AtomUtils.isAttachmentPoint(a.getAtom()))
        {
            massA = 1;
        }
        else
        {
            massA = a.getMassNumber();
        }

        if (AtomUtils.isAccDummy(b.getAtom()))
        {
            massB = 1;
            if (cnB == 1)
            {
                cnB = -100;
            }
        }
        else if (AtomUtils.isAttachmentPoint(b.getAtom()))
        {
            massB = 1;
        }
        else
        {
            massB = b.getMassNumber();
        }

        // Compare
        if (cnA == cnB)
        {
            if (massA == massB)
            {
                res = EQUAL;
            }
            else
            {
                if (massA < massB)
                {
                    res = FIRST;
                } 
                else 
                {
                    res = LAST;
                }
            }
        } 
        else if (cnA > cnB) 
        {
            res = FIRST;
        } 
        else if (cnA < cnB) 
        {
            res = LAST;
        } 

        return res;
    }

//-----------------------------------------------------------------------------

}
