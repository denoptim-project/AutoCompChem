package autocompchem.modeling.basisset;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;


public class CenterBasisSetTest 
{
  
//------------------------------------------------------------------------------

    /**
     * @return a center-specific basis set that is filled with non-sense values.
     */
    public static CenterBasisSet getTestCenterBasisSet()
    {	
    	CenterBasisSet cbs = new CenterBasisSet(null, 3, "W");
    	Shell s = new Shell("S");
    	s.add(new Primitive("S", 0, 12.34, 0.5678, 1, 2));
    	s.add(new Primitive("S", 0, Arrays.asList(7.6, 5.4), 0.089, 2, 3));
    	cbs.addShell(s);
    	Shell p = new Shell("P");
    	p.add(new Primitive("P", 1, Arrays.asList(1.2, 3.4, 5.6), 0.5678, 1, 2));
    	p.add(new Primitive("P", 2, 55.66, 0.089, 2, 3));
    	p.add(new Primitive("P", 3, Arrays.asList(-0.05), 0.456, 7, 8));
    	cbs.addShell(s);
    	
    	ECPShell ecpS = new ECPShell("s-ul potential");
    	ecpS.add(new Primitive("A", 0, Arrays.asList(1.2, 4.5), 555.37, 6, 10));
    	ecpS.add(new Primitive("B", 0, Arrays.asList(1.23), 555.37, 6, 10));
    	ecpS.add(new Primitive("C", 0, 1.23, 555.37, 6, 10));
    	ECPShell ecpP = new ECPShell("p-ul potential");
    	ecpP.add(new Primitive("A", 0, Arrays.asList(0.12, 4.5), 555.37, 6, 10));
    	ecpP.add(new Primitive("B", 0, Arrays.asList(0.123), 555.37, 6, 10));
    	ecpP.add(new Primitive("C", 0, 0.123, 555.37, 6, 10));
    	cbs.addECPShell(ecpS);
    	cbs.addECPShell(ecpP);
    	
    	return cbs;
    }

//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
    	CenterBasisSet cbsA = getTestCenterBasisSet();
    	CenterBasisSet cbsB = getTestCenterBasisSet();
    	
    	assertTrue(cbsA.equals(cbsA));
    	assertTrue(cbsA.equals(cbsB));
    	assertTrue(cbsB.equals(cbsA));
    	
    	cbsB.addShell(ShellTest.getTestShell());
    	assertFalse(cbsA.equals(cbsB));
    	
    	cbsB = getTestCenterBasisSet();
    	cbsB.setECPMaxAngMom(123456);
    	assertFalse(cbsA.equals(cbsB));
    	
    	cbsB = getTestCenterBasisSet();
    	cbsB.setECPType("blabla");
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.setElectronsInECP(123456);
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.setElement("Unl");
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.setCenterTag("blabla");
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.setCenterIndex(123456);
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.getNamedComponents().add("blabla");
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.getShells().get(0).setScaleFact(1.2);
    	assertFalse(cbsA.equals(cbsB));

    	cbsB = getTestCenterBasisSet();
    	cbsB.getECPShells().get(0).setType("blabla");
    	assertFalse(cbsA.equals(cbsB));
    }

//------------------------------------------------------------------------------

}
