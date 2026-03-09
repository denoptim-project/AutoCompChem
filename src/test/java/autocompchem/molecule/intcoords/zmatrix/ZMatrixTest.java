package autocompchem.molecule.intcoords.zmatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Test
    public void testFindAllFirstAnglesInvalidArgs()
    {
    	ZMatrix zm = getTestZMatrix();
    	assertThrows(IllegalArgumentException.class, () -> zm.findAllFirstAngles(),
    			"Zero ids should throw");
    	assertThrows(IllegalArgumentException.class, () -> zm.findAllFirstAngles(null),
    			"Null ids should throw");
    	assertThrows(IllegalArgumentException.class,
    			() -> zm.findAllFirstAngles(0, 1, 2, 3),
    			"Four ids should throw");
    }

    @Test
    public void testFindAllFirstAngles()
    {
    	ZMatrix zm = getTestZMatrix();
    	// Test matrix: atom 2 has first angle (refs 0,1) = 1,0; atoms 3,4,5 have first angle = 2,1

    	// Two indexes: angle between the two centers (order independent)
    	List<ZMatrixAtom> list = zm.findAllFirstAngles(1, 0);
    	assertEquals(1, list.size(), "Exactly one atom has first angle 1-0");
    	assertTrue(list.contains(zm.getZAtom(2)), "Atom 2 has first angle 1-0");

    	list = zm.findAllFirstAngles(2, 1);
    	assertEquals(4, list.size(), "Three atoms have first angle 2-1");
    	assertTrue(list.contains(zm.getZAtom(3)) && list.contains(zm.getZAtom(4))
    			&& list.contains(zm.getZAtom(5)) && list.contains(zm.getZAtom(2)), "Atoms 3,4,5 have first angle 2-1");

    	// One index: any first angle involving that center (ref0 or ref1)
    	list = zm.findAllFirstAngles(0);
    	assertEquals(1, list.size(), "One atom has first refs involving 0 (atom 1: dist to 0)");
    	assertTrue(list.contains(zm.getZAtom(2)));

    	list = zm.findAllFirstAngles(1);
    	assertEquals(4, list.size(), "Four atoms have first angle involving 1");
    	list = zm.findAllFirstAngles(2);
    	assertEquals(4, list.size(), "Three atoms have first angle involving 2");

    	// Three indexes: first angle between any two of the three centers
    	list = zm.findAllFirstAngles(0, 1, 2);
    	assertEquals(1, list.size(), "One atom has first angle between two of {0,1,2}");
    	assertTrue(list.contains(zm.getZAtom(2)));
    }

    @Test
    public void testFindAllSecondAnglesInvalidArgs()
    {
    	ZMatrix zm = getTestZMatrix();
    	assertThrows(IllegalArgumentException.class, () -> zm.findAllSecondAngles(),
    			"Zero ids should throw");
    	assertThrows(IllegalArgumentException.class, () -> zm.findAllSecondAngles(null),
    			"Null ids should throw");
    	assertThrows(IllegalArgumentException.class,
    			() -> zm.findAllSecondAngles(0, 1, 2, 3),
    			"Four ids should throw");
    }

    @Test
    public void testFindAllSecondAngles()
    {
    	ZMatrix zm = getTestZMatrix();

		zm.addZMatrixAtom(new ZMatrixAtom("He", 1, 2, 3, 
				new InternalCoord("He-dist", 0.85, 
						new ArrayList<Integer>(Arrays.asList(6,1))),
				new InternalCoord("He-ang1", 109.5, 
						new ArrayList<Integer>(Arrays.asList(6,1,2))),
				new InternalCoord("He-ang2", -120.09, 
						new ArrayList<Integer>(Arrays.asList(6,1,3)), "1")));

		zm.addZMatrixAtom(new ZMatrixAtom("He", 1, 2, 3, 
				new InternalCoord("He-dist", 0.85, 
						new ArrayList<Integer>(Arrays.asList(7,1))),
				new InternalCoord("He-ang1", 109.5, 
						new ArrayList<Integer>(Arrays.asList(7,1,2))),
				new InternalCoord("He-ang2", -120.09, 
						new ArrayList<Integer>(Arrays.asList(7,1,3)), "-1")));

    	// Two indexes: angle between the two centers
    	List<ZMatrixAtom> list = zm.findAllSecondAngles(1, 0);
    	assertEquals(0, list.size(), "No atom has second angle 1-0");

    	list = zm.findAllSecondAngles(2, 1);
    	assertEquals(0, list.size(), "No atom has second angle 2-1");

    	// One index: any second angle involving that center
    	list = zm.findAllSecondAngles(1);
    	assertEquals(2, list.size(), "Two atoms have second angle involving 1");
    	list = zm.findAllSecondAngles(0);
    	assertEquals(0, list.size(), "No atom has second angle involving 0");
    	list = zm.findAllSecondAngles(2);
    	assertEquals(0, list.size(), "No atoms have second angle involving 2");
    	list = zm.findAllSecondAngles(3);
    	assertEquals(2, list.size(), "Two atoms have second angle involving 3");
    	assertTrue(list.contains(zm.getZAtom(6)) && list.contains(zm.getZAtom(7)));

    	// Three indexes: second angle between any two of the three centers
    	list = zm.findAllSecondAngles(0, 1, 2);
		assertEquals(0, list.size(), "No atom has second angle between two of {0,1,2}");
		list = zm.findAllSecondAngles(6, 1, 3);
    	assertEquals(1, list.size(), "One atom has second angle between two of {6,2,3}");
    	assertTrue(list.contains(zm.getZAtom(6)));
		list = zm.findAllSecondAngles(3, 1, 6);
    	assertEquals(1, list.size(), "One atom has second angle between two of {3,2,6}");
    	assertTrue(list.contains(zm.getZAtom(6)));
    }

//------------------------------------------------------------------------------

}
