package autocompchem.perception.circumstance;

import autocompchem.perception.infochannel.InfoChannel;

/*
 *   Copyright (C) 2023  Marco Foscato
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


/**
 * Interface for circumstance that are able to score
 *
 * @author Marco Foscato
 */

public abstract interface IScoring
{
//------------------------------------------------------------------------------

    /**
     * Return the value of the score given the input.
     * @param input the object on which the score can be calculated.
     * @return the score for the given input.
     */

    public abstract double calculateScore(InfoChannel input);
      	
//------------------------------------------------------------------------------

}
