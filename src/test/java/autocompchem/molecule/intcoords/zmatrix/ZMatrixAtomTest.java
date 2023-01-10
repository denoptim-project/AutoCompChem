package autocompchem.molecule.intcoords.zmatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.modeling.constraints.Constraint.ConstraintType;
import autocompchem.molecule.intcoords.InternalCoord;

public class ZMatrixAtomTest 
{

//------------------------------------------------------------------------------

	/**
	 * @return an object good only for testing purposes.
	 */
	public static ZMatrixAtom getTestZMatrixAtom()
	{
		return new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord("dist3", 0.83, 
						new ArrayList<Integer>(Arrays.asList(2,3))),
				new InternalCoord("ang3", 109.5, 
						new ArrayList<Integer>(Arrays.asList(1,2,3))),
				new InternalCoord("dih3", 0.010, 
						new ArrayList<Integer>(Arrays.asList(0,1,2,3)), "0"));
	}
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ZMatrixAtom zma1 = getTestZMatrixAtom();
    	ZMatrixAtom zma2 = getTestZMatrixAtom();
    	
    	assertTrue(zma1.equals(zma2));
    	assertTrue(zma2.equals(zma1));
    	assertTrue(zma1.equals(zma1));
    	
    	zma2.idI = -1;
    	assertFalse(zma1.equals(zma2));
    	
    	zma2 = getTestZMatrixAtom();
    	zma2.idJ = -1;
    	assertFalse(zma1.equals(zma2));
    	
    	zma2 = getTestZMatrixAtom();
    	zma2.idK = -1;
    	assertFalse(zma1.equals(zma2));
    	
    	zma2 = getTestZMatrixAtom();
    	zma2.icI.setValue(-0.1);
    	assertFalse(zma1.equals(zma2));
    	
    	zma2 = getTestZMatrixAtom();
    	zma2.icJ.setValue(-0.1);
    	assertFalse(zma1.equals(zma2));
    	
    	zma2 = getTestZMatrixAtom();
    	zma2.icK.setValue(-0.1);
    	assertFalse(zma1.equals(zma2));
    	
    	zma2 = getTestZMatrixAtom();
    	zma2.name = "newName";
    	assertFalse(zma1.equals(zma2));
    }
    
//------------------------------------------------------------------------------

}
