package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.atom.AtomUtils;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.molecule.intcoords.InternalCoord;

/**
 * Unit Test for MolecularUtils
 * 
 * @author Marco Foscato
 */

public class MolecularUtilsTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

	@Test
	public void testMakeSimpleCopyWithAtomTags() throws Exception
	{
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("Ru"));
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new PseudoAtom("Xx")); 
    	
    	IAtomContainer tagged = MolecularUtils.makeSimpleCopyWithAtomTags(mol);
    	
    	assertEquals(mol.getAtomCount(),tagged.getAtomCount());
    	for (int iAtm=0; iAtm<mol.getAtomCount(); iAtm++)
    	{
    		IAtom atm = tagged.getAtom(iAtm);
    		IAtom origAtm = mol.getAtom(iAtm);
    		// NB: so far there is only one format for atom tags, so we can
    		// hard-code the expected tag.
    		String expectedTag = AtomUtils.getSymbolOrLabel(origAtm)+(iAtm+1);
    		assertEquals(expectedTag, AtomUtils.getSymbolOrLabel(atm));
    	}
	}
	
//------------------------------------------------------------------------------
	
	@Test
	public void testMakeSimpleCopyWithAtomTags2() throws Exception
	{
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("Ru"));
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new PseudoAtom("Xx")); 
    	
    	List<String> myLabels = new ArrayList<String>(Arrays.asList("F-0", 
    			"CHH", "__", "@", "xX", ""));
    	
    	IAtomContainer tagged = MolecularUtils.makeSimpleCopyWithAtomTags(mol,
    			myLabels);
    	
    	assertEquals(mol.getAtomCount(), tagged.getAtomCount());
    	for (int iAtm=0; iAtm<mol.getAtomCount(); iAtm++)
    	{
    		IAtom atm = tagged.getAtom(iAtm);
    		assertEquals(myLabels.get(iAtm), AtomUtils.getSymbolOrLabel(atm));
    	}
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testElementalSymbols() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("H"));
    	mol.addAtom(new Atom("C"));
    	mol.addAtom(new Atom("O"));
    	mol.addAtom(new Atom("Ru"));
    	//NB: with cdk version > 1.5(?) it is not possible to have custom 
    	// elemental symbols like "Xx" of "Du"
    	mol.addAtom(new PseudoAtom());
    	mol.addAtom(new PseudoAtom("Du"));
    	mol.addAtom(new PseudoAtom("Xx"));    	
    	
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("H"),"Does have H");
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("C"),"Does have C");
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("Ru"),"Does have Ru");
        assertTrue(MolecularUtils.getElementalSymbols(mol, 
        		false).contains("R"),"Does have R");
        assertFalse(MolecularUtils.getElementalSymbols(mol, 
        		true).contains("R"),"Does not have R");
    }
//------------------------------------------------------------------------------

	@Test
	public void testMinInterelementalBondDistance() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("C",new Point3d(0,0,0.0)));
    	mol.addAtom(new Atom("C",new Point3d(1.0,0,0)));
    	mol.addAtom(new Atom("C",new Point3d(2.0,0,0)));
    	mol.addAtom(new Atom("Cl",new Point3d(0,-2.0,0)));
    	mol.addAtom(new Atom("H",new Point3d(0,0,1.1)));
    	mol.addAtom(new Atom("H",new Point3d(0,0,1.2)));
		
    	double trsh = 0.00001;
        assertTrue(Math.abs(MolecularUtils.getMinInterelementalDistance(mol, 
        	"C","H") - 1.1) < trsh,"Min Interatomic distance H-C");
        assertTrue(Math.abs(MolecularUtils.getMinInterelementalDistance(mol,
        		"C","Cl") - 2.0) < trsh,"Min Interatomic distance H-C");
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testInteratormicDistanceMatrix() throws Exception
    {
		IAtomContainer mol = chemBuilder.newAtomContainer();
    	mol.addAtom(new Atom("C",new Point3d(0,0,0.0)));
    	mol.addAtom(new Atom("C",new Point3d(0,3.0,0)));
    	mol.addAtom(new Atom("C",new Point3d(4.0,0,0)));
    	
    	DistanceMatrix dm = MolecularUtils.getInteratormicDistanceMatrix(mol);
    	
    	double trsh = 0.00001;
        assertTrue(Math.abs(dm.get(0, 1) - 3.0) < trsh,"Distance 1-2");
        assertTrue(Math.abs(dm.get(0, 2) - 4.0) < trsh,"Distance 1-2");
        assertTrue(Math.abs(dm.get(1, 2) - 5.0) < trsh,"Distance 1-2");
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateCentroid_emptyArrayThrows()
    {
    	IAtom[] atoms = new IAtom[0];
    	IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
    			() -> MolecularUtils.calculateCentroid(atoms));
    	assertTrue(e.getMessage().contains("empty array"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateCentroid_singleAtom()
    {
    	IAtom[] atoms = new IAtom[] {
    			new Atom("C", new Point3d(1.0, 2.0, 3.0))
    	};
    	Point3d centroid = MolecularUtils.calculateCentroid(atoms);
    	double tol = 1e-10;
    	assertEquals(1.0, centroid.x, tol);
    	assertEquals(2.0, centroid.y, tol);
    	assertEquals(3.0, centroid.z, tol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateCentroid_twoAtoms()
    {
    	IAtom[] atoms = new IAtom[] {
    			new Atom("C", new Point3d(0.0, 0.0, 0.0)),
    			new Atom("C", new Point3d(2.0, 4.0, 6.0))
    	};
    	Point3d centroid = MolecularUtils.calculateCentroid(atoms);
    	double tol = 1e-10;
    	assertEquals(1.0, centroid.x, tol);
    	assertEquals(2.0, centroid.y, tol);
    	assertEquals(3.0, centroid.z, tol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateCentroid_threeAtoms()
    {
    	IAtom[] atoms = new IAtom[] {
    			new Atom("C", new Point3d(1.0, 0.0, 0.0)),
    			new Atom("C", new Point3d(0.0, 3.0, 0.0)),
    			new Atom("C", new Point3d(0.0, 0.0, 6.0))
    	};
    	Point3d centroid = MolecularUtils.calculateCentroid(atoms);
    	double tol = 1e-10;
    	assertEquals(1.0 / 3.0, centroid.x, tol);
    	assertEquals(1.0, centroid.y, tol);
    	assertEquals(2.0, centroid.z, tol);
    }

//------------------------------------------------------------------------------

    private static InternalCoord icDist(double value, int refA) {
        return new InternalCoord("d", value, new ArrayList<>(Arrays.asList(-1, refA)));
    }
    private static InternalCoord icAngle(double degrees, int refA, int refB) {
        return new InternalCoord("a", degrees, new ArrayList<>(Arrays.asList(-1, refA, refB)));
    }
    private static InternalCoord icDihedral(double degrees, int refA, int refB, int refC) {
        return new InternalCoord("t", degrees, new ArrayList<>(Arrays.asList(-1, refA, refB, refC)));
    }

    @Test
    public void testCalculateAtomPosition_nullInternalCoordsThrows()
    {
        IAtom[] ref = new IAtom[] { new Atom("C", new Point3d(0, 0, 0)) };
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MolecularUtils.calculateAtomPosition(ref, null));
        assertTrue(e.getMessage().contains("internal coordinate"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_emptyInternalCoordsThrows()
    {
        IAtom[] ref = new IAtom[] { new Atom("C", new Point3d(0, 0, 0)) };
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MolecularUtils.calculateAtomPosition(ref, new InternalCoord[0]));
        assertTrue(e.getMessage().contains("At least one"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_distanceOnly_fromOrigin()
    {
        IAtom[] ref = new IAtom[] { new Atom("C", new Point3d(0, 0, 0)) };
        InternalCoord[] ics = new InternalCoord[] { icDist(1.0, 0) };
        Point3d pos = MolecularUtils.calculateAtomPosition(ref, ics);
        double tol = 1e-10;
        assertEquals(1.0, pos.x, tol);
        assertEquals(0.0, pos.y, tol);
        assertEquals(0.0, pos.z, tol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_distanceOnly_offsetAnchor()
    {
        IAtom[] ref = new IAtom[] { new Atom("C", new Point3d(1.0, 2.0, 3.0)) };
        InternalCoord[] ics = new InternalCoord[] { icDist(2.0, 0) };
        Point3d pos = MolecularUtils.calculateAtomPosition(ref, ics);
        double tol = 1e-10;
        assertEquals(3.0, pos.x, tol);
        assertEquals(2.0, pos.y, tol);
        assertEquals(3.0, pos.z, tol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_distanceAndAngle_alongBond()
    {
        IAtom[] ref = new IAtom[] {
                new Atom("C", new Point3d(0, 0, 0)),
                new Atom("C", new Point3d(1, 0, 0))
        };
        InternalCoord[] ics = new InternalCoord[] {
                icDist(1.0, 0),
                icAngle(0.0, 0, 1)
        };
        Point3d pos = MolecularUtils.calculateAtomPosition(ref, ics);
        double tol = 1e-10;
        assertEquals(1.0, pos.x, tol);
        assertEquals(0.0, pos.y, tol);
        assertEquals(0.0, pos.z, tol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_distanceAndAngle_90deg()
    {
        IAtom[] ref = new IAtom[] {
                new Atom("C", new Point3d(0, 0, 0)),
                new Atom("C", new Point3d(1, 0, 0))
        };
        InternalCoord[] ics = new InternalCoord[] {
                icDist(1.0, 0),
                icAngle(90.0, 0, 1)
        };
        Point3d pos = MolecularUtils.calculateAtomPosition(ref, ics);
        IAtom newAtm = new Atom("H", pos);
        double dist = MolecularUtils.calculateInteratomicDistance(ref[0], newAtm);
        double angleDeg = MolecularUtils.calculateBondAngle(newAtm, ref[0], ref[1]);
        double tol = 1e-8;
        assertEquals(1.0, dist, tol);
        assertEquals(90.0, angleDeg, tol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_distanceAngleDihedral_roundTrip()
    {
        IAtom[] ref = new IAtom[] {
                new Atom("C", new Point3d(0, 0, 0)),
                new Atom("C", new Point3d(1, 0, 0)),
                new Atom("C", new Point3d(0.5, 1, 0))
        };
        InternalCoord[] ics = new InternalCoord[] {
                icDist(1.2, 0),
                icAngle(110.0, 0, 1),
                icDihedral(30.0, 0, 1, 2)
        };
        Point3d pos = MolecularUtils.calculateAtomPosition(ref, ics);
        IAtom newAtm = new Atom("H", pos);
        double dist = MolecularUtils.calculateInteratomicDistance(ref[0], newAtm);
        double angleDeg = MolecularUtils.calculateBondAngle(newAtm, ref[0], ref[1]);
        double dihedralDeg = MolecularUtils.calculateTorsionAngle(newAtm, ref[0], ref[1], ref[2]);
        double tol = 1e-6;
        assertEquals(1.2, dist, tol);
        assertEquals(110.0, angleDeg, tol);
        assertEquals(30.0, Math.abs(dihedralDeg), 1.0);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_distanceICMissingIdsThrows()
    {
        IAtom[] ref = new IAtom[] { new Atom("C", new Point3d(0, 0, 0)) };
        InternalCoord distOnly = new InternalCoord("d", 1.0, new ArrayList<>(Arrays.asList(-1)));
        InternalCoord[] ics = new InternalCoord[] { distOnly };
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MolecularUtils.calculateAtomPosition(ref, ics));
        assertTrue(e.getMessage().contains("Distance internal coordinate"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_referenceIndexOutOfRangeThrows()
    {
        IAtom[] ref = new IAtom[] { new Atom("C", new Point3d(0, 0, 0)) };
        InternalCoord[] ics = new InternalCoord[] { icDist(1.0, 1) };
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MolecularUtils.calculateAtomPosition(ref, ics));
        assertTrue(e.getMessage().contains("out of range"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_coincidentAngleRefsThrows()
    {
        IAtom[] ref = new IAtom[] {
                new Atom("C", new Point3d(0, 0, 0)),
                new Atom("C", new Point3d(0, 0, 0))
        };
        InternalCoord[] ics = new InternalCoord[] {
                icDist(1.0, 0),
                icAngle(90.0, 0, 1)
        };
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MolecularUtils.calculateAtomPosition(ref, ics));
        assertTrue(e.getMessage().contains("coincident"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateAtomPosition_collinearDihedralRefsThrows()
    {
        IAtom[] ref = new IAtom[] {
                new Atom("C", new Point3d(0, 0, 0)),
                new Atom("C", new Point3d(1, 0, 0)),
                new Atom("C", new Point3d(2, 0, 0))
        };
        InternalCoord[] ics = new InternalCoord[] {
                icDist(1.0, 0),
                icAngle(90.0, 0, 1),
                icDihedral(0.0, 0, 1, 2)
        };
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MolecularUtils.calculateAtomPosition(ref, ics));
        assertTrue(e.getMessage().contains("collinear"));
    }

//------------------------------------------------------------------------------

}
