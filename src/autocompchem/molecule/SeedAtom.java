package autocompchem.molecule;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * The concept of an atom and the number of connected neighbours collected in a
 * single object. SeedAtoms are useful for exploring atom containers following
 * their connectivity.
 * 
 * @author Marco Foscato 
 */

public class SeedAtom
{
    //connection
    private int connections;

    //First atom of the ligand
    private IAtom seed;

//
//          <...part of the ligand...>
//         /
//  ---<seed>---<...part of the ligand...>
//         \ 
//          <...part of the ligand...>
//

//------------------------------------------------------------------------------

    public SeedAtom(IAtom seed, IAtomContainer mol)
    {
        this.seed = seed;
        this.connections = mol.getConnectedAtomsCount(seed);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the element symbol
     * @return the symbol
     */

    public String getSymbol()
    {
        String s = seed.getSymbol();
        return s;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the mass number
     */
    public Integer getMassNumber()
    {
    	Integer a = null;

		try {
			if (Isotopes.getInstance().isElement(seed.getSymbol()))
	        {
				IIsotope i = Isotopes.getInstance().getMajorIsotope(seed.getSymbol());
	            a = i.getMassNumber();
	        }
		} catch (Throwable e) {
			// nothing really
		}
		
    	return a;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the seed atom
     * @return the seed atom
     */

    public IAtom getAtom()
    {
        return seed;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the number of connected atoms
     * @return the connection number
     */

    public int getConnectedAtomsCount()
    {
        return connections;
    }

//------------------------------------------------------------------------------
}
