package autocompchem.molecule.geometry;

import static org.junit.Assert.assertFalse;

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

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
/**
 * Unit Test for {@link GeometryAlignment}.
 * 
 * @author Marco Foscato
 */

public class GeometryAlignmentTest 
{
    
//------------------------------------------------------------------------------

	/** 
	 * Returns a {@link GeometryAlignment} meant for testing, i.e., it contains
	 * dummy values. It is not an actual alignement
	 * @throws CloneNotSupportedException 
	 */
	public static GeometryAlignment getGeometryAlignementA() 
			throws CloneNotSupportedException
    {
    	IAtomContainer mol = GeometryAlignerTest.getGeometryA();
    	IAtomContainer ref = mol.clone();
    	ref.removeAtom(7);
    	ref.removeAtom(6);
    	ref.removeAtom(5);
    	ref.removeAtom(3);
    	Map<IAtom, IAtom> mapping = new HashMap<IAtom, IAtom>();
    	mapping.put(ref.getAtom(0), mol.getAtom(0));
    	mapping.put(ref.getAtom(1), mol.getAtom(1));
    	mapping.put(ref.getAtom(2), mol.getAtom(2));
    	mapping.put(ref.getAtom(3), mol.getAtom(4));
    	
    	GeometryAlignment a = new GeometryAlignment(ref, mol, mapping);
        a.setRMSD(0.123); 
        a.setRMSDIM(0.456);
    	return a;
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testGetMappingIndexes() throws Exception
    {
    	GeometryAlignment alignmentA = getGeometryAlignementA();
    	Map<Integer, Integer> mapIdxs = alignmentA.getMappingIndexes();
    	
    	assertEquals(4, mapIdxs.keySet().size());
    	assertTrue(mapIdxs.keySet().contains(0));
    	assertEquals(0, mapIdxs.get(0));
    	assertTrue(mapIdxs.keySet().contains(1));
    	assertEquals(1, mapIdxs.get(1));
    	assertTrue(mapIdxs.keySet().contains(2));
    	assertEquals(2, mapIdxs.get(2));
    	assertTrue(mapIdxs.keySet().contains(3));
    	assertEquals(4, mapIdxs.get(3));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	GeometryAlignment alignmentA = getGeometryAlignementA();
    	GeometryAlignment alignmentB = getGeometryAlignementA();
    	
    	assertTrue(alignmentA.equals(alignmentA));
    	assertTrue(alignmentA.equals(alignmentB));
    	assertTrue(alignmentB.equals(alignmentA));
    	
    	alignmentB.setRMSD(-0.1);
    	assertFalse(alignmentA.equals(alignmentB));
    	
    	alignmentB = getGeometryAlignementA();
    	alignmentB.setRMSDIM(-0.1);
    	assertFalse(alignmentA.equals(alignmentB));
    	
    	alignmentB = getGeometryAlignementA();
    	alignmentB.getFirstIAC().getAtom(0).setPoint3d(new Point3d(99, 99, 99));
    	assertFalse(alignmentA.equals(alignmentB));
    }
    
//------------------------------------------------------------------------------

}
