package autocompchem.molecule.intcoords.zmatrix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import autocompchem.molecule.intcoords.InternalCoord;

public class ZMatrixTest 
{

//------------------------------------------------------------------------------

	/**
	 * @return an object good only for testing purposes.
	 */
	public static ZMatrix getTestZMatrix()
	{
		ZMatrix zm = new ZMatrix();
		zm.addZMatrixAtom(new ZMatrixAtom("H"));
		zm.addZMatrixAtom(new ZMatrixAtom("O", 0, new InternalCoord("dist1", 
				1.8, new ArrayList<Integer>(Arrays.asList(0,1)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 1, 0, 
				new InternalCoord("dist2", 2.1, 
						new ArrayList<Integer>(Arrays.asList(1,2))),
				new InternalCoord("ang2", 109.5, 
						new ArrayList<Integer>(Arrays.asList(0,1,2)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord("dist3", 0.83, 
						new ArrayList<Integer>(Arrays.asList(2,3))),
				new InternalCoord("ang3", 109.5, 
						new ArrayList<Integer>(Arrays.asList(1,2,3))),
				new InternalCoord("dih3", 0.010, 
						new ArrayList<Integer>(Arrays.asList(0,1,2,3)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord("dist4", 0.84, 
						new ArrayList<Integer>(Arrays.asList(2,4))),
				new InternalCoord("ang4", 109.5, 
						new ArrayList<Integer>(Arrays.asList(1,2,4))),
				new InternalCoord("dih4", 120.09, 
						new ArrayList<Integer>(Arrays.asList(0,1,2,4)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord("dist5", 0.85, 
						new ArrayList<Integer>(Arrays.asList(2,5))),
				new InternalCoord("ang5", 109.5, 
						new ArrayList<Integer>(Arrays.asList(1,2,5))),
				new InternalCoord("dih5", -120.09, 
						new ArrayList<Integer>(Arrays.asList(0,1,2,5)), "0")));
		zm.addPointerToBonded(0, 2);
		zm.addPointerToNonBonded(1, 2);
		return zm;
	}
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ZMatrix zm1 = getTestZMatrix();
    	ZMatrix zm2 = getTestZMatrix();
    	
    	assertTrue(zm1.equals(zm2));
    	assertTrue(zm2.equals(zm1));
    	assertTrue(zm1.equals(zm1));
    	
    	zm2.getZAtom(0).name = "changed";
    	assertFalse(zm1.equals(zm2));
    	
    	zm2 = getTestZMatrix();
    	zm2.addZMatrixAtom(new ZMatrixAtom("added"));
    	assertFalse(zm1.equals(zm2));
    	
    	zm2 = getTestZMatrix();
    	zm2.getZAtom(2).icI = new InternalCoord("bla", 0.001, null);
    	assertFalse(zm1.equals(zm2));
    	
    	zm2 = getTestZMatrix();
    	zm2.addPointerToBonded(1, 3);
    	assertFalse(zm1.equals(zm2));
    	
    	zm2 = getTestZMatrix();
    	zm2.addPointerToNonBonded(1, 2);
    	assertFalse(zm1.equals(zm2));
    }
    
//------------------------------------------------------------------------------

}
