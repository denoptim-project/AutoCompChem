package autocompchem.perception.infochannel;

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


/**
 * Abstract class representing any kind of information source.
 *
 * @author Marco Foscato
 */

public abstract class InfoChannel
{

    /**
     * The type of information channel
     */
     private InfoChannelType type;

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public InfoChannel()
    {
        type = InfoChannelType.NONE;
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of information channel
     * @return the type of channel
     */

    public InfoChannelType getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Set the type of this information channel
     * @param ict the type of this information channel
     */

    public void setType(InfoChannelType ict)
    {
        type = ict;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("InfoChannel [type:").append(type);
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
