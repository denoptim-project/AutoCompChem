package autocompchem.molecule.intcoords.zmatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import autocompchem.molecule.intcoords.InternalCoord;

public class ZMatrixAtomTest 
{

//------------------------------------------------------------------------------

	/**
	 * @return an object good only for testing purposes.
	 */
	public static ZMatrixAtom getTestZMatrixAtom()
	{
		AtomicInteger distCounter = new AtomicInteger(1);
		AtomicInteger angCounter = new AtomicInteger(1);
		AtomicInteger torCounter = new AtomicInteger(1);
		
		return new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.83, 
						new ArrayList<Integer>(Arrays.asList(3,2))),
				new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 109.5, 
						new ArrayList<Integer>(Arrays.asList(3,2,1))),
				new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 0.010, 
						new ArrayList<Integer>(Arrays.asList(3,2,1,0)), "0"));
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

    @Test
    public void testGetStructureCopy() throws Exception
    {
    	ZMatrixAtom original = getTestZMatrixAtom();
    	
    	// Get structure copy
    	ZMatrixAtom copy = original.getStructureCopy();
    	
    	// Verify it's a different object
    	assertNotSame(original, copy, "Structure copy should be a new object");
    	
    	// Verify structure is preserved
    	assertEquals(original.getName(), copy.getName(), "Name should be the same");
    	assertEquals(original.getIdRef(0), copy.getIdRef(0), "First reference ID should be the same");
    	assertEquals(original.getIdRef(1), copy.getIdRef(1), "Second reference ID should be the same");
    	assertEquals(original.getIdRef(2), copy.getIdRef(2), "Third reference ID should be the same");
    	
    	// Verify all internal coordinate values are set to 0.0
    	assertEquals(0.0, copy.getIC(0).getValue(), 0.0001, "First IC value should be 0.0");
    	assertEquals(0.0, copy.getIC(1).getValue(), 0.0001, "Second IC value should be 0.0");
    	assertEquals(0.0, copy.getIC(2).getValue(), 0.0001, "Third IC value should be 0.0");
    	
    	// Verify original values are not modified
    	assertEquals(0.83, original.getIC(0).getValue(), 0.0001, "Original first IC value should be unchanged");
    	assertEquals(109.5, original.getIC(1).getValue(), 0.0001, "Original second IC value should be unchanged");
    	assertEquals(0.010, original.getIC(2).getValue(), 0.0001, "Original third IC value should be unchanged");
    	
    	// Test with an atom that has fewer internal coordinates
    	ZMatrixAtom simpleAtom = new ZMatrixAtom("O", 0, new InternalCoord("dst1", 1.8, 
    			new ArrayList<Integer>(Arrays.asList(0,1)), "0"));
    	ZMatrixAtom simpleCopy = simpleAtom.getStructureCopy();
    	
    	assertEquals("O", simpleCopy.getName(), "Name should be preserved");
    	assertEquals(0, simpleCopy.getIdRef(0), "Reference ID should be preserved");
    	assertEquals(0.0, simpleCopy.getIC(0).getValue(), 0.0001, "IC value should be 0.0");
    	assertEquals(1.8, simpleAtom.getIC(0).getValue(), 0.0001, "Original IC value should be unchanged");
    	
    	// Test with atom with no internal coordinates
    	ZMatrixAtom noICAtom = new ZMatrixAtom("H");
    	ZMatrixAtom noICCopy = noICAtom.getStructureCopy();
    	
    	assertEquals("H", noICCopy.getName(), "Name should be preserved");
    	assertNotSame(noICAtom, noICCopy, "Should be a different object");
    }
    
//------------------------------------------------------------------------------

}
