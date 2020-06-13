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

import java.util.Map;
import java.io.Reader;
import java.io.StringReader;


/**
 * Class representing the use of the environmental variables as info source. 
 *
 * @author Marco Foscato
 */

public class EnvironmentAsSource extends InfoChannel
{
    /**
     * Environment
     */
    private Map<String,String> env;

//------------------------------------------------------------------------------

    /**
     * Constructor. Environment is detected already here.
     */

    public EnvironmentAsSource()
    {
	super();
	env = System.getenv();
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
	for (String k : env.keySet())
	{
	    sb.append(k).append("=").append(env.get(k));
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
        sb.append("EnvironmentAsSource [type:").append(super.getType());
	sb.append("; text:").append(env);
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
