package autocompchem.chemsoftware;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.chemsoftware.nwchem.NWChemOutputAnalyzer;
import autocompchem.files.FileFingerprint;
import autocompchem.io.IOtools;
import autocompchem.worker.Worker;


/**
 * Unit Test for {@link ChemSoftOutputAnalyzerBuilder}.
 * 
 * @author Marco Foscato
 */

public class ChemSoftOutputAnalyzerBuilderTest 
{
	private static String fileSeparator = System.getProperty("file.separator");
	private static String NL = System.getProperty("line.separator");

    @TempDir 
    protected File tempDir;
    
//------------------------------------------------------------------------------
  
    @Test
    public void testRegisterAnalyzer() throws Exception
    {
    	ChemSoftOutputAnalyzerBuilder b = new ChemSoftOutputAnalyzerBuilder();
    	int defaultSize = b.getAnalyzableSoftwareNames().size();
    		
    	b = new ChemSoftOutputAnalyzerBuilder()
        		.registerAnalyzer("123a", new TestOutputAnalyzer())
        		.registerAnalyzer("123b", new TestOutputAnalyzer())
        		.registerAnalyzer("123", new TestOutputAnalyzer());
    	assertEquals(3+defaultSize, b.getAnalyzableSoftwareNames().size());
    }

//------------------------------------------------------------------------------
	  
    @Test
    public void testMakeInstanceFromName() throws Exception
    {
    	ChemSoftOutputAnalyzerBuilder b = new ChemSoftOutputAnalyzerBuilder()
        		.registerAnalyzer("123", new TestOutputAnalyzer());
        		
    	ChemSoftOutputAnalyzer csoa = b.makeInstance("123");
    	assertEquals(TestOutputAnalyzer.IDVAL, csoa.inFile.getName());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testMakeInstanceForFile() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	
    	String query = "Log of MySoftware";
    	TestOutputAnalyzer analyzer = new TestOutputAnalyzer();
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 3, " something we'll not find"));
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 2, "^" + query+ "$"));
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 3, " more stuff we'll not find"));
    	
      	ChemSoftOutputAnalyzerBuilder b = new ChemSoftOutputAnalyzerBuilder()
          		.registerAnalyzer("MySoftware", analyzer);
          		
      	// Simple log/output file
    	String logFilePath = tempDir.getAbsolutePath() + fileSeparator + "log";
		File logFile = new File(logFilePath);
		IOtools.writeTXTAppend(logFile, query + NL + "more"
				+ NL + "and more text", false);
		
      	ChemSoftOutputAnalyzer csoa = b.makeInstance(logFile);
      	assertEquals(TestOutputAnalyzer.IDVAL, csoa.inFile.getName());
      	

      	// Simple log/output file not matched due to to short sample
    	String logFilePath2 = tempDir.getAbsolutePath() + fileSeparator + "log2";
		File logFile2 = new File(logFilePath2);
		IOtools.writeTXTAppend(logFile2, NL + NL + NL + NL + query + " more"
				+ NL + "and more text", false);
		
      	csoa = b.makeInstance(logFile2);
      	assertNull(csoa);
      	
     
      	// Simple log/output file not matched due to wrong formatting
    	String logFilePath3 = tempDir.getAbsolutePath() + fileSeparator + "log3";
		File logFile3 = new File(logFilePath3);
		IOtools.writeTXTAppend(logFile3, query + " and more"
				+ NL + "and more text", false);
		
      	csoa = b.makeInstance(logFile3);
      	assertNull(csoa);
      	
      	
      	// Folder tree containing log/output file 
      	String query2 = "MyNewSoftware Log Starts Here";
      	File dir = new File(tempDir.getAbsolutePath() + fileSeparator + "dir"
      			+ fileSeparator + "dir2");
      	dir.mkdirs();
    	String logFilePath4 = dir.getAbsolutePath() + fileSeparator + "4.log";
		IOtools.writeTXTAppend(new File(logFilePath4), NL + query2 + NL, false);

    	analyzer.outputFingerprints.add(
    			new FileFingerprint("./*/*.log", 2, "^" + query2+ "$"));
      	csoa = b.makeInstance(dir);
      	assertNull(csoa); // Log exists but is in wrong location
      	
    	analyzer.outputFingerprints.add(
    			new FileFingerprint("./*/*/*.out", 2, "^" + query2+ "$"));
      	csoa = b.makeInstance(dir);
      	assertNull(csoa); // Log exists but has wrong name
      	
      	csoa = b.makeInstance(dir);
      	assertNull(csoa); //Log exists but we use wrong root

    	analyzer.outputFingerprints.add(
    			new FileFingerprint("./*/*/*.log", 2, "^" + query2+ "$"));
      	csoa = b.makeInstance(tempDir);
      	assertEquals(TestOutputAnalyzer.IDVAL, csoa.inFile.getName());
    }   
    
//------------------------------------------------------------------------------

}
