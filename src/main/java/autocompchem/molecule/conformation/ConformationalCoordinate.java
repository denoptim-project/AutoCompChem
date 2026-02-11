package autocompchem.molecule.conformation;


import java.util.Arrays;
import java.util.Objects;

import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.utils.NumberUtils;


/**
 * Class representing the a predefined change of an internal coordinate, which 
 * is defined by a tuple of atoms that all belong to the same chemical 
 * structure, typically a molecule. The coordinate can be associated with a 
 * "fold" number that specifies how extensively the coordinate is explored. 
 * To illustrate, fold=3 for a conformational coordinate corresponding to the
 * torsion of a bond defined an exploration of three cases where the bond is
 * twisted by 0, 120, and 240 DEG.
 * We assume that {@link ConformationalCoordinate} can be defined only 
 * by 1, 2, or 4 atoms and, is neighboring relations are defined, we assign
 * the {@link ConformationalCoordType} accordingly to these rules: <ul>
 * <li>if the tuple contains only 1 atom, then we get 
 * {@value ConformationalCoordType#FLIP} </li>
 * <li>with 2 atoms that are neighbors, we have a 
 * {@value ConformationalCoordType#TORSION}</li>
 * <li>with 4 atoms that are consequently connected, we have a 
 * {@value ConformationalCoordType#TORSION}</li>
 * <li>with 4 atoms that are NOT consequently connected, we have a 
 * {@value ConformationalCoordType#IMPROPERTORSION}</li>
 * <li>Any other case with 2 or 4 atoms results in 
 * {@value ConformationalCoordType#UNDEFINED} type.</li>
 * <li></li>
 * </ul>
 *
 * @author Marco Foscato 
 */

public class ConformationalCoordinate extends AnnotatedAtomTuple 
{
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
	public enum ConformationalCoordType {TORSION, FLIP, IMPROPERTORSION,
		UNDEFINED}
	
	/**
	 * The type of this constraint
	 */
	private ConformationalCoordType type = ConformationalCoordType.UNDEFINED;
	
	
//------------------------------------------------------------------------------

  	/**
  	 * Constructs a coordinate from an annotated atom tuple.
  	 * @param tuple the tuple to parse into a conformational coordinate.
  	 */
  	
  	public ConformationalCoordinate(int[] ids)
  	{
  		super(ids);
  		defineType();
  		parseFold();
  	}
  
//------------------------------------------------------------------------------

  	/**
  	 * Constructs a coordinate from an annotated atom tuple.
  	 * @param tuple the tuple to parse into a conformational coordinate.
  	 */
  	
  	public ConformationalCoordinate(AnnotatedAtomTuple tuple)
  	{
  		super(tuple.getAtomIDs(), tuple.getAtmLabels(), 
  				tuple.getValuelessAttribute(), 
  				tuple.getValuedAttributes(), tuple.getNeighboringRelations(),
				tuple.getNumAtoms());
  		defineType();
  		parseFold();
  	}
  	
//------------------------------------------------------------------------------
  	
  	private void defineType()
  	{
  		type = ConformationalCoordType.UNDEFINED;
  		switch (getAtomIDs().size())
		{
			case 1:
			{
				type = ConformationalCoordType.FLIP;
				break;
			}
			
			case 2:
			{
				if (getNeighboringRelations()!=null && areNeighbors(0, 1))
					type = ConformationalCoordType.TORSION;
				break;
			}	
			
			case 4:
			{
				if (getNeighboringRelations()!=null)
				{
					if (areNeighbors(0, 1) && areNeighbors(1, 2) 
							&& areNeighbors(2, 3))
					{
						type = ConformationalCoordType.TORSION;
					} else {
						type = ConformationalCoordType.IMPROPERTORSION;
					}
				}
				break;
			}
			
			default:
				throw new IllegalArgumentException("Unexpected number of "
						+ "atom IDs (" + getAtomIDs().size() + "). "
						+ "Cannot define the type of this conformational "
						+ "coordinate.");
		}
  	}
  	
//------------------------------------------------------------------------------
  	
  	private void parseFold()
  	{
  		if (!hasValuedAttribute(ConformationalCoordDefinition.KEYFOLD))
  		{
  			setValueOfAttribute(ConformationalCoordDefinition.KEYFOLD, "1");
  		}
  		fold = Integer.parseInt(getValueOfAttribute(
  				ConformationalCoordDefinition.KEYFOLD));
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
     * Sets the fold number.
     * @param fold the new fold of this coordinate.
     */

    public void setFold(int fold)
    {
        this.fold = fold;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the selected steps to consider for this coordinate.
     * @return the selected steps to consider for this coordinate or 
     * <code>null</code>.
     */

    public int[] getSteps()
    {
    	if (hasValuedAttribute(ConformationalCoordDefinition.KEYSTEPS))
    	{
    		return NumberUtils.parseArrayOfInt(getValueOfAttribute(
	  				ConformationalCoordDefinition.KEYSTEPS));
    	}
        return null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the selected steps to consider for this coordinate.
     * @param steps the selected steps to consider for this coordinate
     */

    public void setSteps(int[] steps)
    {
        this.setValueOfAttribute(ConformationalCoordDefinition.KEYSTEPS,
        		Arrays.toString(steps));
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the selected steps to consider for this coordinate.
     * @return the selected steps to consider for this coordinate or 
     * <code>null</code> if no values are available.
     */

    public double[] getValues()
    {
    	if (this.hasValuedAttribute(ConformationalCoordDefinition.KEYVALUES))
    	{
            return NumberUtils.parseArrayOfDouble(this.getValueOfAttribute(
            		ConformationalCoordDefinition.KEYVALUES));
    	}
    	return null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the selected steps to consider for this coordinate.
     * @param steps the selected steps to consider for this coordinate
     */

    public void setValues(double[] values)
    {
        this.setValueOfAttribute(ConformationalCoordDefinition.KEYVALUES,
        		Arrays.toString(values));
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
     * Sets he type of motion among irrespectively on the number of centers
     * defining this coordinate. <b>WARNING:</b> using this method can create 
     * inconsistency between the type and the number of centers defining this
     * coordinate.
     * @param type the new type.
     */

    public void setType(ConformationalCoordType type)
    {
        this.type = type;
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
 	   
 	    if (this.fold!=other.fold)
 	    	return false;
 	    
 	   if (this.type!=other.type)
	    	return false;
 	    
 	    /*
 	    if (this.atomDef.size() != other.atomDef.size())
 	    	return false;
        
        for (int iAtm=0; iAtm<this.atomDef.size();iAtm++)
        {
        	IAtom tAtm = this.atomDef.get(iAtm);
        	IAtom oAtm = other.atomDef.get(iAtm);
        	if (tAtm != oAtm)
        		return false;
        }
        */
        
        return super.equals(o);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(fold, type);
    }
    
//-----------------------------------------------------------------------------

  	@Override
  	public ConformationalCoordinate clone()
  	{
  		ConformationalCoordinate clone = new ConformationalCoordinate(
  				super.clone());
  		// Type and fold are  inferred from super but they can be overwritten 
  		// by this class, so we set it here:
  		clone.type = this.type; 
  		clone.fold = this.fold; 
  		return clone;
  	}

//------------------------------------------------------------------------------
    
    @Override
    protected String generateStringForSubClassFields()
    {
    	return "type:" + type + ", fold:" + fold + ", ";
    }
    
//------------------------------------------------------------------------------
    
}
