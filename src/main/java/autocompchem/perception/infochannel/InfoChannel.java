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

import java.io.Reader;

/**
 * Abstract class representing any kind of information source that can be read.
 *
 * @author Marco Foscato
 */
//TODO: change to ReadableInfoChannel to distinguish from data collectors-like infochannels
public abstract class InfoChannel
{
    /**
     * Character stream source
     */
    protected Reader reader;

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
     * Returns the tool reading characters from the info stream source.
     * The Reader subclass instance is decided by the subclasses depending on the
     * kind og source to be read.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defined that the Reader is no longer
     * needed.
     * @return a readed for reading the character-info from the source
     */
 
    public abstract Reader getSourceReader();

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" [type:").append(type);
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
