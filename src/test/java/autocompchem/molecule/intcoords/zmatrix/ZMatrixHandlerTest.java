package autocompchem.molecule.intcoords.zmatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.molecule.intcoords.InternalCoord;

public class ZMatrixHandlerTest
{
//------------------------------------------------------------------------------

	/**
	 * Builds a specific test case meant to trigger the special case where two
	 * angles lead to inaccuracy in the conversion to IAtomContainer.
	 */
	public static ZMatrix getRuChainTestZMatrix()
	{
		AtomicInteger distCounter = new AtomicInteger(1);
		AtomicInteger angCounter = new AtomicInteger(1);
		AtomicInteger torCounter = new AtomicInteger(1);
		ZMatrix zm = new ZMatrix();

		zm.addZMatrixAtom(new ZMatrixAtom("Ru"));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 0,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 1.83291955,
				new ArrayList<>(Arrays.asList(1, 0)))));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 1, 0,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.95058492,
				new ArrayList<>(Arrays.asList(2, 1))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 119.21440685,
				new ArrayList<>(Arrays.asList(2, 1, 0)))));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 1, 0, 2,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 1.44631467,
				new ArrayList<>(Arrays.asList(3, 1))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 121.48643068,
				new ArrayList<>(Arrays.asList(3, 1, 0))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 119.29914479,
				new ArrayList<>(Arrays.asList(3, 1, 0, 2)), "-1")));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 3, 1, 0,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 1.39039622,
				new ArrayList<>(Arrays.asList(4, 3))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 122.06922453,
				new ArrayList<>(Arrays.asList(4, 3, 1))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 175.51613612,
				new ArrayList<>(Arrays.asList(4, 3, 1, 0)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 4, 3, 1,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.94987259,
				new ArrayList<>(Arrays.asList(5, 4))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 119.96241511,
				new ArrayList<>(Arrays.asList(5, 4, 3))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 1.15602099,
				new ArrayList<>(Arrays.asList(5, 4, 3, 1)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 4, 3, 5,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 1.39527601,
				new ArrayList<>(Arrays.asList(6, 4))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 120.06048514,
				new ArrayList<>(Arrays.asList(6, 4, 3))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 119.97708104,
				new ArrayList<>(Arrays.asList(6, 4, 3, 5)), "1")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 6, 4, 3,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.94957157,
				new ArrayList<>(Arrays.asList(7, 6))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 120.10535589,
				new ArrayList<>(Arrays.asList(7, 6, 4))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), -179.92912798,
				new ArrayList<>(Arrays.asList(7, 6, 4, 3)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 6, 4, 7,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 1.38611368,
				new ArrayList<>(Arrays.asList(8, 6))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 119.73843752,
				new ArrayList<>(Arrays.asList(8, 6, 4))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 120.15618558,
				new ArrayList<>(Arrays.asList(8, 6, 4, 7)), "-1")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 8, 6, 4,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.94997698,
				new ArrayList<>(Arrays.asList(9, 8))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 119.03138520,
				new ArrayList<>(Arrays.asList(9, 8, 6))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), -179.13932855,
				new ArrayList<>(Arrays.asList(9, 8, 6, 4)), "0")));
		zm.addZMatrixAtom(new ZMatrixAtom("C", 8, 6, 9,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 1.39823935,
				new ArrayList<>(Arrays.asList(10, 8))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 121.87318402,
				new ArrayList<>(Arrays.asList(10, 8, 6))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 119.09543078,
				new ArrayList<>(Arrays.asList(10, 8, 6, 9)), "-1")));
		zm.addZMatrixAtom(new ZMatrixAtom("H", 10, 8, 6,
			new InternalCoord(InternalCoordNaming.getSequentialDistName(distCounter), 0.95024008,
				new ArrayList<>(Arrays.asList(11, 10))),
			new InternalCoord(InternalCoordNaming.getSequentialAngName(angCounter), 120.88555354,
				new ArrayList<>(Arrays.asList(11, 10, 8))),
			new InternalCoord(InternalCoordNaming.getSequentialTorName(torCounter), 179.44366338,
				new ArrayList<>(Arrays.asList(11, 10, 8, 6)), "0")));
		return zm;
	}

//------------------------------------------------------------------------------

	@Test
	public void testConvertZMatrixToIAC() throws Throwable
	{
		ZMatrix zm = getRuChainTestZMatrix();

		IAtomContainer mol = ZMatrixHandler.convertZMatrixToIAC(zm, null);

		assertNotNull(mol);
		assertEquals(12, mol.getAtomCount());
	}

//------------------------------------------------------------------------------

}