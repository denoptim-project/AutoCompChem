package autocompchem.molecule.sorting;

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

/**
 * Comparator for SortableMolecules 
 *
 * @author Marco Foscato 
 */

public class SortableMoleculeComparator implements Comparator<SortableMolecule>
{

    @Override
    public int compare(SortableMolecule a, SortableMolecule b)
    {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

        int res = EQUAL;

        Object ca = a.getValue();
        Object cb = b.getValue();

        Double da = null;
        Double db = null;
        Integer ia = null;
        Integer ib = null;
        String sa = null;
        String sb = null;
        if (ca instanceof String && cb instanceof String)
        {
            sa = (String) ca;
            sb = (String) cb;
            res = sa.compareTo(sb);
        } else {
            if (ca instanceof Double && cb instanceof Double) 
            {
                da = (Double) ca;
                db = (Double) cb;
                res = da.compareTo(db);
            } else if (ca instanceof Integer && cb instanceof Integer)
            {
                ia = (Integer) ca;
                ib = (Integer) cb;
                res = ia.compareTo(ib);
            }
        }

        return res;
    }
}

