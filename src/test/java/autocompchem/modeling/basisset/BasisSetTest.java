package autocompchem.modeling.basisset;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;


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

    /**
     * @return a basis set that is filled with non-sense values, but allows
     * to test use of shells and ECPshells.
     */
    public BasisSet getTestBasisSet()
    {
    	BasisSet bs = new BasisSet();

    	CenterBasisSet cbs0 = new CenterBasisSet("C");
    	Shell s0 = new Shell("S");
    	s0.add(new Primitive("S", 0, 12.34, 0.5678, 1, 2));
    	s0.add(new Primitive("S", 0, Arrays.asList(7.6, 5.4), 0.089, 2, 3));
    	cbs0.addShell(s0);
    	
    	ECPShell ecpS0 = new ECPShell("s-ul potential");
    	ecpS0.add(new Primitive("A", 0, Arrays.asList(1.2, 4.5), 555.37, 6, 10));
    	ecpS0.add(new Primitive("B", 0, Arrays.asList(1.23), 555.37, 6, 10));
    	ecpS0.add(new Primitive("C", 0, 1.23, 555.37, 6, 10));
    	cbs0.addECPShell(ecpS0);

    	bs.addCenterSpecBSet(cbs0);
    	
    	CenterBasisSet cbs1 = new CenterBasisSet(3, "W");
    	Shell s = new Shell("S");
    	s.add(new Primitive("S", 0, 12.34, 0.5678, 1, 2));
    	s.add(new Primitive("S", 0, Arrays.asList(7.6, 5.4), 0.089, 2, 3));
    	cbs1.addShell(s);
    	Shell p = new Shell("P");
    	p.add(new Primitive("P", 1, Arrays.asList(1.2, 3.4, 5.6), 0.5678, 1, 2));
    	p.add(new Primitive("P", 2, 55.66, 0.089, 2, 3));
    	p.add(new Primitive("P", 3, Arrays.asList(-0.05), 0.456, 7, 8));
    	cbs1.addShell(s);
    	
    	ECPShell ecpS = new ECPShell("s-ul potential");
    	ecpS.add(new Primitive("A", 0, Arrays.asList(1.2, 4.5), 555.37, 6, 10));
    	ecpS.add(new Primitive("B", 0, Arrays.asList(1.23), 555.37, 6, 10));
    	ecpS.add(new Primitive("C", 0, 1.23, 555.37, 6, 10));
    	ECPShell ecpP = new ECPShell("p-ul potential");
    	ecpP.add(new Primitive("A", 0, Arrays.asList(0.12, 4.5), 555.37, 6, 10));
    	ecpP.add(new Primitive("B", 0, Arrays.asList(0.123), 555.37, 6, 10));
    	ecpP.add(new Primitive("C", 0, 0.123, 555.37, 6, 10));
    	cbs1.addECPShell(ecpS);
    	cbs1.addECPShell(ecpP);
    	
    	bs.addCenterSpecBSet(cbs1);
    	return bs;
    }
    

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	BasisSet bsA = getTestBasisSet();
    	BasisSet bsB = getTestBasisSet();
    	
    	assertTrue(bsA.equals(bsA));
    	assertTrue(bsA.equals(bsB));
    	assertTrue(bsB.equals(bsA));
    	
    	bsB.addCenterSpecBSet(CenterBasisSetTest.getTestCenterBasisSet());
    	assertFalse(bsA.equals(bsB));
    	
    	bsB = getTestBasisSet();
    	bsB.getAllCenterBSs().get(0).setAtmId("blabla");
    	assertFalse(bsA.equals(bsB));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	BasisSet bs = getTestBasisSet();
    	
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	String json = writer.toJson(bs);
    	
    	BasisSet bsNew = reader.fromJson(json, BasisSet.class);
    	
    	assertTrue(bsNew.hasECP());
    	assertEquals(bs.getAllCenterBSs().size(),bsNew.getAllCenterBSs().size());
    	assertTrue(bsNew.hasElement("C"));
    	assertTrue(bsNew.hasCenter(3, "W"));
    	assertFalse(bsNew.hasElement("W"));
    	assertFalse(bsNew.hasCenter(3, "C"));
    	assertTrue(bs.equals(bsNew));
    }
    	
//------------------------------------------------------------------------------

}
