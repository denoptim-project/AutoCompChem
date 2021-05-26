package autocompchem.molecule;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * The concept of an atom and the number of connected neighbours collected in a
 * single object. SeedAtoms are useful for exploring atom containers following
 * their connectivity.
 * 
 * @author Marco Foscato 
 */

public class SeedAtom
{
    //connection
    private int connections;

    //First atom of the ligand
    private IAtom seed;

//
//          <...part of the ligand...>
//         /
//  ---<seed>---<...part of the ligand...>
//         \ 
//          <...part of the ligand...>
//

//------------------------------------------------------------------------------

    public SeedAtom(IAtom seed, IAtomContainer mol)
    {
        this.seed = seed;
        this.connections = mol.getConnectedBondsCount(seed);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the element symbol
     * @return the symbol
     */

    public String getSymbol()
    {
        String s = seed.getSymbol();
        return s;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the mass number
     */
    public Integer getMassNumber()
    {
    	Integer a = null;

		try {
			if (Isotopes.getInstance().isElement(seed.getSymbol()))
	        {
				IIsotope i = Isotopes.getInstance().getMajorIsotope(seed.getSymbol());
	            a = i.getMassNumber();
	        }
		} catch (Throwable e) {
			// nothing really
		}
		
    	return a;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the seed atom
     * @return the seed atom
     */

    public IAtom getAtom()
    {
        return seed;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the number of connected atoms
     * @return the connection number
     */

    public int getConnectedBondsCount()
    {
        return connections;
    }

//------------------------------------------------------------------------------
}
