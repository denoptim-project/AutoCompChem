package autocompchem.workflow.task;

/*
 *   Copyright (C) 2017  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * Action
 *
 * @author Marco Foscato
 */

public class Action
{
    /**
     * Type of action
     */
    private String type = "noType";

//------------------------------------------------------------------------------

    /**
     * Return the type of action
     */

    public String getType()
    {
	return type;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable representation of the action
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Action [type:").append(type).append("]");
	return sb.toString();
    }

//------------------------------------------------------------------------------

}
