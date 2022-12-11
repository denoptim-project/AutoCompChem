package autocompchem.modeling.basisset;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


public class BasisSetTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testHasCenter() throws Exception
    {
    	BasisSet bs = new BasisSet();
    	bs.addCenterSpecBSet(new CenterBasisSet(20, "Ca"));
    	bs.addCenterSpecBSet(new CenterBasisSet(7, "C"));
    	bs.addCenterSpecBSet(new CenterBasisSet(12, "Bla"));
    	bs.addCenterSpecBSet(new CenterBasisSet(4, "h"));
    	bs.addCenterSpecBSet(new CenterBasisSet("Ca"));
    	bs.addCenterSpecBSet(new CenterBasisSet("C"));
    	bs.addCenterSpecBSet(new CenterBasisSet("Bla"));
    	bs.addCenterSpecBSet(new CenterBasisSet("h"));

    	assertTrue(bs.hasCenter(20, "ca"));
    	assertTrue(bs.hasCenter(7, "c"));
    	assertTrue(bs.hasCenter(12, "bla"));
    	assertTrue(bs.hasCenter(4, "H"));
    	assertFalse(bs.hasCenter(5, "Ca"));
    	assertFalse(bs.hasCenter(20, "C"));
    	assertFalse(bs.hasCenter(13, "N"));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testHasElement() throws Exception
    {
    	BasisSet bs = new BasisSet();
    	bs.addCenterSpecBSet(new CenterBasisSet("Ca"));
    	bs.addCenterSpecBSet(new CenterBasisSet("C"));
    	bs.addCenterSpecBSet(new CenterBasisSet("Bla"));
    	bs.addCenterSpecBSet(new CenterBasisSet("h"));
    	bs.addCenterSpecBSet(new CenterBasisSet(12, "B"));
    	bs.addCenterSpecBSet(new CenterBasisSet(4, "N"));

    	assertTrue(bs.hasElement("cA"));
    	assertTrue(bs.hasElement("c"));
    	assertTrue(bs.hasElement("bla"));
    	assertTrue(bs.hasElement("H"));
    	assertFalse(bs.hasElement("N"));
    	assertFalse(bs.hasElement("B"));
    	assertFalse(bs.hasCenter(20, "Ca"));
    	assertFalse(bs.hasCenter(7, "C"));
    	assertFalse(bs.hasCenter(12, "Bla"));
    	assertFalse(bs.hasCenter(4, "h"));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetCenterBasisSetForElement() throws Exception
    {
    	BasisSet bs = new BasisSet();
    	bs.addCenterSpecBSet(new CenterBasisSet("Ca"));
    	bs.addCenterSpecBSet(new CenterBasisSet("C"));
    	
    	assertFalse(bs.hasElement("B"));
    	assertFalse(bs.hasCenter(6, "B"));
    	
    	CenterBasisSet cbs = bs.getCenterBasisSetForElement("B");
    	
    	assertTrue(bs.hasElement("B"));
    	assertFalse(bs.hasCenter(6, "B"));
    	assertEquals("B", cbs.getElement());
    	assertEquals(0, cbs.getShells().size());
    	assertEquals(0, cbs.getNamedComponents().size());
    	assertEquals(0, cbs.getECPShells().size());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetCenterBasisSetForCenter() throws Exception
    {
    	BasisSet bs = new BasisSet();
    	bs.addCenterSpecBSet(new CenterBasisSet("Ca"));
    	bs.addCenterSpecBSet(new CenterBasisSet("C"));
    	
    	assertFalse(bs.hasElement("B"));
    	assertFalse(bs.hasCenter(6, "B"));
    	
    	CenterBasisSet cbs = bs.getCenterBasisSetForCenter(6, "B");
    	
    	assertFalse(bs.hasElement("B"));
    	assertTrue(bs.hasCenter(6, "B"));
    	assertEquals("B", cbs.getElement());
    	assertEquals(6, cbs.getCenterIndex());
    	assertEquals(0, cbs.getShells().size());
    	assertEquals(0, cbs.getNamedComponents().size());
    	assertEquals(0, cbs.getECPShells().size());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testHasECP() throws Exception
    {
    	BasisSet bs = new BasisSet();
    	bs.addCenterSpecBSet(new CenterBasisSet("Ca"));
    	bs.addCenterSpecBSet(new CenterBasisSet("C"));
    	
    	assertFalse(bs.hasECP());
    
    	CenterBasisSet cbs = new CenterBasisSet();
    	cbs.addECPShell(new ECPShell("type"));
    	bs.addCenterSpecBSet(cbs);
    	
    	assertTrue(bs.hasECP());
    }
    	
//------------------------------------------------------------------------------

}
