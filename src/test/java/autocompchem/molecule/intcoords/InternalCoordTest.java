package autocompchem.molecule.intcoords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.Constraint.ConstraintType;

public class InternalCoordTest 
{

//------------------------------------------------------------------------------

	/**
	 * @return an object good only for testing purposes.
	 */
	public static InternalCoord getTestInternalCoord()
	{
		return new InternalCoord("myIC", 1.234, 
				new ArrayList<Integer>(Arrays.asList(1,2,3,4)), "0"); 
	}
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	InternalCoord i1 = getTestInternalCoord();
    	InternalCoord i2 = getTestInternalCoord();
    	
    	assertTrue(i1.equals(i2));
    	assertTrue(i2.equals(i1));
    	assertTrue(i1.equals(i1));
    	
    	i2.name = "otherName";
    	assertFalse(i1.equals(i2));
    	
    	i2 = getTestInternalCoord();
    	i2.type = "-1";
    	assertFalse(i1.equals(i2));
    	
    	i2 = getTestInternalCoord();
    	i2.value = -2.938;
    	assertFalse(i1.equals(i2));
    	
    	i2 = getTestInternalCoord();
    	i2.ids.set(0, 23);
    	assertFalse(i1.equals(i2));
    }
    
//------------------------------------------------------------------------------

}
