package autocompchem.perception.circumstance;

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

import autocompchem.perception.infochannel.InfoChannelType;

/**
 * Interface for circumstance
 *
 * @author Marco Foscato
 */

public interface ICircumstance
{

//------------------------------------------------------------------------------

    /**
     * Return the data channel, or feed, this circumstance relates to
     * @return the data channel
     */

    public InfoChannelType getChannelType();

//------------------------------------------------------------------------------

    /**
     * Identifies the circumstance as one that requires to match strings
     * @return <code>true</code> if there is a string query to be matched
     */

    public boolean requiresTXTMatch();

//------------------------------------------------------------------------------

    //TODO
    /**
     * Convert a score from numeric to boolean. Uses a threshold that can be set
     * by method ___TODO:write name___.
     * @param dScore the score in numeric double
     * @return a true/false value
     */

    public boolean scoreToDecision(double dScore);

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    public String toString();

//------------------------------------------------------------------------------

}
