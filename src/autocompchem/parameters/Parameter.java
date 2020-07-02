package autocompchem.parameters;

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
 * General purpose parameter defining an information to be passed unchanged 
 * to some routine or software. A parameter is defined by the reference name,
 * the value type, and the value.
 * 
 * @author Marco Foscato
 */

public class Parameter
{
    /**
     * A string used to identify the parameter. Typically the keyword in a
     * keyword:value pair.
     */
    private String reference;

    /**
     * The actual parameter. Typically the value in a keyword:value pair.
     */
    private Object value;
      
    /**
     * The kind of object used to represent the value
     */
    private ParameterValueType type;

    /**
     * Allowed kinds of parameter values
     */
    public enum ParameterValueType {
    	UNDEFINED,
        STRING,
        DOUBLE,
        INTEGER,
        BOOLEAN,
        IATOMCONTAINER};

//------------------------------------------------------------------------------

    /**
     * Constructor of an empty parameter
     */

    public Parameter()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a parameter with a given reference and value
     * @param reference the name identifying this parameter
     * @param type the type of object
     * @param value the content of this <code>Parameter</code>
     */

    public Parameter(String reference, ParameterValueType type, Object value)
    {
        this.reference = reference;
        this.type = type; 
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * Return the reference name (i.e., key/label string) of this parameter
     * @return the reference name of this parameter
     */

    public String getReference()
    {
        return reference;
    }

//------------------------------------------------------------------------------

    /**
     * Return the value of this parameter
     * @return the value of this parameter
     */

    public Object getValue()
    {
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * Return the value of this parameter as
     * Corresponds to getValue().toString()
     * @return the value of this parameter
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
     * Return the string representation of the value of this parameter.
     * Corresponds to getValue().toString()
     * @return the value of this parameter
     */

    public String getValueAsString()
    {
        return value.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of this parameter
     * @return the type of this parameter
     */

    public ParameterValueType getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Set the value of this parameter
     * @param value the value to be set to this parameter
     */

    public void setValue(Object value)
    {
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * String representation
     * @return the string representation
     */

    public String toString()
    {
        String str = reference + ":" + value.toString();
        return str;
    }

//------------------------------------------------------------------------------

}

