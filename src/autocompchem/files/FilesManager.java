package autocompchem.files;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;

import autocompchem.run.Terminator;

/**
 * Tool for managing files and folder trees
 * 
 * @author Marco Foscato
 */


public class FilesManager
{

//------------------------------------------------------------------------------

    /**
     * Construct an empty FilesManager
     */

    public FilesManager()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Return the root of the filename, which is the filename without path and 
     * without extension
     * @param filename file name that may include the path
     * @return the root of the filename
     */

    public static String getRootOfFileName(String filename)
    {
        File f = new File(filename);
        String name = f.getName();
        String root = name;
        int lastId = name.lastIndexOf(".");
        if (lastId > -1)
        {
            root = name.substring(0,lastId);
        }
        return root;
    }

//------------------------------------------------------------------------------

    /**
     * Find all files in a folder tree and keep only those with a filename 
     * containing the given string. It assumes an existing folder if given
     * as argument.
     * @param path root folded from where the search should start
     * @param str string to be contained in the target file's name. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @return the list of files
     */

    public static ArrayList<File> find(String path, String str)
    {
        String originalStr = str;
        boolean starts = false;
        boolean ends = false;
        boolean mid = true;
        if (str.startsWith("*") && (!str.endsWith("*")))
        {
            ends = true;
            mid = false;
            str = str.substring(1);        
        } else if (str.endsWith("*") && (!str.startsWith("*")))
        {
            starts = true;
            mid = false;
            str = str.substring(0,str.length() - 1);
        } else if (str.startsWith("*") && str.endsWith("*"))
        {
            starts = false;
            ends = false;
            mid = true;
            str = str.substring(1,str.length() - 1);
        } else if ((!str.startsWith("*")) && (!str.endsWith("*")))
        {
            starts = false;
            ends = false;
            mid = true;
        }

        //Get the root for this search
        File root = new File(path);

        //Get the list of files in it
        ArrayList<File> ls = new ArrayList<File>(Arrays.asList(
                                                             root.listFiles()));

        //Loop on files in root and collect targets
        ArrayList<File> targets = new ArrayList<File>();
        for (File f : ls)
        {
            if (f.isDirectory())
            {
                //recursion for forlders
                ArrayList<File> fromInnerLevel = find(f.toString(),originalStr);
                targets.addAll(fromInnerLevel);
            } else {
                if (starts)
                {
                    if (f.getName().startsWith(str))
                        targets.add(f);
                } else if (ends) 
                {
                    if (f.getName().endsWith(str))
                        targets.add(f);
                } else if (mid)
                {
                    if (f.getName().contains(str))
                        targets.add(f);
                }
            }
        }
        return targets;
    }

//------------------------------------------------------------------------------

    /**
     * Terminates if file exists
     * @param filename path/name of the file that must not exist
     */

    public static void mustNotExist(String filename)
    {
        if (filename == null)
        {
            Terminator.withMsgAndStatus("ERROR! Attempt to check for a "
                  + "file but 'null' is given as name",-1);
        }

        File f = new File(filename);
        if (f.exists())
        {
            Terminator.withMsgAndStatus("ERROR! File " +  filename
                  + " exists and this software doesn't have rights "
                  + "to overwrite files.",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Check existence and r/w/x permission of a file/folder. The execution
     * will be stopped if the file does not exist or doesn't have the required
     * permissions
     * @param path filename (with adsolute or relative path) to check
     * @param r set to <code>true</code> to check for READ right
     * @param w set to <code>true</code> to check for WRITE right
     * @param x set to <code>true</code> to check for EXECUTE right
     */

    public static void foundAndPermissions(String path, 
                                                boolean r, boolean w, boolean x)
    {

        File root = new File(path);

        if (!root.exists())
        {
            Terminator.withMsgAndStatus("ERROR! File or folder " + path
                                + " does not exist!",-1);
        }

        //Check read is required
        if (r)
        {
            if (!root.canRead())
            {
                Terminator.withMsgAndStatus("ERROR! File or folder " + path
                                + " exists but is not readable!",-1);
            }
        }

        //Check write if required
        if (w)
        {
            if (!root.canWrite())
            {
                Terminator.withMsgAndStatus("ERROR! File or folder " + path
                                + " exists but is not writable!",-1);
            }
        }

        //Check executable if required
        if (x)
        {
            if (!root.canExecute())
            {
                Terminator.withMsgAndStatus("ERROR! File/folder " + path 
                                + " exists but is not executable!",-1);
            }
        }
    }

//------------------------------------------------------------------------------

}
