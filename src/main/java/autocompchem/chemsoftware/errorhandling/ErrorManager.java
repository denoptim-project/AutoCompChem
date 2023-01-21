package autocompchem.chemsoftware.errorhandling;

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
        List<File> listFiles = FileUtils.find(path,"*.err");

        //sort ascending=true
        Collections.sort(listFiles, new FileComparatorByName(true));

        for (File f : listFiles)
        {
            //Read file
            String fname = f.toString();
            List<List<String>> form = IOtools.readFormattedText(
                                                fname,
                                                ":", //key-value separator
                                                "#", //comment
                                                "$START", //start multiline
                                                "$END"); //end multiline

/*
            String refName = "";
            String action = "";
            List<String> errLines = new List<String>();
            List<String> conditions = new List<String>();
            Map<String,String> details = new HashMap<String,String>();
            boolean refNameFound = false;
            boolean actionFound = false;
            for (int i=0; i<form.size(); i++)
            {
                List<String> signleBlock = form.get(i);
                String key = signleBlock.get(0);
                String value = signleBlock.get(1);

                switch (key) 
                {
                    case "REFERENCENAME":
                        //Reference name of the error
                        if (!refNameFound)
                        {
                            refNameFound = true;
                            refName = value;
                            if (refName.equals(""))
                            {
                                Terminator.withMsgAndStatus("ERROR! Empty "   
                                        + " 'REFERENCENAME' in " + fname, -1);
                            }
                        } else {
                            Terminator.withMsgAndStatus("ERROR! Multiple "
                                + "'REFERENCENAME' in " + fname, -1);
                        }
                        break;

                    case "ERRORMESSAGE":
                        //Lines of the error message
                        value = value.trim();
                        errLines.add(value);
                        break;

                    case "ACTION":
                        //Action
                        if (!actionFound)
                        {
                            actionFound = true;
                            action = value;
                            if (action.equals(""))
                            {
                                Terminator.withMsgAndStatus("ERROR! Empty "
                                        + " 'ACTION' in " + fname, -1);
                            }
                        } else {
                            Terminator.withMsgAndStatus("ERROR! Multiple "
                                + "'ACTION' in " + fname, -1);
                        }
                        break;
                        
                    case "CONDITION":
                        conditions.add(value);
                        break;
                    default:
                        //Other details added as string with label
                        details.put(key,value);
                } //end of switch
            } //end of loop on array of pairs key:value
            
            //Create the object known error
            ErrorMessage em = new ErrorMessage(refName,errLines,conditions,
                                                                action,details);
*/

            //Create the object known error
            ErrorMessage em = new ErrorMessage(form);

            listErrors.add(em);
        }
        return listErrors;
    }

//------------------------------------------------------------------------------

}
