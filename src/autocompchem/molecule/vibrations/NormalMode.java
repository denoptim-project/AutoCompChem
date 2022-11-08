package autocompchem.molecule.vibrations;

import java.awt.Component;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;

import javax.vecmath.Point3d;

/**
 * Class representing an single normal mode
 * 
 * @author Marco Foscato
 */

public class NormalMode implements Cloneable
{
	/**
	 * The frequency associated with this mode
	 */
	private Double freq = null;
	
	/**
	 * Flag indicating an imaginary frequency
	 */
	private Boolean isImaginary = false;
	
	/**
	 * The list of components per atom/center
	 */
	private ArrayList<Point3d> components = new ArrayList<Point3d>();
	
//------------------------------------------------------------------------------

	/**
     * Construct an empty normal mode
     */

    public NormalMode()
    {
    }
 
//------------------------------------------------------------------------------

    /**
     * Sets the frequency associated with this mode.
     * @param val the value of the frequency (no default units).
     */
    
    public void setFrequency(Double val)
    {
    	this.freq = val;
    }
    
//------------------------------------------------------------------------------

    /**
     * sets the flag characterising this mode as an imaginary one
     * @param isImg if <code>true</code>  then the mode is flagged as imaginary
     */
    
    public void setImaginary(boolean isImg)
    {
    	this.isImaginary = isImg;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a specific component for a specific atom index
     * @param atmId the index of the atom (0-N). If N is larger than the current
     * list, then an appropriate number of empty components is added.
     * @param componentId set to 0 for X, 1 for Y, and 2 for Z
     * @param value the numerical value to impose.
     */
    public void setComponent(int atmId, int componentId, Double value)
    {
    	if (atmId >= components.size())
    	{
    		for (int i=components.size(); i<(atmId+1); i++)
    		{
    			components.add(new Point3d());
    		}
    	}
    	switch (componentId) {
    	case (0):
    		components.get(atmId).x = value;
    		break;
    	case (1):
    		components.get(atmId).y = value;
    		break;
    	case (2):
    		components.get(atmId).z = value;
    		break;
    	}
    }
    
//------------------------------------------------------------------------------

    /**
     * Get all the components for atom/center <code>i</code>.
     * @param i the index of the center
     * @return the 3-tupla of components referring to that atom
     */
    public Point3d getComponent(int i)
    {
    	return components.get(i);
    }
    
//------------------------------------------------------------------------------

    /**
     * Add atom components for a new atom/center. Effectively, append atom
     * components.
     * @parameter threeComponents the components to append to the list
     */

    public void append(Point3d threeComponents)
    {
        components.add(threeComponents);
    }

//------------------------------------------------------------------------------
    
    /**
     * @return a deep copy of this NormalMode
     */

    @Override
    public NormalMode clone()
    {
    	NormalMode cnm = new NormalMode();
    	if (freq != null)
    	{
    		cnm.setFrequency(Double.parseDouble(freq.toString()));
    	}
    	cnm.setImaginary(Boolean.parseBoolean(isImaginary.toString()));
    	for (Point3d p : this.components)
    	{
    		cnm.append(new Point3d(p.x,p.y,p.z));
    	}
    	return cnm;
    }
    
//------------------------------------------------------------------------------

    /**
     * @returns a text for human readers
     */
    
    public String toLines()
    {
    	StringBuilder sb = new StringBuilder();
    	for (Point3d p : components)
    	{
    		sb.append(String.format("%-13.7f %-13.7f %-13.7f", p.x, p.y, p.z));
    		sb.append(System.getProperty("line.separator"));
    	}
    	return sb.toString();
    }
    
//------------------------------------------------------------------------------

}
