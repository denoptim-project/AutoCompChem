package autocompchem.chemsoftware.qmmm;

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
 * Comparator for {@link QMMMSection}s. This comparator sorts the sections
 * according to alphabetic order with the exception that it places the 
 * the {@value autocompchem.chemsoftware.qmmm.QMMMConstants#MULTIGENSEC} and 
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#MULTIOPTSEC}
 * in first and second position.
 */

public class QMMMSectionComparator implements Comparator<QMMMSection>
{
    @Override
    public int compare(QMMMSection a, QMMMSection b)
    {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

        int res = EQUAL;

        String nameA = a.getName();
        String nameB = b.getName();

        if (nameA.equals(QMMMConstants.MULTIGENSEC))
        {
            res = LAST;
        }
        else if (nameB.equals(QMMMConstants.MULTIGENSEC))
        {
            res = FIRST;
        }
        else
        {
            if (nameA.equals(QMMMConstants.MULTIOPTSEC))
            {
                res = LAST;
            }
            else if (nameB.equals(QMMMConstants.MULTIOPTSEC))
            {
                res = FIRST;
            }
        }
        return res;
    }
}
