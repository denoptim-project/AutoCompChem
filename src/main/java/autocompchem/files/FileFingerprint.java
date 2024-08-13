package autocompchem.files;

/*   
 *   Copyright (C) 2023  Marco Foscato 
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data structure defining how to identify a file (i.e., folder or proper file)
 * as a match.
 * This class defines a relative pathname and REGEX string that have 
 * to be matched into a given size header for the identification to be 
 * successful. The pathname if relative to the file/folder that has to be 
 * identified. Examples:<ul>
 * <li>to identify a file by looking into it searching for a line containing a
 * given REGEX, the pathname should be "."</li>
 * <li>to identify a folder that contains a file that must be name FILE and
 * must contain the REGEX, the pathname should be "./FILE".</li>
 * <li>to identify a folder that contains a file with any name that contains a 
 * line matching the REGEX, the pathname should be "./*".</li>
 * </ul>
 * 
 * @author Marco Foscato
 *
 */

public class FileFingerprint 
{
	
	/**
	 * The relative pathname to the file where the REGEX should be matched.
	 * This is a relative pathname, and is relative to the file/folder that is 
	 * should be identified.
	 */
	public final String PATHNAME;
	
	/**
	 * The number of lines defining the header of the file located at the 
	 * pathname. This is meant to avoid reading too many lines for no reason.
	 */
	public final int HEADLENGTH;
	
	/**
	 * The query to be matched by a line of the header of the file located
	 * at the pathname.
	 */
	public final String REGEX;
	
//------------------------------------------------------------------------------
	
	public FileFingerprint(String path, int headerLength, String regex) 
	{
		this.PATHNAME = path;
		this.HEADLENGTH = headerLength;
		this.REGEX = regex;
	}
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given file matches this fingerprint. The process may imply
     * reading the file or, if the file is a folder, reading files contained
     * by the folder. This method works only for files that are directly
     * embedded in the given folder, not for deeper levels.
     * @param file the file or folder that is a candidate match for this 
     * fingerprint.
     * @return <code>true</code> if the file/folder is a match. 
     * <code>false</code> if there is no such file or if the  file is not a match.
     */
    public boolean matchedBy(File file)
    {
    	if (!file.exists())
    		return false;
    	
    	List<File> targetFiles = new ArrayList<File>();
    	if (PATHNAME.equals("."))
    	{
    		if (file.isDirectory())
    			return false;
    		targetFiles.add(file);
    	} else {
    		if (!file.isDirectory())
    			return false;

        	if (!file.canRead())
        		return false;
        	if (!file.canExecute())
        		return false;
        	
    		String sep = File.separator;
    		String relPath = PATHNAME.replace("\\", sep)
    				.replace("/", sep)
    				.replaceFirst("\\.\\/", "")
    				.replaceFirst("\\.\\\\", "");
    	
    		targetFiles.addAll(FileUtils.findByGlob(file, relPath, false));
    	}

		boolean result = false;
    	for (File fileToRead : targetFiles)
    	{
        	if (!fileToRead.canRead())
        		continue;
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(fileToRead));
			
	    		for(int lineNum=0; lineNum<HEADLENGTH; lineNum++)
	    		{
	    			String line = reader.readLine();
	    			if (line==null)
	    				break;
					if (line.matches(REGEX))
					{
						result = true;
						break;
					}
	    		}
			} catch (IOException e) {
				return false;
			} 
	        finally 
	        {
	            try 
	            {
	                if(reader != null)
	                	reader.close();
	            } 
	            catch (IOException ioe) 
	            {
	            	return false;
	            }
	        }
			if (result)
				break;
    	}
    	return result;
    }
    
//------------------------------------------------------------------------------

}
