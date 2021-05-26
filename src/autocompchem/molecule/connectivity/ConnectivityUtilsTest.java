package autocompchem.molecule.connectivity;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;


/**
 * Unit Test for connectivity utils.
 * @author Marco Foscato
 */

public class ConnectivityUtilsTest 
{

    private IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
    @Test
    public void testCompareBondDistancesWithReference() throws Exception
    {
        String[] elements = new String[]{"C", "O", "N", "P", "P", "H"};
        
        IAtomContainer ref = chemBuilder.newAtomContainer();
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(new Point3d(0.0,0.0, new Double(i)));
            ref.addAtom(atom);
            if (i>0)
                ref.addBond(i-1, i, IBond.Order.SINGLE);
        }
        
        IAtomContainer mol1 = chemBuilder.newAtomContainer();
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(new Point3d(0.0,0.0, new Double(i)));
            mol1.addAtom(atom);
            if (i>0)
                mol1.addBond(i-1, i, IBond.Order.SINGLE);
        }
        
        IAtomContainer mol2 = chemBuilder.newAtomContainer();
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(new Point3d(0.0,0.0, new Double(i*2)));
            mol2.addAtom(atom);
            if (i>0)
                mol2.addBond(i-1, i, IBond.Order.SINGLE);
        }
        
        assertTrue(ConnectivityUtils.compareBondDistancesWithReference(mol1, 
        		ref, 0.01, 0));
        
        assertFalse(ConnectivityUtils.compareBondDistancesWithReference(mol2, 
        		ref, 0.98, 0));
    }
}
