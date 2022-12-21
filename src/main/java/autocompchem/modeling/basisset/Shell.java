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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import autocompchem.run.Terminator;

/**
 * Object representing a single shell: a set of functions that have the same
 * value of angular momentum quantum number.
 *
 * @author Marco Foscato
 */

public class Shell implements Cloneable
{
    /**
     * Shell type (S, P, D, SP, SPD, F, G, ...)
     */
    protected String type = "";

    /**
     * List of primitive functions
     */
    protected List<Primitive> primitives = new ArrayList<Primitive>();

    /**
     * Scale factor
     */
    protected double scaleFact = 1.0;

	
//------------------------------------------------------------------------------

	/**
     * Constructor for an empty Shell
     */

    public Shell()
    {}

//------------------------------------------------------------------------------

    /**
     * Constructor for a Shell with definition of the type of shell
     * @param type the type of shell
     */

    public Shell(String type)
    {
        this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Shell with definition of the type of shell and the 
     * scale factor.
     * @param type the type of shell.
     * @param scaleFact the scaling factor.
     */

    public Shell(String type, double scaleFact)
    {
        this.type = type;
        this.scaleFact = scaleFact;
    }

//------------------------------------------------------------------------------

    /**
     * Add a primitive function.
     * @param p the primitive function to be added.
     */

    public void add(Primitive p)
    {
        primitives.add(p);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the primitives in this shell.
     */
	public List<Primitive> getPrimitives() 
	{
		return primitives;
	}

//------------------------------------------------------------------------------

	/**
	 * Overwrites the list of primitives with the given list.
	 * @param primitives the new primitives.
	 */
	public void setPrimitives(List<Primitive> primitives) 
	{
		this.primitives = primitives;
	}

//------------------------------------------------------------------------------

    /**
     * Returns the type of this shell.
     * @return the type.
     */

    public String getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the type of this shell.
     */

    public void setType(String type)
    {
        this.type = type;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the scaling factor of this shell.
     */
    public double getScaleFact() 
    {
		return scaleFact;
	}

//------------------------------------------------------------------------------

    /**
     * Sets the scaling factor of this shell.
     * @param scaleFact the scaling factor to use.
     */
	public void setScaleFact(double scaleFact) 
	{
		this.scaleFact = scaleFact;
	}

    
//------------------------------------------------------------------------------

    /**
     * Returns the number of primitives.
     * @return the number of primitives.
     */

    public int getSize()
    {
        return primitives.size();
    }

//------------------------------------------------------------------------------

    /**
     * Deep-clone this shell.
     * @return a new object with the same content as this one.
     */

     public Shell clone()
     {
        Shell newShell = new Shell(type, scaleFact);
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
    	 if (!(o instanceof Shell))
    		 return false;
    	 Shell other = (Shell) o;
    	 
    	 if (this.primitives.size() != other.primitives.size())
    		 return false;
    				 
		 for (int i=0; i<this.primitives.size() ; i++)
		 {
			 if (!this.primitives.get(i).equals(other.primitives.get(i)))
				 return false;
		 }
    	 
    	 return this.type.equals(other.type) 
    			 && this.scaleFact == other.scaleFact;
     }
     
//------------------------------------------------------------------------------

    /**
     * Returns a string (with newline characters) for reporting the shell with
     * the specified format.
     * Known formats include: "Gaussian".
     * @param format the format for input file-like output
     * @param atomId the atom label to be added to the output lines
     * @return a list of lines of text that can be used in input files of
     * software packages recognizing the given format.
     */

     @Deprecated
    public String toInputFileString(String format, String atomId)
    {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                sb.append(String.format(Locale.ENGLISH, "%-3s %-3d %-7.3f",
                                  type,primitives.size(),scaleFact)).append(nl);
                for (Primitive p : primitives)
                {
                    String eForm = "%" + (p.getExpPrecision() + 6) + "." 
                                           + (p.getExpPrecision()-1) + "E     ";
                    sb.append(String.format(Locale.ENGLISH, eForm,p.getExp()));
                    
                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
                                              + (p.getCoeffPrecision()-1) + "E";
                    for (Double c : p.getCoeff())
                    {
                    	sb.append(String.format(Locale.ENGLISH, cForm, c));
                    }
                    sb.append(nl);
                }
                break;

            case "NWCHEM":
                String atmStr = Character.toUpperCase(atomId.charAt(0)) 
                	+ atomId.toLowerCase().substring(1);
                sb.append(String.format(Locale.ENGLISH, 
                		"  %s %s", atmStr, type));
                for (Primitive p : primitives)
                {
                    String eForm = "%" + (p.getExpPrecision() + 6) + "."
                                          + (p.getExpPrecision()-1) + "E      ";
                    sb.append(nl).append(String.format(Locale.ENGLISH, 
                    		eForm,p.getExp()));
                    
                    String cForm = " %" + (p.getCoeffPrecision() + 6) + "."
                            + (p.getCoeffPrecision()-1) + "E";
					for (Double c : p.getCoeff())
					{
						sb.append(String.format(Locale.ENGLISH, 
								cForm, c));
					}
                }
                break;

            default:
                String msg = "ERROR! Format '" + format + "' is not a known "
                             + "format for reporting basis sets (in Shell). "
                             + "Check your input.";
                Terminator.withMsgAndStatus(msg,-1);
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
