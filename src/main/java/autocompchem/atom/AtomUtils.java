package autocompchem.atom;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

import autocompchem.run.Terminator;


/**
 * Tool box for atom-like objects. 
 *
 * @author Marco Foscato
 */

public class AtomUtils
{

//------------------------------------------------------------------------------

    /**
     * Returns the van der Waals radius of the atom or, for elements not covered
     * a default value.
     * @param atm the atom for which the VDW radius is required
     * @return the van der Waals radius of the atom. 
     */

    public static double getVdwRadius(IAtom atm)
    {
        String el = atm.getSymbol();
        return getVdwRradius(el);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the van der Waals radius of the element or, for elements not 
     * covered a default value.
     * @param elSymbol the symbol of the element
     * @return the van der Waals radius of the atom. 
     */

    public static double getVdwRradius(String elSymbol)
    {
        double r = 0.0;

        try {
            r = PeriodicTable.getVdwRadius(elSymbol);

//          TODO use Elements (CDK 1.5)
//          Elements e = Elements.ofString(elSymbol);
//          r = e.vdwRadius();

        } catch (Throwable thb) {
            System.out.println("WARNING! Element "
                                + elSymbol
                                + " not found. Using "
                                + r
                                + " as van der Waals radius.");
        }
        return r;
    }

//------------------------------------------------------------------------------

    /**
     * Check element symbol corresponds to real element of Periodic Table
     * @param symbol of the element to check
     * @return <code>true</code> if the element symbol correspond to an atom
     * in the periodic table.
     */

    public static boolean isElement(String symbol)
    {
        boolean res = false;
        IsotopeFactory ifact = null;
        try {
            ifact = Isotopes.getInstance();
            if (ifact.isElement(symbol))
            {
                @SuppressWarnings("unused")
                IElement el = ifact.getElement(symbol);
                res = true;
            }
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Unable to create IsotopeFactory "
                                        + " (in AtomUtils.getAtomicNumber)",-1);
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Get the elemental symbol from the atomic number.
     * Unrecognised atomic numbers will result in empty strings.
     * @param an the atomic number
     * @return the elemental symbol of a dummy if the atomic number is out of 
     * known range
     */

    public static String getElementalSymbol(int an)
    {
        IsotopeFactory ifact = null;
        IElement el = null;
        try {
            //Identify the element
            ifact = Isotopes.getInstance();
            //TODO: make it use emun from Element rather than hard coded limits
            if (an > 0 && an < 118)
            {
                el = ifact.getElement(an);
            }
            else
            {
                return "";
            }
        } 
        catch (Throwable t) 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to create "
                        + "IsotopeFactory (in AtomUtils.getElemntalSymbol) "
                        + "from atomic number " + an ,-1);
        }

        return el.getSymbol();
    }

//------------------------------------------------------------------------------

    /**
     * Get the atomic number from the element symbol
     * @param symbol of the element to check
     * @return the atomic number
     */

    public static int getAtomicNumber(String symbol)
    {
        IsotopeFactory ifact = null;
        IElement el = null;
        try {
            //Identify the element
            ifact = Isotopes.getInstance();
            if (ifact.isElement(symbol))
            {
                el = ifact.getElement(symbol);
            } else {
                Terminator.withMsgAndStatus("ERROR! Symbol '" + symbol + "' not "
                        + "recognized as element by IsotopeFactory.",-1);
            }
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Unable to create IsotopeFactory "
                                        + " (in AtomUtils.getAtomicNumber)",-1);
        }

        int atmNum = el.getAtomicNumber();
        
        return atmNum;
    }

//------------------------------------------------------------------------------

    /**
     * Interprets element symbol and identifies transition metals (block d)
     * @param symbol of the element to check
     * @param verbosity level for standard output
     * @return <code>true</code> if the element symbol correspond to an atom
     *         belonging to block-d the periodic table
     */

    public static boolean isMetalDblock(String symbol, int verbosity)
    {
        boolean res = false;
        int atmNum = -1;
        if (isElement(symbol))
        {
            atmNum = getAtomicNumber(symbol);
        } 

        //Define position in Periodic table
        if (getTransitionSeries(atmNum) != 0)
        {
            res = true;
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Define the transition series for atoms belonging to d blocks
     * lanthanides and actinides are not included (apart from the last elements 
     * Lu and Lr)
     * @param atmNum the atomic number of the element under investigation
     * @return an integer representing the transition series /1,2, or 3) 
     * or 0 in the atom does not belong to d-block
     */

    public static int getTransitionSeries(int atmNum)
    {
        int ts = 0;
        if ((atmNum > 20) && (atmNum < 31))
        {
            ts = 1;
        } else if ((atmNum > 38) && (atmNum < 49)) {
            ts = 2;
        } else if ((atmNum > 70) && (atmNum < 81)) {
            ts = 3;
        } else if ((atmNum > 102) && (atmNum < 113)) {
            ts = 4;
        }
        return ts;
    }

//------------------------------------------------------------------------------

    /**
     * Check the atom has a CDK property and the value is not null
     * @param atm the atom
     * @param propName the name of the property to be checked
     * @return <code>true</code> if the property is not null
     */

    public static boolean hasProperty(IAtom atm, String propName)
    {
        Object propValue = null;
        try {
            propValue = atm.getProperty(propName);
        } catch (Throwable tsm) {
            return false;
        }

        if (propValue == null)
            return false;

        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Check if the atom-like object is a dummy atom
     * @param atm the atom
     * @return <code>true</code> if the atom is dummy
     */

    public static boolean isAccDummy(IAtom atm)
    {
    	return isPsaudoAtmWithLabel(atm,AtomConstants.DUMMYATMLABEL);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Checks if the input is an instance of {@link PseudoAtom} and has the 
     * given label
     * @param atm the reference to the atom
     * @param lab the requested label.
     * @return <code>true</code> if the underlying type is {@link PseudoAtom}
     * and it has the given label.
     */
    
    //TODO-gg fix typo
    public static boolean isPsaudoAtmWithLabel(IAtom atm, String lab)
    {
    	if (atm instanceof AtomRef)
    	{
    		IAtom baseAtm = ((AtomRef) atm).deref();
	    	if (baseAtm instanceof PseudoAtom)
	    	{
	    		return ((PseudoAtom)baseAtm).getLabel().equals(lab);
	    	} else {
	    		return false;
	    	}
    	} 
    	else if (atm instanceof PseudoAtom) 
    	{
    		return ((PseudoAtom)atm).getLabel().equals(lab);
    	}
    	else if (atm instanceof Atom)
    	{
    		// WARNING: must check this AFTER having checked for PseudoAtom
    		return false;
    	} 
    	else 
    	{
    		Terminator.withMsgAndStatus("ERROR! Unexpected class in "
    				+ "AtomUtils.isDummy() method. The IAtom which has "
    				+ "triggered this is: " + atm.toString(), -1);
    	}
    	
        return false;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Chooses if reporting the elemental symbol of the PseudoAtom label.
     * Since CDK-2.3 (or before? Anyways, later than 1.4.15 ) elemental symbols
     * cannot be any string anymore. They must be valid elemental symbols. So, 
     * dummy atoms are given the default PseudoAtom symbol "R". This method
     * identifies such atoms and returns {@link AtomConstants.DUMMYATMLABEL},
     * i.e., the PseudoAtom label, instead of "R", i.e., the string returned by
     * PseudoAtom.getSymbol().
     * 
     * Note we still return null for if <code>atm = new Atom()</code>.
     * 
     * @param atm the atom to analyse
     * @return either the elemental symbol or the PseudoAtom label.
     */
    
    public static String getSymbolOrLabel(IAtom atm)
    {
    	String res = "";
    	if (atm instanceof AtomRef)
    	{
    		IAtom baseAtm = ((AtomRef) atm).deref();
    		return getSymbolOrLabel(baseAtm);
    	} 
    	else if (atm instanceof PseudoAtom) 
    	{
    		res = ((PseudoAtom) atm).getLabel();
    	}
    	else if (atm instanceof Atom)
    	{
    		res = atm.getSymbol();
    	} 
    	else 
    	{
    		Terminator.withMsgAndStatus("ERROR! Unexpected class in "
    				+ AtomUtils.class.getSimpleName()
    				+ " method. The IAtom which has "
    				+ "triggered this is: " + atm.toString(), -1);
    	}
    	
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Check the atom is an attachment point
     * @param atm the atom
     * @return <code>true</code> if the atom is an AP
     */

    public static boolean isAttachmentPoint(IAtom atm)
    {
        return isPsaudoAtmWithLabel(atm, AtomConstants.ATTACHMENTPOINTLABEL);
    }
    
//------------------------------------------------------------------------------

    /**
     * Counts the H explicitly connected to the atom
     * @param atm
     * @param mol
     * @return the number of Hydrogen atoms
     */

	public static Integer countExplicitHydrogens(IAtom atm, IAtomContainer mol) 
	{
		int nH = 0;
		for (IAtom ngbr : mol.getConnectedAtomsList(atm)) 
		{
			if (AtomUtils.isElement(ngbr.getSymbol()))
			{
				if (ngbr.getAtomicNumber() == 1) 
				{
					nH++;
				}
			}
		}
		return nH;
	}

//------------------------------------------------------------------------------

    /**
     * Get Cartesian coordinates in 3D space of an atom (even if this is in 2D)
     * @param atom the target atom
     * @return the point in 3D Cartesian coords
     */

    public static Point3d getCoords3d(IAtom atom)
    {
        Point3d p3d = new Point3d();
        if (atom.getPoint3d()!=null)
        {
        	Point3d atp3d = atom.getPoint3d();
        	p3d = new Point3d(atp3d.x, atp3d.y, atp3d.z);
        } else if (atom.getPoint2d()!=null)
        {
        	Point2d atp2d = atom.getPoint2d();
        	p3d = new Point3d(atp2d.x, atp2d.y, 0);
        }
        return p3d;
    }

//------------------------------------------------------------------------------
}
