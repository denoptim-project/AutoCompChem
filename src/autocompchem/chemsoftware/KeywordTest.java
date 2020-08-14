package autocompchem.chemsoftware;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;


/**
 * Unit Test for Keyword objects
 * 
 * @author Marco Foscato
 */

public class KeywordTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testConstructorFromString() throws Exception
    {
    	
    	String str = ChemSoftConstants.JDLABLOUDKEY + "LoudKey "
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "my value has 3 lines"
    			+ System.getProperty("line.separator") + "second line"
    			+ System.getProperty("line.separator") + "third line";
    	
    	Keyword k = new Keyword(str);
    	
    	assertEquals("LoudKey",k.getName(),"Keyword name(A)");
    	assertTrue(k.isLoud(),"Kind of keyword (A)");
    	assertEquals(3,k.getValue().size(),"Keyword value size(A)");
    	assertEquals("my value has 3 lines",k.getValue().get(0),
    			"Keyword value(1A)");
    	assertEquals("third line",k.getValue().get(2),"Keyword value(5A)");
    	
    	str = ChemSoftConstants.JDLABMUTEKEY + "MuteKey "
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "value";
    	
    	k = new Keyword(str);
    	
    	assertEquals("MuteKey",k.getName(),"Keyword name(B)");
    	assertTrue(!k.isLoud(),"Kind of keyword (B)");
    	assertEquals(1,k.getValue().size(),"Keyword value size(B)");
    	assertEquals("value",k.getValue().get(0),"Keyword value(1B)");
    	
    }
    
//------------------------------------------------------------------------------

}
