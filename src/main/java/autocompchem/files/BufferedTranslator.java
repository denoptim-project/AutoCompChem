package autocompchem.files;

/*
 *   Copyright (C) 2024  Marco Foscato
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

/**
 * A buffered reader that translates part of what it reads into something else. 
 */

public class BufferedTranslator extends BufferedReader
{
	/**
	 * Matches the char sequence that we want to translate into something else
	 */
	private Pattern pattern;
	
	/**
	 * The string we want to replace the char sequence with.
	 */
	private String replacment;
	
	/**
	 * We must read at least as much as needed to match the regex, and we use 
	 * the length of the regex as measure of how large the minimal buffer size 
	 * can be.
	 */
	private final int MYBUFFERLENGTH;
	
	private final int LENGTHOFREGEX;

	/**
	 * The buffer of data that has been read from 
	 * the reader and translated according to the replacement/translation rules.
	 */
	private char[] translatedBuffer;
	
	/**
	 * The left over from previous filling of translated buffer. This is the 
	 * tails of the read-in buffer that could not be translated as it might 
	 * a portion of any pattern to translate.
	 */
	private char[] leftOverBuffer;
	
	/**
	 * The index of the last valid position in the left-over buffer
	 */
	private int endLeftOverBuffer = 0;
	
	/**
	 * Flag recording that the reader has reached the end of file
	 */
	private boolean reachedEOF = false;
	
//------------------------------------------------------------------------------

	/**
	 * Constructor specifying what to replace and what to replace it with.
	 * @param in the underlying reader to work with
	 * @param regexToReplace the REGEX pattern to be replaced.
	 * @param replacment the string to replace the pattern with.
	 */
	
	public BufferedTranslator(Reader in, String regexToReplace, String replacment) 
	{
		this(in, regexToReplace, replacment, 1024);
	}

//------------------------------------------------------------------------------
	
	/**
	 * Constructor specifying what to replace and what to replace it with.
	 * @param in the underlying reader to work with
	 * @param regexToReplace the REGEX pattern to be replaced.
	 * @param replacment the string to replace the pattern with.
	 * @param bufferSize the max size of the translated buffer. This will be set 
	 * to this value only if the given value if larger than three times length
	 * of regex to match (NB: not that of the match!).
	 */
	
	public BufferedTranslator(Reader in, String regexToReplace, 
			String replacment, int bufferSize) 
	{
		super(in);
		pattern = Pattern.compile(regexToReplace);
		this.LENGTHOFREGEX = regexToReplace.length();
		if (bufferSize > 3*LENGTHOFREGEX)
			this.MYBUFFERLENGTH = bufferSize;
		else
			this.MYBUFFERLENGTH = 1024;
		this.replacment = replacment;
	}
	
//------------------------------------------------------------------------------

	@Override
    public int read() throws IOException {
		throw new IllegalAccessError("USE OF UNIMPLEMENTED read() in " +
				this.getClass().getName());
	}

//------------------------------------------------------------------------------

	@Override
    public String readLine() throws IOException {
		String originalLine = super.readLine();
		if (originalLine==null)
			return null;
		
		Matcher m = pattern.matcher(originalLine);
		
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
		    m.appendReplacement(sb, replacment);
		}
		m.appendTail(sb);

        return sb.toString();
    }

//------------------------------------------------------------------------------
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException 
	{
		if (translatedBuffer==null)
			if (pourToTranslatedBuffer()<1)
				return -1;
		
		// Keep reading and translating as much as we can up to 'len'
		int returnFromReader = 1; // negative means True
		while (translatedBuffer.length<len && returnFromReader>0)
		{
			returnFromReader = pourToTranslatedBuffer();
		}

		// Define how much we can write into the destination buffer (cbuf)
		int numCharsPutInDestBuffer = 0;
		if (len<=translatedBuffer.length)
		{
			numCharsPutInDestBuffer = len; 
		} else {
			numCharsPutInDestBuffer = translatedBuffer.length;
		}

		// Fill the destination buffer
		for (int i=0; i<numCharsPutInDestBuffer; i++)
		{
			cbuf[i+off] = translatedBuffer[i];
		}
		
		// Consume the translated buffer
		if (len<=translatedBuffer.length)
		{
			translatedBuffer = Arrays.copyOfRange(
					translatedBuffer, len, translatedBuffer.length);
		} else {
			translatedBuffer = null;
		}
		
		return numCharsPutInDestBuffer;
	}
	
//------------------------------------------------------------------------------

	/**
	 * The logic is to read more than what we actually translate, so that we
	 * can peek into what comes after the translated piece and
	 * make sure that the pattern to be matched and translated is not truncated 
	 * by the end of the buffer.
	 * @return the number of characters read from the reader, or 
	 * -1 if the end of file has been reached.
	 */
	private int pourToTranslatedBuffer() throws IOException
	{	
		char[] readInBuff = new char[MYBUFFERLENGTH];
		
		// Take the left over from previous iteration
		for (int i=0; i<endLeftOverBuffer; i++)
			readInBuff[i] = leftOverBuffer[i];
		
		// tail that we leave as it could contain a fraction of the pattern
		int tail = 0;
		
		// and take more characters from the reader
		int readInLength = 0;
		if (!reachedEOF)
		{
			readInLength = super.read(readInBuff, endLeftOverBuffer, 
				MYBUFFERLENGTH-endLeftOverBuffer);
			tail = LENGTHOFREGEX;
			// Reached EOF now
			if (readInLength < 0)
			{
				reachedEOF = true;
				tail = 0;
				readInLength = 0;
			}
		}
		
		// Nothing left to poor
		if (reachedEOF && endLeftOverBuffer<1)
			return -1;
		
		Matcher m = pattern.matcher(CharBuffer.wrap(readInBuff));

		// Find the largest index we can consider without breaking a pattern 
		// that could be spanning over the limit of the read-in buffer
		int endOfTranslatable = endLeftOverBuffer + readInLength - tail;
		int endOfLastMatch = -1;
		boolean thereIsAnyMatch = false;
		while (m.find()) {
			thereIsAnyMatch = true;
			endOfLastMatch = m.end();
		}
		if (thereIsAnyMatch)
		{
			endOfTranslatable = Math.max(endOfLastMatch, endOfTranslatable);
		}
		
		// Translate the section that can be translated
		char[] toTranslateBuffer = null;
		if (thereIsAnyMatch)
		{ 
			StringBuilder strBuilder = new StringBuilder();
			m.reset();
			while (m.find()) {
			    m.appendReplacement(strBuilder, replacment);
			}
			// Do NOT append the tail, as the tail is after 'endOfTranslatable'
			//m.appendTail(strBuilder)
			// But we still need to append chars between end of last hit and 
			// end of translatable. Done here:
			strBuilder.append(Arrays.copyOfRange(readInBuff, 
					endOfLastMatch, endOfTranslatable));
			toTranslateBuffer = new char[strBuilder.length()];
			strBuilder.getChars(0, strBuilder.length(), toTranslateBuffer, 0);
		} else {
			toTranslateBuffer = Arrays.copyOfRange(
					readInBuff, 0, endOfTranslatable);
		}
		if (translatedBuffer!=null)
		{
			translatedBuffer = ArrayUtils.addAll(translatedBuffer,
					toTranslateBuffer);
		} else {
			translatedBuffer = toTranslateBuffer;
		}
		
		// Keep the tail, if any, for next fill iteration
		leftOverBuffer = Arrays.copyOfRange(readInBuff, endOfTranslatable, 
				readInLength + endLeftOverBuffer);
		endLeftOverBuffer = readInLength + endLeftOverBuffer - endOfTranslatable;
		
		return readInLength;
	}
	
//------------------------------------------------------------------------------
	
}
