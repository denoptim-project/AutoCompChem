package autocompchem.wiro.chem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;


public class DirectiveComponentTypeTest
{

//------------------------------------------------------------------------------

    @Test
    public void testGetEnum() throws Exception
    {
    	assertEquals(DirectiveComponentType.DIRECTIVE, 
    			DirectiveComponentType.getEnum("dir"));
    	assertEquals(DirectiveComponentType.DIRECTIVE, 
    			DirectiveComponentType.getEnum("diR"));
    	assertEquals(DirectiveComponentType.KEYWORD, 
    			DirectiveComponentType.getEnum("KEY"));
    	assertEquals(DirectiveComponentType.DIRECTIVEDATA, 
    			DirectiveComponentType.getEnum("dat"));
    	assertEquals(DirectiveComponentType.ANY, 
    			DirectiveComponentType.getEnum("*"));
    	DirectiveComponentType c = DirectiveComponentType.getEnum("invalid");
    	assertNull(c);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testShortForms() throws Exception
    {
    	Set<String> collected = new HashSet<String>();
    	for (DirectiveComponentType cdt : DirectiveComponentType.values())
    	{
    		collected.add(cdt.shortString.toUpperCase());
    	}
    	assertEquals(DirectiveComponentType.values().length, collected.size());
    	assertEquals(DirectiveComponentType.getShortForms(), collected);
    }
    
//------------------------------------------------------------------------------

}
