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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.openscience.cdk.interfaces.IAtom;

import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;


/**
 * Class representing the a predefined change of an internal coordinate, which 
 * is defined by a tuple of atoms that all belong to the same chemical 
 * structure, typically a molecule. The coordinate can be associated with a 
 * "fold" number that specifies how extensively the coordinate is explored. 
 * To illustrate, fold=3 for a conformational coordinate corresponding to the
 * torsion of a bond defined an exploration of three cases where the bond is
 * twisted by 0, 120, and 240 DEG.
 * We assume that {@link ConformationalCoordinate} can be defined only 
 * by 1, 2, or 4 atoms.
 *
 * @author Marco Foscato 
 */

public class ConformationalCoordinate extends AnnotatedAtomTuple
{

    /**
     * The ordered list of defining atoms.
     */
    private ArrayList<IAtom> atomDef = new ArrayList<IAtom>();

    /**
     * The number that specifies how extensively the coordinate is explored. 
	 * To illustrate, fold=3 for a conformational coordinate corresponding to the
	 * torsion of a bond defined an exploration of three cases where the bond is
	 * twisted by 0, 120, and 240 DEG. Fold=1 implies no change in this 
	 * coordinate, which essentially means this coordinate is constant and
	 * it does not extend define any new conformation.
	 */
    private int fold = 1;
    
	/**
	 * Classes of the constraints according to the corresponding internal 
	 * coordinate possibly represented.
	 */
	public enum ConformationalCoordType {TORSION, FLIP, UNDEFINED}
	
	/**
	 * The type of this constraint
	 */
	private ConformationalCoordType type = ConformationalCoordType.UNDEFINED;
  
//------------------------------------------------------------------------------

  	/**
  	 * Constructs a coordinate from an annotated atom tuple.
  	 * @param tuple the tuple to parse into a conformational coordinate.
  	 */
  	
  	public ConformationalCoordinate(AnnotatedAtomTuple tuple)
  	{
  		super(tuple.getAtomIDs(), tuple.getValuelessAttribute(), 
  				tuple.getValuedAttributes());
  		switch (getNumberOfIDs())
		{
			case 1:
				type = ConformationalCoordType.FLIP;
				break;
				
			case 2:
				type = ConformationalCoordType.TORSION;
				break;
				
			case 4:
				type = ConformationalCoordType.TORSION;
				break;
				
			default:
				throw new IllegalArgumentException("Unexpected number of "
						+ "atom IDs (" + getNumberOfIDs() + "). "
						+ "Cannot define the type of this conformational "
						+ "coordinate: " + tuple);
		}
  		if (!this.hasValueledAttribute(ConformationalCoordDefinition.KEYFOLD))
  		{
  			this.setValueOfAttribute(ConformationalCoordDefinition.KEYFOLD,"1");
  		}
  	}

//------------------------------------------------------------------------------

    /**
     * Returns the fold number.
     * @return the fold number.
     */

    public int getFold()
    {
        return fold;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the type of motion among those defined by 
     * {@link ConformationalCoordType}.
     * @return the type of motion.
     */

    public ConformationalCoordType getType()
    {
        return type;
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

    @Deprecated
    public String getAtomIDsAsString(boolean oneBased, String format)
    {
        int base = 0;
        if (oneBased)
        {
            base = 1;
        }
        StringBuilder sb = new StringBuilder();
        int i0 = getAtomIDs().get(0);
        if (getAtomIDs().size() > 1)
        {
            int i1 = getAtomIDs().get(1);
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

    @Override
    public boolean equals(Object o)
    {
    	if (o== null)
    		return false;
    	
 	    if (o == this)
 		    return true;
 	   
 	    if (o.getClass() != getClass())
     		return false;
 	    
 	    ConformationalCoordinate other = (ConformationalCoordinate) o;
 	   
 	    if (this.atomDef.size() != other.atomDef.size())
 	    	return false;
        
        for (int iAtm=0; iAtm<this.atomDef.size();iAtm++)
        {
        	IAtom tAtm = this.atomDef.get(iAtm);
        	IAtom oAtm = other.atomDef.get(iAtm);
        	if (tAtm != oAtm)
        		return false;
        }
        
        return super.equals(o);
    }

//------------------------------------------------------------------------------
    
}
