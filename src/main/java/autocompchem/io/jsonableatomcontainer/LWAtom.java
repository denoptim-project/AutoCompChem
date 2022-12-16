/*
 *   This code is taken from the DENOPTIM project. See 
 *   https://github.com/denoptim-project/DENOPTIM for further details.
 *   
 *   Modifications: 1) the use of AtomUtils.isElement() from this
 *   project rather than the MoleculeUtils.isElement() from the DENOPTIM
 *   project. 2) Added constructor from IAtom. 3) added equals method.
 *   
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
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

package autocompchem.io.jsonableatomcontainer;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;

import autocompchem.atom.AtomUtils;
import autocompchem.utils.NumberUtils;

/**
 * A light-weight atom representation to facilitate json serialization of 
 * {@link IAtom}.
 *  
 * @author Marco Foscato
 */

public class LWAtom
{
    /**
     * The elemental symbol of this center, i.e., an atom or a pseudo-atom.
     */
    protected String elSymbol = "";
    
    /**
     * Cartesian coordinates of this center
     */
    protected Point3d p3d = null;

//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param atm the atom from which we build this light-weigth representation.
     */
    public LWAtom(IAtom atm)
    {
        this.elSymbol = AtomUtils.getSymbolOrLabel(atm);
        this.p3d = AtomUtils.getCoords3d(atm);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param elSymbol the elemental symbol
     * @param p3d the Cartesian coordinated of this center
     */
    public LWAtom(String elSymbol, Point3d p3d)
    {
        this.elSymbol = elSymbol;
        this.p3d = p3d;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a CDK representation of this center.
     * @return a CDK representation of this center.
     */
    public IAtom toIAtom()
    {
        IAtom atm = null;
        if (AtomUtils.isElement(elSymbol))
        {
            atm = new Atom(elSymbol);
        } else {
            atm = new PseudoAtom(elSymbol);
        }
        atm.setPoint3d(new Point3d(p3d));
        return atm;
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
    	if (!(o instanceof LWAtom))
    		return false;
    	
    	LWAtom other = (LWAtom) o;
    	
    	if (!this.elSymbol.equals(other.elSymbol))
    		return false;
    	if (!NumberUtils.closeEnough(p3d.x,other.p3d.x))
    		return false;
    	if (!NumberUtils.closeEnough(p3d.y,other.p3d.y))
    		return false;
    	if (!NumberUtils.closeEnough(p3d.z,other.p3d.z))
    		return false;
    	return true;
    }
    
//------------------------------------------------------------------------------
    
}
