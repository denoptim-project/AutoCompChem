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

public class ZMatrixTest 
{

//------------------------------------------------------------------------------

	/**
	 * @return an object good only for testing purposes.
	 */
	public static ZMatrix getTestZMatrix()
	{
		AtomicInteger distCounter = new AtomicInteger(1);
		AtomicInteger angCounter = new AtomicInteger(1);
		AtomicInteger torCounter = new AtomicInteger(1);
		
		ZMatrix zm = new ZMatrix();
		zm.addZMatrixAtom(new ZMatrixAtom("H"));
		zm.addZMatrixAtom(new ZMatrixAtom("O", 0, new InternalCoord(
				InternalCoordNaming.getSequentialDistName(distCounter), 
				1.8, new ArrayList<Integer>(Arrays.asList(1,0)))));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 1, 0, 
				new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 2.1, 
						new ArrayList<Integer>(Arrays.asList(2,1))),
				new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 109.5, 
						new ArrayList<Integer>(Arrays.asList(2,1,0)))));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.83, 
						new ArrayList<Integer>(Arrays.asList(3,2))),
				new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 109.5, 
						new ArrayList<Integer>(Arrays.asList(3,2,1))),
				new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 0.010, 
						new ArrayList<Integer>(Arrays.asList(3,2,1,0)))));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.84, 
						new ArrayList<Integer>(Arrays.asList(4,2))),
				new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 109.5, 
						new ArrayList<Integer>(Arrays.asList(4,2,1))),
				new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 120.09, 
						new ArrayList<Integer>(Arrays.asList(4,2,1,0)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 2, 1, 0, 
				new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.85, 
						new ArrayList<Integer>(Arrays.asList(5,2))),
				new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 109.5, 
						new ArrayList<Integer>(Arrays.asList(5,2,1))),
				new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), -120.09, 
						new ArrayList<Integer>(Arrays.asList(5,2,1,0)), "0")));
		zm.addPointerToBonded(0, 2);
		zm.addPointerToNonBonded(1, 2);
		return zm;
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testConstructorFromLines() throws Exception
	{
    	ZMatrix zm1 = getTestZMatrix();
		ArrayList<String> lines = zm1.toLinesOfText(false, false);
		ZMatrix zm2 = new ZMatrix(lines);

		// Added/removed bonds are not part of the content written to text, so 
		// we need to add them here to expect the two ZMatrices to be equal.
		zm2.addPointerToBonded(0, 2);
		zm2.addPointerToNonBonded(1, 2);

		assertTrue(zm1.equals(zm2));
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

    @Test
    public void testFindBondDistance() throws Exception
    {
    	ZMatrix zm = getTestZMatrix();
    	
    	// Test finding existing bond distances - order should not matter
    	// Atom 1 (O) uses bond distance to atom 0 (H)
    	ZMatrixAtom result = zm.findBondDistance(1, 0);
    	assertTrue(result != null, "Should find bond distance between atoms 1 and 0");
    	assertTrue(result.equals(zm.getZAtom(1)), "Should return atom at index 1");
    	
    	// Test reverse order - should find the same bond
    	result = zm.findBondDistance(0, 1);
    	assertTrue(result != null, "Should find bond distance between atoms 0 and 1 (reverse order)");
    	assertTrue(result.equals(zm.getZAtom(1)), "Should return atom at index 1 (same as forward order)");
    	
    	// Atom 2 (C) uses bond distance to atom 1 (O)
    	result = zm.findBondDistance(2, 1);
    	assertTrue(result != null, "Should find bond distance between atoms 2 and 1");
    	assertTrue(result.equals(zm.getZAtom(2)), "Should return atom at index 2");
    	
    	result = zm.findBondDistance(1, 2);
    	assertTrue(result != null, "Should find bond distance between atoms 1 and 2 (reverse order)");
    	assertTrue(result.equals(zm.getZAtom(2)), "Should return atom at index 2");
    	
    	// Atom 3 (H) uses bond distance to atom 2 (C)
    	result = zm.findBondDistance(3, 2);
    	assertTrue(result != null, "Should find bond distance between atoms 3 and 2");
    	assertTrue(result.equals(zm.getZAtom(3)), "Should return atom at index 3");
    	
    	result = zm.findBondDistance(2, 3);
    	assertTrue(result != null, "Should find bond distance between atoms 2 and 3 (reverse order)");
    	assertTrue(result.equals(zm.getZAtom(3)), "Should return atom at index 3");
    	
    	// Atom 4 (H) uses bond distance to atom 2 (C)
    	result = zm.findBondDistance(4, 2);
    	assertTrue(result != null, "Should find bond distance between atoms 4 and 2");
    	assertTrue(result.equals(zm.getZAtom(4)), "Should return atom at index 4");
    	
    	result = zm.findBondDistance(2, 4);
    	assertTrue(result != null, "Should find bond distance between atoms 2 and 4 (reverse order)");
    	assertTrue(result.equals(zm.getZAtom(4)), "Should return atom at index 4");
    	
    	// Atom 5 (H) uses bond distance to atom 2 (C)
    	result = zm.findBondDistance(5, 2);
    	assertTrue(result != null, "Should find bond distance between atoms 5 and 2");
    	assertTrue(result.equals(zm.getZAtom(5)), "Should return atom at index 5");
    	
    	result = zm.findBondDistance(2, 5);
    	assertTrue(result != null, "Should find bond distance between atoms 2 and 5 (reverse order)");
    	assertTrue(result.equals(zm.getZAtom(5)), "Should return atom at index 5");
    	
    	// Test cases where bond distance does not exist
    	// No bond distance between atoms 0 and 2
    	result = zm.findBondDistance(0, 2);
    	assertTrue(result == null, "Should return null - no bond distance between atoms 0 and 2");
    	
    	result = zm.findBondDistance(2, 0);
    	assertTrue(result == null, "Should return null - no bond distance between atoms 2 and 0");
    	
    	// No bond distance between atoms 1 and 3
    	result = zm.findBondDistance(1, 3);
    	assertTrue(result == null, "Should return null - no bond distance between atoms 1 and 3");
    	
    	result = zm.findBondDistance(3, 1);
    	assertTrue(result == null, "Should return null - no bond distance between atoms 3 and 1");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetStructureCopy() throws Exception
    {
    	ZMatrix original = getTestZMatrix();
    	original.setTitle("Original ZMatrix");
    	
    	// Create structure copy with new title
    	String newTitle = "Structure Copy";
    	ZMatrix copy = original.getStructureCopy(newTitle);
    	
    	// Verify it's a different object
    	assertNotSame(original, copy, "Structure copy should be a new object");
    	
    	// Verify title is set correctly
    	assertEquals(newTitle, copy.getTitle(), "Title should be the new title");
    	assertEquals("Original ZMatrix", original.getTitle(), "Original title should be unchanged");
    	
    	// Verify same number of atoms
    	assertEquals(original.getZAtomCount(), copy.getZAtomCount(), "Should have same number of atoms");
    	
    	// Verify structure is preserved but values are set to 0.0
    	for (int i = 0; i < original.getZAtomCount(); i++)
    	{
    		ZMatrixAtom origAtom = original.getZAtom(i);
    		ZMatrixAtom copyAtom = copy.getZAtom(i);
    		
    		// Verify it's a different object
    		assertNotSame(origAtom, copyAtom, "Each ZMatrixAtom should be a new object");
    		
    		// Verify name is preserved
    		assertEquals(origAtom.getName(), copyAtom.getName(), 
    				"Atom " + i + " name should be preserved");
    		
    		// Verify reference IDs are preserved
    		assertEquals(origAtom.getIdRef(0), copyAtom.getIdRef(0), 
    				"Atom " + i + " first reference ID should be preserved");
    		assertEquals(origAtom.getIdRef(1), copyAtom.getIdRef(1), 
    				"Atom " + i + " second reference ID should be preserved");
    		assertEquals(origAtom.getIdRef(2), copyAtom.getIdRef(2), 
    				"Atom " + i + " third reference ID should be preserved");
    		
    		// Verify internal coordinate values are set to 0.0
    		if (copyAtom.getIC(0) != null)
    		{
    			assertEquals(0.0, copyAtom.getIC(0).getValue(), 0.0001,
    					"Atom " + i + " first IC value should be 0.0");
    		}
    		if (copyAtom.getIC(1) != null)
    		{
    			assertEquals(0.0, copyAtom.getIC(1).getValue(), 0.0001,
    					"Atom " + i + " second IC value should be 0.0");
    		}
    		if (copyAtom.getIC(2) != null)
    		{
    			assertEquals(0.0, copyAtom.getIC(2).getValue(), 0.0001,
    					"Atom " + i + " third IC value should be 0.0");
    		}
    	}
    	
    	// Verify bond pointers are preserved
    	assertEquals(original.hasAddedBonds(), copy.hasAddedBonds(), 
    			"Should have same hasAddedBonds flag");
    	assertEquals(original.hasBondsToDelete(), copy.hasBondsToDelete(), 
    			"Should have same hasBondsToDelete flag");
    	assertEquals(original.getPointersToBonded().size(), copy.getPointersToBonded().size(), 
    			"Should have same number of bonded pointers");
    	assertEquals(original.getPointersToNonBonded().size(), copy.getPointersToNonBonded().size(), 
    			"Should have same number of non-bonded pointers");
    	
    	// Verify original values are not modified
    	assertEquals(1.8, original.getZAtom(1).getIC(0).getValue(), 0.0001, 
    			"Original atom 1 IC value should be unchanged");
    	assertEquals(2.1, original.getZAtom(2).getIC(0).getValue(), 0.0001, 
    			"Original atom 2 IC value should be unchanged");
    }
    
//------------------------------------------------------------------------------

}
