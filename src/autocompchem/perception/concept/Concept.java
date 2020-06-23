package autocompchem.perception.concept;

/*
 *   Copyright (C) 2018  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more description.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * Abstract concept
 *
 * @author Marco Foscato
 */

public class Concept implements Cloneable
{
    /**
     * Type of concept
     */
    private String type;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty concept
     */
    public Concept()
    {
        type = "none";
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
     * Returns the type of this concept
     * @return the type
     */
    public String getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a human readable representation of this object
     * @return a string
     */

    public String toString()
    {
        String s = super.toString() + " " + type;
        return s;
    }

//------------------------------------------------------------------------------

}
