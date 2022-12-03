package autocompchem.datacollections;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.ACCJson;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.perception.situation.Situation;
import autocompchem.run.Action;
import autocompchem.run.Action.ActionObject;
import autocompchem.run.Action.ActionType;
import autocompchem.text.TextBlock;


/**
 * Unit Test for {@link NamedData} class 
 * 
 * @author Marco Foscato
 */

public class NamedDataTest 
{

//------------------------------------------------------------------------------
    
    @Test
    public void testDetectType() throws Exception
    {  
    	NamedData nd = new NamedData();
    	
    	String s = "s";
    	nd.setValue(s);
    	assertTrue(NamedDataType.STRING.equals(nd.getType()),
    			"Detecting String");
    	
    	Boolean b = false;
    	nd.setValue(b);
    	assertTrue(NamedDataType.BOOLEAN.equals(nd.getType()),
    			"Detecting Boolean");
    	
    	int i = 1;
    	nd.setValue(i);
    	assertTrue(NamedDataType.INTEGER.equals(nd.getType()),
    			"Detecting Integer");
    	
    	File f = new File("pathname.txt");
    	nd.setValue(f);
    	assertTrue(NamedDataType.FILE.equals(nd.getType()),
    			"Detecting File");
    	
    	Situation sit = new Situation();
    	nd.setValue(sit);
    	assertTrue(NamedDataType.SITUATION.equals(nd.getType()),
    			"Detecting Situation");
    	
    	IAtomContainer iac = new AtomContainer();
    	nd.setValue(iac);
    	assertTrue(NamedDataType.IATOMCONTAINER.equals(nd.getType()),
    			"Detecting IAtomContainer");
    	
    	AtomContainerSet mols = new AtomContainerSet();
    	nd.setValue(mols);
    	assertTrue(NamedDataType.ATOMCONTAINERSET.equals(nd.getType()),
    			"Detecting IAtomContainer");
    	
    	ArrayList<String> lst = new ArrayList<String>();
    	nd.setValue(lst);
    	assertTrue(NamedDataType.TEXTBLOCK.equals(nd.getType()),
    			"Detecting TextBlock");
    	
    	TextBlock tb = new TextBlock();
    	nd.setValue(tb);
    	assertTrue(NamedDataType.TEXTBLOCK.equals(nd.getType()),
    			"Detecting TextBlock");
    	
    	BasisSet bs = new BasisSet();
    	nd.setValue(bs);
    	assertTrue(NamedDataType.BASISSET.equals(nd.getType()),
    			"Detecting TextBlock");
    	
    	ZMatrix zm = new ZMatrix();
    	nd.setValue(zm);
    	assertTrue(NamedDataType.ZMATRIX.equals(nd.getType()),
    			"Detecting ZMatrix");
    			
    	ListOfDoubles ld =new ListOfDoubles();
    	nd.setValue(ld);
    	assertTrue(NamedDataType.LISTOFDOUBLES.equals(nd.getType()),
    			"Detecting ListOfDoubles");
    	
    	ListOfIntegers li =new ListOfIntegers();
    	nd.setValue(li);
    	assertTrue(NamedDataType.LISTOFINTEGERS.equals(nd.getType()),
    			"Detecting ListOfDoubles");    	
    	
    	NormalMode nm = new NormalMode();
    	nd.setValue(nm);
    	assertTrue(NamedDataType.NORMALMODE.equals(nd.getType()),
    			"Detecting NormalMode");  
    	
    	NormalModeSet nms = new NormalModeSet();
    	nd.setValue(nms);
    	assertTrue(NamedDataType.NORMALMODESET.equals(nd.getType()),
    			"Detecting NormalModeSet");  
    	
    	Action a = new Action(ActionType.STOP,ActionObject.MASTERJOB);
    	nd.setValue(a);
    	assertTrue(NamedDataType.ACTION.equals(nd.getType()),
    			"Detecting Action");  
    	
    	Object o = new Object();
    	nd.setValue(o);
    	assertTrue(NamedDataType.UNDEFINED.equals(nd.getType()),
    			"Detecting Undefined Object");
    }

//------------------------------------------------------------------------------
 
    @Test
    public void testJsonRoundTrip() throws Exception
    {
    	List<NamedData> nds = new ArrayList<NamedData>();
    	nds.add(new NamedData("String", NamedDataType.STRING, "s"));
    	nds.add(new NamedData("Boolean", NamedDataType.BOOLEAN, false));
    	nds.add(new NamedData("Integer", NamedDataType.INTEGER, 1));
    	nds.add(new NamedData("Double", NamedDataType.DOUBLE, 1.23));
    	nds.add(new NamedData("Double", NamedDataType.DOUBLE, 1.0));
    	nds.add(new NamedData("File", NamedDataType.FILE, new File("path")));
    	nds.add(new NamedData("TextBlock", NamedDataType.TEXTBLOCK, 
    			new ArrayList<String>(Arrays.asList("These","are","3 lines"))));
    	nds.add(new NamedData("Situation", NamedDataType.SITUATION, 
    			new Situation()));
    	nds.add(new NamedData("IAtomContainer", NamedDataType.IATOMCONTAINER, 
    			new AtomContainer()));
    	nds.add(new NamedData("Undefined", NamedDataType.UNDEFINED, null));

    	Gson writer = ACCJson.getWriter();
    	Gson reader = ACCJson.getReader();
    	
    	for (NamedData nd : nds)
    	{
        	String jsonStr = writer.toJson(nd);
        	NamedData nd2 = reader.fromJson(jsonStr, NamedData.class);
        	if (!NamedData.jsonable.contains(nd.getType()))
        	{
        		assertEquals(NamedData.NONJSONABLE,nd2.getValueAsString());
        	} else {
	        	jsonStr = writer.toJson(nd2);
	        	assertEquals(nd.getReference(), nd2.getReference());
	        	assertEquals(nd.getType(), nd2.getType());
	        	assertEquals(nd.getValue(), nd2.getValue());
        	}
    	}
    }
    
//-----------------------------------------------------------------------------
    
}
