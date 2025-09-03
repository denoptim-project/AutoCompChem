package autocompchem.perception.concept;

import java.util.Objects;

/*
 *   Copyright (C) 2018  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more description.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//TODO: consider renaming this to Event

/**
 * Abstract concept
 *
 * @author Marco Foscato
 */

public class Concept
{
    /**
     * Type of concept
     */
    private String type;
    
    /**
     * Reference name of this concept
     */
    private String refName;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty concept
     */
    public Concept()
    {
        this.type = "none";
        this.refName = "none";
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an given type of concept
     * @param type of the concept to be constructed
     */
    public Concept(String type)
    {
        this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an given type of concept
     * @param type of the concept to be constructed
     */
    public Concept(String type, String refName)
    {
        this.type = type;
        this.refName = refName;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the type of this concept.
     * @param type the type of concept.
     */
    
    public void setType(String type)
    {
    	this.type = type;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the type of this concept.
     * @param refName the reference name.
     */
    
    public void setRefName(String refName)
    {
    	this.refName = refName;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the type of this concept
     * @return the type
     */
    public String getType()
    {
        return type;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the reference name of this concept
     * @return the name
     */
    public String getRefName()
    {
        return refName;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a human readable representation of this object
     * @return a string
     */
    public String toString()
    {
        String s = type + " " + refName;
        return s;
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
        	return false;
         
        Concept other = (Concept) o;
         
        if (!this.type.equals(other.type))
            return false;

        return this.refName.equals(other.refName);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(type, refName);
    }

//------------------------------------------------------------------------------

}
