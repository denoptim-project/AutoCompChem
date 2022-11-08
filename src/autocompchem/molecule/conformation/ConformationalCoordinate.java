package autocompchem.molecule.conformation;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;


/**
 * The conformational coordinate as a predefined change in a molecular structure
 * 
 * @author Marco Foscato 
 */

public class ConformationalCoordinate
{
    /**
     * Reference Name
     */
    private String refName = "noname";

    /**
     * Type
     */
    private String type = "notype";

    /**
     * The list of defining atoms
     */
    private ArrayList<IAtom> atomDef = new ArrayList<IAtom>();

    /**
     * The list of defining atom indexes (0-based IDs)
     */
    private ArrayList<Integer> atomIdDef = new ArrayList<Integer>();

    /**
     * Current numerical value
     */
    private double value = 0.0;

    /**
     * Imposed fold
     */
    private int fold = 1;


//------------------------------------------------------------------------------

    /**
     * Constructor for empty ConformationalCoordinate
     */

    public ConformationalCoordinate()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ConformationalCoordinate with external definition of
     * all the fields
     * @param refName the reference name
     * @param type the type (torsion, inversion)
     * @param atomDef the vector of atoms defining the coordinate
     * @param atomIdDef the vector of defining atom IDs (0-based)
     * @param value the current numerical value
     * @param fold the fold imposed
     */

    public ConformationalCoordinate(String refName, String type, 
                         ArrayList<IAtom> atomDef, ArrayList<Integer> atomIdDef,
                                                         double value, int fold)
    {
        this.refName = refName;
        this.type = type;
        this.atomDef = atomDef;
        this.atomIdDef = atomIdDef;
        this.value = value;
        this.fold = fold;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the fold number
     * @return the fold number
     */

    public int getFold()
    {
        return fold;
    }

//------------------------------------------------------------------------------

    /**
     * Prepares a string with the atom IDs defining this coordinate. The IDs 
     * are sorted according to ascending order.
     * @param oneBased set to <code>true</code> to request 1-based IDs. The 
     * default is to return 0-based IDs.
     * @param format the format (i.e., "%5d")
     * @return the string containing the IDs
     */

    public String getAtomIDsAsString(boolean oneBased, String format)
    {
        int base = 0;
        if (oneBased)
        {
            base = 1;
        }
        StringBuilder sb = new StringBuilder();
        int i0 = atomIdDef.get(0);
        if (atomIdDef.size() > 1)
        {
            int i1 = atomIdDef.get(1);
            if (i0 > i1)
            {
                sb.append(String.format(format,i1 + base));
                sb.append(String.format(format,i0 + base));
            }
            else
            {
                sb.append(String.format(format,i0 + base));
                sb.append(String.format(format,i1 + base));
            }        
        }
        else
        {
            sb.append(String.format(format,i0 + base));
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Equals method considers only the type and the identity of the atoms
     * involved in the definition of the ConformationalCoordinate
     */

    public boolean equals(Object obj)
    {
        boolean res = false;
        if (obj instanceof ConformationalCoordinate)
        {
            ConformationalCoordinate other = (ConformationalCoordinate) obj;
            if (this.type.equals(other.type) && 
                this.atomDef.size() == other.atomDef.size())
            {
                res = true;
                for (int i=0; i<atomDef.size(); i++)
                {
                    if (!other.atomDef.contains(this.atomDef.get(i)))
                    {
                        res = false;
                        break;
                    }
                }
            }
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Get a string representation of this conformational coordinate
     * @return a string representation of this confomational coordinate
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[ConformationalCoordinate ");
        sb.append(" refName=").append(refName).append(", ");
        sb.append(" type=").append(type).append(", ");
        sb.append(" atomDef=").append(atomDef).append(", ");
        sb.append(" atomIdDef=").append(atomIdDef).append(", ");
        sb.append(" value=").append(value).append(", ");
        sb.append(" fold=").append(fold).append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------
}
