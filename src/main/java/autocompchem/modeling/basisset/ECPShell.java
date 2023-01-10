package autocompchem.modeling.basisset;

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

import java.util.Locale;

import autocompchem.run.Terminator;

/**
 * Object representing a single shell for effective core potentials.
 *
 * @author Marco Foscato
 */

public class ECPShell extends Shell implements Cloneable
{

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ECPShell.
     */

    public ECPShell()
    {
    	super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an ECPShell with type.
     * @param type the type of potential.
     */

    public ECPShell(String type)
    {
    	super(type);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an ECPShell with type and scale factor
     * @param type the type of potential.
     * @param scaleFact the scaling factor.
     */

    public ECPShell(String type, double scaleFact)
    {
    	super(type, scaleFact);
    }

//------------------------------------------------------------------------------

    /**
     * Deep-clone this ECPShell.
     * @return a new object with the same content as this one.
     */

     @Override
     public ECPShell clone()
     {
        ECPShell newShell = new ECPShell(type, scaleFact);
        for (Primitive p : primitives)
        {
            newShell.add(p.clone());
        }
        return newShell;
     }
     
//------------------------------------------------------------------------------
     
     @Override
     public boolean equals(Object o)
     {
    	 if (!(o instanceof ECPShell))
    		 return false;
    	 ECPShell other = (ECPShell) o;
    	 
    	 return super.equals(other);
     }

//------------------------------------------------------------------------------

    /**
     * Returns a string (with newline characters) for reporting the shell with
     * the specified format.
     * Known formats include: "Gaussian".
     * @param format the format for input file-like output
     * @return a list of lines of text that can be used in input files of
     * software packages recognizing the given format.
     */

    public String toInputFileString(String format)
    {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                sb.append(String.format(Locale.ENGLISH,
                		"%-20s",type)).append(nl);
                sb.append(String.format(Locale.ENGLISH,
                		" %2d",primitives.size())).append(nl);
                for (Primitive p : primitives)
                {
                    sb.append(String.format(Locale.ENGLISH,
                    		"%-1d",p.getAngMmnt()));
                    String eForm = "%" + (p.getExpPrecision() + 6) + "."
                                           + (p.getExpPrecision()-1) + "E     ";
                    sb.append(String.format(Locale.ENGLISH,
                    		eForm,p.getExp()));
                    
                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
                            + (p.getCoeffPrecision()-1) + "E";
					for (Double c : p.getCoeff())
					{
					    sb.append(String.format(Locale.ENGLISH,
					    		cForm,c));
					}
                    sb.append(nl);
                }
                break;

            case "NWCHEM":
                // The header is printed by the CenterBasisSet class.
            	for (Primitive p : primitives)
                {
                	sb.append(nl); // we are always writing after the header
                    sb.append(String.format(Locale.ENGLISH,
                    		"  %d ",p.getAngMmnt()));
                    String eForm = "%" + (p.getExpPrecision() + 6) + "."
                                           + (p.getExpPrecision()-1) + "E     ";
                    sb.append(String.format(Locale.ENGLISH,
                    		eForm,p.getExp()));

                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
                            + (p.getCoeffPrecision()-1) + "E";
					for (Double c : p.getCoeff())
					{
					    sb.append(String.format(Locale.ENGLISH,
					    		cForm,c));
					}
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
