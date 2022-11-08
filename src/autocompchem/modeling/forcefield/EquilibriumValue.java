package autocompchem.modeling.forcefield;

/*   
 *   Copyright (C) 2016  Marco Foscato 
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Object representing the equilibrium value of a specific force field energy 
 * term. 
 *
 * @author Marco Foscato
 */

public class EquilibriumValue implements Serializable
{
    /**
         * Version ID
         */
        private static final long serialVersionUID = 7306871947307205225L;

        /**
     * Reference name
     */
    private String name = "noname";

    /**
     * Value
     */
    private double value = 0.0;

    /**
     * Units
     */
    private String units = "noUnits";

    /**
     * Properties
     */
    private Map<String,Object> properties = new HashMap<String,Object>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty EquilibriumValue
     */

    public EquilibriumValue()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a EquilibriumValue with defintition of its values
     * @param name the reference name
     * @param value the numerical value
     * @param units the units used to report the value
     */

    public EquilibriumValue(String name, double value, String units)
    {
        this.name = name;
        this.value = value;
        this.units = units;
    }

//------------------------------------------------------------------------------

    /**
     * Set the value
     * @param value the new value of the Force constant
     */

    public void setValue(double value)
    {
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * Get the value
     * @return the value of the Force constant
     */

    public double getValue()
    {
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * Return a string representation of this object
     * @return a string representation of this primitive
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("EquilibriumValue [name:").append(name).append(", ");
        sb.append("value:").append(value).append(", ");
        sb.append("units:").append(units).append(", ");
        sb.append("properties:").append(properties.toString()).append("] ");
        return sb.toString();
    }

//------------------------------------------------------------------------------
     
}
