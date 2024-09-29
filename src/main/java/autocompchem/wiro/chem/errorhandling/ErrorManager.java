package autocompchem.wiro.chem.errorhandling;

import java.io.File;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import autocompchem.files.FileComparatorByName;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;

/**
 * Tool for managing collections of errors.
 * For instance, the routine to collect an ensemble of <code>ErrorMessage</code>
 *  from a folder tree.
 * 
 * @author Marco Foscato
 */


public class ErrorManager
{

//------------------------------------------------------------------------------

    public ErrorManager()
    {}

//------------------------------------------------------------------------------

    /**
     * Collect all the files defining known errors that are stored in a folder 
     * tree.
     * @param path root directory of the folder tree to be searched
     * @return the list of error messages as objects
     */

    public static List<ErrorMessage> getAll(String path)
    {
        List<ErrorMessage> listErrors = new ArrayList<ErrorMessage>();
        List<File> listFiles = FileUtils.findByREGEX(path, ".*.err");

        //sort ascending=true
        Collections.sort(listFiles, new FileComparatorByName(true));

        for (File f : listFiles)
        {
            //Read file
            List<List<String>> form = IOtools.readFormattedText(f,
                                                ":", //key-value separator
                                                "#", //comment
                                                "$START", //start multiline
                                                "$END"); //end multiline
            
            //Create the object known error
            ErrorMessage em = new ErrorMessage(form);

            listErrors.add(em);
        }
        return listErrors;
    }

//------------------------------------------------------------------------------

}
