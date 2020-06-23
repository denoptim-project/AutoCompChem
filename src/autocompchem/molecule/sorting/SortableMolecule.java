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

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * SortableMolecule represents a property that can be used to sort 
 * chemical objects
 * 
 * @author Marco Foscato 
 */

public class SortableMolecule
{
    //Value
    private Object value;

    //Molecular object
    private IAtomContainer mol;


//------------------------------------------------------------------------------

    public SortableMolecule(IAtomContainer mol, Object value)
    {
        //Store molecule
        this.mol = mol;

        //Evaluate value
        String str = value.toString();
        if (str.matches("^[a-zA-Z]+$"))
        {
            this.value = str;
        } else if (str.matches("^[0-9.]+$"))
        {
            if (str.matches("^[0-9]*\\.?[0-9]*$"))
            {
                this.value = Double.valueOf(str);
            } else {
                this.value = Integer.valueOf(str);
            }
        } else {
            this.value = value;
        } 
    }

//------------------------------------------------------------------------------

    public IAtomContainer getIAtomContainer()
    {
        return this.mol;
    }

//------------------------------------------------------------------------------

    public Object getValue()
    {
        return this.value;
    }

//------------------------------------------------------------------------------

    public String toString()
    {
        String s = "SortableMolecule[ " + mol.toString() + ", " + value + "]";
        return s;
    }

//------------------------------------------------------------------------------
}
