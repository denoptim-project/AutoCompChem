package autocompchem.chemsoftware.nwchem;

/*
 *   Copyright (C) 2016  Marco Foscato
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

/**
 * Comparator for {@link NWChemDirective}s. This comparator sorts the directives
 * according to alphabetic order with the exception that it places the TASK
 * directive at the end and the start-up at the beginning.
 */

public class NWChemDirectiveComparator implements Comparator<NWChemDirective>
{
    @Override
    public int compare(NWChemDirective a, NWChemDirective b)
    {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

        int res = EQUAL;

        String nameA = a.getName().toUpperCase();
        String nameB = b.getName().toUpperCase();

        if (nameA.equals(NWChemConstants.TASKDIR))
        {
            res = FIRST;
        }
        else if (nameB.equals(NWChemConstants.TASKDIR))
        {
            res = LAST;
        }
        else
        {
            boolean isStartupA = NWChemConstants.STARTUPTASKS.contains(nameA);
            boolean isStartupB = NWChemConstants.STARTUPTASKS.contains(nameB);

            if (isStartupA && !isStartupB)
            {
                res = LAST;
            }
            else if (!isStartupA && isStartupB)
            {
                res = FIRST;
            }
            else if (isStartupA && isStartupB)
            {
                if (nameA.contains("START"))
                {
                    res = LAST;
                }
                if (nameB.contains("START"))
                {
                    res = FIRST;
                }
            }
            else
            {
                res = nameA.compareTo(nameB);
            }
        }
        return res;
    }
}
