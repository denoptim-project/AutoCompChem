package autocompchem.molecule.dummyobjects;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.atom.AtomUtils;

/**
 * Unit tests for {@link DummyObjectsHandler} public static methods
 * {@link DummyObjectsHandler#addDummyAtoms(IAtomContainer, IAtomContainer, boolean, boolean, boolean, List, String)}
 * and {@link DummyObjectsHandler#removeDummyAtoms(IAtomContainer, boolean, boolean, boolean, String)}.
 *
 * @author Marco Foscato
 */
public class DummyObjectsHandlerTest
{
    private static final IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    /**
     * Build a 3-atom linear molecule (angle 180° at middle atom) so that
     * addDummyAtoms with doLinearities=true can add a linearity-breaking dummy.
     */
    private static IAtomContainer makeLinearThreeAtomMol()
    {
        IAtomContainer mol = builder.newAtomContainer();
        IAtom a0 = builder.newAtom();
        a0.setSymbol("C");
        a0.setPoint3d(new Point3d(0.0, 0.0, 0.0));
        IAtom a1 = builder.newAtom();
        a1.setSymbol("N");
        a1.setPoint3d(new Point3d(1.0, 0.0, 0.0));
        IAtom a2 = builder.newAtom();
        a2.setSymbol("O");
        a2.setPoint3d(new Point3d(2.0, 0.0, 0.0));
        mol.addAtom(a0);
        mol.addAtom(a1);
        mol.addAtom(a2);
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(1, 2, IBond.Order.SINGLE);
        return mol;
    }

    /**
     * Build a simple 3-atom non-linear molecule so that no linearity dummy is added.
     */
    private static IAtomContainer makeBentThreeAtomMol()
    {
        IAtomContainer mol = builder.newAtomContainer();
        IAtom a0 = builder.newAtom();
        a0.setSymbol("C");
        a0.setPoint3d(new Point3d(0.0, 0.0, 0.0));
        IAtom a1 = builder.newAtom();
        a1.setSymbol("O");
        a1.setPoint3d(new Point3d(1.0, 0.0, 0.0));
        IAtom a2 = builder.newAtom();
        a2.setSymbol("H");
        a2.setPoint3d(new Point3d(1.0, 0.9, 0.0)); // ~120° angle
        mol.addAtom(a0);
        mol.addAtom(a1);
        mol.addAtom(a2);
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(1, 2, IBond.Order.SINGLE);
        return mol;
    }

    /** Count atoms that are ACC dummies (PseudoAtom with label "Du"). */
    private static int countAccDummies(IAtomContainer mol)
    {
        int n = 0;
        for (IAtom a : mol.atoms())
        {
            if (AtomUtils.isAccDummy(a))
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    @Test
    public void testAddDummyAtomsNoOpWhenAllDisabled()
    {
        IAtomContainer mol = makeLinearThreeAtomMol();
        int before = mol.getAtomCount();
        List<Integer> activeSrc = new ArrayList<>();
        DummyObjectsHandler.addDummyAtoms(mol, null, false, false, false, activeSrc, "Du");
        assertEquals(before, mol.getAtomCount(), "No dummies added when all flags false");
        assertEquals(0, countAccDummies(mol));
    }

    @Test
    public void testAddDummyAtomsLinearAddsDummy()
    {
        IAtomContainer mol = makeLinearThreeAtomMol();
        int before = mol.getAtomCount();
        List<Integer> activeSrc = new ArrayList<>();
        DummyObjectsHandler.addDummyAtoms(mol, null, true, false, false, activeSrc, "Du");
        assertTrue(mol.getAtomCount() > before, "At least one dummy added for linearity");
        assertTrue(countAccDummies(mol) >= 1, "At least one ACC dummy present");
    }

    @Test
    public void testAddDummyAtomsBentNoLinearDummy()
    {
        IAtomContainer mol = makeBentThreeAtomMol();
        int before = mol.getAtomCount();
        List<Integer> activeSrc = new ArrayList<>();
        DummyObjectsHandler.addDummyAtoms(mol, null, true, false, false, activeSrc, "Du");
        assertEquals(before, mol.getAtomCount(), "No dummy added on bent molecule for linearity-only");
        assertEquals(0, countAccDummies(mol));
    }

    @Test
    public void testRemoveDummyAtomsNoOpWhenNoDummies()
    {
        IAtomContainer mol = makeLinearThreeAtomMol();
        int before = mol.getAtomCount();
        DummyObjectsHandler.removeDummyAtoms(mol, true, false, false, "Du");
        assertEquals(before, mol.getAtomCount(), "Atom count unchanged when no dummies");
    }

    @Test
    public void testRemoveDummyAtomsRemovesLinearityDummy()
    {
        IAtomContainer mol = makeLinearThreeAtomMol();
        List<Integer> activeSrc = new ArrayList<>();
        DummyObjectsHandler.addDummyAtoms(mol, null, true, false, false, activeSrc, "Du");
        assertTrue(countAccDummies(mol) >= 1, "Dummy present after add");
        int withDummy = mol.getAtomCount();
        DummyObjectsHandler.removeDummyAtoms(mol, true, false, false, "Du");
        assertTrue(mol.getAtomCount() < withDummy, "At least one atom removed");
        assertEquals(0, countAccDummies(mol), "No ACC dummies left after remove");
    }

    @Test
    public void testAddThenRemoveDummyAtomsRestoresCount()
    {
        IAtomContainer mol = makeLinearThreeAtomMol();
        int originalCount = mol.getAtomCount();
        List<Integer> activeSrc = new ArrayList<>();
        DummyObjectsHandler.addDummyAtoms(mol, null, true, false, false, activeSrc, "Du");
        DummyObjectsHandler.removeDummyAtoms(mol, true, false, false, "Du");
        assertEquals(originalCount, mol.getAtomCount(), "Add then remove restores original atom count");
    }
}
