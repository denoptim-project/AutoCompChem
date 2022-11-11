package autocompchem.files;

/*
 *   Copyright (C) 2016  Marco Foscato
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for file utils.
 * 
 * @author Marco Foscato
 */

public class FileUtilsTest 
{
	private static String fileSeparator = System.getProperty("file.separator");
	
    @Test
    public void testGetExtension() throws Exception
    {
        File f = new File("./tmp/__ tmp_acc_junit. srtg.tre.ext");
        assertEquals(".ext",FileUtils.getFileExtension(f),
        		"Extention of filename with many dots");
        
        f = new File("/tmp/ __tmp_acc_junit.ext");
        assertEquals(".ext",FileUtils.getFileExtension(f),
        		"Extention of filename with one dot");
        
        f = new File("/tmp/__tmp_acc_junitext");
        assertEquals(null,FileUtils.getFileExtension(f),
        		"Extention of filename with no dot");
    }
    
    @Test
    public void testGetPathToPatent() throws Exception
    {
        String a = "/path/to/me";
        assertEquals(fileSeparator + "path" + fileSeparator + "to",
        		FileUtils.getPathToPatent(a),
        		"Absolute pathname");
        
        a = "../path/../to/me";
        assertEquals(".." + fileSeparator + "path" + fileSeparator + ".." 
        		+ fileSeparator + "to",FileUtils.getPathToPatent(a),
        		"Relative pathname");
        
        a = "me";
        assertEquals(".",FileUtils.getPathToPatent(a),
        		"Only file name without path");
    }

}
