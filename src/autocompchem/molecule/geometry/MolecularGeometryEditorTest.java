package autocompchem.molecule.geometry;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.geometry.DistanceMatrix;

/**
 * Unit Test for MolecularGeometryEditor methods
 * 
 * @author Marco Foscato
 */

public class MolecularGeometryEditorTest 
{
	
//------------------------------------------------------------------------------

	@Test
	public void testoptimizeScalingFactors() throws Exception
    {
    	IAtomContainer mol = new AtomContainer();
    	mol.addAtom(new Atom("C",new Point3d(0,0,0.0)));
    	mol.addAtom(new Atom("O",new Point3d(5.0,0,0)));
    	mol.addAtom(new Atom("N",new Point3d(10.0,0,0)));
    	
		ArrayList<Point3d> move = new ArrayList<Point3d>();
		move.add(new Point3d(0,0,0));
		move.add(new Point3d(1.0,0,0));
		move.add(new Point3d(0,0,0));
    	

		ArrayList<Double> sf = MolecularGeometryEditor.optimizeScalingFactors(mol, 
				move,15,0.01,0.001,0);
		
		assertEquals(16,sf.size(), "Size of scaling factors list");
    }

//------------------------------------------------------------------------------

}
