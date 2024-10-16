/*
 *   This code is taken from the DENOPTIM project. See 
 *   https://github.com/denoptim-project/DENOPTIM for further details.
 *   
 *   Modifications: 1) Added constructor from IBond. 2) added equals method.
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

import java.util.Objects;

import org.openscience.cdk.interfaces.IBond;

/**
 * A light-weight bond representation to facilitate json serialization of 
 * {@link IBond}. It consider only connections that can be defined between two
 * centers, i.e., two atoms or pseudo-atoms.
 *  
 * @author Marco Foscato
 */
public class LWBond
{
    /**
     * 0-based index of the centers, i.e., atoms or pseudo-atoms, connected by
     * this bond.
     */
    protected int[] atomIds = new int[2];
    
    /**
     * Type of bond
     */
    protected IBond.Order bo;
    
//------------------------------------------------------------------------------
 
    /**
     * Constructor from the CDK object this light-weight representation is 
     * meant to represent.
     * @param bnd the bond to represent.
     */
    public LWBond(IBond bnd)
    {
        this.atomIds[0] = bnd.getAtom(0).getIndex();
        this.atomIds[1] = bnd.getAtom(1).getIndex(); 
        this.bo = bnd.getOrder();
    }
    
//------------------------------------------------------------------------------    
    
    public LWBond(int atmIdA, int atmIdB, IBond.Order bo)
    {
        this.atomIds[0] = atmIdA;
        this.atomIds[1] = atmIdB;
        this.bo = bo;
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o)
    {
    	if (!(o instanceof LWBond))
    		return false;
    	LWBond other = (LWBond) o;
    	
    	if (this.atomIds[0]!=other.atomIds[0])
    		return false;
    	if (this.atomIds[1]!=other.atomIds[1])
    		return false;
    	if (this.bo!=other.bo)
    		return false;
    	return true;
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(atomIds, bo);
    }
    
//------------------------------------------------------------------------------
    
}
