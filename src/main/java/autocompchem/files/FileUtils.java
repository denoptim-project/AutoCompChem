package autocompchem.files;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hp.hpl.jena.reasoner.FinderUtil;

import autocompchem.run.Terminator;

/**
 * Tool for managing files and folder trees
 * 
 * @author Marco Foscato
 */


public class FileUtils
{

//------------------------------------------------------------------------------

    /**
     * Construct an empty FilesManager
     */

    public FileUtils()
    {
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Return the pathname of the parent folder, if possible. If the given 
     * pathname does not define a parent then it returns ".".
     * @param pathname
     * @return the pathname to the parent or ".".
     */
    public static String getPathToPatent(String pathname)
    {
    	File f = new File(pathname);
    	String path = ".";
    	String parent = f.getParent();
    	if (parent != null)
    	{
    		path = parent;
    	}
    	return path;
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
     * Find all files in a folder tree and keep only those with a 
     * filename 
     * containing the given string. It assumes an existing folder if given
     * as argument. This method does not lists folders. 
     * See {@link #find(File, String, boolean)} to include also folders.
     * @param path root folder from where the search should start
     * @param str string to be contained in the target file's name. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @return the list of files
     */

    public static List<File> find(String path, String str)
    {
    	return find(new File(path), str, false);
    }

  //------------------------------------------------------------------------------

    /**
     * Find all files in a folder tree and keep only those with a 
     * filename 
     * containing the given string. It assumes an existing folder if given
     * as argument. This method does not list folders. 
     * See {@link #find(File, String, boolean)} to include also folders.
     * @param root folder from where the search should start
     * @param str string to be contained in the target file's name. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @return the list of files
     */

    public static List<File> find(File root, String str)
    {
    	return find(root, str, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Find all files/folders in a folder tree and keep only those with a 
     * filename 
     * containing the given string. It assumes an existing folder is given
     * as argument.
     * @param root folder from where the search should start
     * @param str string to be contained in the target file's name. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @param collectFolders <code>true</code> to collect also folders.
     * @return the list of files
     */

    public static List<File> find(File root, String str, boolean collectFolders)
    {
    	return find(root, str, Integer.MAX_VALUE, collectFolders);
    }
    
//------------------------------------------------------------------------------

    /**
     * Find all files/folders in a folder tree and keep only those with a 
     * filename 
     * containing the given string. It assumes an existing folder is given
     * as argument.
     * @param root folder from where the search should start
     * @param pattern string to be contained in the target file's name. '*' is used
     * as usual to specify the continuation of the string with any number of 
     * any character. Use it only at the beginning (*blabla), at the end 
     * (blabla*), or in both places (*blabla*). If no '*' is given the third 
     * case is chosen by default: filename must contain the query string.
     * @param collectFolders <code>true</code> to collect also folders.
     * @return the list of files
     */

    public static List<File> find(File root, String pattern, Integer maxdepth, 
    		boolean collectFolders)
    {
    	boolean starts = false;
        boolean ends = false;
        boolean mid = true;
        if (pattern.startsWith("*") && (!pattern.endsWith("*")))
        {
            ends = true;
            mid = false;
            pattern = pattern.substring(1);        
        } else if (pattern.endsWith("*") && (!pattern.startsWith("*")))
        {
            starts = true;
            mid = false;
            pattern = pattern.substring(0,pattern.length() - 1);
        } else if (pattern.startsWith("*") && pattern.endsWith("*"))
        {
            starts = false;
            ends = false;
            mid = true;
            pattern = pattern.substring(1,pattern.length() - 1);
        } else if ((!pattern.startsWith("*")) && (!pattern.endsWith("*")))
        {
            starts = false;
            ends = false;
            mid = true;
        }

        String finalPattern = pattern;
        boolean finalStart = starts;
        boolean finalMid = mid;
        boolean finalEnds = ends;
        List<File> result = new ArrayList<File>();
        try {
			Files.walk(root.toPath(), maxdepth)
				.filter(p -> !p.equals(root.toPath()))
				.filter(p -> matchesPattern(p,
						finalPattern, finalStart, finalMid, finalEnds, 
						collectFolders))
				.forEach(p -> result.add(p.toFile()));
		} catch (IOException e) {
			return result;
		}
        
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks is a path matches the criteria for collecting it.
     * @param path the candidate path
     * @param pattern the pattern to match (without wild card)
     * @param starts if the pattern should be at the beginning of the file name.
     * @param contains if the pattern should be in the mids of the file name.
     * @param ends if the pattern should be at the end of the file name.
     * @param collectFolders if folders should be collected as well.
     * @return <code>true</code> if the path matches the criteria.
     */
    private static boolean matchesPattern(Path path,
    		String pattern, boolean starts, boolean contains, boolean ends,
    		boolean collectFolders)
    {
    	File file = path.toFile();
    	if (file.isDirectory() && !collectFolders)
        {
    		return false;
        }
        if (starts)
        {
            return file.getName().startsWith(pattern);
        } else if (ends) 
        {
            return file.getName().endsWith(pattern);
        } else if (contains)
        {
            return file.getName().contains(pattern);
        }
        return false;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Extracts the extension from the file name
     * @param f the file.
     * @return the extension or null is the filename does not contain any dot.
     */
    
    public static String getFileExtension(File f)
    {
    	String ext = null;
    	String fname = f.getName();
    	if (fname.contains("."))
    	{
    		ext = fname.substring(fname.lastIndexOf("."));
    	}
    	
    	return ext;
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
