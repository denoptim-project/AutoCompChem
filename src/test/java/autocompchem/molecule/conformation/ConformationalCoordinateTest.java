package autocompchem.molecule.conformation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import com.google.gson.Gson;

import autocompchem.io.ACCJson;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.molecule.conformation.ConformationalCoordinate.ConformationalCoordType;
import autocompchem.utils.NumberUtils;

public class ConformationalCoordinateTest 
{

    private IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

	/**
	 * Created a dummy set of constraints filled with non-sense but plausible 
	 * values.
	 * 
	 * The chemical system
	 * 
	 * @return a dummy set of constraints.
	 */
    public static ConformationalCoordinate getTestConformationalCoordinate()
    {
		ConformationalCoordinate cs = new ConformationalCoordinate(
				new int[] {0, 1, 2, 3});
		return cs;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testDefineType() throws Exception
    {
		IAtomContainer mol = builder.newAtomContainer();
		String[] elements = new String[]{"C", "O", "N", "P", "H"};
        for (int i=0; i<elements.length; i++)
        {
            IAtom atom = builder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(new Point3d(0.0, 0.0, Double.valueOf(i)));
            mol.addAtom(atom);
            if (i>0 && i<4)
                mol.addBond(i-1, i, IBond.Order.SINGLE);
        }
        mol.addBond(2, 4, IBond.Order.SINGLE);
        
        ConformationalCoordinate cc = new ConformationalCoordinate(
        		new AnnotatedAtomTuple(Arrays.asList(mol.getAtom(0)), mol));
        assertEquals(ConformationalCoordType.FLIP, cc.getType());
        
        cc = new ConformationalCoordinate(new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(1)), mol));
        assertEquals(ConformationalCoordType.TORSION, cc.getType());
        
        cc = new ConformationalCoordinate(new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(2)), mol));
        assertEquals(ConformationalCoordType.UNDEFINED, cc.getType());
        
        cc = new ConformationalCoordinate(new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(1), mol.getAtom(2), mol.getAtom(3)),
        		mol));
        assertEquals(ConformationalCoordType.TORSION, cc.getType());
        
        cc = new ConformationalCoordinate(new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(0), mol.getAtom(2), mol.getAtom(1), mol.getAtom(3)),
        		mol));
        assertEquals(ConformationalCoordType.IMPROPERTORSION, cc.getType());
        
        cc = new ConformationalCoordinate(new AnnotatedAtomTuple(Arrays.asList(
        		mol.getAtom(1), mol.getAtom(2), mol.getAtom(4), mol.getAtom(3)),
        		mol));
        assertEquals(ConformationalCoordType.IMPROPERTORSION, cc.getType());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testHandlingOfValues() throws Exception
    {
    	ConformationalCoordinate cc = getTestConformationalCoordinate();
    	assertNull(cc.getValues());
    	
    	cc.setValues(new double[] {0.1, -2.2, 0.0});
    	double[] arr = cc.getValues();
    	assertEquals(3, arr.length);
    	assertTrue(NumberUtils.closeEnough(0.1, arr[0]));
    	assertTrue(NumberUtils.closeEnough(-2.2, arr[1]));
    	assertTrue(NumberUtils.closeEnough(0.0, arr[2]));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception
    {
		ConformationalCoordinate c1 = getTestConformationalCoordinate();
		ConformationalCoordinate c2 = getTestConformationalCoordinate();
		
		assertTrue(c1.equals(c2));
		assertTrue(c2.equals(c1));
		assertTrue(c1.equals(c1));
		assertFalse(c1.equals(null));
		
		c2 = getTestConformationalCoordinate();
		c2.setFold(-3);
		assertFalse(c1.equals(c2));
		
		c2 = getTestConformationalCoordinate();
		c2.setType(ConformationalCoordType.FLIP);
		assertFalse(c1.equals(c2));
		
		c2 = getTestConformationalCoordinate();
		c2.setNumAtoms(-10); //nonsense but ok for test
		assertFalse(c1.equals(c2));
		
		ConformationalCoordinate c3 = new ConformationalCoordinate(
				new int[] {0, 1, 2, 4});
		assertFalse(c1.equals(c3));
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
		ConformationalCoordinate c1 = getTestConformationalCoordinate();
		ConformationalCoordinate cl1 = c1.clone();
		assertTrue(c1.equals(cl1));
		assertFalse(c1 == cl1);
		
		c1.setType(ConformationalCoordType.FLIP); //nonsense, but just for test
		assertFalse(c1.equals(cl1));
		
		c1 = getTestConformationalCoordinate();
		assertTrue(c1.equals(cl1));
		c1.setFold(22);
		assertFalse(c1.equals(cl1));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
		ConformationalCoordinate original = getTestConformationalCoordinate();
		
		Gson writer = ACCJson.getWriter();
		Gson reader = ACCJson.getReader();
		
	    String json = writer.toJson(original);
	    ConformationalCoordinate fromJson = reader.fromJson(json, 
				ConformationalCoordinate.class);
	    assertEquals(original, fromJson);

	    ConformationalCoordinate clone = original.clone();
	    assertEquals(clone, fromJson);
    }
    
//------------------------------------------------------------------------------

}
