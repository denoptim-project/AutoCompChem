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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import autocompchem.io.IOtools;
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
        return getRootOfFileName(new File(filename));
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the root of the filename, which is the filename without path and 
     * without extension
     * @param file the file to analyze
     * @return the root of the filename
     */

    public static String getRootOfFileName(File file)
    {
        String name = file.getName();
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
     * Finds files and folders that have a relative pathname matching the given 
     * REGEX pattern.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @return the list of file that match the criteria.
     * @throws IOException
     */

    public static List<File> findByREGEX(String root, String pattern)
    {
    	return findByREGEX(new File(root), pattern, false);
    }

//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the given 
     * REGEX pattern.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @return the list of file that match the criteria.
     * @throws IOException
     */

    public static List<File> findByREGEX(File root, String pattern)
    {
    	return findByREGEX(root, pattern, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the given 
     * REGEX pattern.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @param collectFolders use <code>true</code> to collect both files and
     * directories.
     * @return the list of file that match the criteria.
     * @throws IOException
     */

    public static List<File> findByREGEX(File root, String pattern, 
    		boolean collectFolders)
    {
    	return findByREGEX(root, pattern, Integer.MAX_VALUE, collectFolders);
    }
    
//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the 
     * pattern resulting
     *  by appending the given REGEX to the absolute pathname of the root 
     *  folder.
     * @param root file system location from which to start searching.
     * @param regex the patter to find in filenames.
     * @param maxdepth maximum depth in folder tree starting from the root.
     * @param collectFolders use <code>true</code> to collect both files and
     * directories.
     * See {@link FileSystem#getPathMatcher(String)}
     * @return the list of file that match the criteria.
     */
    public static List<File> findByREGEX(File root, String pattern, 
    		Integer maxdepth, boolean collectFolders)
    {
    	return find(root, pattern, maxdepth, collectFolders, "regex");
    }
    
//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the given
     * glob pattern.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @return the list of file that match the criteria.
     * @throws IOException
     */

    public static List<File> findByGlob(String root, String pattern)
    {
    	return findByGlob(new File(root), pattern, false);
    }

//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the given
     * glob pattern.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @return the list of file that match the criteria.
     * @throws IOException
     */

    public static List<File> findByGlob(File root, String pattern)
    {
    	return findByGlob(root, pattern, false);
    }

//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the given
     * glob pattern.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @param collectFolders use <code>true</code> to collect both files and
     * directories.
     * @return the list of file that match the criteria.
     * @throws IOException
     */

    public static List<File> findByGlob(File root, String pattern, 
    		boolean collectFolders)
    {
    	return find(root, pattern, Integer.MAX_VALUE, collectFolders, "glob");
    }
   
//------------------------------------------------------------------------------

    /**
     * Finds files and folders that have a relative pathname matching the 
     * pattern resulting
     *  by appending the given pattern to the absolute pathname of the root 
     *  folder and interpreting it as a 'regex' or a 'glob' according to
     *  the 'mode' parameter.
     * @param root file system location from which to start searching.
     * @param pattern the patter to find in filenames.
     * @param maxdepth maximum depth in folder tree starting from the root.
     * @param collectFolders use <code>true</code> to collect both files and
     * directories.
     * @param mode whether 'regex' or 'glob'. 
     * See {@link FileSystem#getPathMatcher(String)}
     * @return the list of file that match the criteria.
     */
    public static List<File> find(File root, String pattern, Integer maxdepth, 
    		boolean collectFolders, String mode)
    {
    	switch (mode.toUpperCase())
    	{
    		case "REGEX":
    			pattern = pattern.replace("\\", "\\\\");
    			break;
    			
    		case "GLOB":
    			pattern = root.getPath() + File.separator + pattern;
    			pattern = pattern.replace("\\", "\\\\");
    			break;
    			
    		default:
    			Terminator.withMsgAndStatus("ERROR! "
    					+ "The mode for finding files can "
        				+ "only be 'regex' or 'glob', but you asked for '" 
    					+ mode + "'", -1);
    			break;
    	}
    	
    	PathMatcher matcher = FileSystems.getDefault().getPathMatcher(
    			mode.toLowerCase() + ":" + pattern);
    	List<Path> paths;
		try {
			paths = Files.find(root.toPath(), maxdepth,
			        (path, basicFileAttributes) ->  matcher.matches(path))
					.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			paths = new ArrayList<Path>();
		}
    	
    	if (!collectFolders)
    	{
	    	paths = paths.stream()
	    			.filter(p -> !Files.isDirectory(p, 
	    					LinkOption.NOFOLLOW_LINKS))
	    			.collect(Collectors.toList());
    	}
    	
    	List<File> files = new ArrayList<File>();
    	paths.stream().forEach(p -> files.add(new File(p.toString())));
    	return files;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Extracts the extension from the file name
     * @param f the file.
     * @return the extension starting with a dot or <code>null</code> if the 
     * filename does not contain any dot.
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
     * Creates an id-specific pathname from a given pathname and an id. Adds the
     * identifier to the last location in the
     * original pathname before the last extension (i.e., last ".").
     * @param f original pathname to be modified.
     * @param id the identifier making the original pathname unique. NB: this
     * method does not check for uniqueness.
     * @return the pathname resulting from appending the identifier to the 
     * original pathname before the extension (i.e., last ".").
     */
    public static String getIdSpecPathName(String pathName, String id)
    {
    	return getIdSpecPathName(new File(pathName), id);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates an id-specific pathname from a given pathname and an id. Adds the
     * identifier to the last location in the
     * original pathname before the last extension (i.e., last ".").
     * @param f original pathname to be modified.
     * @param id the identifier making the original pathname unique. NB: this
     * method does not check for uniqueness.
     * @return the pathname resulting from appending the identifier to the 
     * original pathname before the extension (i.e., last ".").
     */
    public static String getIdSpecPathName(File f, String id)
    {
    	String ext = "";
    	String fname = f.getName();
    	String newPathName = fname;
    	
    	if (fname.contains("."))
    	{
    		ext = fname.substring(fname.lastIndexOf("."));
    		newPathName = fname.substring(0,fname.lastIndexOf("."));
    	}
    	
    	String parent = f.getParent();
    	if (parent!=null)
    		newPathName = parent + File.separator + newPathName;
    	
    	return newPathName + "_" + id + ext;
    }

//------------------------------------------------------------------------------

    /**
     * Terminates if file exists
     * @param outFile path/name of the file that must not exist
     */

    public static void mustNotExist(String pathname)
    {
    	mustNotExist(new File(pathname));
    }
    
//------------------------------------------------------------------------------

    /**
     * Terminates if file exists
     * @param outFile the file that must not exist
     */

    public static void mustNotExist(File outFile)
    {
        if (outFile == null)
        {
            Terminator.withMsgAndStatus("ERROR! Attempt to check for a "
                  + "file but 'null' is given as name",-1);
        }

        if (outFile.exists())
        {
            Terminator.withMsgAndStatus("ERROR! File " +  outFile
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
    	foundAndPermissions(new File(path), r, w, x);
    }
    
//------------------------------------------------------------------------------

    /**
     * Check existence and r/w/x permission of a file/folder. The execution
     * will be stopped if the file does not exist or doesn't have the required
     * permissions
     * @param file the file/directory defined from relative or absolute path.
     * @param r set to <code>true</code> to check for READ right
     * @param w set to <code>true</code> to check for WRITE right
     * @param x set to <code>true</code> to check for EXECUTE right
     */

    public static void foundAndPermissions(File file,
    		boolean r, boolean w, boolean x)
    {
        if (!file.exists())
        {
            Terminator.withMsgAndStatus("ERROR! File or folder " + file
                                + " does not exist!",-1);
        }

        //Check read is required
        if (r)
        {
            if (!file.canRead())
            {
                Terminator.withMsgAndStatus("ERROR! File or folder " + file
                                + " exists but is not readable!",-1);
            }
        }

        //Check write if required
        if (w)
        {
            if (!file.canWrite())
            {
                Terminator.withMsgAndStatus("ERROR! File or folder " + file
                                + " exists but is not writable!",-1);
            }
        }

        //Check executable if required
        if (x)
        {
            if (!file.canExecute())
            {
                Terminator.withMsgAndStatus("ERROR! File/folder " + file 
                                + " exists but is not executable!",-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Replace a REGEX-defined pattern with a string into a text file.
     * Corresponds to the 'sed' command in Unix.
     * @param file the text file to work on.
     * @param regex the REGEX defining the patters to replace
     * @param newString the new string to replace the pattern with.
     */
	public static void replaceString(File file, Pattern regex, String newString) 
	{
        List<String> contents = IOtools.readTXT(file);
        for (int i=0; i<contents.size(); i++)
        {
        	contents.set(i, regex.matcher(contents.get(i)).replaceAll(
        			newString));
        }
        IOtools.writeTXTAppend(file, contents, false);
	}

//------------------------------------------------------------------------------
	
	/**
	 * Copies files or folders.
	 * @param sourcePath
	 * @param destPath
	 * @throws IOException
	 */
	public static void copy(String sourcePath, String destPath) throws IOException 
	{
		copy(new File(sourcePath), new File(destPath));
	}

//------------------------------------------------------------------------------
	
	/**
	 * Copies files or folders.
	 * @param sourcePath
	 * @param destPath
	 * @throws IOException
	 */
	public static void copy(File source, File dest) throws IOException 
	{ 
	    if (source.isDirectory())
	    {
	    	if (!dest.exists()) 
	    	{
	    		dest.mkdirs();
	    	}
	        File[] files = source.listFiles();
	        if (files != null) 
	        {
	            for (File file : files) 
	            {
	                if (file.isFile()) 
	                {
	                	com.google.common.io.Files.copy(file, new File(dest, 
	                			file.getName()));
	                } else if (file.isDirectory()) {
	                    // Recursive copy for subdirectories
	                    copy(file.getAbsolutePath(), new File(dest, 
	                    		file.getName()).getAbsolutePath());
	                }
	            }
	        }
	    } else {
	    	com.google.common.io.Files.copy(source, dest);
	    }
	}
	
//------------------------------------------------------------------------------

}
