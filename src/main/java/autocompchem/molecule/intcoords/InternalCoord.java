package autocompchem.molecule.intcoords;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Locale;

/**
 * Object representing a single internal coordinate with a name, a value, and 
 * an optional type.
 *
 * @author Marco Foscato
 */ 

public class InternalCoord implements Cloneable
{
    /**
     * Internal coordinate name
     */
    protected String name = "noname";

    /**
     * Value of the coordinate 
     */
    protected double value = 0.0;

    /**
     * List of atom indexes defining this internal coordinate
     */
    protected ArrayList<Integer> ids = new ArrayList<Integer>();

    /**
     * Type of the coordinate
     */
    protected String type = "notype";


//------------------------------------------------------------------------------

    /**
     * Construct an empty InternalCoord
     */

    public InternalCoord()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Construct an InternalCoord defining only the name ond the value
     * @param name the name of this coordinate
     * @param value the numerical value assignet to this coordinate
     * @param ids the list of indexes defining this coordinate
     */

    public InternalCoord(String name, double value, ArrayList<Integer> ids)
    {
        this.name = name;
        this.value = value;
        this.ids = ids;
    }

//------------------------------------------------------------------------------

    /**
     * Construct an InternalCoord defining all its fields
     * @param name the name
     * @param value the value
     * @param ids the list of indexes defining this coordinate
     * @param type the type as a string (Note that this should be convertable
     * inot an integer)
     */

    public InternalCoord(String name, double value, ArrayList<Integer> ids,
                                                                    String type)
    {
        this.name = name;
        this.value = value;
        this.ids = ids;
        this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the name
     * @return the name of this internal coordinate
     */

    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the numerical value
     * @return the numerical value of this internal coordinate
     */

    public double getValue()
    {
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * Changes the numerical value
     * @param newVal the new value
     */

    public void setValue(double newVal)
    {
        value = newVal;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the type of this internal coordinate
     * @param type the new type
     */

    public void setType(String type)
    {
        this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of atom indexes
     * @return the list of indexes
     */

    public ArrayList<Integer> getIDs()
    {
        return ids;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the number of atom indexes
     * @return the number of indexes
     */

    public int getIDsCount()
    {
        return ids.size();
    }

//------------------------------------------------------------------------------

    /**
     * Returns the type 
     * @return the type of this internal coordinate
     */

    public String getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates if a list of atom indexes corresponds to the indexes used to 
     * define this coordinate.
     * @param otherIds the other list of indexes to compare with those of this
     * coordinate
     * @return <code>true</code> if the given list of indexes corresponds to
     * the one defining this coordinate.
     */

    public boolean compareIDs(ArrayList<Integer> otherIds)
    {
        boolean res = false;
        if (ids.size() == otherIds.size())
        {
            switch (ids.size())
            {
                case 2:
                    if ((ids.get(0).intValue() == otherIds.get(0).intValue() &&
                         ids.get(1).intValue() == otherIds.get(1).intValue()) 
                        ||
                        (ids.get(0).intValue() == otherIds.get(1).intValue() &&
                         ids.get(1).intValue() == otherIds.get(0).intValue()))
                    {
                        res = true;
                    } 
                    break;

                case 3:
                    if ((ids.get(0).intValue() == otherIds.get(0).intValue() &&
                         ids.get(1).intValue() == otherIds.get(1).intValue() &&
                         ids.get(2).intValue() == otherIds.get(2).intValue())
                        ||
                        (ids.get(0).intValue() == otherIds.get(2).intValue() &&
                         ids.get(1).intValue() == otherIds.get(1).intValue() &&
                         ids.get(2).intValue() == otherIds.get(0).intValue()))
                    {
                        res = true;
                    }
                    break;

                case 4:
                    if ((ids.get(0).intValue() == otherIds.get(0).intValue() &&
                         ids.get(1).intValue() == otherIds.get(1).intValue() &&
                         ids.get(2).intValue() == otherIds.get(2).intValue() &&
                         ids.get(3).intValue() == otherIds.get(3).intValue())
                        ||
                        (ids.get(0).intValue() == otherIds.get(3).intValue() &&
                         ids.get(1).intValue() == otherIds.get(2).intValue() &&
                         ids.get(2).intValue() == otherIds.get(1).intValue() &&
                         ids.get(3).intValue() == otherIds.get(0).intValue()))
                    {
                        res = true;
                    }
                    break;

                default:
                    res = false;
            }
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Produces a string to be used for reporting this variable in a zmatrix
     * @param useReference set to <code>true</code> to report only the variable
     * name and, if defined, the type. Otherwise the value, and, if defined, the
     * also type are reported.
     * @return a string formatted according to the type and content of this
     * coordinate
     */

    public String getZMatrixString(boolean useReference)
    {
        String s = "";
        if (useReference)
        {
            s = name;
        }
        else
        {
            s = String.format(Locale.ENGLISH," %5.8f",value);
        }
        if (!type.equals("notype"))
        {
            s = s + " " + type;
        }
        return s;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a string for defining the value of this variable when the 
     * zmatrix has been produced using variables names.
     * @param separator the string to be used between the name and value
     * @return string definitiong of this internal coordinate
     */
 
    public String getVariabeDefLine(String separator)
    {
        String s = name + separator + String.format(Locale.ENGLISH," %5.8f",value);
        return s;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a string for printing lists of internal coordinates
     * @return a string formatted to print lists of internal coordinates
     */

    public String toTableLine()
    {
        StringBuilder sb = new StringBuilder();
        switch (ids.size())
        {
            case 2:
                sb.append("Stretch ");
                break;

            case 3:
                sb.append("Bending ");
                break;

            case 4:
                sb.append("Torsion ");
                break;

            default:
                sb.append("n.d.    ");
                break;
        }
        for (Integer id : ids)
        {
            sb.append(id + " ");
        }
        if (!type.equals("notype"))
        {
            sb.append(type);
        }

        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Return the string representation of this object
     * @return the string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("InternalCoord [name:").append(name).append(", ");
        sb.append("ids:").append(ids).append(", ");
        sb.append("value:").append(value).append(", ");
        sb.append("type:").append(type).append(", ");
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a deep copy
     */

    @Override
    public InternalCoord clone()
    {
    	ArrayList<Integer> cIds = new ArrayList<Integer>();
    	for (Integer id : ids)
    		cIds.add(id.intValue());
    	InternalCoord ic = new InternalCoord(name, value, cIds, type);
    	return ic;
    }

//------------------------------------------------------------------------------

}
