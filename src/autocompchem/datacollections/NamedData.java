package autocompchem.datacollections;

import org.openscience.cdk.interfaces.IAtomContainer;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * General purpose data structure that can be named with a string.
 * The data is defined by its reference name,
 * its value type, and its actual value (i.e., the data itself).
 * 
 * @author Marco Foscato
 */

public class NamedData
{
    /**
     * A string used to identify the data.
     */
    private String reference;

    /**
     * The actual data.
     */
    private Object value;
      
    /**
     * The kind of data structure
     */
    private NamedDataType type;

    /**
     * Allowed kinds of data values
     */
    public enum NamedDataType {
    	UNDEFINED,
        STRING,
        DOUBLE,
        INTEGER,
        BOOLEAN,
        IATOMCONTAINER};

//------------------------------------------------------------------------------

    /**
     * Constructor of an empty (un)named data.
     */

    public NamedData()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a named data with a given content.
     * @param reference the name of the data.
     * @param type the type of object.
     * @param value the actual data.
     */

    public NamedData(String reference, NamedDataType type, Object value)
    {
        this.reference = reference;
        this.type = type; 
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * Return the reference name of this data.
     * @return the reference name.
     */

    public String getReference()
    {
        return reference;
    }

//------------------------------------------------------------------------------

    /**
     * Return the value of this data
     * @return the value of this data
     */

    public Object getValue()
    {
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * Return the value of this data casted into its declared type.
     * @return the value of this data.
     */

    public Object getValueAsObjectSubclass()
    {
    	Object valueObj;
        switch (type) {
			case DOUBLE:
				valueObj = Double.parseDouble(value.toString());
				break;
	
			case INTEGER: 
				valueObj = Integer.parseInt(value.toString());
				break;
			
			case STRING:
				valueObj = value.toString();
				break;
				
			case BOOLEAN:
				valueObj = (Boolean) value;
				break;
				
			case IATOMCONTAINER:
				valueObj = (IAtomContainer) value;
				break;
					
			default:
				valueObj = value.toString();
				break;
		}
        return valueObj;
    }

//------------------------------------------------------------------------------

    /**
     * Return the string representation of the value of this data.
     * Corresponds to getValue().toString().
     * @return the value of this data.
     */

    public String getValueAsString()
    {
        return value.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of this data.
     * @return the type of this data.
     */

    public NamedDataType getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Set the value of this data.
     * @param value the value to be set to this data.
     */

    public void setValue(Object value)
    {
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * String representation.
     * @return the string representation.
     */

    public String toString()
    {
        String str = reference + ":" + value.toString();
        return str;
    }

//------------------------------------------------------------------------------

}

