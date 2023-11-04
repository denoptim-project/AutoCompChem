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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.io.IOtools;


/**
 * Unit Test for file utils.
 * 
 * @author Marco Foscato
 */

public class FileUtilsTest 
{
	private static String fileSeparator = System.getProperty("file.separator");

    @TempDir 
    protected File tempDir;

//------------------------------------------------------------------------------

	@Test
    public void testFind() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
		String basePath = tempDir.getAbsolutePath() + fileSeparator ;
    	for (int i=0; i<3; i++)
    	{
    		IOtools.writeTXTAppend(
    				new File(basePath + "ttt"+i+".log"), "text", false);
    		IOtools.writeTXTAppend(
    				new File(basePath + i), "text", false);
    	}
    	for (int i=0; i<3; i++)
    	{
    		assertEquals(2, FileUtils.find(tempDir, i+"").size());
    	}
    	assertEquals(3, FileUtils.find(tempDir, "*.log").size());
    	assertEquals(3, FileUtils.find(tempDir, "ttt*").size());
		
    	for (int i=0; i<3; i++)
    	{
    		File folder = new File(basePath + "tt" + i);
    		folder.mkdir();
    		IOtools.writeTXTAppend(new File(
    				folder.getAbsolutePath() + fileSeparator + "ttt"+i+".in"), 
    				"text", false);
    	}
    	assertEquals(9, FileUtils.find(tempDir, "tt*", true).size());
    	assertEquals(6, FileUtils.find(tempDir, "tt*", false).size());
    	assertEquals(6, FileUtils.find(tempDir, "ttt*").size());
    	assertEquals(3, FileUtils.find(tempDir, "*.log").size());
    	assertEquals(3, FileUtils.find(tempDir, "*.in").size());
    	assertEquals(0, FileUtils.find(tempDir, "*.*", 0, true).size());
    	assertEquals(3, FileUtils.find(tempDir, "*.*", 1, true).size());
    	assertEquals(6, FileUtils.find(tempDir, "*.*", 2, true).size());
    	assertEquals(3, FileUtils.find(tempDir, "ttt*", 1, true).size());
    	assertEquals(6, FileUtils.find(tempDir, "ttt*", 2, true).size());
    }

//------------------------------------------------------------------------------

	@Test
    public void testFind2() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

		String basePath = tempDir.getAbsolutePath() + fileSeparator ;
	
		File d1Ad1A = new File(basePath + "sub1_abc" + fileSeparator + "subsub1_abc");
		d1Ad1A.mkdirs();
				
		File d2Ad2A = new File(basePath + "sub2_abc" + fileSeparator + "subsub2_abc");
		d2Ad2A.mkdirs();
		
    	File d1Ad1D = new File(basePath + "sub1_abc" + fileSeparator + "subsub1_def");
    	d1Ad1D.mkdirs();
    	
    	File d1Dd1D = new File(basePath + "sub1_def" + fileSeparator + "subsub1_def");
    	d1Dd1D.mkdirs();

		IOtools.writeTXTAppend(new File(basePath + "file_abc"), "text", false);
		IOtools.writeTXTAppend(new File(d1Ad1A.getAbsolutePath() 
				+ fileSeparator + "file_abc"), "text", false);
		IOtools.writeTXTAppend(new File(d1Ad1A.getParentFile().getAbsolutePath() 
				+ fileSeparator + "file_abc"), "text", false);
		IOtools.writeTXTAppend(new File(d2Ad2A.getAbsolutePath() 
				+ fileSeparator + "file_abc"), "text", false);
		IOtools.writeTXTAppend(new File(d2Ad2A.getParentFile().getAbsolutePath() 
				+ fileSeparator + "file_abc"), "text", false);
		
		IOtools.writeTXTAppend(new File(basePath + "file_def"), "text", false);
		IOtools.writeTXTAppend(new File(d1Ad1A.getParentFile().getAbsolutePath() 
				+ fileSeparator + "file_def"), "text", false);
		IOtools.writeTXTAppend(new File(d1Ad1A.getAbsolutePath() 
				+ fileSeparator + "file_def"), "text", false);
		IOtools.writeTXTAppend(new File(d1Dd1D.getAbsolutePath() 
				+ fileSeparator + "file_def"), "text", false);
		
    	/* 
   	 FILES
file_abc
file_def
sub1_abc/subsub1_abc/file_abc
sub1_abc/subsub1_abc/file_def
sub1_abc/file_abc
sub1_abc/file_def
sub1_def/subsub1_def/file_def
sub2_abc/subsub2_abc/file_abc
sub2_abc/file_abc

		DIRECTORIES
sub1_abc
sub1_abc/subsub1_def
sub1_abc/subsub1_abc
sub1_def
sub1_def/subsub1_def
sub2_abc
sub2_abc/subsub2_abc
   	 */
		
		assertEquals(5, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*", true).size());
		assertEquals(7, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*", true).size());
		assertEquals(4, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/*", true).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*", false).size());
		assertEquals(3, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*", false).size());
		assertEquals(4, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/*", false).size());
		
		assertEquals(3, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*ab*", true).size());
		assertEquals(4, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*ab*", true).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/*ab*", true).size());
		assertEquals(1, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*ab*", false).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*ab*", false).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/*ab*", false).size());
		
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "file*", true).size());
		assertEquals(3, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/file*", true).size());
		assertEquals(4, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/file*", true).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "file*", false).size());
		assertEquals(3, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/file*", false).size());
		assertEquals(4, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/file*", false).size());
		
		assertEquals(1, FileUtils.find2(tempDir, Integer.MAX_VALUE, "file_abc", true).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/file_abc", true).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/file_abc", true).size());
		assertEquals(1, FileUtils.find2(tempDir, Integer.MAX_VALUE, "file_abc", false).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/file_abc", false).size());
		assertEquals(2, FileUtils.find2(tempDir, Integer.MAX_VALUE, "*/*/file_abc", false).size());
		
    }
	
//------------------------------------------------------------------------------
	
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
    
//------------------------------------------------------------------------------
    
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
