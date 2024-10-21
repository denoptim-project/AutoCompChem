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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
		
		String[] pattern = {".*", ".*/.*", ".*", ".*/.*", ".*/.*/.*",
				".*ab.*", ".*ab.*", ".*ab.*", ".*ab.*", ".*ab.*", ".*ab.*",
				"file.*", "file.*", ".*file.*", ".*file.*", ".*file.*", ".*file.*",
				"file_abc", ".*/file_abc", ".*/file_abc",};
		int[] depth = {1, 2, 1, 2, 3,
				1, 2, 3, 1, 2, 3,
				Integer.MAX_VALUE, 2, 3, 1, 2, 3,
				3, 2, 3};
		boolean[] countFldrs = {true, true, false, false, false, //  0-4
				true, true, true, false, false, false,           //  5-10
				true, true, true, false, false, false,           // 11-16
				true, true, true,                                // 17-
				};
		int[] expected = {6, 13, 2, 5, 9,
				3, 9, 12, 1, 4, 7,
				0, 0, 9, 2, 5, 9,
				0, 3, 5};
		//NB: you can use this to print the actual matches
		boolean debugLog = false;
		for (int i=0; i<expected.length; i++)
		{
			List<File> matches = FileUtils.findByREGEX(tempDir, 
					pattern[i], depth[i], countFldrs[i]);
			if (debugLog)
			{
				System.out.println("Pattern: '" + pattern[i]+"'");
				System.out.println("Depth: " + depth[i]);
				System.out.println("Count Folders: "+ countFldrs[i]);
				final String label = i + " ->"; 
				matches.forEach(f -> System.out.println(label + f));
			}
			assertEquals(expected[i], matches.size());
		}
		
		
		String[] patternG = {"*", "*/*", "*/*/*", "*", "*/*", "*/*/*", 
				"*ab*", "*/*ab*", "*/*/*ab*", "*ab*", "*/*ab*", "*/*/*ab*",
				"file*", "*/file*", "*/*/file*", "file*", "*/file*", "*/*/file*",
				"file_abc", "*/file_abc", "*/*/file_abc",
				"file_abc", "*/file_abc", "*/*/file_abc"};
		boolean[] countFldrsG = {true, true, true, false, false, false,
				true, true, true, false, false, false,
				true, true, true, false, false, false,
				true, true, true, 
				false, false, false};
		int[] expectedG = {5, 7, 4, 2, 3, 4,
				3, 4, 2, 1, 2, 2,
				2, 3, 4, 2, 3, 4,
				1, 2, 2, 
				1, 2, 2};
		//NB: you can use this to print the actual matches
		debugLog = false; 
		for (int i=0; i<expectedG.length; i++)
		{
			List<File> matches = FileUtils.findByGlob(tempDir, 
					patternG[i], countFldrsG[i]);
			if (debugLog)
			{
				System.out.println("Pattern: '" + patternG[i]+"'");
				System.out.println("Count Folders: "+ countFldrsG[i]);
				final String label = i + " ->"; 
				matches.forEach(f -> System.out.println(label + f));
			}
			assertEquals(expectedG[i], matches.size());
		}
    }
	
//------------------------------------------------------------------------------
	
	/*
	 * This is only meant to test the differences between regex- and glob-based
	 * path matcher.
	 */
	@Test
	public void testPathMatcher() throws Exception
	{
		String filename = "name";
		String pathStr = "_sep_first_sep_second_sep_third_sep_" + filename;
		pathStr = pathStr.replaceAll("_sep_", File.separator);
		File file = new File(pathStr);
		Path path = file.toPath();
		
		// Both work with query that is the abs pathname (only directed)
		PathMatcher regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:" + pathStr);
		assertTrue(regexMatch.matches(path));
		PathMatcher globMatch = FileSystems.getDefault().getPathMatcher(
    			"glob:" + pathStr);
		assertTrue(globMatch.matches(path));
		
		// Pathname with dots
		filename = "name-1.ext";
		pathStr = "_sep_first_sep_second_sep_other_sep_.._sep_third_sep_" + filename;
		pathStr = pathStr.replaceAll("_sep_", File.separator);
		file = new File(pathStr);
		path = file.toPath();
		regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:" + pathStr);
		assertTrue(regexMatch.matches(path));
		globMatch = FileSystems.getDefault().getPathMatcher(
    			"glob:" + pathStr);
		assertTrue(globMatch.matches(path));
		
		// Relative pathname 
		pathStr = ".._sep_.._sep_second_sep_third_sep_" + filename;
		pathStr = pathStr.replaceAll("_sep_", File.separator);
		file = new File(pathStr);
		path = file.toPath();
		regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:" + pathStr);
		assertTrue(regexMatch.matches(path));
		globMatch = FileSystems.getDefault().getPathMatcher(
    			"glob:" + pathStr);
		assertTrue(globMatch.matches(path));
		
		// Wildcard pathname 
		pathStr = ".._sep_.._sep_second_sep_third_sep_" + filename;
		pathStr = pathStr.replaceAll("_sep_", File.separator);
		file = new File(pathStr);
		path = file.toPath();
		regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:.*name.*");
		assertTrue(regexMatch.matches(path));
		regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:.*");
		assertTrue(regexMatch.matches(path));
		regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:.*ext");
		assertTrue(regexMatch.matches(path));
		regexMatch = FileSystems.getDefault().getPathMatcher(
    			"regex:.*.ext");
		assertTrue(regexMatch.matches(path));
		// wildcard in glob does not go beyond file separator
		globMatch = FileSystems.getDefault().getPathMatcher(
    			"glob:*name*");
		assertFalse(globMatch.matches(path));
		globMatch = FileSystems.getDefault().getPathMatcher(
    			"glob:"
    			+ "*" + File.separator + "*name*");
		assertFalse(globMatch.matches(path));
		globMatch = FileSystems.getDefault().getPathMatcher(
    			"glob:"
    			+ "*" + File.separator 
    			+ "*" + File.separator 
    			+ "*" + File.separator 
    			+ "*" + File.separator + "*name*");
		assertTrue(globMatch.matches(path));
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
    public void testReplaceString() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
	
		File testFile = new File(tempDir.getAbsolutePath() + fileSeparator + 
				"fileToBeEdited");
		List<String> originalText = new ArrayList<String>();
		originalText.add("Line one");
		originalText.add("Line two with PatteRN to edit");
		originalText.add("Line three");
		originalText.add("Line four with PatteeeeeeRN to edit");
		IOtools.writeTXTAppend(testFile, originalText, true);
		
		String newString = "my new string";
		
		FileUtils.replaceString(testFile, 
				Pattern.compile("[PpQq].tt.*[A-Z]N to edit"),
				newString);
		
		assertEquals(2,FileAnalyzer.count(testFile, newString));
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
    
//------------------------------------------------------------------------------
    
}
