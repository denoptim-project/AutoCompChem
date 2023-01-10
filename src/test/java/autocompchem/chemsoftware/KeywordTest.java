package autocompchem.chemsoftware;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.ACCJson;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.worker.TaskID;


/**
 * Unit Test for Keyword objects
 * 
 * @author Marco Foscato
 */

public class KeywordTest 
{

//------------------------------------------------------------------------------

    @Test
    public void testMakeFromJDLineg() throws Exception
    {
    	
    	String str = ChemSoftConstants.JDLABLOUDKEY + "LoudKey "
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "my value has 3 lines"
    			+ System.getProperty("line.separator") + "second line"
    			+ System.getProperty("line.separator") + "third line";
    	
    	Keyword k = Keyword.makeFromJDLine(str);
    	
    	assertEquals("LoudKey",k.getName(),"Keyword name(A)");
    	assertTrue(k.isLoud(),"Kind of keyword (A)");
    	assertEquals(3,k.getValueAsLines().size(),"Keyword value size(A)");
    	assertEquals("my value has 3 lines",k.getValueAsLines().get(0),
    			"Keyword value(1A)");
    	assertEquals("third line",k.getValueAsLines().get(2),"Keyword value(5A)");
    	
    	str = ChemSoftConstants.JDLABMUTEKEY + "MuteKey "
    			+ ChemSoftConstants.JDKEYVALSEPARATOR + "value";
    	
    	k = Keyword.makeFromJDLine(str);
    	
    	assertEquals("MuteKey",k.getName(),"Keyword name(B)");
    	assertTrue(!k.isLoud(),"Kind of keyword (B)");
    	assertEquals(1,k.getValueAsLines().size(),"Keyword value size(B)");
    	assertEquals("value",k.getValueAsLines().get(0),"Keyword value(1B)");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	List<Keyword> keys = new ArrayList<Keyword>();
    	keys.add(new Keyword("keyString", true, (Object) "value"));
    	keys.add(new Keyword("keyInt", false, 1));
    	keys.add(new Keyword("keyDouble", false, 1.2));

    	ParameterStorage ps = new ParameterStorage();
    	ps.setParameter(new NamedData(ChemSoftConstants.JDACCTASK, 
    			"SOME_TEXT"));
    	ps.setParameter(new NamedData("nd", "some other text"));
    	Keyword kWithTask = new Keyword("kWithTask", true, ps);
    	kWithTask.setTaskParams(ps);
    	keys.add(kWithTask);
    	
    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	for (Keyword kOriginal : keys)
    	{
	    	String json = writer.toJson(kOriginal);
	    	Keyword kFromJson = reader.fromJson(json, Keyword.class);
	    	assertEquals(kOriginal, kFromJson);
    	}
    }
    
//------------------------------------------------------------------------------

}
