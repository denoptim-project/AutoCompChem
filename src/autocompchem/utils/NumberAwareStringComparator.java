package autocompchem.utils;

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

import java.util.Comparator;

/**
 * Comparator for trings that embedd numbers
 * 
 * @author Marco Foscato
 */

public class NumberAwareStringComparator implements Comparator<String>
{
    @Override
    public int compare(String a, String b)
    {
        final int FIRST = 1; //the first String comes after the second
        final int EQUAL = 0;
        final int LAST = -1; //the second String comes after the first

        int res = EQUAL;

        String[] partsA = a.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] partsB = b.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        for (int i=0; i<Math.min(partsA.length,partsB.length); i++)
        {
            String pA = partsA[i];
            String pB = partsB[i];

            if (pA.equals(pB))
            {
                continue;
            }

            boolean isnumA = NumberUtils.isNumber(pA);
            boolean isnumB = NumberUtils.isNumber(pB);

            if (isnumA && isnumB)
            {
                double dA = Double.parseDouble(pA);
                double dB = Double.parseDouble(pB);
                res = Double.compare(dA,dB);
                break;
            }
            else if (isnumA && !isnumB)
            {
                res = FIRST;
                break;
            }
            else if (!isnumA && isnumB)
            {
                res = LAST;
                break;
            }
            else
            {
                res = pA.compareTo(pB);
                break;
            }
        }
        if (res == EQUAL)
        {
            if (partsA.length < partsB.length)
            {
                res = FIRST;
            }
            else if (partsA.length > partsB.length)
            {
                res = LAST;
            }
        }

        return res;
    }

//------------------------------------------------------------------------------

}


