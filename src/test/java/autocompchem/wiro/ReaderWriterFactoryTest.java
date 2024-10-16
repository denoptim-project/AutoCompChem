package autocompchem.wiro;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileFingerprint;
import autocompchem.io.IOtools;
import autocompchem.run.SoftwareId;
import autocompchem.wiro.OutputReader;
import autocompchem.wiro.ReaderWriterFactory;
import autocompchem.wiro.chem.gaussian.GaussianInputWriter;
import autocompchem.wiro.chem.gaussian.GaussianOutputReader;
import autocompchem.worker.Worker;


/**
 * Unit Test for {@link ReaderWriterFactory}.
 * 
 * @author Marco Foscato
 */

public class ReaderWriterFactoryTest 
{
	private static String fileSeparator = System.getProperty("file.separator");
	private static String NL = System.getProperty("line.separator");

    @TempDir 
    protected File tempDir;
    
//------------------------------------------------------------------------------
  
    @Test
    public void testRegisterAnalyzer() throws Exception
    {
    	// This is needed to make sure we do not have the test analyzer in the 
    	// registry, which could happen if any other unit test that registers 
    	// that analyzer is run before this one.
    	ReaderWriterFactory.getInstance().deregisterOutputReader(
    			new TestOutputAnalyzer());
    	
    	int defaultSize = 
    			ReaderWriterFactory.getRegisteredSoftwareIDs().size();
    		
    	ReaderWriterFactory.getInstance().registerOutputReader(
    			new TestOutputAnalyzer());
    	assertEquals(1+defaultSize, 
    			ReaderWriterFactory.getRegisteredSoftwareIDs().size());
    }

//------------------------------------------------------------------------------
	  
    @Test
    public void testMakeOutputReaderInstance() throws Exception
    {
    	TestOutputAnalyzer example = new TestOutputAnalyzer();
    	ReaderWriterFactory.getInstance().registerOutputReader(example);
        		
    	OutputReader sor = ReaderWriterFactory.getInstance()
    			.makeOutputReaderInstance(example.getSoftwareID());
    	assertEquals(TestOutputAnalyzer.IDVAL, sor.getLogPathName().getName());
    }

//------------------------------------------------------------------------------
  	  
    @Test
    public void testMakeInputWriterInstance() throws Exception
    {
      	TestOutputAnalyzer outputReader = new TestOutputAnalyzer();
      	SoftwareId softwareID = outputReader.getSoftwareID();

      	ReaderWriterFactory factory = 
      			ReaderWriterFactory.getInstance();
      	factory.registerOutputReader(outputReader);
      	
        Worker writer = factory.makeInstanceInputWriter(softwareID);
      	assertTrue(writer instanceof TestInputWriter);
      	
      	// IT's already there, but just to be sure...
      	GaussianOutputReader gor = new GaussianOutputReader();
      	factory.registerOutputReader(gor);
      	
      	writer = factory.makeInstanceInputWriter(gor.getSoftwareID());
       	assertTrue(writer instanceof GaussianInputWriter);
    } 	
    
//------------------------------------------------------------------------------

    @Test
    public void testMakeInstanceForFile_simultaneousMatches() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	
    	String query = "Log of MySoftware";
    	String query2 = "2nd condition to match";
    	String query3 = "3rd condition to match";
    	TestOutputAnalyzer analyzer = new TestOutputAnalyzer();
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 4, "^" + query+ "$"));
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 4, "^" + query2+ "$"));
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 4, "^" + query3+ "$"));
    	
      	ReaderWriterFactory b = 
      			ReaderWriterFactory.getInstance();
      	b.registerOutputReader(analyzer);
          		
      	// Simple log/output file
    	String logFilePath = tempDir.getAbsolutePath() + fileSeparator + "log";
		File logFile = new File(logFilePath);
		IOtools.writeTXTAppend(logFile, "some text " + NL
				+ query3 + NL 
				+ query + NL
				+ query2 + NL 
				+ "more"
				+ NL + "and more text", false);
		
		OutputReader sor = b.makeOutputReaderInstance(logFile);
      	assertEquals(TestOutputAnalyzer.IDVAL, sor.getLogPathName().getName());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testMakeInstanceForFile() throws Exception
    {
    	assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
    	
    	String query = "Log of MySoftware";
    	TestOutputAnalyzer analyzer = new TestOutputAnalyzer();
    	analyzer.outputFingerprints.add(
    			new FileFingerprint(".", 2, "^" + query+ "$"));
    	
      	ReaderWriterFactory b = 
      			ReaderWriterFactory.getInstance();
      	b.registerOutputReader(analyzer);
          		
      	// Simple log/output file
    	String logFilePath = tempDir.getAbsolutePath() + fileSeparator + "log";
		File logFile = new File(logFilePath);
		IOtools.writeTXTAppend(logFile, query + NL + "more"
				+ NL + "and more text", false);
		
		OutputReader sor = b.makeOutputReaderInstance(logFile);
      	assertEquals(TestOutputAnalyzer.IDVAL, sor.getLogPathName().getName());
      	

      	// Simple log/output file not matched due to too short sample:
      	// we read too few lines to find the matching text
    	String logFilePath2 = tempDir.getAbsolutePath() + fileSeparator + "log2";
		File logFile2 = new File(logFilePath2);
		IOtools.writeTXTAppend(logFile2, NL + NL + NL + NL + query + " more"
				+ NL + "and more text", false);
		
      	sor = b.makeOutputReaderInstance(logFile2);
      	assertNull(sor);
      	
     
      	// Simple log/output file not matched due to wrong formatting:
      	// the output fingerprint wants to have the query as the only text in the row
      	// but in logFile3 it is not (because of the " and more" string after the query)
    	String logFilePath3 = tempDir.getAbsolutePath() + fileSeparator + "log3";
		File logFile3 = new File(logFilePath3);
		IOtools.writeTXTAppend(logFile3, query + " and more"
				+ NL + "and more text", false);
		
      	sor = b.makeOutputReaderInstance(logFile3);
      	assertNull(sor);
      	
      	
      	// Folder tree containing log/output file 
      	String query2 = "MyNewSoftware Log Starts Here";
      	File dir = new File(tempDir.getAbsolutePath() + fileSeparator + "dir"
      			+ fileSeparator + "dir2");
      	dir.mkdirs();
    	String logFilePath4 = dir.getAbsolutePath() + fileSeparator + "4.log";
		IOtools.writeTXTAppend(new File(logFilePath4), NL + query2 + NL, false);

    	analyzer.outputFingerprints.add(new FileFingerprint("."
    			+ fileSeparator + "*" 
    			+ fileSeparator + "*.log", 2, "^" + query2+ "$"));
      	sor = b.makeOutputReaderInstance(dir);
      	assertNull(sor); // Log exists but is in wrong location: ./4.log instead of ./*/*.log
      	
    	analyzer.outputFingerprints.add(
    			new FileFingerprint("." 
    					+ fileSeparator + "*.out", 2, "^" + query2+ "$"));
      	sor = b.makeOutputReaderInstance(dir);
      	assertNull(sor); // Log exists but has wrong name: *.log instead of *.out
      	
    	analyzer.outputFingerprints.add(
    			new FileFingerprint("." 
    					+ fileSeparator + "*.log", 2, "^" + query2+ "$"));
      	sor = b.makeOutputReaderInstance(dir);
      	assertNotNull(sor); //Log exists and we find it, and is found by the TestOutputAnalyzer
      	assertEquals(TestOutputAnalyzer.IDVAL, sor.getLogPathName().getName());

    	analyzer.outputFingerprints.add(
    			new FileFingerprint("." 
    					+ fileSeparator + "*" 
    					+ fileSeparator + "*" 
    					+ fileSeparator + "*.log", 2, "^" + query2+ "$"));
      	sor = b.makeOutputReaderInstance(tempDir);
      	assertEquals(TestOutputAnalyzer.IDVAL, sor.getLogPathName().getName());
    }   
    
//------------------------------------------------------------------------------

}
