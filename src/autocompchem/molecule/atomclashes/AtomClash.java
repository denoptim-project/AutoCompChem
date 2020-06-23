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

import org.openscience.cdk.interfaces.IAtom;

/**
 * AtomClash represents the overlap of two atoms.
 * 
 * @author Marco Foscato 
 */

public class AtomClash
{
    //Atoms involved
        private IAtom atm1;
    private String ref1;
    private IAtom atm2;
    private String ref2;

    //Interatomic distance
    private double d;

    //VWD radii sum
    private double vdwsum;

    //Allowance
    private double allowance;

    //Overlap
    private double overlap;


//------------------------------------------------------------------------------

    public AtomClash(IAtom atm1, String ref1, IAtom atm2, String ref2, 
                     double d, double vdwsum, double allowance)
    {
        this.atm1 = atm1;
        this.ref1 = ref1;
        this.atm2 = atm2;
        this.ref2 = ref2;
        this.d = d; 
        this.vdwsum = vdwsum;
        this.allowance = allowance;
        this.overlap = vdwsum - d - allowance;
    }

//------------------------------------------------------------------------------

    public double getOverlap()
    {
        return overlap;
    }

//------------------------------------------------------------------------------

    public String toString()
    {
        String s = "AtomClash " + ref1 + ":" + ref2 + " d: " + d + " vdwsum: "
                   + vdwsum + " allowance: " + allowance + " overlap: " + 
                   + overlap;
        return s;
    }

//------------------------------------------------------------------------------
}
