package autocompchem.perception.infochannel;

import java.io.FileReader;

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

import java.io.Reader;

import autocompchem.files.FileUtils;
import autocompchem.run.Terminator;

/**
 * File as a source of information
 *
 * @author Marco Foscato
 */

public class FileAsSource extends InfoChannel
{
    /**
     * Pathname
     */
    private String pathName = "";

//------------------------------------------------------------------------------

    /**
     * Constructs an empty FileAsSource
     */

    public FileAsSource()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a FileAsSource and specify the pathname. Absolute or relative
     * pathnames should be expected. No assumption of pathname.
     */

    public FileAsSource(String pathName)
    {
        super();
        this.pathName = pathName;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a FileAsSource and specify the pathname. Absolute or relative
     * pathnames should be expected. No assumption of pathname.
     */

    public FileAsSource(String pathName, InfoChannelType ict)
    {
        super();
        this.pathName = pathName;
        setType(ict);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the pathname of the file that is the source of information
     * @return the pathname
     */

    public String getPathName()
    {
        return pathName;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the Reader of the source file.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defined that the Reader is no longer
     * needed.
     * @return a reader for reading the character-info from the source
     */

    public Reader getSourceReader()
    {
        FileUtils.foundAndPermissions(pathName,true,false,false);
        try
        {
            super.reader = new FileReader(pathName);
        }
        catch (Exception e)
        {
            Terminator.withMsgAndStatus("ERROR! File "+ pathName 
                                                          + " not found!.",-1);
        }
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
        sb.append("FileAsSource [type:").append(super.getType());
        sb.append("; pathName:").append(pathName);
        sb.append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
