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
public abstract class ReadableIC extends InfoChannel
{
    /**
     * Character stream source
     */
    protected Reader reader;

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ReadableIC()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the info channel can be read. For example, if the channel is
     * a reader on a file, this method checks if the file exists and is
     * readable.
     * @return <code>true</code> is the channel can deliver some content.
     */
    public abstract boolean canBeRead();

//------------------------------------------------------------------------------

    /**
     * Returns the tool reading characters from the info stream source.
     * The Reader subclass instance is decided by the subclasses depending on the
     * kind of source to be read.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defined that the Reader is no longer
     * needed.
     * @return a reader for reading the character-info from the source
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
        sb.append(super.toString());
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
