package autocompchem.perception.circumstance;

/*   
 *   Copyright (C) 2020  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import autocompchem.perception.infochannel.InfoChannelType;

/**
 * Unit Test for CircumstanceFactory
 * 
 * @author Marco Foscato
 */

public class CircumstanceFactoryTest 
{

    @Test
    public void testCreateFromString() throws Exception
    {
        String s = InfoChannelType.OUTPUTFILE + " "
        		+ CircumstanceConstants.MATCHES + " PATTERN";
        Circumstance c = CircumstanceFactory.createFromString(s);
        
        assertTrue(c instanceof MatchText, "Subclass of circumstance 1.");
        assertTrue(c.getChannelType().equals(InfoChannelType.OUTPUTFILE), 
        "InfoChannelType for circumstance 1.");
        
        
        
        s = InfoChannelType.LOGFEED + " "
        		+ CircumstanceConstants.NOMATCH + " PATTERN";
        c = CircumstanceFactory.createFromString(s);
        
		assertTrue(c instanceof MatchText, "Subclass of circumstance 2");
		assertTrue(((MatchText) c).negation, "Negation in circumstance 2");
		assertTrue(c.getChannelType().equals(InfoChannelType.LOGFEED), 
		"InfoChannelType for circumstance 2.");
        
		
		
		s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.LOOPCOUNTER + " LoopAB1 > 123";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(((LoopCounter) c).getCounterID().equals("LoopAB1"),
	    		"Counter name 1");
	    assertTrue(1000000 < ((LoopCounter) c).getMax(),"Maximum limit 1");
	    assertEquals(123, ((LoopCounter) c).getMin(),"Minimum limit 1");
        
		s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.LOOPCOUNTER + " LoopAB2 < 123";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(((LoopCounter) c).getCounterID().equals("LoopAB2"),
	    		"Counter name 2");
	    assertTrue(-1000000 > ((LoopCounter) c).getMin(),"Minimum limit 2");
	    assertEquals(123, ((LoopCounter) c).getMax(),"Maximum limit 2");
	        
	    
	    
		s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.LOOPCOUNTER + " 123 > ID";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(-1000000 > ((LoopCounter) c).getMin(),"Minimum limit 3");
	    assertEquals(123, ((LoopCounter) c).getMax(),"Maximum limit 3");
	    
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.LOOPCOUNTER + " 123 < ID";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(1000000 < ((LoopCounter) c).getMax(),"Maximum limit 4");
	    assertEquals(123, ((LoopCounter) c).getMin(),"Minimum limit 4");
	    
	    
	    
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.LOOPCOUNTER + " 123 < ID < 150";
	    c = CircumstanceFactory.createFromString(s);
	    assertEquals(123, ((LoopCounter) c).getMin(),"Minimum limit 5");
	    assertEquals(150, ((LoopCounter) c).getMax(),"Minimum limit 5");
	    
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.LOOPCOUNTER + " 150 > ID > 123";
	    c = CircumstanceFactory.createFromString(s);

	    assertEquals(123, ((LoopCounter) c).getMin(),"Minimum limit 6");
	    assertEquals(150, ((LoopCounter) c).getMax(),"Minimum limit 6");

	    
	    
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.MATCHESCOUNT + " myPattern 12";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(c.toString().matches(".*num: 12; cnstrType: EXACT.*"),
	    		"Parsing count-of-matches circumstance 1.");
        
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.MATCHESCOUNT + " myPattern 12 20";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(c.toString().matches(
	    		".*min: 12; max: 20; num: 0; cnstrType: RANGE.*"),
	    		"Parsing count-of-matches circumstance 2.");
	    
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.MATCHESCOUNT + " myPattern 12 MIN";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(c.toString().matches(
	    		".*min: 12; max: 0; num: 0; cnstrType: MIN.*"),
	    		"Parsing count-of-matches circumstance 3.");
        
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.MATCHESCOUNT + " myPattern 12 MAX";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(c.toString().matches(
	    		".*min: 0; max: 12; num: 0; cnstrType: MAX.*"),
	    		"Parsing count-of-matches circumstance 4.");
        
	    s = InfoChannelType.ANY + " "
				+ CircumstanceConstants.MATCHESCOUNT + " myPattern 12 20 NOT";
	    c = CircumstanceFactory.createFromString(s);
	    assertTrue(c.toString().matches(".*negation:true.*"),
	    		"Parsing count-of-matches circumstance 5.");
    }

//------------------------------------------------------------------------------

}
