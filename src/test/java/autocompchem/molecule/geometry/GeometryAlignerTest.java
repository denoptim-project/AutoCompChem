package autocompchem.molecule.geometry;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.io.IOtools;
import autocompchem.utils.NumberUtils;
import uk.ac.ebi.beam.Bond;

/**
 * Unit Test for geometry aligner.
 * 
 * @author Marco Foscato
 */

public class GeometryAlignerTest 
{
	
    private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
    
 //------------------------------------------------------------------------------

    /**
     * Produces an atom container with an hard-coded geometry useful for tests.
     */
	public static IAtomContainer getGeometryNoBonds()
    {
    	IAtomContainer mol = chemBuilder.newAtomContainer();
        mol.addAtom(new Atom("C",new Point3d(-0.12151,0.90985,-0.07717)));
        mol.addAtom(new Atom("O",new Point3d(0.48338,-0.36523,0.17760)));
        mol.addAtom(new Atom("C",new Point3d(-0.92388,-0.35864,-0.08249)));
        mol.addAtom(new Atom("O",new Point3d(-1.73388,-0.59432,1.06962)));
        mol.addAtom(new Atom("Cl",new Point3d(-1.47388,-1.00500,-1.61186)));
        mol.addAtom(new Atom("H",new Point3d(0.10083,1.38908,-1.02098)));
        mol.addAtom(new Atom("H",new Point3d(-0.24612,1.58476,0.75786)));
        mol.addAtom(new Atom("H",new Point3d(-1.26585,-1.22092,1.64563)));
    	return mol;
    }
	
//------------------------------------------------------------------------------

    /**
     * Produces an atom container with an hard-coded geometry useful for tests.
     */
	public static IAtomContainer getGeometryA()
    {
    	IAtomContainer mol = chemBuilder.newAtomContainer();
        mol.addAtom(new Atom("C",new Point3d(-0.12151,0.90985,-0.07717)));
        mol.addAtom(new Atom("O",new Point3d(0.48338,-0.36523,0.17760)));
        mol.addAtom(new Atom("C",new Point3d(-0.92388,-0.35864,-0.08249)));
        mol.addAtom(new Atom("O",new Point3d(-1.73388,-0.59432,1.06962)));
        mol.addAtom(new Atom("Cl",new Point3d(-1.47388,-1.00500,-1.61186)));
        mol.addAtom(new Atom("H",new Point3d(0.10083,1.38908,-1.02098)));
        mol.addAtom(new Atom("H",new Point3d(-0.24612,1.58476,0.75786)));
        mol.addAtom(new Atom("H",new Point3d(-1.26585,-1.22092,1.64563)));
        mol.addBond(0,1,IBond.Order.SINGLE);
        mol.addBond(0,2,IBond.Order.SINGLE);
        mol.addBond(0,5,IBond.Order.SINGLE);
        mol.addBond(0,6,IBond.Order.SINGLE);
        mol.addBond(1,2,IBond.Order.SINGLE);
        mol.addBond(2,3,IBond.Order.SINGLE);
        mol.addBond(2,4,IBond.Order.SINGLE);
        mol.addBond(3,7,IBond.Order.SINGLE);
    	return mol;
    }
	
//------------------------------------------------------------------------------

    /**
     * Produces an atom container with an hard-coded geometry useful for tests.
     */
	public static IAtomContainer getGeometryB()
    {
    	IAtomContainer mol = chemBuilder.newAtomContainer();
        mol.addAtom(new Atom("C",new Point3d(-0.09590,1.03060,-0.08600)));
        mol.addAtom(new Atom("O",new Point3d(0.48340,-0.36520,0.17760)));
        mol.addAtom(new Atom("C",new Point3d(-1.02220,-0.35820,-0.10070)));
        mol.addAtom(new Atom("O",new Point3d(-1.83220,-0.59390,1.05140)));
        mol.addAtom(new Atom("Cl",new Point3d(-1.57220,-1.00450,-1.63000)));
        mol.addAtom(new Atom("Cl",new Point3d(0.25800,1.70370,-1.41230)));
        mol.addAtom(new Atom("O",new Point3d(-0.22190,1.90680,1.00840)));
        mol.addAtom(new Atom("H",new Point3d(-1.71590,-1.52040,1.31880)));
        mol.addAtom(new Atom("H",new Point3d(-0.44390,1.40540,1.79640)));
        mol.addBond(0,1,IBond.Order.SINGLE);
        mol.addBond(0,5,IBond.Order.SINGLE);
        mol.addBond(0,6,IBond.Order.SINGLE);
        mol.addBond(1,2,IBond.Order.SINGLE);
        mol.addBond(2,3,IBond.Order.SINGLE);
        mol.addBond(2,4,IBond.Order.SINGLE);
        mol.addBond(2,0,IBond.Order.SINGLE);
        mol.addBond(3,7,IBond.Order.SINGLE);
        mol.addBond(8,6,IBond.Order.SINGLE);
    	return mol;
    } 
	
//------------------------------------------------------------------------------

    /**
     * Produces an atom container with an hard-coded geometry useful as a query
     * substructure to be searches in the atom container generated by
     * {@link #getGeometryA()} and 
     * {@link #getGeometryB()}
     */
	public static IAtomContainer getGeometryQuery()
    {
    	IAtomContainer mol = chemBuilder.newAtomContainer();
        mol.addAtom(new Atom("O",new Point3d(0.48340,-0.36520,0.17760)));
        mol.addAtom(new Atom("C",new Point3d(-1.02220,-0.35820,-0.10070)));
        mol.addAtom(new Atom("O",new Point3d(-1.83220,-0.59390,1.05140)));
        mol.addAtom(new Atom("Cl",new Point3d(-1.57220,-1.00450,-1.63000)));
        mol.addBond(0,1,IBond.Order.SINGLE);
        mol.addBond(1,2,IBond.Order.SINGLE);
        mol.addBond(1,3,IBond.Order.SINGLE);
    	return mol;
    }
	
//------------------------------------------------------------------------------

    @Test
	public void testAlignGeometries() throws Exception
    {
    	GeometryAlignment result = null;
    
    	// No suitable alignment found
    	boolean thrown = false;
    	try {
    		result = GeometryAligner.alignGeometries(
    				 getGeometryA(), getGeometryNoBonds());
    	} catch (IllegalArgumentException iae) {
    		thrown = true;
    	}
    	assertTrue(thrown);
   
    	// Cannot align structure to reference larger than structure
    	thrown = false;
    	try {
    		result = GeometryAligner.alignGeometries(
    				 getGeometryA(), getGeometryQuery());
    	} catch (IllegalArgumentException iae) {
    		thrown = true;
    	}
    	assertTrue(thrown);
    	
    	// Align molecule to itself
    	result = GeometryAligner.alignGeometries(
    			getGeometryA(), getGeometryA());
    	assertTrue(NumberUtils.closeEnough(0.0, result.getRMSD()));
    	assertTrue(NumberUtils.closeEnough(0.0, result.getRMSDIM()));
    			
    	// Align molecule to substructure (NB: the opposite is not possible!)
    	result = GeometryAligner.alignGeometries(
    			getGeometryQuery(), getGeometryB());
    	assertTrue(NumberUtils.closeEnough(0.0, result.getRMSD()));
    	assertTrue(NumberUtils.closeEnough(0.0, result.getRMSDIM()));
    	
    	result = GeometryAligner.alignGeometries(
    			getGeometryQuery(), getGeometryA());
    	assertTrue(NumberUtils.closeEnough(0.043, result.getRMSD(), 0.001));
    	assertTrue(NumberUtils.closeEnough(0.063, result.getRMSDIM(), 0.001));
    }
	
//------------------------------------------------------------------------------

}