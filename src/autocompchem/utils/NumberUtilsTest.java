package autocompchem.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Unit Test for number utilities
 * 
 * @author Marco Foscato
 */

public class NumberUtilsTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testIsParsableToInt() throws Exception
    {
    	String s = "1234567890";
    	assertTrue(NumberUtils.isParsableToInt(s),"Simple digits");
    	s = "123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Simple digits w/ point");
    	
    	s = " 1234567890 ";
    	assertTrue(NumberUtils.isParsableToInt(s),"Leading/traling spaces ");
    	s = "123456 7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Space in the middle");
    	
    	s = "+1234567890";
    	assertTrue(NumberUtils.isParsableToInt(s),"Signed digits");
    	s = "+123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Signed digits w/ point");
    	s = "-234567890";
    	assertTrue(NumberUtils.isParsableToInt(s),"Signed(-) digits");
    	s = "-123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Signed(-) digits w/ point");
    	
    	s = "i1234567890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Complex");
    	s = "-i123456.7890";
    	assertFalse(NumberUtils.isParsableToInt(s),"Complex w/ point");
    	
    }
    
//------------------------------------------------------------------------------

}
