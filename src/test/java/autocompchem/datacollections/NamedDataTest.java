package autocompchem.datacollections;


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

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData.NamedDataType;
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
 * Unit Test for NamedData class 
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

}
