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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Tools for handling resources
 * 
 * @author Marco Foscato
 */

public class ResourcesTools
{
	
//------------------------------------------------------------------------------

	/**
	 * Looks for all resources under a given entry point defined by the "path",
	 * i.e., the base path in the resources tree.
	 * @param basePath the base path in the resources tree
	 * @return the list of streams.
	 * @throws IOException
	 */
	public static List<InputStream> getAllResourceStreams(String basePath) 
			throws IOException
	{
	    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
	    
	    // Use wildcard pattern to get all resources recursively
	    String pattern = "classpath*:" + basePath + "/**/*";
	    Resource[] resources = resolver.getResources(pattern);
	    
	    List<InputStream> streams = new ArrayList<>();
		for (Resource resource : resources) {
			if (resource.isReadable()) {
				try {
					// For file system resources, check if it's a directory
					if (resource.getFile() != null && resource.getFile().isDirectory()) {
						continue;
					}
				} catch (IOException e) {
					// This is expected for JAR resources - they don't have File objects
					// For JAR resources, check if filename suggests it's a file
					String filename = resource.getFilename();
					if (filename == null || filename.isEmpty()) {
						continue;
					}
				}
				streams.add(resource.getInputStream());
			}
		}
	    return streams;
	}
	
//------------------------------------------------------------------------------

}
