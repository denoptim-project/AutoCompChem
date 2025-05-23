package autocompchem.files;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.io.IOtools;

public class FileFingerprintTest 
{
    public static final String FS = System.getProperty("file.separator");
    public static final String NL = System.getProperty("line.separator");

    @TempDir 
    protected File tempDir;
    
//------------------------------------------------------------------------------

	@Test
    public void testMatchedBy() throws Exception
    {
		FileFingerprint fingerprint = new FileFingerprint(".", 3, "^ QUERY $");
		
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
		
    	
    	// Simple file in "."
    	String testPathA = tempDir.getAbsolutePath() + FS + "t_A";
		File testFileA = new File(testPathA);
		IOtools.writeTXTAppend(testFileA, NL + " QUERY " + NL, false);
		
		String testPathB = tempDir.getAbsolutePath() + FS + "t_B";
		File testFileB = new File(testPathB);
		IOtools.writeTXTAppend(testFileB, NL + " BLA", false);
		
		assertTrue(fingerprint.matchedBy(testFileA));
		assertFalse(fingerprint.matchedBy(testFileB));
		assertFalse(fingerprint.matchedBy(tempDir));
		

		// Any file located at "."
		FileFingerprint fngrprC = new FileFingerprint("." + FS + "*", 3, 
				"^ QUERY $");
		
		assertFalse(fngrprC.matchedBy(testFileA));
		assertFalse(fngrprC.matchedBy(testFileB));
		assertTrue(fngrprC.matchedBy(tempDir));
		
		
		// Nested file with given name
		FileFingerprint fngrprD = new FileFingerprint("." + FS + "dir" + FS 
				+ "t_D", 3, "^ Q $");
		
		String subDirPath = tempDir.getAbsolutePath() + FS + "dir";
		new File(subDirPath).mkdir();
		
		String testPathD = subDirPath + FS + "t_D";
		File testFileD = new File(testPathD);
		IOtools.writeTXTAppend(testFileD, NL + " Q " + NL, false);

		assertFalse(fngrprD.matchedBy(testFileA));
		assertFalse(fngrprD.matchedBy(testFileB));
		assertFalse(fngrprD.matchedBy(testFileD));
		
		assertTrue(fngrprD.matchedBy(tempDir));
		
		
		// Nested file with wild cards in path and name
		FileFingerprint fngrprE = new FileFingerprint("." + FS + "*" + FS 
				+ "*_D", 3, "^ Q $");

		assertFalse(fngrprE.matchedBy(testFileA));
		assertFalse(fngrprE.matchedBy(testFileB));
		assertFalse(fngrprE.matchedBy(testFileD));
		
		assertTrue(fngrprE.matchedBy(tempDir));
    }

//------------------------------------------------------------------------------
    		
}
