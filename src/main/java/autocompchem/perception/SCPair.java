package autocompchem.perception;

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
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.situation.Situation;


/**
 * A pair of references that includes the {@link Situation} and a specific
 * {@link ICircumstance} from its context.
 */

public class SCPair implements Comparable<SCPair>
{
    public Situation s = null;
    public ICircumstance c = null;

//-----------------------------------------------------------------------------

    /**
     * Constructor
     * @param s the parent situation in the pair
     * @param c the child circumstance in the pair
     */

    public SCPair(Situation s, ICircumstance c)
    {
        this.s = s;
        this.c = c;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the parent situation
     * @return the parent situation
     */

    public Situation getSituation()
    {
        return s;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the child circumstance
     * @return the child circumstance
     */

    public ICircumstance getCircumstance()
    {
        return c;
    }

//-----------------------------------------------------------------------------

    /**
     * Method for Comparator interface
     * @param other the object to be compared with this object
     */

    @Override
    public int compareTo(SCPair other)
    {
        int res = 0;
        if (this.getSituation() != other.getSituation())
        {
            res = this.toString().compareTo(other.toString());
        }
        else
        {
            if (this.getCircumstance() != other.getCircumstance())
            {
                res = this.getCircumstance().toString().compareTo(
                                            other.getCircumstance().toString());
            }
        }

        return res;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a short ID string for logging purposes
     * @return a sting identifying this SCPair
     */
 
    public String toIDString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[SituationID ");
        sb.append(Integer.toHexString(s.hashCode()));
        sb.append("]:[CircumstanceID ");
        sb.append(Integer.toHexString(c.hashCode())).append("]]");
        return sb.toString();
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a human readable representation of this object
     * @return a string representation of this object
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(super.toString());
        sb.append(" [Situation: ").append(s.toString());
        sb.append("]-[ICircumstance: ").append(c.toString()).append("]]");
        return sb.toString();
    }

//-----------------------------------------------------------------------------

}
