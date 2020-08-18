package autocompchem.datacollections;

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
 * the value type, and the value. Reference name is case insensitive and is 
 * stored as uppercase.
 * 
 * @author Marco Foscato
 */

public class Parameter extends NamedData implements Cloneable
{

//------------------------------------------------------------------------------

    /**
     * Constructor of an empty parameter
     */

    public Parameter()
    {
    	super();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a parameter with a given reference and value.
     * @param reference the name identifying this parameter.
     * @param value the content of this <code>Parameter</code>.
     */

    public Parameter(String reference, String value)
    {
    	super(reference.toUpperCase(),NamedDataType.STRING,value);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a parameter with a given reference and value.
     * @param reference the name identifying this parameter.
     * @param type the type of object.
     * @param value the content of this <code>Parameter</code>
     */

    public Parameter(String reference, NamedDataType type, Object value)
    {
    	super(reference.toUpperCase(),type,value);
    }

//------------------------------------------------------------------------------

    @Override
    public Parameter clone() throws CloneNotSupportedException
    {
    	NamedData nd = super.clone();
    	Parameter p = new Parameter(nd.getReference(),nd.getType(),nd.getValue());
    	return p;
    }
    
//------------------------------------------------------------------------------
    
}

