package autocompchem.perception;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import autocompchem.perception.infochannel.InfoChannelType;

/**
 * Comparator for InfoChannelTypes.
 * 
 * @author Marco Foscato
 */

//------------------------------------------------------------------------------

public class InfoChannelTypeComparator implements Comparator<InfoChannelType>
{
    public boolean checkCompatibility(InfoChannelType a, InfoChannelType b)
    {
	boolean res = a.equals(b);
	if (a.equals(InfoChannelType.ANY) ||
	    b.equals(InfoChannelType.ANY))
	{
	    res = true;
	}

	return res;
    }

//------------------------------------------------------------------------------

    @Override
    public int compare(InfoChannelType a, InfoChannelType b)
    {
	return a.toString().compareTo(b.toString());
    }

//------------------------------------------------------------------------------

}
