package autocompchem.wiro.chem.nwchem;

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

import autocompchem.wiro.chem.Keyword;

/**
 * Comparator for {@link Keyword}s meant for NWChem.
 * This comparator is used to sort the keywords
 * according to alphabetic order considering only the name of the keyword.
 * Exceptions to the alphabetic order are encoded according to NWChem users 
 * <a href="http://www.nwchem-sw.org/index.php/Release66:Top-level#TASK">
 * NWChem user manual</a>. Currently the only exception is the order of the keywords in
 * the directive TASK: first the 'theory', then the 'operation' type.
 */

public class NWChemKeywordComparator implements Comparator<Keyword>
{
    @Override
    public int compare(Keyword a, Keyword b)
    {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

        int res = EQUAL;

        String nameA = a.getName().toUpperCase();
        String nameB = b.getName().toUpperCase();

        if (nameA.equals(NWChemConstants.THEORYKW.toUpperCase()))
        {
            res = LAST;
        }
        else if (nameB.equals(NWChemConstants.THEORYKW.toUpperCase()))
        {
            res = FIRST;
        }
        else
        {
            if (nameA.equals(NWChemConstants.OPERATIONKW.toUpperCase()))
            {
                res = LAST;
            }
            else if (nameB.equals(NWChemConstants.OPERATIONKW.toUpperCase()))
            {
                res = FIRST;
            }
            else
            {
            	if (nameA.equals(NWChemConstants.GEOMNAMEKW.toUpperCase()))
                {
                    res = LAST;
                }
                else if (nameB.equals(NWChemConstants.GEOMNAMEKW.toUpperCase()))
                {
                    res = FIRST;
                }
                else
                {
                	res = nameA.compareTo(nameB);
                }
            }
        }
        return res;
    }
}
