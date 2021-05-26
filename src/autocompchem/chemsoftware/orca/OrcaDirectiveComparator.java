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

import autocompchem.chemsoftware.Directive;

/**
 * Comparator meant to sort directives according to the expected order of 
 * the Orca input file.
 * 
 * @author Marco Foscato
 */
public class OrcaDirectiveComparator implements Comparator<Directive> 
{

	@Override
	public int compare(Directive dA, Directive dB) 
	{
		String nA = dA.getName().replace("%", "");
		String nB = dB.getName().replace("%", "");
		
		int iA = 0;
		int iB = 0;
		
		if (nA.startsWith("!"))
			iA = -10;
		if (nB.startsWith("!"))
			iB = -10;
		
		if (nA.startsWith("#"))
			iA = -9;
		if (nB.startsWith("#"))
			iB = -9;
		
		if (nA.startsWith("*"))
			iA = 10;
		if (nB.startsWith("*"))
			iB = 10;
		
		if (nA.startsWith("coords"))
			iA = 10;
		if (nB.startsWith("coords"))
			iB = 10;
		
		return Integer.compare(iA, iB);
	}

}
