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
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.io.IOtools;


/**
 * Unit Test for {@link BufferedTranslator}.
 * 
 * @author Marco Foscato
 */

public class BufferedTranslatorTest 
{
    @TempDir 
    File tempDir;
    
    public static String NL = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------

    @Test
    public void testReadline_NoMatch() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File tmpFile = new File(tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "test.txt");
        IOtools.writeLineAppend(tmpFile,"1234567890",false);
        IOtools.writeLineAppend(tmpFile,"abcdefghij",true);
        
        String replacement = "replacement_not_used";
        BufferedTranslator bt = new BufferedTranslator(new FileReader(tmpFile), 
        		"patter_that_will_not_be_matched", replacement);
       
        String line = null;
        while ((line = bt.readLine()) != null)
        {
        	assertFalse(line.contains(replacement));
        }
        bt.close();
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReadline_Match() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File tmpFile = new File(tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "test.txt");
        IOtools.writeLineAppend(tmpFile,"1234567890",false);
        IOtools.writeLineAppend(tmpFile,"abcdefghij_hit1",true);
        IOtools.writeLineAppend(tmpFile,"12345hit67890",true);
        IOtools.writeLineAppend(tmpFile,"abcdefghij",true);
        IOtools.writeLineAppend(tmpFile,"1234567890",true);
        IOtools.writeLineAppend(tmpFile,"abcdefghhit3ij",true);
        
        String replacement = "replacement_used";
        BufferedTranslator bt = new BufferedTranslator(new FileReader(tmpFile), 
        		"h.t[0-9]", replacement);
       
        String line = null;
        int matches = 0;
        while ((line = bt.readLine()) != null)
        {
        	if (line.contains(replacement))
        		matches++;
        }
        bt.close();
        assertEquals(3, matches);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReadBuffer_NoMatch() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File tmpFile = new File(tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "test.txt");
        String line1 = "1234567890";  // 10
        String line2 = "abcdefghij "; // 11
        String line3 = " 1234567890"; // 11
        IOtools.writeLineAppend(tmpFile,line1,false);
        IOtools.writeLineAppend(tmpFile,line2,true);
        IOtools.writeLineAppend(tmpFile,line3,true);
        int expectedLength = 35; // 10 + 11 + 11 + (3*newline)
        
        String replacement = "replacement_not_used";
        
        int[] bufferSize = new int[]{1, 2, 4, 6, 10, 1024};
        int[] readBuffSize = new int[] {1, 2, 10, 1024};
        String[] regex = new String[] {"X", "XX", "long_patter_not_found"};
        
        for (int i=0; i<bufferSize.length; i++)
        {
        	for (int j=0; j<regex.length; j++)
            {
        		for (int iRBuff=0; iRBuff<readBuffSize.length; iRBuff++)
        		{
			        BufferedTranslator bt = new BufferedTranslator(
			        		new FileReader(tmpFile), regex[j], replacement, 
			        		bufferSize[i]);
			        char[] buffer = new char[readBuffSize[iRBuff]];
			        StringBuilder sb = new StringBuilder();
			        int readChars = 0;
			        while (readChars > -1) 
			        {
				        sb.append(Arrays.copyOfRange(buffer, 0, readChars));
			        	readChars = bt.read(buffer, 0, buffer.length);
			        }
			        String whole = sb.toString();
			        assertTrue(whole.contains(line1));
			        assertTrue(whole.contains(line2));
			        assertTrue(whole.contains(line3));
			        assertFalse(whole.contains(replacement));
			        assertEquals(expectedLength, whole.length());
			        bt.close();
        		}
            }
        }
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReadBuffer_Match() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File tmpFile = new File(tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "test.txt");
        List<String> lines = new ArrayList<String>(Arrays.asList(
        		"hit1234567890",          // 1 match
        		"abchit2defg_ij ",        // 1 match
        		" hit2 1234567890",       // 1 match
        		"abchit2defg_ij hit1",    // 2 matches
        		"hit2 1234567890 hit0",   // 2 matches
        		" hit21234567890hit0 ",   // 2 matches
        		"hit0hit1hit2hit3"));     // 4 matches
        IOtools.writeLinesAppend(tmpFile,lines,false);
        
        String[] replacement = new String[]{"", "@", "@NEWSTR@"};
        
        String[] regex = new String[] {"h.t[0-9]", "h", "hi"};
        
        // Define expected results
        int[] hitLength = new int[]{4, 1, 2};
        int[] numHitsPerLine = new int[]{1, 1, 1, 2, 2, 2, 4};
        
        int[] bufferSize = new int[]{1, 2, 4, 6, 10, 1024};
        int[] readBuffSize = new int[] {1, 2, 10, 1024};
        for (int iBuff=0; iBuff<bufferSize.length; iBuff++)
        {
        	for (int iRegex=0; iRegex<regex.length; iRegex++)
            {
            	for (int iRepl=0; iRepl<replacement.length; iRepl++)
                {
            		for (int iRBuff=0; iRBuff<readBuffSize.length; iRBuff++)
            		{
	            		BufferedTranslator bt = new BufferedTranslator(
				        		new FileReader(tmpFile), 
				        		regex[iRegex], replacement[iRepl], 
				        		bufferSize[iBuff]);
				        char[] buffer = new char[readBuffSize[iRBuff]];
				        StringBuilder sb = new StringBuilder();
				        int readChars = 0;
				        while (readChars > -1) 
				        {
					        sb.append(Arrays.copyOfRange(buffer, 0, readChars));
				        	readChars = bt.read(buffer, 0, buffer.length);
				        }
				        String whole = sb.toString();

				        //NB: the -1 make it keep the trailing empty lines
				        List<String> translatedLines = new ArrayList<String>(
				        		Arrays.asList(whole.split("\\r?\\n",-1)));
				        
				        // +1 because the writing to file adds a newline char
				        assertEquals(lines.size()+1, translatedLines.size());
				        
				        int expectedTotLenght = 0;
				        for (int iLine=0; iLine<lines.size(); iLine++)
				        {
				        	String msg = "Line:" + iLine
				        			+ " iRegex:" + iRegex
				        			+ " iRepl:" + iRepl;
				        	int expectedLineLength = lines.get(iLine).length() 
				        			- (hitLength[iRegex] 
				        					* numHitsPerLine[iLine])
				        			+ (replacement[iRepl].length() 
				        					* numHitsPerLine[iLine]);
				        	assertEquals(expectedLineLength, 
				        			translatedLines.get(iLine).length(), msg);
				        	expectedTotLenght = expectedTotLenght 
				        			+ expectedLineLength 
				        			+ 1; // counting the newline char
				        	assertEquals(lines.get(iLine).replaceAll(
				        			regex[iRegex], replacement[iRepl]),
				        			translatedLines.get(iLine), msg);
				        }
				        assertEquals(expectedTotLenght, whole.length());
				        bt.close();
                	}
                }
            }
        }
    }
    
//------------------------------------------------------------------------------

    /*
     * This is isolated because it tests the case of pattern formed upon first 
     * replacement of a matched string. These 2nd generation matches must not
     * be translated.
     */
    
    @Test
    public void testReadBuffer_Match2() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File tmpFile = new File(tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "test.txt");
        String line1 = "hhitit";  
        String line2 = "hhitithhitit"; 
        IOtools.writeLineAppend(tmpFile,line1,false);
        IOtools.writeLineAppend(tmpFile,line2,true);
        
        
        int[] bufferSize = new int[]{1, 2, 4, 6, 10, 1024};
        int[] readBuffSize = new int[] {1, 2, 10, 1024};
        
        for (int i=0; i<bufferSize.length; i++)
        {
    		for (int iRBuff=0; iRBuff<readBuffSize.length; iRBuff++)
    		{
		        BufferedTranslator bt = new BufferedTranslator(
		        		new FileReader(tmpFile), "hit", "", 
		        		bufferSize[i]);
		        char[] buffer = new char[readBuffSize[iRBuff]];
		        StringBuilder sb = new StringBuilder();
		        int readChars = 0;
		        while (readChars > -1) 
		        {
			        sb.append(Arrays.copyOfRange(buffer, 0, readChars));
		        	readChars = bt.read(buffer, 0, buffer.length);
		        }
		        String whole = sb.toString();
		        List<String> translatedLines = new ArrayList<String>(
		        		Arrays.asList(whole.split("\\r?\\n",-1)));
		        assertEquals(3, translatedLines.size());
		        assertEquals("hit", translatedLines.get(0));
		        assertEquals("hithit", translatedLines.get(1));
		        assertEquals("", translatedLines.get(2));
		        bt.close();
    		}
        }
    }
    
//------------------------------------------------------------------------------

    /*
     * This is isolated because it tests the case of a regex involving newline
     * character.
     */
    
    @Test
    public void testReadBuffer_Match3() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        File tmpFile = new File(tempDir.getAbsolutePath() 
        		+ System.getProperty("file.separator") + "test.txt");
        String line1 = "abchit";  
        String line2 = "hit 123 hit hit"; 
        IOtools.writeLineAppend(tmpFile,line1,false);
        IOtools.writeLineAppend(tmpFile,line2,true);
        IOtools.writeLineAppend(tmpFile,line2,true);
        IOtools.writeLineAppend(tmpFile,line2,true);
        
        int[] bufferSize = new int[]{1, 2, 4, 6, 10, 1024};
        int[] readBuffSize = new int[] {1, 2, 10, 1024};
        
        for (int i=0; i<bufferSize.length; i++)
        {
    		for (int iRBuff=0; iRBuff<readBuffSize.length; iRBuff++)
    		{
		        BufferedTranslator bt = new BufferedTranslator(
		        		new FileReader(tmpFile), "hit\\r?\\nhit", "", 
		        		bufferSize[i]);
		        char[] buffer = new char[readBuffSize[iRBuff]];
		        StringBuilder sb = new StringBuilder();
		        int readChars = 0;
		        while (readChars > -1) 
		        {
			        sb.append(Arrays.copyOfRange(buffer, 0, readChars));
		        	readChars = bt.read(buffer, 0, buffer.length);
		        }
		        String whole = sb.toString();
		        List<String> translatedLines = new ArrayList<String>(
		        		Arrays.asList(whole.split("\\r?\\n",-1)));
		        assertEquals(2, translatedLines.size());
		        assertEquals("abc 123 hit  123 hit  123 hit hit", 
		        		translatedLines.get(0));
		        assertEquals("", translatedLines.get(1));
		        bt.close();
    		}
        }
    }
    
//------------------------------------------------------------------------------

}
