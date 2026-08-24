package autocompchem.molecule.sorting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

/**
 * Unit Test for {@link MolecularSorter}.
 */
public class MolecularSorterTest
{
	private static IChemObjectBuilder chemBuilder =
			DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

	@Test
	public void testReverse()
	{
		IAtomContainer molA = chemBuilder.newAtomContainer();
		molA.addAtom(new Atom("H"));
		IAtomContainer molB = chemBuilder.newAtomContainer();
		molB.addAtom(new Atom("C"));
		IAtomContainer molC = chemBuilder.newAtomContainer();
		molC.addAtom(new Atom("O"));

		List<IAtomContainer> input = new ArrayList<IAtomContainer>();
		input.add(molA);
		input.add(molB);
		input.add(molC);

		List<IAtomContainer> reversed = MolecularSorter.reverse(input);

		assertEquals(3, reversed.size());
		assertSame(molC, reversed.get(0));
		assertSame(molB, reversed.get(1));
		assertSame(molA, reversed.get(2));
	}

//------------------------------------------------------------------------------

}
