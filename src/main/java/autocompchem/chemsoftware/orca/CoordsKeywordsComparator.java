package autocompchem.chemsoftware.orca;

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

import java.util.Comparator;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.Keyword;

/**
 * Sorts keywords consistently with the expectations of the %coords input
 * block: first type of coordinates, then charge, then spin multiplicity.
 */
class CoordsKeywordsComparator implements Comparator<Keyword>
{
    @Override
    public int compare(Keyword a, Keyword b)
    {           
        String aName = a.getName();
        String bName = b.getName();
        int intA = 0;
        int intB = 0;
        
        if (aName.toUpperCase().equals(ChemSoftConstants.PARCOORDTYPE))
        {
        	intA = -3;
        }
        if (bName.toUpperCase().equals(ChemSoftConstants.PARCOORDTYPE))
        {
        	intB = -3;
        }
        
        if (aName.toUpperCase().equals(
        		OrcaConstants.COORDSCTYPDIRNAME.toUpperCase()))
        {
        	intA = -3;
        }
        if (bName.toUpperCase().equals(
        		OrcaConstants.COORDSCTYPDIRNAME.toUpperCase()))
        {
        	intB = -3;
        }
        
        if (aName.toUpperCase().equals(ChemSoftConstants.PARCHARGE))
        {
        	intA = -2;
        }
        if (bName.toUpperCase().equals(ChemSoftConstants.PARCHARGE))
        {
        	intB = -2;
        }
        
        if (aName.toUpperCase().equals(ChemSoftConstants.PARSPINMULT))
        {
        	intA = -1;
        }
        if (bName.toUpperCase().equals(ChemSoftConstants.PARSPINMULT))
        {
        	intB = -1;
        }
        
        //add any other priority rules go here... 
        //but now there seems to be no more.
        
        return Integer.compare(intA,intB);
    }
}
