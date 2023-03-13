package autocompchem.molecule.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;


/**
 * Unit Test for connectivity table.
 * @author Marco Foscato
 */

public class ConnectivityTableTest 
{

    private IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromIAC() throws Exception
    {
        String[] elements = new String[]{"O", "C", "Mo", "P", "C", "N"};
        
        /*
         * Empty
         */
        IAtomContainer iac = chemBuilder.newAtomContainer();
        assertTrue(compareWithIAC(new ConnectivityTable(iac), iac));
        
        /*
         * With only atoms
         */
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            iac.addAtom(atom);
        }
        assertEquals(0, iac.getBondCount());
        assertTrue(compareWithIAC(new ConnectivityTable(iac), iac));
        
        /*
         *  O#C-Mo-P-C=N
         */
        for (int i=0; i<iac.getAtomCount(); i++)
        {
            if (i>0)
            {
            	switch (i)
            	{
            	case 5:
            		iac.addBond(i-1, i, IBond.Order.DOUBLE);
            		break;
            	case 1:
            		iac.addBond(i-1, i, IBond.Order.TRIPLE);
            		break;
            	default:
            		iac.addBond(i-1, i, IBond.Order.SINGLE);
            		break;
            	}
            }
        }
        assertEquals(5, iac.getBondCount());
        assertTrue(compareWithIAC(new ConnectivityTable(iac), iac));
        
        /*        ____
         *       /    \
         *  O#C-Mo-P-C=N
         */
        iac.addBond(2, 5, IBond.Order.SINGLE);
        assertEquals(6, iac.getBondCount());
        assertTrue(compareWithIAC(new ConnectivityTable(iac), iac));
        

        /*        ____
         *       /    \
         *  O#C-Mo-P C=N
         */
        iac.removeBond(iac.getAtom(3), iac.getAtom(4));
        assertEquals(5, iac.getBondCount());
        assertTrue(compareWithIAC(new ConnectivityTable(iac), iac));
        

        /*         ____
         *        /    \
         *  O#C  Mo-P C=N
         */
        iac.removeBond(iac.getAtom(1), iac.getAtom(2));
        assertEquals(4, iac.getBondCount());
        assertTrue(compareWithIAC(new ConnectivityTable(iac), iac));
    }

//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromIACSubset() throws Exception
    {
        String[] elements = new String[]{"O", "C", "Mo", "P", "C", "N"};
        IAtomContainer iac = chemBuilder.newAtomContainer();
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            iac.addAtom(atom);
        }
        for (int i=0; i<iac.getAtomCount(); i++)
        {
            if (i>0)
            {
            	switch (i)
            	{
            	case 5:
            		iac.addBond(i-1, i, IBond.Order.DOUBLE);
            		break;
            	case 1:
            		iac.addBond(i-1, i, IBond.Order.TRIPLE);
            		break;
            	default:
            		iac.addBond(i-1, i, IBond.Order.SINGLE);
            		break;
            	}
            }
        }
        iac.addBond(2, 5, IBond.Order.SINGLE);
        /*        ____
         *       /    \
         *  O#C-Mo-P-C=N
         *  0 1 2  3 4 5 
         */
        assertEquals(6, iac.getBondCount());
        Set<IAtom> subset = new HashSet<IAtom>();
        subset.add(iac.getAtom(2));
        subset.add(iac.getAtom(1));
        subset.add(iac.getAtom(4));
        subset.add(iac.getAtom(5));
        
        assertTrue(connectionsInCtExistInIAC(new ConnectivityTable(subset, iac),
        		iac));
        
        /*        ____
         *       /    \
         *  O#C-Mo-P C=N
         */
        iac.removeBond(iac.getAtom(3), iac.getAtom(4));
        assertEquals(5, iac.getBondCount());
        assertTrue(connectionsInCtExistInIAC(new ConnectivityTable(iac), iac));
        

        /*         ____
         *        /    \
         *  O#C  Mo-P C=N
         */
        iac.removeBond(iac.getAtom(1), iac.getAtom(2));
        assertEquals(4, iac.getBondCount());
        assertTrue(connectionsInCtExistInIAC(new ConnectivityTable(iac), iac));
    }
    
//------------------------------------------------------------------------------

    private boolean compareWithIAC(ConnectivityTable ct, IAtomContainer iac)
    {
    	for (IBond bnd : iac.bonds())
    	{
    		if (!ct.getNbrsId(bnd.getAtom(0).getIndex(), true).contains(
    				bnd.getAtom(1).getIndex()))
    			return false;

    		if (!ct.getNbrsId(bnd.getAtom(1).getIndex(), true).contains(
    				bnd.getAtom(0).getIndex()))
    			return false;
    	}
    	for (Integer srcId : ct.keySet())
    	{
    		IAtom srcAtm = iac.getAtom(srcId);
    		List<IAtom> nbrs = iac.getConnectedAtomsList(srcAtm);
    		assertEquals(nbrs.size(), ct.get(srcId).size());
    		for (Integer trgId : ct.get(srcId))
    			assertTrue(nbrs.contains(iac.getAtom(trgId)));
    	}
    	return true;
    }
    
//------------------------------------------------------------------------------

    private boolean connectionsInCtExistInIAC(ConnectivityTable ct, 
    		IAtomContainer iac)
    {
    	for (Integer srcId : ct.keySet())
    	{
    		IAtom srcAtm = iac.getAtom(srcId);
    		List<IAtom> nbrs = iac.getConnectedAtomsList(srcAtm);
    		for (Integer trgId : ct.get(srcId))
    			assertTrue(nbrs.contains(iac.getAtom(trgId)));
    	}
    	return true;
    }
    
//------------------------------------------------------------------------------

    public static ConnectivityTable getTestTable()
    {
    	ConnectivityTable ct = new ConnectivityTable();
    	ct.addNeighborningRelation(3, Arrays.asList(1,5,7,4));
    	ct.addNeighborningRelation(2, Arrays.asList(1,7));
    	return ct;
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	ConnectivityTable c1 = getTestTable();
    	ConnectivityTable c2 = getTestTable();
    	
    	assertTrue(c1.equals(c2));
    	assertTrue(c2.equals(c1));
    	assertTrue(c1.equals(c1));
    	assertFalse(c1.equals(null));
    	
    	c2.getNbrsId(2).add(22);
    	assertFalse(c1.equals(c2));
    	
    	c2 = getTestTable();
    	c2.addNeighborningRelation(2, Arrays.asList(22,33));
    	assertFalse(c1.equals(c2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJSONRoundTrip() throws Exception
    {
    	Gson reader = ACCJson.getReader();
    	Gson writer = ACCJson.getWriter();
    	
    	ConnectivityTable c1 = getTestTable();
    	
    	String json = writer.toJson(c1);
    	ConnectivityTable c2 = reader.fromJson(json, ConnectivityTable.class);
    	
    	assertEquals(c1, c2);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
    	ConnectivityTable c1 = getTestTable();
    	ConnectivityTable clone = c1.clone();
    	assertEquals(c1, clone);
    	assertFalse(c1==clone);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testAddNeighborningRelation() throws Exception
    {
    	ConnectivityTable ct = new ConnectivityTable();
    	ct.addNeighborningRelation(3, Arrays.asList(1,5,0,-4));
    	
    	assertTrue(ct.getNbrsId(3).contains(1));
    	assertTrue(ct.getNbrsId(3).contains(5));
    	assertTrue(ct.getNbrsId(3).contains(0));
    	assertTrue(ct.getNbrsId(3).contains(-4));
    	
    	assertTrue(ct.getNbrsId(1).contains(3));
    	assertTrue(ct.getNbrsId(5).contains(3));
    	assertTrue(ct.getNbrsId(0).contains(3));
    	assertTrue(ct.getNbrsId(-4).contains(3));
    	
    	ct.addNeighborningRelation(14, Arrays.asList(12,16,-14), false);
    	
    	assertTrue(ct.getNbrsId(13).contains(11));
    	assertTrue(ct.getNbrsId(13).contains(15));
    	assertTrue(ct.getNbrsId(13).contains(-15));
    	
    	assertTrue(ct.getNbrsId(11).contains(13));
    	assertTrue(ct.getNbrsId(15).contains(13));
    	assertTrue(ct.getNbrsId(-15).contains(13));
    	
    	ct.addNeighborningRelation(23, new int[]{21,25,20});
    	
    	assertTrue(ct.getNbrsId(23).contains(21));
    	assertTrue(ct.getNbrsId(23).contains(25));
    	assertTrue(ct.getNbrsId(23).contains(20));
    	
    	assertTrue(ct.getNbrsId(21).contains(23));
    	assertTrue(ct.getNbrsId(25).contains(23));
    	assertTrue(ct.getNbrsId(20).contains(23));
    }
    
//------------------------------------------------------------------------------

}
