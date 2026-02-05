/*
 *   Copyright (C) 2022  Marco Foscato
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

package autocompchem.io.jsonableatomcontainer;

import java.util.Objects;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Wrapper of {@link IAtomContainer} that restricts its functionality to
 * selected content that can be serialized to JSON file. The amount and kind
 * of information that is serializable is effectively defined by the 
 * {@link LWAtom} and {@link LWBond} types.
 */
public class JSONableIAtomContainer 
{
	/**
	 * The CDK representation wrapped by this object
	 */
	public IAtomContainer iac;
	
//------------------------------------------------------------------------------
	
	public JSONableIAtomContainer(IAtomContainer iac)
	{
		this.iac = iac;
	}

//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
		IAtomContainer otherIAC = null;
		if (o instanceof IAtomContainer)
		{
			otherIAC = (IAtomContainer) o;
		} else if (o instanceof JSONableIAtomContainer) {
			otherIAC = ((JSONableIAtomContainer) o).iac;
		} else {
			return false;
		}
		
		if (iac.getAtomCount()!= otherIAC.getAtomCount())
			return false;
		for (int i=0; i<iac.getAtomCount(); i++)
		{
			LWAtom thisLWA = new LWAtom(iac.getAtom(i));
			LWAtom otherLWA = new LWAtom(otherIAC.getAtom(i));
			if (!thisLWA.equals(otherLWA))
				return false;
		}
		
		if (iac.getBondCount()!= otherIAC.getBondCount())
			return false;
		for (int i=0; i<iac.getBondCount(); i++)
		{
			LWBond thisLWB = new LWBond(iac.getBond(i));
			LWBond otherLWB = new LWBond(otherIAC.getBond(i));
			if (!thisLWB.equals(otherLWB))
				return false;
		}
		return true;
	}
	
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(iac);
    }
	
//------------------------------------------------------------------------------

	public IAtom getAtom(int i)
	{
		return iac.getAtom(i);
	}
	
//------------------------------------------------------------------------------

}
