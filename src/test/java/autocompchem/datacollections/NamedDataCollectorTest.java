package autocompchem.datacollections;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;


/**
 * Unit Test for NamedData class 
 * 
 * @author Marco Foscato
 */

public class NamedDataCollectorTest 
{

//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {  
    	NamedDataCollector ndc = new NamedDataCollector();
    	ndc.putNamedData(new NamedData("D", 1.2));
    	
    	IAtomContainer mol = new AtomContainer();
    	Atom atm1 = new Atom("C");
    	Atom atm2 = new Atom("O");
    	mol.addAtom(atm1);
    	mol.addAtom(atm2);
    	ndc.putNamedData(new NamedData("IAC", mol));
    	
    	ListOfDoubles ld = new ListOfDoubles();
    	ld.add(0.1);
    	ld.add(0.2);
    	ndc.putNamedData(new NamedData("LD", ld));
    	
    	NormalMode nm1 = new NormalMode();
    	nm1.append(new Point3d(1,1,1));
    	nm1.append(new Point3d(1.1,1.1,1.1));
    	NormalMode nm2 = new NormalMode();
    	nm2.append(new Point3d(2,2,2));
    	nm2.append(new Point3d(2.1,2.1,2.1));
    	NormalModeSet nms = new NormalModeSet();
    	nms.add(nm1);
    	nms.add(nm2);
    	ndc.putNamedData(new NamedData("NMS", nms));
    	
    	NamedDataCollector cndc = ndc.clone();

    	// edit original fields
    	ndc.putNamedData(new NamedData("D", 2.6));
    	Atom atm3 = new Atom("H");
    	mol.addAtom(atm3);
    	ld.add(2.2);
    	nms.setComponent(2, 0, 0, 3.1);
    	
    	
    	assertTrue((1.2-((Double) cndc.getNamedData("D").getValue()))<0.0001,
    			"Checking cloned NamedData with Double");
    	
    	assertEquals(2, 
    			((IAtomContainer) cndc.getNamedData("IAC").getValue()).getAtomCount(),
    			"Checking cloned IAtomCOntainer");
    	assertEquals(3, 
    			((IAtomContainer) ndc.getNamedData("IAC").getValue()).getAtomCount(),
    			"Checking original IAtomCOntainer");
    	
    	assertEquals(2, 
    			((ListOfDoubles) cndc.getNamedData("LD").getValue()).size(),
    			"Checking cloned list of doubles");
    	assertEquals(3, 
    			((ListOfDoubles) ndc.getNamedData("LD").getValue()).size(),
    			"Checking original list of doubles");
    	assertEquals(3, 
    			((NormalModeSet) ndc.getNamedData("NMS").getValue()).size(),
    			"Checking original list of modess");
    	assertEquals(2, 
    			((NormalModeSet) cndc.getNamedData("NMS").getValue()).size(),
    			"Checking cloned list of modess");
    }

//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTripNamedDataCollector() throws Exception
    {
    	NamedDataCollector ndc = new NamedDataCollector();
    	ndc.putNamedData(new NamedData("x", "hello"));
    	ndc.putNamedData(new NamedData("y", 42));

    	Gson w = ACCJson.getWriter();
    	Gson r = ACCJson.getReader();
    	String json = w.toJson(ndc);
    	NamedDataCollector back = r.fromJson(json, NamedDataCollector.class);
    	assertEquals(ndc, back);
    }

//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTripDiskSpillingNamedDataCollector(@TempDir File spill)
    		throws Exception
    {
    	DiskSpillingNamedDataCollector ds =
    			new DiskSpillingNamedDataCollector(spill);
    	ds.putNamedData(new NamedData("spilled", "data"));

    	Gson w = ACCJson.getWriter();
    	Gson r = ACCJson.getReader();
    	String json = w.toJson(ds);
    	NamedDataCollector back = r.fromJson(json, NamedDataCollector.class);
    	assertEquals(1, back.size());
    	assertEquals("data", back.getNamedData("spilled").getValue());
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetNestedDataValue_emptyPathReturnsRoot() throws Exception
    {
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("k", 1));
    	assertSame(root, root.getNestedDataValue(new String[0]));
    }

    @Test
    public void testGetNestedDataValue_singleLeaf() throws Exception
    {
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("x", "leaf"));
    	assertEquals("leaf", root.getNestedDataValue(new String[] { "x" }));
    }

    @Test
    public void testGetNestedDataValue_nestedCollector() throws Exception
    {
    	NamedDataCollector inner = new NamedDataCollector();
    	inner.putNamedData(new NamedData("innerKey", 99));
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("outer", inner));
    	assertEquals(99,
    			root.getNestedDataValue(new String[] { "outer", "innerKey" }));
    }

    @Test
    public void testGetNestedDataValue_namedDataUnwrapByReference() throws Exception
    {
    	NamedData innerNd = new NamedData("innerRef", "payload");
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("wrap", innerNd));
    	assertEquals("payload",
    			root.getNestedDataValue(new String[] { "wrap", "innerRef" }));
    }

    @Test
    public void testGetNestedDataValue_namedDataRefMismatchReturnsNull()
    		throws Exception
    {
    	NamedData innerNd = new NamedData("innerRef", "payload");
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("wrap", innerNd));
    	assertNull(root.getNestedDataValue(new String[] { "wrap", "wrongRef" }));
    }

    @Test
    public void testGetNestedDataValue_listIndexFirstLast() throws Exception
    {
    	List<String> lst = new ArrayList<>(Arrays.asList("a", "b", "c"));
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("L", lst));
    	assertEquals("a", root.getNestedDataValue(new String[] { "L", "0" }));
    	assertEquals("c", root.getNestedDataValue(new String[] { "L", "LAST" }));
    	assertEquals("a", root.getNestedDataValue(new String[] { "L", "first" }));
    }

    @Test
    public void testGetNestedDataValue_mapStringAndIntegerKeys() throws Exception
    {
    	Map<String, Integer> strMap = new HashMap<>();
    	strMap.put("alpha", 10);
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("M", strMap));
    	assertEquals(10,
    			root.getNestedDataValue(new String[] { "M", "alpha" }));

    	Map<Integer, String> intMap = new HashMap<>();
    	intMap.put(2, "two");
    	root.putNamedData(new NamedData("Mi", intMap));
    	assertEquals("two",
    			root.getNestedDataValue(new String[] { "Mi", "2" }));
    }

    @Test
    public void testGetNestedDataValue_collectorFirstLast() throws Exception
    {
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("2", "second"));
    	root.putNamedData(new NamedData("10", "tenth"));
    	assertEquals("second",
    			root.getNestedDataValue(new String[] { "FIRST" }));
    	assertEquals("tenth",
    			root.getNestedDataValue(new String[] { "last" }));
    }

    @Test
    public void testGetNestedDataValue_atomContainerSetIndex() throws Exception
    {
    	AtomContainer first = new AtomContainer();
    	first.addAtom(new Atom("C"));
    	AtomContainer second = new AtomContainer();
    	second.addAtom(new Atom("O"));
    	AtomContainerSet acs = new AtomContainerSet();
    	acs.addAtomContainer(first);
    	acs.addAtomContainer(second);
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("geoms", acs));
    	Object at0 = root.getNestedDataValue(new String[] { "geoms", "0" });
    	assertInstanceOf(IAtomContainer.class, at0);
    	assertEquals(1, ((IAtomContainer) at0).getAtomCount());
    	Object atLast = root.getNestedDataValue(new String[] { "geoms", "LAST" });
    	assertEquals(1, ((IAtomContainer) atLast).getAtomCount());
    }

    @Test
    public void testGetNestedDataValue_missingKeyThrowsDataFetchingException()
    {
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("only", 1));
    	assertThrows(DataFetchingException.class,
    			() -> root.getNestedDataValue(new String[] { "missing" }));
    }

    @Test
    public void testGetNestedDataValue_listBadTokenThrows() throws Exception
    {
    	List<String> lst = new ArrayList<>(List.of("x"));
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("L", lst));
    	assertThrows(DataFetchingException.class,
    			() -> root.getNestedDataValue(new String[] { "L", "notAnIndex" }));
    }

    @Test
    public void testGetNestedDataValue_nullValueThenNavigateThrows()
    		throws Exception
    {
    	NamedDataCollector root = new NamedDataCollector();
    	root.putNamedData(new NamedData("n", null));
    	assertThrows(DataFetchingException.class,
    			() -> root.getNestedDataValue(new String[] { "n", "more" }));
    }

//------------------------------------------------------------------------------

}
