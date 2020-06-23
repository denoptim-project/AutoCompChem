package autocompchem.molecule.atomclashes;

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

/**
 * VDWAllowance represents the concept of allowance in the comparison of 
 * interatomic distances with sum of van der Waals radii.
 * 
 * @author Marco Foscato 
 */

public class VDWAllowance
{
    //SMARTS defining the identity ofatoms involved
    private String smarts1;
    private String smarts2;

    //Allowance in amstrong
    private double allowance;


//------------------------------------------------------------------------------

    /**
     * Contructs a new VDWAllowance specifying the type of atoms
     * involved and the allowance in angstrom.
     * @param smarts1 the SMARTS defining the first atom types.
     * @param smarts2 the SMARTS defining the second atom types.
     * @param allowance the allowance in angstrom.
     */

    public VDWAllowance(String smarts1, String smarts2, Double allowance)
    {
        this.smarts1 = smarts1;
        this.smarts2 = smarts2;
        this.allowance = allowance;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string array with the two SMARTS defining the types of atoms. 
     * @return the array of SMARTS
     */

    public String[] getSmarts()
    {
        String[] smarts = new String[2];
        smarts[0] = smarts1;
        smarts[1] = smarts2;
        return smarts;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the allowance in angstrom.
     * @return the allowance
     */

    public double getAllowance()
    {
        return allowance;
    }

//------------------------------------------------------------------------------

    /**
     * Return the string representation
     * @return the string representation
     */
 
    public String toString()
    {
        String s = "VDWAllowance_" + smarts1 + ":" + smarts2 + "_" + allowance;
        return s;
    }

//------------------------------------------------------------------------------
}
