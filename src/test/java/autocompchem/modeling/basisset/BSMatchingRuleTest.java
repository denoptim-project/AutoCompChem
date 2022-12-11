package autocompchem.modeling.basisset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BSMatchingRuleTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testToParsableString() throws Exception
    {
    	BSMatchingRule orig = new BSMatchingRule("myTestRupe1", 
    			BasisSetConstants.ATMMATCHBYSYMBOL, 
    			null,
    			BasisSetConstants.BSSOURCENAME, 
    			"LANLDZ");
    	
    	String s = orig.toParsableString();
    	
    	BSMatchingRule rebuilt = new BSMatchingRule(s, "myTestRupe1");
    	
    	assertEquals(orig.getRefName(), rebuilt.getRefName());
    	assertEquals(orig.getKey(), rebuilt.getKey());
    	assertEquals(orig.getSourceType(), rebuilt.getSourceType());
    	assertEquals(orig.getSource(), rebuilt.getSource());
    	assertEquals(orig.getType(), rebuilt.getType());
    	
    	rebuilt = new BSMatchingRule(s, 1);
    	
    	assertEquals(s.split("\\s+")[0]+1, rebuilt.getRefName());
    	assertEquals(orig.getKey(), rebuilt.getKey());
    	assertEquals(orig.getSourceType(), rebuilt.getSourceType());
    	assertEquals(orig.getSource(), rebuilt.getSource());
    	assertEquals(orig.getType(), rebuilt.getType());
    	
    	String smarts = "[#6]";
    	String pathname = "/path/ti/file";
    	orig = new BSMatchingRule("myTestRupe1", 
    			BasisSetConstants.ATMMATCHBYSMARTS, 
    			smarts,
    			BasisSetConstants.BSSOURCELINK, 
    			pathname);
    	
    	s = orig.toParsableString();
    	
    	rebuilt = new BSMatchingRule(s, "myTestRupe1");
    	
    	assertEquals(orig.getRefName(), rebuilt.getRefName());
    	assertEquals(orig.getKey(), rebuilt.getKey());
    	assertEquals(orig.getSourceType(), rebuilt.getSourceType());
    	assertEquals(orig.getSource(), rebuilt.getSource());
    	assertEquals(orig.getType(), rebuilt.getType());
    }
    	
//------------------------------------------------------------------------------

}
