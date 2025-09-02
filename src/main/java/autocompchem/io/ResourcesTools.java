package autocompchem.io;

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
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools for handling resources
 * 
 * @author Marco Foscato
 */

public class ResourcesTools
{

//------------------------------------------------------------------------------

	/*
	 * Recursive method to explore resource tree
	 */
	private static void collectResources(File directory, List<String> resources, 
			String prefix) 
	{
	    File[] files = directory.listFiles();
	    if (files != null) 
	    {
	        for (File file : files) 
	        {
	            String resourcePath = prefix + file.getName();
	            if (file.isDirectory()) 
	            {
	                collectResources(file, resources, resourcePath + "/");
	            } else {
	                resources.add(resourcePath);
	            }
	        }
	    }
	}
	
//------------------------------------------------------------------------------

	/**
	 * Returns the list of all resources found under the given path.
	 * @param path the root where to start the search.
	 * @return the list of pathnames for all resources.
	 * @throws URISyntaxException
	 */
	public static List<String> getAllResources(ClassLoader classLoader, 
			String path) throws URISyntaxException
	{
	    List<String> resources = new ArrayList<>();
	    
	    URL resource = classLoader.getResource(path);
	    if (resource != null) 
	    {
	        File directory = new File(resource.toURI());
	        collectResources(directory, resources, "");
	    }
	    
	    return resources;
	}
	
//------------------------------------------------------------------------------

}
