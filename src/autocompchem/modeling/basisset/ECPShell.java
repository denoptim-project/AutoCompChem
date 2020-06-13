package autocompchem.modeling.basisset;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.ArrayList;

import autocompchem.run.Terminator;

/**
 * Object representing a single shell for effective core potentials.
 *
 * @author Marco Foscato
 */

public class ECPShell
{
    /**
     * ECPShell type 
     */
    private String type = "";

    /**
     * List of primitive functions
     */
    private List<Primitive> primitives = new ArrayList<Primitive>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ECPShell
     */

    public ECPShell()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an ECPShell with type
     * @param type the type of potential
     */

    public ECPShell(String type)
    {
	this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Add a primitive function
     * @param p the primitive function to be added
     */

    public void add(Primitive p)
    {
	primitives.add(p);
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of this ECPShell
     * @return the type of this ECPShell
     */

    public String getType()
    {
	return type;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the number of primitives
     * @return the number of primitives
     */

    public int getSize()
    {
	return primitives.size();
    }

//------------------------------------------------------------------------------

    /**
     * Deep-clone this ECPShell
     * @return a new object with the same content as this one
     */

     public ECPShell clone()
     {
        ECPShell newShell = new ECPShell(type);
        for (Primitive p : primitives)
        {
            newShell.add(p.clone());
        }
        return newShell;
     }

//------------------------------------------------------------------------------

    /**
     * Returns a string (with newline characters) for reporting the shell with
     * the specified format.
     * Known formats include: "Gaussian".
     * @param format the format for input file-like output
     * @return a list of lines os text that can be used in input files of
     * software packages recognizing the given format.
     */

    public String toInputFileString(String format)
    {
	StringBuilder sb = new StringBuilder();
	String nl = System.getProperty("line.separator");
	switch (format.toUpperCase())
	{
	    case "GAUSSIAN":
		sb.append(String.format("%-20s",type)).append(nl);
		sb.append(String.format(" %2d",primitives.size())).append(nl);
	        for (Primitive p : primitives)
		{
		    sb.append(String.format("%-1d",p.getAngMmnt()));
                    String eForm = "%" + (p.getExpPrecision() + 6) + "."
                                           + (p.getExpPrecision()-1) + "E     ";
                    String cForm = "%" + (p.getCoeffPrecision() + 6) + "."
                                              + (p.getCoeffPrecision()-1) + "E";
                    sb.append(String.format(eForm,p.getExp()));
                    sb.append(String.format(cForm,p.getCoeff()));
		    sb.append(nl);
		}
	        break;

            case "NWCHEM":
		// The header is printed by the CenterBasisSet class.
                for (Primitive p : primitives)
                {
                    sb.append(String.format("  %d ",p.getAngMmnt()));
                    String eForm = "%" + (p.getExpPrecision() + 6) + "."
                                           + (p.getExpPrecision()-1) + "E     ";
                    String cForm = "%" + (p.getCoeffPrecision() + 6) + "."
                                              + (p.getCoeffPrecision()-1) + "E";
                    sb.append(String.format(eForm,p.getExp()));
                    sb.append(String.format(cForm,p.getCoeff()));
                    sb.append(nl);
                }
                break;

	    default:
		String msg = "ERROR! Format '" + format + "' is not a known "
			  + "format for reporting ECPShells. Check your input.";
		Terminator.withMsgAndStatus(msg,-1);
	}
	return sb.toString();
    }

//------------------------------------------------------------------------------

}
