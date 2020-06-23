package autocompchem.files;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.Comparator;

/**
 * Compare filenames of files. Overwrites default comparator.
 * 
 * @author Marco Foscato
 */


public class FilesComparatorByName implements Comparator<File>
{

    //Flag for ascending/descending ordering
    private boolean ascending;

//-----------------------------------------------------------------------------

    /**
     * Constructs a new FileComparatorByName
     * @param ascending set <code>true</code> to sort ascending 
     */

    public FilesComparatorByName(boolean ascending)
    {
        this.ascending = ascending;
    }

//-----------------------------------------------------------------------------

    @Override
    public int compare(File a, File b)
    {
        if (ascending)
        {
            return ((a.getName()).compareTo(b.getName()));
        } else {
            return -1 * ((a.getName()).compareTo(b.getName()));
        }
    }

//-----------------------------------------------------------------------------

}
