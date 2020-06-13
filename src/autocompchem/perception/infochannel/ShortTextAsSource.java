package autocompchem.perception.infochannel;

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
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.io.Reader;
import java.io.StringReader;


/**
 * Class representing a few lines of text to be used as information source. 
 * Tthe text is meant to be small enough that it is convenient
 * or useful to keep it in an ArrayList.
 *
 * @author Marco Foscato
 */

public class ShortTextAsSource extends InfoChannel
{
    /**
     * Text organized by lines
     */
    private ArrayList<String> txt = new ArrayList<String>();

//------------------------------------------------------------------------------

    /**
     * Constructs an empty ShortTextAsSource
     */

    public ShortTextAsSource()
    {
	super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a ShortTextAsSource with one line of text
     * @param line the text
     */

    public ShortTextAsSource(String line)
    {
        super();
	this.txt = new ArrayList<String>(Arrays.asList(line));
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a ShortTextAsSource and specify the text
     */

    public ShortTextAsSource(ArrayList<String> txt)
    {
        super();
	this.txt = txt;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the Reader of the string source.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defined that the Reader is no longer 
     * needed.
     * @return a readed for reading the character-info from the source
     */

    public Reader getSourceReader()
    {
	StringBuilder sb = new StringBuilder();
	for (String l : txt)
	{
	    sb.append(l);
	    sb.append(System.getProperty("line.separator"));
	}
        super.reader = new StringReader(sb.toString());
	return super.reader;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ShortTextAsSource [type:").append(super.getType());
	sb.append("; text:").append(txt);
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
