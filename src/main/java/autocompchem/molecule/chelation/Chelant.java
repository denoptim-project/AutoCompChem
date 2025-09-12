package autocompchem.molecule.chelation;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Chelant represents a multidentate ligand.
 * 
 * @author Marco Foscato 
 */

public class Chelant
{
    //Name or reference
    private String name;

    //Molecular representation
    private IAtomContainer ligand;
    
    //Denticity
    private int denticity;

//------------------------------------------------------------------------------

    public Chelant(String name, IAtomContainer ligand, int denticity)
    {
        this.name = name;
        this.ligand = ligand;
        this.denticity = denticity;
    }

//------------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    public int getDenticity()
    {
        return denticity;
    }

//------------------------------------------------------------------------------

    public String toString()
    {
        String s = "Chelant [name: " + name + "; denticity:" + denticity + "] ";
        return s;
    }

//------------------------------------------------------------------------------

	public IAtomContainer getLigand() 
	{
		return ligand;
	}

//------------------------------------------------------------------------------
}
