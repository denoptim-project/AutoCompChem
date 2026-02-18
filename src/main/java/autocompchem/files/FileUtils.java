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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import autocompchem.io.IOtools;

/**
 * Tool for managing files and folder trees
 * 
 * @author Marco Foscato
 */


public class FileUtils
{
    
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
     * pattern using OS-independent path matching.
     * @param root file system location from which to start searching.
     * @param pattern the pattern to find in filenames.
     * @param maxdepth maximum depth in folder tree starting from the root.
     * @param collectFolders use <code>true</code> to collect both files and
     * directories.
     * @param mode whether 'regex' or 'glob'. Uses OS-independent path matching
     * that automatically handles path separator differences.
     * @return the list of file that match the criteria.
     */
    public static List<File> find(File root, String pattern, Integer maxdepth, 
    		boolean collectFolders, String mode)
    {
        // Use a hybrid approach: Java NIO for path matching but with OS-independent patterns
        String processedPattern;
        
    	switch (mode.toUpperCase())
    	{
    		case "REGEX":
    			// No normalization needed - patterns should be cross-platform from the start
    			processedPattern = pattern;
    			break;
    			
    		case "GLOB":
    			// For glob, prepend the root path but use forward slashes
    			// GLOB patterns in Java always expect forward slashes, even on Windows
    			String rootPath = root.getPath().replace("\\", "/");
    			processedPattern = rootPath + "/" + pattern;
    			break;
    			
    		default:
    			throw new IllegalArgumentException(
    					"The mode for finding files can "
        				+ "only be 'regex' or 'glob', but you asked for '" 
    					+ mode + "'");
    	}
    	
        // Use Java NIO PathMatcher which handles OS differences automatically
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(
                mode.toLowerCase() + ":" + processedPattern);
        
        List<Path> paths;
        try {
            paths = Files.find(root.toPath(), maxdepth,
                    (path, basicFileAttributes) -> matcher.matches(path))
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
     * Get pathname without the last extension, i.e., the last string adter and
     * including the last index of the dot character.
     * @param file
     */
    
    public static String getFilePathnameWithoutExtension(File file)
    {
    	String fname = file.getName();
    	String newPathName = fname;
    	
    	if (fname.contains("."))
    	{
    		newPathName = fname.substring(0,fname.lastIndexOf("."));
    	}
    	
    	String parent = file.getParent();
    	if (parent!=null)
    	{
    		newPathName = parent + File.separator + newPathName;
    	}
    	return newPathName;
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
    public static String getIdSpecPathName(String pathName, int id)
    {
    	return getIdSpecPathName(new File(pathName), id+"");
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
    	return getIdSpecPathName(f, "_", id);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates an id-specific pathname from a given pathname and an id. Adds the
     * identifier to the last location in the
     * original pathname before the last extension (i.e., last ".").
     * @param f original pathname to be modified.
     * @param sep the separator to use between the file basename 
     * (i.e., the pathname without extension) and the identifier.
     * @param id the identifier making the original pathname unique. NB: this
     * method does not check for uniqueness.
     * @return the pathname resulting from appending the identifier to the 
     * original pathname before the extension (i.e., last ".").
     */
    public static String getIdSpecPathName(File f, String sep, String id)
    {
    	String extension = getFileExtension(f);
    	String newPathName = getFilePathnameWithoutExtension(f) + sep + id;
    	if (extension!=null)
    	{
    		newPathName = newPathName + extension;
    	}
    	return newPathName;
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
            throw new IllegalArgumentException("Attempt to check for a "
                  + "file but 'null' is given as name");
        } else {
            if (outFile.exists())
            {
                throw new IllegalStateException("File " +  outFile
                    + " exists and this software doesn't have rights "
                    + "to overwrite files.");
            }
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
            throw new IllegalArgumentException("File or folder " + file
                                + " does not exist!");
        }

        //Check read is required
        if (r)
        {
            if (!file.canRead())
            {
                throw new SecurityException("File or folder " + file
                                + " exists but is not readable!");
            }
        }

        //Check write if required
        if (w)
        {
            if (!file.canWrite())
            {
                throw new SecurityException("File or folder " + file
                                + " exists but is not writable!");
            }
        }

        //Check executable if required
        if (x)
        {
            if (!file.canExecute())
            {
                throw new SecurityException("File/folder " + file 
                                + " exists but is not executable!");
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
	 * @param sourcePath the file/folder to be copied
	 * @param destPath the destination for the content of the source file/folder. 
	 * This path is created, if not existing.
	 * @throws IOException
	 */
	public static void copy(String sourcePath, String destPath) throws IOException 
	{
		copy(new File(sourcePath), new File(destPath));
	}

//------------------------------------------------------------------------------
	
	/**
	 * Copies files or folders.
	 * @param sourcePath the file/folder to be copied
	 * @param destPath the destination for the content of the source file/folder. 
	 * This path is created, if not existing.
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
	
	/**
	 * Delete files or folders even if not empty
	 * @param file the file/folder to remove
	 * @throws IOException
	 */
	public static void delete(String filePath) throws IOException 
	{
		delete(new File(filePath));
	}

//------------------------------------------------------------------------------
	
	/**
	 * Delete files or folders even if not empty
	 * @param file the file/folder to remove
	 * @throws IOException 
	 */
	public static void delete(File file) throws IOException
	{ 
		if (file.isDirectory())
		{
			org.apache.commons.io.FileUtils.deleteDirectory(file);
		} else {
			file.delete();
		}
	}

//------------------------------------------------------------------------------

	/**
	 * Checks if a string represents a valid path.
	 * Returns false if the string contains wildcards (*,?), regex characters,
	 * or other characters that cannot be used in pathnames.
	 * @param pathname the string to check
	 * @return true if it's a valid path, false otherwise
	 */
	public static boolean isValidPath(String pathname) 
	{
	    if (pathname == null || pathname.trim().isEmpty()) {
	        return false;
	    }
	    
	    // Check for wildcards and invalid characters that cannot be used in pathnames
	    if (pathname.contains("*") || pathname.contains("?") || 
	        pathname.contains("[") || pathname.contains("]") ||
	        pathname.contains("^") || pathname.contains("@")) {
	        return false;
	    }
	    
	    // Try to create a Path object - this will catch invalid path strings
	    try {
	    	Paths.get(pathname);
	    	return true;
	    } catch (Throwable t) {
	    	return false;
	    }
	}

//------------------------------------------------------------------------------

	/**
	 * Checks if a string is a valid relative pathname or not.
	 * @param pathname the string to check
	 * @return true if it's a valid relative pathname, false otherwise
	 */
	public static boolean isRelativePath(String pathname) 
	{
	    // First check if it's a valid path at all
	    if (!isValidPath(pathname)) {
	        return false;
	    }
	    
	    // Then check if it's relative (not absolute)
	    try {
	    	Path path = Paths.get(pathname);
	    	return !path.isAbsolute();
	    } catch (Throwable t) {
	    	return false;
	    }
	}

//------------------------------------------------------------------------------

	/**
	 * Checks if a string is a valid absolute pathname or not.
	 * @param pathname the string to check
	 * @return true if it's a valid absolute pathname, false otherwise
	 */
	public static boolean isAbsolutePath(String pathname) 
	{
	    // First check if it's a valid path at all
	    if (!isValidPath(pathname)) {
	        return false;
	    }
	    
	    // Then check if it's absolute
	    try {
	    	Path path = Paths.get(pathname);
	    	return path.isAbsolute();
	    } catch (Throwable t) {
	    	return false;
	    }
	}

//------------------------------------------------------------------------------

	/**
	 * Gets a path from under the user.dir to a path under a specific location.
	 * @param pathname the pathname to alter. Must be absolute.
	 * @param effectiveUserDir the pathname to the location acting as effective
	 * user dir. Must be absolute.
	 * @return the pathname as it the usr.dir was changed to the location of 
	 * the effective user dir. 
	 */
	public static Path getCustomAbsPath(String pathname, String effectiveUserDir) 
	{
		return getCustomAbsPath(Paths.get(pathname), Paths.get(effectiveUserDir));
	}

//------------------------------------------------------------------------------

	/**
	 * Gets a path from under the user.dir to a path under a specific location.
	 * @param pathname the pathname to alter. Must be absolute.
	 * @param effectiveUserDir the pathname to the location acting as effective
	 * user dir. Must be absolute.
	 * @return the pathname as it the usr.dir was changed to the location of 
	 * the effective user dir. 
	 */
	public static Path getCustomAbsPath(Path filePath, Path customDirPath) 
	{
		Path userDirPath = Paths.get(System.getProperty("user.dir"));
		Path absFilePath = filePath.toAbsolutePath();
		Path absUserDirPath = userDirPath.toAbsolutePath();
		Path absCustomDirPath = customDirPath.toAbsolutePath();
		
		// Check if the file is already under the custom directory
		if (absFilePath.startsWith(absCustomDirPath)) {
			// File is already under customDirPath, return unchanged
			return absFilePath;
		}
		
		// Check if the file path is under the user directory
		if (absFilePath.startsWith(absUserDirPath)) {
			// File is under user.dir, so relativize and resolve under customDirPath
			Path relFilePath = absUserDirPath.relativize(absFilePath);
			Path effectiveFilePath = customDirPath.resolve(relFilePath);
			return effectiveFilePath;
		} else {
			// File is not under user.dir, return the original path unchanged
			return absFilePath;
		}
	}
	
//------------------------------------------------------------------------------

}
